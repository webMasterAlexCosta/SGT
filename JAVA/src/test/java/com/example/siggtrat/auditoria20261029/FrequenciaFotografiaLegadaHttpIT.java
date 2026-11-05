package com.example.siggtrat.auditoria20261029;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Simula dados anteriores à coluna de fotografia sem inventar um horário histórico. */
class FrequenciaFotografiaLegadaHttpIT extends AuditoriaHttpSuporte {
    @Test
    void ausenciaDeEvidenciaHistoricaNaoPodeSerApresentadaComoDescansoDeZeroHoras() throws Exception {
        Cenario c = criarCenario();
        removerFotografiaSemanalLegada(c);
        executar(c.localA, get("/api/relatorios/frequencia-consolidada").param("inicio", DATA.toString())
                .param("fim", DATA.toString()).param("contratoIds", c.contratoA.getId().toString()), 409,
                "TMP-004: ausência documental da carga histórica deve ser explícita, nunca descanso fabricado");
        executar(c.localA, post("/api/frequencias").contentType(APPLICATION_JSON).content(json(Map.of(
                "vinculoContratualId", c.vinculoA.getId(), "dataReferencia", DATA.toString(), "status", "PRESENCA",
                "justificativaRetificacao", "Tentativa de frequência sem fotografia histórica",
                "chaveIdempotencia", "sem-fotografia-" + UUID.randomUUID()))), 409,
                "Frequência retroativa não infere carga do cadastro atual");
    }
    @Test
    void fotografiaDiariaPersistidaContinuaSuficienteMesmoSemFotografiaSemanalLegada() throws Exception {
        Cenario c = criarCenario();
        executar(c.localA, post("/api/frequencias").contentType(APPLICATION_JSON).content(json(Map.of(
                "vinculoContratualId", c.vinculoA.getId(), "dataReferencia", DATA.toString(), "status", "PRESENCA",
                "justificativaRetificacao", "Registro com fotografia diária íntegra",
                "chaveIdempotencia", "fotografia-diaria-" + UUID.randomUUID()))), 200, "Fotografia diária original");
        removerFotografiaSemanalLegada(c);
        JsonNode r = dados(executar(c.localA, get("/api/relatorios/frequencia-consolidada")
                .param("inicio", DATA.toString()).param("fim", DATA.toString())
                .param("contratoIds", c.contratoA.getId().toString()), 200, "Evidência diária existente permanece utilizável"));
        JsonNode dia = r.path("registros").get(0).path("diario").path(DATA.toString());
        assertEquals(480, dia.path("minutosPrevistos").asInt());
        assertEquals(480, dia.path("minutosPagaveis").asInt());
        assertEquals("PRESENCA", dia.path("status").asText());
    }
    private void removerFotografiaSemanalLegada(Cenario c) {
        em.flush();
        em.createQuery("update AlocacaoVinculo a set a.jornadaSemanalFotografia=null where a.id=:id")
                .setParameter("id", c.alocacaoA.getId()).executeUpdate();
        em.clear();
    }
}
