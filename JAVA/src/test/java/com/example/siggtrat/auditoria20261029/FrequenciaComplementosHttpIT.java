package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.CalendarioEvento;
import com.example.siggtrat.entidade.FrequenciaAdicional;
import com.example.siggtrat.entidade.FrequenciaExcecao;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Todos os endpoints de exceções, adicionais e calendário: JSON, manutenção e isolamento. */
class FrequenciaComplementosHttpIT extends AuditoriaHttpSuporte {
    @Test void excecoesPermitemCrudLocalComPeriodoEProtegemOutraGestao() throws Exception {
        Cenario c = criarCenario();
        String corpo = json(Map.of("empregadoId", c.empregadoA.getId(), "dataInicio", DATA.toString(),
                "dataFim", DATA.plusDays(2).toString(), "tipo", "FOLGA_REMUNERADA", "observacao", "Compensação autorizada"));
        JsonNode criada = dados(executar(c.localA, post("/api/frequencias-excecoes").contentType(APPLICATION_JSON)
                .content(corpo), 201, "Exceção local válida"));
        long id = criada.path("id").longValue();
        assertEquals("APROVADO", criada.path("status").asText());
        assertEquals(c.empregadoA.getId().longValue(), criada.path("empregadoId").longValue());
        assertEquals(DATA.plusDays(2).toString(), criada.path("dataFim").asText());
        JsonNode pagina = dados(executar(c.localA, get("/api/frequencias-excecoes").param("data", DATA.toString()),
                200, "Consulta diária de exceção ativa"));
        assertEquals(id, pagina.path("content").get(0).path("id").longValue());
        assertEquals(0, dados(executar(c.localB, get("/api/frequencias-excecoes").param("data", DATA.toString()),
                200, "Escopo de outra Gestão não contém exceção")).path("content").size());
        executar(c.localB, put("/api/frequencias-excecoes/{id}", id).contentType(APPLICATION_JSON)
                .content(json(Map.of("observacao", "Tentativa alheia"))), 403, "PUT alheio bloqueado");
        executar(c.localB, delete("/api/frequencias-excecoes/{id}", id), 403, "DELETE alheio bloqueado");
        JsonNode alterada = dados(executar(c.localA, put("/api/frequencias-excecoes/{id}", id)
                .contentType(APPLICATION_JSON).content(json(Map.of("observacao", "Compensação conferida",
                        "justificativa", "Conferência documental corrigiu a observação da compensação"))),
                200, "PUT parcial preserva datas e altera observação"));
        assertEquals("Compensação conferida", alterada.path("observacao").asText());
        assertEquals(DATA.toString(), alterada.path("dataInicio").asText());
        executar(c.localA, delete("/api/frequencias-excecoes/{id}", id)
                .param("justificativa", "Cancelamento documental da compensação"), 204, "Exclusão autorizada da exceção");
        em.flush(); em.clear();
        FrequenciaExcecao preservada = em.find(FrequenciaExcecao.class, id);
        assertNotNull(preservada, "GOV-003: fonte publicada é cancelada, não removida fisicamente");
        assertEquals("CANCELADO", preservada.getStatus());
    }

    @Test void excecoesRejeitamPeriodoInvalidoSobreposicaoEPerfilConsultivo() throws Exception {
        Cenario c = criarCenario();
        String corpo = json(Map.of("empregadoId", c.empregadoA.getId(), "dataInicio", DATA.toString(),
                "dataFim", DATA.toString(), "tipo", "FOLGA_REMUNERADA"));
        JsonNode criada = dados(executar(c.localA, post("/api/frequencias-excecoes").contentType(APPLICATION_JSON)
                .content(corpo), 201, "Exceção para teste de sobreposição"));
        executar(c.localA, post("/api/frequencias-excecoes").contentType(APPLICATION_JSON).content(corpo),
                409, "Mesmo empregado/tipo/período não pode sobrepor");
        executar(c.estadualA, post("/api/frequencias-excecoes").contentType(APPLICATION_JSON).content(corpo),
                403, "Estadual consulta mas não cadastra exceção");
        executar(c.localA, get("/api/frequencias-excecoes"), 400, "Data de consulta obrigatória");
        executar(c.localA, put("/api/frequencias-excecoes/{id}", criada.path("id").longValue())
                .contentType(APPLICATION_JSON).content(json(Map.of("dataInicio", DATA.plusDays(1).toString()))),
                400, "Período invertido não altera exceção");
        executar(c.localA, delete("/api/frequencias-excecoes/999999999"), 404, "Exceção inexistente");
    }

