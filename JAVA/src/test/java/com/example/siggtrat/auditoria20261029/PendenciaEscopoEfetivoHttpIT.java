package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.PendenciaAdministrativa;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** VIN-008/AUD-006: responsabilidade administrativa pertence à Gestão efetiva do fim. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PendenciaEscopoEfetivoHttpIT extends ContratosHttpBase {
    @Test
    void fimNaturalAposTransferenciaAtribuiPendenciaAoDestinoEConservaDataReal() throws Exception {
        Cenario c = criarCenarioPersistido();
        NovoContrato contrato = emTransacao(() -> {
            try { return publicado(c, 100, null, false); }
            catch (Exception e) { throw new IllegalStateException(e); }
        });
        long vinculo = enviar(c.localA, "/api/vinculos-contratuais", mapa(
                "pessoa", mapa("nome", "Pessoa fim natural transferido", "cpf", cpf(Math.toIntExact(950000000L + contrato.id()))),
                "contratoId", contrato.id(), "composicaoId", contrato.composicao1(), "dataInicio", RelogioSistema.hoje()),
                201, "Vínculo aberto antes da transferência").path("id").asLong();
        conceder(c, contrato.id(), "TRANSFERENCIA_GESTAO_LOCAL");
        enviar(c.localA, base(contrato.id()) + "/transferir-gestao", mapa(
                "dataEfetiva", RelogioSistema.hoje().plusDays(5), "dataAto", RelogioSistema.hoje(),
                "gestaoLocalDestinoId", c.gestaoB.getId(), "tipoContratacaoDestinoId", c.tipoB.getId(),
                "motivo", "Reorganização administrativa documentada", "referenciaSei", "SEI-PENDENCIA-TRANSFERENCIA",
                "autorizacaoAdministrativa", "ATO-HTTP", "mapeamentos", List.of(mapa(
                        "composicaoOrigemId", contrato.composicao1(), "lotacaoDestinoId", c.lotacaoB.getId(),
                        "jornadaDestinoId", c.jornadaB.getId(), "cargoDestinoId", c.cargoB.getId()))),
                200, "Transferência efetiva anterior ao fim natural");
        try (var relogio = Mockito.mockStatic(RelogioSistema.class, Mockito.CALLS_REAL_METHODS)) {
            relogio.when(RelogioSistema::hoje).thenReturn(contrato.fim().plusDays(1));
            var destino = dados(executar(c.localB, get("/api/pendencias").param("contratoIds", Long.toString(contrato.id())),
                    200, "Responsável no fim recebe a pendência sem depender da projeção antiga"));
            assertTrue(destino.toString().contains("FIM_NATURAL_VIGENCIA"), "Pendência deve ser visível no destino");
            var origem = dados(executar(c.localA, get("/api/pendencias").param("contratoIds", Long.toString(contrato.id())),
                    200, "Origem não recebe atribuição indevida depois da transferência"));
            assertFalse(origem.toString().contains("FIM_NATURAL_VIGENCIA"));
        }
        emTransacao(() -> {
            var p = em.createQuery("select p from PendenciaAdministrativa p where p.vinculoId=:id and p.operacaoOrigem='FIM_NATURAL_CONTRATO'",
                    PendenciaAdministrativa.class).setParameter("id", vinculo).getSingleResult();
            assertEquals(c.gestaoB.getId(), p.getGestaoLocalId());
            assertEquals(c.seB.getId(), p.getSuperintendenciaId());
            assertEquals(contrato.fim(), p.getDataEfetiva());
            assertNotNull(p.getSurgidaEm());
            return null;
        });
    }
}
