package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.AditamentoContratual;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** TMP-014 / ADI-008: retirada de cobertura por retificação e continuidade de prorrogação dependente. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ContratosVigenciaRetificacaoContinuidadeNovaAuditoriaHttpIT extends ContratosHttpBase {
    @ParameterizedTest(name = "retificação preserva instrumento posterior sem vínculos; REM={0}")
    @ValueSource(booleans = {false, true})
    void retificarProrrogacaoNaoPodeDeixarInstrumentoPosteriorSemCobertura(boolean rem) throws Exception {
        var c = criarCenarioPersistido();
        var n = emTransacao(() -> {
            try { return publicado(c, 100, 50, false); }
            catch (Exception ex) { throw new IllegalStateException(ex); }
        });
        var efeito = RelogioSistema.hoje().plusDays(5);
        var extensao = aditamento("PRORROGACAO-RETIFICAVEL", efeito);
        extensao.put("novaDataFim", n.fim().plusDays(30));
        var original = registrarEmTransacao(c, n.id(), extensao);
        var dia = n.fim().plusDays(2);
        JsonNode posterior;
        if (rem) {
            var req = remanejamento(n, "REM-DEPENDE-DA-RETIFICACAO", dia, 5);
            posterior = emTransacao(() -> {
                try {
                    previaRemanejamento(c, n.id(), req);
                    return enviar(c.localA, base(n.id()) + "/remanejamentos", req, 201,
                            "REM possui cobertura antes da retificação da prorrogação");
                } catch (Exception ex) { throw new IllegalStateException(ex); }
            });
        } else {
            var req = aditamento("ADI-DEPENDE-DA-RETIFICACAO", dia);
            req.put("novaDescricao", "Instrumento posterior documentado e coberto");
            posterior = registrarEmTransacao(c, n.id(), req);
        }
        assertThat(contar(n.id(), "VinculoContratual")).isZero();
        long eventosAntes = contar(n.id(), "EventoCicloVidaContrato");
        long versoesAntes = contar(n.id(), "VersaoContratoNegocial");
        var retificar = aditamento("PRORROGACAO-RETIFICAVEL", efeito);
        retificar.put("novaDescricao", "Retificação que conserva apenas o texto");
        retificar.put("eventoPrecedenteId", original.path("eventoId").asLong());
        retificar.put("relacaoPrecedencia", "RETIFICA");
        emTransacao(() -> {
            try { return previaAditamento(c, n.id(), retificar); }
            catch (Exception ex) { throw new IllegalStateException(ex); }
        });
        enviar(c.localA, base(n.id()) + "/aditamentos/" + original.path("id").asLong() + "/retificar",
                retificar, 409, "TMP-014: retificação não pode retirar a única cobertura do instrumento posterior");
        assertThat(contar(n.id(), "EventoCicloVidaContrato")).isEqualTo(eventosAntes);
        assertThat(contar(n.id(), "VersaoContratoNegocial")).isEqualTo(versoesAntes);
        assertThat(situacaoOriginal(original)).isEqualTo("VIGENTE");
        var intacta = consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + dia,
                "Recusa integral preserva cobertura e instrumento dependente");
        assertThat(intacta.path("vigenciaFim").asText()).isEqualTo(n.fim().plusDays(30).toString());
        assertThat(intacta.path("eventosAplicados").findValues("id"))
                .extracting(JsonNode::asLong).contains(posterior.path("eventoId").asLong());

        retificar.put("novaDataFim", n.fim().plusDays(30));
        emTransacao(() -> {
            try { return previaAditamento(c, n.id(), retificar); }
            catch (Exception ex) { throw new IllegalStateException(ex); }
        });
        enviar(c.localA, base(n.id()) + "/aditamentos/" + original.path("id").asLong() + "/retificar",
                retificar, 200, "ADI-008: retificação que conserva cobertura mantém posterior válido");
        var valida = consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + dia,
                "O instrumento posterior continua aplicado depois da retificação válida");
        assertThat(valida.path("situacaoTemporal").asText()).isEqualTo("VIGENTE");
        assertThat(valida.path("vigenciaFim").asText()).isEqualTo(n.fim().plusDays(30).toString());
        assertThat(valida.path("eventosAplicados").findValues("id"))
                .extracting(JsonNode::asLong).contains(posterior.path("eventoId").asLong());
        if (!rem) assertThat(valida.path("versaoContratual").path("descricao").asText())
                .isEqualTo("Instrumento posterior documentado e coberto");
        else for (var item : consultar(c.localA, base(n.id()) + "/composicoes?data=" + dia,
                "O remanejamento dependente continua integral depois da retificação válida"))
            assertThat(item.path("efetivoPrevisto").asInt()).isEqualTo(
                    item.path("id").asLong() == n.composicao1() ? 45 : 55);
    }

    @ParameterizedTest(name = "prorrogação posterior não esconde lacuna; retificação={0}")
    @ValueSource(booleans = {false, true})
    void retirarPrimeiraProrrogacaoNaoPodePermitirAutocoberturaComLacuna(boolean retificacao) throws Exception {
        var c = criarCenarioPersistido();
        var n = emTransacao(() -> {
            try { return publicado(c, 100, null, false); }
            catch (Exception ex) { throw new IllegalStateException(ex); }
        });
        var efeito = RelogioSistema.hoje().plusDays(5);
        var primeira = aditamento("PRORROGACAO-QUE-COBRE-INTERVALO", efeito);
        primeira.put("novaDataFim", n.fim().plusDays(30));
        var original = registrarEmTransacao(c, n.id(), primeira);
        var dia = n.fim().plusDays(2);
        var segunda = aditamento("PRORROGACAO-POSTERIOR-DEPENDENTE", dia);
        segunda.put("novaDataFim", n.fim().plusDays(40));
        var posterior = registrarEmTransacao(c, n.id(), segunda);
        assertThat(contar(n.id(), "VinculoContratual")).isZero();
        long eventosAntes = contar(n.id(), "EventoCicloVidaContrato");
        long versoesAntes = contar(n.id(), "VersaoContratoNegocial");
        String rota = base(n.id()) + "/aditamentos/" + original.path("id").asLong();
        if (retificacao) {
            var req = aditamento("PRORROGACAO-QUE-COBRE-INTERVALO", efeito);
            req.put("novaDescricao", "Retificação sem o intervalo da primeira prorrogação");
            req.put("eventoPrecedenteId", original.path("eventoId").asLong());
            req.put("relacaoPrecedencia", "RETIFICA");
            emTransacao(() -> {
                try { return previaAditamento(c, n.id(), req); }
                catch (Exception ex) { throw new IllegalStateException(ex); }
            });
            enviar(c.localA, rota + "/retificar", req, 409,
                    "TMP-014: retificar a cobertura não pode criar lacuna antes do segundo ato de prorrogação");
        } else enviar(c.localA, rota + "/cancelar", mapa("motivo", "Retirada da primeira cobertura documentada",
                        "referenciaSei", "SEI-CONTINUIDADE-PRORROGACAO"), 409,
                "TMP-014: segunda prorrogação não se autocobre sobre lacuna no contrato");
        assertThat(contar(n.id(), "EventoCicloVidaContrato")).isEqualTo(eventosAntes);
        assertThat(contar(n.id(), "VersaoContratoNegocial")).isEqualTo(versoesAntes);
        assertThat(situacaoOriginal(original)).isEqualTo("VIGENTE");
        var intervalo = consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + n.fim().plusDays(1),
                "Recusa mantém o primeiro dia depois do fim original coberto");
        assertThat(intervalo.path("situacaoTemporal").asText()).isEqualTo("VIGENTE");
        assertThat(intervalo.path("vigenciaFim").asText()).isEqualTo(n.fim().plusDays(30).toString());
        var posicao = consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + dia,
                "Segunda prorrogação válida permanece íntegra após a recusa");
        assertThat(posicao.path("vigenciaFim").asText()).isEqualTo(n.fim().plusDays(40).toString());
        assertThat(posicao.path("eventosAplicados").findValues("id"))
                .extracting(JsonNode::asLong).contains(posterior.path("eventoId").asLong());
    }

    @ParameterizedTest(name = "fronteira fim anterior+1 mantém continuidade; retificação={0}")
    @ValueSource(booleans = {false, true})
    void prorrogacaoNoDiaSeguinteAoFimOriginalPermaneceValidaSemPrimeiraFonte(boolean retificacao) throws Exception {
        var c = criarCenarioPersistido();
        var n = emTransacao(() -> {
            try { return publicado(c, 10, null, false); }
            catch (Exception ex) { throw new IllegalStateException(ex); }
        });
        var efeito = RelogioSistema.hoje().plusDays(5);
        var primeira = aditamento("PRORROGACAO-REDUNDANTE-NA-FRONTEIRA", efeito);
        primeira.put("novaDataFim", n.fim().plusDays(30));
        var original = registrarEmTransacao(c, n.id(), primeira);
        var dia = n.fim().plusDays(1);
        var segunda = aditamento("PRORROGACAO-CONTINUA-NA-FRONTEIRA", dia);
        segunda.put("novaDataFim", n.fim().plusDays(40));
        var posterior = registrarEmTransacao(c, n.id(), segunda);
        String rota = base(n.id()) + "/aditamentos/" + original.path("id").asLong();
        if (retificacao) {
            var req = aditamento("PRORROGACAO-REDUNDANTE-NA-FRONTEIRA", efeito);
            req.put("novaDescricao", "Retificação conserva a continuidade por fonte independente");
            req.put("eventoPrecedenteId", original.path("eventoId").asLong());
            req.put("relacaoPrecedencia", "RETIFICA");
            emTransacao(() -> {
                try { return previaAditamento(c, n.id(), req); }
                catch (Exception ex) { throw new IllegalStateException(ex); }
            });
            enviar(c.localA, rota + "/retificar", req, 200,
                    "Prorrogação no primeiro dia após o fim mantém continuidade por si");
        } else enviar(c.localA, rota + "/cancelar", mapa("motivo", "Retirar ato redundante com continuidade preservada",
                        "referenciaSei", "SEI-PRORROGACAO-FRONTEIRA"), 200,
                "Cancelamento mantém prorrogação independente na fronteira válida");
        var posicao = consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + dia,
                "Continuidade fim anterior+1 permanece válida em nova leitura");
        assertThat(posicao.path("situacaoTemporal").asText()).isEqualTo("VIGENTE");
        assertThat(posicao.path("vigenciaFim").asText()).isEqualTo(n.fim().plusDays(40).toString());
        assertThat(posicao.path("eventosAplicados").findValues("id"))
                .extracting(JsonNode::asLong).contains(posterior.path("eventoId").asLong());
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

    private String situacaoOriginal(JsonNode original) {
        return emTransacao(() -> em.find(AditamentoContratual.class, original.path("id").asLong())
                .getEvento().getSituacaoFato().name());
    }
}
