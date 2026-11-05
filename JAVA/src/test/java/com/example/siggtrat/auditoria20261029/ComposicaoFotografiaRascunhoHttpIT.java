package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.utilitario.RelogioSistema;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/** RED proposto pela revisão independente; ainda não executado nesta etapa. */
class ComposicaoFotografiaRascunhoHttpIT extends ContratosHttpBase {
    @Test
    void editarCargoNoRascunhoPublicaFotografiaDoCargoEfetivamenteEscolhido() throws Exception {
        var c = criarCenario();
        var inicio = RelogioSistema.hoje().plusDays(10);
        var fim = inicio.plusDays(90);
        var cargoB = enviar(c.localA, "/api/cargos", mapa("nome", "Cargo B definitivo " + UUID.randomUUID(),
                "ativo", true, "gestaoLocalId", c.gestaoA.getId()), 200,
                "CAT-002 segundo cargo válido no mesmo escopo");
        long cargoBId = cargoB.path("id").asLong();
        String cargoBNome = cargoB.path("nome").asText();

        long id = enviar(c.localA, "/api/contratos", rascunho(c, "FOTO-" + UUID.randomUUID().toString().substring(0, 8),
                10, inicio, fim), 201, "CON-008 contrato em preparação").path("id").asLong();
        long composicaoId = enviar(c.localA, base(id) + "/composicoes", composicao(c, c.cargoA.getId(), 10),
                201, "CMP-001 primeira escolha de cargo ainda em rascunho").path("id").asLong();

        var alterada = dados(executar(c.localA, put(base(id) + "/composicoes/" + composicaoId)
                .contentType(MediaType.APPLICATION_JSON).content(json(composicao(c, cargoBId, 10))), 200,
                "CON-008 rascunho permite trocar a composição antes de publicar"));
        assertThat(alterada.path("cargoId").asLong()).isEqualTo(cargoBId);
        assertThat(alterada.path("cargoNome").asText()).isEqualTo(cargoBNome);

        enviar(c.localA, base(id) + "/publicar", mapa("dataPublicacao", RelogioSistema.hoje(),
                "referenciaSei", "SEI-FOTO-DEFINITIVA", "justificativa", "Publicar a composição final aprovada"), 200,
                "HIS-006 publicação deve congelar os dados finais da preparação");
        em.flush();
        em.clear();

        var fotografia = consultar(c.localA, base(id) + "/composicoes?data=" + inicio,
                "HIS-006/CAT-004 fotografia publicada após nova leitura do MySQL");
        assertThat(fotografia).hasSize(1);
        assertThat(fotografia.get(0).path("cargoId").asLong()).isEqualTo(cargoBId);
        assertThat(fotografia.get(0).path("cargoNome").asText())
                .as("A fotografia deve combinar o ID e o nome do Cargo B publicado, sem conservar o nome descartado do Cargo A")
                .isEqualTo(cargoBNome);

        executar(c.localA, put("/api/cargos/" + cargoBId).contentType(MediaType.APPLICATION_JSON)
                .content(json(mapa("nome", "Nome posterior à publicação " + UUID.randomUUID(), "ativo", true))), 200,
                "CAT-004 renomear o catálogo após publicação não altera a fotografia contratual");
        em.flush();
        em.clear();
        assertThat(consultar(c.localA, base(id) + "/composicoes?data=" + inicio,
                "HIS-004 nome publicado permanece após renomear catálogo e recarregar a base")
                .get(0).path("cargoNome").asText()).isEqualTo(cargoBNome);
    }
}
