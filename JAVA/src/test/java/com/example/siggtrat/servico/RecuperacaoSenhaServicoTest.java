package com.example.siggtrat.servico;

import com.example.siggtrat.configuracao.propriedades.PropriedadesRecuperacaoSenha;
import com.example.siggtrat.entidade.RecuperacaoSenhaToken;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.repositorio.RecuperacaoSenhaTokenRepositorio;
import com.example.siggtrat.repositorio.TokenAtivoRepositorio;
import com.example.siggtrat.repositorio.UsuarioAplicacaoRepositorio;
import com.example.siggtrat.seguranca.FiltroAutenticacaoJwt;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecuperacaoSenhaServicoTest {

    @Mock UsuarioAplicacaoRepositorio usuarioRepositorio;
    @Mock RecuperacaoSenhaTokenRepositorio tokenRepositorio;
    @Mock TokenAtivoRepositorio tokenAtivoRepositorio;
    @Mock PasswordEncoder passwordEncoder;
    @Mock EmailGmailServico emailServico;
    @Mock GeradorSenhaTemporariaServico geradorSenhaTemporaria;
    @Mock FiltroAutenticacaoJwt filtroJwt;

    @Test
    void solicitarEnviaLinkGmailComTokenDeUsoUnicoSemPersistirTokenBruto() {
        UsuarioAplicacao usuario = usuarioAtivo();
        when(usuarioRepositorio.findByEmailIgnoreCase("usuario@correios.com.br"))
                .thenReturn(Optional.of(usuario));
        when(tokenRepositorio.findTopByUsuario_IdOrderByCreatedAtDesc(10L))
                .thenReturn(Optional.empty());
        when(tokenRepositorio.saveAndFlush(any(RecuperacaoSenhaToken.class)))
                .thenAnswer(invocation -> {
                    RecuperacaoSenhaToken token = invocation.getArgument(0);
                    token.setId(99L);
                    return token;
                });
        when(emailServico.enviarHtml(anyString(), anyString(), anyString())).thenReturn(true);

        RecuperacaoSenhaServico servico = novoServico();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");

        String resposta = servico.solicitar(" USUARIO@CORREIOS.COM.BR ", request);

        assertTrue(resposta.startsWith("Se o e-mail estiver vinculado"));

        ArgumentCaptor<RecuperacaoSenhaToken> tokenCaptor = ArgumentCaptor.forClass(RecuperacaoSenhaToken.class);
        verify(tokenRepositorio).saveAndFlush(tokenCaptor.capture());
        RecuperacaoSenhaToken salvo = tokenCaptor.getValue();
        assertEquals(64, salvo.getTokenHash().length());
        assertNotNull(salvo.getExpireAt());
        assertNotNull(salvo.getRequestIpHash());
        assertEquals(usuario, salvo.getUsuario());

        ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailServico).enviarHtml(
                org.mockito.ArgumentMatchers.eq("usuario@correios.com.br"),
                org.mockito.ArgumentMatchers.contains("Recuperação de senha"),
                htmlCaptor.capture()
        );
        String html = htmlCaptor.getValue();
        assertTrue(html.contains("cid:" + EmailGmailServico.CID_LOGO_CORREIOS));
        assertTrue(html.contains("/recuperar-senha?token="));
        assertTrue(html.contains("Confirmar recuperação de senha"));
        assertFalse(html.contains(salvo.getTokenHash()));
    }

    @Test
    void confirmarGeraSenhaTemporariaForcaTrocaERevogaSessoes() {
        UsuarioAplicacao usuario = usuarioAtivo();
        usuario.setPassword("hash-antigo");
        usuario.setForceChangePassword(false);
        usuario.setFailedAttempts(4);
        usuario.setLockoutEnd(LocalDateTime.now().plusMinutes(10));

        RecuperacaoSenhaToken token = new RecuperacaoSenhaToken();
        token.setId(55L);
        token.setUsuario(usuario);
        token.setCreatedAt(RelogioSistema.agora().minusMinutes(1));
        token.setExpireAt(RelogioSistema.agora().plusMinutes(10));

        String temporaria = "Tmp@Correios2026!";
        when(tokenRepositorio.buscarParaConsumo(anyString())).thenReturn(Optional.of(token));
        when(geradorSenhaTemporaria.gerar()).thenReturn(temporaria);
        when(passwordEncoder.matches(temporaria, "hash-antigo")).thenReturn(false);
        when(passwordEncoder.encode(temporaria)).thenReturn("hash-temporario");
        when(emailServico.enviarHtml(anyString(), anyString(), anyString())).thenReturn(true);

        RecuperacaoSenhaServico servico = novoServico();
        String resposta = servico.confirmar("token-publico-de-teste");

        assertTrue(resposta.contains("senha temporária"));
        assertEquals("hash-temporario", usuario.getPassword());
        assertTrue(usuario.getForceChangePassword());
        assertEquals(0, usuario.getFailedAttempts());
        assertEquals(null, usuario.getLockoutEnd());
        assertNotNull(token.getUsedAt());

        ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailServico).enviarHtml(
                org.mockito.ArgumentMatchers.eq("usuario@correios.com.br"),
                org.mockito.ArgumentMatchers.contains("senha temporária"),
                htmlCaptor.capture()
        );
        assertTrue(htmlCaptor.getValue().contains(temporaria));
        assertTrue(htmlCaptor.getValue().contains("primeiro login"));

        verify(tokenAtivoRepositorio).deleteByUsername("usuario.teste");
        verify(filtroJwt).removerSessoesUsuarioDoCache("usuario.teste");
    }

    private RecuperacaoSenhaServico novoServico() {
        return new RecuperacaoSenhaServico(
                usuarioRepositorio,
                tokenRepositorio,
                tokenAtivoRepositorio,
                passwordEncoder,
                emailServico,
                geradorSenhaTemporaria,
                filtroJwt,
                new PropriedadesRecuperacaoSenha(20, 60, "http://localhost:5173")
        );
    }

    private UsuarioAplicacao usuarioAtivo() {
        UsuarioAplicacao usuario = new UsuarioAplicacao();
        usuario.setId(10L);
        usuario.setUsername("usuario.teste");
        usuario.setEmail("usuario@correios.com.br");
        usuario.setActive(true);
        return usuario;
    }
}
