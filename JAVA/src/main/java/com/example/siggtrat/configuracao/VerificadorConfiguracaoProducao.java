package com.example.siggtrat.configuracao;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** Gates da modalidade de produção com TLS direto, antes de qualquer DataSource. */
@Component
@Profile("prod")
public final class VerificadorConfiguracaoProducao implements BeanFactoryPostProcessor, EnvironmentAware {
    private static final Logger LOG = LoggerFactory.getLogger(VerificadorConfiguracaoProducao.class);
    private Environment ambiente;

    @Override public void setEnvironment(Environment environment) { this.ambiente = environment; }

    @Override public void postProcessBeanFactory(ConfigurableListableBeanFactory factory) throws BeansException {
        verificar(ambiente);
        LOG.info("SGT_PROD_CONFIG_VALID modalidade=TLS_DIRETO schema=validate seedsOpcionais=false");
    }

    static void verificar(Environment e) {
        Set<String> invalidas = new LinkedHashSet<>();
        exigir(e, invalidas, "spring.jpa.hibernate.ddl-auto", "validate");
        exigir(e, invalidas, "spring.jpa.generate-ddl", "false");
        exigir(e, invalidas, "spring.sql.init.mode", "never");
        exigir(e, invalidas, "spring.jpa.show-sql", "false");
        exigir(e, invalidas, "logging.level.org.hibernate.SQL", "OFF");
        exigir(e, invalidas, "logging.level.org.hibernate.orm.jdbc.bind", "OFF");
        exigir(e, invalidas, "server.ssl.enabled", "true");
        exigir(e, invalidas, "server.forward-headers-strategy", "none");
        for (String seed : new String[]{"usuarios-globais", "gestores-se-gestao-local", "unidades-operacionais", "jornadas-operacionais"}) {
            exigir(e, invalidas, "app.seed." + seed + "-enabled", "false");
        }
        String origins = valor(e, "cors.allowed-origins");
        if (origins.isBlank()) invalidas.add("cors.allowed-origins");
        for (String origin : origins.split(",", -1)) {
            if (!https(origin.trim(), true)) invalidas.add("cors.allowed-origins");
        }
        if (!https(valor(e, "app.recuperacao-senha.frontend-url"), false)) {
            invalidas.add("app.recuperacao-senha.frontend-url");
        }
        String jwt = valor(e, "jwt.secret");
        String audit = valor(e, "audit.secret");
        if (jwt.length() < 64) invalidas.add("jwt.secret");
        if (audit.length() < 32 || audit.equals(jwt)) invalidas.add("audit.secret");
        String redis = valor(e, "spring.data.redis.password");
        if (redis.isBlank() || redis.equals("sgt-local-redis")) invalidas.add("spring.data.redis.password");
        if (!invalidas.isEmpty()) {
            // Apenas nomes de propriedades fixos; nunca valores, URLs, causas de
            // parse contendo a entrada, senhas ou chaves na mensagem de startup.
            throw new IllegalStateException("SGT_PROD_CONFIG_INVALID: " + String.join(", ", invalidas));
        }
    }

    private static void exigir(Environment e, Set<String> invalidas, String key, String expected) {
        if (!expected.equalsIgnoreCase(valor(e, key))) invalidas.add(key);
    }

    private static String valor(Environment e, String key) {
        try { return e.getProperty(key, "").trim(); }
        catch (IllegalArgumentException error) { return ""; }
    }

    private static boolean https(String value, boolean originOnly) {
        try {
            URI uri = URI.create(value);
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null
                    && !uri.getHost().contains("*") && uri.getUserInfo() == null && uri.getFragment() == null
                    && (!originOnly || ((uri.getPath() == null || uri.getPath().isEmpty()) && uri.getQuery() == null));
        } catch (IllegalArgumentException error) { return false; }
    }
}
