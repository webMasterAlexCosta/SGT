package com.example.siggtrat.integration;

import com.example.siggtrat.entidade.TokenAtivo;
import com.example.siggtrat.repositorio.TokenAtivoRepositorio;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/** MySQL real: duas leituras da mesma sessão precedem revogações concorrentes. */
class TokenRevogacaoConcorrenciaH02IT extends MySqlIntegrationTestSupport {
    @Autowired TokenAtivoRepositorio repositorio;
    @Autowired PlatformTransactionManager transacoes;
    @Autowired DataSource dataSource;

    @Test
    void duasRevogacoesDoMesmoTokenSaoIdempotentesEPreservamOutraSessao() throws Exception {
        try (var connection = dataSource.getConnection()) {
            assertEquals("MySQL", connection.getMetaData().getDatabaseProductName());
            assertEquals("8.0.45", connection.getMetaData().getDatabaseProductVersion());
        }
        String id = "h02-idle-" + UUID.randomUUID();
        String alvoBruto = id + "-alvo-sintetico-sem-jwt";
        String controleBruto = id + "-controle-sintetico-sem-jwt";
        LocalDateTime agora = LocalDateTime.of(2030, 1, 1, 12, 0);
        TokenAtivo alvo = repositorio.saveAndFlush(TokenAtivo.deTokenBruto(alvoBruto, id, agora, agora.plusHours(1)));
        TokenAtivo controle = repositorio.saveAndFlush(TokenAtivo.deTokenBruto(controleBruto, id, agora, agora.plusHours(1)));
        var pool = Executors.newFixedThreadPool(2);
        CountDownLatch ambasLeram = new CountDownLatch(2);
        CountDownLatch liberarRevogacao = new CountDownLatch(1);
        List<Throwable> falhas = new ArrayList<>();
        try {
            Callable<Void> revogar = () -> {
                TransactionTemplate tx = new TransactionTemplate(transacoes);
                tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                tx.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
                tx.executeWithoutResult(status -> {
                    assertEquals(alvo.getId(), repositorio.buscarPorTokenBruto(alvoBruto).orElseThrow().getId());
                    ambasLeram.countDown();
                    try { assertTrue(liberarRevogacao.await(10, TimeUnit.SECONDS)); }
                    catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new AssertionError(e); }
                    repositorio.excluirPorTokenBruto(alvoBruto);
                });
                return null;
            };
            List<Future<Void>> resultados = List.of(pool.submit(revogar), pool.submit(revogar));
            assertTrue(ambasLeram.await(10, TimeUnit.SECONDS), "Ambas as transações devem ler a sessão antes da revogação");
            liberarRevogacao.countDown();
            for (Future<Void> resultado : resultados) {
                try { resultado.get(20, TimeUnit.SECONDS); }
                catch (ExecutionException e) { falhas.add(e.getCause()); }
            }
            assertTrue(falhas.isEmpty(), () -> "Revogação repetida não pode perder a causa da expiração por conflito JPA: " + falhas);
            assertFalse(repositorio.existePorTokenBruto(alvoBruto));
            repositorio.excluirPorTokenBruto(alvoBruto); // replay posterior também é inócuo
            TokenAtivo preservado = repositorio.findById(controle.getId()).orElseThrow();
            assertEquals(controle.getTokenHash(), preservado.getTokenHash());
            assertEquals(controle.getCreatedAt(), preservado.getCreatedAt());
            assertEquals(controle.getExpireAt(), preservado.getExpireAt());
            assertEquals(controle.getVersion(), preservado.getVersion());
        } finally {
            liberarRevogacao.countDown();
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
            // Somente as duas sessões sintéticas deste teste; nenhum dado compartilhado.
            repositorio.excluirPorTokenBruto(alvoBruto);
            repositorio.excluirPorTokenBruto(controleBruto);
        }
    }
}
