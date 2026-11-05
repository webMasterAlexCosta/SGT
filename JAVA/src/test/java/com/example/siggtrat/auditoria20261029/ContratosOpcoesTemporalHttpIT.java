package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.utilitario.RelogioSistema;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

class ContratosOpcoesTemporalHttpIT extends ContratosHttpBase {
    @Test
    void opcoesDeContratoAcompanhamDescricaoProgramadaNaDataDoEfeito() throws Exception {
        var c = criarCenario(); var n = publicado(c, 100, null, false);
        var hoje = RelogioSistema.hoje(); var efeito = hoje.plusDays(10);
        var req = aditamento("DESCRICAO-FUTURA-OPCOES", efeito); req.put("novaDescricao", "Descrição vigente nos filtros");
        registrarAditamento(c, n.id(), req);
        for (var opcao : consultar(c.localA, "/api/contratos/opcoes", "CON-017 opções não antecipam alteração programada"))
            if (opcao.path("id").asLong() == n.id()) assertThat(opcao.path("descricao").asText()).isNotEqualTo("Descrição vigente nos filtros");
        try (var clock = Mockito.mockStatic(RelogioSistema.class, Mockito.CALLS_REAL_METHODS)) {
            clock.when(RelogioSistema::hoje).thenReturn(efeito.plusDays(1));
            var detalhe = consultar(c.localA, base(n.id()), "CON-005 descrição atual do detalhe");
            assertThat(detalhe.path("descricao").asText()).isEqualTo("Descrição vigente nos filtros");
            boolean encontrado = false;
            for (var opcao : consultar(c.localA, "/api/contratos/opcoes", "CON-005/017 opção de seleção usa a mesma verdade temporal")) {
                if (opcao.path("id").asLong() != n.id()) continue;
                encontrado = true;
                assertThat(opcao.path("descricao").asText()).isEqualTo(detalhe.path("descricao").asText());
                assertThat(opcao.path("numeroContrato").asText()).isEqualTo(detalhe.path("numeroContrato").asText());
            }
            assertThat(encontrado).isTrue();
        }
    }
}
