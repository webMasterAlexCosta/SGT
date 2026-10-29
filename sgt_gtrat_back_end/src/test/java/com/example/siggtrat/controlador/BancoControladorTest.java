package com.example.siggtrat.controlador;

import com.example.siggtrat.dto.BancoDTO;
import com.example.siggtrat.servico.BancoServico;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BancoControladorTest {

    @Mock private BancoServico servico;
    @InjectMocks private BancoControlador controller;

    @Test
    void getByIdRetorna200QuandoBancoExiste() {
        BancoDTO dto = new BancoDTO(1L, "Banco", "001");
        when(servico.buscarPorId(1L)).thenReturn(dto);

        var resposta = controller.buscarPorId(1L);

        assertEquals(HttpStatus.OK, resposta.getStatusCode());
        assertSame(dto, resposta.getBody());
    }

    @Test
    void getByIdRetorna404QuandoBancoNaoExiste() {
        when(servico.buscarPorId(404L)).thenReturn(null);
        assertEquals(HttpStatus.NOT_FOUND, controller.buscarPorId(404L).getStatusCode());
    }

    @Test
    void deleteRetorna204EDelegaAoServico() {
        assertEquals(HttpStatus.NO_CONTENT, controller.excluir(3L).getStatusCode());
        verify(servico).excluir(3L);
    }
}
