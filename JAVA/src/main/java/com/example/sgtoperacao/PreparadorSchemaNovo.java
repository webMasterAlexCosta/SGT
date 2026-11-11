package com.example.sgtoperacao;

import com.example.siggtrat.AplicacaoBackendSigGtrat;
import com.example.siggtrat.configuracao.VerificadorAmbientePostgres186;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

/** Ferramenta explícita para um banco novo. Fora do ComponentScan da aplicação. */
@Configuration(proxyBeanMethods = false)
@EnableAutoConfiguration
@EntityScan(basePackageClasses = AplicacaoBackendSigGtrat.class)
@Import(VerificadorAmbientePostgres186.class)
public class PreparadorSchemaNovo {
    private static final Logger LOG = LoggerFactory.getLogger(PreparadorSchemaNovo.class);

    public static void main(String[] args) {
        var app = new SpringApplication(PreparadorSchemaNovo.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.setDefaultProperties(Map.of(
                "spring.main.web-application-type", "none",
                "spring.jpa.hibernate.ddl-auto", "update",
                "spring.jpa.generate-ddl", "true",
                "spring.sql.init.mode", "never",
                "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error", "true"));
        try (var context = app.run(args)) {
            LOG.info("SGT_SCHEMA_NEW_READY schemaCriadoPorHibernate=true http=false seeds=false");
            System.out.println("SGT_SCHEMA_NEW_READY");
        }
    }

    @Bean
    static BeanFactoryPostProcessor conferirDestinoAntesDoHibernate(Environment environment) {
        return factory -> {
            verificarOpcoes(environment);
            try (Connection connection = DriverManager.getConnection(
                    environment.getRequiredProperty("spring.datasource.url"),
                    environment.getRequiredProperty("spring.datasource.username"),
                    environment.getRequiredProperty("spring.datasource.password"))) {
                connection.setReadOnly(true);
                VerificadorAmbientePostgres186.verificar(connection, "preparacaoSchemaNovo");
                verificarDestinoVazio(connection);
            } catch (SQLException error) {
                // Detalhes da resposta do servidor podem conter dados privados.
                LOG.error("SGT_SCHEMA_NEW_DATABASE_CHECK_FAILED sqlState={} tipo={}", error.getSQLState(), error.getClass().getSimpleName());
                throw new IllegalStateException("SGT_SCHEMA_NEW_DATABASE_CHECK_FAILED: identidade/conexao nao comprovada");
            }
        };
    }

    static void verificarOpcoes(Environment e) {
        if (!"true".equals(e.getProperty("sgt.schema.autorizado"))
                || !"update".equals(e.getProperty("spring.jpa.hibernate.ddl-auto"))
                || !"true".equals(e.getProperty("spring.jpa.generate-ddl"))
                || !"never".equals(e.getProperty("spring.sql.init.mode"))
                || !"none".equals(e.getProperty("spring.main.web-application-type"))) {
            throw new IllegalStateException("SGT_SCHEMA_NEW_CONFIG_INVALID: exige autorizacao explicita, update/true/never e ausencia de HTTP");
        }
    }

    static void verificarDestinoVazio(Connection c) throws SQLException {
        String query = """
                SELECT d.datdba=r.oid,
                       has_schema_privilege(current_user,'public','CREATE')
                           AND has_schema_privilege(current_user,'public','USAGE'),
                       (SELECT count(*) FROM pg_class t JOIN pg_namespace n ON n.oid=t.relnamespace
                        WHERE n.nspname !~ '^pg_' AND n.nspname<>'information_schema'
                          AND t.relkind IN ('r','p','S','v','m','f'))
                  FROM pg_database d JOIN pg_roles r ON r.rolname=current_user
                 WHERE d.datname=current_database()
                """;
        try (var statement = c.createStatement(); var result = statement.executeQuery(query)) {
            if (!result.next() || !result.getBoolean(1) || !result.getBoolean(2) || result.getLong(3) != 0) {
                throw new IllegalStateException("SGT_SCHEMA_NEW_TARGET_REFUSED: exige banco proprio vazio e schema autorizado; nada foi removido");
            }
        }
    }
}
