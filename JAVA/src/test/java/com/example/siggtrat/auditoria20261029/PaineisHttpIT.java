package com.example.siggtrat.auditoria20261029;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

class PaineisHttpIT extends AuditoriaHttpSuporte {
    @Test
    void efetivoFiltrosEstatisticasAtividadesERankingRespeitamGestaoEContratos() throws Exception {
        Cenario c = criarCenario();
        registrar(c);
        JsonNode filtros = dados(executar(c.localA, get("/api/painel/efetivo/filtros")
                .param("referencia", DATA.toString()).param("periodo", "DIARIO"), 200,
                "Filtros de efetivo usam os catálogos do escopo e da data"));
        assertEquals(1, filtros.path("contratos").size());
        assertEquals(c.contratoA.getId().longValue(), filtros.path("contratos").get(0).path("id").asLong());
        assertEquals(c.lotacaoA.getId().longValue(), filtros.path("lotacoes").get(0).path("id").asLong());
        assertTrue(filtros.path("permiteDetalhamentoOperacional").asBoolean());
        JsonNode efetivo = dados(executar(c.localA, get("/api/painel/efetivo")
                .param("referencia", DATA.toString()).param("periodo", "DIARIO")
                .param("contratoId", c.contratoA.getId().toString()), 200,
                "Efetivo separa cinco vagas, um alocado e um lançamento"));
        assertEquals(5, efetivo.path("resumoAtual").path("efetivoPrevisto").asLong());
        assertEquals(1, efetivo.path("resumoAtual").path("efetivoAlocado").asLong());
        assertEquals(1, efetivo.path("resumoAtual").path("registrosLancados").asLong());
        assertEquals(1, efetivo.path("resumoAtual").path("presentes").asLong());
        assertEquals(4, efetivo.path("resumoAtual").path("deficitContratual").asLong());
        assertEquals(1, efetivo.path("serieAtual").size());
        assertEquals(DATA.toString(), efetivo.path("serieAtual").get(0).path("data").asText());
        JsonNode alheio = dados(executar(c.localB, get("/api/painel/efetivo")
                .param("referencia", DATA.toString()).param("periodo", "DIARIO")
                .param("contratoId", c.contratoA.getId().toString()), 200, "Contrato A não produz números no escopo B"));
        assertEquals(0, alheio.path("resumoAtual").path("efetivoAlocado").asLong());
        assertEquals(0, alheio.path("resumoAtual").path("efetivoPrevisto").asLong());
        JsonNode estatisticas = dados(executar(c.localA, get("/api/painel/estatisticas"), 200,
                "Estatísticas são restritas à Gestão Local"));
        assertEquals(1, estatisticas.path("empregados").asLong());
        assertEquals(1, estatisticas.path("ativos").asLong());
        assertEquals(1, estatisticas.path("lotacoes").asLong());
        JsonNode atividades = dados(executar(c.localA, get("/api/painel/atividades").param("size", "10"), 200,
                "Atividade da frequência exibe autor e colaborador do escopo"));
        assertFalse(atividades.path("content").isEmpty());
        for (JsonNode a : atividades.path("content")) {
            assertEquals(c.empregadoA.getId().longValue(), a.path("empregadoId").asLong());
            assertEquals(c.localA.getId().longValue(), a.path("usuarioId").asLong());
        }
        assertTrue(dados(executar(c.localB, get("/api/painel/atividades"), 200,
                "Atividade A não aparece na Gestão B")).path("content").isEmpty());
        JsonNode ranking = dados(executar(c.localA, get("/api/painel/ranking-lotacoes")
                .param("inicio", DATA.toString()).param("fim", DATA.toString())
                .param("contratoId", c.contratoA.getId().toString()), 200, "Ranking calcula presença no escopo"));
        assertEquals(1, ranking.size());
        assertEquals(c.lotacaoA.getId().longValue(), ranking.get(0).path("lotacaoId").asLong());
        assertEquals(1, ranking.get(0).path("presencas").asLong());
        assertEquals(0, ranking.get(0).path("faltas").asLong());
        JsonNode rankingB = dados(executar(c.localB, get("/api/painel/ranking-lotacoes")
                .param("inicio", DATA.toString()).param("fim", DATA.toString())
                .param("contratoId", c.contratoA.getId().toString()), 200,
                "Ranking B não recebe a lotação A"));
        for (JsonNode item : rankingB) {
            assertNotEquals(c.lotacaoA.getId().longValue(), item.path("lotacaoId").asLong());
            assertEquals(0, item.path("presencas").asLong());
            assertEquals(0, item.path("faltas").asLong());
            assertEquals(0, item.path("escalados").asLong());
        }
    }

