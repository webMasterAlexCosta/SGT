package com.example.siggtrat.servico;

import com.example.siggtrat.entidade.Cargo;
import com.example.siggtrat.entidade.ComposicaoContratual;
import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.entidade.Jornada;
import com.example.siggtrat.entidade.Lotacao;
import com.example.siggtrat.repositorio.AditamentoContratualRepositorio;
import com.example.siggtrat.repositorio.ComposicaoContratualRepositorio;
import com.example.siggtrat.repositorio.RemanejamentoContratualRepositorio;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FotografiaCatalogoComposicaoTest {

    @Test
    void renomeacaoDoCadastroNaoReescreveComposicaoPublicada() {
        ComposicaoContratualRepositorio composicoes = mock(ComposicaoContratualRepositorio.class);
        AditamentoContratualRepositorio aditamentos = mock(AditamentoContratualRepositorio.class);
        RemanejamentoContratualRepositorio remanejamentos = mock(RemanejamentoContratualRepositorio.class);
        MotorTemporalContratoServico motor = mock(MotorTemporalContratoServico.class);
        ComposicaoContratualTemporalServico servico = new ComposicaoContratualTemporalServico(
                composicoes, aditamentos, remanejamentos, motor);

        Contrato contrato = new Contrato();
        contrato.setId(1L);
        Lotacao lotacao = new Lotacao();
        lotacao.setId(2L);
        lotacao.setNome("Lotação renomeada");
        Jornada jornada = new Jornada();
        jornada.setId(3L);
        jornada.setDescricao("Turno renomeado");
        Cargo cargo = new Cargo();
        cargo.setId(4L);
        cargo.setNome("Cargo renomeado");

        ComposicaoContratual base = new ComposicaoContratual();
        base.setId(5L);
        base.setContrato(contrato);
        base.setLotacao(lotacao);
        base.setJornada(jornada);
        base.setCargo(cargo);
        base.setLotacaoNomeFotografia("Lotação publicada");
        base.setJornadaDescricaoFotografia("Turno publicado");
        base.setCargoNomeFotografia("Cargo publicado");
        base.setEfetivoPrevisto(10);
        base.setDataInicioVigencia(LocalDate.of(2026, 1, 1));

        when(composicoes.findByContratoIdOrderByLotacaoNomeAscJornadaDescricaoAscCargoNomeAsc(1L))
                .thenReturn(List.of(base));
        when(aditamentos.findByContratoIdOrderByDataEfetivaAscRegistradoEmAsc(1L)).thenReturn(List.of());
        when(remanejamentos.findByContratoIdOrderByDataEfetivaAscRegistradoEmAsc(1L)).thenReturn(List.of());
        when(motor.eventosValidosOrdenados(1L, LocalDate.of(2026, 2, 1), Instant.MAX)).thenReturn(List.of());

        var projetada = servico.projetarEntidades(
                contrato, LocalDate.of(2026, 2, 1), Instant.MAX).get(0);

        assertEquals("Lotação publicada", projetada.lotacaoNome());
        assertEquals("Turno publicado", projetada.jornadaDescricao());
        assertEquals("Cargo publicado", projetada.cargoNome());
    }
}
