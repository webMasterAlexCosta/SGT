package com.example.siggtrat.configuracao.propriedades;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "audit")
@Validated
public record PropriedadesAuditoria(
        @NotBlank String secret,
        @NotNull Path fallbackLog,
        @NotNull @Valid ConteudoRequisicao payload,
        @NotBlank String sensitiveFieldsRegex
) {
    public record ConteudoRequisicao(@Positive int maxBytes) {
    }
}
