package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.servico.DisponibilidadeCatalogoTemporalServico;
import com.example.siggtrat.servico.ProgramacaoFuturaCatalogoServico;
import com.example.siggtrat.utilitario.RelogioSistema;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.DayOfWeek;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * CAT005/008: uso e alteração administrativa compartilham o bloqueio até commit.
 * Spies chamam métodos reais e somente ordenam as transações. Regras, consultas,
 * JPA, locks e MVC permanecem reais. A prova RED original fica em novos_red_04;
 * as duas ordens válidas agora possuem oráculos de estado e rollback próprios.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CatalogoPublicacaoConcorrenteAuditoriaIndependenteHttpIT extends ContratosHttpBase {
    @MockitoSpyBean ProgramacaoFuturaCatalogoServico programacao;
    @MockitoSpyBean DisponibilidadeCatalogoTemporalServico disponibilidade;

    @ParameterizedTest
    @EnumSource(value = TipoCatalogoTemporal.class, names = {"CARGO", "LOTACAO", "JORNADA"})
    @Timeout(value = 45, unit = TimeUnit.SECONDS)
    void publicacaoConfirmadaImpedeInativacaoQueAguardouCatalogo(TipoCatalogoTemporal tipo) throws Exception {
        Corrida f = prepararCorrida(tipo);
        CountDownLatch liberar = new CountDownLatch(1);
        CompletableFuture<Long> conexao = new CompletableFuture<>();
        AtomicBoolean interceptou = new AtomicBoolean();
        doAnswer(invocacao -> {
            Object resposta = invocacao.callRealMethod();
            if (interceptou.compareAndSet(false, true)) {
                conexao.complete(conexaoAtual()); aguardarLiberacao(liberar);
            }
            return resposta;
        }).when(disponibilidade).ativoEm(eq(tipo), eq(f.item().id()), eq(f.inicio()), eq(Boolean.TRUE));
        var reqPublicar = autenticar(post(base(f.contratoId()) + "/publicar")
                .contentType(MediaType.APPLICATION_JSON).content(json(publicacao())), f.c().localA);
        var reqExcluir = autenticar(delete(f.item().rota()), f.c().localA);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<MvcResult> publicar = executor.submit(() -> mvc.perform(reqPublicar).andReturn());
            long bloqueador = conexao.get(15, TimeUnit.SECONDS);
            Future<MvcResult> excluir = executor.submit(() -> mvc.perform(reqExcluir).andReturn());
            aguardarEspera(bloqueador, excluir); liberar.countDown();
            conferirStatus(publicar.get(15, TimeUnit.SECONDS), 200);
            conferirStatus(excluir.get(15, TimeUnit.SECONDS), 409);
            conferirResultado(f, true, EstadoNegocialContrato.PUBLICADO, 1L, 1L);
        } finally { liberar.countDown(); executor.shutdownNow(); }
    }

    @ParameterizedTest
    @EnumSource(value = TipoCatalogoTemporal.class, names = {"CARGO", "LOTACAO", "JORNADA"})
    @Timeout(value = 45, unit = TimeUnit.SECONDS)
    void inativacaoConfirmadaImpedePublicacaoQueAguardouCatalogo(TipoCatalogoTemporal tipo) throws Exception {
        Corrida f = prepararCorrida(tipo);
        CountDownLatch liberar = new CountDownLatch(1);
        CompletableFuture<Long> conexao = new CompletableFuture<>();
        AtomicBoolean interceptou = new AtomicBoolean();
        doAnswer(invocacao -> {
            Object resposta = invocacao.callRealMethod();
            if (interceptou.compareAndSet(false, true)) {
                conexao.complete(conexaoAtual()); aguardarLiberacao(liberar);
            }
            return resposta;
        }).when(programacao).exigirSemProgramacaoFutura(
                eq(ProgramacaoFuturaCatalogoServico.TipoCatalogo.valueOf(tipo.name())), eq(f.item().id()));
        var reqExcluir = autenticar(delete(f.item().rota()), f.c().localA);
        var reqPublicar = autenticar(post(base(f.contratoId()) + "/publicar")
                .contentType(MediaType.APPLICATION_JSON).content(json(publicacao())), f.c().localA);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<MvcResult> excluir = executor.submit(() -> mvc.perform(reqExcluir).andReturn());
            long bloqueador = conexao.get(15, TimeUnit.SECONDS);
            Future<MvcResult> publicar = executor.submit(() -> mvc.perform(reqPublicar).andReturn());
            aguardarEspera(bloqueador, publicar); liberar.countDown();
            conferirStatus(excluir.get(15, TimeUnit.SECONDS), 204);
            MvcResult recusada = publicar.get(15, TimeUnit.SECONDS);
            conferirStatus(recusada, 400);
            assertThat(recusada.getResponse().getContentAsString()).contains("disponíveis na data inicial");
            conferirResultado(f, false, EstadoNegocialContrato.RASCUNHO, 0L, 0L);
        } finally { liberar.countDown(); executor.shutdownNow(); }
    }

    @ParameterizedTest
    @EnumSource(TipoCatalogoTemporal.class)
    @Timeout(value = 45, unit = TimeUnit.SECONDS)
    void confirmacaoReleEstadoETrilhaAposInativacaoEReativacaoConcorrentes(TipoCatalogoTemporal tipo) throws Exception {
        Cenario c = criarCenarioPersistido(); Item item = criarItem(c, tipo);
        conferirLeituraDepoisDaMudanca(c, item, true, false);
        conferirLeituraDepoisDaMudanca(c, item, false, true);
        assertThat(emTransacao(() -> em.createQuery("select count(e) from EventoDisponibilidadeCatalogo e "
                        + "where e.tipoCatalogo=:tipo and e.catalogoId=:id", Long.class)
                .setParameter("tipo", tipo).setParameter("id", item.id()).getSingleResult())).isEqualTo(3L);
    }

    @Test
    @Timeout(value = 45, unit = TimeUnit.SECONDS)
    void consultaSomenteLeituraNaoAguardaEscritorDoCatalogo() throws Exception {
        Cenario c = criarCenarioPersistido(); Item item = criarItem(c, TipoCatalogoTemporal.CARGO);
        CountDownLatch liberar = new CountDownLatch(1);
        CompletableFuture<Long> bloqueador = new CompletableFuture<>();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> escrita = executor.submit(() -> emTransacao(() -> {
                em.find(Cargo.class, item.id(), LockModeType.PESSIMISTIC_WRITE);
                bloqueador.complete(conexaoAtual()); aguardarLiberacao(liberar); return null;
            }));
            bloqueador.get(10, TimeUnit.SECONDS);
            Future<Boolean> leitura = executor.submit(() -> {
                TransactionTemplate tx = new TransactionTemplate(transactionManager); tx.setReadOnly(true);
                return tx.execute(status -> disponibilidade.ativoEm(TipoCatalogoTemporal.CARGO,
                        item.id(), RelogioSistema.hoje().plusDays(1), true));
            });
            assertThat(leitura.get(5, TimeUnit.SECONDS)).isTrue();
            liberar.countDown(); escrita.get(10, TimeUnit.SECONDS);
        } finally { liberar.countDown(); executor.shutdownNow(); }
    }

    private void conferirLeituraDepoisDaMudanca(Cenario c, Item item, boolean antes, boolean depois) throws Exception {
        CountDownLatch carregado = new CountDownLatch(1), mudou = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<Boolean> uso = executor.submit(() -> emTransacao(() -> {
                Object velho = em.find(classe(item.tipo()), item.id()); Boolean flagAnterior = ativo(velho);
                assertThat(flagAnterior).isEqualTo(antes);
                // Abre também a visão MVCC antes de a outra transação alterar o catálogo.
                em.createQuery("select count(e) from EventoDisponibilidadeCatalogo e where e.tipoCatalogo=:tipo "
                        + "and e.catalogoId=:id", Long.class).setParameter("tipo", item.tipo())
                        .setParameter("id", item.id()).getSingleResult();
                carregado.countDown(); aguardarLiberacao(mudou);
                return disponibilidade.ativoEm(item.tipo(), item.id(), RelogioSistema.hoje().plusDays(2), flagAnterior);
            }));
            assertThat(carregado.await(10, TimeUnit.SECONDS)).isTrue();
            Map<String, Object> alteracao = new LinkedHashMap<>(item.corpo()); alteracao.put("ativo", depois);
            executar(c.localA, put(item.rota()).contentType(MediaType.APPLICATION_JSON).content(json(alteracao)),
                    200, "CAT006/008 alteração confirmada enquanto outro contexto conserva flag antiga");
            mudou.countDown();
            assertThat(uso.get(10, TimeUnit.SECONDS)).as("Novo uso considera o estado confirmado").isEqualTo(depois);
            assertThat(emTransacao(() -> ativo(em.find(classe(item.tipo()), item.id())))).isEqualTo(depois);
        } finally { mudou.countDown(); executor.shutdownNow(); }
    }

    private Corrida prepararCorrida(TipoCatalogoTemporal tipo) throws Exception {
        Cenario c = criarCenarioPersistido(); Item item = criarItem(c, tipo);
        long lotacao = c.lotacaoA.getId(), jornada = c.jornadaA.getId(), cargo = c.cargoA.getId();
        if (tipo == TipoCatalogoTemporal.CARGO) cargo = item.id();
        if (tipo == TipoCatalogoTemporal.LOTACAO) lotacao = item.id();
        if (tipo == TipoCatalogoTemporal.JORNADA) {
            jornada = item.id();
            lotacao = enviar(c.localA, "/api/lotacoes", mapa("nome", "Lotação da corrida " + UUID.randomUUID(),
                    "municipio", "CURITIBA", "ativo", true, "turnos", List.of(mapa("id", jornada))),
                    201, "Preparação: Lotação associada ao Turno exclusivo").path("id").asLong();
        }
        var inicio = RelogioSistema.hoje().plusDays(10);
        long contratoId = enviar(c.localA, "/api/contratos", rascunho(c,
                "RACE-" + UUID.randomUUID().toString().substring(0, 8), 10, inicio, inicio.plusDays(90)),
                201, "Preparação: contrato futuro em rascunho").path("id").asLong();
        enviar(c.localA, base(contratoId) + "/composicoes", mapa("lotacaoId", lotacao, "jornadaId", jornada,
                "cargoId", cargo, "efetivoPrevisto", 10), 201, "Preparação: composição ainda não publicada");
        return new Corrida(c, item, contratoId, inicio);
    }

    private Item criarItem(Cenario c, TipoCatalogoTemporal tipo) throws Exception {
        String recurso = switch (tipo) {
            case CARGO -> "cargos"; case LOTACAO -> "lotacoes"; case JORNADA -> "jornadas";
            case TIPO_CONTRATACAO -> "tipos-contratacao"; case MOTIVO_DESLIGAMENTO -> "motivos-desligamento";
        };
        Map<String, Object> corpo = mapa(tipo == TipoCatalogoTemporal.CARGO || tipo == TipoCatalogoTemporal.LOTACAO
                ? "nome" : "descricao", "Catálogo concorrente " + UUID.randomUUID(), "ativo", true,
                "gestaoLocalId", c.gestaoA.getId());
        if (tipo == TipoCatalogoTemporal.LOTACAO) {
            corpo.put("municipio", "CURITIBA"); corpo.put("turnos", List.of(mapa("id", c.jornadaA.getId())));
        }
        if (tipo == TipoCatalogoTemporal.JORNADA) {
            List<Map<String, Object>> dias = new ArrayList<>();
            for (DayOfWeek dia : DayOfWeek.values()) {
                Map<String, Object> d = mapa("diaSemana", dia.name(), "diaTrabalho", dia.getValue() <= 5);
                if (dia.getValue() <= 5) { d.put("entrada1", "08:00"); d.put("saida1", "16:00"); }
                dias.add(d);
            }
            corpo.put("dias", dias);
        }
        if (tipo == TipoCatalogoTemporal.TIPO_CONTRATACAO) corpo.put("matriculaVinculoObrigatoria", false);
        if (tipo == TipoCatalogoTemporal.MOTIVO_DESLIGAMENTO) corpo.put("categoria", "DESLIGAMENTO_INDIVIDUAL");
        long id = enviar(c.localA, "/api/" + recurso, corpo, tipo == TipoCatalogoTemporal.CARGO ? 200 : 201,
                "Preparação: catálogo exclusivo sem dependências publicadas").path("id").asLong();
        assertThat(id).isPositive(); return new Item(tipo, id, "/api/" + recurso + "/" + id, corpo);
    }

    private void conferirResultado(Corrida f, boolean ativoEsperado, EstadoNegocialContrato estado,
                                   long publicacoes, long versoes) {
        emTransacao(() -> {
            assertThat(ativo(em.find(classe(f.item().tipo()), f.item().id()))).isEqualTo(ativoEsperado);
            assertThat(em.find(Contrato.class, f.contratoId()).getEstadoNegocial()).isEqualTo(estado);
            assertThat(em.createQuery("select count(e) from EventoCicloVidaContrato e where e.contrato.id=:id "
                            + "and e.tipoEvento=com.example.siggtrat.entidade.TipoEventoCicloVidaContrato.PUBLICACAO", Long.class)
                    .setParameter("id", f.contratoId()).getSingleResult()).isEqualTo(publicacoes);
            assertThat(em.createQuery("select count(v) from VersaoContratoNegocial v where v.contrato.id=:id", Long.class)
                    .setParameter("id", f.contratoId()).getSingleResult()).isEqualTo(versoes);
            assertThat(em.createQuery("select count(e) from EventoDisponibilidadeCatalogo e where e.tipoCatalogo=:tipo "
                            + "and e.catalogoId=:id and e.ativo=false", Long.class)
                    .setParameter("tipo", f.item().tipo()).setParameter("id", f.item().id()).getSingleResult())
                    .isEqualTo(ativoEsperado ? 0L : 1L);
            return null;
        });
    }

    private Map<String, Object> publicacao() {
        return mapa("dataPublicacao", RelogioSistema.hoje(), "referenciaSei", "SEI-CORRIDA-CATALOGO",
                "justificativa", "Publicação concorrente documentada");
    }
    private long conexaoAtual() { return ((Number) em.createNativeQuery("select connection_id()").getSingleResult()).longValue(); }
    private void aguardarLiberacao(CountDownLatch barreira) {
        try {
            if (!barreira.await(25, TimeUnit.SECONDS)) throw new AssertionError("Barreira de teste expirou");
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
    }
    private void aguardarEspera(long conexao, Future<MvcResult> concorrente) throws Exception {
        long fim = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < fim) {
            long esperas = emTransacao(() -> ((Number) em.createNativeQuery("""
                    select count(*) from performance_schema.data_lock_waits espera
                    join performance_schema.threads bloqueador on bloqueador.THREAD_ID=espera.BLOCKING_THREAD_ID
                    where bloqueador.PROCESSLIST_ID=:conexao
                    """).setParameter("conexao", conexao).getSingleResult()).longValue());
            if (esperas > 0) return;
            if (concorrente.isDone()) {
                MvcResult r = concorrente.get();
                throw new AssertionError("Comando terminou sem aguardar o catálogo: " + r.getResponse().getStatus()
                        + " " + r.getResponse().getContentAsString());
            }
            TimeUnit.MILLISECONDS.sleep(20);
        }
        throw new AssertionError("MySQL não registrou espera na transação " + conexao);
    }
    private void conferirStatus(MvcResult resposta, int status) throws Exception {
        assertThat(resposta.getResponse().getStatus()).as(resposta.getResponse().getContentAsString()).isEqualTo(status);
    }
    private Class<?> classe(TipoCatalogoTemporal tipo) {
        return switch (tipo) {
            case CARGO -> Cargo.class; case LOTACAO -> Lotacao.class; case JORNADA -> Jornada.class;
            case TIPO_CONTRATACAO -> TipoContratacao.class; case MOTIVO_DESLIGAMENTO -> MotivoDesligamento.class;
        };
    }
    private Boolean ativo(Object item) {
        if (item instanceof Cargo e) return e.getAtivo(); if (item instanceof Lotacao e) return e.getAtivo();
        if (item instanceof Jornada e) return e.getAtivo(); if (item instanceof TipoContratacao e) return e.getAtivo();
        return ((MotivoDesligamento) item).getAtivo();
    }
    private record Item(TipoCatalogoTemporal tipo, long id, String rota, Map<String, Object> corpo) {}
    private record Corrida(Cenario c, Item item, long contratoId, java.time.LocalDate inicio) {}
}
