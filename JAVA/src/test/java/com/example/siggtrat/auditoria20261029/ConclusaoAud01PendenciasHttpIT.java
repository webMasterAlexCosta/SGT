package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.infra.EsperaJdbcPostgres;
import com.example.siggtrat.infra.ObservadorAtorPostgres.Ator;
import org.springframework.jdbc.core.JdbcTemplate;

import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.example.siggtrat.dominio.pendencia.EstadoPendencia;
import com.example.siggtrat.dominio.pendencia.EscopoBloqueioPendencia;
import com.example.siggtrat.dominio.pendencia.TipoPendencia;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.servico.ComposicaoContratualTemporalServico;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** AUD-01: a decisão humana não substitui a remoção comprovada da causa negocial. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ConclusaoAud01PendenciasHttpIT extends ContratosHttpBase {
    @org.springframework.beans.factory.annotation.Autowired private javax.sql.DataSource dataSource;
    @Autowired private ComposicaoContratualTemporalServico composicaoTemporal;

    private record Conflito(Cenario c, NovoContrato contrato, long remanejamentoId,
                            long eventoId, long pendenciaId, LocalDate dataPosterior) {}

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void iniciarTratamentoNaoResolveConflitoDeCapacidadeAindaPresente(boolean futuro) throws Exception {
        Conflito f = prepararConflito(futuro);
        iniciar(f);
        assertAll(
                () -> enviar(f.c.localA, pendencia(f) + "/confirmar-resolucao", mapa("observacao", "Causa ainda não corrigida"),
                        409, "AUD-01 a causa atual ou programada impede resolução apenas administrativa"),
                () -> conferirPendente(f, EstadoPendencia.EM_TRATAMENTO),
                () -> assertThat(conflitoPermanece(f)).isTrue());
    }

    @Test
    void conflitoAbertoExigeTratamentoEContinuaPersistido() throws Exception {
        Conflito f = prepararConflito(false);
        enviar(f.c.localA, pendencia(f) + "/confirmar-resolucao", mapa("observacao", "Sem tratamento"),
                409, "AUD-006 conflito aberto não pode ser resolvido por confirmação direta");
        conferirPendente(f, EstadoPendencia.ABERTA);
    }

    @Test
    void cancelamentoDoFatoFuturoPermiteResolverSemApagarAOrigem() throws Exception {
        Conflito f = prepararConflito(true);
        iniciar(f);
        enviar(f.c.localA, base(f.contrato.id()) + "/remanejamentos/" + f.remanejamentoId + "/cancelar",
                mapa("dataAto", RelogioSistema.hoje(), "motivo", "Cancelamento documental da redistribuição incompatível",
                        "referenciaSei", "SEI-AUD01-CANCELAMENTO"), 200, "TMP-009 tratamento explícito da origem futura");
        assertThat(conflitoPermanece(f)).isFalse();
        resolverEConferir(f);
        emTransacao(() -> {
            EventoCicloVidaContrato original = em.find(EventoCicloVidaContrato.class, f.eventoId);
            assertThat(original).isNotNull();
            assertThat(original.getTipoEvento()).isEqualTo(TipoEventoCicloVidaContrato.REMANEJAMENTO);
            assertThat(original.getSituacaoFato()).isEqualTo(SituacaoFatoNegocial.CANCELADO);
            return null;
        });
    }

    @Test
    void retificacaoDoFatoPosteriorPermiteResolverComAutoriaESemApagarOriginal() throws Exception {
        Conflito f = prepararConflito(false);
        iniciar(f);
        Map<String, Object> req = remanejamento(f.contrato, "REM-AUD01", f.dataPosterior, 10);
        req.put("eventoPrecedenteId", f.eventoId);
        req.put("relacaoPrecedencia", "RETIFICA");
        confirmarPrevia(f.c, base(f.contrato.id()) + "/remanejamentos/previa", req);
        enviar(f.c.localA, base(f.contrato.id()) + "/remanejamentos/" + f.remanejamentoId + "/retificar",
                req, 200, "ADI-008 correção do delta incompatível preserva a origem");
        assertThat(conflitoPermanece(f)).isFalse();
        resolverEConferir(f);
        emTransacao(() -> {
            EventoCicloVidaContrato original = em.find(EventoCicloVidaContrato.class, f.eventoId);
            assertThat(original).isNotNull();
            assertThat(original.getSituacaoFato()).isEqualTo(SituacaoFatoNegocial.RETIFICADO);
            return null;
        });
    }

    @Test
    void gestorDeOutraGestaoEPerfilEstadualNaoResolvemConflito() throws Exception {
        Conflito f = prepararConflito(false);
        iniciar(f);
        enviar(f.c.localB, pendencia(f) + "/confirmar-resolucao", mapa("observacao", "Fora do escopo"),
                404, "AUD-006 Gestão alheia não resolve pendência");
        enviar(f.c.estadualA, pendencia(f) + "/confirmar-resolucao", mapa("observacao", "Perfil somente leitura"),
                403, "AUD-006 consulta estadual não concede tratamento negocial");
        conferirPendente(f, EstadoPendencia.EM_TRATAMENTO);
    }

    @Test
    void origemDeConflitoAusenteNaoEquivaleACausaResolvida() throws Exception {
        Cenario c = criarCenarioPersistido();
        long id = emTransacao(() -> {
            PendenciaAdministrativa p = new PendenciaAdministrativa();
            p.setChaveOrigem("AUD01-ORIGEM-AUSENTE:" + UUID.randomUUID());
            p.setTipo(TipoPendencia.CONFLITO_REPLAY_TEMPORAL);
            p.setEstado(EstadoPendencia.ABERTA);
            p.setMotivo("Fonte histórica de conflito ausente na recuperação sintética");
            p.setDataEfetiva(DATA); p.setOperacaoOrigem("REMANEJAMENTO");
            p.setEscopoBloqueio(EscopoBloqueioPendencia.CONTRATO_TEMPORAL);
            p.setContratoId(c.contratoA.getId()); p.setGestaoLocalId(c.gestaoA.getId());
            p.setSuperintendenciaId(c.seA.getId()); p.setOrigemEntidade("EventoCicloVidaContrato");
            p.setOrigemId(Long.MAX_VALUE); em.persist(p); em.flush(); return p.getId();
        });
        enviar(c.localA, "/api/pendencias/" + id + "/iniciar-tratamento", mapa("observacao", "Buscar documento original"),
                200, "AUD-006 análise da pendência com origem ainda desconhecida");
        assertAll(
                () -> enviar(c.localA, "/api/pendencias/" + id + "/confirmar-resolucao", mapa("observacao", "Sem fonte"),
                        409, "AUD-01 não inventar resolução por falta da origem"),
                () -> assertThat(emTransacao(() -> em.find(PendenciaAdministrativa.class, id).getEstado()))
                        .isEqualTo(EstadoPendencia.EM_TRATAMENTO));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void origemDeConflitoIncoerenteNaoEquivaleACausaResolvida(boolean entidadeDesconhecida) throws Exception {
        Conflito f = prepararConflito(false);
        long id = emTransacao(() -> {
            PendenciaAdministrativa p = new PendenciaAdministrativa();
            p.setChaveOrigem("AUD01-ORIGEM-INCOERENTE:" + UUID.randomUUID());
            p.setTipo(TipoPendencia.CONFLITO_REPLAY_TEMPORAL); p.setEstado(EstadoPendencia.ABERTA);
            p.setMotivo("Fonte sintética com referência de origem incoerente");
            p.setDataEfetiva(f.dataPosterior); p.setOperacaoOrigem("REMANEJAMENTO");
            p.setEscopoBloqueio(EscopoBloqueioPendencia.CONTRATO_TEMPORAL);
            p.setContratoId(entidadeDesconhecida ? f.contrato.id() : f.c.contratoA.getId());
            p.setGestaoLocalId(f.c.gestaoA.getId()); p.setSuperintendenciaId(f.c.seA.getId());
            p.setOrigemEntidade(entidadeDesconhecida ? "OrigemHistoricaDesconhecida" : "EventoCicloVidaContrato");
            p.setOrigemId(f.eventoId); em.persist(p); em.flush(); return p.getId();
        });
        enviar(f.c.localA, "/api/pendencias/" + id + "/iniciar-tratamento", mapa("observacao", "Conferir fonte"),
                200, "AUD-006 análise da fonte incoerente");
        assertAll(
                () -> enviar(f.c.localA, "/api/pendencias/" + id + "/confirmar-resolucao", mapa("observacao", "Sem prova válida"),
                        409, "AUD-01 entidade desconhecida ou evento de outro contrato não comprova resolução"),
                () -> assertThat(emTransacao(() -> em.find(PendenciaAdministrativa.class, id).getEstado()))
                        .isEqualTo(EstadoPendencia.EM_TRATAMENTO));
    }

    @Test
    @Timeout(value = 45, unit = TimeUnit.SECONDS)
    void confirmacaoDisputaLockRealDoContratoAntesDeValidarReplay() throws Exception {
        Conflito f = prepararConflito(false);
        iniciar(f);
        var requisicao = autenticar(post(pendencia(f) + "/confirmar-resolucao").contentType(APPLICATION_JSON)
                .content(json(mapa("observacao", "Conferência concorrente da causa"))), f.c.localA);
        CompletableFuture<Long> contratoBloqueado = new CompletableFuture<>();
        CountDownLatch liberarContrato = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> alteracao = pool.submit(() -> emTransacao(() -> {
                em.find(Contrato.class, f.contrato.id(), LockModeType.PESSIMISTIC_WRITE);
                contratoBloqueado.complete(((Number) em.createNativeQuery("select pg_backend_pid()")
                        .getSingleResult()).longValue());
                try {
                    if (!liberarContrato.await(20, TimeUnit.SECONDS))
                        throw new AssertionError("Confirmação não chegou ao lock contratual observável.");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); throw new IllegalStateException(e);
                }
                return null;
            }));
            long conexao = contratoBloqueado.get(10, TimeUnit.SECONDS);
            var atorConfirmacao = new Ator("confirmacao-AUD004");
            var confirmacao = pool.submit(() -> atorConfirmacao.executar(() -> mvc.perform(requisicao).andReturn()));
            var espera = EsperaJdbcPostgres.aguardar(new JdbcTemplate(dataSource), conexao, atorConfirmacao,
                    sql -> sql.selecaoChave("contrato", "id_contrato", f.contrato.id()),
                    "AUD004 confirmação/Contrato " + f.contrato.id(), confirmacao);
            boolean bloqueioContratual = !espera.isEmpty();
            assertThat(confirmacao.isDone()).as("O ator identificado ainda aguarda o Contrato pelo binding de sua PK").isFalse();
            liberarContrato.countDown();
            alteracao.get(10, TimeUnit.SECONDS);
            var resposta = confirmacao.get(10, TimeUnit.SECONDS);
            boolean lockObservado = bloqueioContratual;
            assertAll(
                    () -> assertThat(lockObservado).as("A resolução deve serializar com os fatos do mesmo contrato").isTrue(),
                    () -> assertThat(resposta.getResponse().getStatus())
                            .as("Causa continua presente: %s", resposta.getResponse().getContentAsString()).isEqualTo(409),
                    () -> conferirPendente(f, EstadoPendencia.EM_TRATAMENTO));
        } finally {
            liberarContrato.countDown(); pool.shutdownNow();
        }
    }

    private Conflito prepararConflito(boolean futuro) throws Exception {
        Cenario c = criarCenarioPersistido();
        LocalDate inicio = RelogioSistema.hoje().minusDays(30), fim = RelogioSistema.hoje().plusDays(120);
        long cargo2 = emTransacao(() -> {
            Cargo cargo = new Cargo("Cargo secundário AUD01 " + UUID.randomUUID(), "2");
            cargo.setGestaoLocal(em.find(GestaoLocal.class, c.gestaoA.getId())); em.persist(cargo); em.flush();
            EventoDisponibilidadeCatalogo origem = new EventoDisponibilidadeCatalogo();
            origem.setTipoCatalogo(TipoCatalogoTemporal.CARGO); origem.setCatalogoId(cargo.getId());
            origem.setDataEfetiva(INICIO); origem.setAtivo(true);
            origem.setMotivo("Fonte sintética explícita de disponibilidade do Cargo para AUD01"); em.persist(origem);
            return cargo.getId();
        });
        long contrato = enviar(c.localA, "/api/contratos", rascunho(c, "AUD01-" + UUID.randomUUID().toString().substring(0, 8),
                100, inicio, fim), 201, "Fixture AUD01: contrato publicado isolado").path("id").asLong();
        long comp1 = enviar(c.localA, base(contrato) + "/composicoes", composicao(c, c.cargoA.getId(), 25),
                201, "Fixture AUD01: primeira capacidade 25").path("id").asLong();
        long comp2 = enviar(c.localA, base(contrato) + "/composicoes", composicao(c, cargo2, 75),
                201, "Fixture AUD01: segunda capacidade 75").path("id").asLong();
        var publicar = mapa("dataPublicacao", RelogioSistema.hoje(), "referenciaSei", "SEI-AUD01-ORIGINAL",
                "justificativa", "Original documental sintético");
        confirmarPrevia(c, base(contrato) + "/previas/publicar", publicar);
        enviar(c.localA, base(contrato) + "/publicar", publicar, 200, "Fixture AUD01: publicação commitada");
        NovoContrato n = new NovoContrato(contrato, comp1, comp2, inicio, fim);
        LocalDate dataPosterior = futuro ? RelogioSistema.hoje().plusDays(10) : inicio.plusDays(5);
        var posterior = remanejamento(n, "REM-AUD01", dataPosterior, 20);
        confirmarPrevia(c, base(contrato) + "/remanejamentos/previa", posterior);
        JsonNode rem = enviar(c.localA, base(contrato) + "/remanejamentos", posterior, 201,
                "Fixture AUD01: remanejamento posterior inicialmente válido");
        var anterior = aditamento("ADI-AUD01", inicio.plusDays(2));
        anterior.put("novoEfetivoTotal", 90);
        anterior.put("alteracoesComposicao", List.of(mapa("composicaoId", comp1, "quantidadeResultante", 15)));
        confirmarPrevia(c, base(contrato) + "/aditamentos/previa", anterior);
        enviar(c.localA, base(contrato) + "/aditamentos", anterior, 201,
                "TMP-009 origem retroativa faz o remanejamento consumir 20 de capacidade 15");
        long eventoId = rem.path("eventoId").asLong();
        long pendenciaId = emTransacao(() -> em.createQuery("select p.id from PendenciaAdministrativa p "
                        + "where p.contratoId=:c and p.origemId=:e and p.tipo=:tipo", Long.class)
                .setParameter("c", contrato).setParameter("e", eventoId)
                .setParameter("tipo", TipoPendencia.CONFLITO_REPLAY_TEMPORAL).getSingleResult());
        Conflito f = new Conflito(c, n, rem.path("id").asLong(), eventoId, pendenciaId, dataPosterior);
        assertThat(conflitoPermanece(f)).isTrue();
        return f;
    }

    private void confirmarPrevia(Cenario c, String path, Map<String, Object> req) throws Exception {
        JsonNode p = enviar(c.localA, path, req, 200, "Prévia da operação negocial AUD01");
        req.put("tokenPrevia", p.path("tokenPrevia").asText()); req.put("confirmarImpacto", true);
    }

    private String pendencia(Conflito f) { return "/api/pendencias/" + f.pendenciaId; }

    private void iniciar(Conflito f) throws Exception {
        enviar(f.c.localA, pendencia(f) + "/iniciar-tratamento", mapa("observacao", "Em análise documental"),
                200, "AUD-006 início do tratamento não elimina a incompatibilidade");
        conferirPendente(f, EstadoPendencia.EM_TRATAMENTO);
    }

    private boolean conflitoPermanece(Conflito f) {
        return emTransacao(() -> composicaoTemporal.diagnosticarConflitos(
                        em.find(Contrato.class, f.contrato.id()), LocalDate.MAX, Instant.now()).stream()
                .anyMatch(c -> c.eventoId().equals(f.eventoId)));
    }

    private void conferirPendente(Conflito f, EstadoPendencia estado) {
        emTransacao(() -> {
            PendenciaAdministrativa p = em.find(PendenciaAdministrativa.class, f.pendenciaId);
            assertThat(p.getEstado()).isEqualTo(estado);
            assertThat(p.getOrigemId()).isEqualTo(f.eventoId);
            assertThat(p.getResolvidaEm()).isNull(); assertThat(p.getResolvidaPorId()).isNull();
            return null;
        });
    }

    private void resolverEConferir(Conflito f) throws Exception {
        enviar(f.c.localA, pendencia(f) + "/confirmar-resolucao", mapa("observacao", "Causa corrigida e conferida"),
                200, "AUD-01 resolução exige causa eliminada por fato auditado");
        Instant resolvidaEm = emTransacao(() -> {
            PendenciaAdministrativa p = em.find(PendenciaAdministrativa.class, f.pendenciaId);
            assertThat(p.getEstado()).isEqualTo(EstadoPendencia.RESOLVIDA);
            assertThat(p.getOrigemId()).isEqualTo(f.eventoId);
            assertThat(p.getResolvidaPorId()).isEqualTo(f.c.localA.getId());
            assertThat(p.getObservacaoTratamento()).isEqualTo("Causa corrigida e conferida");
            assertThat(p.getResolvidaEm()).isNotNull(); return p.getResolvidaEm();
        });
        enviar(f.c.localA, pendencia(f) + "/confirmar-resolucao", mapa("observacao", "Reenvio de rede"),
                200, "AUD-005 reenvio não altera autoria/data da resolução já persistida");
        assertThat(emTransacao(() -> em.find(PendenciaAdministrativa.class, f.pendenciaId).getResolvidaEm()))
                .isEqualTo(resolvidaEm);
    }
}
