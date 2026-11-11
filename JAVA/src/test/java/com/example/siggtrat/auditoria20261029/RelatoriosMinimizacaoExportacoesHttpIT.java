package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.Empregado;
import com.example.siggtrat.entidade.Pessoa;
import com.fasterxml.jackson.databind.JsonNode;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Exportadores e executores reais, com fatos commitados antes de abrir o arquivo. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RelatoriosMinimizacaoExportacoesHttpIT extends AuditoriaHttpSuporte {
    @ParameterizedTest
    @CsvSource({"frequencia-consolidada,XLSX", "absenteismo,XLSX", "terceirizados,XLSX",
            "frequencia-consolidada,PDF", "absenteismo,PDF", "terceirizados,PDF"})
    void arquivosInstitucionaisMantemConteudoSemNomeCanario(String tipo, String formato) throws Exception {
        Cenario c = criarCenarioPersistido();
        String canario = configurarCanario(c);
        // Controle nominal explícito: o mestre atual não prova o nome do período exportado.
        declararFonteNominalSintetica(c, "EXPORTACAO-MINIMIZACAO-" + tipo + "-" + canario);
        registrar(c);
        MvcResult local = executar(c.localA, parametros(get("/api/relatorios/" + tipo + "/exportacao"), tipo, c)
                .param("formato", formato), 200, "Arquivo local possui conteúdo nominal de referência " + tipo + " " + formato);
        MvcResult institucional = executar(c.presidente, parametros(get("/api/relatorios/" + tipo + "/exportacao"), tipo, c)
                .param("formato", formato), 200, "PER-011 arquivo institucional minimizado " + tipo + " " + formato);
        String textoLocal = texto(local, formato);
        String textoInstitucional = texto(institucional, formato);
        assertTrue(compacto(textoLocal).contains(canario), "A exportação de controle deve conter o nome canário.");
        assertAll(
                () -> assertFalse(compacto(textoInstitucional).contains(canario),
                        "A exportação institucional vazou identificação pessoal " + tipo + " " + formato),
                () -> assertTrue(textoInstitucional.length() > 100, "A exportação minimizada continua legível e não vazia."),
                () -> assertTrue(compacto(textoInstitucional).contains(compacto(c.lotacaoA.getNome())),
                        "O arquivo preserva a dimensão de lotação e o recorte apurado."),
                () -> assertEquals(local.getResponse().getContentType(), institucional.getResponse().getContentType()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"frequencia-consolidada", "absenteismo"})
    void filaDeExportacaoPreservaMinimizacaoDoSolicitanteInstitucional(String tipo) throws Exception {
        Cenario c = criarCenarioPersistido();
        String canario = configurarCanario(c);
        // Controle nominal explícito: o mestre atual não prova o nome do período exportado.
        declararFonteNominalSintetica(c, "EXPORTACAO-MINIMIZACAO-" + tipo + "-" + canario);
        registrar(c);
        JsonNode solicitada = dados(executar(c.presidente,
                parametros(post("/api/relatorios/" + tipo + "/exportacao/solicitacao"), tipo, c)
                        .param("formato", "XLSX").param("totalEstimado", "1"),
                202, "PER-011 fila real conserva o perfil institucional do solicitante " + tipo));
        String id = solicitada.path("id").asText();
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(25);
        boolean pronta = false;
        do {
            JsonNode estado = dados(executar(c.presidente, get("/api/relatorios/exportacoes/{id}", id), 200,
                    "Consulta institucional da tarefa de exportação " + tipo));
            assertFalse(estado.path("falhou").asBoolean(), estado.toString());
            if (estado.path("podeBaixar").asBoolean()) { pronta = true; break; }
            Thread.sleep(100);
        } while (System.nanoTime() < limite);
        assertTrue(pronta, "Exportação deve concluir dentro de 25 segundos.");
        String texto = texto(executar(c.presidente, get("/api/relatorios/exportacoes/{id}/arquivo", id),
                200, "Download institucional após execução assíncrona real " + tipo), "XLSX");
        assertAll(
                () -> assertFalse(compacto(texto).contains(canario), "O worker não pode elevar a exposição dos dados pessoais."),
                () -> assertTrue(texto.length() > 100),
                () -> assertTrue(compacto(texto).contains(compacto(c.lotacaoA.getNome()))));
    }

    private String configurarCanario(Cenario c) {
        String nome = "CANARIOPRESIDENCIA" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        emTransacao(() -> {
            em.find(Pessoa.class, c.pessoaA.getId()).setNome(nome);
            em.find(Empregado.class, c.empregadoA.getId()).setNome(nome);
            return null;
        });
        return nome;
    }
    private void registrar(Cenario c) throws Exception {
        executar(c.localA, post("/api/frequencias").contentType(APPLICATION_JSON).content(json(Map.of(
                "vinculoContratualId", c.vinculoA.getId(), "dataReferencia", DATA.toString(), "status", "FALTA",
                "justificativaRetificacao", "Conferência documental para exportação institucional",
                "chaveIdempotencia", "minimizacao-arquivo-" + UUID.randomUUID()))), 200, "Fonte commitada para exportação real");
    }
    private AbstractMockHttpServletRequestBuilder<?> parametros(AbstractMockHttpServletRequestBuilder<?> r, String tipo, Cenario c) {
        if (tipo.equals("frequencia-consolidada")) return r.param("inicio", DATA.toString())
                .param("fim", DATA.toString()).param("contratoIds", c.contratoA.getId().toString());
        if (tipo.equals("absenteismo")) r.param("mes", "6").param("ano", "2026");
        return r.param("contratoId", c.contratoA.getId().toString());
    }
    private String texto(MvcResult r, String formato) throws Exception {
        byte[] bytes = r.getResponse().getContentAsByteArray();
        if (formato.equals("PDF")) {
            assertEquals("application/pdf", r.getResponse().getContentType());
            PdfReader leitor = new PdfReader(bytes);
            try {
                assertTrue(leitor.getNumberOfPages() > 0);
                PdfTextExtractor extrator = new PdfTextExtractor(leitor);
                StringBuilder texto = new StringBuilder();
                for (int p = 1; p <= leitor.getNumberOfPages(); p++) texto.append(extrator.getTextFromPage(p));
                return texto.toString();
            } finally { leitor.close(); }
        }
        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", r.getResponse().getContentType());
        StringBuilder xml = new StringBuilder(); boolean workbook = false;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            for (ZipEntry e; (e = zip.getNextEntry()) != null;) {
                if (e.getName().equals("xl/workbook.xml")) workbook = true;
                if (e.getName().endsWith(".xml")) xml.append(new String(zip.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        assertTrue(workbook, "O arquivo deve ser um XLSX válido.");
        return xml.toString();
    }
    private String compacto(String texto) { return texto.replaceAll("\\s+", ""); }
}
