package com.example.siggtrat.servico;

import com.example.siggtrat.dto.VersaoContratoNegocialDTO;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.GestaoLocalRepositorio;
import com.example.siggtrat.repositorio.TransferenciaGestaoContratoRepositorio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GestaoContratoTemporalLegadoTest {
    private static final LocalDate JULHO = LocalDate.of(2026, 7, 1);
    private static final Instant CONHECIDO = Instant.parse("2026-10-01T00:00:00Z");
    private final MotorTemporalContratoServico motor = mock(MotorTemporalContratoServico.class);
    private final TransferenciaGestaoContratoRepositorio transferencias = mock(TransferenciaGestaoContratoRepositorio.class);
    private final GestaoLocalRepositorio gestoes = mock(GestaoLocalRepositorio.class);
    private final GestaoContratoTemporalServico servico = new GestaoContratoTemporalServico(motor, transferencias, gestoes);

    @ParameterizedTest
    @CsvSource({"2026-06-30,false", "2026-07-01,true"})
    void fotografiasSemEventoMantemGestaoDaDataEmVezDaGestaoMaterializada(String data, boolean destino) {
        Contexto c = contexto();
        when(motor.listarVersoes(1L)).thenReturn(List.of(
                foto(1, null, LocalDate.of(2026, 1, 1), c.origem().getId(), CONHECIDO.minusSeconds(30)),
                foto(2, null, JULHO, c.destino().getId(), CONHECIDO.minusSeconds(20))));
        assertSame(destino ? c.destino() : c.origem(), servico.gestaoEm(c.contrato(), LocalDate.parse(data), CONHECIDO));
    }

    @Test
    void transferenciaCanonicaSobreBaseLegadaRespeitaEfeitoEConhecimentoSemRegravarFotografias() {
        Contexto c = contexto();
        Instant registrada = CONHECIDO.minusSeconds(10);
        EventoCicloVidaContrato evento = mock(EventoCicloVidaContrato.class);
        when(evento.getId()).thenReturn(50L);
        when(evento.getTipoEvento()).thenReturn(TipoEventoCicloVidaContrato.TRANSFERENCIA_GESTAO_LOCAL);
        TransferenciaGestaoContrato transferencia = mock(TransferenciaGestaoContrato.class);
        when(transferencia.getEvento()).thenReturn(evento);
        when(transferencia.getGestaoDestino()).thenReturn(c.destino());
        when(transferencias.findByContratoIdOrderByDataEfetivaAscRegistradoEmAsc(1L))
                .thenReturn(List.of(transferencia));
        when(motor.listarVersoes(1L)).thenReturn(List.of(
                foto(1, null, LocalDate.of(2026, 1, 1), c.origem().getId(), CONHECIDO.minusSeconds(30)),
                foto(2, 50L, JULHO, c.destino().getId(), registrada)));
        when(motor.eventosValidosOrdenados(eq(1L), any(LocalDate.class), any(Instant.class))).thenAnswer(inv -> {
            LocalDate data = inv.getArgument(1);
            Instant conhecimento = inv.getArgument(2);
            return !data.isBefore(JULHO) && !conhecimento.isBefore(registrada) ? List.of(evento) : List.of();
        });
        assertSame(c.origem(), servico.gestaoEm(c.contrato(), JULHO.minusDays(1), CONHECIDO));
        assertSame(c.destino(), servico.gestaoEm(c.contrato(), JULHO, CONHECIDO));
        assertSame(c.origem(), servico.gestaoEm(c.contrato(), JULHO, registrada.minusSeconds(1)));
    }

    @Test
    void intervaloPreservaAsDuasResponsabilidadesESomenteAsDatasConhecidas() {
        Contexto c = contexto();
        Instant registroDestino = CONHECIDO.minusSeconds(20);
        when(motor.listarVersoes(1L)).thenReturn(List.of(
                foto(1, null, LocalDate.of(2026, 1, 1), c.origem().getId(), CONHECIDO.minusSeconds(30)),
                foto(2, null, JULHO, c.destino().getId(), registroDestino)));
        assertEquals(List.of(c.origem(), c.destino()), servico.gestoesNoIntervalo(c.contrato(),
                JULHO.minusDays(1), JULHO.plusDays(1), CONHECIDO));
        assertEquals(List.of(c.destino()), servico.gestoesNoIntervalo(c.contrato(),
                JULHO, JULHO.plusDays(1), CONHECIDO));
        assertEquals(List.of(c.origem()), servico.gestoesNoIntervalo(c.contrato(),
                JULHO.minusDays(1), JULHO.plusDays(1), registroDestino.minusSeconds(1)));
    }

    private Contexto contexto() {
        // O seletor é uma função pura do motor; o double continua isolando apenas
        // as fontes/eventos, sem substituir a nova seleção por uma coleção vazia.
        when(motor.fotografiasConhecidasPorEvento(anyList(), any(Instant.class))).thenCallRealMethod();
        GestaoLocal origem = new GestaoLocal(), destino = new GestaoLocal();
        origem.setId(10L); destino.setId(20L);
        Contrato contrato = mock(Contrato.class);
        when(contrato.getId()).thenReturn(1L);
        when(contrato.getEstadoNegocial()).thenReturn(EstadoNegocialContrato.PUBLICADO);
        when(contrato.getGestaoLocal()).thenReturn(destino);
        when(gestoes.findById(10L)).thenReturn(Optional.of(origem));
        when(gestoes.findById(20L)).thenReturn(Optional.of(destino));
        when(transferencias.findByContratoIdOrderByDataEfetivaAscRegistradoEmAsc(1L)).thenReturn(List.of());
        when(motor.eventosValidosOrdenados(eq(1L), any(LocalDate.class), any(Instant.class))).thenReturn(List.of());
        return new Contexto(contrato, origem, destino);
    }

    private VersaoContratoNegocialDTO foto(int versao, Long evento, LocalDate data, Long gestao, Instant registro) {
        return new VersaoContratoNegocialDTO((long) versao, versao, evento, data, registro,
                "HISTORICO", 2026, "Fotografia preservada", null, null, null, null, null, null,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), 10, gestao, "Gestão " + gestao);
    }

    private record Contexto(Contrato contrato, GestaoLocal origem, GestaoLocal destino) {}
}
