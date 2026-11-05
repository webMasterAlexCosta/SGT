package com.example.siggtrat.configuracao.propriedades;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.email.gmail")
public record PropriedadesEmailGmail(
        boolean enabled,
        String usuario,
        String remetente,
        String nomeRemetente,
        String logoClasspath
) {
    public void validarConfiguracao() {
        if (!enabled) {
            throw new IllegalStateException("Envio de e-mail pelo Gmail está desabilitado.");
        }
        exigir("GMAIL_SMTP_USERNAME", usuario);
        exigir("GMAIL_EMAIL_FROM", remetenteEfetivo());
        exigir("GMAIL_EMAIL_FROM_NAME", nomeRemetente);
        exigir("GMAIL_EMAIL_LOGO_CLASSPATH", logoClasspath);
    }

    public String remetenteEfetivo() {
        return remetente == null || remetente.isBlank() ? usuario : remetente;
    }

    private static void exigir(String nome, String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException("Configuração obrigatória ausente: " + nome + ".");
        }
    }
}
