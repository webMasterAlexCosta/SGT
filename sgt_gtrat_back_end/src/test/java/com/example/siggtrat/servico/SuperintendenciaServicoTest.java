package com.example.siggtrat.servico;

import com.example.siggtrat.dto.SuperintendenciaDTO;
import com.example.siggtrat.dto.AtivacaoUsuarioAplicacaoDTO;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.entidade.SuperintendenciaEstadual;
import com.example.siggtrat.repositorio.SuperintendenciaEstadualRepositorio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SuperintendenciaServicoTest {

    @Mock private SuperintendenciaEstadualRepositorio repositorio;
    @Mock private AcessoOrganizacionalServico acessoOrganizacionalServico;
    @Mock private GestaoAcessoHierarquicoServico gestaoAcessoHierarquicoServico;
    @InjectMocks private SuperintendenciaServico servico;

    @Test
    void usuarioGlobalListaTodasSuperintendencias() {
        UsuarioAplicacao user = new UsuarioAplicacao();
        SuperintendenciaEstadual df = se(1L, "DF");
        SuperintendenciaEstadual sp = se(2L, "SP");
        when(acessoOrganizacionalServico.usuarioAtual()).thenReturn(user);
        when(acessoOrganizacionalServico.podeVisualizarTodasSuperintendencias(user)).thenReturn(true);
        when(repositorio.findAllByOrderByUfAsc()).thenReturn(List.of(df, sp));

        List<SuperintendenciaDTO> resposta = servico.listar();

        assertEquals(2, resposta.size());
        assertEquals("DF", resposta.get(0).uf());
        assertEquals("SP", resposta.get(1).uf());
    }

    @Test
    void usuarioRestritoListaSomenteSuaSuperintendencia() {
        SuperintendenciaEstadual rj = se(5L, "RJ");
        UsuarioAplicacao user = new UsuarioAplicacao();
        user.setSuperintendencia(rj);
        when(acessoOrganizacionalServico.usuarioAtual()).thenReturn(user);
        when(acessoOrganizacionalServico.podeVisualizarTodasSuperintendencias(user)).thenReturn(false);
        when(repositorio.findById(5L)).thenReturn(Optional.of(rj));

        List<SuperintendenciaDTO> resposta = servico.listar();

        assertEquals(1, resposta.size());
        assertEquals("RJ", resposta.get(0).uf());
        verify(repositorio, never()).findAllByOrderByUfAsc();
    }

    @Test
    void rejeitaUfInvalidaNaCriacao() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> servico.criar(new SuperintendenciaDTO(null, null, "XX", true)));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(repositorio, never()).save(any());
    }

    @Test
    void rejeitaUfDuplicada() {
        when(repositorio.findByUfIgnoreCase("SP")).thenReturn(Optional.of(se(1L, "SP")));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> servico.criar(new SuperintendenciaDTO(null, null, "SP", true)));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(repositorio, never()).save(any());
    }

    @Test
    void updateNaoPermiteAlterarUfEstrutural() {
        SuperintendenciaEstadual existente = se(1L, "SP");
        when(repositorio.findById(1L)).thenReturn(Optional.of(existente));
        when(repositorio.findByUfIgnoreCase("RJ")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> servico.atualizar(1L, new SuperintendenciaDTO(1L, null, "RJ", true)));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(repositorio, never()).save(any());
    }

    @Test
    void seEstruturalNuncaPodeSerExcluida() {
        when(repositorio.findById(1L)).thenReturn(Optional.of(se(1L, "DF")));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> servico.validarExclusao(1L));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void inativacaoPropagaParaTodaEstruturaEstadual() {
        SuperintendenciaEstadual rj = se(1L, "RJ");
        when(repositorio.findById(1L)).thenReturn(Optional.of(rj));
        when(repositorio.findByUfIgnoreCase("RJ")).thenReturn(Optional.of(rj));
        when(repositorio.save(rj)).thenReturn(rj);

        SuperintendenciaDTO resposta = servico.atualizar(
                1L,
                new SuperintendenciaDTO(1L, "SE/RJ", "RJ", false)
        );

        assertFalse(resposta.ativo());
        verify(gestaoAcessoHierarquicoServico).inativarSuperintendencia(1L);
    }

    @Test
    void reativacaoRetornaSenhaTemporariaSemReativarSubordinadosDiretamente() {
        SuperintendenciaEstadual rj = se(1L, "RJ");
        rj.setAtivo(false);
        AtivacaoUsuarioAplicacaoDTO credencial = new AtivacaoUsuarioAplicacaoDTO(
                "Usuário ativado.", "gestor.rj", "Senha@Temporaria2026", true);
        when(repositorio.findById(1L)).thenReturn(Optional.of(rj));
        when(gestaoAcessoHierarquicoServico.ativarGestorEstadual(1L)).thenReturn(credencial);

        AtivacaoUsuarioAplicacaoDTO resposta = servico.ativar(1L);

        assertEquals("gestor.rj", resposta.username());
        assertEquals("Senha@Temporaria2026", resposta.temporaryPassword());
        verify(gestaoAcessoHierarquicoServico).ativarGestorEstadual(1L);
    }

    @Test
    void atualizacaoComumNaoReativaGestorEstadualSemSenhaTemporaria() {
        SuperintendenciaEstadual rj = se(1L, "RJ");
        rj.setAtivo(false);
        when(repositorio.findById(1L)).thenReturn(Optional.of(rj));
        when(repositorio.findByUfIgnoreCase("RJ")).thenReturn(Optional.of(rj));

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> servico.atualizar(1L,
                        new SuperintendenciaDTO(1L, "SE/RJ", "RJ", true)));

        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
        verify(repositorio, never()).save(any());
    }

    private SuperintendenciaEstadual se(Long id, String uf) {
        SuperintendenciaEstadual se = new SuperintendenciaEstadual("SE/" + uf, uf);
        se.setId(id);
        se.setAtivo(true);
        return se;
    }
}
