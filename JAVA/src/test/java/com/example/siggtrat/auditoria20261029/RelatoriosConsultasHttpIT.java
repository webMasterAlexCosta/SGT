package com.example.siggtrat.auditoria20261029;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

class RelatoriosConsultasHttpIT extends AuditoriaHttpSuporte {
    @Test
    void filtrosConsolidacaoPaginaEHorasMantemMesmaFonteTemporalEQuantidade() throws Exception {
        Cenario c = criarCenario(); registrar(c);
        JsonNode filtros = dados(executar(c.localA, get("/api/relatorios/filtros")
                .param("inicio", DATA.toString()).param("fim", DATA.plusDays(1).toString()), 200,
                "Filtros históricos de contratos e dimensões"));
        assertEquals(1, filtros.path("contratos").size());
        assertEquals(c.contratoA.getId().longValue(), filtros.path("contratos").get(0).path("id").asLong());
        assertEquals(c.lotacaoA.getId().longValue(), filtros.path("lotacoes").get(0).path("id").asLong());
        JsonNode completo = dados(executar(c.localA, get("/api/relatorios/frequencia-consolidada")
                .param("inicio", DATA.toString()).param("fim", DATA.plusDays(1).toString())
                .param("contratoIds", c.contratoA.getId().toString()), 200, "Consolidado tem uma presença e uma falta"));
        assertEquals(1, completo.path("registros").size());
        JsonNode linha = completo.path("registros").get(0);
        assertEquals(c.empregadoA.getId().longValue(), linha.path("empregadoId").asLong());
        assertEquals(c.contratoA.getId().longValue(), linha.path("contratoId").asLong());
        assertEquals(c.lotacaoA.getId().longValue(), linha.path("lotacaoId").asLong());
        assertEquals("PRESENCA", linha.path("diario").path(DATA.toString()).path("status").asText());
        assertEquals(480, linha.path("diario").path(DATA.toString()).path("minutosPagaveis").asInt());
        assertEquals("FALTA", linha.path("diario").path(DATA.plusDays(1).toString()).path("status").asText());
        assertEquals(1, completo.path("totais").path("faltas").asInt());
        assertEquals(1, completo.path("totais").path("presencas").asInt());
        assertEquals(960, completo.path("totais").path("minutosPrevistos").asInt());
        JsonNode pagina = dados(executar(c.estadualA, get("/api/relatorios/frequencia-consolidada/paginada")
                .param("inicio", DATA.toString()).param("fim", DATA.plusDays(1).toString())
                .param("contratoIds", c.contratoA.getId().toString()).param("size", "1"), 200,
                "Paginação por pessoa mantém o mesmo conteúdo e totais da linha"));
        assertEquals(1, pagina.path("totalColaboradores").asLong());
        assertEquals(1, pagina.path("totalPaginas").asInt());
        assertEquals(linha, pagina.path("registros").get(0));
        JsonNode horas = dados(executar(c.estadualA, get("/api/relatorios/horas-trabalhadas")
                .param("mes", "6").param("ano", "2026").param("contratoId", c.contratoA.getId().toString()), 200,
                "Relatório de horas agrega a mesma frequência canônica"));
        assertEquals(1, horas.size());
        assertEquals("08:00", horas.get(0).path("totalHorasTrabalhadas").asText());
        assertEquals(1, horas.get(0).path("totalPresencas").asInt());
        for (String rota : new String[]{"/frequencia-consolidada", "/frequencia-consolidada/paginada"}) {
            executar(c.localB, get("/api/relatorios" + rota).param("inicio", DATA.toString()).param("fim", DATA.toString())
                    .param("contratoIds", c.contratoA.getId().toString()), 404, "Contrato alheio não é consultável " + rota);
        }
        executar(c.localB, get("/api/relatorios/horas-trabalhadas").param("mes", "6").param("ano", "2026")
                .param("contratoId", c.contratoA.getId().toString()), 404, "Horas não revelam contrato de outra Gestão");
    }

    @Test
    void absenteismoListaPaginaEResumoConcordamNaAusenciaEPopulacao() throws Exception {
        Cenario c = criarCenario(); registrar(c);
        JsonNode lista = dados(executar(c.localA, get("/api/relatorios/absenteismo").param("mes", "6").param("ano", "2026")
                .param("contratoId", c.contratoA.getId().toString()), 200, "Absenteísmo detalhado possui falta documentada"));
        assertEquals(1, lista.size());
        assertEquals(c.empregadoA.getId().longValue(), lista.get(0).path("empregadoId").asLong());
        assertEquals(1, lista.get(0).path("faltas").asInt());
        assertEquals(0, lista.get(0).path("atestados").asInt());
        assertTrue(lista.get(0).path("minutosPrevistos").asInt() >= 960);
        assertTrue(lista.get(0).path("taxaIndividual").asDouble() > 0);
        JsonNode pagina = dados(executar(c.localA, get("/api/relatorios/absenteismo/paginado")
                .param("mes", "6").param("ano", "2026").param("contratoId", c.contratoA.getId().toString())
                .param("size", "1"), 200, "Página mantém a população e os indicadores por pessoa"));
        assertEquals(1, pagina.path("totalColaboradores").asLong());
        assertEquals(lista.get(0), pagina.path("registros").get(0));
        JsonNode resumo = dados(executar(c.localA, get("/api/relatorios/absenteismo/resumo")
                .param("mes", "6").param("ano", "2026").param("contratoId", c.contratoA.getId().toString()), 200,
                "Resumo utiliza toda população, sem limitar ao tamanho de página"));
        assertEquals(1, resumo.path("totalColaboradores").asLong());
        assertEquals(1, resumo.path("faltas").asLong());
        assertEquals(1, resumo.path("maioresTaxas").size());
        for (String rota : new String[]{"/absenteismo", "/absenteismo/paginado", "/absenteismo/resumo"}) {
            executar(c.estadualB, get("/api/relatorios" + rota).param("mes", "6").param("ano", "2026")
                    .param("contratoId", c.contratoA.getId().toString()), 404, "Estado alheio não recebe absenteísmo " + rota);
        }
    }

