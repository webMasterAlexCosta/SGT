package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** CON-021/025: cancelamento antes do início não deixa um intervalo operacional indevido. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ConclusaoCancelamentoFuturoHttpIT extends ContratosHttpBase {
    @Test
    void contratoAindaFuturoNaoAceitaCancelamentoComAlcanceDepoisDoInicio() throws Exception {
        var c = criarCenarioPersistido();
        var n = emTransacao(() -> { try { return publicado(c, 5, null, true); }
            catch (Exception ex) { throw new IllegalStateException(ex); } });
        long eventosAntes = eventos(n.id());
        var invalido = cancelamento(n.inicio().plusDays(2));
        confirmarPrevia(c, n.id(), invalido);
        enviar(c.localA, base(n.id()) + "/cancelar-futuro", invalido, 400,
                "CON021 estar FUTURO no registro não autoriza alcance depois do início e um intervalo de operação antes de cancelar");
        assertThat(eventos(n.id())).isEqualTo(eventosAntes);
        assertThat(emTransacao(() -> em.find(Contrato.class, n.id()).getEstadoNegocial().name())).isEqualTo("PUBLICADO");

        // Controle: um ato documentado antes do primeiro efeito continua permitido.
        var valido = cancelamento(RelogioSistema.hoje());
        confirmarPrevia(c, n.id(), valido);
        enviar(c.localA, base(n.id()) + "/cancelar-futuro", valido, 200,
                "CON021 cancelamento antes do início elimina todos os efeitos operacionais futuros sem apagar o contrato");
        for (LocalDate dia : new LocalDate[]{RelogioSistema.hoje(), n.inicio(), n.inicio().plusDays(2), n.fim()}) {
            assertThat(consultar(c.nacional, base(n.id()) + "/posicao-temporal?data=" + dia,
                    "CON021 nenhuma data do contrato cancelado permite cobertura operacional")
                    .path("situacaoTemporal").asText()).isEqualTo("CANCELADO");
        }
        assertThat(eventos(n.id())).isEqualTo(eventosAntes + 1);
    }

    private Map<String, Object> cancelamento(LocalDate data) {
        return mapa("dataEfetiva", data, "dataAto", RelogioSistema.hoje(), "motivo", "Instrumento futuro oficialmente cancelado",
                "referenciaSei", "SEI-CANCELAMENTO-ANTES-DO-INICIO");
    }

    private void confirmarPrevia(Cenario c, long id, Map<String, Object> req) throws Exception {
        var previa = enviar(c.localA, base(id) + "/previas/cancelar-futuro", req, 200, "Prévia do ato de cancelamento");
        req.put("tokenPrevia", previa.path("tokenPrevia").asText());
        req.put("confirmarImpacto", true);
    }

    private long eventos(long id) {
        return emTransacao(() -> em.createQuery("select count(e) from EventoCicloVidaContrato e where e.contrato.id=:id", Long.class)
                .setParameter("id", id).getSingleResult());
    }
}
