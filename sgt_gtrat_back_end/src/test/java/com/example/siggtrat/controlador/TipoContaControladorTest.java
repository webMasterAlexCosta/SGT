package com.example.siggtrat.controlador;

import com.example.siggtrat.dto.TipoContaDTO;
import com.example.siggtrat.servico.TipoContaServico;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TipoContaControladorTest {

    @Mock private TipoContaServico servico;
    @InjectMocks private TipoContaControlador controller;

    @Test
    void createDelegaAoServico() {
        TipoContaDTO entrada = new TipoContaDTO(null, "Corrente", null);
        TipoContaDTO salvo = new TipoContaDTO(1L, "Corrente", 0L);
        when(servico.criar(entrada)).thenReturn(salvo);

        assertSame(salvo, controller.criar(entrada));
        verify(servico).criar(entrada);
    }

    @Test
    void getByIdDistingueEncontradoENaoEncontrado() {
        TipoContaDTO dto = new TipoContaDTO(1L, "Corrente", 0L);
        when(servico.buscarPorId(1L)).thenReturn(dto);
        when(servico.buscarPorId(2L)).thenReturn(null);

        assertEquals(HttpStatus.OK, controller.buscarPorId(1L).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, controller.buscarPorId(2L).getStatusCode());
    }

    @Test
    void deleteRetornaStatusCoerenteComServico() {
        when(servico.excluir(1L)).thenReturn(true);
        when(servico.excluir(2L)).thenReturn(false);

        assertEquals(HttpStatus.NO_CONTENT, controller.excluir(1L).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, controller.excluir(2L).getStatusCode());
    }
}
