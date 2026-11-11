package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.Frequencia;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

class FrequenciaConsultaMinimizacaoHttpIT extends AuditoriaHttpSuporte {
    @ParameterizedTest
    @CsvSource({"NACIONAL,dia", "NACIONAL,mensal", "NACIONAL,intervalo",
            "ESTADUAL,dia", "ESTADUAL,mensal", "ESTADUAL,intervalo"})
    void consultaAuditoraNaoExpoeObservacaoPessoalPorViaParalela(String perfil, String rota) throws Exception {
        Cenario c = criarCenario();
        String segredo = "OBSERVACAO-PRIVADA-" + UUID.randomUUID();
        long id = salvar(c, false, segredo);
        UsuarioAplicacao leitor = perfil.equals("NACIONAL") ? c.nacional : c.estadualA;
        JsonNode pagina = dados(executar(leitor, consulta(rota, c.contratoA.getId()), 200,
                "PER-006/PER-011: minimização também se aplica ao DTO de consulta " + rota));
        assertEquals(1, pagina.path("content").size());
        JsonNode linha = pagina.path("content").get(0);
        assertEquals(id, linha.path("id").asLong());
        assertEquals("04:00", linha.path("horasTrabalhadas").asText());
        assertFalse(pagina.toString().contains(segredo));
        assertTrue(linha.path("observacao").isNull() || linha.path("observacao").isMissingNode());
        JsonNode local = dados(executar(c.localA, consulta(rota, c.contratoA.getId()), 200,
                "Gestão responsável continua recebendo a observação original"));
        assertEquals(segredo, local.path("content").get(0).path("observacao").asText());
        em.clear();
        assertEquals(segredo, em.find(Frequencia.class, id).getObservacao());
    }

    @ParameterizedTest
    @ValueSource(strings = {"dia", "mensal", "intervalo"})
    void operadorMantemObservacaoNecessariaNoSeuEscopo(String rota) throws Exception {
        Cenario c = criarCenario();
        String observacao = "Anotação operacional da escala " + UUID.randomUUID();
        salvar(c, false, observacao);
        JsonNode pagina = dados(executar(c.operacionalA, consulta(rota, c.contratoA.getId()), 200,
                "Minimização da auditoria não remove o contexto necessário ao Operacional autorizado"));
        assertEquals(observacao, pagina.path("content").get(0).path("observacao").asText());
        JsonNode alheia = dados(executar(c.operacionalB, consulta(rota, c.contratoA.getId()), 200,
                "A necessidade operacional continua limitada ao escopo autorizado"));
        assertEquals(0, alheia.path("content").size());
    }

