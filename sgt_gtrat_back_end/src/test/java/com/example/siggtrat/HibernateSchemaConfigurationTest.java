package com.example.siggtrat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class HibernateSchemaConfigurationTest {

    @Test
    void deveCriarEAtualizarEsquemaExclusivamentePeloHibernate() throws IOException {
        Properties properties = new Properties();
        try (InputStream input = getClass().getResourceAsStream("/application.properties")) {
            if (input == null) {
                throw new IOException("application.properties não encontrado no classpath");
            }
            properties.load(input);
        }

        assertEquals("${JPA_DDL_AUTO:update}", properties.getProperty("spring.jpa.hibernate.ddl-auto"));
        assertEquals("true", properties.getProperty("spring.jpa.generate-ddl"));
        assertNull(properties.getProperty("spring.flyway.enabled"));
    }
}
