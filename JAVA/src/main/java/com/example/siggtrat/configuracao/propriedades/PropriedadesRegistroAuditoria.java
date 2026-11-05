package com.example.siggtrat.configuracao.propriedades;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "auditlog")
@Validated
public record PropriedadesRegistroAuditoria(
        @NotNull @Valid Fila queue,
        @NotNull @Valid Descarga flush
) {
    public record Fila(@Positive int capacity) {
    }

    public record Descarga(
            @Positive int batchSize,
            @Positive long delayMs,
            @Positive int retryAttempts,
            @Positive long retryDelayMs
    ) {
    }
}
