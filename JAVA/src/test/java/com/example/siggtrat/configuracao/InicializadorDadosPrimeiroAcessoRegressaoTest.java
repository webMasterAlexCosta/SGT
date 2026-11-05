package com.example.siggtrat.configuracao;

import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.entidade.GestaoLocal;
import com.example.siggtrat.entidade.SuperintendenciaEstadual;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.repositorio.UsuarioAplicacaoRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** A carga inicial exige troca da senha fornecida e não redefine contas existentes. */
class InicializadorDadosPrimeiroAcessoRegressaoTest {
    private static final String USERNAME = "usuario-sintetico";
    private static final String MATRICULA = "90000111";
    private static final String HASH_INICIAL = "HASH-SINTETICO-INICIAL";
    private static final String HASH_EXISTENTE = "HASH-SINTETICO-JA-ALTERADO";
    private static final LocalDateTime BLOQUEIO = LocalDateTime.of(2030, 1, 1, 12, 0);

    private UsuarioAplicacaoRepositorio repositorio;
    private SuperintendenciaEstadual se;
    private GestaoLocal gestao;

    @BeforeEach
    void preparar() {
        repositorio = mock(UsuarioAplicacaoRepositorio.class);
        when(repositorio.save(any(UsuarioAplicacao.class))).thenAnswer(i -> i.getArgument(0));
        se = new SuperintendenciaEstadual("SE sintética", "QZ");
        se.setId(10L);
        gestao = new GestaoLocal();
        gestao.setId(20L); gestao.setNome("Gestão sintética"); gestao.setSuperintendencia(se);
    }

    @Test
    void novoUsuarioGlobalDeveTrocarSenhaInicialNoPrimeiroAcesso() {
        UsuarioAplicacao criado = garantirGlobal();

        assertAll(
                () -> assertTrue(criado.getForceChangePassword(), "Senha inicial global deve exigir troca"),
                () -> assertEquals(HASH_INICIAL, criado.getPassword()),
                () -> assertEquals(PerfilAcesso.ADMINISTRADOR, criado.getPerfil()),
                () -> assertTrue(criado.getActive()));
        verify(repositorio).save(criado);
    }

    @Test
    void novoUsuarioGestorDeveTrocarSenhaInicialNoPrimeiroAcesso() {
        UsuarioAplicacao criado = garantirGestor();

        assertAll(
                () -> assertTrue(criado.getForceChangePassword(), "Senha inicial do gestor deve exigir troca"),
                () -> assertEquals(HASH_INICIAL, criado.getPassword()),
                () -> assertEquals(PerfilAcesso.GESTOR_LOCAL, criado.getPerfil()),
                () -> assertSame(se, criado.getSuperintendencia()),
                () -> assertSame(gestao, criado.getGestaoLocal()),
                () -> assertTrue(criado.getActive()));
        verify(repositorio).save(criado);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void usuarioGlobalExistentePreservaSenhaTotpEEstado(boolean trocaObrigatoria) {
        UsuarioAplicacao existente = usuarioExistente(trocaObrigatoria);
        existente.setPerfil(PerfilAcesso.GESTOR_NACIONAL);
        when(repositorio.findByMatricula(MATRICULA)).thenReturn(Optional.of(existente));

        UsuarioAplicacao retornado = garantirGlobal();

        assertSame(existente, retornado);
        assertEstadoPreservado(existente, trocaObrigatoria);
        assertEquals(PerfilAcesso.GESTOR_NACIONAL, retornado.getPerfil());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void usuarioGestorExistentePreservaSenhaTotpEEstado(boolean trocaObrigatoria) {
        UsuarioAplicacao existente = usuarioExistente(trocaObrigatoria);
        existente.setPerfil(PerfilAcesso.GESTOR_ESTADUAL);
        SuperintendenciaEstadual seAnterior = new SuperintendenciaEstadual("SE anterior", "QY");
        seAnterior.setId(30L);
        GestaoLocal gestaoAnterior = new GestaoLocal(); gestaoAnterior.setId(40L);
        existente.setSuperintendencia(seAnterior); existente.setGestaoLocal(gestaoAnterior);
        when(repositorio.findByUsername(USERNAME)).thenReturn(Optional.of(existente));

        UsuarioAplicacao retornado = garantirGestor();

        assertSame(existente, retornado);
        assertEstadoPreservado(existente, trocaObrigatoria);
        assertEquals(PerfilAcesso.GESTOR_ESTADUAL, retornado.getPerfil());
        assertSame(seAnterior, retornado.getSuperintendencia());
        assertSame(gestaoAnterior, retornado.getGestaoLocal());
    }

    private UsuarioAplicacao garantirGlobal() {
        return ReflectionTestUtils.invokeMethod(InicializadorDados.class, "garantirUsuarioGlobal",
                repositorio, USERNAME, MATRICULA, "usuario-sintetico@example.invalid",
                PerfilAcesso.ADMINISTRADOR, HASH_INICIAL);
    }

    private UsuarioAplicacao garantirGestor() {
        return ReflectionTestUtils.invokeMethod(InicializadorDados.class, "garantirUsuarioGestor",
                repositorio, USERNAME, MATRICULA, "usuario-sintetico@example.invalid",
                PerfilAcesso.GESTOR_LOCAL, se, gestao, HASH_INICIAL);
    }

    private UsuarioAplicacao usuarioExistente(boolean trocaObrigatoria) {
        UsuarioAplicacao user = new UsuarioAplicacao();
        user.setId(99L); user.setUsername(USERNAME); user.setMatricula(MATRICULA);
        user.setEmail("contato-preservado@example.invalid"); user.setPassword(HASH_EXISTENTE);
        user.setActive(false); user.setForceChangePassword(trocaObrigatoria);
        user.setSecret2fa("SEGREDO-TOTP-SINTETICO"); user.setPendingSecret2fa("SEGREDO-PENDENTE-SINTETICO");
        user.setTwoFactorEnabled(true); user.setFailedAttempts(4); user.setLockoutEnd(BLOQUEIO);
        return user;
    }

    private void assertEstadoPreservado(UsuarioAplicacao user, boolean trocaObrigatoria) {
        assertAll(
                () -> assertEquals(HASH_EXISTENTE, user.getPassword()),
                () -> assertEquals(trocaObrigatoria, user.getForceChangePassword()),
                () -> assertEquals("SEGREDO-TOTP-SINTETICO", user.getSecret2fa()),
                () -> assertEquals("SEGREDO-PENDENTE-SINTETICO", user.getPendingSecret2fa()),
                () -> assertTrue(user.getTwoFactorEnabled()),
                () -> assertFalse(user.getActive()),
                () -> assertEquals(4, user.getFailedAttempts()),
                () -> assertEquals(BLOQUEIO, user.getLockoutEnd()),
                () -> assertEquals("contato-preservado@example.invalid", user.getEmail()),
                () -> assertEquals(MATRICULA, user.getMatricula()));
    }
}
