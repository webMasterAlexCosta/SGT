package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.Frequencia;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Lacunas de regressão: recusa direta de futuro, atomicidade e batidas físicas no PostgreSQL real. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PreservacaoFrequenciaSemFinanceiroHttpIT extends AuditoriaHttpSuporte {
    @ParameterizedTest(name = "Frequência futura recusada; gestor={0}")
    @ValueSource(booleans = {true, false})
    void apiRecusaPresencaFuturaSemGravarFonteOuEvento(boolean gestor) throws Exception {
        Cenario c = criarCenarioPersistido();
        LocalDate amanha = LocalDate.now(ZoneId.of("America/Sao_Paulo")).plusDays(1);
        JsonNode resposta = dados(executar(gestor ? c.localA : c.operacionalA,
                post("/api/frequencias").contentType(APPLICATION_JSON)
                        .content(json(presenca(c, amanha))), 400,
                "A API impede presença futura, inclusive para Gestor Local"));
        assertTrue(resposta.toString().contains("datas futuras"));
        assertSemFrequenciaOuEvento(c);
    }

    @Test void loteComItemFuturoReverteTambemOPrimeiroItemValido() throws Exception {
        Cenario c = criarCenarioPersistido();
        var valido = presenca(c, DATA);
        var futuro = presenca(c, LocalDate.now(ZoneId.of("America/Sao_Paulo")).plusDays(1));
        JsonNode resposta = dados(executar(c.localA, post("/api/frequencias/lote")
                        .contentType(APPLICATION_JSON).content(json(List.of(valido, futuro))),
                400, "Lote não confirma parcialmente quando contém presença futura"));
        assertTrue(resposta.toString().contains("datas futuras"));
        assertSemFrequenciaOuEvento(c);
    }

    @ParameterizedTest(name = "Batidas até {0}: {1} e {2} minutos físicos")
    @CsvSource({"17:00,PRESENCA,480", "15:00,PRESENCA_PARCIAL,360"})
    void batidasPreservamApuracaoFisicaVinculoEConsolidado(String saida, String status, int minutos) throws Exception {
        Cenario c = criarCenarioPersistido();
        declararFonteNominalSintetica(c, "REMOCAO-FINANCEIRA-BATIDAS-" + saida);
        Map<String,Object> corpo = presenca(c, DATA);
        corpo.put("entrada1", "08:00"); corpo.put("saida1", "12:00");
        corpo.put("entrada2", "13:00"); corpo.put("saida2", saida);
        // O status solicitado é integral nos dois casos; o cálculo real decide o parcial.
        JsonNode criada = dados(executar(c.localA, post("/api/frequencias")
                        .contentType(APPLICATION_JSON).content(json(corpo)),
                200, "Presença apurada pelas batidas, sem dependência de tarifa"));
        assertEquals(status, criada.path("status").asText());
        long id = criada.path("id").asLong();
        emTransacao(() -> {
            Frequencia f = em.find(Frequencia.class, id);
            assertEquals(minutos, f.getMinutosFisicos());
            assertEquals(minutos, f.getMinutosPagaveis());
            assertEquals(c.vinculoA.getId(), f.getVinculoContratual().getId());
            assertEquals(c.alocacaoA.getId(), f.getAlocacaoVinculo().getId());
            assertEquals(c.contratoA.getId(), f.getContratoSnapshotId());
            return null;
        });
        JsonNode consolidado = dados(executar(c.localA, get("/api/relatorios/frequencia-consolidada")
                        .param("inicio", DATA.toString()).param("fim", DATA.toString())
                        .param("contratoIds", c.contratoA.getId().toString()),
                200, "Relatório operacional usa a memória física preservada"));
        assertEquals(1, consolidado.path("registros").size());
        JsonNode dia = consolidado.path("registros").get(0).path("diario").path(DATA.toString());
        assertEquals(minutos, dia.path("minutosFisicos").asInt());
        assertEquals(minutos, dia.path("minutosPagaveis").asInt());
        JsonNode eventos = dados(executar(c.localA, get("/api/frequencias/{id}/eventos", id),
                200, "Trilha operacional preservada"));
        assertEquals(1, eventos.size());
        assertEquals(64, eventos.get(0).path("hashEvento").asText().length());
    }

    private Map<String,Object> presenca(Cenario c, LocalDate data) {
        Map<String,Object> corpo = new LinkedHashMap<>();
        corpo.put("vinculoContratualId", c.vinculoA.getId());
        corpo.put("dataReferencia", data.toString()); corpo.put("status", "PRESENCA");
        corpo.put("justificativaRetificacao", "Evidência sintética de regressão operacional");
        corpo.put("chaveIdempotencia", "preservacao-" + UUID.randomUUID());
        return corpo;
    }

    private void assertSemFrequenciaOuEvento(Cenario c) {
        emTransacao(() -> {
            assertEquals(0L, em.createQuery("select count(f) from Frequencia f where f.vinculoContratual.id=:v", Long.class)
                    .setParameter("v", c.vinculoA.getId()).getSingleResult());
            assertEquals(0L, em.createQuery("select count(e) from EventoFrequencia e where e.frequencia.vinculoContratual.id=:v", Long.class)
                    .setParameter("v", c.vinculoA.getId()).getSingleResult());
            return null;
        });
    }
}
