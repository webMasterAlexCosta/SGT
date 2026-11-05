package com.example.siggtrat.configuracao.propriedades;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

@ConfigurationProperties(prefix = "cors")
public record PropriedadesCors(String allowedOrigins) {

    public PropriedadesCors {
        allowedOrigins = allowedOrigins == null ? "" : allowedOrigins;
    }

    public List<String> listarOrigensPermitidas() {
        return Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .distinct()
                .toList();
    }
}
