package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.dto.RelatorioFrequenciaConsolidadoDTO;
import com.example.siggtrat.entidade.Frequencia;
import com.example.siggtrat.entidade.GestaoLocal;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.servico.RelatorioFrequenciaConsolidadoServico;
import com.example.siggtrat.servico.CalculoFrequenciaServico;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * Spies apenas colocam barreiras no fluxo real. As duas transações e o PostgreSQL são
 * reais. Aceita serialização por lock, rejeição por conflito ou revalidação.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class EmissaoRetificacaoConcorrenciaAuditoriaHttpIT extends AuditoriaHttpSuporte {
    @MockitoSpyBean RelatorioFrequenciaConsolidadoServico relatorio;
    @MockitoSpyBean CalculoFrequenciaServico calculo;

    @Test
    @Timeout(value = 45, unit = TimeUnit.SECONDS)
    void primeiraEmissaoNaoPodeFicarVigenteComFonteRetificadaDuranteSeuCalculo() throws Exception {
        Cenario c = criarCenarioPersistido();
        UsuarioAplicacao corretor = emTransacao(() -> usuario(PerfilAcesso.GESTOR_LOCAL,
                em.find(GestaoLocal.class, c.gestaoA.getId())));
        assertNotEquals(c.localA.getId(), corretor.getId(), "Atores e sessões distintos no mesmo escopo.");
        JsonNode fonteOriginal = dados(executar(c.localA, post("/api/frequencias").contentType(APPLICATION_JSON)
                        .content(json(frequencia(c, "PRESENCA"))),
                200, "Fonte confirmada antes da corrida: presença de 8 horas"));
        long frequenciaId = fonteOriginal.path("id").asLong();
        var reqEmissao = autenticar(post("/api/relatorios/administrativos/emissoes")
                .contentType(APPLICATION_JSON).content(json(Map.of(
                        "tipo", "FREQUENCIA_CONSOLIDADA", "formato", "VISUALIZACAO",
                        "inicio", DATA.toString(), "fim", DATA.toString(),
                        "contratoIds", List.of(c.contratoA.getId()),
                        "chaveIdempotencia", "corrida-emissao-" + UUID.randomUUID()))), c.localA);
        var reqRetificacao = autenticar(post("/api/frequencias").contentType(APPLICATION_JSON)
                .content(json(frequencia(c, "FALTA"))), corretor);

        CompletableFuture<Long> conteudoPronto = new CompletableFuture<>();
        CountDownLatch liberarEmissao = new CountDownLatch(1);
        AtomicBoolean primeira = new AtomicBoolean(true);
        doAnswer(invocacao -> {
            RelatorioFrequenciaConsolidadoDTO resultado = (RelatorioFrequenciaConsolidadoDTO) invocacao.callRealMethod();
            Collection<Long> ids = invocacao.getArgument(2);
            if (ids != null && ids.contains(c.contratoA.getId()) && primeira.compareAndSet(true, false)) {
                assertEquals(1, resultado.totais().presencas(), "A barreira vem depois do cálculo antigo real.");
                conteudoPronto.complete(((Number) em.createNativeQuery("select pg_backend_pid()")
                        .getSingleResult()).longValue());
                if (!liberarEmissao.await(25, TimeUnit.SECONDS)) throw new AssertionError("Barreira não liberada.");
            }
            return resultado;
        }).when(org.springframework.test.util.AopTestUtils.<RelatorioFrequenciaConsolidadoServico>getUltimateTargetObject(relatorio)).gerar(eq(DATA), eq(DATA), anyCollection(), isNull(), isNull());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<MvcResult> emissao = executor.submit(() -> mvc.perform(reqEmissao).andReturn());
            long conexaoEmissao = conteudoPronto.get(10, TimeUnit.SECONDS);
            Future<MvcResult> retificacao = executor.submit(() -> mvc.perform(reqRetificacao).andReturn());
            long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            boolean intercalacaoObservada = false;
            while (System.nanoTime() < limite) {
                if (retificacao.isDone()) { intercalacaoObservada = true; break; }
                long esperas = emTransacao(() -> ((Number) em.createNativeQuery("""
SELECT count(*) FROM pg_stat_activity a WHERE a.datname=current_database() AND :conexao = ANY(pg_blocking_pids(a.pid))
""").setParameter("conexao", conexaoEmissao).getSingleResult()).longValue());
                if (esperas > 0) { intercalacaoObservada = true; break; }
                TimeUnit.MILLISECONDS.sleep(20);
            }
            assertTrue(intercalacaoObservada,
                    "Antes de liberar emissão, a retificação deve concluir ou disputar um lock real da emissão.");
            liberarEmissao.countDown();
            MvcResult resultadoEmissao = emissao.get(15, TimeUnit.SECONDS);
            MvcResult resultadoRetificacao = retificacao.get(15, TimeUnit.SECONDS);
            assertEquals(200, resultadoRetificacao.getResponse().getStatus(),
                    resultadoRetificacao.getResponse().getContentAsString());
            JsonNode fonteCorrigida = dados(resultadoRetificacao);
            assertEquals(frequenciaId, fonteCorrigida.path("id").asLong());
            assertEquals("FALTA", fonteCorrigida.path("status").asText());
            assertEquals(fonteOriginal.path("versaoNegocial").asInt() + 1,
                    fonteCorrigida.path("versaoNegocial").asInt());
            assertTrue(List.of(201, 409).contains(resultadoEmissao.getResponse().getStatus()),
                    resultadoEmissao.getResponse().getContentAsString());

            JsonNode atual = dados(executar(c.localA, get("/api/relatorios/frequencia-consolidada")
                            .param("inicio", DATA.toString()).param("fim", DATA.toString())
                            .param("contratoIds", c.contratoA.getId().toString()),
                    200, "A retificação confirmada é a verdade operacional após as duas transações"));
            assertEquals(1, atual.path("totais").path("faltas").asInt());
            JsonNode eventos = dados(executar(c.localA, get("/api/frequencias/{id}/eventos", frequenciaId),
                    200, "A autoria da versão corrigida pertence ao segundo Gestor Local"));
            assertEquals(corretor.getId().longValue(), eventos.get(eventos.size() - 1).path("usuarioId").asLong());
            if (resultadoEmissao.getResponse().getStatus() == 409) return;
            long id = dados(resultadoEmissao).path("metadados").path("id").asLong();
            JsonNode preservada = dados(executar(c.localA,
                    get("/api/relatorios/administrativos/emissoes/{id}", id), 200,
                    "Após commits, fotografia antiga pode existir somente como histórica/superada"));
            boolean antiga = preservada.path("conteudo").path("totais").path("presencas").asInt() == 1;
            boolean vigente = "VIGENTE".equals(preservada.path("metadados").path("estado").asText());
            assertFalse(antiga && vigente,
                    "REL-001/REL-006: retificação confirmada não pode deixar emissão antiga como VIGENTE: "
                            + preservada.path("metadados"));
        } finally {
            liberarEmissao.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    @Timeout(value = 45, unit = TimeUnit.SECONDS)
    void emissaoQueEsperaRetificacaoLeFonteConfirmadaMesmoSemFiltroDeContrato() throws Exception {
        Cenario c = criarCenarioPersistido();
        UsuarioAplicacao corretor = emTransacao(() -> usuario(PerfilAcesso.GESTOR_LOCAL,
                em.find(GestaoLocal.class, c.gestaoA.getId())));
        JsonNode original = dados(executar(c.localA, post("/api/frequencias").contentType(APPLICATION_JSON)
                        .content(json(frequencia(c, "PRESENCA"))), 200, "Fonte anterior à retificação bloqueada"));
        long frequenciaId = original.path("id").asLong();
        var reqRetificacao = autenticar(post("/api/frequencias").contentType(APPLICATION_JSON)
                .content(json(frequencia(c, "FALTA"))), corretor);
        var reqEmissao = autenticar(post("/api/relatorios/administrativos/emissoes")
                .contentType(APPLICATION_JSON).content(json(Map.of(
                        "tipo", "FREQUENCIA_CONSOLIDADA", "formato", "VISUALIZACAO",
                        "inicio", DATA.toString(), "fim", DATA.toString(),
                        "chaveIdempotencia", "emissao-aguarda-fonte-" + UUID.randomUUID()))), c.localA);
        CompletableFuture<Long> fonteBloqueada = new CompletableFuture<>();
        CountDownLatch liberarRetificacao = new CountDownLatch(1);
        AtomicBoolean primeira = new AtomicBoolean(true);
        doAnswer(invocacao -> {
            Frequencia f = invocacao.getArgument(0);
            Object resultado = invocacao.callRealMethod();
            if (Long.valueOf(frequenciaId).equals(f.getId()) && "FALTA".equals(f.getStatus())
                    && primeira.compareAndSet(true, false)) {
                fonteBloqueada.complete(((Number) em.createNativeQuery("select pg_backend_pid()")
                        .getSingleResult()).longValue());
                if (!liberarRetificacao.await(25, TimeUnit.SECONDS)) throw new AssertionError("Barreira não liberada.");
            }
            return resultado;
        }).when(org.springframework.test.util.AopTestUtils.<CalculoFrequenciaServico>getUltimateTargetObject(calculo)).processarCalculos(any(Frequencia.class));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<MvcResult> retificacao = executor.submit(() -> mvc.perform(reqRetificacao).andReturn());
            long conexaoRetificacao = fonteBloqueada.get(10, TimeUnit.SECONDS);
            Future<MvcResult> emissao = executor.submit(() -> mvc.perform(reqEmissao).andReturn());
            long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            boolean observada = false;
            while (System.nanoTime() < limite) {
                if (emissao.isDone()) { observada = true; break; }
                long esperas = emTransacao(() -> ((Number) em.createNativeQuery("""
SELECT count(*) FROM pg_stat_activity a WHERE a.datname=current_database() AND :conexao = ANY(pg_blocking_pids(a.pid))
""").setParameter("conexao", conexaoRetificacao).getSingleResult()).longValue());
                if (esperas > 0) { observada = true; break; }
                TimeUnit.MILLISECONDS.sleep(20);
            }
            assertTrue(observada, "Emissão deve concluir ou esperar por lock real antes de liberar a fonte.");
            liberarRetificacao.countDown();
            MvcResult corrigida = retificacao.get(15, TimeUnit.SECONDS);
            MvcResult emitida = emissao.get(15, TimeUnit.SECONDS);
            assertEquals(200, corrigida.getResponse().getStatus(), corrigida.getResponse().getContentAsString());
            assertEquals("FALTA", dados(corrigida).path("status").asText());
            assertTrue(List.of(201, 409).contains(emitida.getResponse().getStatus()),
                    emitida.getResponse().getContentAsString());
            if (emitida.getResponse().getStatus() == 409) return;
            long id = dados(emitida).path("metadados").path("id").asLong();
            JsonNode persistida = dados(executar(c.localA, get("/api/relatorios/administrativos/emissoes/{id}", id),
                    200, "Após espera por correção, uma emissão vigente usa a fonte confirmada"));
            if ("VIGENTE".equals(persistida.path("metadados").path("estado").asText())) {
                assertEquals(1, persistida.path("conteudo").path("totais").path("faltas").asInt(),
                        "Uma leitura anterior ao lock não pode fixar o snapshot antigo do PostgreSQL.");
                assertEquals(0, persistida.path("conteudo").path("totais").path("presencas").asInt());
            }
        } finally {
            liberarRetificacao.countDown();
            executor.shutdownNow();
        }
    }

    private Map<String, Object> frequencia(Cenario c, String status) {
        return Map.of("vinculoContratualId", c.vinculoA.getId(), "dataReferencia", DATA.toString(),
                "status", status, "justificativaRetificacao", "Conferência documental concorrente com emissão",
                "chaveIdempotencia", "corrida-fonte-" + UUID.randomUUID());
    }
}
