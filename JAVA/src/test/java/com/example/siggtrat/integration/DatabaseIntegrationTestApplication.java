package com.example.siggtrat.integration;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Contexto mínimo para testes de persistência.
 *
 * <p>Carrega apenas JPA/JDBC + entidades + repositories. Não executa os
 * CommandLineRunner de carga inicial da aplicação e não depende de Redis/web.</p>
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan(basePackages = "com.example.siggtrat.entidade")
@EnableJpaRepositories(basePackages = "com.example.siggtrat.repositorio")
@ComponentScan(basePackages = "com.example.siggtrat.repositorio")
@org.springframework.context.annotation.Import({
        com.example.siggtrat.servico.MotorTemporalContratoServico.class,
        com.example.siggtrat.servico.ComposicaoContratualTemporalServico.class
})
class DatabaseIntegrationTestApplication {
}
