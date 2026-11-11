package com.example.siggtrat.auditoria20261029;

import com.fasterxml.jackson.databind.JsonNode;
import com.lowagie.text.pdf.PdfReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Fixtures commitadas: os executores reais leem em transações e conexões próprias. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RelatoriosExportacoesHttpIT extends AuditoriaHttpSuporte {
    @ParameterizedTest
    @ValueSource(strings = {"frequencia-consolidada", "absenteismo", "terceirizados"})
    void exportacaoSincronaXlsxPossuiConteudoSignificativoEIsolamento(String tipo) throws Exception {
        Cenario c = criarCenarioPersistido();
        // O controle nominal do arquivo exige fonte sintética conhecida no período.
        declararFonteNominalSintetica(c, "EXPORTACAO-SINCRONA-" + tipo);
        registrar(c);
        MvcResult r = executar(c.localA, parametros(get("/api/relatorios/" + tipo + "/exportacao"), tipo, c)
                .param("formato", "XLSX"), 200, "Exportação XLSX síncrona " + tipo);
        verificarXlsx(r, c);
    }

    @ParameterizedTest
    @ValueSource(strings = {"frequencia-consolidada", "absenteismo", "terceirizados"})
    void exportacaoAssincronaConcluiAutorizaProprietarioEEntregaArquivo(String tipo) throws Exception {
        Cenario c = criarCenarioPersistido();
        // O controle nominal do arquivo exige fonte sintética conhecida no período.
        declararFonteNominalSintetica(c, "EXPORTACAO-ASSINCRONA-" + tipo);
        registrar(c);
        JsonNode solicitada = dados(executar(c.localA,
                parametros(post("/api/relatorios/" + tipo + "/exportacao/solicitacao"), tipo, c)
                        .param("formato", "XLSX").param("totalEstimado", "1"),
                202, "Solicitação assíncrona aceita " + tipo));
        String id = solicitada.path("id").asText();
        assertDoesNotThrow(() -> UUID.fromString(id));
        assertEquals("XLSX", solicitada.path("formato").asText());
        assertFalse(solicitada.path("nomeArquivo").asText().isBlank());
        executar(c.localB, get("/api/relatorios/exportacoes/{id}", id), 403,
                "Outro usuário não lê progresso de exportação");
        executar(c.localB, get("/api/relatorios/exportacoes/{id}/arquivo", id), 403,
                "Outro usuário não baixa relatório");
        JsonNode pronta = aguardar(c, id);
        assertEquals("CONCLUIDO", pronta.path("status").asText());
        assertTrue(pronta.path("podeBaixar").asBoolean());
        assertTrue(pronta.path("tamanhoBytes").asLong() > 0);
        MvcResult arquivo = executar(c.localA, get("/api/relatorios/exportacoes/{id}/arquivo", id),
                200, "Download de exportação pronta " + tipo);
        verificarXlsx(arquivo, c);
        assertEquals(pronta.path("tamanhoBytes").asLong(), arquivo.getResponse().getContentAsByteArray().length);
        JsonNode baixada = dados(executar(c.localA, get("/api/relatorios/exportacoes/{id}", id), 200,
                "Download confirma consumo do arquivo temporário"));
        assertEquals("BAIXADO", baixada.path("status").asText());
        assertFalse(baixada.path("podeBaixar").asBoolean());
        executar(c.localA, get("/api/relatorios/exportacoes/{id}/arquivo", id), 409,
                "Arquivo já consumido exige uma nova exportação");
    }

    @Test
    void frequenciaPdfPossuiDocumentoLegivel() throws Exception {
        Cenario c = criarCenarioPersistido(); registrar(c);
        MvcResult r = executar(c.presidente,
                parametros(get("/api/relatorios/frequencia-consolidada/exportacao"), "frequencia-consolidada", c)
                        .param("formato", "PDF"), 200, "Presidência pode baixar representação PDF do relatório");
        assertEquals("application/pdf", r.getResponse().getContentType());
        PdfReader leitor = new PdfReader(r.getResponse().getContentAsByteArray());
        try { assertTrue(leitor.getNumberOfPages() >= 1); }
        finally { leitor.close(); }
        assertTrue(r.getResponse().getHeader("Content-Disposition").contains(".pdf"));
    }

    @Test
    void exportacoesRejeitamFormatosCompetenciasPerfilEIdentificadoresInvalidos() throws Exception {
        Cenario c = criarCenarioPersistido();
        for (String tipo : new String[]{"frequencia-consolidada", "absenteismo", "terceirizados"}) {
            executar(c.localA, parametros(get("/api/relatorios/" + tipo + "/exportacao"), tipo, c)
                    .param("formato", "CSV"), 400, "Formato não previsto para exportação síncrona " + tipo);
            executar(c.localA, parametros(post("/api/relatorios/" + tipo + "/exportacao/solicitacao"), tipo, c)
                    .param("formato", "CSV"), 400, "Formato não previsto para fila " + tipo);
            executar(c.operacionalA, parametros(get("/api/relatorios/" + tipo + "/exportacao"), tipo, c),
                    403, "Operacional não exporta relatórios gerenciais " + tipo);
            executar(c.operacionalA, parametros(post("/api/relatorios/" + tipo + "/exportacao/solicitacao"), tipo, c),
                    403, "Operacional não cria tarefa de exportação " + tipo);
        }
        executar(c.localA, get("/api/relatorios/absenteismo/exportacao").param("mes", "13").param("ano", "2026"),
                400, "Competência inválida antes de iniciar streaming");
        executar(c.localA, post("/api/relatorios/absenteismo/exportacao/solicitacao").param("mes", "13").param("ano", "2026"),
                400, "Competência inválida não entra na fila");
        executar(c.localA, get("/api/relatorios/exportacoes/{id}", UUID.randomUUID()), 404, "Tarefa inexistente");
        executar(c.localA, get("/api/relatorios/exportacoes/{id}/arquivo", UUID.randomUUID()), 404, "Arquivo inexistente");
        executar(c.localA, get("/api/relatorios/exportacoes/invalido"), 400, "Identificador deve ser UUID");
    }

    private AbstractMockHttpServletRequestBuilder<?> parametros(AbstractMockHttpServletRequestBuilder<?> r, String tipo, Cenario c) {
        if (tipo.equals("frequencia-consolidada")) return r.param("inicio", DATA.toString())
                .param("fim", DATA.toString()).param("contratoIds", c.contratoA.getId().toString());
        if (tipo.equals("absenteismo")) r.param("mes", "6").param("ano", "2026");
        return r.param("contratoId", c.contratoA.getId().toString());
    }
    private JsonNode aguardar(Cenario c, String id) throws Exception {
        long limite = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(25);
        JsonNode estado;
        do {
            estado = dados(executar(c.localA, get("/api/relatorios/exportacoes/{id}", id), 200,
                    "Consulta do progresso pelo solicitante"));
            assertFalse(estado.path("falhou").asBoolean(), estado.toString());
            if (estado.path("podeBaixar").asBoolean()) return estado;
            Thread.sleep(100);
        } while (System.nanoTime() < limite);
        return fail("Exportação não concluiu dentro de 25s: " + estado);
    }
    private void verificarXlsx(MvcResult r, Cenario c) throws Exception {
        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", r.getResponse().getContentType());
        assertTrue(r.getResponse().getHeader("Content-Disposition").contains(".xlsx"));
        assertEquals("no-store", r.getResponse().getHeader("Cache-Control"));
        byte[] bytes = r.getResponse().getContentAsByteArray();
        StringBuilder xml = new StringBuilder(); boolean workbook = false;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            for (ZipEntry e; (e = zip.getNextEntry()) != null;) {
                if (e.getName().equals("xl/workbook.xml")) workbook = true;
                if (e.getName().endsWith(".xml")) xml.append(new String(zip.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        assertTrue(workbook, "O arquivo precisa ter estrutura XLSX válida.");
        assertTrue(xml.toString().contains(c.pessoaA.getNome()), "A planilha contém a pessoa do escopo selecionado.");
        assertFalse(xml.toString().contains(c.pessoaB.getNome()), "A planilha não pode vazar pessoa da outra gestão.");
    }
    private void registrar(Cenario c) throws Exception {
        executar(c.localA, post("/api/frequencias").contentType(APPLICATION_JSON).content(json(Map.of(
                "vinculoContratualId", c.vinculoA.getId(), "dataReferencia", DATA.toString(), "status", "FALTA",
                "justificativaRetificacao", "Ausência documentada para relatório exportado",
                "chaveIdempotencia", "exportar-" + UUID.randomUUID()))), 200, "Fonte persistida antes do processamento assíncrono");
    }
}
