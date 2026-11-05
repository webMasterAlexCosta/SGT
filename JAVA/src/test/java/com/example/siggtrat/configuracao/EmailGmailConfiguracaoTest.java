package com.example.siggtrat.configuracao;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailGmailConfiguracaoTest {

    @Test
    void configuracaoGmailFicaNoAmbienteJavaELogoExisteNoClasspath() throws Exception {
        String properties = Files.readString(Path.of("src/main/resources/application.properties"));
        String exemplo = Files.readString(Path.of(".env.example"));

        for (String chave : new String[] {
                "GMAIL_EMAIL_ENABLED",
                "GMAIL_SMTP_HOST",
                "GMAIL_SMTP_PORT",
                "GMAIL_SMTP_USERNAME",
                "GMAIL_SMTP_APP_PASSWORD",
                "GMAIL_EMAIL_FROM",
                "GMAIL_EMAIL_FROM_NAME",
                "PASSWORD_RESET_FRONTEND_URL"
        }) {
            assertTrue(exemplo.contains(chave + "="), () -> "Variável Gmail ausente: " + chave);
            assertTrue(properties.contains(chave), () -> "application.properties não referencia: " + chave);
        }

        assertTrue(Files.exists(Path.of("src/main/resources/email/correios.png")));
        assertTrue(properties.contains("mail.smtp.starttls.enable"));
        assertTrue(properties.contains("mail.smtp.connectiontimeout"));
        assertFalse(properties.contains("MICROSOFT_GRAPH_CLIENT_SECRET"));
    }
}
