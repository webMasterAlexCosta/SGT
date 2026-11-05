package com.example.siggtrat.auditoria20261029;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** AUD-002: motivo tecnico da operacao nao substitui justificativa humana da correcao/cancelamento. */
class FrequenciaFontesMotivoObrigatorioHttpIT extends ContratosHttpBase {

    @ParameterizedTest
    @CsvSource({"calendario,PUT", "excecoes,PUT", "adicionais,PUT",
            "calendario,DELETE", "excecoes,DELETE", "adicionais,DELETE"})
    void retificarOuCancelarFontePublicadaExigeMotivoNovoSemReutilizarNarrativaAntiga(String fonte, String operacao) throws Exception {
        Cenario c = criarCenario();
        String rota = fonte.equals("calendario") ? "/api/calendario-eventos" : "/api/frequencias-" + fonte;
        Map<String, Object> original = corpo(c, fonte, 1, null);
        long id = enviar(c.localA, rota, original, 201, "Criacao continua aceitando ausencia de justificativa de retificacao")
                .path("id").asLong();
        JsonNode criacao = consultar(c.localA, rota + "/" + id + "/eventos", "Primeira publicacao preservada");
        assertEquals(1, criacao.size());
        executar(c.localA, put(rota + "/{id}", id).contentType(APPLICATION_JSON).content(json(original)),
                200, "Reenvio sem mudanca e sem motivo nao cria nova intencao nem exige justificativa");
        assertEquals(criacao, consultar(c.localA, rota + "/" + id + "/eventos", "No-op nao acrescenta versao"));

        String motivoAnterior = "Segunda versao: documento corrigiu o conteudo originalmente informado";
        executar(c.localA, put(rota + "/{id}", id).contentType(APPLICATION_JSON)
                        .content(json(corpo(c, fonte, 2, motivoAnterior))),
                200, "Retificacao com motivo humano explicito preserva autoria e a prova anterior");
        JsonNode duasVersoes = consultar(c.localA, rota + "/" + id + "/eventos", "Motivo declarado deve compor a versao");
        assertEquals(2, duasVersoes.size());
        assertEquals(motivoAnterior, duasVersoes.get(1).path("justificativa").asText());
        assertEquals(c.localA.getId().longValue(), duasVersoes.get(1).path("usuarioId").asLong());
        assertFalse(duasVersoes.get(1).path("motivoTecnico").asText().isBlank());
        assertNotEquals(motivoAnterior, duasVersoes.get(1).path("motivoTecnico").asText());

        if (operacao.equals("PUT")) {
            executar(c.localA, put(rota + "/{id}", id).contentType(APPLICATION_JSON)
                            .content(json(corpo(c, fonte, 3, null))),
                    400, "Nova alteracao exige motivo proprio; nao usa observacao antiga nem motivo da versao anterior");
            executar(c.localA, put(rota + "/{id}", id).contentType(APPLICATION_JSON)
                            .content(json(corpo(c, fonte, 3, "   "))),
                    400, "Espacos nao justificam uma nova alteracao negocial");
        } else {
            executar(c.localA, delete(rota + "/{id}", id),
                    400, "Cancelamento de fonte publicada exige motivo humano declarado");
            executar(c.localA, delete(rota + "/{id}", id).param("justificativa", "   "),
                    400, "Cancelamento com motivo em branco deve ser recusado");
        }
        assertEquals(duasVersoes, consultar(c.localA, rota + "/" + id + "/eventos",
                "Comandos sem motivo sao atomicos: nao gravam nova versao nem apagam a prova"));
        String motivoNovo = "Terceiro ato: conferencia posterior exige " + operacao;
        if (operacao.equals("PUT")) {
            executar(c.localA, put(rota + "/{id}", id).contentType(APPLICATION_JSON)
                            .content(json(corpo(c, fonte, 3, motivoNovo))),
                    200, "Retificacao seguinte com nova justificativa e aceita");
        } else {
            executar(c.localA, delete(rota + "/{id}", id).param("justificativa", motivoNovo),
                    204, "Cancelamento com justificativa e registrado sem exclusao fisica");
        }
        JsonNode tresVersoes = consultar(c.localA, rota + "/" + id + "/eventos", "Tres fatos com autoria e motivos preservados");
        assertEquals(3, tresVersoes.size());
        assertEquals(motivoNovo, tresVersoes.get(2).path("justificativa").asText());
        assertEquals(c.localA.getId().longValue(), tresVersoes.get(2).path("usuarioId").asLong());
        assertEquals(duasVersoes.get(1), tresVersoes.get(1));
        if (operacao.equals("DELETE")) {
            executar(c.localA, delete(rota + "/{id}", id), fonte.equals("calendario") ? 404 : 204,
                    "Reenvio preserva a resposta anterior do recurso cancelado e nao cria outra versao");
            assertEquals(tresVersoes, consultar(c.localA, rota + "/" + id + "/eventos", "Cancelamento idempotente conserva cadeia"));
        }
    }

    private Map<String, Object> corpo(Cenario c, String fonte, int versao, String justificativa) {
        Map<String, Object> corpo = switch (fonte) {
            case "calendario" -> mapa("data", DATA, "tipo", "FERIADO", "abrangencia", "LOTACAO",
                    "lotacaoId", c.lotacaoA.getId(), "ativo", true, "descricao", "Descricao documental versao " + versao);
            case "excecoes" -> mapa("empregadoId", c.empregadoA.getId(), "dataInicio", DATA, "dataFim", DATA,
                    "tipo", "FOLGA_REMUNERADA", "observacao", "Observacao da fonte versao " + versao);
            default -> mapa("empregadoId", c.empregadoA.getId(), "dataReferencia", DATA, "tipo", "RT",
                    "minutos", versao * 60, "quantidade", 1);
        };
        if (justificativa != null) corpo.put("justificativa", justificativa);
        return corpo;
    }
}
