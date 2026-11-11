package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.entidade.GestaoLocal;
import com.example.siggtrat.entidade.VersaoContratoNegocial;
import com.example.siggtrat.servico.GestaoContratoTemporalServico;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** TMP004/005, CON023 e HIS001: responsabilidade não pode ser retrocarregada do mestre ou de ato posterior. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RevisaoGestaoSemOrigemHttpIT extends ContratosHttpBase {
    @Autowired private GestaoContratoTemporalServico gestoes;

    @Test
    void ausenciaTotalDeFotografiaNaoUsaGestaoMaterializadaComoProvaDoPassado() throws Exception {
        var c = criarCenarioPersistido();
        var n = publicar(c);
        LocalDate data = n.inicio().plusDays(1);
        assertThat(gestao(n.id(), data, null)).isEqualTo(c.gestaoA.getId());
        emTransacao(() -> {
            assertThat(em.createQuery("delete from VersaoContratoNegocial v where v.contrato.id=:c")
                    .setParameter("c", n.id()).executeUpdate()).isEqualTo(1);
            // A projeção corrente continua existindo, mas não é uma fonte datada.
            assertThat(em.find(Contrato.class, n.id()).getGestaoLocal().getId()).isEqualTo(c.gestaoA.getId());
            return null;
        });
        assertThat(gestao(n.id(), data, null))
                .as("A coluna atual sem origem preservada não comprova a responsabilidade histórica").isNull();
        assertThat(emTransacao(() -> em.createQuery("select count(v) from VersaoContratoNegocial v where v.contrato.id=:c", Long.class)
                .setParameter("c", n.id()).getSingleResult())).isZero();
    }

    @Test
    void corteDeConhecimentoAnteriorAFonteNaoPodeUsarResponsabilidadeConhecidaDepois() throws Exception {
        var c = criarCenarioPersistido();
        var n = publicar(c);
        Instant corte = Instant.now().minusSeconds(300);
        assertThat(gestao(n.id(), n.inicio(), corte))
                .as("Nenhuma fotografia ou publicação deste contrato era conhecida no corte solicitado").isNull();
        assertThat(gestao(n.id(), n.inicio(), null))
                .as("A fonte original conhecida comprova o controle positivo").isEqualTo(c.gestaoA.getId());
    }

    @Test
    void fotografiaDeTransferenciaSemOrigemNaoAntecipaDestinoMasProvaDestinoDesdeADataDoAto() throws Exception {
        var c = criarCenarioPersistido();
        var n = publicar(c);
        LocalDate transferencia = RelogioSistema.hoje().plusDays(10);
        conceder(c, n.id(), "TRANSFERENCIA_GESTAO_LOCAL");
        enviar(c.localA, base(n.id()) + "/transferir-gestao", mapa(/* Conferência SINTÉTICA explícita desta fixture; não é padrão do produto. */
                "conferenciaDocumentalConfirmada", true, "versaoConferenciaDocumental", "transferencia-gestao-conferencia-v1", 
                "dataEfetiva", transferencia, "dataAto", RelogioSistema.hoje(),
                "gestaoLocalDestinoId", c.gestaoB.getId(), "tipoContratacaoDestinoId", c.tipoB.getId(),
                "motivo", "Transferência futura documentada para testar ausência da publicação original",
                "referenciaSei", "SEI-GESTAO-SEM-ORIGEM", "autorizacaoAdministrativa", "ATO-HTTP",
                "mapeamentos", List.of(mapa("composicaoOrigemId", n.composicao1(),
                        "lotacaoDestinoId", c.lotacaoB.getId(), "jornadaDestinoId", c.jornadaB.getId(),
                        "cargoDestinoId", c.cargoB.getId()))), 200, "CON023 transferência legítima com mapa e autorização");
        assertThat(gestao(n.id(), transferencia.minusDays(1), null)).isEqualTo(c.gestaoA.getId());
        assertThat(gestao(n.id(), transferencia, null)).isEqualTo(c.gestaoB.getId());
        emTransacao(() -> {
            assertThat(em.createQuery("delete from VersaoContratoNegocial v where v.contrato.id=:c and v.numeroVersao=1")
                    .setParameter("c", n.id()).executeUpdate()).isEqualTo(1);
            assertThat(em.createQuery("select count(v) from VersaoContratoNegocial v where v.contrato.id=:c", Long.class)
                    .setParameter("c", n.id()).getSingleResult()).isEqualTo(1L);
            return null;
        });
        assertThat(gestao(n.id(), n.inicio(), null))
                .as("A origem do contrato não é substituída pela fotografia integral do destino futuro").isNull();
        assertThat(gestao(n.id(), transferencia, null))
                .as("O ato comprovado continua determinando seu próprio período").isEqualTo(c.gestaoB.getId());
        assertThat(gestao(n.id(), transferencia.plusDays(1), null)).isEqualTo(c.gestaoB.getId());
    }

    @Test
    void fotografiaLegadaPosteriorProvaSomenteSeuProprioPeriodo() throws Exception {
        var c = criarCenarioPersistido();
        var n = publicar(c);
        LocalDate dataDaFonte = RelogioSistema.hoje().plusDays(5);
        emTransacao(() -> {
            assertThat(em.createQuery("delete from VersaoContratoNegocial v where v.contrato.id=:c")
                    .setParameter("c", n.id()).executeUpdate()).isEqualTo(1);
            var foto = new VersaoContratoNegocial();
            foto.setContrato(em.find(Contrato.class, n.id()));
            foto.setNumeroVersao(2); foto.setDataEfetiva(dataDaFonte);
            foto.setGestaoLocalId(c.gestaoB.getId()); foto.setGestaoLocalNome(c.gestaoB.getNome());
            foto.setDescricao("Fonte legada datada; não é a publicação original");
            em.persist(foto);
            return null;
        });
        assertThat(gestao(n.id(), dataDaFonte.minusDays(1), null))
                .as("Primeira fonte disponível posterior não é prova de uma data anterior").isNull();
        assertThat(gestao(n.id(), dataDaFonte, null)).isEqualTo(c.gestaoB.getId());
    }

    private NovoContrato publicar(Cenario c) {
        return emTransacao(() -> {
            try { return publicado(c, 5, null, false); }
            catch (Exception e) { throw new IllegalStateException(e); }
        });
    }

    private Long gestao(long contratoId, LocalDate data, Instant corte) {
        return emTransacao(() -> {
            GestaoLocal g = gestoes.gestaoEm(em.find(Contrato.class, contratoId), data, corte);
            return g == null ? null : g.getId();
        });
    }
}
