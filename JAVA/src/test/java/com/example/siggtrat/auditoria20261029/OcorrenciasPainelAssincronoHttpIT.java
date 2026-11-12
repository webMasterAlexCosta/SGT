package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
class OcorrenciasPainelAssincronoHttpIT extends AuditoriaHttpSuporte {
    @Test
    void ocorrenciaComAnexoPossuiConteudoAutorizadoERemocaoPreservaEvidenciaDeFrequencia() throws Exception {
        Cenario c = criarCenarioPersistido();
        JsonNode original = dados(executar(c.localA, post("/api/frequencias").contentType(APPLICATION_JSON).content(json(Map.of(
                "vinculoContratualId", c.vinculoA.getId(), "dataReferencia", DATA.toString(), "status", "PRESENCA",
                "entrada1", "08:00", "saida1", "12:00", "entrada2", "13:00", "saida2", "17:00",
                "justificativaRetificacao", "Presença documental anterior à ocorrência", "chaveIdempotencia", "ocorrencia-" + UUID.randomUUID()))),
                200, "Registro físico anterior à classificação administrativa"));
        byte[] conteudo = "Documento da ocorrência: evidência do caso de auditoria.".getBytes(StandardCharsets.UTF_8);
        JsonNode ocorrencia = dados(executar(c.localA, multipart("/api/ocorrencias")
                .file(new MockMultipartFile("anexos", "evidencia.txt", "text/plain", conteudo))
                .param("empregadoId", c.empregadoA.getId().toString()).param("motivo", "ATESTADO")
                .param("dataOcorrencia", DATA.toString()).param("dias", "1")
                .param("observacao", "Atestado recebido e conferido").param("numeroSei", "00001.000001/2026-01"),
                201, "Ocorrência e anexo são confirmados na mesma transação"));
        long id = ocorrencia.path("id").asLong(), anexoId = ocorrencia.path("anexos").get(0).path("id").asLong();
        assertEquals("ATESTADO", ocorrencia.path("motivo").asText());
        assertEquals(1, ocorrencia.path("quantidadeDias").asInt());
        assertEquals(c.empregadoA.getId().longValue(), ocorrencia.path("empregado").path("id").asLong());
        assertEquals("evidencia.txt", ocorrencia.path("anexos").get(0).path("nomeOriginal").asText());
        var arquivo = executar(c.localA, get("/api/ocorrencias/anexos/{id}", anexoId), 200, "Download do anexo confirmado");
        assertArrayEquals(conteudo, arquivo.getResponse().getContentAsByteArray());
        assertTrue(arquivo.getResponse().getContentType().startsWith("text/plain"));
        assertTrue(arquivo.getResponse().getHeader("Content-Disposition").contains("evidencia.txt"));
        executar(c.localB, get("/api/ocorrencias/anexos/{id}", anexoId), 403, "Outra gestão não baixa anexo");
        JsonNode listagem = dados(executar(c.localA, get("/api/ocorrencias")
                .param("empregadoId", c.empregadoA.getId().toString()).param("data", DATA.toString()), 200,
                "Listagem devolve ocorrência, colaborador e metadados de anexo"));
        assertEquals(1, listagem.path("totalElements").asInt());
        assertEquals(id, listagem.path("content").get(0).path("id").asLong());
        executar(c.localB, get("/api/ocorrencias").param("empregadoId", c.empregadoA.getId().toString()), 403,
                "Consulta por colaborador alheio é negada");
        executar(c.localB, delete("/api/ocorrencias/{id}", id), 403, "Outra gestão não remove ocorrência");
        executar(c.localA, delete("/api/ocorrencias/{id}", id), 204, "Remoção autorizada da classificação administrativa");
        emTransacao(() -> {
            // GOV-003/DOC-007: retirar a classificação não descarta a fonte já publicada.
            Ocorrencia cancelada = em.find(Ocorrencia.class, id);
            assertNotNull(cancelada);
            assertNotNull(cancelada.getCanceladoEm());
            assertNotNull(em.find(AnexoOcorrencia.class, anexoId));
            Frequencia f = em.find(Frequencia.class, original.path("id").asLong());
            assertEquals("PRESENCA", f.getStatus());
            assertEquals("08:00", f.getEntrada1());
            assertEquals("08:00", f.getHorasTrabalhadas());
            assertEquals(original.path("versaoNegocial").asInt() + 2, f.getVersaoNegocial());
            assertTrue(f.getBloqueadaEdicaoOperacional());
            return null;
        });
        assertArrayEquals(conteudo, executar(c.localA, get("/api/ocorrencias/anexos/{id}", anexoId),
                200, "A fonte documental preservada continua recuperável após a retirada")
                .getResponse().getContentAsByteArray());
        JsonNode cadeia = dados(executar(c.localA, get("/api/frequencias/{id}/eventos", original.path("id").asLong()),
                200, "Registro e remoção da ocorrência permanecem na cadeia de auditoria"));
        assertEquals(3, cadeia.size());
        assertTrue(cadeia.get(1).path("snapshotAnterior").asText().contains("status=PRESENCA"));
        assertTrue(cadeia.get(2).path("snapshotAnterior").asText().contains("status=ATESTADO"));
    }

