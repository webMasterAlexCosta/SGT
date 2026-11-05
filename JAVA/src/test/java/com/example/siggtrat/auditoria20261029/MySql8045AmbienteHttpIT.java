package com.example.siggtrat.auditoria20261029;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/** Prova de conexão efetiva da aplicação; não é executado sem o banco autorizado. */
@org.springframework.test.context.TestPropertySource(properties = "app.homologacao.mysql8045=true")
class MySql8045AmbienteHttpIT extends AuditoriaHttpSuporte {
    @Autowired DataSource dataSource;
    @Autowired Environment environment;
    @Autowired JdbcTemplate jdbc;
    @Autowired org.springframework.context.ApplicationContext contexto;

    @Test
    void conexaoEfetivaUsaServidorExatoESchemaSomenteHibernate() throws Exception {
        assertThat(contexto.getBeansOfType(com.example.siggtrat.configuracao.VerificadorAmbienteMysql8045.class))
                .as("Guarda do próprio JAR ativa neste contexto real").hasSize(1);
        Map<String, Object> prova = new LinkedHashMap<>();
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("MySQL");
            assertThat(connection.getMetaData().getDatabaseProductVersion()).isEqualTo("8.0.45");
            prova.put("driver", connection.getMetaData().getDriverVersion());
            prova.put("catalog", connection.getCatalog());
            prova.put("metadataVersion", connection.getMetaData().getDatabaseProductVersion());
            String usuarioConfigurado = environment.getRequiredProperty("spring.datasource.username");
            assertThat(usuarioConfigurado).isNotBlank().isNotEqualToIgnoringCase("root");
            String usuarioMetadata = connection.getMetaData().getUserName();
            try (var statement = connection.createStatement();
                    var result = statement.executeQuery("SELECT CURRENT_USER(), USER(), DATABASE(), CONNECTION_ID()")) {
                assertThat(result.next()).isTrue();
                String contaEfetiva = result.getString(1);
                String identidadeCliente = result.getString(2);
                assertThat(contaEfetiva.substring(0, contaEfetiva.lastIndexOf('@')))
                        .as("Conta efetivamente autenticada nesta conexão JDBC")
                        .isEqualTo(usuarioConfigurado);
                assertThat(identidadeCliente.substring(0, identidadeCliente.lastIndexOf('@')))
                        .as("Identidade enviada pelo cliente JDBC")
                        .isEqualTo(usuarioConfigurado);
                assertThat(usuarioMetadata.split("@", 2)[0]).isEqualTo(usuarioConfigurado);
                assertThat(result.getString(3)).isEqualTo(connection.getCatalog());
                prova.put("configuredUsername", usuarioConfigurado);
                prova.put("metadataUsername", usuarioMetadata);
                prova.put("currentUser", contaEfetiva);
                prova.put("clientUser", identidadeCliente);
                prova.put("connectionId", result.getLong(4));
            }
        }
        assertThat(jdbc.queryForObject("SELECT VERSION()", String.class)).isEqualTo("8.0.45");
        assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("update");
        assertThat(environment.getProperty("spring.sql.init.mode")).isEqualTo("never");
        assertThat(jdbc.queryForObject("SELECT @@foreign_key_checks", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT @@innodb_strict_mode", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT @@character_set_server", String.class)).isEqualTo("utf8mb4");
        assertThat(jdbc.queryForObject("SELECT @@collation_server", String.class)).isEqualTo("utf8mb4_0900_ai_ci");
        assertThat(jdbc.queryForObject("SELECT MICROSECOND(CAST('2026-11-01 23:59:59.123456' AS DATETIME(6)))",
                Integer.class)).isEqualTo(123456);
        assertThat(jdbc.queryForObject("SELECT JSON_UNQUOTE(JSON_EXTRACT('{\"presenca\":true}', '$.presenca'))",
                String.class)).isEqualTo("true");
        prova.put("serverVersion", jdbc.queryForObject("SELECT VERSION()", String.class));
        prova.put("serverPort", jdbc.queryForObject("SELECT @@port", Integer.class));
        prova.put("serverUuid", jdbc.queryForObject("SELECT @@server_uuid", String.class));
        prova.put("collation", jdbc.queryForObject("SELECT @@collation_server", String.class));
        prova.put("sessionTimezone", jdbc.queryForObject("SELECT @@session.time_zone", String.class));
        prova.put("isolation", jdbc.queryForObject("SELECT @@transaction_isolation", String.class));
        prova.put("ddlAuto", environment.getProperty("spring.jpa.hibernate.ddl-auto"));
        prova.put("sqlInit", environment.getProperty("spring.sql.init.mode"));
        Path destino = Path.of("target/auditoria-http/mysql8045-conexao-aplicacao.json");
        Files.createDirectories(destino.getParent());
        Files.writeString(destino, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(prova));
    }
}

