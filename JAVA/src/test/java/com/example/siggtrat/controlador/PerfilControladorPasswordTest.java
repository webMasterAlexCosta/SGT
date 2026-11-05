package com.example.siggtrat.controlador;

import com.example.siggtrat.configuracao.propriedades.PropriedadesAutenticacao;
import com.example.siggtrat.configuracao.propriedades.PropriedadesArmazenamentoArquivo;
import com.example.siggtrat.configuracao.propriedades.PropriedadesSegundoFator;
import com.example.siggtrat.dto.SenhaAtualDTO;
import com.example.siggtrat.dto.AlteracaoSenhaPerfilDTO;
import com.example.siggtrat.dto.ResultadoAlteracaoSenhaDTO;
import com.example.siggtrat.dto.ConfiguracaoSegundoFatorDTO;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.repositorio.UsuarioAplicacaoRepositorio;
import com.example.siggtrat.repositorio.TokenAtivoRepositorio;
import com.example.siggtrat.seguranca.FiltroAutenticacaoJwt;
import com.example.siggtrat.servico.PerfilServico;
import com.example.siggtrat.servico.TotpServico;
import com.example.siggtrat.servico.CodigoQrServico;
import jakarta.validation.Validation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PerfilControladorPasswordTest {

    @Mock UsuarioAplicacaoRepositorio userRepositorio;
    @Mock PasswordEncoder passwordEncoder;
    @Mock TotpServico totpServico;
    @Mock CodigoQrServico qrCodeServico;
    @Mock TokenAtivoRepositorio tokenAtivoRepositorio;
    @Mock FiltroAutenticacaoJwt jwtAuthenticationFilter;

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void alterarPropriaSenhaMantemSessaoAtualAutenticada() {
        UsuarioAplicacao user = new UsuarioAplicacao();
        user.setId(10L);
        user.setUsername("bfct3");
        user.setPassword("hash-antigo");
        user.setSecret2fa("JBSWY3DPEHPK3PXP");
        user.setTwoFactorEnabled(true);
        user.setForceChangePassword(true);
        user.setFailedAttempts(3);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("bfct3", "n/a", List.of())
        );

        when(userRepositorio.findByUsername("bfct3")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("senha-atual", "hash-antigo")).thenReturn(true);
        when(passwordEncoder.matches("senha-nova", "hash-antigo")).thenReturn(false);
        when(totpServico.validarCodigo(user.getSecret2fa(), 123456)).thenReturn(true);
        when(passwordEncoder.encode("senha-nova")).thenReturn("hash-novo");
        when(userRepositorio.saveAndFlush(user)).thenReturn(user);

        PerfilServico servico = new PerfilServico(
                userRepositorio,
                passwordEncoder,
                totpServico,
                qrCodeServico,
                tokenAtivoRepositorio,
                jwtAuthenticationFilter,
                new PropriedadesArmazenamentoArquivo(Path.of("uploads-test")),
                new PropriedadesSegundoFator(30),
                new PropriedadesAutenticacao(6, 31_536_000, "Credenciais inválidas.")
        );

        ResponseEntity<?> response = servico.alterarSenha(
                new AlteracaoSenhaPerfilDTO("senha-atual", "senha-nova", "123456")
        );

        assertEquals(200, response.getStatusCode().value());
        assertEquals("hash-novo", user.getPassword());
        assertEquals(false, user.getForceChangePassword());
        assertEquals(0, user.getFailedAttempts());
        assertTrue(response.getBody() instanceof ResultadoAlteracaoSenhaDTO);
        assertEquals(false, ((ResultadoAlteracaoSenhaDTO) response.getBody()).reauthenticationRequired());
        verify(tokenAtivoRepositorio, never()).deleteByUsername("bfct3");
        verify(jwtAuthenticationFilter, never()).removerSessoesUsuarioDoCache("bfct3");
    }


    @Test
    void dtoPermiteTokenVazioQuandoSegundoFatorNaoEstaAtivo() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();

            var semToken = validator.validate(
                    new AlteracaoSenhaPerfilDTO("senha-atual", "senha-nova", "")
            );
            var tokenInvalido = validator.validate(
                    new AlteracaoSenhaPerfilDTO("senha-atual", "senha-nova", "12345")
            );

            assertTrue(semToken.isEmpty());
            assertTrue(tokenInvalido.stream().anyMatch(v ->
                    "O código de segurança deve conter 6 dígitos.".equals(v.getMessage())
            ));
        }
    }

    @Test
    void alterarSenhaSem2faIgnoraTokenAusenteEMantemSessao() {
        UsuarioAplicacao user = new UsuarioAplicacao();
        user.setId(12L);
        user.setUsername("usuario.sem2fa");
        user.setPassword("hash-antigo");
        user.setTwoFactorEnabled(false);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("usuario.sem2fa", "n/a", List.of())
        );

        when(userRepositorio.findByUsername("usuario.sem2fa")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("senha-atual", "hash-antigo")).thenReturn(true);
        when(passwordEncoder.matches("senha-nova", "hash-antigo")).thenReturn(false);
        when(passwordEncoder.encode("senha-nova")).thenReturn("hash-novo");
        when(userRepositorio.saveAndFlush(user)).thenReturn(user);

        PerfilServico servico = new PerfilServico(
                userRepositorio,
                passwordEncoder,
                totpServico,
                qrCodeServico,
                tokenAtivoRepositorio,
                jwtAuthenticationFilter,
                new PropriedadesArmazenamentoArquivo(Path.of("uploads-test")),
                new PropriedadesSegundoFator(30),
                new PropriedadesAutenticacao(6, 31_536_000, "Credenciais inválidas.")
        );

        ResponseEntity<?> response = servico.alterarSenha(
                new AlteracaoSenhaPerfilDTO("senha-atual", "senha-nova", null)
        );

        assertEquals(200, response.getStatusCode().value());
        assertEquals("hash-novo", user.getPassword());
        verifyNoInteractions(totpServico);
        verify(tokenAtivoRepositorio, never()).deleteByUsername("usuario.sem2fa");
        verify(jwtAuthenticationFilter, never()).removerSessoesUsuarioDoCache("usuario.sem2fa");
    }

    @Test
    void configuracaoDoSegundoFatorDevolveQrCodeSemAtivarAntesDaConfirmacao() {
        UsuarioAplicacao user = new UsuarioAplicacao();
        user.setId(11L);
        user.setUsername("usuario.teste");
        user.setPassword("hash-atual");
        user.setTwoFactorEnabled(false);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("usuario.teste", "n/a", List.of())
        );

        when(userRepositorio.findByUsername("usuario.teste")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("senha-atual", "hash-atual")).thenReturn(true);
        when(totpServico.gerarSegredo()).thenReturn("JBSWY3DPEHPK3PXP");
        when(qrCodeServico.gerarDataUrl(anyString())).thenReturn("data:image/svg+xml;base64,PHN2Zz4=");
        when(userRepositorio.saveAndFlush(user)).thenReturn(user);

        PerfilServico servico = new PerfilServico(
                userRepositorio,
                passwordEncoder,
                totpServico,
                qrCodeServico,
                tokenAtivoRepositorio,
                jwtAuthenticationFilter,
                new PropriedadesArmazenamentoArquivo(Path.of("uploads-test")),
                new PropriedadesSegundoFator(30),
                new PropriedadesAutenticacao(6, 31_536_000, "Credenciais inválidas.")
        );

        ResponseEntity<?> response = servico.configurarSegundoFator(new SenhaAtualDTO("senha-atual"));

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody() instanceof ConfiguracaoSegundoFatorDTO);
        assertEquals("data:image/svg+xml;base64,PHN2Zz4=", ((ConfiguracaoSegundoFatorDTO) response.getBody()).qrCodeDataUrl());
        assertEquals("JBSWY3DPEHPK3PXP", user.getPendingSecret2fa());
        assertEquals(false, user.getTwoFactorEnabled());
    }
}
