package com.example.siggtrat.servico;

import com.example.siggtrat.dto.EmpregadoReferenciaDTO;
import com.example.siggtrat.dto.TelefoneDTO;
import com.example.siggtrat.entidade.Empregado;
import com.example.siggtrat.entidade.Telefone;
import com.example.siggtrat.repositorio.EmpregadoRepositorio;
import com.example.siggtrat.repositorio.TelefoneRepositorio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TelefoneServicoTest {

    @Mock private TelefoneRepositorio repositorio;
    @Mock private EmpregadoRepositorio empregadoRepositorio;
    @InjectMocks private TelefoneServico servico;

    @Test
    void criaTelefoneVinculadoAEmpregadoExistente() {
        Empregado empregado = new Empregado();
        empregado.setId(15L);
        when(empregadoRepositorio.findById(15L)).thenReturn(Optional.of(empregado));
        when(repositorio.save(any(Telefone.class))).thenAnswer(invocation -> {
            Telefone telefone = invocation.getArgument(0);
            telefone.setId(3L);
            return telefone;
        });

        TelefoneDTO resposta = servico.criar(new TelefoneDTO(null, " 61999990000 ", new EmpregadoReferenciaDTO(15L), 0));

        assertEquals(3L, resposta.id());
        assertEquals("61999990000", resposta.numero());
        ArgumentCaptor<Telefone> captor = ArgumentCaptor.forClass(Telefone.class);
        verify(repositorio).save(captor.capture());
        assertSame(empregado, captor.getValue().getEmpregado());
    }

    @Test
    void rejeitaTelefoneSemEmpregadoNaCriacao() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> servico.criar(new TelefoneDTO(null, "61999990000", null, 0)));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(repositorio, never()).save(any());
    }

    @Test
    void rejeitaEmpregadoInexistente() {
        when(empregadoRepositorio.findById(99L)).thenReturn(Optional.empty());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> servico.criar(new TelefoneDTO(null, "61999990000", new EmpregadoReferenciaDTO(99L), 0)));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void atualizarTelefoneInexistenteRetornaNull() {
        when(repositorio.findById(50L)).thenReturn(Optional.empty());
        assertNull(servico.atualizar(50L, new TelefoneDTO(null, "61999990000", null, 0)));
        verify(repositorio, never()).save(any());
    }

    @Test
    void deleteRetornaIndicadorDeExistencia() {
        when(repositorio.existsById(1L)).thenReturn(false);
        when(repositorio.existsById(2L)).thenReturn(true);

        assertFalse(servico.excluir(1L));
        assertTrue(servico.excluir(2L));
        verify(repositorio).deleteById(2L);
    }
}
