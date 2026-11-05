package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.Frequencia;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * Memória manual deve permanecer coerente em todos os consumidores. A fixture e todas as escritas passam pelo
 * banco MySQL real. O modo manual inclui zero legítimo e redução noturna;
 * PRESENCA_PARCIAL é controle do caminho que já usa a memória gravada.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class FrequenciaMemoriaManualAuditoriaHttpIT extends AuditoriaHttpSuporte {
    @ParameterizedTest(name = "{0}: {1} físicas, saída {2}; memória preservada em todos os consumidores")
    @CsvSource({
            "PRESENCA,04:00,12:00,240,0,240",
            "PRESENCA,00:00,12:00,0,0,0",
            "RT,04:00,12:00,240,0,240",
            "RT,07:00,05:00,420,420,480",
            "PRESENCA_PARCIAL,07:00,05:00,420,420,480"
    })
    void memoriaManualPersistidaPrevaleceSobreJornadaEmConsultaEmissaoEArquivo(
            String status, String duracao, String saida, int fisicos, int noturnos, int pagaveis) throws Exception {
        Cenario c = criarCenarioPersistido();
        // A linha nominal de controle exige fonte do período, além dos minutos apurados.
        declararFonteNominalSintetica(c, "MEMORIA-MANUAL-" + status + "-" + duracao + "-" + saida);
        JsonNode criada = dados(executar(c.localA, post("/api/frequencias")
                        .contentType(APPLICATION_JSON).content(json(Map.of(
                                "vinculoContratualId", c.vinculoA.getId(), "dataReferencia", DATA.toString(),
                                "status", status, "horasTrabalhadas", duracao, "horarioSaida", saida,
                                "justificativaRetificacao", "Horas manuais documentadas para a jornada histórica",
                                "chaveIdempotencia", "memoria-manual-" + UUID.randomUUID()))),
                200, "Fonte canônica manual sem batidas, com memória persistida"));
        long frequenciaId = criada.path("id").asLong();
        List<Integer> memoria = emTransacao(() -> {
            Frequencia f = em.find(Frequencia.class, frequenciaId);
            assertNull(f.getEntrada1()); assertNull(f.getSaida1());
            assertNotNull(f.getCalculoVersao());
            assertEquals(480, com.example.siggtrat.utilitario.UtilitarioHorario.calcularIntervaloMinutos(
                    f.getJornadaSnapshotEntrada1(), f.getJornadaSnapshotSaida1(), "entrada", "saída")
                    + com.example.siggtrat.utilitario.UtilitarioHorario.calcularIntervaloMinutos(
                    f.getJornadaSnapshotEntrada2(), f.getJornadaSnapshotSaida2(), "entrada", "saída"));
            return List.of(f.getMinutosFisicos(), f.getMinutosNoturnosReais(), f.getMinutosPagaveis());
        });
        assertEquals(List.of(fisicos, noturnos, pagaveis), memoria,
                "A fonte precisa estar correta antes de verificar seus consumidores.");

        JsonNode consolidado = dados(executar(c.localA, get("/api/relatorios/frequencia-consolidada")
                        .param("inicio", DATA.toString()).param("fim", DATA.toString())
                        .param("contratoIds", c.contratoA.getId().toString()),
                200, "REL-001: consolidado deve reproduzir memória manual"));
        JsonNode paginado = dados(executar(c.localA, get("/api/relatorios/frequencia-consolidada/paginada")
                        .param("inicio", DATA.toString()).param("fim", DATA.toString())
                        .param("contratoIds", c.contratoA.getId().toString()),
                200, "REL-001: página deve usar a mesma memória do lançamento"));
        JsonNode emissao = dados(executar(c.localA, post("/api/relatorios/administrativos/emissoes")
                        .contentType(APPLICATION_JSON).content(json(Map.of(
                                "tipo", "FREQUENCIA_CONSOLIDADA", "formato", "VISUALIZACAO",
                                "inicio", DATA.toString(), "fim", DATA.toString(),
                                "contratoIds", List.of(c.contratoA.getId()),
                                "chaveIdempotencia", "emissao-manual-" + UUID.randomUUID()))),
                201, "REL-005: fotografia formal preserva resultado manual"));
        byte[] xlsx = executar(c.localA, get("/api/relatorios/frequencia-consolidada/exportacao")
                        .param("inicio", DATA.toString()).param("fim", DATA.toString())
                        .param("contratoIds", c.contratoA.getId().toString()).param("formato", "XLSX"),
                200, "Arquivo XLSX: conferir valores, além de estrutura e nome").getResponse().getContentAsByteArray();
        List<List<String>> linhas = linhasXlsx(xlsx);
        List<String> cabecalho = linhas.stream().filter(l -> l.contains("COLABORADOR") && l.contains("H.REAL"))
                .findFirst().orElseThrow();
        List<String> linha = linhas.stream().filter(l -> !l.isEmpty() && c.pessoaA.getNome().equals(l.get(0)))
                .findFirst().orElseThrow();
        int noturnosReduzidos = noturnos == 0 ? 0 : (int) Math.round(noturnos * 60.0 / 52.5);

        assertAll("A escala de 8 horas não substitui uma duração manual já apurada",
                () -> conferirDia(consolidado, fisicos, noturnos, pagaveis),
                () -> conferirDia(paginado, fisicos, noturnos, pagaveis),
                () -> conferirDia(emissao.path("conteudo"), fisicos, noturnos, pagaveis),
                () -> assertEquals(horas(fisicos), linha.get(cabecalho.indexOf("H.REAL")), "XLSX H.REAL"),
                () -> assertEquals(horas(noturnosReduzidos), linha.get(cabecalho.indexOf("H.NOT")), "XLSX H.NOT"),
                () -> assertEquals(horas(pagaveis), linha.get(cabecalho.indexOf("H.PAG")), "XLSX H.PAG"));
    }

    private void conferirDia(JsonNode relatorio, int fisicos, int noturnos, int pagaveis) {
        assertEquals(1, relatorio.path("registros").size());
        JsonNode dia = relatorio.path("registros").get(0).path("diario").path(DATA.toString());
        assertAll(() -> assertEquals(fisicos, dia.path("minutosFisicos").asInt()),
                () -> assertEquals(noturnos, dia.path("minutosNoturnosReais").asInt()),
                () -> assertEquals(pagaveis, dia.path("minutosPagaveis").asInt()));
    }

    private String horas(int minutos) { return String.format("%02d:%02d", minutos / 60, minutos % 60); }

    private List<List<String>> linhasXlsx(byte[] bytes) throws Exception {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            for (ZipEntry entrada; (entrada = zip.getNextEntry()) != null;) {
                if (!entrada.getName().equals("xl/worksheets/sheet1.xml")) continue;
                DocumentBuilderFactory fabrica = DocumentBuilderFactory.newInstance();
                fabrica.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                var documento = fabrica.newDocumentBuilder().parse(new ByteArrayInputStream(zip.readAllBytes()));
                var rows = documento.getElementsByTagName("row");
                List<List<String>> resultado = new ArrayList<>();
                for (int r = 0; r < rows.getLength(); r++) {
                    var cells = ((Element) rows.item(r)).getElementsByTagName("c");
                    List<String> valores = new ArrayList<>();
                    for (int i = 0; i < cells.getLength(); i++) {
                        Element cell = (Element) cells.item(i);
                        var textos = cell.getElementsByTagName("t");
                        var numeros = cell.getElementsByTagName("v");
                        valores.add(textos.getLength() > 0 ? textos.item(0).getTextContent()
                                : numeros.getLength() > 0 ? numeros.item(0).getTextContent() : "");
                    }
                    resultado.add(valores);
                }
                return resultado;
            }
        }
        return fail("Planilha não contém sheet1.xml.");
    }
}
