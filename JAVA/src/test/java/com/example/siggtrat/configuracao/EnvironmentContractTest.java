package com.example.siggtrat.configuracao;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class EnvironmentContractTest {

    @Test
    void applicationPropertiesExposesSeedControlsThroughEnvironment() throws IOException {
        String properties = Files.readString(Path.of("src/main/resources/application.properties"));

        assertTrue(properties.contains(
                "spring.config.import=optional:file:${env-file:./.env}[.properties]"));
        assertTrue(properties.contains("app.seed.jornadas-operacionais-enabled=${APP_SEED_JORNADAS_OPERACIONAIS_ENABLED:true}"));
        assertTrue(properties.contains("app.seed.usuarios-globais-enabled=${APP_SEED_USUARIOS_GLOBAIS_ENABLED:false}"));
        assertTrue(properties.contains("app.seed.usuarios-globais-initial-password=${APP_SEED_USUARIOS_GLOBAIS_INITIAL_PASSWORD:}"));
        assertTrue(properties.contains("app.seed.gestores-se-gestao-local-enabled=${APP_SEED_GESTORES_SE_GESTAO_LOCAL_ENABLED:false}"));
        assertTrue(properties.contains("app.seed.gestores-initial-password=${APP_SEED_GESTORES_INITIAL_PASSWORD:}"));
        assertFalse(properties.contains("INITIAL_PASSWORD:123456"), "Bootstrap não fornece senha compartilhada.");
    }

    @Test
    void envExampleDocumentsOperationalVariables() throws IOException {
        String example = Files.readString(Path.of(".env.example"));

        for (String key : new String[] {
                "JPA_DEFAULT_BATCH_FETCH_SIZE",
                "APP_SEED_JORNADAS_OPERACIONAIS_ENABLED",
                "APP_SEED_USUARIOS_GLOBAIS_ENABLED",
                "APP_SEED_USUARIOS_GLOBAIS_INITIAL_PASSWORD",
                "PAINEL_CACHE_MINUTOS",
                "PAINEL_TIMEOUT_MINUTOS",
                "PAINEL_RECUPERACAO_INTERVALO_MS"
        }) {
            assertTrue(example.contains(key + "="), () -> "Variável ausente no .env.example: " + key);
        }

        String jwtSecret = example.lines()
                .filter(line -> line.startsWith("JWT_SECRET="))
                .map(line -> line.substring("JWT_SECRET=".length()))
                .findFirst()
                .orElseThrow();
        assertTrue(jwtSecret.isBlank(), "O exemplo público não fornece uma chave JWT utilizável.");
        try (var factory=jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            var validator=factory.getValidator();
            assertFalse(validator.validate(new com.example.siggtrat.configuracao.propriedades.PropriedadesTokenJwt(
                    "",3600)).isEmpty(), "Aplicação recusa a chave ainda não configurada.");
            assertFalse(validator.validate(new com.example.siggtrat.configuracao.propriedades.PropriedadesTokenJwt(
                    "A".repeat(63),3600)).isEmpty(), "Limite mínimo64continua obrigatório.");
            assertTrue(validator.validate(new com.example.siggtrat.configuracao.propriedades.PropriedadesTokenJwt(
                    "A".repeat(64),3600)).isEmpty());
        }
    }
}
