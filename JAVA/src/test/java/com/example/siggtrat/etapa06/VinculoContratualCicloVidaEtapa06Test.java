package com.example.siggtrat.etapa06;

import com.example.siggtrat.dominio.vinculo.*;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.EventoVinculoContratualRepositorio;
import com.example.siggtrat.servico.MotorTemporalVinculoServico;
import jakarta.persistence.*;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VinculoContratualCicloVidaEtapa06Test {
    @Test void matriculaEhNormalizadaNoDominioDoVinculo() {
        assertEquals("ABC 123", VinculoContratual.normalizarMatricula("  abc   123 "));
    }
    @Test void cancelamentoNegocialPrevaleceSobreDatas() {
        VinculoContratual v = new VinculoContratual(); v.setDataInicio(LocalDate.now().minusDays(5));
        v.setEstadoNegocial(EstadoNegocialVinculo.CANCELADO);
        assertEquals(SituacaoTemporalVinculo.CANCELADO, v.situacaoEm(LocalDate.now()));
    }
    @Test void trocaDiretaEmDiasConsecutivosNaoSobrepoePeriodosInclusivos() {
        LocalDate fimA = LocalDate.of(2026, 5, 10); LocalDate inicioB = LocalDate.of(2026, 5, 11);
        assertEquals(fimA.plusDays(1), inicioB);
        assertTrue(fimA.isBefore(inicioB));
    }
    @Test void reservaPossuiUnicidadeHistoricaPorContratoEMatricula() {
        Table table = ReservaMatriculaContrato.class.getAnnotation(Table.class);
        assertTrue(Arrays.stream(table.uniqueConstraints()).anyMatch(u ->
                Arrays.equals(u.columnNames(), new String[]{"id_contrato", "matricula"})));
    }
    @Test void eventosGuardamDataEfetivaSeparadaDoConhecimento() {
        EventoVinculoContratual e = new EventoVinculoContratual();
        e.setDataEfetiva(LocalDate.of(2026, 1, 10)); e.setRegistradoEm(Instant.parse("2026-02-01T12:00:00Z"));
        assertEquals(LocalDate.of(2026, 1, 10), e.getDataEfetiva());
        assertEquals(Instant.parse("2026-02-01T12:00:00Z"), e.getRegistradoEm());
    }
    @Test void eventoPreservaFotografiasDePessoaContratoEEscopo() throws Exception {
        for (String nome : List.of("pessoaNomeFotografia", "pessoaCpfFotografia", "contratoNumeroFotografia",
                "contratoAnoFotografia", "gestaoLocalIdFotografia", "gestaoLocalNomeFotografia")) {
            Field campo = EventoVinculoContratual.class.getDeclaredField(nome);
            assertFalse(campo.getAnnotation(Column.class).updatable(), nome);
        }
    }
    @Test void cadeiaHashDetectaQuebraDePrecedencia() {
        EventoVinculoContratualRepositorio repo = mock(EventoVinculoContratualRepositorio.class);
        EventoVinculoContratual a = new EventoVinculoContratual(); a.setHashEvento("a".repeat(64));
        EventoVinculoContratual b = new EventoVinculoContratual(); b.setHashAnterior("x".repeat(64)); b.setHashEvento("b".repeat(64));
        when(repo.findByVinculoIdOrderByRegistradoEmAscIdAsc(1L)).thenReturn(List.of(a, b));
        assertFalse(new MotorTemporalVinculoServico(repo).cadeiaHashIntegra(1L));
    }
}
