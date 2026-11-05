package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.frequencia.TipoEventoFrequencia;
import com.example.siggtrat.entidade.EventoFrequencia;
import com.example.siggtrat.entidade.Frequencia;
import com.example.siggtrat.servico.AuditoriaFrequenciaServico;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** O histórico diagnosticado não é reassinado nem confundido com integridade comprovada. */
class FrequenciaEvidenciasIntegridadeHttpIT extends AuditoriaHttpSuporte {
    @Autowired AuditoriaFrequenciaServico auditoria;

    @Test
    void eventoComRepresentacaoLegadaPermaneceConsultavelSemSerDeclaradoIntegroOuReassinado() throws Exception {
        Cenario c = criarCenario();
        Frequencia f = frequencia(c, false, DATA, "FALTA"); em.flush();
        EventoFrequencia e = new EventoFrequencia();
        e.setFrequencia(f); e.setNumeroVersao(1); e.setTipoEvento(TipoEventoFrequencia.REGISTRO);
        e.setDataReferencia(DATA); e.setSnapshotNovo(auditoria.snapshot(f));
        e.setMotivo("  Motivo   herdado  "); e.setUsuarioId(c.localA.getId()); e.setUsuarioUsername(c.localA.getUsername());
        e.setRegistradoEm(Instant.parse("2026-10-20T15:20:30.123456789Z"));
        // Reproduz o formato anterior: assinatura precede a normalização e usa nanos.
        String hashOriginal = ReflectionTestUtils.invokeMethod(auditoria, "calcularHash", e);
        e.setHashEvento(hashOriginal); em.persist(e); em.flush();
        long frequenciaId = f.getId(), eventoId = e.getId(); em.clear();
        executar(c.localA, get("/api/frequencias/{id}/eventos", frequenciaId), 409,
                "Consulta estrita não atesta integridade que não consegue demonstrar");
        JsonNode evidencia = dados(executar(c.localA, get("/api/frequencias/{id}/evidencias-integridade", frequenciaId), 200,
                "Consulta diagnóstica preserva os campos e os hashes disponíveis do evento legado"));
        assertEquals(frequenciaId, evidencia.path("frequenciaId").asLong());
        assertEquals("INCONSISTENTE_OU_NAO_VERIFICAVEL", evidencia.path("estado").asText());
        assertFalse(evidencia.path("cadeiaIntegra").asBoolean());
        assertEquals(1, evidencia.path("eventos").size());
        JsonNode item = evidencia.path("eventos").get(0);
        assertEquals(eventoId, item.path("eventoOriginal").path("id").asLong());
        assertEquals(hashOriginal, item.path("hashArmazenado").asText());
        assertEquals(hashOriginal, item.path("eventoOriginal").path("hashEvento").asText());
        assertEquals("Motivo herdado", item.path("eventoOriginal").path("motivo").asText());
        assertEquals(64, item.path("hashCalculado").asText().length());
        assertFalse(item.path("hashConfere").asBoolean());
        assertTrue(item.path("encadeamentoConfere").asBoolean());
        em.clear();
        assertEquals(hashOriginal, em.find(EventoFrequencia.class, eventoId).getHashEvento());
    }

    @Test
    void evidenciaIntegraExibeComparacaoEReutilizaEscopoHistoricoDaFrequencia() throws Exception {
        Cenario c = criarCenario();
        JsonNode criada = dados(executar(c.localA, post("/api/frequencias").contentType(APPLICATION_JSON).content(json(Map.of(
                "vinculoContratualId", c.vinculoA.getId(), "dataReferencia", DATA.toString(), "status", "FALTA",
                "justificativaRetificacao", "Registro formal de frequência íntegra",
                "chaveIdempotencia", "evidencia-integra-" + UUID.randomUUID()))), 200, "Evento novo verificável"));
        long id = criada.path("id").asLong(); em.flush(); em.clear();
        JsonNode evidencia = dados(executar(c.localA, get("/api/frequencias/{id}/evidencias-integridade", id), 200,
                "Evento novo possui hash calculado igual ao armazenado"));
        assertEquals("INTEGRA", evidencia.path("estado").asText());
        assertTrue(evidencia.path("cadeiaIntegra").asBoolean());
        JsonNode item = evidencia.path("eventos").get(0);
        assertTrue(item.path("hashConfere").asBoolean());
        assertEquals(item.path("hashArmazenado"), item.path("hashCalculado"));
        executar(c.localB, get("/api/frequencias/{id}/evidencias-integridade", id), 404,
                "Diagnóstico não revela frequência de outra Gestão");
        executar(c.localA, get("/api/frequencias/{id}/evidencias-integridade", Long.MAX_VALUE), 404,
                "Diagnóstico mantém contrato de recurso inexistente");
        executar(c.presidente, get("/api/frequencias/{id}/evidencias-integridade", id), 403,
                "Diagnóstico não amplia o acesso operacional da Presidência");
    }
}
