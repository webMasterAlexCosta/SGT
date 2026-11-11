package com.example.siggtrat.configuracao.transacao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.sql.SQLException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Repete somente callbacks idempotentes, sem I/O externo, após o fim da transação
 * inteira. Uma transação já existente pertence ao chamador e nunca é repetida,
 * confirmada ou suspensa aqui. Não usar para operações de negócio arbitrárias.
 */
@Component
public class ExecutorTransacaoIdempotente {
    private static final Logger log = LoggerFactory.getLogger(ExecutorTransacaoIdempotente.class);
    private static final int MAX_TENTATIVAS = 3;
    private final PlatformTransactionManager transacoes;

    public ExecutorTransacaoIdempotente(PlatformTransactionManager transacoes) {
        this.transacoes = transacoes;
    }

    public <T> T executar(Supplier<T> operacao) {
        return executar(TransactionDefinition.ISOLATION_DEFAULT, operacao);
    }

    public <T> T executar(int isolamento, Supplier<T> operacao) {
        Objects.requireNonNull(operacao, "operacao");
        if (TransactionSynchronizationManager.isActualTransactionActive()) return operacao.get();
        TransactionTemplate template = new TransactionTemplate(transacoes);
        template.setIsolationLevel(isolamento);
        // REQUIRED: a transação é nossa apenas porque não existe uma anterior.
        for (int tentativa = 1; ; tentativa++) {
            try {
                return template.execute(status -> operacao.get());
            } catch (RuntimeException erro) {
                String estado = estadoRepetivel(erro);
                if (estado == null || tentativa >= MAX_TENTATIVAS || Thread.currentThread().isInterrupted()) throw erro;
                // TransactionTemplate já concluiu rollback/commit antes deste catch.
                // Não registrar argumentos, tokens, mensagens SQL ou payloads.
                log.warn("Transação idempotente reiniciada: sqlState={} tentativa={} limite={}", estado, tentativa, MAX_TENTATIVAS);
            }
        }
    }

    private static String estadoRepetivel(Throwable erro) {
        Set<Throwable> visitados = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable causa = erro; causa != null && visitados.add(causa); causa = causa.getCause()) {
            if (causa instanceof SQLException sql && ("40001".equals(sql.getSQLState()) || "40P01".equals(sql.getSQLState()))) {
                return sql.getSQLState();
            }
        }
        return null;
    }
}
