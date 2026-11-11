package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.utilitario.RelogioSistema;
import java.time.LocalDate;
import java.util.*;

/** Todas as referências e conferências destas fixtures são SINTÉTICAS; não simulam documentos autênticos. */
abstract class TransferenciaConferencia64HttpBase extends ContratosHttpBase {
    protected static final String VERSAO = "transferencia-gestao-conferencia-v1";
    protected record Fixture(Cenario c, NovoContrato n) { }
    protected Fixture preparar64(boolean futuro) throws Exception {
        var c = criarCenarioPersistido();
        var n = emTransacao(() -> { try { return publicado(c, 5, null, futuro); }
            catch (Exception e) { throw new IllegalStateException(e); } });
        conceder(c, n.id(), "TRANSFERENCIA_GESTAO_LOCAL");
        return new Fixture(c, n);
    }
    protected Map<String, Object> intencao64(Fixture f, LocalDate data) {
        return mapa("dataEfetiva", data, "dataAto", RelogioSistema.hoje(),
                "gestaoLocalDestinoId", f.c().gestaoB.getId(), "tipoContratacaoDestinoId", f.c().tipoB.getId(),
                "motivo", "Fixture sintética64: operação explicitamente conferida por este ator",
                "referenciaSei", "SEI-SINTETICO-64", "autorizacaoAdministrativa", "ATO-HTTP",
                "mapeamentos", List.of(mapa("composicaoOrigemId", f.n().composicao1(),
                        "lotacaoDestinoId", f.c().lotacaoB.getId(), "jornadaDestinoId", f.c().jornadaB.getId(),
                        "cargoDestinoId", f.c().cargoB.getId())));
    }
    protected void declarar64(Map<String, Object> req) {
        // Somente fixture sintética explícita; nenhum helper HTTP ou padrão do produto preenche consentimento.
        req.put("conferenciaDocumentalConfirmada", true);
        req.put("versaoConferenciaDocumental", VERSAO);
    }
    protected List<List<Long>> identidades64(Fixture f) {
        return emTransacao(() -> List.of(
                em.createQuery("select e.id from EventoCicloVidaContrato e where e.contrato.id=:c order by e.id", Long.class)
                        .setParameter("c", f.n().id()).getResultList(),
                em.createQuery("select t.id from TransferenciaGestaoContrato t where t.contrato.id=:c order by t.id", Long.class)
                        .setParameter("c", f.n().id()).getResultList(),
                em.createQuery("select u.id from UsoPermissaoExcepcional u where u.recursoId=:c and u.tipoRecurso='Contrato' order by u.id", Long.class)
                        .setParameter("c", f.n().id()).getResultList()));
    }
    protected long transferencia64(Fixture f) {
        return emTransacao(() -> em.createQuery("select t.id from TransferenciaGestaoContrato t where t.contrato.id=:c", Long.class)
                .setParameter("c", f.n().id()).getSingleResult());
    }
}
