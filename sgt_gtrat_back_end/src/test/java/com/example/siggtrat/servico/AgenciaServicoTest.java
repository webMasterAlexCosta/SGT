package com.example.siggtrat.servico;

import com.example.siggtrat.dto.AgenciaDTO;
import com.example.siggtrat.entidade.Agencia;
import com.example.siggtrat.entidade.Banco;
import com.example.siggtrat.repositorio.AgenciaRepositorio;
import com.example.siggtrat.repositorio.BancoRepositorio;
import com.example.siggtrat.repositorio.ContaRepositorio;
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
class AgenciaServicoTest {

    @Mock private AgenciaRepositorio agenciaRepositorio;
    @Mock private BancoRepositorio bancoRepositorio;
    @Mock private ContaRepositorio contaRepositorio;
    @InjectMocks private AgenciaServico servico;

    @Test
    void criaAgenciaNormalizandoNumeroEVinculandoBanco() {
        Banco banco = new Banco("Banco", "001");
        banco.setId(3L);
        when(bancoRepositorio.findById(3L)).thenReturn(Optional.of(banco));
        when(agenciaRepositorio.findByBancoIdAndNumeroIgnoreCase(3L, "1234-5")).thenReturn(Optional.empty());
        when(agenciaRepositorio.save(any(Agencia.class))).thenAnswer(invocation -> {
            Agencia agencia = invocation.getArgument(0);
            agencia.setId(8L);
            return agencia;
        });

        AgenciaDTO resposta = servico.criar(new AgenciaDTO(null, " 12 34-5 ", 3L));

        assertEquals(8L, resposta.getId());
        assertEquals("1234-5", resposta.getNumero());
        assertEquals(3L, resposta.getBancoId());

        ArgumentCaptor<Agencia> captor = ArgumentCaptor.forClass(Agencia.class);
        verify(agenciaRepositorio).save(captor.capture());
        assertSame(banco, captor.getValue().getBanco());
    }

    @Test
    void rejeitaAgenciaSemBanco() {
        assertThrows(IllegalArgumentException.class,
                () -> servico.criar(new AgenciaDTO(null, "123", null)));
        verifyNoInteractions(bancoRepositorio);
        verify(agenciaRepositorio, never()).save(any());
    }

    @Test
    void rejeitaNumeroDuplicadoNoMesmoBanco() {
        Banco banco = new Banco("Banco", "001");
        banco.setId(3L);
        Agencia existente = new Agencia();
        existente.setId(99L);
        when(bancoRepositorio.findById(3L)).thenReturn(Optional.of(banco));
        when(agenciaRepositorio.findByBancoIdAndNumeroIgnoreCase(3L, "123")).thenReturn(Optional.of(existente));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> servico.criar(new AgenciaDTO(null, "123", 3L)));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(agenciaRepositorio, never()).save(any());
    }

    @Test
    void impedeExclusaoQuandoHaContaVinculada() {
        Agencia agencia = new Agencia();
        agencia.setId(4L);
        when(agenciaRepositorio.findById(4L)).thenReturn(Optional.of(agencia));
        when(contaRepositorio.existsByAgenciaId(4L)).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> servico.excluir(4L));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(agenciaRepositorio, never()).delete(any());
    }

    @Test
    void excluiAgenciaSemVinculosEExecutaFlush() {
        Agencia agencia = new Agencia();
        agencia.setId(4L);
        when(agenciaRepositorio.findById(4L)).thenReturn(Optional.of(agencia));
        when(contaRepositorio.existsByAgenciaId(4L)).thenReturn(false);

        servico.excluir(4L);

        verify(agenciaRepositorio).delete(agencia);
        verify(agenciaRepositorio).flush();
    }
}
