package com.example.siggtrat.auditoria20261029;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/** AUD-005: um reenvio que não muda intenção nem conteúdo não gera outra versão do fato. */
class IdempotenciaFontesFrequenciaHttpIT extends ContratosHttpBase {

    @ParameterizedTest
    @ValueSource(strings = {"calendario", "excecoes", "adicionais"})
    void reenvioDePutIdenticoNaoDuplicaVersaoMasNovaAlteracaoPermaneceDocumentada(String recurso) throws Exception {
        Cenario c = criarCenario();
        String rota = rota(recurso);
        long id = enviar(c.localA, rota, corpo(c, recurso, 1), 201,
                "Fonte original registrada uma única vez").path("id").asLong();
        Map<String,Object> retificacao = corpo(c, recurso, 2);
        executar(c.localA, put(rota + "/{id}", id).contentType(APPLICATION_JSON).content(json(retificacao)), 200,
                "Alteração material gera a segunda versão da fonte");
        assertEquals(2, consultar(c.localA, rota + "/" + id + "/eventos", "Duas intenções já documentadas").size());
        executar(c.localA, put(rota + "/{id}", id).contentType(APPLICATION_JSON).content(json(retificacao)), 200,
                "Mesmo PUT reenviado mantém resposta válida e não duplica o fato");
        assertEquals(2, consultar(c.localA, rota + "/" + id + "/eventos", "Reenvio idêntico conserva a cadeia").size());
        executar(c.localA, put(rota + "/{id}", id).contentType(APPLICATION_JSON).content(json(corpo(c, recurso, 3))), 200,
                "Uma alteração factual posterior distinta continua sendo uma nova retificação");
        assertEquals(3, consultar(c.localA, rota + "/" + id + "/eventos", "Terceira intenção é preservada").size());
    }

    private String rota(String recurso) {
        return recurso.equals("calendario") ? "/api/calendario-eventos" : "/api/frequencias-" + recurso;
    }

    private Map<String,Object> corpo(Cenario c, String recurso, int versao) {
        Map<String, Object> pedido = switch (recurso) {
            case "calendario" -> mapa("data", DATA, "tipo", "FERIADO", "descricao", "Descrição documental " + versao,
                    "abrangencia", "LOTACAO", "lotacaoId", c.lotacaoA.getId(), "ativo", true);
            case "excecoes" -> mapa("empregadoId", c.empregadoA.getId(), "dataInicio", DATA, "dataFim", DATA,
                    "tipo", "FOLGA_REMUNERADA", "observacao", "Compensação documental " + versao);
            default -> mapa("empregadoId", c.empregadoA.getId(), "dataReferencia", DATA, "tipo", "RT",
                    "minutos", 60 * versao, "quantidade", 1, "justificativa", "Serviço documentado " + versao);
        };
        if (versao > 1 && !recurso.equals("adicionais"))
            pedido.put("justificativa", "Documento retificador da fonte na versao " + versao);
        return pedido;
    }
}
