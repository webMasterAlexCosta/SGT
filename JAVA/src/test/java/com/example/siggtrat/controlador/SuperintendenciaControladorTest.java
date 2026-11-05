package com.example.siggtrat.controlador;

import com.example.siggtrat.dto.SuperintendenciaDTO;
import com.example.siggtrat.servico.SuperintendenciaServico;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SuperintendenciaControladorTest {

    @Mock private SuperintendenciaServico servico;
    @InjectMocks private SuperintendenciaControlador controller;

    @Test
    void listRetornaDtosDoServico() {
        var dados = List.of(new SuperintendenciaDTO(1L, "SE/DF", "DF", true));
        when(servico.listar()).thenReturn(dados);
        assertSame(dados, controller.listar());
    }

    @Test
    void createRetorna201() {
        SuperintendenciaDTO entrada = new SuperintendenciaDTO(null, null, "DF", true);
        SuperintendenciaDTO salvo = new SuperintendenciaDTO(1L, "SE/DF", "DF", true);
        when(servico.criar(entrada)).thenReturn(salvo);

        var resposta = controller.criar(entrada);

        assertEquals(HttpStatus.CREATED, resposta.getStatusCode());
        assertSame(salvo, resposta.getBody());
    }

    @Test
    void deleteValidaRegraEstruturalAntesDeResponder204() {
        assertEquals(HttpStatus.NO_CONTENT, controller.excluir(1L).getStatusCode());
        verify(servico).validarExclusao(1L);
    }
}
