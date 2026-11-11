package com.example.siggtrat.auditoria20261029;

import com.fasterxml.jackson.databind.JsonNode;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * FRE-008/REL-006: um job criado durante uma transação de fonte também deve
 * reconhecer o commit dessa fonte. A prova não lê a geração ou sua tabela.
 * As barreiras só coordenam UPDATE real de frequência e SELECT real do worker.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class PreauditoriaPainelNovoDuranteRetificacaoHttpIT extends ContratosHttpBase {
    @DynamicPropertySource
    static void barreirasSql(DynamicPropertyRegistry props) {
        props.add("spring.jpa.properties.hibernate.session_factory.statement_inspector", BarreirasSql.class::getName);
    }

    private record Espera(CompletableFuture<Void> alcancou, CountDownLatch liberar, AtomicBoolean usada) {
        static Espera nova() { return new Espera(new CompletableFuture<>(), new CountDownLatch(1), new AtomicBoolean()); }
        void aguardarLiberacao() {
            alcancou.complete(null);
            try {
                if (!liberar.await(35, TimeUnit.SECONDS)) throw new AssertionError("Barreira SQL não foi liberada a tempo.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
    }

    public static class BarreirasSql implements StatementInspector {
        private static final ThreadLocal<Espera> escritor = new ThreadLocal<>();
        private static final AtomicReference<Espera> conclusao = new AtomicReference<>();

        @Override public String inspect(String sql) {
            String normal = sql.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
            Espera fonte = escritor.get();
            if (fonte != null && normal.matches(".*\\bupdate\\s+frequencia\\b.*")
                    && fonte.usada().compareAndSet(false, true)) {
                assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
                // O hook é instalado por SQL da própria fonte. beforeCommit
                // espera somente após os demais comandos transacionais terem
                // retornado, inclusive eventual registro técnico de invalidação.
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override public void beforeCommit(boolean readOnly) { fonte.aguardarLiberacao(); }
                });
            }

            Espera publicacao = conclusao.get();
            boolean finalizando = publicacao != null
                    && Thread.currentThread().getName().startsWith("painel-calculo-")
                    && Arrays.stream(Thread.currentThread().getStackTrace()).anyMatch(frame ->
                    frame.getClassName().equals("com.example.siggtrat.servico.PainelCalculoEstadoServico")
                            && frame.getMethodName().equals("concluir"));
            // PESSIMISTIC_WRITE no PostgreSQL atual usa FOR NO KEY UPDATE.
            if (finalizando && normal.contains(" from painel_calculo ")
                    && (normal.contains("for update") || normal.contains("for no key update"))
                    && publicacao.usada().compareAndSet(false, true)) {
                publicacao.aguardarLiberacao();
            }
            return sql;
        }
    }

    @Test
    @Timeout(value = 85, unit = TimeUnit.SECONDS)
    void primeiroCalculoCriadoAntesDoCommitDaFonteNaoPublicaResultadoAntigoComoAtual() throws Exception {
        Cenario c = criarCenarioPersistido();
        registrar(c, "FALTA");
        JsonNode original = painelAtual(c);
        assertEquals(1, faltas(original), "Controle: fonte original tem uma falta; ainda não foi solicitado job deste escopo");

        Espera fonte = Espera.nova(), publicacao = Espera.nova();
        assertTrue(BarreirasSql.conclusao.compareAndSet(null, publicacao));
        ExecutorService executor = Executors.newSingleThreadExecutor();
        long id;
        try {
            var retificacao = executor.submit(() -> {
                BarreirasSql.escritor.set(fonte);
                try { return registrar(c, "PRESENCA"); }
                finally { BarreirasSql.escritor.remove(); }
            });
            fonte.alcancou().get(15, TimeUnit.SECONDS);
            assertFalse(retificacao.isDone(), "A fonte precisa permanecer sem commit durante a criação do primeiro job");

            JsonNode criado = dados(executar(c.localA, post("/api/painel/calculos")
                    .param("referencia", DATA.toString()).param("periodoDias", "7"), 202,
                    "Primeiro cálculo nasce depois das mutações SQL e antes do commit da fonte"));
            id = criado.path("id").asLong();
            assertTrue(id > 0);
            publicacao.alcancou().get(15, TimeUnit.SECONDS);
            assertFalse(retificacao.isDone(), "O worker calculou a fonte antiga enquanto a transação estava suspensa");

            fonte.liberar().countDown();
            retificacao.get(15, TimeUnit.SECONDS);
            assertEquals(0, faltas(painelAtual(c)), "Controle: o commit já tornou a retificação visível por HTTP");
        } finally {
            fonte.liberar().countDown();
            publicacao.liberar().countDown();
            BarreirasSql.conclusao.compareAndSet(publicacao, null);
            executor.shutdownNow();
        }

        JsonNode publicado = aguardarFimDaTentativa(c, id);
        assertTrue(atualOuExplicitamenteSuperado(publicado),
                "Job novo escapou da alteração que fez commit durante seu cálculo: " + publicado);
        MvcResult resposta = mvc.perform(autenticar(post("/api/painel/calculos")
                .param("referencia", DATA.toString()).param("periodoDias", "7"), c.localA)).andReturn();
        assertTrue(resposta.getResponse().getStatus() == 200 || resposta.getResponse().getStatus() == 202,
                resposta.getResponse().getContentAsString());
        JsonNode renovacao = dados(resposta);
        assertTrue(atualOuExplicitamenteSuperado(renovacao), renovacao.toString());
        JsonNode atual = aguardarAtual(c, renovacao.path("id").asLong());
        assertEquals(0, faltas(atual.path("dados")));
        assertFalse(atual.path("desatualizado").asBoolean());
    }

    private JsonNode aguardarFimDaTentativa(Cenario c, long id) throws Exception {
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
        JsonNode estado;
        do {
            estado = consultar(c.localA, "/api/painel/calculos/" + id, "Aguardar término da tentativa original");
            assertNotEquals("ERRO", estado.path("status").asText(), estado.toString());
            if (!"PROCESSANDO".equals(estado.path("status").asText())) return estado;
            Thread.sleep(75);
        } while (System.nanoTime() < limite);
        return fail("Tentativa permaneceu PROCESSANDO após ambas as barreiras serem liberadas: " + estado);
    }

    private JsonNode aguardarAtual(Cenario c, long id) throws Exception {
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        JsonNode estado;
        do {
            estado = consultar(c.localA, "/api/painel/calculos/" + id, "Renovação deve produzir a fonte já confirmada");
            assertNotEquals("ERRO", estado.path("status").asText(), estado.toString());
            assertTrue(atualOuExplicitamenteSuperado(estado), estado.toString());
            if (estado.path("concluido").asBoolean() && !estado.path("desatualizado").asBoolean()
                    && faltas(estado.path("dados")) == 0) return estado;
            Thread.sleep(75);
        } while (System.nanoTime() < limite);
        return fail("Renovação não produziu a verdade atual: " + estado);
    }

    private JsonNode painelAtual(Cenario c) throws Exception {
        return dados(executar(c.localA, get("/api/painel/gestao")
                .param("referencia", DATA.toString()).param("periodoDias", "7"), 200,
                "Controle externo do indicador calculado com a fonte confirmada"));
    }

    private JsonNode registrar(Cenario c, String status) throws Exception {
        return enviar(c.localA, "/api/frequencias", mapa("vinculoContratualId", c.vinculoA.getId(),
                "dataReferencia", DATA, "status", status,
                "justificativaRetificacao", "Conferência documental concorrente antes do primeiro painel",
                "chaveIdempotencia", "preaud-painel-novo-" + UUID.randomUUID()), 200,
                "Frequência por API publica status " + status);
    }

    private boolean atualOuExplicitamenteSuperado(JsonNode resultado) {
        return resultado.path("dados").isNull() || resultado.path("dados").isMissingNode()
                || faltas(resultado.path("dados")) == 0 || resultado.path("desatualizado").asBoolean();
    }

    private long faltas(JsonNode painel) { return painel.path("periodo").path("faltasNaoJustificadas").asLong(-1); }
}
