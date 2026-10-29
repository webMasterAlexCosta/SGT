package com.example.siggtrat.configuracao.propriedades;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.security.jwt")
@Validated
public record PropriedadesSegurancaJwt(
        @Positive long maxInactivityMinutes,
        @Positive long updateIntervalSeconds,
        @Positive long cacheTtlMs,
        @Positive long cacheCleanupMs,
        @NotNull @Valid Limpeza cleanup
) {
    public record Limpeza(
            @NotBlank String cron,
            @Positive int batchSize
    ) {
    }
}
