package com.example.siggtrat.integration;

import com.example.siggtrat.infra.ConfiguracaoMysqlIntegracao;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

/**
 * Base compartilhada pelos testes de banco real.
 *
 * <p>Usa um MySQL isolado informado explicitamente por {@code INTEGRATION_MYSQL_URL},
 * {@code INTEGRATION_MYSQL_USER} e {@code INTEGRATION_MYSQL_PASSWORD}. O schema
 * continua sendo criado e atualizado pelo Hibernate com update. Testcontainers
 * é uma escolha separada e explícita ({@code INTEGRATION_MYSQL_MODE=testcontainers});
 * configuração nativa incompleta nunca recorre a root ou a Docker.</p>
 */
@org.springframework.context.annotation.Import(com.example.siggtrat.infra.GuardaMysql8045.class)
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

    private static final ConfiguracaoMysqlIntegracao CONFIGURACAO = ConfiguracaoMysqlIntegracao.carregar();
    static final boolean MYSQL_NATIVO = !CONFIGURACAO.usaTestcontainers();
    static final MySQLContainer<?> MYSQL;

    static {
        if (MYSQL_NATIVO) {
            MYSQL = null;
        } else {
            MYSQL = new MySQLContainer<>("mysql:8.0.45")
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
            registry.add("spring.datasource.url", CONFIGURACAO::url);
            registry.add("spring.datasource.username", CONFIGURACAO::usuario);
            registry.add("spring.datasource.password", CONFIGURACAO::senha);
        } else {
            registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
            registry.add("spring.datasource.username", MYSQL::getUsername);
            registry.add("spring.datasource.password", MYSQL::getPassword);
        }
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "8");
        registry.add("spring.datasource.hikari.minimum-idle", () -> "2");
        registry.add("spring.datasource.hikari.connection-timeout", () -> "5000");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
        registry.add("spring.sql.init.mode", () -> "never");
        registry.add("spring.jpa.generate-ddl", () -> "true");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.MySQLDialect");
    }

}
