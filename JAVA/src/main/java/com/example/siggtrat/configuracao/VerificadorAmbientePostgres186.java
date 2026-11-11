package com.example.siggtrat.configuracao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Set;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

/** Verifica a conexão real antes de disponibilizar a DataSource ao Hibernate. */
@Component
public final class VerificadorAmbientePostgres186 implements BeanPostProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(VerificadorAmbientePostgres186.class);
    public static final int VERSAO_NUMERICA = 180006;
    private static final String VERSAO = "18\\.6(?: \\((?:Debian|Ubuntu|Red Hat|RPM)[^()]*\\))?";

    /** Política conservadora: nenhuma associação administrativa, mesmo sem INHERIT/SET.
     * pg_database_owner governa apenas a base própria e não é uma role proibida.
     * Não certifica permissões arbitrárias de funções SECURITY DEFINER/extensões.
     */
    public static final String CONSULTA_IDENTIDADE = """
            WITH RECURSIVE roles_associadas(oid) AS (
                SELECT oid FROM pg_catalog.pg_roles WHERE rolname = current_user
                UNION
                SELECT m.roleid FROM pg_catalog.pg_auth_members m
                JOIN roles_associadas a ON m.member = a.oid
            )
            SELECT version(), current_setting('server_version_num')::integer,
                   current_database(), current_user, session_user, current_schema(),
                   inet_server_addr()::text, inet_server_port(), pg_backend_pid(),
                   rolsuper, rolcreatedb, rolcreaterole, rolreplication, rolbypassrls,
                   EXISTS (SELECT 1 FROM roles_associadas a JOIN pg_catalog.pg_roles r ON r.oid=a.oid
                           WHERE r.rolsuper OR r.rolcreatedb OR r.rolcreaterole OR r.rolreplication OR r.rolbypassrls
                              OR r.rolname IN ('pg_checkpoint', 'pg_create_subscription', 'pg_maintain',
                                              'pg_monitor', 'pg_read_all_settings', 'pg_read_all_stats', 'pg_stat_scan_tables',
                                              'pg_read_all_data', 'pg_write_all_data', 'pg_read_server_files',
                                              'pg_write_server_files', 'pg_execute_server_program',
                                              'pg_signal_autovacuum_worker', 'pg_signal_backend', 'pg_use_reserved_connections'))
              FROM pg_catalog.pg_roles WHERE rolname = current_user
            """;

    public static void exigirVersao(String produto, String metadata, String servidor, int numero) {
        if (!"PostgreSQL".equals(produto) || numero != VERSAO_NUMERICA || metadata == null
                || !metadata.matches(VERSAO) || servidor == null
                || !servidor.matches("PostgreSQL " + VERSAO + " on .+")) {
            throw new IllegalStateException("Execução exige PostgreSQL Server exatamente 18.6; "
                    + "produto/metadata/version()/server_version_num divergentes. Execução recusada.");
        }
    }

    public static void verificar(Connection conexao, String beanName) throws SQLException {
        try (var comando = conexao.createStatement(); var r = comando.executeQuery(CONSULTA_IDENTIDADE)) {
            if (!r.next()) throw new IllegalStateException("PostgreSQL não retornou a identidade da conexão.");
            var md = conexao.getMetaData();
            exigirVersao(md.getDatabaseProductName(), md.getDatabaseProductVersion(), r.getString(1), r.getInt(2));
            String conta = r.getString(4);
            if (Set.of("postgres", "root", "rdsadmin", "azure_pg_admin", "cloudsqladmin")
                    .contains(conta.toLowerCase(Locale.ROOT)) || !conta.equals(r.getString(5))
                    || !conta.equals(md.getUserName())) {
                throw new IllegalStateException("Execução exige identidade PostgreSQL explícita, própria e limitada.");
            }
            for (int i = 10; i <= 14; i++) {
                if (r.getBoolean(i)) throw new IllegalStateException("Role PostgreSQL possui privilégios administrativos; execução recusada.");
            }
            if (r.getBoolean(15)) {
                throw new IllegalStateException("Role PostgreSQL associada direta ou indiretamente a capacidades administrativas; execução recusada.");
            }
            // Os textos completos de versão são preservados; nunca registrar URL/credenciais/payloads.
            LOG.info("SGT_POSTGRES186_DATASOURCE bean={} product={} metadataVersion={} serverVersion={} "
                            + "serverVersionNum={} catalog={} currentUser={} schema={} serverAddress={} serverPort={} "
                            + "backendPid={} driverVersion={}", beanName, md.getDatabaseProductName(),
                    md.getDatabaseProductVersion(), r.getString(1), r.getInt(2), r.getString(3), conta,
                    r.getString(6), r.getString(7), r.getInt(8), r.getLong(9), md.getDriverVersion());
        }
    }

    @Override public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (bean instanceof DataSource dataSource) {
            try (var conexao = dataSource.getConnection()) { verificar(conexao, beanName); }
            catch (SQLException erro) {
                throw new IllegalStateException("Não foi possível verificar a DataSource PostgreSQL real antes do schema.", erro);
            }
        }
        return bean;
    }
}
