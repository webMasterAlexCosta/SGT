package com.example.siggtrat.integration;

import com.example.siggtrat.entidade.TokenAtivo;
import com.example.siggtrat.repositorio.TokenAtivoRepositorio;
import org.junit.jupiter.api.Test;
import com.example.siggtrat.configuracao.transacao.ExecutorTransacaoIdempotente;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.concurrent.atomic.AtomicInteger;
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

/** PostgreSQL real: duas leituras da mesma sessão precedem revogações concorrentes. */
class TokenRevogacaoConcorrenciaH02IT extends PostgresIntegrationTestSupport {
    @Autowired TokenAtivoRepositorio repositorio;
    @Autowired PlatformTransactionManager transacoes;
    @Autowired DataSource dataSource;

    @Test
    void duasRevogacoesDoMesmoTokenSaoIdempotentesEPreservamOutraSessao() throws Exception {
        try (var connection = dataSource.getConnection()) {
            assertEquals("PostgreSQL", connection.getMetaData().getDatabaseProductName());
            com.example.siggtrat.configuracao.VerificadorAmbientePostgres186.verificar(connection, "revogacao-concorrente");
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
        var totalTentativas = new AtomicInteger();
        var idsTransacoes = ConcurrentHashMap.<Long>newKeySet();
        var provasCriadas = ConcurrentHashMap.<String>newKeySet();
        var provasConfirmadas = ConcurrentHashMap.<String>newKeySet();
        try {
            Callable<Void> revogar = () -> {
                // PostgreSQL exige repetir TODA a transação abortada; o DELETE isolado
                // não é serialization-safe. Mesma fronteira usada pela revogação real.
                var tentativasAtor = new AtomicInteger();
                String ator = UUID.randomUUID().toString();
                new ExecutorTransacaoIdempotente(transacoes).executar(TransactionDefinition.ISOLATION_REPEATABLE_READ, () -> {
                    int tentativa = tentativasAtor.incrementAndGet();
                    totalTentativas.incrementAndGet();
                    var jdbc = new JdbcTemplate(dataSource);
                    assertEquals("repeatable read", jdbc.queryForObject("show transaction_isolation", String.class));
                    assertTrue(idsTransacoes.add(jdbc.queryForObject("select txid_current()", Long.class)), "Cada tentativa exige nova transação");
                    if (tentativa == 1) {
                        assertEquals(alvo.getId(), repositorio.buscarPorTokenBruto(alvoBruto).orElseThrow().getId());
                        ambasLeram.countDown();
                        try { assertTrue(liberarRevogacao.await(10, TimeUnit.SECONDS)); }
                        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new AssertionError(e); }
                    } else {
                        assertFalse(repositorio.existePorTokenBruto(alvoBruto), "Nova transação relê o estado confirmado pelo concorrente");
                    }
                    String prova = id + "-prova-" + ator + "-" + tentativa;
                    provasCriadas.add(prova);
                    repositorio.saveAndFlush(TokenAtivo.deTokenBruto(prova, id, agora, agora.plusHours(1)));
                    repositorio.excluirPorTokenBruto(alvoBruto);
                    return prova;
                });
                provasConfirmadas.add(id + "-prova-" + ator + "-" + tentativasAtor.get());
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
            assertEquals(3, totalTentativas.get(), "Duas transações iniciais mais uma repetição integral");
            assertEquals(3, idsTransacoes.size());
            assertEquals(2, provasConfirmadas.size());
            for (String prova : provasCriadas) assertEquals(provasConfirmadas.contains(prova), repositorio.existePorTokenBruto(prova), "Escritas da tentativa abortada devem reverter");
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
            provasCriadas.forEach(repositorio::excluirPorTokenBruto);
        }
    }
}
