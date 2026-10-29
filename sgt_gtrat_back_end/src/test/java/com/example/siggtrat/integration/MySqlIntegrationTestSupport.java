package com.example.siggtrat.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

/**
 * Base compartilhada pelos testes de banco real.
 *
 * <p>Por padrão usa Testcontainers/MySQL, preservando a homologação reproduzível.
 * Em ambientes sem Docker, a mesma suíte pode ser executada contra um MySQL
 * descartável informado explicitamente por {@code INTEGRATION_MYSQL_URL},
 * {@code INTEGRATION_MYSQL_USER} e {@code INTEGRATION_MYSQL_PASSWORD}. O schema
 * continua sendo criado e removido pelo Hibernate com create-drop.</p>
 */
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
abstract class MySqlIntegrationTestSupport {

    private static final String URL_NATIVA = primeiroNaoVazio(
            System.getProperty("integration.mysql.url"), System.getenv("INTEGRATION_MYSQL_URL"));
    private static final String USUARIO_NATIVO = primeiroNaoVazio(
            System.getProperty("integration.mysql.user"), System.getenv("INTEGRATION_MYSQL_USER"));
    private static final String SENHA_NATIVA = primeiroNaoNulo(
            System.getProperty("integration.mysql.password"), System.getenv("INTEGRATION_MYSQL_PASSWORD"));

    static final boolean MYSQL_NATIVO = URL_NATIVA != null;
    static final MySQLContainer<?> MYSQL;

    static {
        if (MYSQL_NATIVO) {
            MYSQL = null;
        } else {
            MYSQL = new MySQLContainer<>("mysql:8.4")
                    .withDatabaseName("sig_gestaoLocal_it")
                    .withUsername("sig_test")
                    .withPassword("sig_test")
                    .withCommand(
                            "--character-set-server=utf8mb4",
                            "--collation-server=utf8mb4_0900_ai_ci"
                    );
            MYSQL.start();
        }
    }

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        if (MYSQL_NATIVO) {
            registry.add("spring.datasource.url", () -> URL_NATIVA);
            registry.add("spring.datasource.username", () -> USUARIO_NATIVO == null ? "root" : USUARIO_NATIVO);
            registry.add("spring.datasource.password", () -> SENHA_NATIVA == null ? "" : SENHA_NATIVA);
        } else {
            registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
            registry.add("spring.datasource.username", MYSQL::getUsername);
            registry.add("spring.datasource.password", MYSQL::getPassword);
        }
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "16");
        registry.add("spring.datasource.hikari.minimum-idle", () -> "2");
        registry.add("spring.datasource.hikari.connection-timeout", () -> "5000");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.generate-ddl", () -> "true");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.MySQLDialect");
    }

    private static String primeiroNaoVazio(String a, String b) {
        if (a != null && !a.isBlank()) return a.trim();
        if (b != null && !b.isBlank()) return b.trim();
        return null;
    }

    private static String primeiroNaoNulo(String a, String b) {
        return a != null ? a : b;
    }
}
