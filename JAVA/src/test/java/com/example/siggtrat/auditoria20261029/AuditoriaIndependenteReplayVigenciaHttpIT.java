package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.utilitario.RelogioSistema;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** RED independente confirmado em novos_red_04; regressão da vigência compartilhada. */
class AuditoriaIndependenteReplayVigenciaHttpIT extends ContratosHttpBase {
    @Test
    void aditamentoPosteriorDeDescricaoNaoPodeApagarProrrogacaoAnteriorRecebidaDepois() throws Exception {
        var c = criarCenario();
        var n = publicado(c, 100, null, false);

        // Conhecimento primeiro: ato posterior que só altera descrição.
        var descricao = aditamento("DESCRICAO-POSTERIOR", RelogioSistema.hoje().plusDays(10));
        descricao.put("novaDescricao", "Descrição posterior preservada pelo replay");
        registrarAditamento(c, n.id(), descricao);

        // Conhecimento depois: instrumento anterior que prorroga validamente
        // o mesmo contrato. Não existe vínculo encerrado a reabrir.
        var prorrogacao = aditamento("PRORROGACAO-ANTERIOR", RelogioSistema.hoje().plusDays(5));
        prorrogacao.put("novaDataFim", n.fim().plusDays(30));
        registrarAditamento(c, n.id(), prorrogacao);
        em.flush();
        em.clear();

        var diaNovoVinculo = n.fim().plusDays(5);
        var contrato = consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + diaNovoVinculo,
                "TMP008/012/014: replay oficial conserva a prorrogação e o ato posterior de descrição");
        assertThat(contrato.path("situacaoTemporal").asText()).isEqualTo("VIGENTE");
        assertThat(contrato.path("vigenciaFim").asText()).isEqualTo(n.fim().plusDays(30).toString());
        assertThat(contrato.path("versaoContratual").path("dataInicio").asText())
                .isEqualTo(contrato.path("vigenciaInicio").asText());
        assertThat(contrato.path("versaoContratual").path("dataFim").asText())
                .isEqualTo(contrato.path("vigenciaFim").asText());
        assertThat(contrato.path("versaoContratual").path("descricao").asText())
                .isEqualTo("Descrição posterior preservada pelo replay");

        // Oráculo público: o período já confirmado como vigente deve aceitar
        // um novo vínculo programado, sem usar uma fotografia anterior ao delta.
        var vinculo = enviar(c.localA, "/api/vinculos-contratuais", mapa(
                "pessoa", mapa("nome", "Pessoa nova na vigência prorrogada",
                        "cpf", cpf(Math.toIntExact(970000000L + n.id()))),
                "contratoId", n.id(), "composicaoId", n.composicao1(),
                "dataInicio", diaNovoVinculo), 201,
                "TMP014: vínculo novo dentro da vigência reconstruída deve ser aceito");
        assertThat(vinculo.path("dataInicio").asText()).isEqualTo(diaNovoVinculo.toString());
        assertThat(vinculo.path("estadoNegocial").asText()).isEqualTo("REGISTRADO");
    }
}
