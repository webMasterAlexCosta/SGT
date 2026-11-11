package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.EmissaoRelatorioAdministrativo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** PER-011: a autorização nacional preserva indicadores sem expor a identidade pessoal. */
class RelatoriosMinimizacaoHttpIT extends AuditoriaHttpSuporte {
    @ParameterizedTest
    @ValueSource(strings = {"frequencia-consolidada", "frequencia-consolidada/paginada", "absenteismo",
            "absenteismo/paginado", "absenteismo/resumo", "horas-trabalhadas", "terceirizados/consolidado"})
    void presidenciaRecebeIdsEIndicadoresSemNomeCanarioEmConsultasEPaginas(String rota) throws Exception {
        Cenario c = criarCenario();
        String canario = configurarCanario(c);
        // A presença nominal do controle exige fonte sintética válida no período.
        declararFonteNominalSintetica(c, "MINIMIZACAO-CONSULTAS-" + canario);
        registrar(c);
        JsonNode local = dados(executar(c.localA, consulta(rota, c), 200,
                "Fonte nominal da Gestão Local antes de comparar minimização " + rota));
        JsonNode institucional = dados(executar(c.presidente, consulta(rota, c), 200,
                "PER-011 Presidência consulta indicadores sem identificação pessoal " + rota));
        JsonNode linhasLocais = linhas(local, rota);
        JsonNode linhasInstitucionais = linhas(institucional, rota);
        assertEquals(1, linhasLocais.size(), "O teste precisa de uma pessoa real no relatório.");
        assertEquals(canario, linhasLocais.get(0).path("nome").asText());
        assertEquals(1, linhasInstitucionais.size(), "Minimizar não pode eliminar a população apurada.");
        JsonNode linha = linhasInstitucionais.get(0);
        String campoId = rota.startsWith("terceirizados") ? "id" : "empregadoId";
        assertAll(
                () -> assertFalse(institucional.toString().contains(canario),
                        "Nome pessoal canário vazou na consulta institucional " + rota),
                () -> assertTrue(linha.path("nome").isNull() || linha.path("nome").isMissingNode(),
                        "A política institucional exige nome ausente/null."),
                () -> assertEquals(c.empregadoA.getId().longValue(), linha.path(campoId).asLong()),
                () -> assertEquals(semIdentidade(linhasLocais.get(0)), semIdentidade(linha),
                        "IDs, dimensões e totais permanecem iguais ao recorte local."),
                () -> assertFalse(linha.path("cpf").isTextual() && !linha.path("cpf").asText().isBlank()),
                () -> assertFalse(linha.path("matricula").isTextual() && !linha.path("matricula").asText().isBlank()));
        if (rota.equals("frequencia-consolidada")) {
            assertEquals(local.path("totais"), institucional.path("totais"));
            assertEquals(1, institucional.path("totais").path("faltas").asInt());
        }
        if (rota.equals("absenteismo/resumo")) {
            assertEquals(1, institucional.path("totalColaboradores").asInt());
            assertEquals(1, institucional.path("faltas").asInt());
        }
    }

