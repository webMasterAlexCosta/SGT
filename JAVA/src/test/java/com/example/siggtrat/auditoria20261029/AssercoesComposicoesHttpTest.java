package com.example.siggtrat.auditoria20261029;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Controles negativos do próprio oráculo: negócio alterado e corte velho continuam falhando. */
class AssercoesComposicoesHttpTest {
    private static final Instant ANTES = Instant.parse("2026-11-02T10:00:00Z");
    private static final Instant DEPOIS = Instant.parse("2026-11-02T10:00:01Z");

    @Test
    void aceitaSomenteAvancoDoInstanteSemAlterarAsRespostasOriginais() throws Exception {
        var antes = leitura(ANTES);
        var depois = leitura(DEPOIS);
        var antesOriginal = antes.dados().deepCopy();
        var depoisOriginal = depois.dados().deepCopy();
        AssercoesComposicoesHttp.exigirPreservacaoComConsultaPosterior(antes, depois);
        assertThat(antes.dados()).isEqualTo(antesOriginal);
        assertThat(depois.dados()).isEqualTo(depoisOriginal);
    }

    @Test
    void naoOcultaMutacaoDeCapacidadeOuDeProvaHistorica() throws Exception {
        var antes = leitura(ANTES);
        var capacidadeAlterada = leitura(DEPOIS);
        ((ObjectNode) capacidadeAlterada.dados().get(0)).put("efetivoPrevisto", 0);
        assertThrows(AssertionError.class, () ->
                AssercoesComposicoesHttp.exigirPreservacaoComConsultaPosterior(antes, capacidadeAlterada));
        var provaAlterada = leitura(DEPOIS);
        ((ObjectNode) provaAlterada.dados().get(0).path("prova")).put("conhecidoAte", "origem alterada");
        assertThrows(AssertionError.class, () ->
                AssercoesComposicoesHttp.exigirPreservacaoComConsultaPosterior(antes, provaAlterada));
    }

    @Test
    void recusaCorteAntigoMesmoQuandoOsDadosDeNegocioSaoIguais() throws Exception {
        var antes = leitura(ANTES);
        var depoisComCorteAntigo = new AssercoesComposicoesHttp.Leitura(
                antes.dados(), DEPOIS.minusMillis(1), DEPOIS.plusMillis(1));
        assertThrows(AssertionError.class, () ->
                AssercoesComposicoesHttp.exigirPreservacaoComConsultaPosterior(antes, depoisComCorteAntigo));
    }

    private AssercoesComposicoesHttp.Leitura leitura(Instant instante) throws Exception {
        ArrayNode dados = (ArrayNode) new ObjectMapper().readTree("""
                [{"id":1,"efetivoPrevisto":1,"dataReferencia":"2026-11-10",
                  "prova":{"estado":"COMPROVADO","conhecidoAte":"origem histórica preservada"}}]
                """);
        ((ObjectNode) dados.get(0)).put("conhecidoAte", instante.toString());
        return new AssercoesComposicoesHttp.Leitura(dados, instante.minusMillis(1), instante.plusMillis(1));
    }
}
