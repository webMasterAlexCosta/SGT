package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.ComposicaoContratual;
import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/** CON-014: confirma a revalidação da preparação, não somente a emissão de um token. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ConclusaoPublicacaoPreviaHttpIT extends ContratosHttpBase {
    @ParameterizedTest
    @ValueSource(strings = {"descricao", "empresa", "composicao"})
    void alteracaoDaPreparacaoDepoisDaPreviaExigeNovoTokenSemPublicacaoParcial(String alteracao) throws Exception {
        var c = criarCenarioPersistido();
        var inicio = RelogioSistema.hoje().minusDays(10);
        var fim = RelogioSistema.hoje().plusDays(60);
        var corpo = rascunho(c, "PREVIA-PUB-" + UUID.randomUUID().toString().substring(0, 8), 5, inicio, fim);
        long id = enviar(c.localA, "/api/contratos", corpo, 201, "CON014 preparação original retroativa").path("id").asLong();
        long composicaoId = enviar(c.localA, base(id) + "/composicoes", composicao(c, c.cargoA.getId(), 5), 201,
                "CON014 composição original completa").path("id").asLong();
        var publicar = mapa("dataPublicacao", RelogioSistema.hoje(), "referenciaSei", "SEI-PREVIA-PUBLICACAO",
                "justificativa", "Original recebido depois da data efetiva", "confirmarImpacto", true);
        var previa = enviar(c.localA, base(id) + "/previas/publicar", publicar, 200,
                "CON014 prévia assina a primeira preparação válida");
        String tokenAntigo = previa.path("tokenPrevia").asText();
        assertThat(tokenAntigo).isNotBlank();
        publicar.put("tokenPrevia", tokenAntigo);

        switch (alteracao) {
            case "descricao" -> corpo.put("descricao", "Descrição documental corrigida antes de publicar");
            case "empresa" -> corpo.put("razaoSocialEmpresa", "Razão social conferida na fonte original");
            case "composicao" -> {
                corpo.put("efetivoPrevisto", 6);
                executar(c.localA, put(base(id) + "/composicoes/" + composicaoId)
                        .contentType(MediaType.APPLICATION_JSON).content(json(composicao(c, c.cargoA.getId(), 6))), 200,
                        "CON014 quantitativo de preparação alterado após a prévia");
            }
            default -> throw new IllegalArgumentException(alteracao);
        }
        executar(c.localA, put(base(id)).contentType(MediaType.APPLICATION_JSON).content(json(corpo)), 200,
                "CON014 preparação alterada permanece completa e com soma consistente");
        var erro = enviar(c.localA, base(id) + "/publicar", publicar, 409,
                "CON014 o token anterior não autoriza publicar outra preparação");
        assertThat(erro.path("message").asText()).containsIgnoringCase("prévia");
        emTransacao(() -> {
            var contrato = em.find(Contrato.class, id);
            assertThat(contrato.getEstadoNegocial().name()).isEqualTo("RASCUNHO");
            assertThat(contrato.getPublicadoEm()).isNull();
            assertThat(contrato.getEfetivoOriginalPublicado()).isNull();
            assertThat(contrato.getDescricao()).isEqualTo(corpo.get("descricao"));
            assertThat(contrato.getRazaoSocialEmpresa()).isEqualTo(corpo.get("razaoSocialEmpresa"));
            assertThat(contrato.getEfetivoPrevisto()).isEqualTo(corpo.get("efetivoPrevisto"));
            assertThat(em.find(ComposicaoContratual.class, composicaoId).getEfetivoPrevisto())
                    .isEqualTo(corpo.get("efetivoPrevisto"));
            assertThat(contar(id, "EventoCicloVidaContrato")).isZero();
            assertThat(contar(id, "VersaoContratoNegocial")).isZero();
            return null;
        });

        var atualizada = enviar(c.localA, base(id) + "/previas/publicar", publicar, 200,
                "CON014 nova prévia descreve os dados de preparação atuais");
        assertThat(atualizada.path("tokenPrevia").asText()).isNotBlank().isNotEqualTo(tokenAntigo);
        publicar.put("tokenPrevia", atualizada.path("tokenPrevia").asText());
        Instant antesDePublicar = com.example.siggtrat.utilitario.PrecisaoTemporal.agoraMicros();
        enviar(c.localA, base(id) + "/publicar", publicar, 200,
                "CON014 confirmação atualizada publica exatamente a nova preparação");
        var versoes = consultar(c.localA, base(id) + "/versoes", "HIS006 uma fotografia original após a confirmação válida");
        assertThat(versoes).hasSize(1);
        assertThat(versoes.get(0).path("descricao").asText()).isEqualTo(corpo.get("descricao"));
        assertThat(versoes.get(0).path("razaoSocialEmpresa").asText()).isEqualTo(corpo.get("razaoSocialEmpresa"));
        assertThat(versoes.get(0).path("efetivoPrevisto").asInt()).isEqualTo(corpo.get("efetivoPrevisto"));
        var eventos = consultar(c.localA, base(id) + "/eventos", "CON014 data real e registro permanecem distintos");
        assertThat(eventos).hasSize(1);
        assertThat(eventos.get(0).path("dataEfetiva").asText()).isEqualTo(inicio.toString());
        assertThat(Instant.parse(eventos.get(0).path("registradoEm").asText())).isAfter(antesDePublicar);
        assertThat(eventos.get(0).path("usuarioResponsavelId").asLong()).isEqualTo(c.localA.getId());
    }

    private long contar(long contratoId, String entidade) {
        return em.createQuery("select count(e) from " + entidade + " e where e.contrato.id=:id", Long.class)
                .setParameter("id", contratoId).getSingleResult();
    }
}
