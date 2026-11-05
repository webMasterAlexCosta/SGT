package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.Cargo;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReplayCapacidadeDependenteHttpIT extends ContratosHttpBase {
    @Test
    void aditamentoAnteriorNaoPodeInvalidarRemanejamentoQueSustentaVinculoFuturo() throws Exception {
        verificarReplayCompleto(true);
    }

    @Test
    void remanejamentoAnteriorNaoPodeInvalidarRemanejamentoQueSustentaVinculoFuturo() throws Exception {
        verificarReplayCompleto(false);
    }

    private void verificarReplayCompleto(boolean aditamento) throws Exception {
        var c = criarCenario(); var hoje = RelogioSistema.hoje(); var d1 = hoje.plusDays(5); var d2 = hoje.plusDays(10);
        var cargoB = new Cargo("Cargo B replay", "2"); cargoB.setGestaoLocal(c.gestaoA); em.persist(cargoB);
        var cargoC = new Cargo("Cargo C replay", "3"); cargoC.setGestaoLocal(c.gestaoA); em.persist(cargoC);
        long id = enviar(c.localA, "/api/contratos", rascunho(c, "REPLAY-" + c.contratoA.getId(), 100,
                hoje.plusDays(2), hoje.plusMonths(4)), 201, "CMP-006 contrato futuro com três combinações").path("id").asLong();
        long a = enviar(c.localA, base(id) + "/composicoes", composicao(c, c.cargoA.getId(), 90), 201, "base A90").path("id").asLong();
        long b = enviar(c.localA, base(id) + "/composicoes", composicao(c, cargoB.getId(), 10), 201, "base B10").path("id").asLong();
        long destinoC = enviar(c.localA, base(id) + "/composicoes", composicao(c, cargoC.getId(), 0), 201, "base C0 explícita").path("id").asLong();
        enviar(c.localA, base(id) + "/publicar", mapa("dataPublicacao", hoje, "referenciaSei", "SEI-REPLAY-ORIGINAL"),
                200, "CMP-003 publicação de composição completa");
        var posterior = mapa("identificacao", "REM-POSTERIOR-SUSTENTA-C", "dataEfetiva", d2,
                "justificativa", "Transferir dez vagas para C", "confirmarImpacto", true,
                "itens", List.of(mapa("composicaoId", b, "variacao", -10), mapa("composicaoId", destinoC, "variacao", 10)));
        previaRemanejamento(c, id, posterior);
        var rem = enviar(c.localA, base(id) + "/remanejamentos", posterior, 201, "REM posterior inicialmente compatível");
        long vinculo = enviar(c.localA, "/api/vinculos-contratuais", mapa("contratoId", id, "composicaoId", destinoC,
                "dataInicio", d2, "pessoa", mapa("nome", "Pessoa dependente do replay C", "cpf", "52998224725")),
                201, "CMP-006 vínculo depende da capacidade criada pelo fato posterior").path("id").asLong();
        em.flush(); em.clear();
        var leituraAntes = AssercoesComposicoesHttp.capturar(() -> consultar(c.localA,
                base(id) + "/composicoes?data=" + d2, "CMP-006 posição antes da tentativa preserva C10"));
        var composicoesAntes = leituraAntes.dados();
        assertThat(quantidade(composicoesAntes, destinoC)).isEqualTo(10);
        assertThat(consultar(c.localA, base(id) + "/remanejamentos", "REM posterior está vigente").get(0).path("situacao").asText())
                .isEqualTo("VIGENTE");
        var eventosAntes = consultar(c.localA, base(id) + "/eventos", "fatos antes da tentativa anterior incompatível");
        JsonNode previa;
        if (aditamento) {
            var anterior = aditamento("ADI-ANTERIOR-RETIRA-B", d1); anterior.put("novoEfetivoTotal", 100);
            anterior.put("alteracoesComposicao", List.of(mapa("composicaoId", b, "quantidadeResultante", 0),
                    mapa("composicaoId", a, "quantidadeResultante", 100)));
            previa = previaAditamento(c, id, anterior);
            enviar(c.localA, base(id) + "/aditamentos", anterior, 409,
                    "CMP-006 alteração anterior deve reaplicar REM posterior antes de validar capacidade C");
        } else {
            var anterior = mapa("identificacao", "REM-ANTERIOR-RETIRA-B", "dataEfetiva", d1,
                    "justificativa", "Transferir B para A", "confirmarImpacto", true,
                    "itens", List.of(mapa("composicaoId", b, "variacao", -10), mapa("composicaoId", a, "variacao", 10)));
            previa = previaRemanejamento(c, id, anterior);
            enviar(c.localA, base(id) + "/remanejamentos", anterior, 409,
                    "CMP-006 redistribuição anterior deve reaplicar REM posterior antes de validar capacidade C");
        }
        assertThat(previa.path("possuiImpactoOcupacao").asBoolean()).isTrue();
        assertThat(previa.path("alertas").toString()).contains("Pessoa dependente do replay C");
        em.flush(); em.clear();
        assertThat(consultar(c.localA, base(id) + "/eventos", "CMP-007 recusa não grava fato parcial")).isEqualTo(eventosAntes);
        var leituraDepois = AssercoesComposicoesHttp.capturar(() -> consultar(c.localA,
                base(id) + "/composicoes?data=" + d2, "CMP-007 capacidade posterior preservada"));
        AssercoesComposicoesHttp.exigirPreservacaoComConsultaPosterior(leituraAntes, leituraDepois);
        var remDepois = consultar(c.localA, base(id) + "/remanejamentos", "CMP-007 instrumento dependente preservado");
        assertThat(remDepois).hasSize(1); assertThat(remDepois.get(0).path("id").asLong()).isEqualTo(rem.path("id").asLong());
        assertThat(remDepois.get(0).path("situacao").asText()).isEqualTo("VIGENTE");
        assertThat(consultar(c.localA, "/api/vinculos-contratuais/" + vinculo, "vínculo não foi movido").path("composicaoInicialId").asLong())
                .isEqualTo(destinoC);
    }

    private int quantidade(JsonNode composicoes, long id) {
        for (var composicao : composicoes) if (composicao.path("id").asLong() == id)
            return composicao.path("efetivoPrevisto").asInt();
        throw new AssertionError("Composição publicada não encontrada: " + id);
    }
}
