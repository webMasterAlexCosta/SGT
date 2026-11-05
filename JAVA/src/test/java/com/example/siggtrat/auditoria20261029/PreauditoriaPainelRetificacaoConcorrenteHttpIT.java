package com.example.siggtrat.auditoria20261029;

import com.fasterxml.jackson.databind.JsonNode;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * FRE-008/REL-006: uma consolidação já calculada não pode reaparecer como atual
 * se a fonte for retificada antes de o processador publicar seu resultado.
 * O Inspector apenas coordena SQL real; nenhum serviço, cálculo ou repositório
 * é substituído. As asserções observam exclusivamente as respostas HTTP.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class PreauditoriaPainelRetificacaoConcorrenteHttpIT extends ContratosHttpBase {
    @DynamicPropertySource
    static void barreiraSql(DynamicPropertyRegistry props) {
        props.add("spring.jpa.properties.hibernate.session_factory.statement_inspector", BarreiraConclusao.class::getName);
    }

    private record Espera(CompletableFuture<Void> alcancou, CountDownLatch liberar, AtomicBoolean usada) {}

    public static class BarreiraConclusao implements StatementInspector {
        private static final AtomicReference<Espera> ativa = new AtomicReference<>();

        @Override public String inspect(String sql) {
            Espera espera = ativa.get();
            if (espera == null || !Thread.currentThread().getName().startsWith("painel-calculo-")) return sql;
            String normal = sql.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
            boolean conclusao = Arrays.stream(Thread.currentThread().getStackTrace()).anyMatch(frame ->
                    frame.getClassName().equals("com.example.siggtrat.servico.PainelCalculoEstadoServico")
                            && frame.getMethodName().equals("concluir"));
            if (conclusao && normal.startsWith("select ") && normal.contains(" from painel_calculo ")
                    && normal.contains("for update") && espera.usada().compareAndSet(false, true)) {
                // O DTO já foi calculado e serializado. A espera ocorre ANTES
                // da execução do SELECT: não se retém o lock de publicação.
                espera.alcancou().complete(null);
                try {
                    if (!espera.liberar().await(30, TimeUnit.SECONDS))
                        throw new AssertionError("Retificação HTTP não liberou a publicação do painel a tempo.");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e);
                }
            }
            return sql;
        }
    }

    @Test
    @Timeout(value = 70, unit = TimeUnit.SECONDS)
    void resultadoCalculadoAntesDaRetificacaoNaoPodeSerPublicadoComoAtualDepoisDoCommit() throws Exception {
        Cenario c = criarCenarioPersistido();
        registrar(c, "FALTA");
        JsonNode fonteOriginal = dados(executar(c.localA, get("/api/painel/gestao")
                .param("referencia", DATA.toString()).param("periodoDias", "7"), 200,
                "Controle: a verdade original contém uma falta antes da corrida"));
        assertEquals(1, faltas(fonteOriginal));

        Espera espera = new Espera(new CompletableFuture<>(), new CountDownLatch(1), new AtomicBoolean());
        assertTrue(BarreiraConclusao.ativa.compareAndSet(null, espera));
        long id;
        try {
            JsonNode aceito = dados(executar(c.localA, post("/api/painel/calculos")
                    .param("referencia", DATA.toString()).param("periodoDias", "7"), 202,
                    "Consolidação assíncrona usa a fonte original antes da retificação"));
            id = aceito.path("id").asLong();
            espera.alcancou().get(20, TimeUnit.SECONDS);
            registrar(c, "PRESENCA");
            JsonNode fonteVigente = dados(executar(c.localA, get("/api/painel/gestao")
                    .param("referencia", DATA.toString()).param("periodoDias", "7"), 200,
                    "Controle: retificação já fez commit e o cálculo atual mostra ausência zero"));
            assertEquals(0, faltas(fonteVigente));
        } finally {
            espera.liberar().countDown();
            BarreiraConclusao.ativa.compareAndSet(espera, null);
        }

        JsonNode publicado = aguardarFimDaTentativa(c, id);
        assertTrue(atualOuExplicitamenteSuperado(publicado),
                "GET publicou resultado anterior à retificação como atual: " + publicado);
        var resposta = mvc.perform(autenticar(post("/api/painel/calculos")
                .param("referencia", DATA.toString()).param("periodoDias", "7"), c.localA)).andReturn();
        assertTrue(resposta.getResponse().getStatus() == 200 || resposta.getResponse().getStatus() == 202,
                resposta.getResponse().getContentAsString());
        JsonNode renovacao = dados(resposta);
        assertTrue(atualOuExplicitamenteSuperado(renovacao),
                "Nova solicitação reutilizou resultado antigo como atual: " + renovacao);
        JsonNode atual = aguardarAtual(c, renovacao.path("id").asLong());
        assertEquals(0, faltas(atual.path("dados")));
        assertFalse(atual.path("desatualizado").asBoolean());
    }

    private JsonNode aguardarFimDaTentativa(Cenario c, long id) throws Exception {
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
        JsonNode estado;
        do {
            estado = consultar(c.localA, "/api/painel/calculos/" + id,
                    "Polling aguarda a publicação real depois de liberar o processador");
            assertNotEquals("ERRO", estado.path("status").asText(), estado.toString());
            if (!"PROCESSANDO".equals(estado.path("status").asText())) return estado;
            Thread.sleep(75);
        } while (System.nanoTime() < limite);
        return fail("A tentativa de cálculo ficou em processamento após liberar a barreira: " + estado);
    }

    private JsonNode aguardarAtual(Cenario c, long id) throws Exception {
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        JsonNode estado;
        do {
            estado = consultar(c.localA, "/api/painel/calculos/" + id, "Renovação precisa produzir a verdade vigente");
            assertNotEquals("ERRO", estado.path("status").asText(), estado.toString());
            assertTrue(atualOuExplicitamenteSuperado(estado), estado.toString());
            if (estado.path("concluido").asBoolean() && !estado.path("desatualizado").asBoolean()
                    && faltas(estado.path("dados")) == 0) return estado;
            Thread.sleep(75);
        } while (System.nanoTime() < limite);
        return fail("Renovação não produziu painel atual em 20 segundos: " + estado);
    }

    private boolean atualOuExplicitamenteSuperado(JsonNode resultado) {
        return resultado.path("dados").isNull() || resultado.path("dados").isMissingNode()
                || faltas(resultado.path("dados")) == 0 || resultado.path("desatualizado").asBoolean();
    }

    private long faltas(JsonNode painel) { return painel.path("periodo").path("faltasNaoJustificadas").asLong(-1); }

    private void registrar(Cenario c, String status) throws Exception {
        enviar(c.localA, "/api/frequencias", mapa("vinculoContratualId", c.vinculoA.getId(),
                "dataReferencia", DATA, "status", status,
                "justificativaRetificacao", "Conferência documental concorrente da frequência",
                "chaveIdempotencia", "preaud-painel-concorrente-" + UUID.randomUUID()), 200,
                "Frequência real com commit independente publica status " + status);
    }
}
