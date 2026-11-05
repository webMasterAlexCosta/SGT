package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.Cargo;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReplayCapacidadeAumentoHttpIT extends ContratosHttpBase {
    @Test
    void aumentoAnteriorNaoPodeReabilitarRemanejamentoQueRetiraCapacidadeDeVinculo() throws Exception {
        var c = criarCenario(); var hoje = RelogioSistema.hoje();
        var d1 = hoje.plusDays(5); var d2 = hoje.plusDays(10); var d3 = hoje.plusDays(15);
        var cargoB = new Cargo("Cargo B aumento replay", "2"); cargoB.setGestaoLocal(c.gestaoA); em.persist(cargoB);
        var cargoC = new Cargo("Cargo C aumento replay", "3"); cargoC.setGestaoLocal(c.gestaoA); em.persist(cargoC);
        long id = enviar(c.localA, "/api/contratos", rascunho(c, "AUMENTO-REPLAY-" + c.contratoA.getId(), 2,
                hoje.plusDays(2), hoje.plusMonths(4)), 201, "CMP-006 original A1/B1/C0").path("id").asLong();
        long a = enviar(c.localA, base(id) + "/composicoes", composicao(c, c.cargoA.getId(), 1), 201, "base A1").path("id").asLong();
        long b = enviar(c.localA, base(id) + "/composicoes", composicao(c, cargoB.getId(), 1), 201, "base B1").path("id").asLong();
        long destinoC = enviar(c.localA, base(id) + "/composicoes", composicao(c, cargoC.getId(), 0), 201, "base C0").path("id").asLong();
        enviar(c.localA, base(id) + "/publicar", mapa("dataPublicacao", hoje, "referenciaSei", "SEI-AUMENTO-REPLAY"),
                200, "original futuro com todas as combinações explícitas");
        var posterior = mapa("identificacao", "REM-D3-A-B-PARA-C", "dataEfetiva", d3,
                "justificativa", "Destinar as duas vagas a C", "confirmarImpacto", true,
                "itens", List.of(mapa("composicaoId", a, "variacao", -1), mapa("composicaoId", b, "variacao", -1),
                        mapa("composicaoId", destinoC, "variacao", 2)));
        previaRemanejamento(c, id, posterior);
        var remD3 = enviar(c.localA, base(id) + "/remanejamentos", posterior, 201, "REM d3 inicialmente compatível");
        var intermediario = mapa("identificacao", "REM-D2-B-PARA-C", "dataEfetiva", d2,
                "justificativa", "Antecipar vaga B para C", "confirmarImpacto", true,
                "itens", List.of(mapa("composicaoId", b, "variacao", -1), mapa("composicaoId", destinoC, "variacao", 1)));
        previaRemanejamento(c, id, intermediario);
        enviar(c.localA, base(id) + "/remanejamentos", intermediario, 201,
                "ADI-008/TMP-009 sem dependentes permite preservar instrumento incompatível com pendência");
        long vinculo = enviar(c.localA, "/api/vinculos-contratuais", mapa("contratoId", id, "composicaoId", a,
                "dataInicio", d3, "pessoa", mapa("nome", "Pessoa mantida na capacidade A", "cpf", "52998224725")),
                201, "CMP-006 vínculo usa A1 na projeção em que REM d3 está incompatível").path("id").asLong();
        em.flush(); em.clear();
        var leituraAntes = AssercoesComposicoesHttp.capturar(() -> consultar(c.localA,
                base(id) + "/composicoes?data=" + d3,
                "pré-condição A1/B0/C1 com remanejamento d3 incompatível"));
        var composicoesAntes = leituraAntes.dados();
        assertThat(quantidade(composicoesAntes, a)).isEqualTo(1);
        assertThat(quantidade(composicoesAntes, b)).isZero();
        assertThat(quantidade(composicoesAntes, destinoC)).isEqualTo(1);
        assertThat(em.createQuery("select count(p) from PendenciaAdministrativa p where p.contratoId=:id and p.origemId=:evento", Long.class)
                .setParameter("id", id).setParameter("evento", remD3.path("eventoId").asLong()).getSingleResult()).isEqualTo(1L);
        var eventosAntes = consultar(c.localA, base(id) + "/eventos", "fatos preservados antes do aumento proposto");
        var aumento = aditamento("ADI-D1-SOMENTE-AUMENTO-B", d1); aumento.put("novoEfetivoTotal", 3);
        aumento.put("alteracoesComposicao", List.of(mapa("composicaoId", b, "quantidadeResultante", 2)));
        var previa = previaAditamento(c, id, aumento);
        enviar(c.localA, base(id) + "/aditamentos", aumento, 409,
                "CMP-006 aumento deve simular replay que reabilita REM d3 e retira A do vínculo");
        assertThat(previa.path("possuiImpactoOcupacao").asBoolean()).isTrue();
        assertThat(previa.path("alertas").toString()).contains("Pessoa mantida na capacidade A");
        em.flush(); em.clear();
        assertThat(consultar(c.localA, base(id) + "/eventos", "recusa não grava aumento parcial")).isEqualTo(eventosAntes);
        var leituraDepois = AssercoesComposicoesHttp.capturar(() -> consultar(c.localA,
                base(id) + "/composicoes?data=" + d3, "suporte do vínculo A preservado"));
        AssercoesComposicoesHttp.exigirPreservacaoComConsultaPosterior(leituraAntes, leituraDepois);
        assertThat(consultar(c.localA, "/api/vinculos-contratuais/" + vinculo, "vínculo não foi movido").path("composicaoInicialId").asLong())
                .isEqualTo(a);
    }

    private int quantidade(JsonNode composicoes, long id) {
        for (var composicao : composicoes) if (composicao.path("id").asLong() == id)
            return composicao.path("efetivoPrevisto").asInt();
        throw new AssertionError("Composição publicada não encontrada: " + id);
    }
}
