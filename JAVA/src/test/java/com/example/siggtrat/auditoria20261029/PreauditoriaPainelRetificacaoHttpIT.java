package com.example.siggtrat.auditoria20261029;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** FRE-008/REL-006: o cache persistido do painel também depende da verdade retificada. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PreauditoriaPainelRetificacaoHttpIT extends ContratosHttpBase {
    @Test
    void retificacaoDeFrequenciaNaoDeixaPayloadAntigoDoPainelMarcadoComoAtual() throws Exception {
        Cenario c = criarCenarioPersistido();
        registrar(c, "FALTA");
        JsonNode inicial = dados(executar(c.localA, post("/api/painel/calculos")
                        .param("referencia", DATA.toString()).param("periodoDias", "7"), 202,
                "Painel real é calculado com a falta publicada"));
        long id = inicial.path("id").asLong();
        JsonNode pronto = aguardar(c, id);
        assertEquals(1, faltas(pronto.path("dados")), "Controle: o payload inicial realmente contém a falta");

        registrar(c, "PRESENCA");
        JsonNode vigente = dados(executar(c.localA, get("/api/painel/gestao")
                        .param("referencia", DATA.toString()).param("periodoDias", "7"), 200,
                "Controle: cálculo síncrono já usa a verdade gerencial retificada"));
        assertEquals(0, faltas(vigente), "Controle: fonte/indicador atual deixou de conter falta");
        JsonNode consulta = consultar(c.localA, "/api/painel/calculos/" + id,
                "Polling do mesmo cálculo após retificação precisa sinalizar payload superado");

        // A implementação pode recalcular sincronamente (200) ou reenfileirar
        // (202). Ambas são aceitáveis, desde que não anuncie dado antigo como atual.
        var resposta = mvc.perform(autenticar(post("/api/painel/calculos")
                .param("referencia", DATA.toString()).param("periodoDias", "7"), c.localA)).andReturn();
        assertTrue(resposta.getResponse().getStatus() == 200 || resposta.getResponse().getStatus() == 202);
        JsonNode repeticao = dados(resposta);
        assertAll(
                () -> assertTrue(atualOuExplicitamenteSuperado(consulta),
                        "GET não pode devolver FALTA=1 com CONCLUIDO e desatualizado=false após retificação"),
                () -> assertTrue(atualOuExplicitamenteSuperado(repeticao),
                        "Nova solicitação não pode reutilizar FALTA=1 como dado atual durante TTL do payload persistido"));
    }

    private boolean atualOuExplicitamenteSuperado(JsonNode resultado) {
        return resultado.path("dados").isNull() || resultado.path("dados").isMissingNode()
                || faltas(resultado.path("dados")) == 0
                || resultado.path("desatualizado").asBoolean();
    }

    private long faltas(JsonNode painel) { return painel.path("periodo").path("faltasNaoJustificadas").asLong(-1); }

    private void registrar(Cenario c, String status) throws Exception {
        enviar(c.localA, "/api/frequencias", mapa("vinculoContratualId", c.vinculoA.getId(),
                "dataReferencia", DATA, "status", status,
                "justificativaRetificacao", "Conferência documental da verdade do indicador",
                "chaveIdempotencia", "preauditoria-painel-" + UUID.randomUUID()), 200,
                "Retificação real por API publica status " + status);
    }

    private JsonNode aguardar(Cenario c, long id) throws Exception {
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(25);
        JsonNode estado;
        do {
            estado = consultar(c.localA, "/api/painel/calculos/" + id, "Aguardar processamento assíncrono real");
            assertNotEquals("ERRO", estado.path("status").asText(), estado.toString());
            if (estado.path("concluido").asBoolean()) return estado;
            Thread.sleep(100);
        } while (System.nanoTime() < limite);
        return fail("Painel não concluiu no prazo de 25 segundos: " + estado);
    }
}
