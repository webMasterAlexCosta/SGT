package com.example.siggtrat.auditoria20261029;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/** CT006: RED externo proposto para execução central, sem fix correspondente nesta rodada. */
class InstrumentosFotografiaCatalogoHttpIT extends ContratosHttpBase {
    @Test
    void historicoDoAditamentoPreservaNomeFotografadoDoCargoAposRenomearCatalogo() throws Exception {
        var c = criarCenario();
        var n = publicado(c, 100, null, true);
        String nomePublicado = c.cargoA.getNome();
        var req = aditamento("ADI-FOTOGRAFIA-CARGO", n.inicio().plusDays(1));
        req.put("novoEfetivoTotal", 110);
        req.put("alteracoesComposicao", List.of(mapa("composicaoId", n.composicao1(), "quantidadeResultante", 110)));
        var instrumento = registrarAditamento(c, n.id(), req);
        assertThat(instrumento.path("alteracoesComposicao").get(0).path("cargoNome").asText()).isEqualTo(nomePublicado);

        renomear(c);
        var historico = consultar(c.localA, base(n.id()) + "/aditamentos", "CAT-004/HIS-004 instrumento publicado após recarga");
        var item = historico.get(0).path("alteracoesComposicao").get(0);
        assertThat(item.path("cargoId").asLong()).isEqualTo(c.cargoA.getId());
        assertThat(item.path("cargoNome").asText()).isEqualTo(nomePublicado);
    }

    @Test
    void historicoDoRemanejamentoPreservaNomeFotografadoDoCargoAposRenomearCatalogo() throws Exception {
        var c = criarCenario();
        var n = publicado(c, 100, 50, true);
        String nomePublicado = c.cargoA.getNome();
        var req = remanejamento(n, "REM-FOTOGRAFIA-CARGO", n.inicio().plusDays(1), 5);
        previaRemanejamento(c, n.id(), req);
        var instrumento = enviar(c.localA, base(n.id()) + "/remanejamentos", req, 201,
                "REM-001 remanejamento documentado antes da alteração de catálogo");
        assertThat(itemDoCargo(instrumento.path("itens"), c.cargoA.getId()).path("cargoNome").asText()).isEqualTo(nomePublicado);

        renomear(c);
        var historico = consultar(c.localA, base(n.id()) + "/remanejamentos", "CAT-004/HIS-004 histórico do remanejamento após recarga");
        assertThat(itemDoCargo(historico.get(0).path("itens"), c.cargoA.getId()).path("cargoNome").asText())
                .isEqualTo(nomePublicado);
    }

    private void renomear(Cenario c) throws Exception {
        String novoNome = "Cargo renomeado após documento " + UUID.randomUUID();
        executar(c.localA, put("/api/cargos/" + c.cargoA.getId()).contentType(MediaType.APPLICATION_JSON)
                .content(json(mapa("nome", novoNome, "ativo", true))), 200,
                "CAT-004 nome atual pode mudar sem reescrever instrumento publicado");
        em.flush();
        em.clear();
        assertThat(consultar(c.localA, "/api/cargos/" + c.cargoA.getId(), "CAT-002 alteração atual do nome confirmada")
                .path("nome").asText()).isEqualTo(novoNome);
    }

    private JsonNode itemDoCargo(JsonNode itens, long cargoId) {
        for (var item : itens) if (item.path("cargoId").asLong() == cargoId) return item;
        throw new AssertionError("Instrumento sem item do cargo esperado " + cargoId);
    }
}
