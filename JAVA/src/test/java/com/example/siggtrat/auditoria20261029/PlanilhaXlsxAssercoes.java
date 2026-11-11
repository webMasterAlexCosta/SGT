package com.example.siggtrat.auditoria20261029;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;
import static org.junit.jupiter.api.Assertions.*;

/** Leitura limitada de células OOXML reais, sem dependência adicional. */
final class PlanilhaXlsxAssercoes {
    private PlanilhaXlsxAssercoes() {}
    static List<List<String>> linhasXlsx(byte[] bytes) throws Exception {
        Map<String, byte[]> membros = new HashMap<>();
        int total = 0;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            for (var entrada = zip.getNextEntry(); entrada != null; entrada = zip.getNextEntry()) {
                String nome = entrada.getName();
                assertFalse(nome.startsWith("/") || Arrays.asList(nome.split("/")).contains(".."));
                assertTrue(membros.size() < 64, "Número limitado de membros do XLSX de teste");
                byte[] conteudo = zip.readNBytes(4 * 1024 * 1024 + 1);
                assertTrue(conteudo.length <= 4 * 1024 * 1024);
                total += conteudo.length; assertTrue(total <= 8 * 1024 * 1024);
                assertNull(membros.put(nome, conteudo), "Membro ZIP repetido");
            }
        }
        assertTrue(membros.containsKey("[Content_Types].xml"));
        assertEquals(1, xml(membros.get("xl/workbook.xml")).getElementsByTagNameNS("*", "sheet").getLength());
        List<String> compartilhadas = new ArrayList<>();
        if (membros.containsKey("xl/sharedStrings.xml")) {
            var itens = xml(membros.get("xl/sharedStrings.xml")).getElementsByTagNameNS("*", "si");
            for (int i = 0; i < itens.getLength(); i++) compartilhadas.add(textos((Element) itens.item(i), "t"));
        }
        var linhas = xml(membros.get("xl/worksheets/sheet1.xml")).getElementsByTagNameNS("*", "row");
        List<List<String>> resultado = new ArrayList<>();
        for (int n = 0; n < linhas.getLength(); n++) {
            List<String> linha = new ArrayList<>();
            for (Node no = linhas.item(n).getFirstChild(); no != null; no = no.getNextSibling()) {
                if (!(no instanceof Element celula) || !celula.getLocalName().equals("c")) continue;
                String endereco = celula.getAttribute("r"); assertTrue(endereco.matches("[A-Z]+[1-9][0-9]*"));
                int coluna = 0;
                for (char letra : endereco.replaceAll("[0-9]", "").toCharArray()) coluna = coluna * 26 + letra - 'A' + 1;
                assertTrue(coluna <= 64);
                while (linha.size() < coluna) linha.add("");
                String tipo = celula.getAttribute("t"), valor = textos(celula, tipo.equals("inlineStr") ? "t" : "v");
                if (tipo.equals("s")) valor = compartilhadas.get(Integer.parseInt(valor));
                linha.set(coluna - 1, valor);
            }
            resultado.add(linha);
        }
        return resultado;
    }

    private static org.w3c.dom.Document xml(byte[] bytes) throws Exception {
        assertNotNull(bytes, "Parte obrigatória do XLSX");
        DocumentBuilderFactory fabrica = DocumentBuilderFactory.newInstance();
        fabrica.setNamespaceAware(true); fabrica.setXIncludeAware(false); fabrica.setExpandEntityReferences(false);
        fabrica.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        fabrica.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        fabrica.setFeature("http://xml.org/sax/features/external-general-entities", false);
        fabrica.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        fabrica.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, ""); fabrica.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return fabrica.newDocumentBuilder().parse(new ByteArrayInputStream(bytes));
    }

    private static String textos(Element elemento, String local) {
        StringBuilder valor = new StringBuilder(); var partes = elemento.getElementsByTagNameNS("*", local);
        for (int i = 0; i < partes.getLength(); i++) valor.append(partes.item(i).getTextContent());
        return valor.toString();
    }

}
