package com.example.siggtrat.frequencia;

import com.example.siggtrat.controlador.FechamentoFrequenciaControlador;
import com.example.siggtrat.dto.RequisicaoCriacaoFechamentoFrequencia;
import com.example.siggtrat.dto.FechamentoFrequenciaDTO;
import com.example.siggtrat.servico.FechamentoFrequenciaServico;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FechamentoFrequenciaControladorTest {

    @Mock private FechamentoFrequenciaServico servico;
    private FechamentoFrequenciaControlador controller;

    @BeforeEach
    void setUp() {
        controller = new FechamentoFrequenciaControlador(servico);
    }

    @Test
    void criarRetorna202Accepted() {
        var request = new RequisicaoCriacaoFechamentoFrequencia(LocalDate.of(2026, 8, 6), 1L, 2L, List.of(), List.of());
        var dto = dto("PENDENTE");
        when(servico.criarOuReiniciar(request)).thenReturn(dto);
        var resposta = controller.criar(request);
        assertEquals(HttpStatus.ACCEPTED, resposta.getStatusCode());
        assertSame(dto, resposta.getBody());
    }

    @Test
    void buscarRetorna200() {
        var dto = dto("PROCESSANDO");
        when(servico.buscar(1L)).thenReturn(dto);
        assertEquals(HttpStatus.OK, controller.buscar(1L).getStatusCode());
    }

    @Test
    void buscarAtualSemRegistroRetorna204() {
        when(servico.buscarAtual(any(), anyLong(), anyLong(), any())).thenReturn(Optional.empty());
        var resposta = controller.buscarAtual(LocalDate.of(2026, 8, 6), 1L, 2L, List.of());
        assertEquals(HttpStatus.NO_CONTENT, resposta.getStatusCode());
    }

    @Test
    void cancelamentoEReprocessamentoRetornam202() {
        when(servico.solicitarCancelamento(1L)).thenReturn(dto("PROCESSANDO"));
        when(servico.reprocessar(2L)).thenReturn(dto("PENDENTE"));
        assertEquals(HttpStatus.ACCEPTED, controller.cancelar(1L).getStatusCode());
        assertEquals(HttpStatus.ACCEPTED, controller.reprocessar(2L).getStatusCode());
    }

    private FechamentoFrequenciaDTO dto(String status) {
        return new FechamentoFrequenciaDTO(
                1L, status, LocalDate.of(2026, 8, 6), 1L, "CTE BENFICA", 2L, "TURNO A",
                List.of(), 100, 0, 0, 0, 0, 0, false, 0, 1, false,
                "ok", null, null, null, null, null);
    }
}
