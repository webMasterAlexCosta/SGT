package com.example.siggtrat.servico;

import com.example.siggtrat.dto.TipoContaDTO;
import com.example.siggtrat.entidade.TipoConta;
import com.example.siggtrat.repositorio.TipoContaRepositorio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TipoContaServicoTest {

    @Mock
    private TipoContaRepositorio repositorio;

    @InjectMocks
    private TipoContaServico servico;

    @Test
    void criaTipoContaRemovendoEspacosExternos() {
        when(repositorio.save(any(TipoConta.class))).thenAnswer(invocation -> {
            TipoConta tipo = invocation.getArgument(0);
            tipo.setId(2L);
            return tipo;
        });

        TipoContaDTO resposta = servico.criar(new TipoContaDTO(null, "  Corrente  ", null));

        assertEquals(2L, resposta.id());
        assertEquals("Corrente", resposta.descricao());
        ArgumentCaptor<TipoConta> captor = ArgumentCaptor.forClass(TipoConta.class);
        verify(repositorio).save(captor.capture());
        assertEquals("Corrente", captor.getValue().getDescricao());
    }

    @Test
    void rejeitaDescricaoVazia() {
        assertThrows(IllegalArgumentException.class,
                () -> servico.criar(new TipoContaDTO(null, "   ", null)));
        verify(repositorio, never()).save(any());
    }

    @Test
    void updateInexistenteRetornaNull() {
        when(repositorio.findById(77L)).thenReturn(Optional.empty());
        assertNull(servico.atualizar(77L, new TipoContaDTO(null, "Poupança", null)));
    }

    @Test
    void deleteRespeitaExistenciaDoRegistro() {
        when(repositorio.existsById(1L)).thenReturn(false);
        when(repositorio.existsById(2L)).thenReturn(true);

        assertFalse(servico.excluir(1L));
        assertTrue(servico.excluir(2L));
        verify(repositorio, never()).deleteById(1L);
        verify(repositorio).deleteById(2L);
    }
}