    @Test void adicionaisPermitemCrudLocalERetornamMinutosEQuantidade() throws Exception {
        Cenario c = criarCenario();
        String corpo = json(Map.of("empregadoId", c.empregadoA.getId(), "dataReferencia", DATA.toString(),
                "tipo", "RT", "minutos", 60, "quantidade", 1.25, "justificativa", "Serviço adicional documentado"));
        JsonNode criada = dados(executar(c.localA, post("/api/frequencias-adicionais").contentType(APPLICATION_JSON)
                .content(corpo), 201, "Criação de adicional da Gestão Local"));
        long id = criada.path("id").longValue();
        assertEquals(60, criada.path("minutos").intValue());
        assertEquals(1.25, criada.path("quantidade").doubleValue());
        assertEquals("CALCULADO", criada.path("status").asText());
        JsonNode pagina = dados(executar(c.localA, get("/api/frequencias-adicionais").param("inicio", DATA.toString())
                .param("fim", DATA.toString()).param("tipo", "RT"), 200, "Consulta de adicionais por período/tipo"));
        assertEquals(id, pagina.path("content").get(0).path("id").longValue());
        assertEquals(0, dados(executar(c.localB, get("/api/frequencias-adicionais").param("data", DATA.toString()),
                200, "Adicional alheio não aparece na Gestão B")).path("content").size());
        executar(c.localB, put("/api/frequencias-adicionais/{id}", id).contentType(APPLICATION_JSON)
                .content(json(Map.of("empregadoId", c.empregadoB.getId(), "minutos", 90))),
                403, "Adicional alheio não pode ser capturado por troca de empregado");
        executar(c.localB, delete("/api/frequencias-adicionais/{id}", id), 403, "Adicional alheio não pode ser excluído");
        JsonNode atualizada = dados(executar(c.localA, put("/api/frequencias-adicionais/{id}", id)
                .contentType(APPLICATION_JSON).content(json(Map.of("minutos", 90, "quantidade", 1.5,
                        "justificativa", "Conferência documental corrigiu a quantidade do adicional"))),
                200, "Atualização local do adicional"));
        assertEquals(90, atualizada.path("minutos").intValue());
        assertEquals(DATA.toString(), atualizada.path("dataReferencia").asText());
        executar(c.localA, delete("/api/frequencias-adicionais/{id}", id)
                .param("justificativa", "Cancelamento documental do adicional"), 204, "Exclusão autorizada do adicional");
        em.flush(); em.clear();
        FrequenciaAdicional preservado = em.find(FrequenciaAdicional.class, id);
        assertNotNull(preservado, "GOV-003: adicional manual mantém a raiz e sua cadeia histórica");
        assertEquals("CANCELADO", preservado.getStatus());
    }

    @Test void adicionaisRejeitamConsultaAmbiguaCamposAusentesEPerfilConsultivo() throws Exception {
        Cenario c = criarCenario();
        executar(c.localA, get("/api/frequencias-adicionais").param("data", DATA.toString())
                .param("inicio", DATA.toString()).param("fim", DATA.toString()), 400, "Data e período não podem ser misturados");
        executar(c.localA, get("/api/frequencias-adicionais").param("inicio", DATA.toString()),
                400, "Intervalo incompleto de adicionais");
        String semData = json(Map.of("empregadoId", c.empregadoA.getId(), "tipo", "RT", "minutos", 60));
        executar(c.localA, post("/api/frequencias-adicionais").contentType(APPLICATION_JSON).content(semData),
                400, "Data de referência obrigatória");
        executar(c.nacional, post("/api/frequencias-adicionais").contentType(APPLICATION_JSON).content(semData),
                403, "Gestor Nacional sem escrita de adicionais");
        executar(c.localA, put("/api/frequencias-adicionais/999999999").contentType(APPLICATION_JSON).content("{}"),
                404, "PUT adicional inexistente");
        executar(c.localA, delete("/api/frequencias-adicionais/999999999"), 404, "DELETE adicional inexistente");
    }

