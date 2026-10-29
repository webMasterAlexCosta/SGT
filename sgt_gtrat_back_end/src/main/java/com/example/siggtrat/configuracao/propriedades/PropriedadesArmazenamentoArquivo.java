package com.example.siggtrat.configuracao.propriedades;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "file")
@Validated
public record PropriedadesArmazenamentoArquivo(@NotNull Path uploadDir) {
}
