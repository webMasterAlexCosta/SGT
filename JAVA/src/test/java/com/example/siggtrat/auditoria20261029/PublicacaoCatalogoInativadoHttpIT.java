package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.utilitario.RelogioSistema;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/** RED proposto pela revisão independente; ainda não executado nesta etapa. */
class PublicacaoCatalogoInativadoHttpIT extends ContratosHttpBase {
    @Test
    void publicacaoRevalidaCargoInativadoDepoisDaCriacaoDaComposicaoEmRascunho() throws Exception {
        var c = criarCenario();
        var inicio = RelogioSistema.hoje().plusDays(10);
        var cargo = enviar(c.localA, "/api/cargos", mapa("nome", "Cargo preparação " + UUID.randomUUID(),
                "ativo", true, "gestaoLocalId", c.gestaoA.getId()), 200, "CAT-002 cargo exclusivo da preparação");
        long cargoId = cargo.path("id").asLong();
        long id = enviar(c.localA, "/api/contratos", rascunho(c,
                "CAT-PUB-" + UUID.randomUUID().toString().substring(0, 8), 10, inicio, inicio.plusDays(90)), 201,
                "CON-008 rascunho completo e futuro").path("id").asLong();
        enviar(c.localA, base(id) + "/composicoes", composicao(c, cargoId, 10), 201,
                "CMP-001 catálogo estava ativo quando a composição foi preparada");
        executar(c.localA, delete("/api/cargos/" + cargoId), 204,
                "CAT-003 inativação antes de existir fato publicado mantém o cadastro histórico");
        em.flush();
        em.clear();
        assertThat(consultar(c.localA, "/api/cargos/" + cargoId, "CAT-008 cargo está inativo antes da publicação")
                .path("ativo").asBoolean()).isFalse();

        enviar(c.localA, base(id) + "/publicar", mapa("dataPublicacao", RelogioSistema.hoje(),
                "referenciaSei", "SEI-PUBLICACAO-CATALOGO", "justificativa", "Publicação exige composição ainda válida"),
                400, "CON-008/CAT-008 publicação não pode confirmar composição futura com catálogo atualmente inativo");
        em.flush();
        em.clear();

        assertThat(consultar(c.localA, base(id), "CON-008 publicação recusada conserva rascunho")
                .path("estadoNegocial").asText()).isEqualTo("RASCUNHO");
        assertThat(consultar(c.localA, base(id) + "/eventos", "GOV-004 nenhuma publicação parcial foi registrada")).isEmpty();
        assertThat(consultar(c.localA, base(id) + "/versoes", "HIS-006 não há fotografia publicada de catálogo inválido")).isEmpty();

        executar(c.localA, put("/api/cargos/" + cargoId).contentType(MediaType.APPLICATION_JSON)
                .content(json(mapa("nome", cargo.path("nome").asText(), "ativo", true))), 200,
                "CAT-008 reativação explícita torna o cargo novamente disponível");
        enviar(c.localA, base(id) + "/publicar", mapa("dataPublicacao", RelogioSistema.hoje(),
                "referenciaSei", "SEI-PUBLICACAO-CATALOGO", "justificativa", "Publicação após sanar indisponibilidade"), 200,
                "CON-008 publicação pode prosseguir após corrigir a preparação");
        assertThat(consultar(c.localA, base(id), "CON-008 contrato confirmado com catálogo revalidado")
                .path("estadoNegocial").asText()).isEqualTo("PUBLICADO");
    }
}
