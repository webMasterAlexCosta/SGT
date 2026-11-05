package com.example.siggtrat.controlador;

import com.example.siggtrat.configuracao.propriedades.PropriedadesAutenticacao;
import com.example.siggtrat.constante.ConstantesDominio;
import com.example.siggtrat.dto.AlteracaoSenhaInicialAutenticacaoDTO;
import com.example.siggtrat.dto.CredenciaisLoginDTO;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.entidade.TokenAtivo;
import com.example.siggtrat.repositorio.UsuarioAplicacaoRepositorio;
import com.example.siggtrat.repositorio.GestaoLocalRepositorio;
import com.example.siggtrat.repositorio.JornadaRepositorio;
import com.example.siggtrat.repositorio.LotacaoRepositorio;
import com.example.siggtrat.repositorio.SuperintendenciaEstadualRepositorio;
import com.example.siggtrat.repositorio.TokenAtivoRepositorio;
import com.example.siggtrat.seguranca.FiltroAutenticacaoJwt;
import com.example.siggtrat.servico.AutenticacaoServico;
import com.example.siggtrat.servico.TentativaLoginServico;
import com.example.siggtrat.servico.TotpServico;
import com.example.siggtrat.servico.EscopoAcessoUsuarioServico;
import com.example.siggtrat.utilitario.UtilitarioJwt;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.servlet.http.Cookie;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutenticacaoControladorInitialPasswordTest {

    @Mock AuthenticationManager authenticationManager;
    @Mock UsuarioAplicacaoRepositorio userRepositorio;
    @Mock TokenAtivoRepositorio tokenAtivoRepositorio;
    @Mock PasswordEncoder passwordEncoder;
    @Mock UtilitarioJwt jwtUtil;
    @Mock TentativaLoginServico loginAttemptServico;
    @Mock LotacaoRepositorio lotacaoRepositorio;
    @Mock JornadaRepositorio jornadaRepositorio;
    @Mock SuperintendenciaEstadualRepositorio superintendenciaRepositorio;
    @Mock GestaoLocalRepositorio gestaoLocalRepositorio;
    @Mock EscopoAcessoUsuarioServico userAccessScopeServico;
    @Mock FiltroAutenticacaoJwt jwtAuthenticationFilter;
    @Mock TotpServico totpServico;

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void trocaInicialAlteraSenhaERenovaCookieDeSessao() {
        UsuarioAplicacao user = new UsuarioAplicacao();
        user.setId(3L);
        user.setUsername("bfct3");
        user.setActive(true);
        user.setPassword("hash-temporario");
        user.setForceChangePassword(true);
        user.setFailedAttempts(2);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("bfct3", "n/a", List.of())
        );

        when(userRepositorio.buscarPorUsernameParaAtualizacao("bfct3")).thenReturn(Optional.of(user));
        TokenAtivo sessaoAtual = new TokenAtivo();
        sessaoAtual.setUsername("bfct3");
        when(jwtUtil.validarToken("jwt-atual")).thenReturn(true);
        when(tokenAtivoRepositorio.buscarPorTokenBruto("jwt-atual")).thenReturn(Optional.of(sessaoAtual));
        when(passwordEncoder.matches("senha-nova", "hash-temporario")).thenReturn(false);
        when(passwordEncoder.encode("senha-nova")).thenReturn("hash-novo");
        when(userRepositorio.saveAndFlush(user)).thenReturn(user);
        when(jwtUtil.gerarToken(3L)).thenReturn("jwt-renovado");
        when(jwtUtil.extrairExpiracao("jwt-renovado"))
                .thenReturn(Date.from(Instant.now().plusSeconds(3600)));
        when(tokenAtivoRepositorio.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AutenticacaoServico servico = new AutenticacaoServico(
                authenticationManager,
                userRepositorio,
                tokenAtivoRepositorio,
                passwordEncoder,
                jwtUtil,
                loginAttemptServico,
                lotacaoRepositorio,
                jornadaRepositorio,
                superintendenciaRepositorio,
                gestaoLocalRepositorio,
                userAccessScopeServico,
                jwtAuthenticationFilter,
                totpServico,
                new PropriedadesAutenticacao(6, 31_536_000, "Credenciais inválidas.")
        );

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(ConstantesDominio.COOKIE_AUTH, "jwt-atual"));
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("User-Agent", "teste");

        ResponseEntity<?> response = servico.alterarSenhaInicial(
                new AlteracaoSenhaInicialAutenticacaoDTO("senha-nova"),
                request
        );

        assertEquals(200, response.getStatusCode().value());
        assertEquals("hash-novo", user.getPassword());
        assertFalse(user.getForceChangePassword());
        assertEquals(0, user.getFailedAttempts());
        assertNull(user.getSecret2fa());
        assertFalse(user.getTwoFactorEnabled());
        List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertNotNull(cookies);
        assertEquals(3, cookies.size());
        assertTrue(cookies.stream().anyMatch(cookie ->
                cookie.startsWith(ConstantesDominio.COOKIE_AUTH + "=jwt-renovado;")
                        && cookie.contains("Path=/") && cookie.contains("HttpOnly")
                        && cookie.contains("SameSite=Strict")), "Deve renovar o cookie de autenticação.");
        assertTrue(cookies.stream().anyMatch(cookie ->
                cookie.matches(ConstantesDominio.COOKIE_DEVICE + "=[^;]+;.*")
                        && cookie.contains("Path=/") && cookie.contains("HttpOnly")
                        && cookie.contains("Max-Age=31536000") && cookie.contains("SameSite=Strict")),
                "Deve preservar o cookie de identificação do dispositivo.");
        assertTrue(cookies.stream().anyMatch(cookie -> cookie.startsWith("XSRF-TOKEN=;")
                        && cookie.contains("Max-Age=0") && cookie.contains("Path=/")
                        && !cookie.contains("HttpOnly")),
                "Deve remover o cookie CSRF para o cliente obter um token após a troca inicial.");
        verify(tokenAtivoRepositorio).deleteByUsername("bfct3");
        verify(jwtAuthenticationFilter).removerSessoesUsuarioDoCache("bfct3");
        verify(totpServico, never()).gerarSegredo();
    }

    @Test
    void loginComSegundoFatorAtivoNaoCriaSessaoAntesDoCodigo() {
        UsuarioAplicacao user = new UsuarioAplicacao();
        user.setId(7L);
        user.setUsername("usuario.2fa");
        user.setMatricula("89614745");
        user.setPassword("hash");
        user.setActive(true);
        user.setSecret2fa("JBSWY3DPEHPK3PXP");
        user.setTwoFactorEnabled(true);

        when(loginAttemptServico.obterTempoBloqueioRestante("89614745")).thenReturn(0L);
        when(userRepositorio.buscarPorMatriculaParaAtualizacao("89614745")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken(
                        "89614745",
                        null,
                        List.of(new SimpleGrantedAuthority("PERFIL_OPERACIONAL"))
                )
        );

        AutenticacaoServico servico = new AutenticacaoServico(
                authenticationManager,
                userRepositorio,
                tokenAtivoRepositorio,
                passwordEncoder,
                jwtUtil,
                loginAttemptServico,
                lotacaoRepositorio,
                jornadaRepositorio,
                superintendenciaRepositorio,
                gestaoLocalRepositorio,
                userAccessScopeServico,
                jwtAuthenticationFilter,
                totpServico,
                new PropriedadesAutenticacao(6, 31_536_000, "Credenciais inválidas.")
        );

        ResponseEntity<?> response = servico.autenticar(
                new CredenciaisLoginDTO("8.961.474-5", "senha", null),
                new MockHttpServletRequest()
        );

        assertEquals(202, response.getStatusCode().value());
        assertTrue(response.getBody() instanceof Map);
        assertEquals(true, ((Map<?, ?>) response.getBody()).get("requiresTwoFactor"));
        assertEquals(false, ((Map<?, ?>) response.getBody()).get("canSkipTwoFactor"));
        verify(jwtUtil, never()).gerarToken(anyLong());
        verify(jwtUtil, never()).gerarToken(anyLong(), anyBoolean());
        verify(tokenAtivoRepositorio, never()).save(any());
    }


    @Test
    void loginNaoPermiteIgnorarSegundoFatorAtivo() {
        UsuarioAplicacao user = new UsuarioAplicacao();
        user.setId(7L);
        user.setUsername("usuario.2fa");
        user.setMatricula("89614745");
        user.setPassword("hash");
        user.setActive(true);
        user.setSecret2fa("JBSWY3DPEHPK3PXP");
        user.setTwoFactorEnabled(true);

        when(loginAttemptServico.obterTempoBloqueioRestante("89614745")).thenReturn(0L);
        when(userRepositorio.buscarPorMatriculaParaAtualizacao("89614745")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken(
                        "89614745",
                        null,
                        List.of(new SimpleGrantedAuthority("PERFIL_OPERACIONAL"))
                )
        );
        AutenticacaoServico servico = new AutenticacaoServico(
                authenticationManager,
                userRepositorio,
                tokenAtivoRepositorio,
                passwordEncoder,
                jwtUtil,
                loginAttemptServico,
                lotacaoRepositorio,
                jornadaRepositorio,
                superintendenciaRepositorio,
                gestaoLocalRepositorio,
                userAccessScopeServico,
                jwtAuthenticationFilter,
                totpServico,
                new PropriedadesAutenticacao(6, 31_536_000, "Credenciais inválidas.")
        );

        ResponseEntity<?> response = servico.autenticar(
                new CredenciaisLoginDTO("8.961.474-5", "senha", null),
                new MockHttpServletRequest()
        );

        assertEquals(202, response.getStatusCode().value());
        assertTrue(response.getBody() instanceof Map);
        assertEquals(false, ((Map<?, ?>) response.getBody()).get("canSkipTwoFactor"));
        verify(jwtUtil, never()).gerarToken(anyLong(), anyBoolean());
        verify(tokenAtivoRepositorio, never()).save(any());
        verify(loginAttemptServico, never()).registrarSucessoLogin("89614745");
        verify(totpServico, never()).validarCodigo(any(), anyInt());
    }

    @Test
    void loginComCodigoValidoMarcaSessaoComProtecaoAvancada() {
        UsuarioAplicacao user = new UsuarioAplicacao();
        user.setId(8L);
        user.setUsername("usuario.2fa.validado");
        user.setMatricula("12345678");
        user.setPassword("hash");
        user.setActive(true);
        user.setSecret2fa("JBSWY3DPEHPK3PXP");
        user.setTwoFactorEnabled(true);

        when(loginAttemptServico.obterTempoBloqueioRestante("12345678")).thenReturn(0L);
        when(userRepositorio.buscarPorMatriculaParaAtualizacao("12345678")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken(
                        "12345678",
                        null,
                        List.of(new SimpleGrantedAuthority("PERFIL_OPERACIONAL"))
                )
        );
        when(totpServico.validarCodigo("JBSWY3DPEHPK3PXP", 123456)).thenReturn(true);
        when(jwtUtil.gerarToken(8L, true)).thenReturn("jwt-com-2fa");
        when(jwtUtil.extrairExpiracao("jwt-com-2fa"))
                .thenReturn(Date.from(Instant.now().plusSeconds(3600)));

        AutenticacaoServico servico = new AutenticacaoServico(
                authenticationManager,
                userRepositorio,
                tokenAtivoRepositorio,
                passwordEncoder,
                jwtUtil,
                loginAttemptServico,
                lotacaoRepositorio,
                jornadaRepositorio,
                superintendenciaRepositorio,
                gestaoLocalRepositorio,
                userAccessScopeServico,
                jwtAuthenticationFilter,
                totpServico,
                new PropriedadesAutenticacao(6, 31_536_000, "Credenciais inválidas.")
        );

        ResponseEntity<?> response = servico.autenticar(
                new CredenciaisLoginDTO("1.234.567-8", "senha", "123456"),
                new MockHttpServletRequest()
        );

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody() instanceof Map);
        assertEquals(true, ((Map<?, ?>) response.getBody()).get("twoFactorVerified"));
        verify(jwtUtil).gerarToken(8L, true);
        verify(totpServico).validarCodigo("JBSWY3DPEHPK3PXP", 123456);
        verify(tokenAtivoRepositorio).save(any());
    }
}
