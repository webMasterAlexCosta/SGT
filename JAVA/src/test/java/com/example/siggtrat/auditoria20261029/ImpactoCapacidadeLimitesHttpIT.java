package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.ComposicaoContratual;
import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class ImpactoCapacidadeLimitesHttpIT extends ContratosHttpBase {
    @Test
    void previaDeCapacidadeContaTodosOsOcupantesELimitaAmostraDePessoas() throws Exception {
        var c = criarCenario(); var n = publicado(c, 1000, 500, false); var efeito = RelogioSistema.hoje().plusDays(5);
        var contrato = em.find(Contrato.class, n.id()); var composicao = em.find(ComposicaoContratual.class, n.composicao1());
        int semente = 760000000 + (int) (c.pessoaA.getId() % 100000) * 1000;
        for (int i = 0; i < 206; i++) {
            var pessoa = pessoa("Pessoa volume capacidade " + i, cpf(semente + i));
            var vinculo = vinculo(pessoa, contrato); vinculo.setDataInicio(efeito.plusDays(1)); alocacao(vinculo, composicao);
        }
        em.flush(); em.clear();
        var req = aditamento("CAPACIDADE-AMOSTRA-LIMITADA", efeito); req.put("novoEfetivoTotal", 1000);
        req.put("alteracoesComposicao", List.of(mapa("composicaoId", n.composicao1(), "quantidadeResultante", 0),
                mapa("composicaoId", n.composicao2(), "quantidadeResultante", 1000)));
        var previa = previaAditamento(c, n.id(), req);
        assertThat(previa.path("possuiImpactoOcupacao").asBoolean()).isTrue();
        String alertas = previa.path("alertas").toString();
        assertThat(alertas).contains("206 alocação(ões)");
        long nomesExibidos = Pattern.compile("Pessoa volume capacidade \\d+").matcher(alertas).results().count();
        assertThat(nomesExibidos).isBetween(1L, 20L);
        assertThat(alertas).contains("amostra");
        enviar(c.localA, base(n.id()) + "/aditamentos", req, 409,
                "CMP-006 limite de detalhes não autoriza retirar capacidade de pessoas fora da amostra");
        assertThat(consultar(c.localA, base(n.id()) + "/aditamentos", "recusa não grava instrumento parcial")).isEmpty();
    }
}
