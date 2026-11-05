package com.example.siggtrat.seguranca;

import com.example.siggtrat.configuracao.propriedades.PropriedadesAutenticacao;
import com.example.siggtrat.constante.ConstantesDominio;
import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.dto.AlteracaoSenhaInicialAutenticacaoDTO;
import com.example.siggtrat.dto.CredenciaisLoginDTO;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.entidade.TokenAtivo;
import com.example.siggtrat.repositorio.*;
import com.example.siggtrat.servico.*;
import com.example.siggtrat.utilitario.UtilitarioJwt;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Mudanças reais de autenticação invalidam CSRF; uma resposta de erro comum não. */
class AutenticacaoRotacaoCsrfRegressaoTest {
    private AutenticacaoServico servico;
    private UsuarioAplicacao usuario;
    private MockHttpServletRequest requisicao;

    @BeforeEach
    void preparar() {
        UsuarioAplicacaoRepositorio usuarios = mock(UsuarioAplicacaoRepositorio.class);
        AuthenticationManager autenticacao = mock(AuthenticationManager.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        UtilitarioJwt jwt = mock(UtilitarioJwt.class);
        usuario = new UsuarioAplicacao(); usuario.setId(7L); usuario.setUsername("usuario-sintetico");
        usuario.setMatricula("90000111"); usuario.setEmail("sintetico@example.invalid");
        usuario.setPassword("HASH-SINTETICO-ANTERIOR"); usuario.setActive(true);
        usuario.setPerfil(PerfilAcesso.ADMINISTRADOR);
        when(usuarios.buscarPorUsernameParaAtualizacao(usuario.getUsername())).thenReturn(Optional.of(usuario));
        when(usuarios.buscarPorMatriculaParaAtualizacao(usuario.getMatricula())).thenReturn(Optional.of(usuario));
        var principal = new UsernamePasswordAuthenticationToken(usuario.getUsername(), null,
                List.of(new SimpleGrantedAuthority(ConstantesDominio.PERFIL_ADMINISTRADOR)));
        when(autenticacao.authenticate(any())).thenReturn(principal);
        when(encoder.encode(any())).thenReturn("HASH-SINTETICO-NOVO");
        when(jwt.gerarToken(anyLong())).thenReturn("JWT-SINTETICO-NOVO");
        when(jwt.gerarToken(anyLong(), anyBoolean())).thenReturn("JWT-SINTETICO-NOVO");
        when(jwt.extrairExpiracao(anyString())).thenReturn(Date.from(Instant.now().plusSeconds(3600)));
        TokenAtivoRepositorio tokens = mock(TokenAtivoRepositorio.class);
        TokenAtivo sessaoAtual = new TokenAtivo();
        sessaoAtual.setUsername(usuario.getUsername());
        when(jwt.validarToken("JWT-SINTETICO-ANTERIOR")).thenReturn(true);
        when(tokens.buscarPorTokenBruto("JWT-SINTETICO-ANTERIOR")).thenReturn(Optional.of(sessaoAtual));
        servico = new AutenticacaoServico(autenticacao, usuarios, tokens, encoder, jwt,
                mock(TentativaLoginServico.class), mock(LotacaoRepositorio.class), mock(JornadaRepositorio.class),
                mock(SuperintendenciaEstadualRepositorio.class), mock(GestaoLocalRepositorio.class),
                mock(EscopoAcessoUsuarioServico.class), mock(FiltroAutenticacaoJwt.class), mock(TotpServico.class),
                new PropriedadesAutenticacao(6, 31_536_000, "Credenciais inválidas."));
        SecurityContextHolder.getContext().setAuthentication(principal);
        requisicao = new MockHttpServletRequest();
        requisicao.setCookies(new Cookie("XSRF-TOKEN", "CSRF-SINTETICO-ANTERIOR"),
                new Cookie(ConstantesDominio.COOKIE_AUTH, "JWT-SINTETICO-ANTERIOR"),
                new Cookie(ConstantesDominio.COOKIE_DEVICE, "DISPOSITIVO-SINTETICO"));
    }

    @AfterEach void limpar() { SecurityContextHolder.clearContext(); }

    @Test
    void loginConcluidoInvalidaCsrfDoContextoAnterior() {
        var resposta = servico.autenticar(new CredenciaisLoginDTO(usuario.getMatricula(), "senha-sintetica", null), requisicao);

        assertEquals(200, resposta.getStatusCode().value());
        assertTrue(removeCookieCsrf(resposta), "Novo login exige bootstrap CSRF do novo contexto");
    }

    @Test
    void trocaInicialConcluidaInvalidaCsrfDoContextoAnterior() {
        var resposta = servico.alterarSenhaInicial(new AlteracaoSenhaInicialAutenticacaoDTO("nova-senha-sintetica"), requisicao);

        assertEquals(200, resposta.getStatusCode().value());
        assertTrue(removeCookieCsrf(resposta), "A liberação do primeiro acesso renova o contexto CSRF");
    }

    @Test
    void logoutInvalidaCsrfJuntoComCookiesDeAutenticacao() {
        var resposta = servico.encerrarSessao(requisicao);

        assertEquals(200, resposta.getStatusCode().value());
        assertTrue(removeCookieCsrf(resposta), "Logout remove o CSRF vinculado ao contexto encerrado");
    }

    @Test
    void rejeicaoDeTrocaInicialEmContaEstabelecidaPreservaCsrfAtual() {
        usuario.setForceChangePassword(false);

        var resposta = servico.alterarSenhaInicial(new AlteracaoSenhaInicialAutenticacaoDTO("nova-senha-sintetica"), requisicao);

        assertEquals(403, resposta.getStatusCode().value());
        assertFalse(removeCookieCsrf(resposta));
    }

    private boolean removeCookieCsrf(ResponseEntity<?> resposta) {
        List<String> cookies = resposta.getHeaders().get(HttpHeaders.SET_COOKIE);
        return cookies != null && cookies.stream().anyMatch(cookie -> cookie.startsWith("XSRF-TOKEN=")
                && cookie.contains("Max-Age=0") && cookie.contains("Path=/"));
    }
}
