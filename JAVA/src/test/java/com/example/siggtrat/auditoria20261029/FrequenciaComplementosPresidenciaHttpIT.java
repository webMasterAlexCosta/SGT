package com.example.siggtrat.auditoria20261029;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** PER-004/PER-011: consulta institucional de fontes de frequência sem exposição nominal nem escrita. */
class FrequenciaComplementosPresidenciaHttpIT extends ContratosHttpBase {

    @ParameterizedTest
    @ValueSource(strings = {"excecoes", "adicionais"})
    void presidenciaConsultaListaEHistoricoMinimizadosSemReceberPermissaoDeEscrita(String recurso) throws Exception {
        Cenario c = criarCenario();
        // O controle de minimização parte de uma identidade histórica comprovada.
        // A fixture genérica continua sem fonte para os cenários de ausência.
        declararFonteNominalSintetica(c, "COMPLEMENTOS-PRESIDENCIA-" + recurso);
        String rota = "/api/frequencias-" + recurso;
        String narrativaOriginal = "Comprovante pessoal apresentado para compensacao original";
        String narrativaRetificada = "Conferencia pessoal reservada do documento retificado";
        String campoNarrativa = recurso.equals("excecoes") ? "observacao" : "justificativa";
        Map<String, Object> corpo = recurso.equals("excecoes")
                ? mapa("empregadoId", c.empregadoA.getId(), "dataInicio", DATA, "dataFim", DATA,
                        "tipo", "FOLGA_REMUNERADA", campoNarrativa, narrativaOriginal)
                : mapa("empregadoId", c.empregadoA.getId(), "dataReferencia", DATA, "tipo", "RT",
                        "minutos", 60, "quantidade", 1, campoNarrativa, narrativaOriginal);
        JsonNode criado = enviar(c.localA, rota, corpo, 201, "Gestor publica a fonte manual no contexto valido");
        long id = criado.path("id").asLong();
        Map<String, Object> retificacao = mapa(campoNarrativa, narrativaRetificada);
        if (recurso.equals("excecoes")) retificacao.put("justificativa", "Conferencia documental corrigiu a narrativa pessoal");
        executar(c.localA, put(rota + "/{id}", id).contentType(APPLICATION_JSON)
                        .content(json(retificacao)),
                200, "Retificacao produz duas fotografias pessoais preservadas");
        JsonNode provaOriginal = consultar(c.localA, rota + "/" + id + "/eventos",
                "Gestor autorizado ve a prova integral antes da consulta institucional");
        assertEquals(2, provaOriginal.size());
        assertEquals(c.empregadoA.getNome(), provaOriginal.get(0).path("fotografia").path("empregadoNome").asText());
        assertEquals(narrativaOriginal, provaOriginal.get(0).path("fotografia").path(campoNarrativa).asText());

        JsonNode lista = dados(executar(c.presidente, get(rota).param("data", DATA.toString())
                        .param("contratoId", c.contratoA.getId().toString()),
                200, "PER004 Presidencia consulta a fonte institucional no recorte solicitado"));
        assertEquals(1, lista.path("content").size());
        JsonNode item = lista.path("content").get(0);
        assertEquals(id, item.path("id").asLong());
        assertEquals(criado.path("tipo").asText(), item.path("tipo").asText());
        assertEquals("", item.path("empregadoNome").asText(""), "Nome pessoal deve ser minimizado na lista");
        assertEquals("", item.path(campoNarrativa).asText(""), "Narrativa pessoal deve ser minimizada na lista");

        JsonNode eventos = consultar(c.presidente, rota + "/" + id + "/eventos",
                "PER004 Presidencia consulta as versoes da fonte institucional");
        assertEquals(2, eventos.size());
        for (int i = 0; i < eventos.size(); i++) {
            JsonNode evento = eventos.get(i);
            assertEquals(i + 1, evento.path("numeroVersao").asInt());
            assertEquals(provaOriginal.get(i).path("hashVersao").asText(), evento.path("hashVersao").asText(),
                    "Minimizacao da resposta preserva o hash da prova integral armazenada");
            assertEquals("", evento.path("fotografia").path("empregadoNome").asText(""));
            assertEquals("", evento.path("fotografia").path(campoNarrativa).asText(""));
            assertEquals("", evento.path("usuarioUsername").asText(""));
            assertEquals("", evento.path("justificativa").asText(""));
        }
        assertFalse(lista.toString().contains(c.empregadoA.getNome()));
        assertFalse(eventos.toString().contains(c.empregadoA.getNome()));
        assertFalse(eventos.toString().contains(narrativaOriginal));
        assertFalse(eventos.toString().contains(narrativaRetificada));

        executar(c.presidente, post(rota).contentType(APPLICATION_JSON).content(json(corpo)),
                403, "Consulta institucional nao autoriza cadastrar complemento");
        executar(c.presidente, put(rota + "/{id}", id).contentType(APPLICATION_JSON)
                        .content(json(mapa(campoNarrativa, "Tentativa sem competencia de escrita"))),
                403, "Consulta institucional nao autoriza retificar complemento");
        executar(c.presidente, delete(rota + "/{id}", id),
                403, "Consulta institucional nao autoriza cancelar complemento");
        assertEquals(provaOriginal, consultar(c.localA, rota + "/" + id + "/eventos",
                "Consultas minimizadas e escritas recusadas preservam a prova integral"));
    }
}
