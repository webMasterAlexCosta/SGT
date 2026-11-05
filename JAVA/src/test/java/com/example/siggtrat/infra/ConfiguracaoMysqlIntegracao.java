package com.example.siggtrat.infra;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/** Resolução única da identidade de banco usada pelos dois suportes de integração. */
public final class ConfiguracaoMysqlIntegracao {
    private final boolean testcontainers;
    private final String url;
    private final String usuario;
    private final String senha;

    private ConfiguracaoMysqlIntegracao(boolean testcontainers, String url, String usuario, String senha) {
        this.testcontainers = testcontainers;
        this.url = url;
        this.usuario = usuario;
        this.senha = senha;
    }

    public static ConfiguracaoMysqlIntegracao carregar() {
        return resolver(System.getProperties(), System.getenv());
    }

    static ConfiguracaoMysqlIntegracao resolver(Properties propriedades, Map<String, String> ambiente) {
        String modo = valor(propriedades, ambiente, "mode");
        String url = valor(propriedades, ambiente, "url");
        String usuario = valor(propriedades, ambiente, "user");
        String senha = valor(propriedades, ambiente, "password");
        if ("testcontainers".equals(modo)) {
            if (url != null || usuario != null || senha != null) {
                throw new IllegalArgumentException("Testcontainers exige escolha explícita sem configuração nativa parcial.");
            }
            return new ConfiguracaoMysqlIntegracao(true, null, null, null);
        }
        if (modo != null && !modo.equals("native")) {
            throw new IllegalArgumentException("INTEGRATION_MYSQL_MODE deve ser native ou testcontainers.");
        }
        if (url == null || url.isBlank() || usuario == null || usuario.isBlank() || senha == null || senha.isBlank()) {
            throw new IllegalArgumentException("Informe INTEGRATION_MYSQL_URL, INTEGRATION_MYSQL_USER e "
                    + "INTEGRATION_MYSQL_PASSWORD explicitamente. Não há fallback para conta administrativa ou Docker.");
        }
        usuario = usuario.trim();
        if (Set.of("root", "mysql.sys", "mysql.session", "mysql.infoschema").contains(usuario.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Integrações exigem uma conta própria restrita ao schema de validação.");
        }
        validarUrlSemCredenciais(url);
        return new ConfiguracaoMysqlIntegracao(false, url, usuario, senha);
    }

    private static String valor(Properties propriedades, Map<String, String> ambiente, String nome) {
        // Uma propriedade explícita vazia é um erro de configuração, não um pedido
        // para recorrer silenciosamente a outra identidade do ambiente.
        String propriedade = propriedades.getProperty("integration.mysql." + nome);
        return propriedade != null ? propriedade : ambiente.get("INTEGRATION_MYSQL_" + nome.toUpperCase(Locale.ROOT));
    }

    private static void validarUrlSemCredenciais(String url) {
        if (!url.startsWith("jdbc:mysql://")) {
            throw new IllegalArgumentException("A integração requer uma URL JDBC MySQL com schema explícito.");
        }
        final URI uri;
        try {
            uri = URI.create(url.substring("jdbc:".length()));
        } catch (IllegalArgumentException erro) {
            throw new IllegalArgumentException("URL JDBC MySQL inválida; configure endpoint e schema separadamente das credenciais.");
        }
        if (uri.getHost() == null || uri.getUserInfo() != null || uri.getPath() == null
                || !uri.getPath().matches("/[A-Za-z0-9_]+") || uri.getFragment() != null) {
            throw new IllegalArgumentException("A URL JDBC deve identificar um servidor/schema, sem usuário ou senha embutidos.");
        }
        if (uri.getRawQuery() == null) return;
        for (String parametro : uri.getRawQuery().split("&")) {
            String chave = URLDecoder.decode(parametro.split("=", 2)[0], StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
            if (chave.equals("user") || chave.startsWith("password")) {
                throw new IllegalArgumentException("Usuário e senha da integração devem vir dos campos explícitos, não da URL.");
            }
        }
    }

    public boolean usaTestcontainers() { return testcontainers; }
    public String url() { return url; }
    public String usuario() { return usuario; }
    public String senha() { return senha; }
}
