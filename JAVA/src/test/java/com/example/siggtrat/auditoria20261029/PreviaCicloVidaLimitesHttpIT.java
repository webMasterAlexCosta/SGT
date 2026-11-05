package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.VinculoContratual;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PreviaCicloVidaLimitesHttpIT extends ContratosHttpBase {
    @Test
    void previaLimitaDetalhesMasContaTudoEAssinaDependenciaForaDaAmostra() throws Exception {
        var c = criarCenario(); c.contratoA.setEfetivoPrevisto(300); c.composicaoA.setEfetivoPrevisto(300);
        VinculoContratual ultimo = null;
        int semente = 810000000 + (int) (c.pessoaA.getId() % 100000) * 1000;
        for (int i = 0; i < 205; i++) {
            var pessoa = pessoa("Pessoa programada " + i, cpf(semente + i));
            ultimo = vinculo(pessoa, c.contratoA); ultimo.setDataInicio(DATA.plusDays(1));
            alocacao(ultimo, c.composicaoA);
        }
        for (int i = 0; i < 450; i++) frequencia(c, false, DATA.plusDays(i), "FALTA");
        em.flush(); long ultimoId = ultimo.getId(); long contratoId = c.contratoA.getId(); em.clear();
        var req = mapa("dataEfetiva", DATA, "motivo", "Prévia de contrato com grande volume de dependências");
        var previa = enviar(c.localA, base(contratoId) + "/previas/inativar", req, 200,
                "AUD-004 prévia com mais dependências que o limite de detalhes");
        assertThat(previa.path("quantidadeVinculosAfetados").asInt()).isEqualTo(206);
        assertThat(previa.path("quantidadeFrequenciasAfetadas").asLong()).isEqualTo(450);
        assertThat(previa.path("quantidadeDatasComFrequenciasAfetadas").asLong()).isEqualTo(450);
        assertThat(previa.path("datasComFrequenciasAfetadas")).hasSize(200);
        assertThat(previa.path("datasComFrequenciasTruncadas").asBoolean()).isTrue();
        assertThat(previa.path("limiteDetalhes").asInt()).isEqualTo(200);
        assertThat(previa.path("vinculosAfetados")).hasSize(200);
        assertThat(previa.path("vinculosTruncados").asBoolean()).isTrue();
        assertThat(previa.path("quantidadeProgramacoesFuturasAfetadas").asLong()).isEqualTo(410);
        assertThat(previa.path("programacoesFuturasAfetadas")).hasSize(200);
        assertThat(previa.path("programacoesTruncadas").asBoolean()).isTrue();
        for (var v : previa.path("vinculosAfetados")) assertThat(v.path("vinculoId").asLong()).isNotEqualTo(ultimoId);
        em.find(VinculoContratual.class, ultimoId).setMatricula("FORA-AMOSTRA-NOVA"); em.flush();
        req.put("tokenPrevia", previa.path("tokenPrevia").asText()); req.put("confirmarImpacto", true);
        enviar(c.localA, base(contratoId) + "/inativar", req, 409,
                "AUD-004 token considera a última dependência, mesmo fora dos detalhes limitados");
        assertThat(consultar(c.localA, base(contratoId), "AUD-004 token recusado não muda habilitação")
                .path("habilitacaoAdministrativa").asText()).isEqualTo("ATIVO");
        var nova = enviar(c.localA, base(contratoId) + "/previas/inativar", req, 200,
                "AUD-004 nova prévia reflete dependência fora da amostra");
        assertThat(nova.path("tokenPrevia").asText()).isNotEqualTo(previa.path("tokenPrevia").asText());
        assertThat(nova.path("quantidadeVinculosAfetados").asInt()).isEqualTo(206);
    }
}
