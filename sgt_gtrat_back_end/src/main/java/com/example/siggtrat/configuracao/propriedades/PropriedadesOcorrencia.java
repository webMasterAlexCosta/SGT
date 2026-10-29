package com.example.siggtrat.configuracao.propriedades;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "ocorrencia")
@Validated
public record PropriedadesOcorrencia(@Positive int maxPeriodDays) {
}
