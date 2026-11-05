package com.example.siggtrat.configuracao.propriedades;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "jwt")
@Validated
public record PropriedadesTokenJwt(
        @NotBlank @Size(min = 64) String secret,
        @Positive long expiration
) {
}