    @Test
    void permissaoExcepcionalEscopadaRegistraUsoERevogacaoValeEmConsultaEEventos() throws Exception {
        Cenario c = criarCenario();
        String segredoA = "OBSERVACAO-CONCESSAO-A-" + UUID.randomUUID();
        String segredoB = "OBSERVACAO-CONCESSAO-B-" + UUID.randomUUID();
        long frequenciaA = salvar(c, false, segredoA);
        long frequenciaB = salvar(c, true, segredoB);
        JsonNode antes = dados(executar(c.nacional, consulta("intervalo", c.contratoA.getId()), 200,
                "Consulta auditora ordinária não revela observação pessoal"));
        assertFalse(antes.toString().contains(segredoA));
        JsonNode concessao = dados(executar(c.admin, post("/api/permissoes-excepcionais")
                        .contentType(APPLICATION_JSON).content(json(Map.of(
                                "tipo", "ACESSO_DADOS_PESSOAIS_RESTRITOS", "usuarioBeneficiarioId", c.nacional.getId(),
                                "contratoId", c.contratoA.getId(), "finalidade", "Auditoria de frequência autorizada",
                                "justificativa", "Exame documental de observações pessoais no contrato específico",
                                "autorizacaoAdministrativa", "SEI-AUDITORIA-FREQUENCIA",
                                "validoAte", Instant.now().plusSeconds(3600).toString()))),
                201, "Concessão real limitada ao Contrato A"));
        long permissaoId = concessao.path("id").asLong();
        JsonNode autorizada = dados(executar(c.nacional, consulta("intervalo", c.contratoA.getId()), 200,
                "A concessão vigente eleva somente o recorte autorizado"));
        assertEquals(segredoA, autorizada.path("content").get(0).path("observacao").asText());
        JsonNode eventosA = dados(executar(c.nacional, get("/api/frequencias/{id}/eventos", frequenciaA), 200,
                "A mesma concessão permite inspecionar a prova completa do recorte"));
        assertTrue(eventosA.get(0).path("snapshotNovo").asText().contains(segredoA));
        JsonNode evidenciasA = dados(executar(c.nacional, get("/api/frequencias/{id}/evidencias-integridade", frequenciaA), 200,
                "O diagnóstico autorizado preserva a prova completa"));
        assertTrue(evidenciasA.toString().contains(segredoA));
        JsonNode fora = dados(executar(c.nacional, consulta("intervalo", c.contratoB.getId()), 200,
                "A concessão não amplia a leitura pessoal do Contrato B"));
        assertFalse(fora.toString().contains(segredoB));
        JsonNode eventosB = dados(executar(c.nacional, get("/api/frequencias/{id}/eventos", frequenciaB), 200,
                "Eventos de outro contrato continuam minimizados"));
        assertFalse(eventosB.toString().contains(segredoB));
        JsonNode usos = dados(executar(c.admin, get("/api/permissoes-excepcionais/{id}/usos", permissaoId), 200,
                "Consumo excepcional da consulta e da prova fica auditado"));
        assertTrue(usos.size() >= 3);
        executar(c.admin, post("/api/permissoes-excepcionais/{id}/revogar", permissaoId)
                        .contentType(APPLICATION_JSON).content(json(Map.of("motivo", "Finalidade documental encerrada"))),
                200, "Revogação real da concessão");
        JsonNode revogada = dados(executar(c.nacional, consulta("intervalo", c.contratoA.getId()), 200,
                "Uma página consultada antes não conserva o privilégio revogado"));
        assertFalse(revogada.toString().contains(segredoA));
        JsonNode eventoRevogado = dados(executar(c.nacional, get("/api/frequencias/{id}/eventos", frequenciaA), 200,
                "A prova retorna à representação minimizada após revogação"));
        assertFalse(eventoRevogado.toString().contains(segredoA));
        em.clear();
        assertEquals(segredoA, em.find(Frequencia.class, frequenciaA).getObservacao());
    }

    private long salvar(Cenario c, boolean ladoB, String observacao) throws Exception {
        return dados(executar(ladoB ? c.localB : c.localA, post("/api/frequencias")
                        .contentType(APPLICATION_JSON).content(json(Map.of(
                                "vinculoContratualId", (ladoB ? c.vinculoB : c.vinculoA).getId(),
                                "dataReferencia", DATA.toString(), "status", "PRESENCA",
                                "horasTrabalhadas", "04:00", "horarioSaida", "12:00", "observacao", observacao,
                                "justificativaRetificacao", "Conferência formal da anotação do dia",
                                "chaveIdempotencia", "consulta-minimizada-" + UUID.randomUUID()))),
                200, "Fonte real com observação pessoal e memória de quatro horas")).path("id").asLong();
    }

    private AbstractMockHttpServletRequestBuilder<?> consulta(String rota, Long contrato) {
        AbstractMockHttpServletRequestBuilder<?> request = switch (rota) {
            case "dia" -> get("/api/frequencias").param("data", DATA.toString());
            case "mensal" -> get("/api/frequencias/mensal").param("mes", "6").param("ano", "2026");
            default -> get("/api/frequencias/intervalo").param("inicio", DATA.toString()).param("fim", DATA.toString());
        };
        return request.param("contratoId", contrato.toString());
    }
}
