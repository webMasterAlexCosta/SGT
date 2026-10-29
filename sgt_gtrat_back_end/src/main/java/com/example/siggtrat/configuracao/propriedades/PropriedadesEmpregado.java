package com.example.siggtrat.configuracao.propriedades;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "empregado")
@Validated
public record PropriedadesEmpregado(@Positive long maxJornadaMinutes) {
}
