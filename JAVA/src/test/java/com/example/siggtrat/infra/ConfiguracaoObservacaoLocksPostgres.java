package com.example.siggtrat.infra;

import jakarta.persistence.EntityManager;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.hibernate.Session;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Não integra o JAR. Sem ator ativo, não faz consulta ou altera o fluxo. */
@TestConfiguration(proxyBeanMethods = false)
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class ConfiguracaoObservacaoLocksPostgres {
    @Bean static org.springframework.beans.factory.config.BeanPostProcessor capturaJdbcAtorPostgres() {return new CapturaJdbcAtorPostgres();}
    @Bean AspectoAtorPostgres aspectoAtorPostgres(EntityManager em) { return new AspectoAtorPostgres(em); }
    @Aspect
    public static class AspectoAtorPostgres {
        private final EntityManager em;
        public AspectoAtorPostgres(EntityManager em) { this.em = em; }
        @Around("execution(* com.example.siggtrat.repositorio..*(..)) || execution(* com.example.siggtrat.servico.SuporteProvaHistoricaServico.contrato(..))")
        public Object observar(ProceedingJoinPoint p) throws Throwable {
            var ator = ObservadorAtorPostgres.ATOR.get();
            if (ator == null || !TransactionSynchronizationManager.isActualTransactionActive()) return p.proceed();
            // JDBC work não provoca flush JPA; não escreve configuração na sessão/pool.
            long pid = em.unwrap(Session.class).doReturningWork(c -> {
                try (var s=c.createStatement(); var r=s.executeQuery("SELECT pg_backend_pid()")) {
                    if (!r.next()) throw new java.sql.SQLException("PID ausente na fixture");
                    return r.getLong(1);
                }
            });
            var assinatura = (MethodSignature) p.getSignature();
            var anterior = ator.atual;
            ator.atual = new ObservadorAtorPostgres.Operacao(pid,
                    assinatura.getDeclaringType().getSimpleName() + "." + assinatura.getName(),
                    ObservadorAtorPostgres.chaves(p.getArgs()));
            try { return p.proceed(); } finally { ator.atual = anterior; }
        }
    }
}
