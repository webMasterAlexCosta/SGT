package com.example.siggtrat.configuracao.propriedades;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sistema")
public record PropriedadesSistema(
        boolean manutencao,
        String previsaoRetorno
) {
    public PropriedadesSistema {
        previsaoRetorno = previsaoRetorno == null ? "" : previsaoRetorno;
    }
}
