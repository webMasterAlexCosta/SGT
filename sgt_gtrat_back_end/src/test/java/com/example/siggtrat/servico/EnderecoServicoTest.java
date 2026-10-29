package com.example.siggtrat.servico;

import com.example.siggtrat.dto.EnderecoDTO;
import com.example.siggtrat.entidade.Empregado;
import com.example.siggtrat.entidade.Endereco;
import com.example.siggtrat.repositorio.EmpregadoRepositorio;
import com.example.siggtrat.repositorio.EnderecoRepositorio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class EnderecoServicoTest {

    @Mock private EnderecoRepositorio repositorio;
    @Mock private EmpregadoRepositorio empregadoRepositorio;
    @InjectMocks private EnderecoServico servico;

    @Test
    void rejeitaEnderecoSemLogradouro() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> servico.criar(new EnderecoDTO(null, " ", null, null, null, null, null, 0)));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(repositorio, never()).save(any());
    }

    @Test
    void criaEnderecoValido() {
        when(repositorio.save(any(Endereco.class))).thenAnswer(invocation -> {
            Endereco e = invocation.getArgument(0);
            e.setId(10L);
            return e;
        });

        EnderecoDTO resposta = servico.criar(new EnderecoDTO(null, "Rua A", "10", "Centro", "Brasília", "DF", "70000-000", 0));

        assertEquals(10L, resposta.id());
        assertEquals("Rua A", resposta.logradouro());
        assertEquals("DF", resposta.uf());
    }

    @Test
    void atualizarEnderecoInexistenteRetorna404() {
        when(repositorio.findById(50L)).thenReturn(Optional.empty());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> servico.atualizar(50L, new EnderecoDTO(null, "Rua", null, null, null, null, null, 0)));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void impedeExcluirEnderecoVinculadoAEmpregado() {
        Endereco endereco = new Endereco();
        endereco.setId(5L);
        when(repositorio.findById(5L)).thenReturn(Optional.of(endereco));
        when(empregadoRepositorio.findByEndereco_Id(5L)).thenReturn(Optional.of(new Empregado()));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> servico.excluir(5L));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(repositorio, never()).delete(any());
    }

    @Test
    void excluiEnderecoSemVinculoEExecutaFlush() {
        Endereco endereco = new Endereco();
        endereco.setId(5L);
        when(repositorio.findById(5L)).thenReturn(Optional.of(endereco));
        when(empregadoRepositorio.findByEndereco_Id(5L)).thenReturn(Optional.empty());

        servico.excluir(5L);

        verify(repositorio).delete(endereco);
        verify(repositorio).flush();
    }
}
