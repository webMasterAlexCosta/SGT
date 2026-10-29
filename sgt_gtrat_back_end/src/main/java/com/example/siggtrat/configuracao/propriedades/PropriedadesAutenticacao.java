package com.example.siggtrat.configuracao.propriedades;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "auth")
@Validated
public record PropriedadesAutenticacao(
        @Positive int passwordMinLength,
        @Positive long deviceCookieMaxAgeSeconds,
        @NotBlank String genericError
) {
}
