package com.example.siggtrat.auditoria20261029;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** DIAGNOSTIC ONLY. No criterion credit: the norm does not require textual equality of purposes. */
class CriteriosCFinalidadeObservacaoHttpIT extends CriteriosCHttpBase {
    @Test
    void registrarComportamentoDeFinalidadeTextualDiferenteSemInventarOraculoDeNegacao() throws Exception {
        PessoaPreparada x = pessoaPreparada(); String marca = "SONDAGEM-C-" + UUID.randomUUID();
        JsonNode o = dados(executar(x.c().localA, ocorrencia(x, x.fato(), 1, "c-sonda-" + UUID.randomUUID(),
                marca, "sondagem.txt", bytes(marca), "PESSOAL_RESTRITO"), 201, "Fonte sintética de sondagem"));
        long anexo = o.path("anexos").get(0).path("id").asLong(); String rota = "/api/ocorrencias/anexos/" + anexo;
        String declarada = "Conferência C A", usada = "Conferência C B";
        JsonNode p = enviar(x.c().admin, "/api/permissoes-excepcionais", mapa(
                "tipo", "ACESSO_DADOS_PESSOAIS_RESTRITOS", "usuarioBeneficiarioId", x.c().nacional.getId(),
                "contratoId", x.contrato().id(), "finalidade", declarada, "justificativa", "Sondagem de finalidade",
                "autorizacaoAdministrativa", "ATO-SINTETICO-C", "validoAte", Instant.now().plusSeconds(1800)),
                201, "Concessão vigente com finalidade declarada");
        String usosRota = "/api/permissoes-excepcionais/" + p.path("id").asLong() + "/usos";
        assertArrayEquals(bytes(marca), executar(x.c().nacional, get(rota).param("finalidade", declarada), 200,
                "Controle positivo com finalidade textual original").getResponse().getContentAsByteArray());
        JsonNode antes = consultar(x.c().admin, usosRota, "Usos antes da sondagem");
        var r = mvc.perform(autenticar(get(rota).param("finalidade", usada), x.c().nacional)).andReturn();
        int status = r.getResponse().getStatus();
        assertTrue(status == 200 || status == 400 || status == 403, "Sondagem não admite erro inesperado de servidor");
        JsonNode depois = consultar(x.c().admin, usosRota, "Usos após finalidade diferente");
        if (status == 200) {
            assertArrayEquals(bytes(marca), r.getResponse().getContentAsByteArray());
            assertEquals(antes.size() + 1, depois.size());
            assertTrue(java.util.stream.StreamSupport.stream(depois.spliterator(), false)
                    .anyMatch(u -> usada.equals(u.path("finalidade").asText()) && u.path("recursoId").asLong() == anexo));
        } else {
            assertFalse(Arrays.equals(bytes(marca), r.getResponse().getContentAsByteArray())); assertEquals(antes, depois);
        }
        registrar("finalidade-divergente-observacao-" + x.vinculo(), mapa("natureza", "OBSERVACAO_SEM_ACEITE_CRITERIAL",
                "concessao", p.path("id"), "finalidadeConcessao", declarada, "finalidadeUso", usada,
                "statusObservado", status, "usos", depois, "shaBytesFonte", sha(bytes(marca))));
    }
}
