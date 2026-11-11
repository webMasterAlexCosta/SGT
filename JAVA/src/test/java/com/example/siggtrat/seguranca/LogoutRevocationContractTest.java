package com.example.siggtrat.seguranca;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class LogoutRevocationContractTest {

    @Test
    void logoutCanAlwaysReachRevocationAndCookieCleanup() throws IOException {
        String security = Files.readString(Path.of("src/main/java/com/example/siggtrat/configuracao/ConfiguracaoSeguranca.java"));
        String authServico = Files.readString(Path.of("src/main/java/com/example/siggtrat/servico/AutenticacaoServico.java"));

        assertTrue(security.contains("HttpMethod.POST"));
        assertTrue(security.contains("\"/api/autenticacao/autenticar\""));
        assertTrue(security.contains("\"/api/autenticacao/cadastrar\""));
        assertTrue(security.contains("\"/api/autenticacao/encerrar-sessao\""));
        assertTrue(authServico.contains("revogacaoSessaoServico.revogarToken(token)"));
        String revogacao = Files.readString(Path.of("src/main/java/com/example/siggtrat/servico/RevogacaoSessaoServico.java"));
        assertTrue(revogacao.contains("tokens.excluirPorTokenBruto(tokenBruto)"));
        assertTrue(authServico.contains("jwtAuthenticationFilter.removerTokenDoCache(token)"));
        assertTrue(authServico.contains("ResponseCookie.from(ConstantesDominio.COOKIE_AUTH, \"\")"));
        assertTrue(authServico.contains(".maxAge(0)"));
        assertTrue(authServico.contains("headers.setCacheControl(\"no-store, no-cache, must-revalidate\")"));
    }

    @Test
    void environmentDocumentsThirtyMinuteInactivity() throws IOException {
        String example = Files.readString(Path.of(".env.example"));
        String properties = Files.readString(Path.of("src/main/resources/application.properties"));

        assertTrue(example.contains("JWT_MAX_INACTIVITY_MINUTES=30"));
        assertTrue(properties.contains("${JWT_MAX_INACTIVITY_MINUTES}"));
    }
}
