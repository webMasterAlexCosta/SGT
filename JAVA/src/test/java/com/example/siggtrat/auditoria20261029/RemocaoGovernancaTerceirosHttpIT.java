package com.example.siggtrat.auditoria20261029;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpMethod;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * Complemento real do gate de arquitetura: aplicação, segurança e PostgreSQL.
 * Uma recusa 401/403 não vale como prova de remoção; exige Gestor Local válido,
 * controle operacional 200 e ausência dos handlers registrados em runtime.
 */
class RemocaoGovernancaTerceirosHttpIT extends ContratosHttpBase {
    private static final String G = "/api/governanca-terceiros";
    @Autowired private ApplicationContext contexto;

    @Test
    void registroRuntimeRetiraPrefixoInteiroEPreservaFornecedoresEAuditoria() {
        Set<String> rotas = mappings.getHandlerMethods().keySet().stream()
                .flatMap(info -> info.getPatternValues().stream()).collect(Collectors.toSet());
        assertTrue(rotas.stream().noneMatch(path -> path.startsWith(G)),
                "Não pode restar um handler ativo no prefixo do módulo retirado");
        assertTrue(rotas.containsAll(Set.of("/api/fornecedores", "/api/fornecedores/{id}",
                "/api/contratos/{contratoId}/fornecedor/{fornecedorId}",
                "/api/contratos", "/api/frequencias", "/api/registros-auditoria")));
    }

    @Test
    void metamodeloRetiraEntidadesExclusivasEPreservaNucleoOperacional() {
        Set<String> entidades = em.getMetamodel().getEntities().stream()
                .map(e -> e.getJavaType().getSimpleName()).collect(Collectors.toSet());
        assertFalse(entidades.contains("ProcessoTerceiro"));
        assertFalse(entidades.contains("AvaliacaoRiscoFornecedor"));
        assertTrue(entidades.containsAll(Set.of("Fornecedor", "Contrato", "AditamentoContratual",
                "VersaoContratoNegocial", "Empregado", "VinculoContratual", "AlocacaoVinculo",
                "Frequencia", "EventoFrequencia", "RegistroAuditoria")));
    }

    @Test
    void nenhumServicoOuJobDeRiscoPermaneceAtivo() {
        assertFalse(contexto.containsBean("governancaTerceirosServico"));
        assertFalse(contexto.containsBean("riscoContinuoServico"));
        assertFalse(contexto.containsBean("processoTerceiroRepositorio"));
        assertFalse(contexto.containsBean("avaliacaoRiscoFornecedorRepositorio"));
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("com.example.siggtrat.servico.RiscoContinuoServico"));
    }

    @Test
    void dezRotasAntigasRetornam404ParaGestorLocalAutenticado() throws Exception {
        var c = criarCenario();
        executar(c.localA, get("/api/fornecedores"), 200,
                "Controle: mesma sessão tem acesso válido ao cadastro operacional");
        List<RotaRetirada> rotas = List.of(
                new RotaRetirada("GET", "/resumo", "{}"),
                new RotaRetirada("GET", "/fornecedores", "{}"),
                new RotaRetirada("POST", "/fornecedores",
                        json(mapa("razaoSocial", "Empresa sintética da rota retirada", "cnpj", "12345678000190"))),
                new RotaRetirada("PUT", "/fornecedores/1",
                        json(mapa("razaoSocial", "Empresa sintética da rota retirada", "cnpj", "12345678000190"))),
                new RotaRetirada("PUT", "/contratos/" + c.contratoA.getId() + "/fornecedor/1", "{}"),
                new RotaRetirada("GET", "/processos", "{}"),
                new RotaRetirada("POST", "/processos", json(mapa("empregadoId", c.empregadoA.getId(),
                        "contratoId", c.contratoA.getId(), "tipo", "ONBOARDING", "prazo", DATA.plusDays(5)))),
                new RotaRetirada("PATCH", "/processos/1/status", "{\"status\":\"EM_ANDAMENTO\"}"),
                new RotaRetirada("GET", "/riscos", "{}"),
                new RotaRetirada("POST", "/riscos", json(mapa("fornecedorId", 1,
                        "scoreTrabalhista", 10, "scoreFinanceiro", 10, "scoreCompliance", 10,
                        "scoreSeguranca", 10, "proximaAvaliacao", DATA.plusMonths(3),
                        "evidencias", "Fixture sintética de rota retirada"))));
        for (RotaRetirada rota : rotas) {
            executar(c.localA, request(HttpMethod.valueOf(rota.metodo()), G + rota.caminho())
                            .contentType(APPLICATION_JSON).content(rota.corpo()), 404,
                    "Handler retirado de verdade: " + rota.metodo() + " " + G + rota.caminho());
        }
        executar(c.localA, get("/api/fornecedores"), 200,
                "Controle final: as chamadas retiradas não invalidaram a sessão autorizada");
    }

    @Test
    void auditoriaGeralContinuaDisponivelParaAdministrador() throws Exception {
        var c = criarCenario();
        executar(c.admin, get("/api/registros-auditoria"), 200,
                "GOV-003: consulta geral de auditoria permanece após retirada do módulo");
    }

    private record RotaRetirada(String metodo, String caminho, String corpo) {}
}
