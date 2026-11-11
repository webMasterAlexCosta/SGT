package com.example.siggtrat.infra;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/** Resolução única da identidade de banco usada pelos dois suportes de integração. */
public final class ConfiguracaoPostgresIntegracao {
    private final boolean testcontainers;
    private final String url;
    private final String usuario;
    private final String senha;

    private ConfiguracaoPostgresIntegracao(boolean testcontainers, String url, String usuario, String senha) {
        this.testcontainers = testcontainers;
        this.url = url;
        this.usuario = usuario;
        this.senha = senha;
    }

    public static ConfiguracaoPostgresIntegracao carregar() {
        return resolver(System.getProperties(), System.getenv());
    }

    static ConfiguracaoPostgresIntegracao resolver(Properties propriedades, Map<String, String> ambiente) {
        String modo = valor(propriedades, ambiente, "mode");
        String url = valor(propriedades, ambiente, "url");
        String usuario = valor(propriedades, ambiente, "user");
        String senha = valor(propriedades, ambiente, "password");
        if ("testcontainers".equals(modo)) {
            throw new IllegalArgumentException("Use PostgreSQL 18.6 nativo explicitamente provisionado com role limitada; Testcontainers genérico não garante esse isolamento.");
        }
        if (modo != null && !modo.equals("native")) {
            throw new IllegalArgumentException("INTEGRATION_POSTGRES_MODE deve ser native.");
        }
        if (url == null || url.isBlank() || usuario == null || usuario.isBlank() || senha == null || senha.isBlank()) {
            throw new IllegalArgumentException("Informe INTEGRATION_POSTGRES_URL, INTEGRATION_POSTGRES_USER e "
                    + "INTEGRATION_POSTGRES_PASSWORD explicitamente. Não há fallback para conta administrativa ou Docker.");
        }
        usuario = usuario.trim();
        if (Set.of("root", "postgres", "rdsadmin", "azure_pg_admin", "cloudsqladmin").contains(usuario.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Integrações exigem uma conta própria restrita ao schema de validação.");
        }
        validarUrlSemCredenciais(url);
        return new ConfiguracaoPostgresIntegracao(false, url, usuario, senha);
    }

    private static String valor(Properties propriedades, Map<String, String> ambiente, String nome) {
        // Uma propriedade explícita vazia é um erro de configuração, não um pedido
        // para recorrer silenciosamente a outra identidade do ambiente.
        String propriedade = propriedades.getProperty("integration.postgres." + nome);
        return propriedade != null ? propriedade : ambiente.get("INTEGRATION_POSTGRES_" + nome.toUpperCase(Locale.ROOT));
    }

    private static void validarUrlSemCredenciais(String url) {
        if (!url.startsWith("jdbc:postgresql://")) {
            throw new IllegalArgumentException("A integração requer uma URL JDBC PostgreSQL com schema explícito.");
        }
        final URI uri;
        try {
            uri = URI.create(url.substring("jdbc:".length()));
        } catch (IllegalArgumentException erro) {
            throw new IllegalArgumentException("URL JDBC PostgreSQL inválida; configure endpoint e schema separadamente das credenciais.");
        }
        if (uri.getHost() == null || uri.getUserInfo() != null || uri.getPath() == null
                || !uri.getPath().matches("/[A-Za-z0-9_]+") || uri.getFragment() != null) {
            throw new IllegalArgumentException("A URL JDBC deve identificar um servidor/schema, sem usuário ou senha embutidos.");
        }
        if (uri.getRawQuery() == null) return;
        for (String parametro : uri.getRawQuery().split("&")) {
            String chave = URLDecoder.decode(parametro.split("=", 2)[0], StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
            if (chave.equals("user") || chave.startsWith("password") || chave.equals("sslpassword") || chave.equals("passfile") || chave.equals("service") || chave.equals("servicefile")) {
                throw new IllegalArgumentException("Usuário e senha da integração devem vir dos campos explícitos, não da URL.");
            }
        }
    }

    public boolean usaTestcontainers() { return testcontainers; }
    public String url() { return url; }
    public String usuario() { return usuario; }
    public String senha() { return senha; }
}
