package com.example.siggtrat.auditoria20261029;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Proposta externa à candidata. Nenhuma execução foi feita pelo auditor. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AuditoriaInstrumentosConcorrentesHttpIT extends ContratosHttpBase {
    @ParameterizedTest
    @ValueSource(strings = {"prorrogacao", "adi_quantitativo", "remanejamento"})
    void intençõesConflitantesComPreviasDoMesmoEstadoNaoPodemConfirmarDuasVersoes(String modalidade) throws Exception {
        var c = criarCenarioPersistido();
        NovoContrato n = emTransacao(() -> {
            try { return publicado(c, 100, 50, true); }
            catch (Exception ex) { throw new IllegalStateException(ex); }
        });
        var efeito = n.inicio().plusDays(1);
        boolean rem = modalidade.equals("remanejamento");
        String familia = rem ? "remanejamentos" : "aditamentos";
        Map<String, Object> primeira = rem ? remanejamento(n, "REM-CONCORRENTE-A", efeito, 5)
                : aditamento("ADI-CONCORRENTE-A", efeito);
        Map<String, Object> segunda = rem ? remanejamento(n, "REM-CONCORRENTE-B", efeito, 10)
                : aditamento("ADI-CONCORRENTE-B", efeito);
        if (modalidade.equals("prorrogacao")) {
            primeira.put("novaDataFim", n.fim().plusDays(10));
            segunda.put("novaDataFim", n.fim().plusDays(20));
        } else if (modalidade.equals("adi_quantitativo")) {
            primeira.put("novoEfetivoTotal", 110);
            primeira.put("alteracoesComposicao", List.of(mapa("composicaoId", n.composicao1(), "quantidadeResultante", 60)));
            segunda.put("novoEfetivoTotal", 115);
            segunda.put("alteracoesComposicao", List.of(mapa("composicaoId", n.composicao1(), "quantidadeResultante", 65)));
        }
        for (var req : List.of(primeira, segunda)) {
            JsonNode previa = enviar(c.localA, base(n.id()) + "/" + familia + "/previa", req, 200,
                    "Duas intenções válidas recebem prévia do mesmo estado original");
            req.put("tokenPrevia", previa.path("tokenPrevia").asText());
        }
        var a = autenticar(post(base(n.id()) + "/" + familia).contentType(MediaType.APPLICATION_JSON).content(json(primeira)), c.localA);
        var b = autenticar(post(base(n.id()) + "/" + familia).contentType(MediaType.APPLICATION_JSON).content(json(segunda)), c.localA);
        var largada = new CountDownLatch(1);
        var prontas = new CountDownLatch(2);
        var pool = Executors.newFixedThreadPool(2);
        List<MvcResult> respostas;
        try {
            var fa = pool.submit(() -> { prontas.countDown(); largada.await(); return mvc.perform(a).andReturn(); });
            var fb = pool.submit(() -> { prontas.countDown(); largada.await(); return mvc.perform(b).andReturn(); });
            assertThat(prontas.await(10, TimeUnit.SECONDS)).isTrue();
            largada.countDown();
            respostas = List.of(fa.get(30, TimeUnit.SECONDS), fb.get(30, TimeUnit.SECONDS));
        } finally {
            largada.countDown();
            pool.shutdownNow();
        }
        assertThat(respostas.stream().map(r -> r.getResponse().getStatus()).toList())
                .as("Uma intenção confirma; a outra encontra prévia obsoleta ou conflito sem precedência")
                .containsExactlyInAnyOrder(201, 409);
        int vencedor = respostas.get(0).getResponse().getStatus() == 201 ? 0 : 1;
        var instrumentos = consultar(c.localA, base(n.id()) + "/" + familia, "Somente uma cadeia foi confirmada");
        assertThat(instrumentos).hasSize(1);
        assertThat(instrumentos.get(0).path("identificacao").asText())
                .isEqualTo((vencedor == 0 ? primeira : segunda).get("identificacao"));
        assertThat(emTransacao(() -> em.createQuery(
                "select count(e) from EventoCicloVidaContrato e where e.contrato.id=:c", Long.class)
                .setParameter("c", n.id()).getSingleResult())).isEqualTo(2L);
        var posicao = consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + efeito,
                "Posição é a do vencedor, sem efeitos parciais da intenção recusada");
        if (modalidade.equals("prorrogacao")) {
            assertThat(posicao.path("vigenciaFim").asText())
                    .isEqualTo(n.fim().plusDays(vencedor == 0 ? 10 : 20).toString());
        }
        var composicoes = consultar(c.localA, base(n.id()) + "/composicoes?data=" + efeito,
                "Distribuição confirma a mesma intenção vencedora após as transações");
        int aEsperado = modalidade.equals("adi_quantitativo") ? (vencedor == 0 ? 60 : 65)
                : rem ? (vencedor == 0 ? 45 : 40) : 50;
        int bEsperado = rem ? (vencedor == 0 ? 55 : 60) : 50;
        for (var comp : composicoes) {
            assertThat(comp.path("efetivoPrevisto").asInt()).isEqualTo(
                    comp.path("id").asLong() == n.composicao1() ? aEsperado : bEsperado);
        }
        assertThat(composicoes).hasSize(2);
        assertThat(posicao.path("versaoContratual").path("efetivoPrevisto").asInt()).isEqualTo(aEsperado + bEsperado);
    }
}
