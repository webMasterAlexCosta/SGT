package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.entidade.TransferenciaGestaoContrato;
import com.example.siggtrat.entidade.UsoPermissaoExcepcional;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** CON-013/023: mapa completo e perfil local não substituem a concessão administrativa específica. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TransferenciaConcessaoObrigatoriaHttpIT extends ContratosHttpBase {
    @Test
    void transferenciaSemConcessaoPreservaTodaOrigemEPassaSomenteAposAutorizacaoEspecifica() throws Exception {
        Cenario c = criarCenarioPersistido();
        NovoContrato n = emTransacao(() -> {
            try { return publicado(c, 5, null, true); }
            catch (Exception e) { throw new IllegalStateException(e); }
        });
        LocalDate data = n.inicio().plusDays(1);
        var req = mapa("dataEfetiva", data, "dataAto", RelogioSistema.hoje(),
                "gestaoLocalDestinoId", c.gestaoB.getId(), "tipoContratacaoDestinoId", c.tipoB.getId(),
                "motivo", "Transferência dependente de autorização específica documentada",
                "referenciaSei", "SEI-TRANSFERENCIA-CONCESSAO", "autorizacaoAdministrativa", "ATO-HTTP",
                "mapeamentos", List.of(mapa("composicaoOrigemId", n.composicao1(),
                        "lotacaoDestinoId", c.lotacaoB.getId(), "jornadaDestinoId", c.jornadaB.getId(),
                        "cargoDestinoId", c.cargoB.getId())));
        EstadoPersistido anterior = estado(n.id());
        enviar(c.localA, base(n.id()) + "/transferir-gestao", req, 403,
                "CON013/CON023 Gestor responsável com mapa válido ainda precisa de concessão específica");
        assertEquals(anterior, estado(n.id()), "Recusa relida em nova transação preserva identidade, origem, versões e eventos");
        emTransacao(() -> {
            assertEquals(0, em.createQuery("select count(t) from TransferenciaGestaoContrato t where t.contrato.id=:c", Long.class)
                    .setParameter("c", n.id()).getSingleResult());
            assertEquals(0, em.createQuery("select count(u) from UsoPermissaoExcepcional u where u.recursoId=:c and u.tipoRecurso='Contrato'", Long.class)
                    .setParameter("c", n.id()).getSingleResult());
            return null;
        });
        conceder(c, n.id(), "TRANSFERENCIA_GESTAO_LOCAL");
        var semMapa = new java.util.LinkedHashMap<>(req);
        semMapa.remove("mapeamentos");
        var negativaMapa = enviar(c.localA, base(n.id()) + "/transferir-gestao", semMapa, 400,
                "CON023 concessão e Tipo de destino válidos não substituem mapeamento obrigatório");
        assertTrue(negativaMapa.path("message").asText().contains("mapeamento explícito"));
        assertEquals(anterior, estado(n.id()), "Mapa ausente não modifica contrato, versões ou eventos");
        emTransacao(() -> {
            assertEquals(0, em.createQuery("select count(t) from TransferenciaGestaoContrato t where t.contrato.id=:c", Long.class)
                    .setParameter("c", n.id()).getSingleResult());
            assertEquals(0, em.createQuery("select count(u) from UsoPermissaoExcepcional u where u.recursoId=:c and u.tipoRecurso='Contrato'", Long.class)
                    .setParameter("c", n.id()).getSingleResult());
            return null;
        });
        enviar(c.localA, base(n.id()) + "/transferir-gestao", req, 200,
                "A mesma intenção pode ser confirmada depois da concessão administrativa específica");
        emTransacao(() -> {
            TransferenciaGestaoContrato transferencia = em.createQuery(
                    "select t from TransferenciaGestaoContrato t where t.contrato.id=:c", TransferenciaGestaoContrato.class)
                    .setParameter("c", n.id()).getSingleResult();
            assertEquals(c.gestaoA.getId(), transferencia.getGestaoOrigem().getId());
            assertEquals(c.gestaoB.getId(), transferencia.getGestaoDestino().getId());
            assertEquals(c.localA.getId(), transferencia.getResponsavelId());
            assertEquals(data, transferencia.getDataEfetiva());
            assertEquals("ATO-HTTP", transferencia.getAutorizacaoAdministrativa());
            assertEquals(1, transferencia.getMapeamentos().size());
            UsoPermissaoExcepcional uso = em.createQuery(
                    "select u from UsoPermissaoExcepcional u where u.recursoId=:c and u.tipoRecurso='Contrato'", UsoPermissaoExcepcional.class)
                    .setParameter("c", n.id()).getSingleResult();
            assertEquals(c.localA.getId(), uso.getUsuarioId());
            assertEquals(req.get("motivo"), uso.getFinalidade());
            assertNotNull(uso.getUsadoEm());
            Contrato contrato = em.find(Contrato.class, n.id());
            assertEquals(anterior.numero(), contrato.getNumeroContrato());
            assertEquals(anterior.ano(), contrato.getAnoContrato());
            assertEquals(anterior.gestao(), contrato.getGestaoLocal().getId(), "A transferência futura não antecipa a projeção corrente");
            return null;
        });
    }

    private EstadoPersistido estado(long id) {
        return emTransacao(() -> {
            Contrato c = em.find(Contrato.class, id);
            return new EstadoPersistido(c.getNumeroContrato(), c.getAnoContrato(), c.getGestaoLocal().getId(),
                    em.createQuery("select e.id from EventoCicloVidaContrato e where e.contrato.id=:c order by e.id", Long.class)
                            .setParameter("c", id).getResultList(),
                    em.createQuery("select v.id from VersaoContratoNegocial v where v.contrato.id=:c order by v.id", Long.class)
                            .setParameter("c", id).getResultList());
        });
    }

    private record EstadoPersistido(String numero, Integer ano, Long gestao, List<Long> eventos, List<Long> versoes) {}
}
