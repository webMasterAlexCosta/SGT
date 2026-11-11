package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CancelamentoCapacidadeContratualHttpIT extends ContratosHttpBase {
    @Test
    void cancelarAditamentoQueReativouComposicaoComVinculoFuturoEhBloqueadoSemMutacao() throws Exception {
        verificarCancelamento(false);
    }

    @Test
    void cancelarRemanejamentoQueAtivouComposicaoComVinculoFuturoEhBloqueadoSemMutacao() throws Exception {
        verificarCancelamento(true);
    }

    @Test
    void cancelarProrrogacaoFuturaNaoPodeDeixarVinculoProgramadoForaDaVigencia() throws Exception {
        var c = criarCenario(); var n = publicado(c, 100, null, false);
        var req = aditamento("PRORROGACAO-PROGRAMADA", RelogioSistema.hoje().plusDays(5));
        req.put("novaDataFim", n.fim().plusDays(30)); var instrumento = registrarAditamento(c, n.id(), req);
        var vinculo = enviar(c.localA, "/api/vinculos-contratuais", mapa("contratoId", n.id(), "composicaoId", n.composicao1(),
                "dataInicio", n.fim().plusDays(1), "pessoa", mapa("nome", "Pessoa dependente da prorrogação", "cpf", "52998224725")),
                201, "VIN-008 programação posterior ao fim original depende de prorrogação válida");
        em.flush(); em.clear();
        var eventos = consultar(c.localA, base(n.id()) + "/eventos", "CON-017 fatos antes da tentativa de cancelamento");
        enviar(c.localA, base(n.id()) + "/aditamentos/" + instrumento.path("id").asLong() + "/cancelar",
                mapa("motivo", "Retirar extensão futura que já tem dependentes", "referenciaSei", "SEI-CANCELA-PRORROGA"), 409,
                "VIN-008/CMP-006 cancelamento de prorrogação exige tratamento dos vínculos futuros");
        em.flush(); em.clear();
        assertThat(consultar(c.localA, base(n.id()) + "/eventos", "VIN-008 cancelamento recusado preserva fatos")).isEqualTo(eventos);
        assertThat(consultar(c.localA, "/api/vinculos-contratuais/" + vinculo.path("id").asLong(),
                "VIN-008 vínculo programado permanece registrado").path("estadoNegocial").asText()).isEqualTo("REGISTRADO");
    }

    private void verificarCancelamento(boolean remanejamento) throws Exception {
        var c = criarCenario(); var n = publicado(c, 100, 100, false);
        var efeito = RelogioSistema.hoje().plusDays(5); var inicioPessoa = efeito.plusDays(1);
        String familia = remanejamento ? "remanejamentos" : "aditamentos";
        JsonNode instrumento;
        if (remanejamento) {
            var req = remanejamento(n, "CAPACIDADE-FUTURA-REM", efeito, 1); previaRemanejamento(c, n.id(), req);
            instrumento = enviar(c.localA, base(n.id()) + "/remanejamentos", req, 201, "CMP-006 REM cria capacidade futura na composição zerada");
        } else {
            var req = aditamento("CAPACIDADE-FUTURA-ADI", efeito); req.put("novoEfetivoTotal", 101);
            req.put("alteracoesComposicao", List.of(mapa("composicaoId", n.composicao2(), "quantidadeResultante", 1)));
            instrumento = registrarAditamento(c, n.id(), req);
        }
        var vinculo = enviar(c.localA, "/api/vinculos-contratuais", mapa("contratoId", n.id(), "composicaoId", n.composicao2(),
                "dataInicio", inicioPessoa, "pessoa", mapa("nome", "Pessoa dependente da capacidade futura", "cpf", "52998224725")),
                201, "CMP-006 vínculo futuro depende da capacidade publicada");
        long vinculoId = vinculo.path("id").asLong();
        em.flush(); em.clear();
        var eventosAntes = consultar(c.localA, base(n.id()) + "/eventos", "CMP-007 histórico antes da tentativa de cancelamento");
        var instrumentosAntes = consultar(c.localA, base(n.id()) + "/" + familia, "ADI006 fotografia integral do original antes da recusa");
        assertThat(instrumentosAntes).hasSize(1);
        assertThat(instrumentosAntes.get(0)).isEqualTo(instrumento);
        var leituraAntes = AssercoesComposicoesHttp.capturar(() -> consultar(c.localA,
                base(n.id()) + "/composicoes?data=" + inicioPessoa,
                "CMP-006 composição disponível no início futuro"));
        enviar(c.localA, base(n.id()) + "/" + familia + "/" + instrumento.path("id").asLong() + "/cancelar",
                mapa("motivo", "Cancelamento que retiraria a capacidade da pessoa", "referenciaSei", "SEI-CANCELAMENTO-CAPACIDADE"),
                409, "CMP-006/007 cancelamento não pode deixar vínculo futuro sem composição válida");
        em.flush(); em.clear();
        assertThat(consultar(c.localA, base(n.id()) + "/eventos", "CMP-007 cancelamento recusado não produz evento")).isEqualTo(eventosAntes);
        var leituraDepois = AssercoesComposicoesHttp.capturar(() -> consultar(c.localA,
                base(n.id()) + "/composicoes?data=" + inicioPessoa,
                "CMP-007 capacidade permanece publicada após a recusa"));
        AssercoesComposicoesHttp.exigirPreservacaoComConsultaPosterior(leituraAntes, leituraDepois);
        var instrumentos = consultar(c.localA, base(n.id()) + "/" + familia, "CMP-007 instrumento original continua vigente");
        assertThat(instrumentos).hasSize(1);
        assertThat(instrumentos).as("ADI006 recusa conserva ID, versão, conteúdo e todos os itens originais").isEqualTo(instrumentosAntes);
        assertThat(instrumentos.get(0).path("situacao").asText()).isEqualTo("VIGENTE");
        var pessoaDepois = consultar(c.localA, "/api/vinculos-contratuais/" + vinculoId, "CMP-007 vínculo não é apagado ou movido");
        assertThat(pessoaDepois.path("composicaoInicialId").asLong()).isEqualTo(n.composicao2());
        assertThat(pessoaDepois.path("dataInicio").asText()).isEqualTo(inicioPessoa.toString());
        assertThat(pessoaDepois.path("dataFim").isNull()).isTrue();
        assertThat(pessoaDepois.path("estadoNegocial").asText()).isEqualTo("REGISTRADO");
    }
}
