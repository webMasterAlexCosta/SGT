package com.example.siggtrat.configuracao.demonstracao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;

@Configuration(proxyBeanMethods=false)
@ConditionalOnProperty(name="app.demo-rj.enabled",havingValue="true")
public class ConfiguracaoDemonstracaoRj {
    private static final Logger LOG=LoggerFactory.getLogger(ConfiguracaoDemonstracaoRj.class);
    @Bean static BeanFactoryPostProcessor conferirBancoDemoAntesDoHibernate(Environment env) {
        return factory->{
            if(!"true".equalsIgnoreCase(env.getProperty("APP_DEMO_RJ_ENABLED"))
                    || !env.getRequiredProperty("spring.datasource.url").equals(env.getProperty("APP_DEMO_RJ_DB_URL")))
                throw new IllegalStateException("SGT_DEMO_RJ_SELECAO_OBRIGATORIA");
            try(var c=DriverManager.getConnection(env.getRequiredProperty("spring.datasource.url"),
                    env.getRequiredProperty("spring.datasource.username"),env.getRequiredProperty("spring.datasource.password"))) {
                c.setReadOnly(true);
                try(var s=c.createStatement();var r=s.executeQuery("""
                    SELECT NOT r.rolsuper AND NOT r.rolcreaterole AND NOT r.rolcreatedb AND d.datdba=r.oid
                      AND has_schema_privilege(current_user,current_schema(),'CREATE')
                      AND NOT EXISTS(SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
                           WHERE n.nspname=current_schema() AND c.relkind IN ('r','p','S') AND c.relowner<>r.oid)
                      FROM pg_database d JOIN pg_roles r ON r.rolname=current_user
                     WHERE d.datname=current_database()
                    """)) {
                    if(!r.next() || !r.getBoolean(1))throw new IllegalStateException("SGT_DEMO_RJ_OWNERSHIP_REQUIRED");
                }
            } catch(SQLException ex) {
                LOG.error("SGT_DEMO_RJ_DATABASE_CHECK_FAILED sqlState={} tipo={}",ex.getSQLState(),ex.getClass().getSimpleName());
                throw new IllegalStateException("SGT_DEMO_RJ_DATABASE_CHECK_FAILED: confira o banco isolado e as credenciais");
            }
        };
    }
    @Bean @Order(100) CommandLineRunner popularDemonstracaoRj(PopuladorDadosRj populador,Environment env) {
        return args->populador.popular(LocalDate.now(ZoneId.of("America/Sao_Paulo")),env.getProperty("APP_DEMO_RJ_INITIAL_PASSWORD",""));
    }
}
