package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/** HIS-001/006: desconhecido não é certificado pelo estado atual nem por fato posterior. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ConclusaoFotografiaContratoHttpIT extends ContratosHttpBase {
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void habilitacaoNaoFotografadaPermaneceDesconhecidaMesmoComProjecaoAtual(boolean ativoAtual) throws Exception {
        var c = criarCenarioPersistido();
        var n = emTransacao(() -> { try { return publicado(c, 5, null, true); }
            catch (Exception ex) { throw new IllegalStateException(ex); } });
        emTransacao(() -> {
            // DML apenas na fixture sintética: simula acervo que nunca guardou este campo imutável.
            assertThat(em.createQuery("update VersaoContratoNegocial set habilitacaoAdministrativa=null where contrato.id=:id")
                    .setParameter("id", n.id()).executeUpdate()).isEqualTo(1);
            em.find(Contrato.class, n.id()).setAtivo(ativoAtual);
            return null;
        });

        var posicao = consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + n.inicio(),
                "HIS001 nenhuma fotografia ou evento prova a habilitação original ausente");
        assertThat(posicao.path("habilitacaoAdministrativa").isNull()).isTrue();
        assertThat(posicao.path("versaoContratual").path("habilitacaoAdministrativa").isNull()).isTrue();
        var atual = consultar(c.localA, base(n.id()), "HIS005 consulta atual expõe desconhecido e não libera operação");
        assertThat(atual.path("habilitacaoAdministrativa").isNull()).isTrue();
        assertThat(atual.path("ativo").asBoolean()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"inativar", "reativar"})
    void primeiroAtoComprovadoDefineHabilitacaoSomenteDesdeSuaData(String operacao) throws Exception {
        var c = criarCenarioPersistido();
        var n = emTransacao(() -> { try { return publicado(c, 5, null, true); }
            catch (Exception ex) { throw new IllegalStateException(ex); } });
        emTransacao(() -> {
            // DML apenas na fixture sintética: simula acervo que nunca guardou este campo imutável.
            assertThat(em.createQuery("update VersaoContratoNegocial set habilitacaoAdministrativa=null where contrato.id=:id")
                    .setParameter("id", n.id()).executeUpdate()).isEqualTo(1);
            em.find(Contrato.class, n.id()).setAtivo(operacao.equals("inativar"));
            return null;
        });
        var data = n.inicio().plusDays(2);
        var ato = mapa("dataEfetiva", data, "dataAto", RelogioSistema.hoje(),
                "motivo", "Primeiro ato com habilitação documental comprovada", "referenciaSei", "SEI-HABILITACAO-CONHECIDA");
        var previa = enviar(c.localA, base(n.id()) + "/previas/" + operacao, ato, 200, "Prévia do primeiro ato comprovado");
        ato.put("tokenPrevia", previa.path("tokenPrevia").asText());
        ato.put("confirmarImpacto", true);
        enviar(c.localA, base(n.id()) + "/" + operacao, ato, 200, "Novo ato estabelece habilitação sem completar passado ausente");

        assertThat(consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + data.minusDays(1),
                "HIS006 fato posterior não autoriza inventar a habilitação anterior")
                .path("habilitacaoAdministrativa").isNull()).isTrue();
        assertThat(consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + data,
                "HIS001 habilitação explicitamente conhecida desde o ato")
                .path("habilitacaoAdministrativa").asText()).isEqualTo(operacao.equals("inativar") ? "INATIVO" : "ATIVO");
    }
}
