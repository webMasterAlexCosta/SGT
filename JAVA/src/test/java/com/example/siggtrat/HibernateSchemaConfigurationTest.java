package com.example.siggtrat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class HibernateSchemaConfigurationTest {

    @Test
    void perfilIsoladoDeIntegracaoTambemDesabilitaInicializacaoSql() throws IOException {
        Properties properties = new Properties();
        try (InputStream input = getClass().getResourceAsStream("/application-test.properties")) {
            if (input == null) throw new IOException("application-test.properties não encontrado no classpath");
            properties.load(input);
        }
        // O contexto mínimo de integração carrega exclusivamente este arquivo;
        // a regra never da configuração principal não é herdada nesse contexto.
        assertEquals("update", properties.getProperty("spring.jpa.hibernate.ddl-auto"));
        assertEquals("never", properties.getProperty("spring.sql.init.mode"));
    }

    @Test
    void deveCriarEAtualizarEsquemaExclusivamentePeloHibernate() throws IOException {
        Properties properties = new Properties();
        try (InputStream input = getClass().getResourceAsStream("/application.properties")) {
            if (input == null) {
                throw new IOException("application.properties não encontrado no classpath");
            }
            properties.load(input);
        }

        // A regra técnica desta entrega fixa update no arquivo da aplicação;
        // não aceita substituir esse valor por um modo destrutivo via placeholder.
        assertEquals("update", properties.getProperty("spring.jpa.hibernate.ddl-auto"));
        assertEquals("true", properties.getProperty("spring.jpa.generate-ddl"));
        assertEquals("never", properties.getProperty("spring.sql.init.mode"));
        assertNull(properties.getProperty("spring.flyway.enabled"));
        assertNull(properties.getProperty("spring.liquibase.enabled"));
    }
}
