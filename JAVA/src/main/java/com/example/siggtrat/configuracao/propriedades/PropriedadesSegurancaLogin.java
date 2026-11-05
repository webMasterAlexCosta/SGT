package com.example.siggtrat.configuracao.propriedades;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.security.autenticar")
@Validated
public record PropriedadesSegurancaLogin(
        @Positive int maxAttempts,
        @Positive long blockDurationMs
) {
}
