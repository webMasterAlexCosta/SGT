package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.pendencia.EstadoPendencia;
import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.servico.ComposicaoContratualTemporalServico;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** ADI-007/008: o oráculo usa totais e deltas exatos, com leitura após cada transação HTTP. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ConclusaoAditamentoRetificacaoQuantitativaHttpIT extends ContratosHttpBase {
    @Autowired private ComposicaoContratualTemporalServico composicaoTemporal;

    @Test
    void retificacoesQuantitativasPreservamValoresOriginaisECortesDeConhecimentoSemSomarVersoes() throws Exception {
        var c = criarCenarioPersistido();
        var n = preparar(c, null);
        LocalDate efeito = n.inicio().plusDays(2);
        var original = registrar(c, n.id(), quantitativo(n, "ADI-QUANTIDADE-CORRIGIDA", efeito, 120, 120), null, 201);
        Instant conhecidoOriginal = Instant.now();
        long eventosAntes = contar(n.id(), "EventoCicloVidaContrato");
        long versoesAntes = contar(n.id(), "VersaoContratoNegocial");
        var invalido = quantitativo(n, "ADI-QUANTIDADE-CORRIGIDA", efeito, 74, 74);
        retifica(invalido, original);
        registrar(c, n.id(), invalido, original.path("id").asLong(), 409);
        assertThat(contar(n.id(), "AditamentoContratual")).isEqualTo(1L);
        assertThat(contar(n.id(), "EventoCicloVidaContrato")).isEqualTo(eventosAntes);
        assertThat(contar(n.id(), "VersaoContratoNegocial")).isEqualTo(versoesAntes);
        assertTotal(c, n, efeito, null, 120);
        assertThat(consultar(c.localA, base(n.id()) + "/aditamentos", "ADI007 recusa não retifica o original")
                .get(0).path("situacao").asText()).isEqualTo("VIGENTE");

        var segundaReq = quantitativo(n, "ADI-QUANTIDADE-CORRIGIDA", efeito, 110, 110);
        retifica(segundaReq, original);
        var segunda = registrar(c, n.id(), segundaReq, original.path("id").asLong(), 200);
        Instant conhecidoSegunda = Instant.now();
        var terceiraReq = quantitativo(n, "ADI-QUANTIDADE-CORRIGIDA", efeito, 115, 115);
        retifica(terceiraReq, segunda);
        var terceira = registrar(c, n.id(), terceiraReq, segunda.path("id").asLong(), 200);

        var cadeia = consultar(c.localA, base(n.id()) + "/aditamentos", "ADI007 todas as fotografias quantitativas permanecem");
        assertThat(cadeia).hasSize(3);
        for (int i = 0; i < 3; i++) {
            var item = cadeia.get(i).path("alteracoesComposicao").get(0);
            int resultado = new int[]{120, 110, 115}[i];
            assertThat(cadeia.get(i).path("versaoInstrumento").asInt()).isEqualTo(i + 1);
            assertThat(cadeia.get(i).path("situacao").asText()).isEqualTo(i < 2 ? "RETIFICADO" : "VIGENTE");
            assertThat(cadeia.get(i).path("dataEfetiva").asText()).isEqualTo(efeito.toString());
            assertThat(item.path("composicaoId").asLong()).isEqualTo(n.composicao1());
            assertThat(item.path("quantidadeAnterior").asInt()).isEqualTo(100);
            assertThat(item.path("variacao").asInt()).isEqualTo(resultado - 100);
            assertThat(item.path("quantidadeResultante").asInt()).isEqualTo(resultado);
        }
        assertThat(cadeia.get(0).path("id").asLong()).isEqualTo(original.path("id").asLong());
        assertThat(cadeia.get(1).path("id").asLong()).isEqualTo(segunda.path("id").asLong());
        assertThat(cadeia.get(2).path("id").asLong()).isEqualTo(terceira.path("id").asLong());
        assertTotal(c, n, efeito.minusDays(1), null, 100);
        assertTotal(c, n, efeito, null, 115);
        assertTotal(c, n, efeito, conhecidoOriginal, 120);
        assertTotal(c, n, efeito, conhecidoSegunda, 110);
        assertThat(contar(n.id(), "AditamentoContratual")).isEqualTo(3L);
        assertThat(contar(n.id(), "EventoCicloVidaContrato")).isEqualTo(eventosAntes + 2);
    }

    @Test
    void retificarAntecedentePreservaPosteriorIncompativelESuspendeTodaSuaRedistribuicao() throws Exception {
        var c = criarCenarioPersistido();
        var n = preparar(c, 25);
        LocalDate antecedente = n.inicio().plusDays(2), posterior = antecedente.plusDays(4);
        var original = registrar(c, n.id(), quantitativo(n, "ADI-ANTECEDENTE-CORRIGIDO", antecedente, 30, 105), null, 201);
        var remReq = remanejamento(n, "REM-POSTERIOR-PRESERVADO", posterior, 25);
        var previaRem = enviar(c.localA, base(n.id()) + "/remanejamentos/previa", remReq, 200, "REM002 redistribuição original válida");
        remReq.put("tokenPrevia", previaRem.path("tokenPrevia").asText());
        var rem = enviar(c.localA, base(n.id()) + "/remanejamentos", remReq, 201, "REM002 30/75 passa a 5/100 sem mudar105");
        Instant conhecidoAntes = Instant.now();
        assertDistribuicao(c, n, posterior, null, 5, 100, 105);

        var correcao = quantitativo(n, "ADI-ANTECEDENTE-CORRIGIDO", antecedente, 20, 95);
        retifica(correcao, original);
        registrar(c, n.id(), correcao, original.path("id").asLong(), 200);
        assertDistribuicao(c, n, antecedente.minusDays(1), null, 25, 75, 100);
        assertDistribuicao(c, n, antecedente, null, 20, 75, 95);
        // O posterior tentaria produzir -5/100; nenhum de seus dois deltas pode
        // ser aplicado isoladamente. A verdade válida permanece exatamente20/75.
        assertDistribuicao(c, n, posterior, null, 20, 75, 95);
        assertDistribuicao(c, n, posterior, conhecidoAntes, 5, 100, 105);
        var remanejos = consultar(c.localA, base(n.id()) + "/remanejamentos", "ADI008 incompatibilidade não apaga ou retifica silenciosamente o posterior");
        assertThat(remanejos).hasSize(1);
        assertThat(remanejos.get(0).path("id").asLong()).isEqualTo(rem.path("id").asLong());
        assertThat(remanejos.get(0).path("eventoId").asLong()).isEqualTo(rem.path("eventoId").asLong());
        assertThat(remanejos.get(0).path("situacao").asText()).isEqualTo("VIGENTE");
        assertThat(remanejos.get(0).path("itens")).isEqualTo(rem.path("itens"));
        assertThat(contar(n.id(), "AditamentoContratual")).isEqualTo(2L);
        assertThat(contar(n.id(), "RemanejamentoContratual")).isEqualTo(1L);
        assertThat(emTransacao(() -> em.createQuery("select count(p) from PendenciaAdministrativa p where p.contratoId=:c "
                        + "and p.origemId=:e and p.estado=:estado", Long.class)
                .setParameter("c", n.id()).setParameter("e", rem.path("eventoId").asLong())
                .setParameter("estado", EstadoPendencia.ABERTA).getSingleResult())).isEqualTo(1L);
    }

    private NovoContrato preparar(Cenario c, Integer primeira) {
        return emTransacao(() -> {
            try { return publicado(c, 100, primeira, false); }
            catch (Exception ex) { throw new IllegalStateException(ex); }
        });
    }

    private Map<String, Object> quantitativo(NovoContrato n, String identificacao, LocalDate data, int quantidade, int total) {
        var req = aditamento(identificacao, data);
        req.put("novoEfetivoTotal", total);
        req.put("alteracoesComposicao", List.of(mapa("composicaoId", n.composicao1(), "quantidadeResultante", quantidade)));
        return req;
    }

    private void retifica(Map<String, Object> req, JsonNode anterior) {
        req.put("eventoPrecedenteId", anterior.path("eventoId").asLong());
        req.put("relacaoPrecedencia", "RETIFICA");
        req.put("justificativa", "Correção documental explícita do quantitativo anterior");
    }

    private JsonNode registrar(Cenario c, long contrato, Map<String, Object> req, Long retificado, int status) throws Exception {
        var previa = enviar(c.localA, base(contrato) + "/aditamentos/previa", req, 200, "ADI007 prévia do quantitativo pretendido");
        req.put("tokenPrevia", previa.path("tokenPrevia").asText());
        return enviar(c.localA, base(contrato) + "/aditamentos" + (retificado == null ? "" : "/" + retificado + "/retificar"),
                req, status, "ADI007/008 ato quantitativo explícito e auditável");
    }

    private void assertTotal(Cenario c, NovoContrato n, LocalDate data, Instant corte, int total) throws Exception {
        String conhecimento = corte == null ? "" : "&conhecidoAte=" + corte;
        var posicao = consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + data + conhecimento,
                "ADI007 somente a versão válida no corte participa do total");
        assertThat(posicao.path("versaoContratual").path("efetivoPrevisto").asInt()).isEqualTo(total);
        var composicoes = composicoes(c, n, data, corte);
        assertThat(composicoes).hasSize(1);
        assertThat(composicoes.get(0).path("efetivoPrevisto").asInt()).isEqualTo(total);
    }

    private void assertDistribuicao(Cenario c, NovoContrato n, LocalDate data, Instant corte, int primeira, int segunda, int total) throws Exception {
        String conhecimento = corte == null ? "" : "&conhecidoAte=" + corte;
        var composicoes = composicoes(c, n, data, corte);
        assertThat(composicoes).hasSize(2);
        assertThat(java.util.stream.StreamSupport.stream(composicoes.spliterator(), false)
                .map(comp -> comp.path("id").asLong()).toList())
                .containsExactlyInAnyOrder(n.composicao1(), n.composicao2());
        for (var comp : composicoes) {
            long id = comp.path("id").asLong();
            assertThat(id).isIn(n.composicao1(), n.composicao2());
            assertThat(comp.path("efetivoPrevisto").asInt()).isEqualTo(id == n.composicao1() ? primeira : segunda);
        }
        assertThat(java.util.stream.StreamSupport.stream(composicoes.spliterator(), false)
                .mapToLong(comp -> comp.path("efetivoPrevisto").asLong()).sum()).isEqualTo(total);
        assertThat(consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + data + conhecimento,
                "CMP003 total é a soma integral da distribuição efetiva").path("versaoContratual").path("efetivoPrevisto").asInt())
                .isEqualTo(total);
    }

    private JsonNode composicoes(Cenario c, NovoContrato n, LocalDate data, Instant corte) throws Exception {
        if (corte == null) return consultar(c.localA, base(n.id()) + "/composicoes?data=" + data,
                "CMP003 distribuição HTTP válida na data");
        // A rota de composição aceita data efetiva; o corte de conhecimento é
        // verificado no serviço canônico real, com nova transação e PostgreSQL.
        return emTransacao(() -> mapper.valueToTree(composicaoTemporal.projetarDTO(em.find(Contrato.class, n.id()), data, corte)));
    }

    private long contar(long id, String entidade) {
        return emTransacao(() -> em.createQuery("select count(e) from " + entidade + " e where e.contrato.id=:id", Long.class)
                .setParameter("id", id).getSingleResult());
    }
}
