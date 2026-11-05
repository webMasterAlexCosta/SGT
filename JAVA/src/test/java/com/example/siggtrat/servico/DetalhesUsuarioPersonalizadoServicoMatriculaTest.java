package com.example.siggtrat.servico;

import com.example.siggtrat.constante.ConstantesDominio;
import com.example.siggtrat.entidade.GestaoLocal;
import com.example.siggtrat.entidade.SuperintendenciaEstadual;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.repositorio.UsuarioAplicacaoRepositorio;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DetalhesUsuarioPersonalizadoServicoMatriculaTest {

    @Test
    void autenticaPorMatriculaNormalizadaEMantemUsernameInterno() {
        UsuarioAplicacaoRepositorio repositorio = mock(UsuarioAplicacaoRepositorio.class);
        UsuarioAplicacao user = new UsuarioAplicacao();
        user.setUsername("gestorEstadual");
        user.setMatricula("91000019");
        user.setPassword("hash");
        user.setPerfil(com.example.siggtrat.dominio.acesso.PerfilAcesso.valueOf(("PERFIL_GESTOR_ESTADUAL").replace("PERFIL_", "")));
        user.setActive(true);
        when(repositorio.findByMatricula("91000019")).thenReturn(Optional.of(user));

        DetalhesUsuarioPersonalizadoServico servico = new DetalhesUsuarioPersonalizadoServico(repositorio);
        UserDetails details = servico.loadUserByUsername("91.000.019");

        assertEquals("gestorEstadual", details.getUsername());
        verify(repositorio).findByMatricula("91000019");
        verify(repositorio, never()).findByUsername("91.000.019");
    }

    @Test
    void gestorEstadualDesativadoNaEstruturaPerdeAcesso() {
        UsuarioAplicacaoRepositorio repositorio = mock(UsuarioAplicacaoRepositorio.class);
        SuperintendenciaEstadual estrutura = superintendencia(false);
        UsuarioAplicacao gestor = usuarioAtivo(ConstantesDominio.PERFIL_GESTOR_ESTADUAL);
        gestor.setSuperintendencia(estrutura);
        when(repositorio.findById(10L)).thenReturn(Optional.of(gestor));

        UserDetails detalhes = new DetalhesUsuarioPersonalizadoServico(repositorio)
                .carregarUsuarioPorId(10L);

        assertFalse(detalhes.isEnabled());
    }

    @Test
    void gestaoLocalDependeDoProprioEstadoEDoGestorEstadual() {
        UsuarioAplicacaoRepositorio repositorio = mock(UsuarioAplicacaoRepositorio.class);
        SuperintendenciaEstadual estrutura = superintendencia(true);
        GestaoLocal gestaoLocal = new GestaoLocal();
        gestaoLocal.setAtivo(true);
        gestaoLocal.setSuperintendencia(estrutura);
        UsuarioAplicacao gestor = usuarioAtivo(ConstantesDominio.PERFIL_GESTOR_LOCAL);
        gestor.setGestaoLocal(gestaoLocal);
        when(repositorio.findById(20L)).thenReturn(Optional.of(gestor));

        DetalhesUsuarioPersonalizadoServico servico = new DetalhesUsuarioPersonalizadoServico(repositorio);
        assertTrue(servico.carregarUsuarioPorId(20L).isEnabled());

        estrutura.setAtivo(false);
        assertFalse(servico.carregarUsuarioPorId(20L).isEnabled());
    }

    private UsuarioAplicacao usuarioAtivo(String perfil) {
        UsuarioAplicacao usuario = new UsuarioAplicacao();
        usuario.setUsername("usuario");
        usuario.setPassword("hash");
        usuario.setPerfil(com.example.siggtrat.dominio.acesso.PerfilAcesso.valueOf((perfil).replace("PERFIL_", "")));
        usuario.setActive(true);
        return usuario;
    }

    private SuperintendenciaEstadual superintendencia(boolean ativa) {
        SuperintendenciaEstadual estrutura = new SuperintendenciaEstadual("SE/RJ", "RJ");
        estrutura.setAtivo(ativa);
        return estrutura;
    }
}
