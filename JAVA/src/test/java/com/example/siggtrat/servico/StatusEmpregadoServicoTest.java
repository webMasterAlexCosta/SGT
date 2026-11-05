package com.example.siggtrat.servico;

import com.example.siggtrat.constante.ConstantesDominio;
import com.example.siggtrat.dto.StatusEmpregadoDTO;
import com.example.siggtrat.entidade.StatusEmpregado;
import com.example.siggtrat.repositorio.EmpregadoRepositorio;
import com.example.siggtrat.repositorio.StatusEmpregadoRepositorio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
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
class StatusEmpregadoServicoTest {

    @Mock private StatusEmpregadoRepositorio repositorio;
    @Mock private EmpregadoRepositorio empregadoRepositorio;
    @Mock private AcessoOrganizacionalServico acesso;
    @InjectMocks private StatusEmpregadoServico servico;

    @BeforeEach
    void autorizarAdministradorDoCenario() {
        var administrador = new com.example.siggtrat.entidade.UsuarioAplicacao();
        when(acesso.usuarioAtual()).thenReturn(administrador);
        when(acesso.ehAdministrador(administrador)).thenReturn(true);
    }

    @Test
    void criaStatusNormalizadoEmMaiusculas() {
        when(repositorio.findByDescricaoIgnoreCase("EM TREINAMENTO")).thenReturn(Optional.empty());
        when(repositorio.save(any(StatusEmpregado.class))).thenAnswer(invocation -> {
            StatusEmpregado status = invocation.getArgument(0);
            status.setId(6L);
            return status;
        });

        StatusEmpregadoDTO resposta = servico.criar(new StatusEmpregadoDTO(null, "  em   treinamento ", 0));

        assertEquals(6L, resposta.id());
        assertEquals("EM TREINAMENTO", resposta.descricao());
        ArgumentCaptor<StatusEmpregado> captor = ArgumentCaptor.forClass(StatusEmpregado.class);
        verify(repositorio).save(captor.capture());
        assertEquals("EM TREINAMENTO", captor.getValue().getDescricao());
    }

    @Test
    void rejeitaStatusDuplicado() {
        StatusEmpregado existente = new StatusEmpregado("TREINAMENTO");
        existente.setId(1L);
        when(repositorio.findByDescricaoIgnoreCase("TREINAMENTO")).thenReturn(Optional.of(existente));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> servico.criar(new StatusEmpregadoDTO(null, "treinamento", 0)));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(repositorio, never()).save(any());
    }

    @Test
    void statusEstruturalNaoPodeSerRenomeado() {
        StatusEmpregado ativo = new StatusEmpregado(ConstantesDominio.STATUS_ATIVO);
        ativo.setId(1L);
        when(repositorio.buscarPorIdParaAtualizacao(1L)).thenReturn(Optional.of(ativo));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> servico.atualizar(1L, new StatusEmpregadoDTO(null, "OUTRO", 0)));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(repositorio, never()).save(any());
    }

    @Test
    void statusEstruturalNaoPodeSerExcluido() {
        StatusEmpregado ativo = new StatusEmpregado(ConstantesDominio.STATUS_ATIVO);
        ativo.setId(1L);
        when(repositorio.buscarPorIdParaAtualizacao(1L)).thenReturn(Optional.of(ativo));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> servico.excluir(1L));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void statusCustomizadoVinculadoNaoPodeSerExcluido() {
        StatusEmpregado customizado = new StatusEmpregado("TREINAMENTO");
        customizado.setId(9L);
        when(repositorio.buscarPorIdParaAtualizacao(9L)).thenReturn(Optional.of(customizado));
        when(empregadoRepositorio.existsByStatus_Id(9L)).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> servico.excluir(9L));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(repositorio, never()).delete(any());
    }
}
