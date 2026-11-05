package com.example.siggtrat.seguranca;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class JwtAuthenticationFilterInactivityTest {

    @Test
    void expiresExactlyAtThirtyMinutes() {
        LocalDateTime lastSeen = LocalDateTime.of(2026, 8, 17, 12, 0, 0);

        assertFalse(FiltroAutenticacaoJwt.atingiuLimiteInatividade(
                lastSeen, lastSeen.plusMinutes(30).minusNanos(1), 30));
        assertTrue(FiltroAutenticacaoJwt.atingiuLimiteInatividade(
                lastSeen, lastSeen.plusMinutes(30), 30));
        assertTrue(FiltroAutenticacaoJwt.atingiuLimiteInatividade(
                lastSeen, lastSeen.plusMinutes(31), 30));
    }

    @Test
    void passiveSessionPollingDoesNotRenewActivity() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/autenticacao/sessao");
        request.setParameter("touch", "false");

        assertFalse(FiltroAutenticacaoJwt.ehRegistroAtividadeExplicita(request));
        assertFalse(FiltroAutenticacaoJwt.deveRegistrarAtividade(request));
    }

    @Test
    void explicitHumanHeartbeatRenewsActivity() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/autenticacao/sessao");
        request.setParameter("touch", "true");

        assertTrue(FiltroAutenticacaoJwt.ehRegistroAtividadeExplicita(request));
        assertTrue(FiltroAutenticacaoJwt.deveRegistrarAtividade(request));
    }

    @Test
    void backgroundGetDoesNotCountButMutationDoes() {
        MockHttpServletRequest get = new MockHttpServletRequest("GET", "/api/painel/resumo");
        MockHttpServletRequest post = new MockHttpServletRequest("POST", "/api/perfil/alterar-senha");
        MockHttpServletRequest encerrarSessao = new MockHttpServletRequest("POST", "/api/autenticacao/encerrar-sessao");

        assertFalse(FiltroAutenticacaoJwt.deveRegistrarAtividade(get));
        assertTrue(FiltroAutenticacaoJwt.deveRegistrarAtividade(post));
        assertFalse(FiltroAutenticacaoJwt.deveRegistrarAtividade(encerrarSessao));
    }
}
