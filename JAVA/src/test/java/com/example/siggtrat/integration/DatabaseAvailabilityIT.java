package com.example.siggtrat.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Integração PostgreSQL: disponibilidade e pool de conexões")
class DatabaseAvailabilityIT extends PostgresIntegrationTestSupport {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("pool atende rajada concorrente sem perder operações")
    void poolAtendeRajadaConcorrente() throws Exception {
        int total = 240;
        int workers = 24;
        ExecutorService pool = Executors.newFixedThreadPool(workers);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(total);
        AtomicInteger ok = new AtomicInteger();
        List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());
        List<Long> latenciesMs = Collections.synchronizedList(new ArrayList<>());

        try {
            for (int i = 0; i < total; i++) {
                pool.submit(() -> {
                    try {
                        start.await(5, TimeUnit.SECONDS);
                        long begin = System.nanoTime();
                        Integer one = jdbc.queryForObject("SELECT 1", Integer.class);
                        latenciesMs.add(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - begin));
                        if (Integer.valueOf(1).equals(one)) ok.incrementAndGet();
                    } catch (Throwable ex) {
                        failures.add(ex);
                    } finally {
                        done.countDown();
                    }
                });
            }

            start.countDown();
            assertTrue(done.await(20, TimeUnit.SECONDS), "A rajada não terminou dentro de 20s.");
            assertTrue(failures.isEmpty(), () -> "Falhas de conexão: " + failures);
            assertEquals(total, ok.get());

            List<Long> sorted = new ArrayList<>(latenciesMs);
            Collections.sort(sorted);
            long p95 = percentile(sorted, 0.95);
            assertTrue(p95 <= 5_000, () -> "P95 excessivo na rajada: " + p95 + "ms");
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @DisplayName("erro SQL em uma operação não deixa o pool indisponível")
    void poolSeRecuperaAposErroSql() {
        assertThrows(DataAccessException.class,
                () -> jdbc.queryForObject("SELECT coluna_que_nao_existe FROM tabela_que_nao_existe", Integer.class));

        assertEquals(1, jdbc.queryForObject("SELECT 1", Integer.class));
    }

    @Test
    @DisplayName("conexões devolvidas ao pool podem ser reutilizadas muito além do tamanho máximo")
    void conexoesSaoReutilizadasSemVazamento() throws Exception {
        for (int i = 0; i < 200; i++) {
            try (Connection connection = dataSource.getConnection()) {
                assertFalse(connection.isClosed());
                try (var statement = connection.prepareStatement("SELECT 1");
                     var rs = statement.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals(1, rs.getInt(1));
                }
            }
        }

        assertEquals(1, jdbc.queryForObject("SELECT 1", Integer.class));
    }

    private static long percentile(List<Long> sorted, double p) {
        if (sorted.isEmpty()) return Long.MAX_VALUE;
        int index = (int) Math.ceil(sorted.size() * p) - 1;
        return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1)));
    }
}