    @Test void calendarioCriaConsultaAtualizaExcluiSomenteNoEscopoProprio() throws Exception {
        Cenario c = criarCenario();
        Map<String,Object> corpo = calendario(c, "Evento inicial");
        JsonNode criada = dados(executar(c.localA, post("/api/calendario-eventos").contentType(APPLICATION_JSON)
                .content(json(corpo)), 201, "Calendário local por lotação"));
        long id = criada.path("id").longValue();
        assertEquals("LOTACAO", criada.path("abrangencia").asText());
        assertEquals(c.lotacaoA.getId().longValue(), criada.path("lotacaoId").longValue());
        JsonNode lista = dados(executar(c.localA, get("/api/calendario-eventos").param("data", DATA.toString())
                .param("lotacaoId", c.lotacaoA.getId().toString()), 200, "Consulta calendário aplicável à lotação"));
        assertEquals(id, lista.get(0).path("id").longValue());
        assertEquals(0, dados(executar(c.localB, get("/api/calendario-eventos").param("data", DATA.toString()),
                200, "Eventos de outra Gestão são filtrados")).size());
        executar(c.localB, get("/api/calendario-eventos").param("data", DATA.toString())
                .param("lotacaoId", c.lotacaoA.getId().toString()), 403, "Filtro de lotação alheia não amplia consulta");
        executar(c.localB, put("/api/calendario-eventos/{id}", id).contentType(APPLICATION_JSON).content(json(corpo)),
                404, "Evento não editável por outra Gestão");
        executar(c.localB, delete("/api/calendario-eventos/{id}", id), 404, "Evento não removível por outra Gestão");
        JsonNode atualizada = dados(executar(c.localA, put("/api/calendario-eventos/{id}", id)
                .contentType(APPLICATION_JSON).content(json(calendario(c, "Evento conferido"))),
                200, "Atualização local do calendário"));
        assertEquals("Evento conferido", atualizada.path("descricao").asText());
        executar(c.localA, delete("/api/calendario-eventos/{id}", id)
                .param("justificativa", "Ato posterior cancelou o evento local"), 204, "Exclusão local do calendário");
        // GOV-003: DELETE mantém o contrato HTTP, mas o fato publicado é cancelado e auditado.
        em.flush(); em.clear();
        CalendarioEvento preservado = em.find(CalendarioEvento.class, id);
        assertNotNull(preservado);
        assertTrue(preservado.isCancelado());
        assertFalse(preservado.getAtivo());
        assertEquals(0, dados(executar(c.localA, get("/api/calendario-eventos").param("data", DATA.toString()),
                200, "O calendário vigente exclui o fato cancelado")).size());
        JsonNode cadeia = dados(executar(c.localA, get("/api/calendario-eventos/{id}/eventos", id),
                200, "Criação, retificação e cancelamento permanecem auditáveis"));
        assertEquals(3, cadeia.size());
        assertEquals("CANCELAMENTO", cadeia.get(2).path("tipoOperacao").asText());
    }

    @Test void calendarioRejeitaDadosInvalidosDuplicidadeENacionalPeloLocal() throws Exception {
        Cenario c = criarCenario();
        String corpo = json(calendario(c, "Feriado da unidade"));
        executar(c.localA, post("/api/calendario-eventos").contentType(APPLICATION_JSON).content(corpo),
                201, "Evento para teste de duplicidade");
        executar(c.localA, post("/api/calendario-eventos").contentType(APPLICATION_JSON).content(corpo),
                409, "Evento duplicado na mesma data/tipo/escopo");
        executar(c.localA, post("/api/calendario-eventos").contentType(APPLICATION_JSON).content("{}"),
                400, "Data e tipo obrigatórios");
        executar(c.localA, post("/api/calendario-eventos").contentType(APPLICATION_JSON).content(json(Map.of(
                "data", DATA.toString(), "tipo", "FERIADO", "abrangencia", "NACIONAL"))),
                403, "Gestão Local não cria calendário nacional");
        executar(c.localA, get("/api/calendario-eventos").param("inicio", DATA.plusDays(1).toString())
                .param("fim", DATA.toString()), 400, "Calendário com período invertido");
        executar(c.localA, put("/api/calendario-eventos/999999999").contentType(APPLICATION_JSON).content(corpo),
                404, "Atualização de evento inexistente");
        executar(c.localA, delete("/api/calendario-eventos/999999999"), 404, "Exclusão de evento inexistente");
    }

    private Map<String,Object> calendario(Cenario c, String descricao) {
        return Map.of("data", DATA.toString(), "tipo", "FERIADO", "descricao", descricao,
                "abrangencia", "LOTACAO", "lotacaoId", c.lotacaoA.getId(), "ativo", true,
                "justificativa", "Conferência documental do evento: " + descricao);
    }
}
