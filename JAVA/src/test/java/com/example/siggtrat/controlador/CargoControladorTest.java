package com.example.siggtrat.controlador;

import com.example.siggtrat.dto.CargoDTO;
import com.example.siggtrat.servico.CargoServico;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CargoControladorTest {

    @Mock private CargoServico servico;
    @InjectMocks private CargoControlador controller;

    @Test
    void updateRetorna200ComRespostaDoServico() {
        CargoDTO entrada = new CargoDTO(null, "Analista", null, null, null, null, null, null, null, null, null);
        CargoDTO salvo = new CargoDTO(7L, "Analista", null, null, true, 5L, "GTRAT", 1L, "SE/RJ", "RJ", 0L);
        when(servico.atualizar(7L, entrada)).thenReturn(salvo);

        var resposta = controller.atualizar(7L, entrada);

        assertEquals(HttpStatus.OK, resposta.getStatusCode());
        assertSame(salvo, resposta.getBody());
    }

    @Test
    void updateRetorna404QuandoRegistroNaoExiste() {
        CargoDTO entrada = new CargoDTO(null, "Analista", null, null, null, null, null, null, null, null, null);
        when(servico.atualizar(77L, entrada)).thenReturn(null);
        assertEquals(HttpStatus.NOT_FOUND, controller.atualizar(77L, entrada).getStatusCode());
    }

    @Test
    void deleteRefleteResultadoDoServico() {
        when(servico.excluir(1L)).thenReturn(true);
        when(servico.excluir(2L)).thenReturn(false);

        assertEquals(HttpStatus.NO_CONTENT, controller.excluir(1L).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, controller.excluir(2L).getStatusCode());
    }
}
