package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.infra.ObservadorAtorPostgres;
import com.example.siggtrat.infra.ObservadorAtorPostgres.Ator;

import com.example.siggtrat.dominio.pendencia.EstadoPendencia;
import com.example.siggtrat.dominio.pendencia.TipoPendencia;
import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.entidade.EventoVinculoContratual;
import com.example.siggtrat.entidade.PendenciaAdministrativa;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.entidade.VinculoContratual;
import com.example.siggtrat.repositorio.VinculoContratualRepositorio;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Escopo: a consulta filtrada descobre a causa pedida, mas não disputa lock de
 * contrato alheio ao recorte. Publicação, Pessoa e vínculo nascem por HTTP;
 * persistência e observação de waits usam PostgreSQL/JPA reais. Só o relógio é
 * controlado. O prazo da barreira é watchdog do teste, não requisito de SLA.
 *
 * A barreira usa o primeiro candidato elegível na ordem do sincronizador. Na
 * base acumulada ele pode ser anterior à fixture: somente SELECT FOR UPDATE,
 * sem alterar qualquer campo. Na base isolada, o contrato externo da própria
 * fixture foi criado antes do consultado e garante a mesma ordem.
 *
 * A recorrência por atos válidos e a concorrência entre produtores continuam
 * cobertas por AuditoriaFinalPendenciaFimControlesHttpIT, sem alterar seus
 * prazos ou asserções. Este teste não finge executar essa sequência novamente.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PendenciasRecorteSincronizacaoHttpIT extends ContratosHttpBase {
    @org.springframework.beans.factory.annotation.Autowired private javax.sql.DataSource dataSourceObservador;

    @Autowired private VinculoContratualRepositorio vinculos;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void consultaFiltradaDescobreCausaEReenvioSemDisputarContratoNaoSolicitado(boolean resumo) throws Exception {
        var f = preparar();
        var antesConsultado = fonte(f.consultado().vinculoId());
        var antesExterno = fonte(f.externo().vinculoId());
        long barreira = primeiroCandidatoExterno(f.hoje(), f.consultado().contrato().id());
        // Repetidos e ordenação não ampliam o recorte. Zero/negativo são IDs
        // sem correspondência, não autorização para uma varredura global.
        String filtro = "contratoIds=" + f.consultado().contrato().id()
                + "&contratoIds=-1&contratoIds=0&contratoIds=" + f.consultado().contrato().id();
        String rota = rota(resumo, filtro);
        assertThat(pendencias(f.consultado().vinculoId())).isEmpty();
        assertThat(pendencias(f.externo().vinculoId())).isEmpty();

        var primeira = consultarEnquantoOutroContratoPermaneceBloqueado(f.c().localA, rota, f.hoje(), barreira);
        var materializadas = pendencias(f.consultado().vinculoId());
        var repetida = consultarEnquantoOutroContratoPermaneceBloqueado(f.c().localA, rota, f.hoje(), barreira);

        assertAll(
                () -> conferirResposta(resumo, primeira, 1, f.consultado().vinculoId()),
                () -> assertThat(repetida).as("Reenvio conserva resposta e identidade da tarefa").isEqualTo(primeira),
                () -> assertThat(materializadas).as("Causa ainda não materializada é descoberta no recorte").hasSize(1),
                () -> assertThat(materializadas).allSatisfy(p -> {
                    assertThat(p.estado()).isEqualTo(EstadoPendencia.ABERTA);
                    assertThat(p.data()).isEqualTo(f.consultado().contrato().fim());
                    assertThat(p.chave()).isEqualTo("FIM_VIGENCIA_VINCULO:" + f.consultado().vinculoId()
                            + ":" + f.consultado().contrato().fim());
                    assertThat(p.gestao()).isEqualTo(f.c().gestaoA.getId());
                    assertThat(p.se()).isEqualTo(f.c().seA.getId());
                }),
                () -> assertThat(pendencias(f.consultado().vinculoId())).isEqualTo(materializadas),
                () -> assertThat(pendencias(f.externo().vinculoId()))
                        .as("GET filtrado não materializa o contrato externo da fixture").isEmpty(),
                () -> assertThat(fonte(f.consultado().vinculoId())).isEqualTo(antesConsultado),
                () -> assertThat(fonte(f.externo().vinculoId())).isEqualTo(antesExterno));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void idsSemCorrespondenciaNaoSeTransformamEmConsultaGlobal(boolean resumo) throws Exception {
        var f = preparar();
        var antesConsultado = fonte(f.consultado().vinculoId());
        var antesExterno = fonte(f.externo().vinculoId());
        long barreira = primeiroCandidatoExterno(f.hoje(), f.consultado().contrato().id());
        String rota = rota(resumo, "contratoIds=-1&contratoIds=0&contratoIds=-1&contratoIds=" + Long.MAX_VALUE);

        var resposta = consultarEnquantoOutroContratoPermaneceBloqueado(f.c().localA, rota, f.hoje(), barreira);

        assertAll(
                () -> conferirResposta(resumo, resposta, 0, null),
                () -> assertThat(pendencias(f.consultado().vinculoId())).isEmpty(),
                () -> assertThat(pendencias(f.externo().vinculoId())).isEmpty(),
                () -> assertThat(fonte(f.consultado().vinculoId())).isEqualTo(antesConsultado),
                () -> assertThat(fonte(f.externo().vinculoId())).isEqualTo(antesExterno));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void ausenciaDeFiltroEParametroVazioConservamDescobertaGlobal(String filtro) throws Exception {
        var f = preparar();
        var antesConsultado = fonte(f.consultado().vinculoId());
        var antesExterno = fonte(f.externo().vinculoId());
        String parametros = filtro == null ? "" : "contratoIds=" + filtro;
        assertThat(pendencias(f.consultado().vinculoId())).isEmpty();
        assertThat(pendencias(f.externo().vinculoId())).isEmpty();

        try (var clock = Mockito.mockStatic(RelogioSistema.class, Mockito.CALLS_REAL_METHODS)) {
            clock.when(RelogioSistema::hoje).thenReturn(f.hoje());
            consultar(f.c().localA, rota(false, parametros), "Sem recorte explícito, a descoberta continua global");
            var resumo = consultar(f.c().localA, rota(true, parametros), "Resumo sem filtro conserva o escopo do leitor");
            assertThat(resumo.path("finsNaturaisVigencia").asLong()).isGreaterThanOrEqualTo(2);
        }
        assertAll(
                () -> assertThat(pendencias(f.consultado().vinculoId())).hasSize(1),
                () -> assertThat(pendencias(f.externo().vinculoId())).hasSize(1),
                () -> assertThat(fonte(f.consultado().vinculoId())).isEqualTo(antesConsultado),
                () -> assertThat(fonte(f.externo().vinculoId())).isEqualTo(antesExterno));
    }

    private Preparado preparar() throws Exception {
        var c = criarCenarioPersistido();
        var externo = criarContratoAberto(c, "Externo");
        var consultado = criarContratoAberto(c, "Consultado");
        assertThat(externo.contrato().id()).isLessThan(consultado.contrato().id());
        assertThat(externo.contrato().fim()).isEqualTo(consultado.contrato().fim());
        return new Preparado(c, externo, consultado, consultado.contrato().fim().plusDays(1));
    }

    private Aberto criarContratoAberto(Cenario c, String papel) throws Exception {
        var n = tx(() -> publicado(c, 5, null, false));
        long id = enviar(c.localA, "/api/vinculos-contratuais", mapa(
                "contratoId", n.id(), "composicaoId", n.composicao1(), "dataInicio", RelogioSistema.hoje(),
                "pessoa", mapa("nome", "Pessoa sintética recorte " + papel + " " + n.id(),
                        "cpf", cpf(Math.toIntExact(758900000L + n.id())))),
                201, "Origem contratual e pessoal completa, antes do término futuro").path("id").asLong();
        return new Aberto(n, id);
    }

    private long primeiroCandidatoExterno(LocalDate hoje, long consultado) {
        return emTransacao(() -> {
            var primeiro = vinculos.buscarCandidatosFimNatural(hoje, 0L, 0L, PageRequest.of(0, 1));
            assertThat(primeiro).as("Fixture garante ao menos o contrato externo elegível").hasSize(1);
            long id = primeiro.get(0).getContrato().getId();
            assertThat(id).as("Barreira é anterior e externa ao contrato consultado").isLessThan(consultado);
            return id;
        });
    }

    private JsonNode consultarEnquantoOutroContratoPermaneceBloqueado(
            UsuarioAplicacao usuario, String rota, LocalDate hoje, long contratoExterno) throws Exception {
        var requisicao = autenticar(get(rota), usuario); // Sessão preparada no chamador.
        var bloqueado = new CompletableFuture<Long>();
        var liberar = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        Future<?> detentor = null;
        Future<MvcResult> consulta = null;
        MvcResult resposta = null;
        Throwable erro = null;
        var problemasLimpeza = new ArrayList<Throwable>();
        boolean esperaExterna = false, terminouComLockRetido = false, detentorAindaRetinha = false;
        try {
            detentor = pool.submit(() -> emTransacao(() -> {
                em.find(Contrato.class, contratoExterno, LockModeType.PESSIMISTIC_WRITE);
                bloqueado.complete(((Number) em.createNativeQuery("select pg_backend_pid()").getSingleResult()).longValue());
                try {
                    if (!liberar.await(30, TimeUnit.SECONDS)) throw new AssertionError("Barreira não foi liberada pelo teste.");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); throw new IllegalStateException(e);
                }
                return null;
            }));
            long conexaoExterna = bloqueado.get(10, TimeUnit.SECONDS);
            var atorConsulta = new Ator("consulta-recorte");
            var jdbcObservador = new org.springframework.jdbc.core.JdbcTemplate(dataSourceObservador);
            consulta = pool.submit(() -> atorConsulta.executar(() -> {
                try (var clock = Mockito.mockStatic(RelogioSistema.class, Mockito.CALLS_REAL_METHODS)) {
                    clock.when(RelogioSistema::hoje).thenReturn(hoje);
                    return mvc.perform(requisicao).andReturn();
                }
            }));
            // Há dois desfechos observáveis: resposta concluída com a barreira
            // retida, ou uma espera indevida na conexão externa. Ausência de
            // ambos até o watchdog é preparação inconclusiva, não prova de SLA.
            long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (!consulta.isDone() && System.nanoTime() < limite) {
                if (ObservadorAtorPostgres.esperaDiretaNoAlvo(jdbcObservador, conexaoExterna, atorConsulta,
                        "ContratoRepositorio.buscarPorIdParaAtualizacao", contratoExterno, "contrato")) { esperaExterna = true; break; }
                TimeUnit.MILLISECONDS.sleep(20);
            }
            terminouComLockRetido = consulta.isDone();
            detentorAindaRetinha = !detentor.isDone() && liberar.getCount() == 1;
            if (terminouComLockRetido) resposta = consulta.get(1, TimeUnit.SECONDS);
        } catch (Throwable falha) {
            erro = falha;
        } finally {
            liberar.countDown();
            pool.shutdown();
            // A drenagem não pode tornar GREEN um HTTP que só terminou depois
            // de soltar o contrato externo. O resultado foi capturado acima.
            for (Future<?> tarefa : new Future<?>[] {detentor, consulta}) {
                if (tarefa == null) continue;
                try { tarefa.get(30, TimeUnit.SECONDS); }
                catch (Throwable falha) { problemasLimpeza.add(falha); }
            }
            try {
                if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                    pool.shutdownNow();
                    problemasLimpeza.add(new AssertionError("Workers não terminaram após a liberação da barreira."));
                }
            } catch (InterruptedException falha) {
                Thread.currentThread().interrupt(); pool.shutdownNow(); problemasLimpeza.add(falha);
            }
        }
        boolean terminou = terminouComLockRetido, disputou = esperaExterna, retido = detentorAindaRetinha;
        Throwable falhaCapturada = erro;
        assertAll(
                () -> assertThat(terminou || disputou)
                        .as("Preparação conclusiva: resposta ou espera física observada; timeout isolado não é RED funcional").isTrue(),
                () -> assertThat(disputou).as("GET filtrado não disputa o PRIMARY de contrato não solicitado").isFalse(),
                () -> assertThat(terminou).as("A resposta conclui enquanto o contrato externo continua bloqueado").isTrue(),
                () -> assertThat(retido).as("A barreira física não foi liberada prematuramente").isTrue(),
                () -> { if (falhaCapturada != null) throw falhaCapturada; },
                () -> assertThat(problemasLimpeza).as("Todas as transações e workers encerraram após a liberação").isEmpty());
        assertThat(resposta).isNotNull();
        assertThat(resposta.getResponse().getStatus()).as("Resposta do recorte solicitado").isEqualTo(200);
        return dados(resposta);
    }

    private void conferirResposta(boolean resumo, JsonNode resposta, int quantidade, Long vinculoId) {
        if (resumo) {
            assertThat(resposta.path("abertas").asLong()).isEqualTo(quantidade);
            assertThat(resposta.path("emTratamento").asLong()).isZero();
            assertThat(resposta.path("resolvidas").asLong()).isZero();
            assertThat(resposta.path("finsNaturaisVigencia").asLong()).isEqualTo(quantidade);
            assertThat(resposta.path("retificacoesFrequencia").asLong()).isZero();
        } else {
            assertThat(resposta.path("registros")).hasSize(quantidade);
            assertThat(resposta.path("totalElementos").asLong()).isEqualTo(quantidade);
            if (vinculoId != null) assertThat(resposta.path("registros").path(0).path("vinculoId").asLong()).isEqualTo(vinculoId);
        }
    }

    private String rota(boolean resumo, String parametros) {
        String prefixo = "/api/pendencias" + (resumo ? "/resumo" : "");
        String tipo = resumo ? "" : "tipo=FIM_NATURAL_VIGENCIA&estado=ABERTA";
        String query = parametros.isEmpty() ? tipo : parametros + (tipo.isEmpty() ? "" : "&" + tipo);
        return prefixo + (query.isEmpty() ? "" : "?" + query);
    }

    private List<PendenciaLida> pendencias(long vinculoId) {
        return emTransacao(() -> em.createQuery("select p from PendenciaAdministrativa p where p.vinculoId=:id "
                        + "and p.tipo=:tipo order by p.id", PendenciaAdministrativa.class)
                .setParameter("id", vinculoId).setParameter("tipo", TipoPendencia.FIM_NATURAL_VIGENCIA)
                .getResultList().stream().map(p -> new PendenciaLida(p.getId(), p.getChaveOrigem(), p.getEstado(),
                        p.getDataEfetiva(), p.getSurgidaEm(), p.getGestaoLocalId(), p.getSuperintendenciaId(),
                        p.getIniciadaEm(), p.getResolvidaEm(), p.getVersion())).toList());
    }

    private FonteVinculo fonte(long vinculoId) {
        return emTransacao(() -> {
            var v = em.find(VinculoContratual.class, vinculoId);
            var eventos = em.createQuery("select e from EventoVinculoContratual e where e.vinculo.id=:id order by e.id",
                            EventoVinculoContratual.class).setParameter("id", vinculoId).getResultList().stream()
                    .map(e -> new EventoLido(e.getId(), e.getTipoEvento().name(), e.getDataEfetiva(), e.getRegistradoEm(),
                            e.getResponsavelId(), e.getHashAnterior(), e.getHashEvento(), e.getFimNovo())).toList();
            return new FonteVinculo(v.getId(), v.getDataInicio(), v.getDataFim(), v.getVersion(), eventos);
        });
    }

    private record Aberto(NovoContrato contrato, long vinculoId) { }
    private record Preparado(Cenario c, Aberto externo, Aberto consultado, LocalDate hoje) { }
    private record PendenciaLida(Long id, String chave, EstadoPendencia estado, LocalDate data, Instant surgida,
                                Long gestao, Long se, Instant iniciada, Instant resolvida, long versao) { }
    private record EventoLido(Long id, String tipo, LocalDate data, Instant registrado, Long autor,
                             String hashAnterior, String hashEvento, LocalDate fimNovo) { }
    private record FonteVinculo(Long id, LocalDate inicio, LocalDate fim, long versao, List<EventoLido> eventos) { }

    private <T> T tx(Callable<T> tarefa) {
        return emTransacao(() -> {
            try { return tarefa.call(); }
            catch (Exception falha) { throw new IllegalStateException(falha); }
        });
    }
}
