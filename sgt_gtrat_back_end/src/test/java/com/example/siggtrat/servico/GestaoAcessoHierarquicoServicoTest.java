package com.example.siggtrat.servico;

import com.example.siggtrat.constante.ConstantesDominio;
import com.example.siggtrat.dto.AtivacaoUsuarioAplicacaoDTO;
import com.example.siggtrat.entidade.GestaoLocal;
import com.example.siggtrat.entidade.SuperintendenciaEstadual;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.repositorio.GestaoLocalRepositorio;
import com.example.siggtrat.repositorio.SuperintendenciaEstadualRepositorio;
import com.example.siggtrat.repositorio.TokenAtivoRepositorio;
import com.example.siggtrat.repositorio.UsuarioAplicacaoRepositorio;
import com.example.siggtrat.seguranca.FiltroAutenticacaoJwt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GestaoAcessoHierarquicoServicoTest {

    @Mock private UsuarioAplicacaoRepositorio usuarioRepositorio;
    @Mock private SuperintendenciaEstadualRepositorio superintendenciaRepositorio;
    @Mock private GestaoLocalRepositorio gestaoLocalRepositorio;
    @Mock private TokenAtivoRepositorio tokenRepositorio;
    @Mock private FiltroAutenticacaoJwt filtro;
    @Mock private PasswordEncoder codificadorSenha;
    @Mock private GeradorSenhaTemporariaServico geradorSenhaTemporaria;

    private GestaoAcessoHierarquicoServico servico;

    @BeforeEach
    void preparar() {
        servico = new GestaoAcessoHierarquicoServico(
                usuarioRepositorio,
                superintendenciaRepositorio,
                gestaoLocalRepositorio,
                tokenRepositorio,
                filtro,
                codificadorSenha,
                geradorSenhaTemporaria
        );
    }

    @Test
    void inativarGestorEstadualInativaEstruturaContasESenhasEmCascata() {
        SuperintendenciaEstadual rj = superintendencia(7L, false);
        GestaoLocal gestaoLocal = gestaoLocal(11L, rj, true);
        UsuarioAplicacao gestorEstadual = usuario(
                "gestor-estadual", ConstantesDominio.PERFIL_GESTOR_ESTADUAL, rj, null);
        UsuarioAplicacao usuarioLocal = usuario(
                "gestaoLocal", ConstantesDominio.PERFIL_GESTOR_LOCAL, rj, gestaoLocal);
        UsuarioAplicacao operacional = usuario(
                "operacional", ConstantesDominio.PERFIL_OPERACIONAL, rj, gestaoLocal);
        when(superintendenciaRepositorio.findById(7L)).thenReturn(Optional.of(rj));
        when(gestaoLocalRepositorio.findBySuperintendenciaIdOrderByNomeAsc(7L))
                .thenReturn(List.of(gestaoLocal));
        when(usuarioRepositorio.buscarUsuariosVinculadosASuperintendencia(7L))
                .thenReturn(List.of(gestorEstadual, usuarioLocal, operacional));
        when(geradorSenhaTemporaria.gerar()).thenReturn("Descartada@1");
        when(codificadorSenha.encode("Descartada@1")).thenReturn("hash-descartado");

        servico.inativarSuperintendencia(7L);

        assertFalse(rj.getAtivo());
        assertFalse(gestaoLocal.getAtivo());
        for (UsuarioAplicacao usuario : List.of(gestorEstadual, usuarioLocal, operacional)) {
            assertFalse(usuario.getActive());
            assertTrue(usuario.getForceChangePassword());
            assertNull(usuario.getSecret2fa());
            assertFalse(usuario.getTwoFactorEnabled());
            assertNotEquals("hash-antigo", usuario.getPassword());
        }
        verify(gestaoLocalRepositorio).saveAll(List.of(gestaoLocal));
        verify(usuarioRepositorio).saveAll(List.of(gestorEstadual, usuarioLocal, operacional));
        verify(tokenRepositorio).deleteByUsername("gestor-estadual");
        verify(tokenRepositorio).deleteByUsername("gestaoLocal");
        verify(tokenRepositorio).deleteByUsername("operacional");
        verify(filtro).removerSessoesUsuarioDoCache("gestor-estadual");
        verify(filtro).removerSessoesUsuarioDoCache("gestaoLocal");
        verify(filtro).removerSessoesUsuarioDoCache("operacional");
    }

    @Test
    void reativarGestorEstadualAtivaSomenteSeuNivelComSenhaTemporaria() {
        SuperintendenciaEstadual rj = superintendencia(7L, false);
        GestaoLocal gestaoLocal = gestaoLocal(11L, rj, false);
        UsuarioAplicacao gestorEstadual = usuario(
                "gestor-estadual", ConstantesDominio.PERFIL_GESTOR_ESTADUAL, rj, null);
        gestorEstadual.setActive(false);
        UsuarioAplicacao operacional = usuario(
                "operacional", ConstantesDominio.PERFIL_OPERACIONAL, rj, gestaoLocal);
        operacional.setActive(false);
        when(usuarioRepositorio.findByPerfilAndSuperintendenciaIdOrderByUsernameAsc(
                com.example.siggtrat.dominio.acesso.PerfilAcesso.GESTOR_ESTADUAL, 7L))
                .thenReturn(List.of(gestorEstadual));
        when(superintendenciaRepositorio.findById(7L)).thenReturn(Optional.of(rj));
        when(gestaoLocalRepositorio.findBySuperintendenciaIdOrderByNomeAsc(7L))
                .thenReturn(List.of(gestaoLocal));
        when(usuarioRepositorio.buscarUsuariosVinculadosASuperintendencia(7L))
                .thenReturn(List.of(gestorEstadual, operacional));
        when(geradorSenhaTemporaria.gerar())
                .thenReturn("Descartada@1", "Nova@Senha2026");
        when(codificadorSenha.encode("Descartada@1")).thenReturn("hash-descartado-1");
        when(codificadorSenha.encode("Nova@Senha2026")).thenReturn("hash-novo");

        AtivacaoUsuarioAplicacaoDTO resposta = servico.ativarGestorEstadual(7L);

        assertTrue(rj.getAtivo());
        assertTrue(gestorEstadual.getActive());
        assertEquals("hash-novo", gestorEstadual.getPassword());
        assertFalse(gestaoLocal.getAtivo());
        assertFalse(operacional.getActive());
        assertEquals("Nova@Senha2026", resposta.temporaryPassword());
        verify(superintendenciaRepositorio, times(2)).save(rj);
        verify(gestaoLocalRepositorio, never()).save(any());
        verify(gestaoLocalRepositorio).saveAll(List.of(gestaoLocal));
        verify(usuarioRepositorio).save(gestorEstadual);
    }

    @Test
    void reativarUmGestorLocalNaoExigeUnicidadeNemAtivaOperacionais() {
        SuperintendenciaEstadual rj = superintendencia(7L, true);
        GestaoLocal gestaoLocal = gestaoLocal(11L, rj, true);
        UsuarioAplicacao usuarioLocal = usuario(
                "gestor-local-1", ConstantesDominio.PERFIL_GESTOR_LOCAL, rj, gestaoLocal);
        UsuarioAplicacao outroGestorLocal = usuario(
                "gestor-local-2", ConstantesDominio.PERFIL_GESTOR_LOCAL, rj, gestaoLocal);
        UsuarioAplicacao operacional = usuario(
                "operacional", ConstantesDominio.PERFIL_OPERACIONAL, rj, gestaoLocal);
        usuarioLocal.setActive(false);
        outroGestorLocal.setActive(false);
        operacional.setActive(false);
        when(geradorSenhaTemporaria.gerar()).thenReturn("Local@Nova2026");
        when(codificadorSenha.encode("Local@Nova2026")).thenReturn("hash-local");

        AtivacaoUsuarioAplicacaoDTO resposta = servico.ativarComSenhaTemporaria(usuarioLocal);

        assertTrue(gestaoLocal.getAtivo());
        assertTrue(usuarioLocal.getActive());
        assertFalse(outroGestorLocal.getActive());
        assertFalse(operacional.getActive());
        assertEquals("Local@Nova2026", resposta.temporaryPassword());
        verify(usuarioRepositorio).save(usuarioLocal);
        verify(usuarioRepositorio, never()).findByPerfilAndGestaoLocalIdOrderByUsernameAsc(any(), any());
    }

    @Test
    void gestaoLocalSoPodeSerReativadoDepoisDoGestorEstadual() {
        SuperintendenciaEstadual rj = superintendencia(7L, false);
        GestaoLocal gestaoLocal = gestaoLocal(11L, rj, false);
        UsuarioAplicacao usuarioLocal = usuario(
                "gestaoLocal", ConstantesDominio.PERFIL_GESTOR_LOCAL, rj, gestaoLocal);
        usuarioLocal.setActive(false);

        var erro = assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> servico.ativarComSenhaTemporaria(usuarioLocal));

        assertEquals(org.springframework.http.HttpStatus.CONFLICT, erro.getStatusCode());
        assertFalse(gestaoLocal.getAtivo());
        verify(usuarioRepositorio, never()).save(any());
    }

    @Test
    void operacionalSoPodeSerReativadoDepoisDoGestaoLocal() {
        SuperintendenciaEstadual rj = superintendencia(7L, true);
        GestaoLocal gestaoLocal = gestaoLocal(11L, rj, false);
        UsuarioAplicacao operacional = usuario(
                "operacional", ConstantesDominio.PERFIL_OPERACIONAL, rj, gestaoLocal);
        operacional.setActive(false);

        var erro = assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> servico.ativarComSenhaTemporaria(operacional));

        assertEquals(org.springframework.http.HttpStatus.CONFLICT, erro.getStatusCode());
        verify(usuarioRepositorio, never()).save(any());
    }

    private static SuperintendenciaEstadual superintendencia(Long id, boolean ativa) {
        SuperintendenciaEstadual superintendencia = new SuperintendenciaEstadual("SE/RJ", "RJ");
        superintendencia.setId(id);
        superintendencia.setAtivo(ativa);
        return superintendencia;
    }

    private static GestaoLocal gestaoLocal(Long id, SuperintendenciaEstadual superintendencia, boolean ativo) {
        GestaoLocal gestaoLocal = new GestaoLocal();
        gestaoLocal.setId(id);
        gestaoLocal.setNome("GTRAT/RJ");
        gestaoLocal.setCodigo("GTRAT-RJ");
        gestaoLocal.setSuperintendencia(superintendencia);
        gestaoLocal.setAtivo(ativo);
        return gestaoLocal;
    }

    private static UsuarioAplicacao usuario(
            String username,
            String perfil,
            SuperintendenciaEstadual superintendencia,
            GestaoLocal gestaoLocal
    ) {
        UsuarioAplicacao usuario = new UsuarioAplicacao();
        usuario.setUsername(username);
        usuario.setPerfil(com.example.siggtrat.dominio.acesso.PerfilAcesso.valueOf((perfil).replace("PERFIL_", "")));
        usuario.setPassword("hash-antigo");
        usuario.setActive(true);
        usuario.setForceChangePassword(false);
        usuario.setTwoFactorEnabled(true);
        usuario.setSecret2fa("segredo-antigo");
        usuario.setSuperintendencia(superintendencia);
        usuario.setGestaoLocal(gestaoLocal);
        return usuario;
    }
}
