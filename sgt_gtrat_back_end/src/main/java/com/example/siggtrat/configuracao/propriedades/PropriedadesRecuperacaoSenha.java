package com.example.siggtrat.configuracao.propriedades;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.recuperacao-senha")
@Validated
public record PropriedadesRecuperacaoSenha(
        @Positive long tokenMinutos,
        @Positive long intervaloMinimoSolicitacaoSegundos,
        @NotBlank String frontendUrl
) {
}