    @Test
    void ocorrenciasRejeitamEntradaInvalidaEscopoPerfilEIdentificadoresAusentes() throws Exception {
        Cenario c = criarCenarioPersistido();
        executar(c.localA, get("/api/ocorrencias"), 400, "Ocorrências exigem data ou colaborador");
        executar(c.localA, multipart("/api/ocorrencias").param("empregadoId", c.empregadoA.getId().toString())
                .param("motivo", "ATESTADO").param("dataOcorrencia", LocalDate.now().plusDays(1).toString()),
                400, "Ocorrência futura é rejeitada");
        executar(c.localA, multipart("/api/ocorrencias").param("empregadoId", c.empregadoA.getId().toString())
                .param("motivo", " ").param("dataOcorrencia", DATA.toString()), 400, "Motivo é obrigatório");
        executar(c.localA, multipart("/api/ocorrencias").param("empregadoId", c.empregadoB.getId().toString())
                .param("motivo", "ATESTADO").param("dataOcorrencia", DATA.toString()), 403, "Não cadastra em gestão alheia");
        executar(c.operacionalA, multipart("/api/ocorrencias").param("empregadoId", c.empregadoA.getId().toString())
                .param("motivo", "ATESTADO").param("dataOcorrencia", DATA.toString()), 403, "Ocorrência é ato do gestor local");
        executar(c.localA, get("/api/ocorrencias/anexos/{id}", Long.MAX_VALUE), 404, "Anexo inexistente");
        executar(c.localA, delete("/api/ocorrencias/{id}", Long.MAX_VALUE), 404, "Ocorrência inexistente");
    }

    @Test
    void painelAssincronoConcluiReutilizaCacheEPreservaEscopo() throws Exception {
        Cenario c = criarCenarioPersistido();
        JsonNode inicial = dados(executar(c.localA, post("/api/painel/calculos")
                .param("referencia", DATA.toString()).param("periodoDias", "7"), 202,
                "Cálculo de painel é enfileirado após o commit"));
        long id = inicial.path("id").asLong();
        assertTrue(id > 0);
        executar(c.localB, get("/api/painel/calculos/{id}", id), 403, "Outro escopo não consulta cálculo");
        JsonNode pronto = aguardarPainel(c, id);
        assertEquals("CONCLUIDO", pronto.path("status").asText());
        assertEquals(100, pronto.path("progresso").asInt());
        assertEquals(c.gestaoA.getId().longValue(), pronto.path("dados").path("escopo").path("gestaoLocalId").asLong());
        assertEquals(1, pronto.path("dados").path("quadro").path("empregadosCadastrados").asLong());
        JsonNode cache = dados(executar(c.localA, post("/api/painel/calculos")
                .param("referencia", DATA.toString()).param("periodoDias", "7"), 200, "Resultado recente é reutilizado"));
        assertEquals(id, cache.path("id").asLong());
        assertTrue(cache.path("concluido").asBoolean());
        assertEquals(pronto.path("dados"), cache.path("dados"));
        executar(c.localA, post("/api/painel/calculos").param("referencia", DATA.toString()).param("periodoDias", "8"),
                400, "Cálculo não enfileira janela inválida");
        executar(c.localA, post("/api/painel/calculos").param("referencia", LocalDate.now().plusDays(1).toString()),
                400, "Cálculo não enfileira futuro");
        executar(c.localA, get("/api/painel/calculos/{id}", Long.MAX_VALUE), 404, "Cálculo inexistente");
        executar(c.operacionalA, post("/api/painel/calculos"), 403, "Operacional não solicita painel de gestão");
        executar(c.operacionalA, get("/api/painel/calculos/{id}", id), 403, "Operacional não lê resultado gerencial");
    }
    @Test
    void consultasSemPeriodoUsamDiarioNoGestaoEfetivoECalculoAssincrono() throws Exception {
        Cenario c = criarCenarioPersistido();
        JsonNode gestao = dados(executar(c.localA, get("/api/painel/gestao")
                .param("referencia", DATA.toString()), 200, "Gestão sem período é diária"));
        assertEquals(1, gestao.path("periodo").path("dias").asInt());
        assertEquals(DATA.toString(), gestao.path("periodo").path("inicio").asText());
        assertEquals(DATA.toString(), gestao.path("periodo").path("fim").asText());
        JsonNode efetivo = dados(executar(c.localA, get("/api/painel/efetivo")
                .param("referencia", DATA.toString()), 200, "Efetivo sem período é diário"));
        assertEquals("DIARIO", efetivo.path("periodo").asText());
        assertEquals(DATA.toString(), efetivo.path("inicio").asText());
        assertEquals(DATA.toString(), efetivo.path("fim").asText());
        JsonNode inicial = dados(executar(c.localA, post("/api/painel/calculos")
                .param("referencia", DATA.toString()), 202, "Cálculo sem período enfileira somente o dia"));
        JsonNode pronto = aguardarPainel(c, inicial.path("id").asLong());
        assertEquals(1, pronto.path("dados").path("periodo").path("dias").asInt());
        assertEquals(DATA.toString(), pronto.path("dados").path("periodo").path("inicio").asText());
        executar(c.localA, post("/api/painel/calculos").param("referencia", DATA.toString()).param("periodoDias", "0"),
                400, "Zero continua sendo janela inválida");
    }

    private JsonNode aguardarPainel(Cenario c, long id) throws Exception {
        long limite = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(25);
        JsonNode estado;
        do {
            estado = dados(executar(c.localA, get("/api/painel/calculos/{id}", id), 200, "Consulta de progresso do painel"));
            assertNotEquals("ERRO", estado.path("status").asText(), estado.toString());
            if (estado.path("concluido").asBoolean()) return estado;
            Thread.sleep(100);
        } while (System.nanoTime() < limite);
        return fail("Painel não concluiu em 25s: " + estado);
    }
}
