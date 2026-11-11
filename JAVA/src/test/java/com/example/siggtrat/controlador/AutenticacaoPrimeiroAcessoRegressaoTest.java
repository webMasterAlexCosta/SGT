package com.example.siggtrat.controlador;

import com.example.siggtrat.configuracao.propriedades.PropriedadesAutenticacao;
import com.example.siggtrat.dto.AlteracaoSenhaInicialAutenticacaoDTO;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.entidade.TokenAtivo;
import com.example.siggtrat.constante.ConstantesDominio;
import com.example.siggtrat.repositorio.*;
import com.example.siggtrat.seguranca.FiltroAutenticacaoJwt;
import com.example.siggtrat.servico.*;
import com.example.siggtrat.utilitario.UtilitarioJwt;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.servlet.http.Cookie;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AutenticacaoPrimeiroAcessoRegressaoTest {
    @AfterEach void limpar() { SecurityContextHolder.clearContext(); }

    @Test
    void contaEstabelecidaNaoTrocaSenhaNemDesativaSegundoFatorPelaRotaInicial() {
        UsuarioAplicacaoRepositorio usuarios = mock(UsuarioAplicacaoRepositorio.class);
        TokenAtivoRepositorio tokens = mock(TokenAtivoRepositorio.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        UtilitarioJwt jwt = mock(UtilitarioJwt.class);
        UsuarioAplicacao usuario = new UsuarioAplicacao();
        usuario.setId(7L);
        usuario.setUsername("usuario.teste");
        usuario.setActive(true);
        usuario.setPassword("hash-original");
        usuario.setForceChangePassword(false);
        usuario.setTwoFactorEnabled(true);
        usuario.setSecret2fa("JBSWY3DPEHPK3PXP");
        when(usuarios.buscarPorUsernameParaAtualizacao(usuario.getUsername())).thenReturn(Optional.of(usuario));
        TokenAtivo sessaoAtual = new TokenAtivo();
        sessaoAtual.setUsername(usuario.getUsername());
        when(jwt.validarToken("jwt-atual")).thenReturn(true);
        when(tokens.buscarPorTokenBruto("jwt-atual")).thenReturn(Optional.of(sessaoAtual));
        when(encoder.encode(any())).thenReturn("hash-novo");
        when(jwt.gerarToken(7L)).thenReturn("jwt-novo");
        when(jwt.extrairExpiracao("jwt-novo")).thenReturn(Date.from(Instant.now().plusSeconds(3600)));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                usuario.getUsername(), null, List.of()));

        AutenticacaoServico servico = new AutenticacaoServico(mock(AuthenticationManager.class), usuarios,
                tokens, encoder, jwt, mock(TentativaLoginServico.class), mock(LotacaoRepositorio.class),
                mock(JornadaRepositorio.class), mock(SuperintendenciaEstadualRepositorio.class),
                mock(GestaoLocalRepositorio.class), mock(EscopoAcessoUsuarioServico.class),
                mock(FiltroAutenticacaoJwt.class), mock(TotpServico.class),
                new PropriedadesAutenticacao(6, 31_536_000, "Credenciais inválidas."), mock(com.example.siggtrat.servico.RevogacaoSessaoServico.class));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(ConstantesDominio.COOKIE_AUTH, "jwt-atual"));
        var resposta = servico.alterarSenhaInicial(new AlteracaoSenhaInicialAutenticacaoDTO("nova-senha"), request);

        assertEquals(403, resposta.getStatusCode().value());
        assertEquals("hash-original", usuario.getPassword());
        assertTrue(usuario.getTwoFactorEnabled());
        assertEquals("JBSWY3DPEHPK3PXP", usuario.getSecret2fa());
        verify(usuarios, never()).saveAndFlush(any());
        verify(tokens, never()).save(any());
    }
}