    @ParameterizedTest
    @CsvSource({"FREQUENCIA_CONSOLIDADA,obter", "FREQUENCIA_CONSOLIDADA,comparar", "ABSENTEISMO,obter", "ABSENTEISMO,comparar"})
    void leituraInstitucionalDeEmissaoLocalMinimizaSemReescreverProva(String tipo, String operacao) throws Exception {
        Cenario c = criarCenario();
        String canario = configurarCanario(c);
        // A presença nominal do controle exige fonte sintética válida no período.
        declararFonteNominalSintetica(c, "MINIMIZACAO-CONSULTAS-" + canario);
        registrar(c);
        JsonNode original = dados(executar(c.localA, post("/api/relatorios/administrativos/emissoes")
                .contentType(APPLICATION_JSON).content(json(Map.of("tipo", tipo, "formato", "VISUALIZACAO",
                        "inicio", DATA.toString(), "fim", DATA.toString(), "contratoIds", List.of(c.contratoA.getId()),
                        "chaveIdempotencia", "emissao-nominal-" + UUID.randomUUID()))),
                201, "Emissão original contém a fotografia nominal autorizada à Gestão Local"));
        long id = original.path("metadados").path("id").asLong();
        String hashOriginal = original.path("metadados").path("hashConteudo").asText();
        assertTrue(original.path("conteudo").toString().contains(canario), "Canário precisa constar da prova original.");
        em.flush(); em.clear();
        byte[] gzipOriginal = em.find(EmissaoRelatorioAdministrativo.class, id).getConteudoSnapshotGzip().clone();
        JsonNode leitura = dados(executar(c.presidente, get("/api/relatorios/administrativos/emissoes/" + id
                        + (operacao.equals("comparar") ? "/comparacao-vigente" : "")),
                200, "PER-011 projeção autorizada da emissão local pela Presidência: " + operacao + " " + tipo));
        JsonNode historico = leitura.path(operacao.equals("comparar") ? "conteudoHistorico" : "conteudo");
        JsonNode vigente = operacao.equals("comparar") ? leitura.path("conteudoVigente") : historico;
        String hashRecebido = operacao.equals("comparar") ? leitura.path("hashHistorico").asText()
                : leitura.path("metadados").path("hashConteudo").asText();
        JsonNode localDepois = dados(executar(c.localA, get("/api/relatorios/administrativos/emissoes/{id}", id),
                200, "A Gestão Local continua recebendo exatamente a emissão original"));
        em.flush(); em.clear();
        EmissaoRelatorioAdministrativo armazenada = em.find(EmissaoRelatorioAdministrativo.class, id);
        String rota = tipo.equals("ABSENTEISMO") ? "absenteismo" : "frequencia-consolidada";
        assertEquals(1, linhas(historico, rota).size());
        assertEquals(1, linhas(vigente, rota).size());
        assertAll(
                () -> assertFalse(leitura.toString().contains(canario),
                        "A leitura/comparação institucional não pode devolver o JSON nominal armazenado."),
                () -> assertEquals(semIdentidade(linhas(original.path("conteudo"), rota).get(0)),
                        semIdentidade(linhas(historico, rota).get(0)), "A minimização mantém IDs e indicadores históricos."),
                () -> assertEquals(semIdentidade(linhas(original.path("conteudo"), rota).get(0)),
                        semIdentidade(linhas(vigente, rota).get(0)), "A apuração vigente mantém os mesmos indicadores."),
                () -> assertEquals(hashOriginal, hashRecebido, "O hash original continua identificando a emissão original."),
                () -> assertEquals(hashOriginal, armazenada.getHashConteudo()),
                () -> assertArrayEquals(gzipOriginal, armazenada.getConteudoSnapshotGzip(), "A leitura jamais regrava o snapshot."),
                () -> assertEquals(original.path("conteudo"), localDepois.path("conteudo")));
        if (operacao.equals("comparar")) {
            assertFalse(leitura.path("alterado").asBoolean(),
                    "Trocar o perfil leitor não constitui uma mudança dos dados do relatório.");
        }
    }

    private String configurarCanario(Cenario c) {
        String nome = "CANARIOPRESIDENCIA" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        c.pessoaA.setNome(nome); c.empregadoA.setNome(nome);
        em.flush(); return nome;
    }
    private void registrar(Cenario c) throws Exception {
        executar(c.localA, post("/api/frequencias").contentType(APPLICATION_JSON).content(json(Map.of(
                "vinculoContratualId", c.vinculoA.getId(), "dataReferencia", DATA.toString(), "status", "FALTA",
                "justificativaRetificacao", "Conferência documental para apuração institucional",
                "chaveIdempotencia", "minimizacao-" + UUID.randomUUID()))), 200, "Frequência real que torna o relatório não vazio");
    }
    private AbstractMockHttpServletRequestBuilder<?> consulta(String rota, Cenario c) {
        var r = get("/api/relatorios/" + rota);
        if (rota.startsWith("frequencia-consolidada")) return r.param("inicio", DATA.toString())
                .param("fim", DATA.toString()).param("contratoIds", c.contratoA.getId().toString());
        if (!rota.startsWith("terceirizados")) r.param("mes", "6").param("ano", "2026");
        return r.param("contratoId", c.contratoA.getId().toString());
    }
    private JsonNode linhas(JsonNode resposta, String rota) {
        if (rota.startsWith("terceirizados")) return resposta.path("empregados").path("content");
        if (rota.equals("absenteismo/resumo")) return resposta.path("maioresTaxas");
        return resposta.isArray() ? resposta : resposta.path("registros");
    }
    private JsonNode semIdentidade(JsonNode linha) {
        ObjectNode copia = linha.deepCopy();
        copia.remove(List.of("nome", "cpf", "matricula"));
        return copia;
    }
}
