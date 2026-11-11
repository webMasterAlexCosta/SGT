package com.example.siggtrat.regra;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class MaterialHashEventosTest {
    private enum Simbolo { CRIACAO, ALTERACAO_MATRICULA, RETIFICACAO_INICIO, DESLIGAMENTO,
        DESLIGAMENTO_INDIVIDUAL, TRANSFERENCIA_GESTAO, REGISTRADA }

    @Test
    void bytesEDigestsCorrespondemAos28VetoresIndependentesDeFronteira() throws Exception {
        var entrada = getClass().getResourceAsStream("/hash-eventos-tipado-v1-vetores.json");
        assertNotNull(entrada);
        JsonNode vetores;
        try (entrada) { vetores = new ObjectMapper().readTree(entrada).path("vectors"); }
        assertEquals(28, vetores.size());
        Map<String, String> hashes = new LinkedHashMap<>();
        for (JsonNode vetor : vetores) {
            MaterialHashEventos.Campo[] campos = campos(vetor.path("campos"));
            String dominio = vetor.path("dominio").asText();
            assertArrayEquals(HexFormat.of().parseHex(vetor.path("materialHex").asText()),
                    MaterialHashEventos.material(dominio, campos), vetor.path("id").asText());
            assertEquals(vetor.path("sha256").asText(), MaterialHashEventos.sha256(dominio, campos), vetor.path("id").asText());
            hashes.put(vetor.path("id").asText(), vetor.path("sha256").asText());
        }
        assertNotEquals(hashes.get("V_COMPENSATORIO_A"), hashes.get("V_COMPENSATORIO_B"));
        assertNotEquals(hashes.get("A_COMPENSATORIO_A"), hashes.get("A_COMPENSATORIO_B"));
        assertNotEquals(hashes.get("NULO_TEXTO"), hashes.get("VAZIO"));
        assertNotEquals(hashes.get("NULO_TEXTO"), hashes.get("LITERAL_ANGULOS"));
        assertNotEquals(hashes.get("NULO_TEXTO"), hashes.get("NULO_INT64"));
        assertNotEquals(hashes.get("TEXTO_1"), hashes.get("INT32_1"));
        assertNotEquals(hashes.get("GRUPO_NULO"), hashes.get("GRUPO_VAZIO"));
        assertNotEquals(hashes.get("DOMINIO_A"), hashes.get("DOMINIO_B"));
    }

    @Test
    void textoUnicodeMalformadoNaoEReescritoSilenciosamenteParaOutroValor() {
        String malformado = String.valueOf((char) 0xD800);
        var erro = assertThrows(IllegalArgumentException.class, () ->
                MaterialHashEventos.material("sgt.fixture.fronteiras", MaterialHashEventos.texto(malformado)));
        assertEquals("SGT_HASH_EVENTOS_UTF8_INVALIDO", erro.getMessage());
    }

    private MaterialHashEventos.Campo[] campos(JsonNode nodes) {
        MaterialHashEventos.Campo[] campos = new MaterialHashEventos.Campo[nodes.size()];
        for (int i = 0; i < nodes.size(); i++) {
            JsonNode f = nodes.get(i), v = f.path("valor");
            campos[i] = switch (f.path("tipo").asText()) {
                case "TEXTO" -> MaterialHashEventos.texto(v.isNull() ? null : v.asText());
                case "INT64" -> MaterialHashEventos.longo(v.isNull() ? null : v.asLong());
                case "INT32" -> MaterialHashEventos.inteiro(v.isNull() ? null : v.asInt());
                case "DATA" -> MaterialHashEventos.data(v.isNull() ? null : LocalDate.parse(v.asText()));
                case "ENUM" -> MaterialHashEventos.enumeracao(v.isNull() ? null : Simbolo.valueOf(v.asText()));
                case "GRUPO" -> MaterialHashEventos.grupo(v.isNull() ? null : campos(v));
                default -> throw new AssertionError("Tipo desconhecido na fixture");
            };
        }
        return campos;
    }
}
