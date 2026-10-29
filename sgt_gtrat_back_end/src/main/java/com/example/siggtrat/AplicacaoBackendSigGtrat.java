package com.example.siggtrat;

import com.example.siggtrat.constante.ConstantesDominio;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.retry.annotation.EnableRetry;

import java.util.TimeZone;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
@EnableAsync
@EnableRetry
@EnableCaching
public class AplicacaoBackendSigGtrat {

    public static void main(String[] args) {
        // Deve ocorrer antes da criação do contexto. Configurar o fuso apenas em
        // @PostConstruct permite que Jackson/Hibernate capturem o fuso do host e
        // pode deslocar LocalDate em ambientes configurados fora do Brasil.
        TimeZone.setDefault(TimeZone.getTimeZone(ConstantesDominio.TIMEZONE_PADRAO));
        SpringApplication.run(AplicacaoBackendSigGtrat.class, args);
    }
}
