package com.example.siggtrat.auditoria20261029;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Aplicação, filtros e PostgreSQL reais; não confunde tabela preservada com entidade ativa. */
class RemocaoFinanceiraHttpIT extends AuditoriaHttpSuporte {
    private static final String G = "/api/governanca-terceiros";
    private static final Set<String> ENTIDADES_RETIRADAS = Set.of(
            "TabelaTarifaria", "FolhaHoras", "Fatura", "PreviaVigenciaTarifa", "ReciboVigenciaTarifa");

    @Test void metamodeloRetiraSomenteAsEntidadesFinanceiras() {
        Set<String> entidades = em.getMetamodel().getEntities().stream()
                .map(e -> e.getJavaType().getSimpleName()).collect(Collectors.toSet());
        for (String removida : ENTIDADES_RETIRADAS) {
            assertFalse(entidades.contains(removida), "Mapeamento financeiro ainda ativo: " + removida);
            assertThrows(ClassNotFoundException.class,
                    () -> Class.forName("com.example.siggtrat.entidade." + removida),
                    "Classe financeira não deve permanecer no artefato");
        }
        assertTrue(entidades.containsAll(Set.of("Contrato", "Empregado", "VinculoContratual",
                "AlocacaoVinculo", "Frequencia", "EventoFrequencia", "Fornecedor")));
    }

    @Test void registroDeHandlersNaoContemRotasFinanceirasEMantemAsOperacionais() {
        Set<String> rotas = mappings.getHandlerMethods().keySet().stream()
                .flatMap(info -> info.getPatternValues().stream()).collect(Collectors.toSet());
        for (String segmento : List.of("/tabelas-tarifarias", "/folhas-horas", "/faturas"))
            assertTrue(rotas.stream().noneMatch(p -> p.startsWith(G + segmento)),
                    "Handler financeiro ainda registrado: " + segmento);
        assertTrue(rotas.containsAll(Set.of("/api/fornecedores", "/api/fornecedores/{id}",
                "/api/contratos/{contratoId}/fornecedor/{fornecedorId}", "/api/frequencias")));
    }

    @Test void consultasFinanceirasNaoSaoAcessiveisPorUsuarioAutorizado() throws Exception {
        Cenario c = criarCenario();
        for (String caminho : List.of("/tabelas-tarifarias", "/folhas-horas", "/faturas"))
            executar(c.localA, get(G + caminho).param("contratoId", c.contratoA.getId().toString()),
                    404, "Recurso financeiro removido de verdade: " + caminho);
        executar(c.localA, get("/api/fornecedores"), 200, "Consulta operacional de empresa preservada");
        executar(c.operacionalA, get("/api/fornecedores"), 403,
                "A retirada financeira não amplia a permissão do cadastro de empresa");
    }

    @Test void comandosFinanceirosNaoPermanecemAtivosSemInterface() throws Exception {
        Cenario c = criarCenario();
        for (String caminho : List.of("/tabelas-tarifarias",
                "/tabelas-tarifarias/1/encerramento/previa", "/tabelas-tarifarias/1/encerramento",
                "/tabelas-tarifarias/1/substituicao/previa", "/tabelas-tarifarias/1/substituicao",
                "/folhas-horas/gerar", "/folhas-horas/1/submeter", "/folhas-horas/1/aprovar",
                "/folhas-horas/1/rejeitar", "/faturas/gerar/1"))
            executar(c.localA, post(G + caminho).contentType(APPLICATION_JSON).content("{}"),
                    404, "Comando financeiro sem handler: " + caminho);
        executar(c.localA, patch(G + "/faturas/1/status").contentType(APPLICATION_JSON)
                .content("{\"status\":\"PAGA\"}"), 404, "Status financeiro sem handler");
    }
}
