package com.example.siggtrat.configuracao;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.*;

class VerificadorConfiguracaoProducaoTest {
    private MockEnvironment seguro() {
        return new MockEnvironment()
                .withProperty("spring.jpa.hibernate.ddl-auto", "validate")
                .withProperty("spring.jpa.generate-ddl", "false")
                .withProperty("spring.sql.init.mode", "never")
                .withProperty("spring.jpa.show-sql", "false")
                .withProperty("logging.level.org.hibernate.SQL", "OFF")
                .withProperty("logging.level.org.hibernate.orm.jdbc.bind", "OFF")
                .withProperty("server.ssl.enabled", "true")
                .withProperty("server.forward-headers-strategy", "none")
                .withProperty("cors.allowed-origins", "https://sgt.example.invalid:8443")
                .withProperty("app.recuperacao-senha.frontend-url", "https://sgt.example.invalid:8443")
                .withProperty("jwt.secret", "chave-sintetica-de-teste-".repeat(4))
                .withProperty("audit.secret", "auditoria-sintetica-de-teste-".repeat(3))
                .withProperty("spring.data.redis.password", "redis-sintetico-exclusivo")
                .withProperty("app.seed.usuarios-globais-enabled", "false")
                .withProperty("app.seed.gestores-se-gestao-local-enabled", "false")
                .withProperty("app.seed.unidades-operacionais-enabled", "false")
                .withProperty("app.seed.jornadas-operacionais-enabled", "false");
    }

    @Test void aceitaConfiguracaoProdTlsDiretoSemAlterarAmbiente() {
        MockEnvironment e = seguro();
        assertDoesNotThrow(() -> VerificadorConfiguracaoProducao.verificar(e));
        assertEquals("validate", e.getProperty("spring.jpa.hibernate.ddl-auto"));
    }

    @ParameterizedTest
    @CsvSource({
            "spring.jpa.hibernate.ddl-auto,update",
            "spring.jpa.generate-ddl,true",
            "spring.sql.init.mode,always",
            "spring.jpa.show-sql,true",
            "logging.level.org.hibernate.SQL,DEBUG",
            "logging.level.org.hibernate.orm.jdbc.bind,TRACE",
            "server.ssl.enabled,false",
            "server.forward-headers-strategy,framework",
            "app.seed.usuarios-globais-enabled,true",
            "app.seed.gestores-se-gestao-local-enabled,true",
            "app.seed.unidades-operacionais-enabled,true",
            "app.seed.jornadas-operacionais-enabled,true"
    })
    void recusaOverridesInsegurosComCodigoEPropriedadeSemValor(String key, String value) {
        var error = assertThrows(IllegalStateException.class,
                () -> VerificadorConfiguracaoProducao.verificar(seguro().withProperty(key, value)));
        assertTrue(error.getMessage().contains("SGT_PROD_CONFIG_INVALID"));
        assertTrue(error.getMessage().contains(key));
    }

    @ParameterizedTest
    @CsvSource({"http://sgt.example.invalid", "https://*.example.invalid", "https://u:p@sgt.example.invalid", "https://sgt.example.invalid/path"})
    void recusaOrigemInseguraSemRevelarSeuConteudo(String origin) {
        var error = assertThrows(IllegalStateException.class,
                () -> VerificadorConfiguracaoProducao.verificar(seguro().withProperty("cors.allowed-origins", origin)));
        assertTrue(error.getMessage().contains("cors.allowed-origins"));
        assertFalse(error.getMessage().contains(origin));
    }

    @Test void recusaUrlDeRecuperacaoHttp() {
        assertThrows(IllegalStateException.class, () -> VerificadorConfiguracaoProducao.verificar(seguro()
                .withProperty("app.recuperacao-senha.frontend-url", "http://sgt.example.invalid")));
    }

    @Test void segredosAusentesFracosOuIguaisNaoSaoExpostos() {
        for (String key : new String[]{"jwt.secret", "audit.secret", "spring.data.redis.password"}) {
            String unsafe = key.equals("spring.data.redis.password") ? "sgt-local-redis" : "SEGREDO_CURTO_NAO_LOGAR";
            var error = assertThrows(IllegalStateException.class,
                    () -> VerificadorConfiguracaoProducao.verificar(seguro().withProperty(key, unsafe)));
            assertTrue(error.getMessage().contains(key));
            assertFalse(error.getMessage().contains(unsafe));
        }
        var e = seguro();e.setProperty("audit.secret", e.getProperty("jwt.secret"));
        assertThrows(IllegalStateException.class, () -> VerificadorConfiguracaoProducao.verificar(e));
    }

    @Test void falhaAntesDeInstanciarQualquerDataSource() {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.setEnvironment(seguro().withProperty("server.ssl.enabled", "false"));
            context.getEnvironment().setActiveProfiles("prod");
            context.register(VerificadorConfiguracaoProducao.class);
            context.registerBean("datasourceSentinela", Sentinela.class);
            Sentinela.created = false;
            assertThrows(Exception.class, context::refresh);
            assertFalse(Sentinela.created);
        }
    }

    @Test void naoAlteraPerfilDeDesenvolvimento() {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.setEnvironment(new MockEnvironment());
            context.register(VerificadorConfiguracaoProducao.class);
            assertDoesNotThrow(context::refresh);
            assertTrue(context.getBeansOfType(VerificadorConfiguracaoProducao.class).isEmpty());
        }
    }

    static class Sentinela { static boolean created; Sentinela() {created = true;} }
}
