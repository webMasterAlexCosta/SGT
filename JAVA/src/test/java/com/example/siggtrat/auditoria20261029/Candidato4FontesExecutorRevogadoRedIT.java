package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * Revisão independente C4-TR-H03. Quatro caminhos públicos distintos da frequência diária:
 * criação de adicional, exceção, ocorrência e cancelamento de ocorrência.
 * Hipótese: ator carregado antes do lock ou conservado no contexto JPA; ocorrência
 * lê depois do lock e é controle da suficiência dessa ordem. Nenhum resultado presumido.
 * Manifesto C4: 13c84fd30b668335228704fd1028ddea11adad6f607458a7b7fa993dcbc64a89.
 * Proposta não compilada/executada pelo revisor; sem DDL ou mocks de persistência/autorização.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class Candidato4FontesExecutorRevogadoRedIT extends ContratosHttpBase {
    @Autowired private DataSource dataSource;

    @ParameterizedTest
    @ValueSource(strings = {"ADICIONAL", "EXCECAO", "OCORRENCIA", "CANCELAR_OCORRENCIA"})
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void fonteManualRevalidaExecutorAposEsperaDaRaiz(String fluxo) throws Exception {
        Cenario c = criarCenarioPersistido();
        var positivo = dados(executar(c.localA, criar(fluxo, c, DATA.minusDays(1)), 201,
                "Controle positivo de " + fluxo + " antes da desativação"));
        long criado = positivo.path("id").asLong();
        assertThat(criado).isPositive();
        Estado antes = estado(c.empregadoA.getId());
        MockHttpServletRequestBuilder comando = fluxo.equals("CANCELAR_OCORRENCIA")
                ? delete("/api/ocorrencias/" + criado) : criar(fluxo, c, DATA);
        var pedido = autenticar(comando, c.localA);
        var pool = Executors.newFixedThreadPool(2);
        CountDownLatch liberar = new CountDownLatch(1);
        CompletableFuture<Long> conexaoBloqueadora = new CompletableFuture<>();
        try {
            Future<?> bloqueador = pool.submit(() -> emTransacao(() -> {
                em.find(Contrato.class, c.contratoA.getId(), LockModeType.PESSIMISTIC_WRITE);
                conexaoBloqueadora.complete(((Number) em.createNativeQuery("select connection_id()")
                        .getSingleResult()).longValue());
                try {
                    if (!liberar.await(30, TimeUnit.SECONDS)) throw new AssertionError("Limite da fixture: raiz não liberada");
                } catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new IllegalStateException(ex); }
                return null;
            }));
            long conexao = conexaoBloqueadora.get(10, TimeUnit.SECONDS);
            Future<MvcResult> tentativa = pool.submit(() -> mvc.perform(pedido).andReturn());
            aguardarBloqueioReal(conexao, tentativa);
            executar(c.admin, put("/api/usuarios/" + c.localA.getId()).contentType(APPLICATION_JSON)
                    .content(json(mapa("active", false))), 200,
                    "Desativação administrativa confirmada antes de liberar " + fluxo);
            emTransacao(() -> {
                assertThat(em.find(UsuarioAplicacao.class, c.localA.getId()).getActive()).isFalse(); return null;
            });
            assertThat(tentativa.isDone()).isFalse();
            liberar.countDown(); bloqueador.get(10, TimeUnit.SECONDS);
            MvcResult resultado = tentativa.get(15, TimeUnit.SECONDS);
            Estado depois = estado(c.empregadoA.getId());
            Path evidencia = Path.of("target/auditoria-http/candidato4-fontes-executor-" + fluxo + ".json");
            Files.createDirectories(evidencia.getParent());
            Files.writeString(evidencia, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapa(
                    "hipotese", "C4-TR-H03", "fluxo", fluxo, "esperaMySqlObservada", true,
                    "revogacaoCommitadaAntesDaLiberacao", true, "status", resultado.getResponse().getStatus(),
                    "antes", antes, "depois", depois)));
            assertAll("A autorização atual deve sobreviver à espera do contexto negocial",
                    () -> assertThat(resultado.getResponse().getStatus()).isIn(401, 403, 404, 409),
                    () -> assertThat(depois).as("Sem criação, cancelamento, versão ou histórico novo").isEqualTo(antes));
        } finally { liberar.countDown(); pool.shutdownNow(); pool.awaitTermination(10, TimeUnit.SECONDS); }
    }

    private MockHttpServletRequestBuilder criar(String fluxo, Cenario c, LocalDate dia) throws Exception {
        if (fluxo.equals("ADICIONAL")) return post("/api/frequencias-adicionais").contentType(APPLICATION_JSON)
                .content(json(mapa("empregadoId", c.empregadoA.getId(), "dataReferencia", dia,
                        "tipo", "RT", "minutos", 30, "quantidade", 1,
                        "justificativa", "Fonte sintética para revisão de autorização")));
        if (fluxo.equals("EXCECAO")) return post("/api/frequencias-excecoes").contentType(APPLICATION_JSON)
                .content(json(mapa("empregadoId", c.empregadoA.getId(), "dataInicio", dia, "dataFim", dia,
                        "tipo", "FOLGA_REMUNERADA", "justificativa", "Fonte sintética para revisão de autorização")));
        return multipart("/api/ocorrencias").param("empregadoId", c.empregadoA.getId().toString())
                .param("motivo", "ATESTADO").param("dataOcorrencia", dia.toString()).param("dias", "1")
                .param("chaveIdempotencia", "c4-fonte-" + UUID.randomUUID());
    }

    private Estado estado(long empregadoId) {
        return emTransacao(() -> new Estado(
                contar("FrequenciaAdicional", empregadoId), contar("FrequenciaExcecao", empregadoId),
                contar("Ocorrencia", empregadoId), contar("HistoricoEmpregado", empregadoId),
                em.createQuery("select count(o) from Ocorrencia o where o.empregado.id=:e and o.canceladoEm is not null", Long.class)
                        .setParameter("e", empregadoId).getSingleResult(),
                em.createQuery("select count(v) from VersaoComplementoFrequencia v where "
                        + "(v.tipoComplemento='ADICIONAL' and v.complementoId in (select a.id from FrequenciaAdicional a where a.empregado.id=:e)) "
                        + "or (v.tipoComplemento='EXCECAO' and v.complementoId in (select x.id from FrequenciaExcecao x where x.empregado.id=:e))", Long.class)
                        .setParameter("e", empregadoId).getSingleResult()));
    }
    private long contar(String entidade, long empregadoId) {
        return em.createQuery("select count(x) from " + entidade + " x where x.empregado.id=:e", Long.class)
                .setParameter("e", empregadoId).getSingleResult();
    }
    private void aguardarBloqueioReal(long conexaoBloqueadora, Future<?> tentativa) throws Exception {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        CompletableFuture<Void> observada = new CompletableFuture<>();
        var observador = Executors.newSingleThreadScheduledExecutor();
        var consulta = observador.scheduleWithFixedDelay(() -> {
            try {
                Long esperas = jdbc.queryForObject("select count(*) from performance_schema.data_lock_waits w "
                        + "join performance_schema.threads t on t.THREAD_ID=w.BLOCKING_THREAD_ID "
                        + "where t.PROCESSLIST_ID=?", Long.class, conexaoBloqueadora);
                if (esperas != null && esperas > 0) observada.complete(null);
                else if (tentativa.isDone()) observada.completeExceptionally(new AssertionError("Fonte não aguardou Contrato"));
            } catch (Throwable ex) { observada.completeExceptionally(ex); }
        }, 0, 25, TimeUnit.MILLISECONDS);
        try { observada.get(10, TimeUnit.SECONDS); }
        finally { consulta.cancel(true); observador.shutdownNow(); observador.awaitTermination(5, TimeUnit.SECONDS); }
    }
    private record Estado(long adicionais, long excecoes, long ocorrencias, long historicos, long canceladas, long versoes) { }
}
