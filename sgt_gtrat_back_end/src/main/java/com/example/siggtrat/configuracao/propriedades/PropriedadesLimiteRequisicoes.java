package com.example.siggtrat.configuracao.propriedades;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.ratelimit")
@Validated
public record PropriedadesLimiteRequisicoes(
        @Positive long capacity,
        @Positive long durationSeconds,
        @Positive long cleanupRateMs,
        @Positive long timeoutMs
) {
}
