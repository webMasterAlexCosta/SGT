package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.entidade.CorrecaoIdentificadorContrato;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Hipótese C2-CTR-02. Fonte 54af0c36b2ca034453dd0ee79da9f390e0cc92f1446947c294681449263f3aac. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RevisaoCandidato2IdentificadorRetroativoIT extends ContratosHttpBase {
    @Test
    void correcaoRetroativaRegistraIdentificadorAnteriorDoAlcanceEPreservaCorrecaoPosterior() throws Exception {
        var c = criarCenarioPersistido();
        var n = emTransacao(() -> {
            try { return publicado(c, 10, null, false); }
            catch (Exception ex) { throw new IllegalStateException(ex); }
        });
        String original = emTransacao(() -> em.find(Contrato.class, n.id()).getNumeroContrato());
        int ano = n.inicio().getYear();
        String posterior = "POSTERIOR-" + n.id();
        String retroativo = "RETROATIVO-" + n.id();
        conceder(c, n.id(), "CORRECAO_IDENTIFICADOR_CONTRATO");

        var segundaData = enviar(c.localA, base(n.id()) + "/correcoes-identificador",
                pedido(posterior, ano, n.inicio().plusDays(10)), 201,
                "correção posterior conhecida primeiro possui fonte e concessão próprias");
        assertThat(segundaData.path("numeroAnterior").asText()).isEqualTo(original);
        Instant conhecidoAntes = Instant.now();
        var primeiraData = enviar(c.localA, base(n.id()) + "/correcoes-identificador",
                pedido(retroativo, ano, n.inicio().plusDays(5)), 201,
                "nova correção é inserida antes do ato posterior, sem conflito na mesma data");

        assertThat(primeiraData.path("numeroAnterior").asText())
                .as("CON-003/TMP-004: antes do ato retroativo vale a identidade original, e não a projeção da segunda data")
                .isEqualTo(original);
        assertThat(primeiraData.path("numeroNovo").asText()).isEqualTo(retroativo);
        assertThat(consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + n.inicio().plusDays(6),
                "a nova verdade alcança apenas sua faixa anterior ao segundo fato")
                .path("versaoContratual").path("numeroContrato").asText()).isEqualTo(retroativo);
        assertThat(consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + n.inicio().plusDays(11),
                "o ato posterior válido continua prevalecendo em sua própria faixa")
                .path("versaoContratual").path("numeroContrato").asText()).isEqualTo(posterior);
        assertThat(consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + n.inicio().plusDays(6)
                        + "&conhecidoAte=" + conhecidoAntes, "o corte anterior não conhece a correção retroativa")
                .path("versaoContratual").path("numeroContrato").asText()).isEqualTo(original);
        emTransacao(() -> {
            assertThat(em.find(Contrato.class, n.id()).getNumeroContrato())
                    .as("A projeção persistida atual conserva o ato posterior válido").isEqualTo(posterior);
            var correcoes = em.createQuery("select c from CorrecaoIdentificadorContrato c where c.contrato.id=:id",
                    CorrecaoIdentificadorContrato.class).setParameter("id", n.id()).getResultList();
            assertThat(correcoes).hasSize(2);
            assertThat(em.find(CorrecaoIdentificadorContrato.class, segundaData.path("id").asLong()).getNumeroNovo())
                    .isEqualTo(posterior);
            assertThat(em.find(CorrecaoIdentificadorContrato.class, primeiraData.path("id").asLong()).getNumeroAnterior())
                    .isEqualTo(original);
            return null;
        });
    }

    private Map<String, Object> pedido(String novo, int ano, LocalDate data) {
        return mapa("novoNumero", novo, "novoAno", ano, "dataEfetiva", data,
                "dataAto", RelogioSistema.hoje(), "motivo", "Correção documental independente de identidade no alcance expresso",
                "referenciaSei", "SEI-IDENTIDADE-C2-" + novo, "autorizacaoAdministrativa", "ATO-HTTP",
                "chaveIdempotencia", UUID.randomUUID().toString());
    }
}