    @Test
    @org.springframework.transaction.annotation.Transactional(
            propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void gestaoEVisaoPresidenciaPossuemContratoConsolidadoSemDetalhamentoOperacional() throws Exception {
        Cenario c = criarCenarioPersistido(); registrar(c);
        JsonNode gestao = dados(executar(c.localA, get("/api/painel/gestao")
                .param("referencia", DATA.toString()).param("periodoDias", "7"), 200,
                "Painel gerencial informa escopo, cobertura e série do período"));
        assertEquals(c.gestaoA.getId().longValue(), gestao.path("escopo").path("gestaoLocalId").asLong());
        assertEquals(1, gestao.path("quadro").path("empregadosCadastrados").asLong());
        assertEquals(DATA.toString(), gestao.path("referencia").asText());
        assertEquals(7, gestao.path("periodo").path("dias").asInt());
        assertEquals(7, gestao.path("evolucao").size());
        JsonNode presidente = dados(executar(c.presidente, get("/api/painel/gestao")
                .param("referencia", DATA.toString()).param("periodoDias", "7"), 200,
                "Presidência consulta consolidado nacional por Gestão Estadual"));
        assertEquals("NACIONAL", presidente.path("escopo").path("nivel").asText());
        assertFalse(presidente.path("escopo").path("permiteDetalhamentoOperacional").asBoolean());
        JsonNode filtros = dados(executar(c.presidente, get("/api/painel/efetivo/filtros")
                .param("referencia", DATA.toString()), 200, "Filtros da Presidência minimizam dimensões operacionais"));
        assertFalse(filtros.path("permiteDetalhamentoOperacional").asBoolean());
        assertTrue(filtros.path("lotacoes").isEmpty());
        assertTrue(filtros.path("turnos").isEmpty());
        assertTrue(filtros.path("cargos").isEmpty());
        assertTrue(dados(executar(c.presidente, get("/api/painel/atividades"), 200,
                "Presidência não recebe nomes dos operadores")).path("content").isEmpty());
        executar(c.presidente, get("/api/painel/estatisticas"), 403, "Detalhamento cadastral exige perfil operacional de gestão");
        executar(c.presidente, get("/api/painel/ranking-lotacoes").param("inicio", DATA.toString())
                .param("fim", DATA.toString()), 403, "Presidência não acessa ranking por lotação");
    }

    @Test
    void paineisRejeitamPeriodosInvalidosDatasFuturasEPerfilOperacional() throws Exception {
        Cenario c = criarCenario();
        executar(c.localA, get("/api/painel/efetivo").param("periodo", "INVENTADO"), 400, "Período do efetivo inválido");
        executar(c.localA, get("/api/painel/efetivo/filtros").param("referencia", LocalDate.now().plusDays(1).toString()),
                400, "Filtros não aceitam referência futura");
        executar(c.localA, get("/api/painel/gestao").param("periodoDias", "8"), 400, "Janela gerencial não suportada");
        executar(c.localA, get("/api/painel/ranking-lotacoes").param("inicio", "invalida").param("fim", DATA.toString()),
                400, "Ranking rejeita data malformada");
        executar(c.localA, get("/api/painel/atividades").param("size", "invalido"), 400, "Paginação de atividades inválida");
        for (String rota : new String[]{"/efetivo", "/efetivo/filtros", "/gestao", "/estatisticas", "/atividades"}) {
            executar(c.operacionalA, get("/api/painel" + rota), 403, "Operacional não acessa painel de gestão " + rota);
        }
    }

    private void registrar(Cenario c) throws Exception {
        executar(c.localA, post("/api/frequencias").contentType(APPLICATION_JSON).content(json(Map.of(
                "vinculoContratualId", c.vinculoA.getId(), "dataReferencia", DATA.toString(), "status", "PRESENCA",
                "justificativaRetificacao", "Presença documental para indicador gerencial",
                "chaveIdempotencia", "painel-" + UUID.randomUUID()))), 200, "Presença canônica de oito horas");
    }
}
