package com.example.siggtrat.seguranca;

import com.example.siggtrat.excecao.ExcecaoSenhaInvalida;
import com.example.siggtrat.excecao.ExcecaoSenhaAusente;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.repositorio.UsuarioAplicacaoRepositorio;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExigirSenhaInterceptorTest {

    private UsuarioAplicacaoRepositorio repositorioUsuario;
    private PasswordEncoder codificadorSenha;
    private InterceptadorExigirSenha interceptor;
    private HandlerMethod manipuladorProtegido;

    static class ControladorDeTeste {
        @ExigirSenha
        public void salvar() {}
    }

    @BeforeEach
    void preparar() throws Exception {
        repositorioUsuario = mock(UsuarioAplicacaoRepositorio.class);
        codificadorSenha = mock(PasswordEncoder.class);
        interceptor = new InterceptadorExigirSenha(repositorioUsuario, codificadorSenha);
        Method metodo = ControladorDeTeste.class.getMethod("salvar");
        manipuladorProtegido = new HandlerMethod(new ControladorDeTeste(), metodo);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("adm", "", java.util.List.of())
        );
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deveRejeitarQuandoCabecalhoNaoForEnviado() {
        MockHttpServletRequest requisicao = new MockHttpServletRequest();

        assertThrows(ExcecaoSenhaAusente.class, () ->
                interceptor.preHandle(requisicao, new MockHttpServletResponse(), manipuladorProtegido));
    }

    @Test
    void deveRejeitarSenhaIncorretaSemEncerrarSessao() {
        UsuarioAplicacao usuario = new UsuarioAplicacao();
        usuario.setUsername("adm");
        usuario.setPassword("hash");
        when(repositorioUsuario.findByUsername("adm")).thenReturn(Optional.of(usuario));
        when(codificadorSenha.matches("incorreta", "hash")).thenReturn(false);

        MockHttpServletRequest requisicao = new MockHttpServletRequest();
        requisicao.addHeader("X-User-Password", "incorreta");

        assertThrows(ExcecaoSenhaInvalida.class, () ->
                interceptor.preHandle(requisicao, new MockHttpServletResponse(), manipuladorProtegido));
    }

    @Test
    void deveLiberarSomenteARequisicaoQueContemASenhaCorreta() throws Exception {
        UsuarioAplicacao usuario = new UsuarioAplicacao();
        usuario.setUsername("adm");
        usuario.setPassword("hash");
        when(repositorioUsuario.findByUsername("adm")).thenReturn(Optional.of(usuario));
        when(codificadorSenha.matches("123456", "hash")).thenReturn(true);

        MockHttpServletRequest requisicao = new MockHttpServletRequest();
        requisicao.addHeader("X-User-Password", "123456");

        assertTrue(interceptor.preHandle(requisicao, new MockHttpServletResponse(), manipuladorProtegido));
        verify(codificadorSenha).matches("123456", "hash");
    }

    @Test
    void deveDispensarSenhaQuandoContaTem2faESessaoValidouSegundoFator() throws Exception {
        UsuarioAplicacao usuario = new UsuarioAplicacao();
        usuario.setUsername("adm");
        usuario.setPassword("hash");
        usuario.setTwoFactorEnabled(true);
        when(repositorioUsuario.findByUsername("adm")).thenReturn(Optional.of(usuario));

        MockHttpServletRequest requisicao = new MockHttpServletRequest();
        requisicao.setAttribute("twoFactorVerified", true);

        assertTrue(interceptor.preHandle(requisicao, new MockHttpServletResponse(), manipuladorProtegido));
        verifyNoInteractions(codificadorSenha);
    }

    @Test
    void deveContinuarExigindoSenhaQuando2faFoiIgnoradoNoLogin() {
        MockHttpServletRequest requisicao = new MockHttpServletRequest();
        requisicao.setAttribute("twoFactorVerified", false);

        assertThrows(ExcecaoSenhaAusente.class, () ->
                interceptor.preHandle(requisicao, new MockHttpServletResponse(), manipuladorProtegido));
        verifyNoInteractions(codificadorSenha);
    }
}
