package com.example.siggtrat.configuracao;

import com.example.siggtrat.servico.InvalidacaoCacheServico;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;

@Aspect
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class AspectoInvalidacaoCacheMutacaoApi {

    private final InvalidacaoCacheServico cacheInvalidationServico;
    private final PlatformTransactionManager transactionManager;

    public AspectoInvalidacaoCacheMutacaoApi(InvalidacaoCacheServico cacheInvalidationServico,
                                           PlatformTransactionManager transactionManager) {
        this.cacheInvalidationServico = cacheInvalidationServico;
        this.transactionManager = transactionManager;
    }

    @Pointcut("@within(com.example.siggtrat.configuracao.InvalidaCacheNegocio) && ("
            + "@annotation(org.springframework.web.bind.annotation.PostMapping) || "
            + "@annotation(org.springframework.web.bind.annotation.PutMapping) || "
            + "@annotation(org.springframework.web.bind.annotation.PatchMapping) || "
            + "@annotation(org.springframework.web.bind.annotation.DeleteMapping))")
    public void operacaoDeEscritaDaApi() {
    }

    @Around("operacaoDeEscritaDaApi()")
    public Object executarComInvalidacaoAtomica(ProceedingJoinPoint operacao) throws Throwable {
        var definicao = new DefaultTransactionDefinition();
        definicao.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        var transacao = transactionManager.getTransaction(definicao);
        try {
            Object resultado = operacao.proceed();
            // Os serviços REQUIRED e a revisão são confirmados juntos. Apenas
            // a limpeza física do Redis ocorre depois do commit.
            cacheInvalidationServico.invalidarCachesDeNegocioAposCommit();
            transactionManager.commit(transacao);
            return resultado;
        } catch (Throwable erro) {
            if (!transacao.isCompleted()) transactionManager.rollback(transacao);
            throw erro;
        }
    }
}
