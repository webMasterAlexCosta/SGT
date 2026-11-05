package com.example.siggtrat.configuracao;

import java.sql.SQLException;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Verificação opcional da campanha, executada na DataSource do próprio JAR
 * antes de ela chegar ao Hibernate. Não cria schema, dados ou endpoint.
 */
@Component
@ConditionalOnProperty(name = "app.homologacao.mysql8045", havingValue = "true")
public final class VerificadorAmbienteMysql8045 implements BeanPostProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(VerificadorAmbienteMysql8045.class);

    public static void exigirVersao(String produto, String metadata, String servidor) {
        if (!"MySQL".equals(produto) || !"8.0.45".equals(metadata) || !"8.0.45".equals(servidor)) {
            throw new IllegalStateException("Campanha exige MySQL Server exatamente 8.0.45; "
                    + "produto/metadata/SELECT VERSION() divergentes. Execução recusada.");
        }
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (bean instanceof DataSource dataSource) {
            try (var conexao = dataSource.getConnection(); var comando = conexao.createStatement();
                    var resultado = comando.executeQuery(
                            "SELECT VERSION(), @@server_uuid, @@port, CURRENT_USER(), DATABASE(), CONNECTION_ID()")) {
                if (!resultado.next()) throw new IllegalStateException("MySQL não retornou a identidade do servidor.");
                var metadata = conexao.getMetaData();
                exigirVersao(metadata.getDatabaseProductName(), metadata.getDatabaseProductVersion(), resultado.getString(1));
                String conta = resultado.getString(4).split("@", 2)[0];
                if (java.util.Set.of("root", "mysql.sys", "mysql.session", "mysql.infoschema")
                        .contains(conta.toLowerCase(java.util.Locale.ROOT))) {
                    throw new IllegalStateException("Campanha exige conta MySQL própria e restrita.");
                }
                // Não registrar URL, senha, propriedades da conexão ou tokens.
                LOG.info("SGT_MYSQL8045_DATASOURCE bean={} product={} metadataVersion={} serverVersion={} "
                                + "serverUuid={} serverPort={} currentUser={} catalog={} connectionId={} driverVersion={}",
                        beanName, metadata.getDatabaseProductName(), metadata.getDatabaseProductVersion(),
                        resultado.getString(1), resultado.getString(2), resultado.getInt(3), resultado.getString(4),
                        resultado.getString(5), resultado.getLong(6), metadata.getDriverVersion());
            } catch (SQLException erro) {
                throw new IllegalStateException("Não foi possível verificar a DataSource MySQL real antes do schema.", erro);
            }
        }
        return bean;
    }
}
