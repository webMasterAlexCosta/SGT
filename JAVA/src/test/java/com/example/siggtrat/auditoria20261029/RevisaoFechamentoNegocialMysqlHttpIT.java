package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.frequencia.EstadoNegocialFrequencia;
import com.example.siggtrat.dominio.frequencia.TipoEventoFrequencia;
import com.example.siggtrat.entidade.EventoFrequencia;
import com.example.siggtrat.entidade.Frequencia;
import com.example.siggtrat.servico.AuditoriaFrequenciaServico;
import com.example.siggtrat.servico.FechamentoAutomaticoFrequenciaNegocialServico;
import com.example.siggtrat.servico.PoliticaTemporalFrequenciaServico;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** FRE-013: o serviço real confirma estado, versão e cadeia no PostgreSQL em transações próprias. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RevisaoFechamentoNegocialMysqlHttpIT extends ContratosHttpBase {
    @Autowired FechamentoAutomaticoFrequenciaNegocialServico fechamento;
    @Autowired PoliticaTemporalFrequenciaServico politica;
    @Autowired AuditoriaFrequenciaServico auditoria;

    @Test
    void oitavoDiaPersisteFechamentoComEventoEncadeadoSemAlterarFonteNemDuplicarNoReprocessamento() throws Exception {
        Cenario c = criarCenarioPersistido();
        LocalDate hoje = RelogioSistema.hoje(), diaSete = hoje.minusDays(6), amanha = hoje.plusDays(1);
        long id = enviar(c.operacionalA, "/api/frequencias", mapa(
                "vinculoContratualId", c.vinculoA.getId(), "dataReferencia", diaSete, "status", statusCompativelComJornadaDoCenario(diaSete)),
                200, "Frequência real ainda aberta no sétimo dia").path("id").asLong();
        long controleId = enviar(c.operacionalA, "/api/frequencias", mapa(
                "vinculoContratualId", c.vinculoA.getId(), "dataReferencia", hoje, "status", statusCompativelComJornadaDoCenario(hoje)),
                200, "Controle dentro da janela continua aberto depois da próxima virada").path("id").asLong();
        Foto original = foto(id);
        assertEquals(EstadoNegocialFrequencia.ABERTA, original.frequencia().getEstadoNegocial());
        assertEquals(1, original.frequencia().getVersaoNegocial());
        assertEquals(c.operacionalA.getId(), original.frequencia().getAutorOriginalId());
        assertEquals(1, original.eventos().size());
        assertEquals(TipoEventoFrequencia.REGISTRO, original.eventos().get(0).getTipoEvento());
        EventoFrequencia registro = original.eventos().get(0);
        ZonedDateTime virada = amanha.atStartOfDay(politica.zona());
        var zona = politica.zona();

        // Somente o relógio é controlado. Serviço, gerenciador de transação,
        // repositórios, bloqueios, commit e leitura posterior usam PostgreSQL real.
        try (var datas = Mockito.mockStatic(LocalDate.class, Mockito.CALLS_REAL_METHODS);
             var instantes = Mockito.mockStatic(ZonedDateTime.class, Mockito.CALLS_REAL_METHODS)) {
            datas.when(() -> LocalDate.now(zona)).thenReturn(amanha);
            instantes.when(() -> ZonedDateTime.now(zona)).thenReturn(virada);
            assertTrue(politica.fechada(diaSete, null));
            assertFalse(politica.fechada(hoje, null));
            assertTrue(fechamento.fecharVencidas() >= 1,
                    "A execução real precisa confirmar ao menos a frequência deste cenário.");

            Foto fechada = foto(id);
            assertEquals(EstadoNegocialFrequencia.FECHADA, fechada.frequencia().getEstadoNegocial());
            assertEquals(2, fechada.frequencia().getVersaoNegocial());
            assertEquals(original.frequencia().getStatus(), fechada.frequencia().getStatus());
            assertEquals(original.frequencia().getDataReferencia(), fechada.frequencia().getDataReferencia());
            assertEquals(original.frequencia().getAutorOriginalId(), fechada.frequencia().getAutorOriginalId());
            assertEquals(original.frequencia().getUltimaRetificacaoPorId(), fechada.frequencia().getUltimaRetificacaoPorId());
            assertEquals(2, fechada.eventos().size());
            EventoFrequencia preservado = fechada.eventos().get(0), evento = fechada.eventos().get(1);
            assertEquals(registro.getId(), preservado.getId());
            assertEquals(registro.getHashEvento(), preservado.getHashEvento());
            assertEquals(registro.getSnapshotNovo(), preservado.getSnapshotNovo());
            assertEquals(registro.getRegistradoEm(), preservado.getRegistradoEm());
            assertEquals(TipoEventoFrequencia.FECHAMENTO_AUTOMATICO, evento.getTipoEvento());
            assertEquals(2, evento.getNumeroVersao());
            assertEquals(diaSete, evento.getDataReferencia());
            assertEquals(registro.getHashEvento(), evento.getHashAnterior());
            assertTrue(evento.getSnapshotAnterior().contains("estado=ABERTA"));
            assertTrue(evento.getSnapshotNovo().contains("estado=FECHADA"));
            assertTrue(evento.getMotivo().contains("8º dia"));
            assertEquals("SISTEMA", evento.getUsuarioUsername());
            assertNull(evento.getUsuarioId());
            assertNotNull(evento.getRegistradoEm());
            assertEquals(64, evento.getHashEvento().length());
            assertTrue(auditoria.evidenciasIntegridade(id).cadeiaIntegra(),
                    "A cadeia é recalculada pelo serviço real a partir dos eventos confirmados.");
            Foto controle = foto(controleId);
            assertEquals(EstadoNegocialFrequencia.ABERTA, controle.frequencia().getEstadoNegocial());
            assertEquals(1, controle.frequencia().getVersaoNegocial());
            assertEquals(1, controle.eventos().size());

            assertEquals(0, fechamento.fecharVencidas(), "Reprocessar a mesma virada não publica outro fechamento.");
            Foto repetida = foto(id);
            assertEquals(2, repetida.frequencia().getVersaoNegocial());
            assertEquals(fechada.eventos().stream().map(EventoFrequencia::getId).toList(),
                    repetida.eventos().stream().map(EventoFrequencia::getId).toList());
            assertEquals(fechada.eventos().stream().map(EventoFrequencia::getHashEvento).toList(),
                    repetida.eventos().stream().map(EventoFrequencia::getHashEvento).toList());
        }
    }

    // A Jornada sintética da base trabalha de segunda a sexta. O oráculo é
    // fechamento/preservação; não pede FALTA em um dia que a própria fonte
    // documentou como descanso. Ambas as datas mantêm exatamente sua idade.
    private String statusCompativelComJornadaDoCenario(LocalDate data) {
        return data.getDayOfWeek().getValue() >= 6 ? "DESCANSO" : "FALTA";
    }

    private Foto foto(long id) {
        return emTransacao(() -> {
            em.clear();
            Frequencia frequencia = em.find(Frequencia.class, id);
            List<EventoFrequencia> eventos = em.createQuery(
                    "select e from EventoFrequencia e where e.frequencia.id=:id order by e.numeroVersao,e.id",
                    EventoFrequencia.class).setParameter("id", id).getResultList();
            return new Foto(frequencia, eventos);
        });
    }

    private record Foto(Frequencia frequencia, List<EventoFrequencia> eventos) { }
}
