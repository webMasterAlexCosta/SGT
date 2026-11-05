package com.example.siggtrat.configuracao.propriedades;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "frequencia.fechamento")
@Validated
public record PropriedadesFechamentoFrequencia(
        @Positive @Max(5000) int chunkSize,
        @Positive @Max(100000) int maxAlteracoes,
        @Positive @Max(16) int executorCorePoolSize,
        @Positive @Max(16) int executorMaxPoolSize,
        @Positive @Max(10000) int executorQueueCapacity,
        @Positive long recoveryDelayMs,
        @Positive long staleAfterSeconds
) {
    public PropriedadesFechamentoFrequencia {
        if (executorCorePoolSize > executorMaxPoolSize) {
            throw new IllegalArgumentException(
                    "O número mínimo de workers do fechamento não pode superar o máximo.");
        }
    }
}
