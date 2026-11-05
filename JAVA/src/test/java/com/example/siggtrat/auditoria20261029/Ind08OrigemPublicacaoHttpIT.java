package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.VersaoContratoNegocial;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** IND08: ordinal de versão não substitui vínculo documental nem apaga fonte explícita de estado. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class Ind08OrigemPublicacaoHttpIT extends ContratosHttpBase {

    @Test
    void fotoSemVinculoNaoCertificaCamposMasEventoSeparadoContinuaProvandoPublicacao() throws Exception {
        Cenario c = criarCenarioPersistido();
        NovoContrato n = emTransacao(() -> {
            try { return publicado(c, 10, null, false); }
            catch (Exception erro) { throw new IllegalStateException(erro); }
        });
        long[] ids = emTransacao(() -> {
            VersaoContratoNegocial foto = em.createQuery(
                    "select v from VersaoContratoNegocial v where v.contrato.id=:id order by v.numeroVersao", VersaoContratoNegocial.class)
                    .setParameter("id", n.id()).getResultList().get(0);
            long versao = foto.getId(), evento = foto.getEventoOrigem().getId();
            // Lacuna sintética: perde-se somente a referência da fotografia.
            // O evento publicado e todos os escalares originais permanecem.
            em.createQuery("update VersaoContratoNegocial v set v.eventoOrigem=null where v.id=:id")
                    .setParameter("id", versao).executeUpdate();
            em.clear();
            return new long[]{versao, evento};
        });
        JsonNode foto = consultar(c.localA, base(n.id()) + "/versoes",
                "Acervo conserva a linha de origem documental não vinculada").path(0);
        JsonNode posicao = dados(executar(c.localA, get(base(n.id()) + "/posicao-temporal")
                .param("data", n.inicio().toString()), 200, "Evento de publicação continua explicitamente disponível"));

        assertAll("A fonte de cada campo é independente da fonte de estado negocial",
                () -> assertThat(foto.path("id").asLong()).isEqualTo(ids[0]),
                () -> assertThat(foto.path("numeroVersao").asInt()).isEqualTo(1),
                () -> assertThat(foto.path("eventoOrigemId").isNull()).isTrue(),
                () -> assertThat(foto.path("numeroContrato").asText()).isNotBlank(),
                () -> assertThat(foto.path("dataFim").asText()).isEqualTo(n.fim().toString()),
                () -> assertThat(foto.path("matriculaVinculoObrigatoriaEfetiva").booleanValue()).isFalse(),
                () -> estado(foto, "numeroContrato", "NAO_COMPROVADO"),
                () -> estado(foto, "dataFim", "NAO_COMPROVADO"),
                () -> estado(foto, "matriculaVinculoObrigatoriaEfetiva", "NAO_COMPROVADO"),
                () -> estado(foto, "diaSemanaReduzido", "NAO_COMPROVADO"),
                () -> assertThat(posicao.path("estadoNegocial").asText()).isEqualTo("PUBLICADO"),
                () -> estado(posicao, "estadoNegocial", "VALOR_COMPROVADO"),
                () -> origemEvento(posicao, "estadoNegocial", ids[1]),
                () -> estado(posicao.path("versaoContratual"), "numeroContrato", "NAO_COMPROVADO"));
    }

    @Test
    void ordinalUmSemNenhumEventoDePublicacaoNaoComprovaEstadoNemCampos() throws Exception {
        Cenario c = criarCenarioPersistido();
        String numeroArmazenado = emTransacao(() -> em.createQuery(
                        "select v.numeroContrato from VersaoContratoNegocial v where v.contrato.id=:id order by v.numeroVersao", String.class)
                .setParameter("id", c.contratoA.getId()).getResultList().get(0));
        JsonNode foto = consultar(c.localA, base(c.contratoA.getId()) + "/versoes",
                "Linha legada continua consultável sem inventar fonte").path(0);
        JsonNode posicao = dados(executar(c.localA, get(base(c.contratoA.getId()) + "/posicao-temporal")
                .param("data", c.contratoA.getDataInicio().toString()), 200,
                "Projeção escalar legada não certifica por ordinal sua origem"));

        assertAll("O número da versão é identidade do acervo, não fonte documental",
                () -> assertThat(foto.path("numeroVersao").asInt()).isEqualTo(1),
                () -> assertThat(foto.path("eventoOrigemId").isNull()).isTrue(),
                () -> assertThat(foto.path("numeroContrato").asText()).isEqualTo(numeroArmazenado),
                () -> assertThat(foto.path("dataFim").asText()).isEqualTo(c.contratoA.getDataFim().toString()),
                () -> assertThat(posicao.path("estadoNegocial").asText()).isEqualTo("PUBLICADO"),
                () -> estado(foto, "numeroContrato", "NAO_COMPROVADO"),
                () -> estado(foto, "dataFim", "NAO_COMPROVADO"),
                () -> estado(posicao, "estadoNegocial", "NAO_COMPROVADO"),
                () -> estado(posicao.path("versaoContratual"), "numeroContrato", "NAO_COMPROVADO"));
    }

    private void estado(JsonNode objeto, String campo, String esperado) {
        JsonNode prova = objeto.path("provaCampos").path(campo);
        assertThat(prova.path("visibilidade").asText()).isEqualTo("DISPONIVEL");
        assertThat(prova.path("estado").asText()).as("Comprovação de " + campo).isEqualTo(esperado);
        if ("NAO_COMPROVADO".equals(esperado)) assertThat(prova.path("origens")).isEmpty();
        else assertThat(prova.path("origens")).isNotEmpty();
    }

    private void origemEvento(JsonNode objeto, String campo, long id) {
        boolean encontrada = false;
        for (JsonNode origem : objeto.path("provaCampos").path(campo).path("origens"))
            if (origem.path("eventoId").asLong() == id) encontrada = true;
        assertThat(encontrada).as("A fonte explícita de publicação continua disponível").isTrue();
    }
}
