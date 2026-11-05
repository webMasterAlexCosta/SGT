package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.EventoFrequencia;
import com.example.siggtrat.entidade.Frequencia;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MvcResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** AUD005: o clique repetido não cria outra versão negocial na escrita operacional. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ConclusaoFrequenciaIdempotenciaOperacionalHttpIT extends AuditoriaHttpSuporte {
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void reenvioOperacionalPreservaVersaoCadeiaEAutorComOuSemChave(boolean comChave) throws Exception {
        Cenario c = criarCenarioPersistido();
        Map<String, Object> req = payload(c, comChave);
        JsonNode primeira = salvar(c, req, 200);
        long id = primeira.path("id").asLong();
        List<String> cadeia = cadeia(id);
        JsonNode segunda = salvar(c, req, 200);
        assertThat(segunda.path("id")).isEqualTo(primeira.path("id"));
        assertThat(segunda.path("versaoNegocial")).isEqualTo(primeira.path("versaoNegocial"));
        assertThat(segunda.path("estadoAuditoria")).isEqualTo(primeira.path("estadoAuditoria"));
        assertThat(cadeia(id)).as("Reenvio não cria nem modifica o evento original").isEqualTo(cadeia);
        emTransacao(() -> {
            Frequencia f = em.find(Frequencia.class, id);
            assertThat(f.getAutorOriginalId()).isEqualTo(c.operacionalA.getId());
            assertThat(f.getVersaoNegocial()).isEqualTo(1);
            assertThat(contar(c)).isEqualTo(1L);
            return null;
        });
    }

    @Test
    void mesmaChaveOperacionalComIntencaoDiferenteConflitaSemMutacao() throws Exception {
        Cenario c = criarCenarioPersistido();
        Map<String, Object> req = payload(c, true);
        JsonNode primeira = salvar(c, req, 200);
        long id = primeira.path("id").asLong();
        List<String> cadeia = cadeia(id);
        req.put("status", "PRESENCA");
        salvar(c, req, 409);
        assertThat(cadeia(id)).isEqualTo(cadeia);
        emTransacao(() -> {
            Frequencia f = em.find(Frequencia.class, id);
            assertThat(f.getStatus()).isEqualTo("FALTA");
            assertThat(f.getVersaoNegocial()).isEqualTo(1);
            return null;
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @Timeout(value = 45, unit = TimeUnit.SECONDS)
    void reenviosOperacionaisConcorrentesConfirmamUmUnicoFato(boolean comChave) throws Exception {
        Cenario c = criarCenarioPersistido();
        String corpo = json(payload(c, comChave));
        var a = autenticar(post("/api/frequencias").contentType(APPLICATION_JSON).content(corpo), c.operacionalA);
        var b = autenticar(post("/api/frequencias").contentType(APPLICATION_JSON).content(corpo), c.operacionalA);
        CountDownLatch largada = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<MvcResult> fa = pool.submit(() -> { largada.await(); return mvc.perform(a).andReturn(); });
            Future<MvcResult> fb = pool.submit(() -> { largada.await(); return mvc.perform(b).andReturn(); });
            largada.countDown();
            MvcResult ra = fa.get(35, TimeUnit.SECONDS), rb = fb.get(35, TimeUnit.SECONDS);
            assertThat(ra.getResponse().getStatus()).as(ra.getResponse().getContentAsString()).isEqualTo(200);
            assertThat(rb.getResponse().getStatus()).as(rb.getResponse().getContentAsString()).isEqualTo(200);
            JsonNode primeiro = dados(ra), segundo = dados(rb);
            assertThat(segundo.path("id")).isEqualTo(primeiro.path("id"));
            assertThat(segundo.path("versaoNegocial")).isEqualTo(primeiro.path("versaoNegocial"));
            assertThat(cadeia(primeiro.path("id").asLong())).hasSize(1);
            assertThat(emTransacao(() -> contar(c))).isEqualTo(1L);
        } finally { largada.countDown(); pool.shutdownNow(); }
    }

    @Test
    void novaAlteracaoSemChaveContinuaVersionadaEAuditavel() throws Exception {
        Cenario c = criarCenarioPersistido();
        Map<String, Object> req = payload(c, false);
        JsonNode primeira = salvar(c, req, 200);
        req.put("status", "PRESENCA");
        JsonNode segunda = salvar(c, req, 200);
        assertThat(segunda.path("id")).isEqualTo(primeira.path("id"));
        assertThat(segunda.path("versaoNegocial").asInt()).isEqualTo(2);
        assertThat(segunda.path("estadoAuditoria").asText()).isEqualTo("ALTERADA_OPERACIONAL");
        assertThat(cadeia(primeira.path("id").asLong())).hasSize(2);
    }

    @Test
    void camposTextuaisComSeparadorNaoColidemNaIntencaoGerencial() throws Exception {
        Cenario c = criarCenarioPersistido();
        Map<String, Object> req = payload(c, true);
        req.put("observacao", "Observação original|DOCUMENTO");
        req.put("justificativaRetificacao", "Conferência documental autorizada");
        JsonNode primeira = dados(executar(c.localA, post("/api/frequencias").contentType(APPLICATION_JSON)
                .content(json(req)), 200, "AUD005 primeira intenção textual do Gestor"));
        List<String> cadeia = cadeia(primeira.path("id").asLong());
        req.put("observacao", "Observação original");
        req.put("justificativaRetificacao", "DOCUMENTO|Conferência documental autorizada");
        executar(c.localA, post("/api/frequencias").contentType(APPLICATION_JSON)
                .content(json(req)), 409, "AUD005 fronteiras dos campos não podem se confundir na assinatura da intenção");
        assertThat(cadeia(primeira.path("id").asLong())).isEqualTo(cadeia);
    }

    private Map<String, Object> payload(Cenario c, boolean comChave) {
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("vinculoContratualId", c.vinculoA.getId());
        req.put("dataReferencia", DataJornadaSintetica.ultimoDiaDeTrabalhoAnterior(c.jornadaA));
        req.put("status", "FALTA");
        if (comChave) req.put("chaveIdempotencia", "operacional-reenvio-" + UUID.randomUUID());
        return req;
    }
    private JsonNode salvar(Cenario c, Map<String, Object> req, int status) throws Exception {
        return dados(executar(c.operacionalA, post("/api/frequencias").contentType(APPLICATION_JSON)
                .content(json(req)), status, "AUD005 frequência operacional com idempotência"));
    }
    private long contar(Cenario c) {
        return em.createQuery("select count(f) from Frequencia f where f.vinculoContratual.id=:id", Long.class)
                .setParameter("id", c.vinculoA.getId()).getSingleResult();
    }
    private List<String> cadeia(long id) {
        return emTransacao(() -> em.createQuery("select e from EventoFrequencia e where e.frequencia.id=:id order by e.numeroVersao,e.id", EventoFrequencia.class)
                .setParameter("id", id).getResultList().stream()
                .map(e -> e.getId() + "|" + e.getNumeroVersao() + "|" + e.getHashEvento() + "|" + e.getRegistradoEm())
                .toList());
    }
}
