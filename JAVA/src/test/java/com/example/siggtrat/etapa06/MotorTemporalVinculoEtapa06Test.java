package com.example.siggtrat.etapa06;

import com.example.siggtrat.dominio.vinculo.*;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.EventoVinculoContratualRepositorio;
import com.example.siggtrat.servico.MotorTemporalVinculoServico;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MotorTemporalVinculoEtapa06Test {
    @Test void vinculoFuturoPermaneceProgramadoAteInicioEfetivo() {
        var repositorio = mock(EventoVinculoContratualRepositorio.class);
        VinculoContratual v = vinculo(LocalDate.of(2026, 10, 1), null, LocalDate.of(2026, 12, 31));
        when(repositorio.findByVinculoIdAndRegistradoEmLessThanEqualOrderByRegistradoEmAscIdAsc(anyLong(), any())).thenReturn(List.of(criacao(v, "M1")));
        var posicao = new MotorTemporalVinculoServico(repositorio).reconstruir(v, LocalDate.of(2026, 9, 30), Instant.now());
        assertEquals(SituacaoTemporalVinculo.PROGRAMADO, posicao.situacaoTemporal());
    }

    @Test void fimDoContratoInativaOperacionalmenteVinculoAbertoNoDiaSeguinte() {
        var repositorio = mock(EventoVinculoContratualRepositorio.class);
        VinculoContratual v = vinculo(LocalDate.of(2026, 1, 1), null, LocalDate.of(2026, 6, 30));
        when(repositorio.findByVinculoIdAndRegistradoEmLessThanEqualOrderByRegistradoEmAscIdAsc(anyLong(), any())).thenReturn(List.of(criacao(v, null)));
        var motor = new MotorTemporalVinculoServico(repositorio);
        assertEquals(SituacaoTemporalVinculo.VIGENTE, motor.reconstruir(v, LocalDate.of(2026, 6, 30), Instant.now()).situacaoTemporal());
        assertEquals(SituacaoTemporalVinculo.ENCERRADO, motor.reconstruir(v, LocalDate.of(2026, 7, 1), Instant.now()).situacaoTemporal());
    }

    @Test void alteracaoDeMatriculaSoValeAPartirDaDataEfetiva() {
        var repositorio = mock(EventoVinculoContratualRepositorio.class);
        VinculoContratual v = vinculo(LocalDate.of(2026, 1, 1), null, LocalDate.of(2026, 12, 31));
        EventoVinculoContratual alteracao = evento(v, TipoEventoVinculoContratual.ALTERACAO_MATRICULA, LocalDate.of(2026, 2, 1));
        alteracao.setMatriculaAnterior("M1"); alteracao.setMatriculaNova("M2");
        when(repositorio.findByVinculoIdAndRegistradoEmLessThanEqualOrderByRegistradoEmAscIdAsc(anyLong(), any())).thenReturn(List.of(criacao(v, "M1"), alteracao));
        var motor = new MotorTemporalVinculoServico(repositorio);
        assertEquals("M1", motor.reconstruir(v, LocalDate.of(2026, 1, 31), Instant.now()).matricula());
        assertEquals("M2", motor.reconstruir(v, LocalDate.of(2026, 2, 1), Instant.now()).matricula());
    }

    @Test void corteDeConhecimentoPreservaVisaoHistoricaAnteriorARetificacao() {
        var repositorio = mock(EventoVinculoContratualRepositorio.class);
        VinculoContratual v = vinculo(LocalDate.of(2026, 1, 10), null, LocalDate.of(2026, 12, 31));
        EventoVinculoContratual criacao = criacao(v, null); criacao.setRegistradoEm(Instant.parse("2026-01-01T10:00:00Z"));
        EventoVinculoContratual retificacao = evento(v, TipoEventoVinculoContratual.RETIFICACAO_INICIO, LocalDate.of(2026, 1, 5));
        retificacao.setInicioAnterior(LocalDate.of(2026, 1, 10)); retificacao.setInicioNovo(LocalDate.of(2026, 1, 5));
        retificacao.setRegistradoEm(Instant.parse("2026-02-01T10:00:00Z"));
        List<EventoVinculoContratual> todos = List.of(criacao, retificacao);
        when(repositorio.findByVinculoIdAndRegistradoEmLessThanEqualOrderByRegistradoEmAscIdAsc(anyLong(), any()))
                .thenAnswer(i -> todos.stream().filter(e -> !e.getRegistradoEm().isAfter(i.getArgument(1))).toList());
        var motor = new MotorTemporalVinculoServico(repositorio);
        assertEquals(LocalDate.of(2026, 1, 10), motor.reconstruir(v, LocalDate.of(2026, 1, 7), Instant.parse("2026-01-15T00:00:00Z")).dataInicio());
        assertEquals(LocalDate.of(2026, 1, 5), motor.reconstruir(v, LocalDate.of(2026, 1, 7), Instant.parse("2026-02-02T00:00:00Z")).dataInicio());
    }

    private VinculoContratual vinculo(LocalDate inicio, LocalDate fim, LocalDate fimContrato) {
        Pessoa p = new Pessoa(); p.setId(1L); p.setNome("Pessoa"); p.setCpf("52998224725");
        Contrato c = new Contrato(); c.setId(2L); c.setDataInicio(LocalDate.of(2026, 1, 1)); c.setDataFim(fimContrato);
        VinculoContratual v = new VinculoContratual(); v.setId(3L); v.setPessoa(p); v.setContrato(c); v.setDataInicio(inicio); v.setDataFim(fim);
        v.setEstadoNegocial(EstadoNegocialVinculo.REGISTRADO); return v;
    }
    private EventoVinculoContratual criacao(VinculoContratual v, String matricula) {
        EventoVinculoContratual e = evento(v, TipoEventoVinculoContratual.CRIACAO, v.getDataInicio());
        e.setInicioNovo(v.getDataInicio()); e.setFimNovo(v.getDataFim()); e.setMatriculaNova(matricula); return e;
    }
    private EventoVinculoContratual evento(VinculoContratual v, TipoEventoVinculoContratual tipo, LocalDate data) {
        EventoVinculoContratual e = new EventoVinculoContratual(); e.setVinculo(v); e.setTipoEvento(tipo); e.setDataEfetiva(data);
        e.setPessoaNomeFotografia("Pessoa"); e.setPessoaCpfFotografia("52998224725"); e.setHashEvento("a".repeat(64)); return e;
    }
}