    @Test
    void consolidadoDeTerceirizadosTemPaginaTotaisEDadosMinimizadosPorPerfil() throws Exception {
        Cenario c = criarCenario();
        JsonNode local = dados(executar(c.localA, get("/api/relatorios/terceirizados/consolidado")
                .param("contratoId", c.contratoA.getId().toString()), 200, "Relatório cadastral recorta Gestão Local"));
        assertEquals(1, local.path("indicadores").path("empregados").asLong());
        assertEquals(1, local.path("indicadores").path("ativos").asLong());
        assertEquals(1, local.path("empregados").path("totalElements").asLong());
        JsonNode pessoa = local.path("empregados").path("content").get(0);
        assertEquals(c.empregadoA.getId().longValue(), pessoa.path("id").asLong());
        assertEquals(c.pessoaA.getNome(), pessoa.path("nome").asText());
        assertEquals(c.lotacaoA.getId().longValue(), pessoa.path("lotacaoId").asLong());
        JsonNode alheio = dados(executar(c.localB, get("/api/relatorios/terceirizados/consolidado")
                .param("contratoId", c.contratoA.getId().toString()), 200, "Filtro de contrato não amplia escopo cadastral"));
        assertEquals(0, alheio.path("indicadores").path("empregados").asLong());
        assertTrue(alheio.path("empregados").path("content").isEmpty());
        JsonNode nacional = dados(executar(c.presidente, get("/api/relatorios/terceirizados/consolidado")
                .param("contratoId", c.contratoA.getId().toString()), 200, "Presidência consulta o relatório autorizado"));
        assertEquals(1, nacional.path("indicadores").path("empregados").asLong());
        String cpf = nacional.path("empregados").path("content").get(0).path("cpf").asText();
        assertNotEquals(c.pessoaA.getCpf(), cpf, "A visão nacional não expõe CPF completo por padrão.");
    }

    @Test
    void consultasRejeitamParametrosInvalidosEPerfisOperacionais() throws Exception {
        Cenario c = criarCenario();
        executar(c.localA, get("/api/relatorios/filtros").param("inicio", "data-invalida"), 400, "Data de filtro malformada");
        for (String rota : new String[]{"/frequencia-consolidada", "/frequencia-consolidada/paginada"}) {
            executar(c.localA, get("/api/relatorios" + rota).param("inicio", DATA.plusDays(1).toString())
                    .param("fim", DATA.toString()), 400, "Período invertido " + rota);
            executar(c.operacionalA, get("/api/relatorios" + rota).param("inicio", DATA.toString()).param("fim", DATA.toString()),
                    403, "Operacional não consulta relatório de gestão " + rota);
        }
        for (String rota : new String[]{"/absenteismo", "/absenteismo/paginado", "/absenteismo/resumo", "/horas-trabalhadas"}) {
            executar(c.localA, get("/api/relatorios" + rota).param("mes", "13").param("ano", "2026")
                    .param("contratoId", c.contratoA.getId().toString()), 400, "Competência inválida " + rota);
            executar(c.operacionalA, get("/api/relatorios" + rota).param("mes", "6").param("ano", "2026")
                    .param("contratoId", c.contratoA.getId().toString()), 403, "Operacional não consulta relatório " + rota);
        }
        executar(c.localA, get("/api/relatorios/terceirizados/consolidado").param("busca", "a".repeat(121)),
                400, "Pesquisa cadastral acima do limite");
        executar(c.operacionalA, get("/api/relatorios/terceirizados/consolidado"), 403, "Operacional não lê consolidado cadastral");
        executar(c.operacionalA, get("/api/relatorios/filtros"), 403, "Operacional não recebe filtros gerenciais");
    }
    private void registrar(Cenario c) throws Exception {
        for (int i = 0; i < 2; i++) {
            executar(c.localA, post("/api/frequencias").contentType(APPLICATION_JSON).content(json(Map.of(
                    "vinculoContratualId", c.vinculoA.getId(), "dataReferencia", DATA.plusDays(i).toString(),
                    "status", i == 0 ? "PRESENCA" : "FALTA", "justificativaRetificacao", "Conferência documental de frequência",
                    "chaveIdempotencia", "consulta-relatorio-" + UUID.randomUUID()))), 200, "Fonte canônica de relatório");
        }
    }
}
