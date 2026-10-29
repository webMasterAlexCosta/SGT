package com.example.siggtrat.servico;

import com.example.siggtrat.configuracao.propriedades.PropriedadesFechamentoFrequencia;
import com.example.siggtrat.dto.FrequenciaDTO;
import com.example.siggtrat.repositorio.EmpregadoRepositorio;
import com.example.siggtrat.repositorio.FechamentoFrequenciaItemRepositorio;
import com.example.siggtrat.servico.FechamentoFrequenciaProcessador;
import com.example.siggtrat.servico.FechamentoFrequenciaServico;
import com.example.siggtrat.servico.FechamentoFrequenciaEstadoServico;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FechamentoFrequenciaProcessadorTest {

    @Mock private FechamentoFrequenciaEstadoServico stateServico;
    @Mock private FechamentoFrequenciaItemRepositorio itemRepositorio;
    @Mock private EmpregadoRepositorio empregadoRepositorio;
    @Mock private com.example.siggtrat.servico.FrequenciaServico frequenciaServico;
    @Mock private FechamentoFrequenciaServico fechamentoServico;
    @Mock private InvalidacaoCacheServico cacheInvalidationServico;

    private FechamentoFrequenciaProcessador processor;

    @BeforeEach
    void setUp() {
        var props = new PropriedadesFechamentoFrequencia(500, 100_000, 1, 1, 10, 10_000, 120);
        processor = new FechamentoFrequenciaProcessador(
                stateServico, itemRepositorio, empregadoRepositorio, frequenciaServico,
                fechamentoServico, props, cacheInvalidationServico);
    }

    @Test
    void idNuloNaoIniciaProcessamento() {
        processor.processar(null);
        verifyNoInteractions(stateServico, itemRepositorio, empregadoRepositorio,
                frequenciaServico, fechamentoServico, cacheInvalidationServico);
    }

    @Test
    void fechamentoNaoReivindicadoEhIgnorado() {
        when(stateServico.reivindicar(1L)).thenReturn(Optional.empty());
        processor.processar(1L);
        verify(stateServico).reivindicar(1L);
        verifyNoInteractions(frequenciaServico);
    }

    @Test
    void fechamentoCompletoSemPendentesConcluiIdempotentemente() {
        var descritor = descritor(false, 0);
        when(stateServico.reivindicar(1L)).thenReturn(Optional.of(descritor));
        when(fechamentoServico.contarElegiveisParaProcessamento(anyLong(), any(), anyLong(), anyLong(), anyList()))
                .thenReturn(0L);

        processor.processar(1L);

        verify(stateServico).concluir(1L);
        verifyNoInteractions(frequenciaServico);
    }

    @Test
    void processaBlocosComKeysetEConclui() {
        var descritor = descritor(false, 0);
        when(stateServico.reivindicar(1L)).thenReturn(Optional.of(descritor));
        when(fechamentoServico.contarElegiveisParaProcessamento(anyLong(), any(), anyLong(), anyLong(), anyList()))
                .thenReturn(2L);
        when(stateServico.cancelamentoSolicitado(1L)).thenReturn(false);
        when(empregadoRepositorio.findIdsPendentesParaFechamentoApos(
                anyLong(), anyString(), any(), any(), any(), anyLong(), anyLong(), anyBoolean(), anyList(), anyLong(), any()))
                .thenReturn(List.of(10L, 20L), List.of());
        when(itemRepositorio.buscarPorIdFechamentoEIdsEmpregados(eq(1L), anyList())).thenReturn(List.of());

        processor.processar(1L);

        verify(stateServico).definirTotalPrevisto(1L, 2L);
        verify(frequenciaServico).processarBlocoFechamento(
                eq(1L), eq(List.of(10L, 20L)), eq(LocalDate.of(2026, 8, 6)), eq(Map.of()), eq(99L));
        verify(stateServico).concluir(1L);
    }

    @Test
    void cancelamentoInterrompeAntesDoPrimeiroBloco() {
        var descritor = descritor(false, 10);
        when(stateServico.reivindicar(1L)).thenReturn(Optional.of(descritor));
        when(stateServico.cancelamentoSolicitado(1L)).thenReturn(true);

        processor.processar(1L);

        verify(stateServico).cancelar(1L);
        verifyNoInteractions(frequenciaServico);
    }

    @Test
    void modoSomenteAlteracoesUsaItensDoFechamento() {
        var descritor = descritor(true, 1);
        when(stateServico.reivindicar(1L)).thenReturn(Optional.of(descritor));
        when(stateServico.cancelamentoSolicitado(1L)).thenReturn(false);
        when(itemRepositorio.buscarIdsEmpregadosApos(eq(1L), anyLong(), any()))
                .thenReturn(List.of(44L), List.of());
        when(itemRepositorio.buscarPorIdFechamentoEIdsEmpregados(1L, List.of(44L))).thenReturn(List.of());

        processor.processar(1L);

        verify(frequenciaServico).processarBlocoFechamento(
                eq(1L), eq(List.of(44L)), any(), anyMap(), eq(99L));
        verify(fechamentoServico, never()).contarElegiveisParaProcessamento(anyLong(), any(), anyLong(), anyLong(), anyList());
        verify(stateServico).concluir(1L);
    }

    @Test
    void falhaNoChunkEhConvertidaEmEstadoErro() {
        var descritor = descritor(false, 1);
        when(stateServico.reivindicar(1L)).thenReturn(Optional.of(descritor));
        when(stateServico.cancelamentoSolicitado(1L)).thenReturn(false);
        when(empregadoRepositorio.findIdsPendentesParaFechamentoApos(
                anyLong(), anyString(), any(), any(), any(), anyLong(), anyLong(), anyBoolean(), anyList(), anyLong(), any()))
                .thenReturn(List.of(10L));
        when(itemRepositorio.buscarPorIdFechamentoEIdsEmpregados(eq(1L), anyList())).thenReturn(List.of());
        RuntimeException erro = new RuntimeException("falha simulada");
        doThrow(erro).when(frequenciaServico).processarBlocoFechamento(anyLong(), anyList(), any(), anyMap(), anyLong());

        processor.processar(1L);

        verify(stateServico).falhar(1L, erro);
        verify(stateServico, never()).concluir(1L);
    }

    private FechamentoFrequenciaEstadoServico.DescritorProcessamento descritor(boolean somenteAlteracoes,
                                                                                long totalPrevisto) {
        return new FechamentoFrequenciaEstadoServico.DescritorProcessamento(
                1L, LocalDate.of(2026, 8, 6), 10L, 20L, List.of(), 99L,
                somenteAlteracoes, totalPrevisto, 0L);
    }
}
