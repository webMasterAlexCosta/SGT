package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.EventoVinculoContratual;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Proposta RED TMP-004/005, HIS-004/005 e REL-001: fonte futura não prova nome passado. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TransversaisRelatorioFonteFuturaRedIT extends ContratosHttpBase {
    @Test
    void fotografiaNominalDeAtoFuturoNaoPreencheRelatorioAnteriorSemFonte() throws Exception {
        Cenario c = criarCenarioPersistido();
        String rota = "/api/relatorios/frequencia-consolidada?inicio=" + DATA + "&fim=" + DATA
                + "&contratoIds=" + c.contratoA.getId();
        JsonNode antes = consultar(c.localA, rota, "Período tem cobertura, mas não tem fonte nominal");
        assertEquals(1, antes.path("registros").size());
        assertTrue(antes.path("registros").get(0).path("nome").isNull());
        assertEquals(0L, emTransacao(() -> em.createQuery(
                        "select count(e) from EventoVinculoContratual e where e.vinculo.id=:id", Long.class)
                .setParameter("id", c.vinculoA.getId()).getSingleResult()));

        // Posterior ao relatório e contemporâneo: o comando exige fonte para alteração retroativa
        // e proíbe programação futura. Hoje captura identidade somente do ato de hoje.
        var futuro = RelogioSistema.hoje();
        assertTrue(futuro.isAfter(DATA));
        assertFalse(futuro.isAfter(FIM));
        enviar(c.localA, "/api/vinculos-contratuais/" + c.vinculoA.getId() + "/alterar-matricula",
                mapa("dataEfetiva", futuro, "novaMatricula", "FUTURA-" + UUID.randomUUID().toString().substring(0, 8),
                        "justificativa", "Alteração posterior ao intervalo sem declarar nome no período passado",
                        "evidencia", "SEI-MATRICULA-FUTURA", "autorizacao", "ATO-MATRICULA"), 200,
                "Ato já efetivo é posterior ao período do relatório e conhecido agora");
        emTransacao(() -> {
            var eventos = em.createQuery("select e from EventoVinculoContratual e where e.vinculo.id=:id",
                            EventoVinculoContratual.class).setParameter("id", c.vinculoA.getId()).getResultList();
            assertFalse(eventos.isEmpty());
            assertTrue(eventos.stream().allMatch(e -> e.getDataEfetiva().isAfter(DATA)),
                    "Nenhum fato novo prova identidade na data antiga.");
            return null;
        });
        JsonNode depois = consultar(c.localA, rota, "Fonte futura conhecida não pode retroagir nome");
        assertEquals(1, depois.path("registros").size());
        assertTrue(depois.path("registros").get(0).path("nome").isNull(),
                "O relatório adotou fotografia nominal futura para um período anterior sem prova.");
        assertEquals(antes.path("totais"), depois.path("totais"));

        JsonNode absenteismo = consultar(c.localA,
                "/api/relatorios/absenteismo?mes=" + DATA.getMonthValue() + "&ano=" + DATA.getYear()
                        + "&contratoId=" + c.contratoA.getId(),
                "Absenteísmo usa o mesmo recorte nominal comprovado");
        assertEquals(1, absenteismo.size());
        assertTrue(absenteismo.get(0).path("nome").isNull());
    }
}
