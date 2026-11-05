package com.example.siggtrat.auditoria20261029;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** REDs propostos pela revisão independente; ainda não executados nesta etapa. */
class ContratosPisoReplayHttpIT extends ContratosHttpBase {
    @Test
    void cancelarAumentoNaoPermiteQueReducaoPosteriorReaplicadaUltrapassePisoOriginal() throws Exception {
        var c = criarCenario();
        var n = publicado(c, 100, null, true);
        var aumento = quantitativo("AUMENTO-40", n.inicio().plusDays(1), n.composicao1(), 140);
        var a = registrarAditamento(c, n.id(), aumento);
        var reducao = quantitativo("REDUCAO-65", n.inicio().plusDays(2), n.composicao1(), 75);
        var r = registrarAditamento(c, n.id(), reducao);
        assertThat(total(c, n.id(), n.inicio().plusDays(3))).isEqualTo(75);

        enviar(c.localA, base(n.id()) + "/aditamentos/" + a.path("id").asLong() + "/cancelar",
                mapa("motivo", "Aumento revogado antes de produzir efeito", "referenciaSei", "SEI-REVOGACAO"), 200,
                "ADI-006 cancelamento futuro válido preserva os documentos posteriores");
        em.flush();

        assertThat(total(c, n.id(), n.inicio().plusDays(3)))
                .as("RED-006/TMP-009: sem o aumento, aplicar −65 a 100 daria35; o fato posterior incompatível não pode valer")
                .isEqualTo(100);
        assertThat(consultar(c.localA, base(n.id()) + "/aditamentos", "ADI-008 ambos os instrumentos permanecem auditáveis"))
                .hasSize(2);
        assertThat(pendenciasDoEvento(n.id(), r.path("eventoId").asLong()))
                .as("TMP-009/AUD-006 deve existir pendência identificando a redução posterior e o piso violado")
                .isEqualTo(1);
    }

    @Test
    void reducaoRetroativaLegalReavaliaPisoDosAditamentosPosteriores() throws Exception {
        var c = criarCenario();
        var n = publicado(c, 100, null, false);
        registrarAditamento(c, n.id(), quantitativo("AUMENTO-CONHECIDO", n.inicio().plusDays(10), n.composicao1(), 140));
        var posterior = registrarAditamento(c, n.id(),
                quantitativo("REDUCAO-CONHECIDA", n.inicio().plusDays(20), n.composicao1(), 75));

        var antiga = quantitativo("REDUCAO-ANTIGA-RECEBIDA-DEPOIS", n.inicio().plusDays(5), n.composicao1(), 75);
        var previa = previaAditamento(c, n.id(), antiga);
        assertThat(previa.path("alertas").toString()).contains("piso", String.valueOf(posterior.path("eventoId").asLong()));
        registrarAditamento(c, n.id(), antiga);
        em.flush();

        assertThat(total(c, n.id(), n.inicio().plusDays(6))).isEqualTo(75);
        assertThat(total(c, n.id(), n.inicio().plusDays(11))).isEqualTo(115);
        assertThat(total(c, n.id(), n.inicio().plusDays(21)))
                .as("RED-006: a sequência 100−25+40−65 não pode publicar total50 abaixo do piso75 sem exceção")
                .isEqualTo(115);
        assertThat(pendenciasDoEvento(n.id(), posterior.path("eventoId").asLong())).isEqualTo(1);
        assertThat(consultar(c.localA, base(n.id()) + "/aditamentos", "TMP-012 documentos posteriores preservados"))
                .hasSize(3);
    }

