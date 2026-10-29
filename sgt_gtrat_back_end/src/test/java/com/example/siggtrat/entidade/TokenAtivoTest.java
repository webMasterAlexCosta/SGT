package com.example.siggtrat.entidade;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TokenAtivoTest {

    @Test
    void persisteSomenteHashDoJwt() {
        String jwt = "cabecalho.payload.assinatura";
        TokenAtivo token = TokenAtivo.deTokenBruto(jwt, "usuario", LocalDateTime.now(), LocalDateTime.now().plusHours(1));

        assertFalse(jwt.equals(token.getTokenHash()));
        assertEquals(64, token.getTokenHash().length());
    }
}
