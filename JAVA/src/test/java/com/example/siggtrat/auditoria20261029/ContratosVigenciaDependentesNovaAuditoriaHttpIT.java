package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.AditamentoContratual;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Nova auditoria sobre a fonte a695a0af05c2b2878bdbeafcd07648b91dd882ab1aeaa164ccf070733014405f.
 * CON-017 / ADI-006 / TMP-014: uma prorrogação futura pode ser a fonte de
 * cobertura de outros instrumentos, mesmo sem haver qualquer vínculo humano.
 * PostgreSQL real, sem transação externa envolvendo o comando sob auditoria.
 * Instalar apenas em checkout de trabalho; não alterar a fonte congelada.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ContratosVigenciaDependentesNovaAuditoriaHttpIT extends ContratosHttpBase {
    @ParameterizedTest(name = "cancelamento preserva cobertura de posterior REM={0}")
    @ValueSource(booleans = {false, true})
    void cancelarProrrogacaoExigeTratarInstrumentosPosterioresMesmoSemVinculos(boolean rem) throws Exception {
        var c = criarCenarioPersistido();
        var n = emTransacao(() -> {
            try { return publicado(c, 100, 50, false); }
            catch (Exception ex) { throw new IllegalStateException(ex); }
        });
        var extensao = aditamento("PRORROGACAO-FONTE-COBERTURA", RelogioSistema.hoje().plusDays(5));
        extensao.put("novaDataFim", n.fim().plusDays(30));
        var instrumento = registrarEmTransacao(c, n.id(), extensao);
        var dataPosterior = n.fim().plusDays(2);
        JsonNode posterior;
        if (rem) {
            var req = remanejamento(n, "REM-DEPENDE-DA-PRORROGACAO", dataPosterior, 5);
            posterior = emTransacao(() -> {
                try {
                    previaRemanejamento(c, n.id(), req);
                    return enviar(c.localA, base(n.id()) + "/remanejamentos", req, 201,
                            "REM posterior possui vigência válida antes do cancelamento");
                } catch (Exception ex) { throw new IllegalStateException(ex); }
            });
        } else {
            var req = aditamento("ADI-DEPENDE-DA-PRORROGACAO", dataPosterior);
            req.put("novaDescricao", "Descrição que depende do intervalo prorrogado");
            posterior = registrarEmTransacao(c, n.id(), req);
        }
        assertThat(contar(n.id(), "VinculoContratual")).as("precondição: nenhuma dependência humana").isZero();
        long eventosAntes = contar(n.id(), "EventoCicloVidaContrato");
        long versoesAntes = contar(n.id(), "VersaoContratoNegocial");
        var posicaoAntes = consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + dataPosterior,
                "a fonte da vigência existe antes da tentativa de retirada");
        assertThat(posicaoAntes.path("situacaoTemporal").asText()).isEqualTo("VIGENTE");
        assertThat(posicaoAntes.path("eventosAplicados").findValues("id"))
                .extracting(JsonNode::asLong).contains(posterior.path("eventoId").asLong());

        var resposta = mvc.perform(autenticar(post(base(n.id()) + "/aditamentos/"
                        + instrumento.path("id").asLong() + "/cancelar").contentType(MediaType.APPLICATION_JSON)
                .content(json(mapa("motivo", "Retirar prorrogação sem apagar ou invalidar silenciosamente instrumentos posteriores",
                        "referenciaSei", "SEI-AUDITORIA-CANCELAMENTO-PRORROGACAO"))), c.localA)).andReturn();
        int status = resposta.getResponse().getStatus();
        assertThat(status).as("A operação deve recusar integralmente ou registrar tratamento explícito: %s",
                resposta.getResponse().getContentAsString()).isIn(200, 409);
        var posicaoDepois = consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + dataPosterior,
                "Posição em nova leitura após a tentativa de retirar cobertura");
        if (status == 409) {
            // Cada leitura abre outra transação; a recusa deve preservar todo o estado.
            assertThat(contar(n.id(), "EventoCicloVidaContrato")).isEqualTo(eventosAntes);
            assertThat(contar(n.id(), "VersaoContratoNegocial")).isEqualTo(versoesAntes);
            assertThat(emTransacao(() -> em.find(AditamentoContratual.class, instrumento.path("id").asLong())
                    .getEvento().getSituacaoFato().name())).isEqualTo("VIGENTE");
            assertThat(posicaoDepois.path("vigenciaFim").asText()).isEqualTo(n.fim().plusDays(30).toString());
        } else {
            // Não se impõe uma solução específica: aceitar é possível somente se o fato
            // dependente perder efeito operacional e sua incompatibilidade ficar explícita.
            assertThat(posicaoDepois.path("vigenciaFim").asText()).isEqualTo(n.fim().toString());
            assertThat(posicaoDepois.path("eventosAplicados").findValues("id"))
                    .as("TMP-014: evento %s não pode produzir efeito em %s depois do fim %s; posição=%s",
                            posterior.path("eventoId").asLong(), dataPosterior, n.fim(), posicaoDepois)
                    .extracting(JsonNode::asLong).doesNotContain(posterior.path("eventoId").asLong());
            assertThat(emTransacao(() -> em.createQuery("select count(p) from PendenciaAdministrativa p "
                            + "where p.contratoId=:c and p.origemId=:e and p.motivo is not null", Long.class)
                    .setParameter("c", n.id()).setParameter("e", posterior.path("eventoId").asLong()).getSingleResult()))
                    .as("Incompatibilidade deve identificar o evento dependente preservado").isPositive();
            if (rem) {
                var composicoes = consultar(c.localA, base(n.id()) + "/composicoes?data=" + dataPosterior,
                        "Redistribuição fora da vigência não pode continuar aplicada");
                for (var item : composicoes) assertThat(item.path("efetivoPrevisto").asInt()).isEqualTo(50);
            } else {
                assertThat(posicaoDepois.path("versaoContratual").path("descricao").asText())
                        .isEqualTo("Contrato de auditoria HTTP");
            }
        }
    }

    @ParameterizedTest(name = "cobertura independente; REM={0}; cobertura registrada primeiro={1}")
    @CsvSource({"false,false", "false,true", "true,false", "true,true"})
    void coberturaIndependentePermiteCancelarProrrogacaoSemRetirarPosteriorValido(
            boolean rem, boolean coberturaPrimeiro) throws Exception {
        var c = criarCenarioPersistido();
        var n = emTransacao(() -> {
            try { return publicado(c, 100, 50, false); }
            catch (Exception ex) { throw new IllegalStateException(ex); }
        });
        var removivel = aditamento("PRORROGACAO-REMOVIVEL", RelogioSistema.hoje().plusDays(5));
        removivel.put("novaDataFim", n.fim().plusDays(30));
        var cobertura = aditamento("PRORROGACAO-INDEPENDENTE", RelogioSistema.hoje().plusDays(6));
        cobertura.put("novaDataFim", n.fim().plusDays(40));
        if (coberturaPrimeiro) registrarEmTransacao(c, n.id(), cobertura);
        var instrumento = registrarEmTransacao(c, n.id(), removivel);
        if (!coberturaPrimeiro) registrarEmTransacao(c, n.id(), cobertura);
        var dia = n.fim().plusDays(2);
        JsonNode posterior;
        if (rem) {
            var req = remanejamento(n, "REM-COM-COBERTURA-INDEPENDENTE", dia, 5);
            posterior = emTransacao(() -> {
                try {
                    previaRemanejamento(c, n.id(), req);
                    return enviar(c.localA, base(n.id()) + "/remanejamentos", req, 201, "Posterior coberto");
                } catch (Exception ex) { throw new IllegalStateException(ex); }
            });
        } else {
            var req = aditamento("ADI-COM-COBERTURA-INDEPENDENTE", dia);
            req.put("novaDescricao", "Posterior válido preservado");
            posterior = registrarEmTransacao(c, n.id(), req);
        }
        enviar(c.localA, base(n.id()) + "/aditamentos/" + instrumento.path("id").asLong() + "/cancelar",
                mapa("motivo", "Ato redundante retirado; cobertura independente documentada permanece",
                        "referenciaSei", "SEI-COBERTURA-INDEPENDENTE"), 200,
                "Uma segunda prorrogação válida deve preservar a cobertura do posterior");
        var posicao = consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + dia,
                "Cobertura independe da ordem de digitação dos dois atos");
        assertThat(posicao.path("situacaoTemporal").asText()).isEqualTo("VIGENTE");
        assertThat(posicao.path("vigenciaFim").asText()).isEqualTo(n.fim().plusDays(40).toString());
        assertThat(posicao.path("eventosAplicados").findValues("id"))
                .extracting(JsonNode::asLong).contains(posterior.path("eventoId").asLong());
        if (!rem) assertThat(posicao.path("versaoContratual").path("descricao").asText())
                .isEqualTo("Posterior válido preservado");
        else {
            var composicoes = consultar(c.localA, base(n.id()) + "/composicoes?data=" + dia,
                    "Redistribuição válida permanece integral");
            for (var item : composicoes) assertThat(item.path("efetivoPrevisto").asInt())
                    .isEqualTo(item.path("id").asLong() == n.composicao1() ? 45 : 55);
        }
    }

    private JsonNode registrarEmTransacao(Cenario c, long contrato, Map<String, Object> requisicao) {
        return emTransacao(() -> {
            try { return registrarAditamento(c, contrato, requisicao); }
            catch (Exception ex) { throw new IllegalStateException(ex); }
        });
    }

    private long contar(long contrato, String entidade) {
        return emTransacao(() -> em.createQuery("select count(e) from " + entidade + " e where e.contrato.id=:id", Long.class)
                .setParameter("id", contrato).getSingleResult());
    }
}