    @Test
    void cancelarAumentoPreservaExcecaoOriginalERecomposicaoPositivaAbaixoDoPiso() throws Exception {
        var c = criarCenario();
        var n = publicado(c, 100, null, true);
        var aumento = registrarAditamento(c, n.id(),
                quantitativo("AUMENTO-ANTES-EXCECAO", n.inicio().plusDays(1), n.composicao1(), 140));
        conceder(c, n.id(), "REDUCAO_CONTRATUAL_EXCEPCIONAL");
        var excecao = quantitativo("REDUCAO-AUTORIZADA", n.inicio().plusDays(2), n.composicao1(), 60);
        excecao.put("reducaoExcepcional", true);
        excecao.put("fundamentoExcecao", "Ato específico de redução excepcional com fonte publicada");
        var excepcional = registrarAditamento(c, n.id(), excecao);
        var recomposicao = registrarAditamento(c, n.id(),
                quantitativo("RECOMPOSICAO-PARCIAL", n.inicio().plusDays(3), n.composicao1(), 70));

        enviar(c.localA, base(n.id()) + "/aditamentos/" + aumento.path("id").asLong() + "/cancelar",
                mapa("motivo", "Revogação do aumento preserva a exceção válida do instrumento posterior",
                        "referenciaSei", "SEI-CANCELA-AUMENTO"), 200,
                "RED-008 exceção já autorizada pertence ao seu instrumento e não é apagada pelo replay");

        assertThat(total(c, n.id(), n.inicio().plusDays(2))).isEqualTo(20);
        assertThat(total(c, n.id(), n.inicio().plusDays(3))).isEqualTo(30);
        assertThat(pendenciasDoEvento(n.id(), excepcional.path("eventoId").asLong())).isZero();
        assertThat(pendenciasDoEvento(n.id(), recomposicao.path("eventoId").asLong())).isZero();
    }

    @Test
    void indicacaoDeExcecaoEmReducaoNormalNaoCriaAutorizacaoExcepcionalParaReplay() throws Exception {
        var c = criarCenario();
        var n = publicado(c, 100, null, true);
        var aumento = registrarAditamento(c, n.id(),
                quantitativo("AUMENTO-SEM-CONCESSAO", n.inicio().plusDays(1), n.composicao1(), 140));
        var normal = quantitativo("REDUCAO-NORMAL-COM-INDICACAO", n.inicio().plusDays(2), n.composicao1(), 75);
        normal.put("reducaoExcepcional", true);
        normal.put("fundamentoExcecao", "Texto indicado sem concessão excepcional, desnecessária para o piso normal");
        var reducao = registrarAditamento(c, n.id(), normal);

        enviar(c.localA, base(n.id()) + "/aditamentos/" + aumento.path("id").asLong() + "/cancelar",
                mapa("motivo", "Aumento cancelado antes do efeito", "referenciaSei", "SEI-CANCELA-SEM-CONCESSAO"), 200,
                "RED-008 flag não validada como exceção não concede autorização durante o replay");

        assertThat(total(c, n.id(), n.inicio().plusDays(3))).isEqualTo(100);
        assertThat(pendenciasDoEvento(n.id(), reducao.path("eventoId").asLong())).isEqualTo(1);
    }

    private java.util.Map<String, Object> quantitativo(String identificacao, LocalDate data,
                                                       long composicao, int quantidade) {
        var req = aditamento(identificacao, data);
        req.put("novoEfetivoTotal", quantidade);
        req.put("alteracoesComposicao", List.of(mapa("composicaoId", composicao, "quantidadeResultante", quantidade)));
        return req;
    }

    private int total(Cenario c, long contrato, LocalDate data) throws Exception {
        JsonNode posicao = consultar(c.localA, base(contrato) + "/posicao-temporal?data=" + data,
                "RED-006/CMP-003 total reconstruído pelo contrato e suas composições");
        JsonNode composicoes = consultar(c.localA, base(contrato) + "/composicoes?data=" + data,
                "CMP-003 soma do recorte temporal");
        int soma = 0;
        for (JsonNode item : composicoes) soma += item.path("efetivoPrevisto").asInt();
        assertThat(posicao.path("versaoContratual").path("efetivoPrevisto").asInt()).isEqualTo(soma);
        return soma;
    }

    private long pendenciasDoEvento(long contrato, long evento) {
        return em.createQuery("select count(p) from PendenciaAdministrativa p where p.contratoId=:contrato and p.origemId=:evento", Long.class)
                .setParameter("contrato", contrato).setParameter("evento", evento).getSingleResult();
    }
}
