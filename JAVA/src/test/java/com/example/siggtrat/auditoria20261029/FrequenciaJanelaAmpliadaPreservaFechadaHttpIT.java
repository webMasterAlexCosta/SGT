package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.frequencia.EstadoNegocialFrequencia;
import com.example.siggtrat.dominio.frequencia.TipoEventoFrequencia;
import com.example.siggtrat.entidade.Frequencia;
import com.example.siggtrat.servico.AuditoriaFrequenciaServico;
import com.example.siggtrat.servico.PoliticaTemporalFrequenciaServico;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Proposta: retenção de FECHADA ao ampliar janela. Estado anterior é fixture sintética declarada. */
@TestPropertySource(properties = "frequencia.janela-operacional-dias=30")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class FrequenciaJanelaAmpliadaPreservaFechadaHttpIT extends ContratosHttpBase {
    @Autowired PoliticaTemporalFrequenciaServico politica;
    @Autowired AuditoriaFrequenciaServico auditoria;

    @Test
    void ampliarSeteParaTrintaNaoReabreFechadaNemModificaSuaCadeia() throws Exception {
        Cenario c = criarCenarioPersistido();
        declararFonteNominalSintetica(c, "FASE36-TRANSICAO-JANELA");
        LocalDate dia = LocalDate.now(politica.zona()).minusDays(8)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.WEDNESDAY));
        assertTrue(politica.abertaParaOperacional(dia, null), "Pré-condição: data está na janela nova30");
        assertFalse(dia.plusDays(7).atStartOfDay(politica.zona()).isAfter(ZonedDateTime.now(politica.zona())),
                "Pré-condição: regra antiga7 já teria alcançado o limite");
        JsonNode criada = dados(executar(c.operacionalA, post("/api/frequencias").contentType(APPLICATION_JSON)
                .content(json(mapa("vinculoContratualId", c.vinculoA.getId(), "dataReferencia", dia, "status", "PRESENCA"))),
                200, "Fonte completa criada no backend sob configuração30 antes de declarar estado anterior"));
        long id = criada.path("id").asLong();
        emTransacao(() -> {
            Frequencia f = em.find(Frequencia.class, id);
            assertEquals(EstadoNegocialFrequencia.ABERTA, f.getEstadoNegocial());
            assertFalse(Boolean.TRUE.equals(f.getBloqueadaEdicaoOperacional()), "Isolar FECHADA de intervenção gerencial");
            String antes = auditoria.snapshot(f);
            f.setEstadoNegocial(EstadoNegocialFrequencia.FECHADA);
            f.setVersaoNegocial(f.getVersaoNegocial() + 1);
            em.flush();
            auditoria.registrar(f, TipoEventoFrequencia.FECHAMENTO_AUTOMATICO, antes,
                    "Fonte sintética declarada: estado já fechado sob janela anterior7, preservado após ampliação30",
                    null, null, null);
            em.flush(); return null;
        });
        String snapshot = emTransacao(() -> auditoria.snapshot(em.find(Frequencia.class, id)));
        long versao = emTransacao(() -> em.find(Frequencia.class, id).getVersaoNegocial()).longValue();
        List<String> eventos = hashes(id);
        assertEquals(2, eventos.size(), "Criação e fechamento declarados na fixture, sem fingir execução histórica");
        executar(c.operacionalA, post("/api/frequencias").contentType(APPLICATION_JSON)
                .content(json(mapa("vinculoContratualId", c.vinculoA.getId(), "dataReferencia", dia, "status", "FALTA"))),
                403, "Janela ampliada não reabre estado negocial persistido FECHADA");
        emTransacao(() -> {
            Frequencia f = em.find(Frequencia.class, id);
            assertEquals(EstadoNegocialFrequencia.FECHADA, f.getEstadoNegocial());
            assertEquals(versao, f.getVersaoNegocial().longValue());
            assertEquals(snapshot, auditoria.snapshot(f));
            assertFalse(Boolean.TRUE.equals(f.getBloqueadaEdicaoOperacional()), "Recusa decorre do estado, não de bloqueio gerencial");
            return null;
        });
        assertEquals(eventos, hashes(id), "403 não reescreve nem acrescenta evento");
    }

    private List<String> hashes(long id) {
        return emTransacao(() -> em.createQuery("select e.hashEvento from EventoFrequencia e where e.frequencia.id=:id order by e.id", String.class)
                .setParameter("id", id).getResultList());
    }
}
