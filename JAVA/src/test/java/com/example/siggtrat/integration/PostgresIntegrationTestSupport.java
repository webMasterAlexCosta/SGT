package com.example.siggtrat.integration;

import com.example.siggtrat.infra.ConfiguracaoPostgresIntegracao;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base compartilhada pelos testes de banco real.
 *
 * <p>Exige INTEGRATION_POSTGRES_URL/USER/PASSWORD de uma base isolada,
 * previamente provisionada com role limitada. Não inicia containers nem
 * escolhe automaticamente uma identidade administrativa.</p>
 */
@org.springframework.context.annotation.Import(com.example.siggtrat.infra.GuardaPostgres186.class)
@SpringBootTest(
        classes = DatabaseIntegrationTestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.config.location=classpath:/application-test.properties",
                "spring.main.web-application-type=none",
                "spring.cache.type=none",
                "spring.jpa.open-in-view=false",
                "spring.jpa.show-sql=false",
                "spring.jpa.properties.hibernate.format_sql=false",
                "logging.level.root=WARN",
                "logging.level.org.testcontainers=INFO"
        }
)
@ActiveProfiles("test")
abstract class PostgresIntegrationTestSupport {

    private static final ConfiguracaoPostgresIntegracao CONFIGURACAO = ConfiguracaoPostgresIntegracao.carregar();
    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", CONFIGURACAO::url);
        registry.add("spring.datasource.username", CONFIGURACAO::usuario);
        registry.add("spring.datasource.password", CONFIGURACAO::senha);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "8");
        registry.add("spring.datasource.hikari.minimum-idle", () -> "2");
        registry.add("spring.datasource.hikari.connection-timeout", () -> "5000");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
        registry.add("spring.sql.init.mode", () -> "never");
        registry.add("spring.jpa.properties.hibernate.hbm2ddl.halt_on_error", () -> "true");
        registry.add("spring.jpa.generate-ddl", () -> "true");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

}
