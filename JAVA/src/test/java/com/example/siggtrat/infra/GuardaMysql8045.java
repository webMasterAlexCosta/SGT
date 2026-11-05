package com.example.siggtrat.infra;

import java.sql.SQLException;
import javax.sql.DataSource;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * Guarda de infraestrutura da campanha: inspeciona a DataSource real antes
 * de a entidade gerenciadora inicializar o schema. Não cria tabelas ou dados.
 * Nenhum resultado funcional obtido em outra versão pode entrar na campanha.
 */
public final class GuardaMysql8045 implements BeanPostProcessor {
    public static final String VERSAO_AUTORIZADA = "8.0.45";

    public static void exigirVersao(String produto, String versaoMetadata, String versaoServidor) {
        com.example.siggtrat.configuracao.VerificadorAmbienteMysql8045.exigirVersao(
                produto, versaoMetadata, versaoServidor);
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof DataSource dataSource) {
            try (var connection = dataSource.getConnection(); var statement = connection.createStatement();
                    var result = statement.executeQuery("SELECT VERSION(), CURRENT_USER()")) {
                if (!result.next()) throw new IllegalStateException("MySQL não retornou identidade do servidor.");
                exigirVersao(connection.getMetaData().getDatabaseProductName(),
                        connection.getMetaData().getDatabaseProductVersion(), result.getString(1));
                String conta = result.getString(2).split("@", 2)[0];
                if (java.util.Set.of("root", "mysql.sys", "mysql.session", "mysql.infoschema").contains(conta.toLowerCase(java.util.Locale.ROOT))) {
                    throw new IllegalStateException("Campanha exige conta MySQL própria e restrita.");
                }
            } catch (SQLException erro) {
                throw new IllegalStateException("Não foi possível verificar a DataSource MySQL real antes do schema.", erro);
            }
        }
        return bean;
    }
}
