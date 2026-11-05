package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.infra.ConfiguracaoMysqlIntegracao;
import com.example.siggtrat.infra.GuardaMysql8045;
import java.net.URI;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.env.Environment;

/**
 * Guarda exclusiva dos suplementos portados. Não cria schema ou fixture.
 * Confere a conexão efetiva antes do JPA e novamente antes de cada caso.
 * Credenciais só são lidas da configuração autorizada; nunca são registradas.
 */
public final class Suplementos8045Ambiente implements BeanPostProcessor {
    private static final String USUARIO = "sgt_campaign";

    private static ConfiguracaoMysqlIntegracao configuracao() {
        if (!"1".equals(System.getenv("SGT_SUPPLEMENTAL_EXECUTE"))) {
            throw new IllegalStateException("Suplementos exigem SGT_SUPPLEMENTAL_EXECUTE=1 após autorização do coordenador.");
        }
        String modo = System.getProperty("integration.mysql.mode", System.getenv("INTEGRATION_MYSQL_MODE"));
        if (!"native".equals(modo)) {
            throw new IllegalStateException("Suplementos exigem a seleção explícita do MySQL nativo da campanha.");
        }
        serverUuid(); // Identidade esperada obrigatória antes de abrir qualquer conexão.
        var cfg = ConfiguracaoMysqlIntegracao.carregar();
        if (cfg.usaTestcontainers()) throw new IllegalStateException("Suplementos não admitem Testcontainers.");
        var uri = URI.create(cfg.url().substring("jdbc:".length()));
        if (!USUARIO.equals(cfg.usuario()) || !"127.0.0.1".equals(uri.getHost())
                || uri.getPort() != 33075 || !uri.getPath().matches("/sgt_audit_8045_[a-z0-9_]+")) {
            throw new IllegalStateException("Identidade JDBC fora do endpoint, conta e prefixo de schema autorizados.");
        }
        return cfg;
    }

    static String schema() {
        return URI.create(configuracao().url().substring("jdbc:".length())).getPath().substring(1);
    }

    static String usuario() { configuracao(); return USUARIO; }

    private static String ambienteExplicito(String nome) {
        String valor = System.getenv(nome);
        if (valor == null || (!nome.equals("REDIS_PASSWORD") && valor.isBlank())) {
            throw new IllegalStateException("Informe explicitamente " + nome + " para os suplementos.");
        }
        return valor;
    }

    private static String serverUuid() {
        String valor = ambienteExplicito("SGT_MYSQL_SERVER_UUID");
        if (!valor.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) {
            throw new IllegalStateException("SGT_MYSQL_SERVER_UUID deve ser um UUID explícito válido.");
        }
        return java.util.UUID.fromString(valor).toString();
    }

    static int redisDatabase() {
        String valor = ambienteExplicito("REDIS_DATABASE");
        if (!valor.matches("[0-9]+")) throw new IllegalStateException("REDIS_DATABASE deve ser um índice explícito válido.");
        try { return Integer.parseInt(valor); }
        catch (NumberFormatException erro) { throw new IllegalStateException("REDIS_DATABASE fora do intervalo de índice."); }
    }

    static void confirmar(Environment ambiente, DataSource dataSource) {
        var cfg = configuracao();
        if (!cfg.url().equals(ambiente.getProperty("spring.datasource.url"))
                || !cfg.usuario().equals(ambiente.getProperty("spring.datasource.username"))
                || !"update".equals(ambiente.getProperty("spring.jpa.hibernate.ddl-auto"))
                || !"never".equals(ambiente.getProperty("spring.sql.init.mode"))) {
            throw new IllegalStateException("Configuração efetiva do JPA/DataSource difere da campanha declarada.");
        }
        for (String parte : new String[]{"host", "port", "password", "database"}) {
            String esperado = ambienteExplicito("REDIS_" + parte.toUpperCase(java.util.Locale.ROOT));
            if (!esperado.equals(ambiente.getProperty("spring.data.redis." + parte))) {
                throw new IllegalStateException("Configuração efetiva do Redis difere do ambiente explícito: " + parte);
            }
        }
        redisDatabase();
        conferirConexao(dataSource);
    }

    private static void conferirConexao(DataSource dataSource) {
        var cfg = configuracao();
        if (dataSource == null) throw new IllegalStateException("DataSource real ausente nos suplementos.");
        try (var conexao = dataSource.getConnection(); var stmt = conexao.createStatement();
                var resultado = stmt.executeQuery("SELECT VERSION(), DATABASE(), CURRENT_USER(), @@server_uuid")) {
            if (!resultado.next()) throw new IllegalStateException("Servidor não retornou identidade para os suplementos.");
            GuardaMysql8045.exigirVersao(conexao.getMetaData().getDatabaseProductName(),
                    conexao.getMetaData().getDatabaseProductVersion(), resultado.getString(1));
            if (!schema().equals(resultado.getString(2))
                    || !cfg.usuario().equals(resultado.getString(3).split("@", 2)[0])
                    || !serverUuid().equals(resultado.getString(4))) {
                throw new IllegalStateException("Conexão efetiva fora do schema, conta ou servidor autorizado.");
            }
        } catch (SQLException erro) {
            // Mensagem deliberadamente sem URL, usuário observado ou erro que possa conter credenciais.
            throw new IllegalStateException("Não foi possível conferir a identidade MySQL efetiva dos suplementos.");
        }
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof DataSource dataSource) conferirConexao(dataSource);
        return bean;
    }
}
