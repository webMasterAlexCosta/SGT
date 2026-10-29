package com.example.siggtrat.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

@Tag("load")
@DisplayName("Carga MySQL: leitura/escrita concorrente e integridade")
class DatabaseLoadIT extends MySqlIntegrationTestSupport {

    @Autowired
    private JdbcTemplate jdbc;

    private final String prefix = "dbload-" + Long.toUnsignedString(System.nanoTime());

    @AfterEach
    void cleanup() {
        jdbc.update("DELETE FROM configuracao_sistema WHERE chave LIKE ?", prefix + "%");
    }

    @Test
    @DisplayName("carga mista mantém disponibilidade, latência e contadores íntegros")
    void mixedReadWriteLoad() throws Exception {
        int users = intProperty("dbload.users", 24, 1, 256);
        int durationSeconds = intProperty("dbload.durationSeconds", 15, 1, 900);
        int keys = intProperty("dbload.keys", 50, 2, 5_000);
        double writePercent = doubleProperty("dbload.writePercent", 20.0, 0.0, 100.0);
        long maxP95Ms = longProperty("dbload.maxP95Ms", 2_000L, 1L, 120_000L);
        double maxErrorRatePct = doubleProperty("dbload.maxErrorRatePct", 0.5, 0.0, 100.0);
        double minRps = doubleProperty("dbload.minRps", 10.0, 0.0, 1_000_000.0);

        for (int i = 0; i < keys; i++) {
            jdbc.update("INSERT INTO configuracao_sistema (chave, valor, descricao) VALUES (?, '0', 'db load')",
                    key(i));
        }

        // Aquecimento curto: força criação do pool, prepared statements e metadados antes da janela medida.
        for (int i = 0; i < Math.min(100, keys * 2); i++) {
            jdbc.queryForObject("SELECT valor FROM configuracao_sistema WHERE chave = ?", String.class, key(i % keys));
        }

        ExecutorService pool = Executors.newFixedThreadPool(users);
        CountDownLatch ready = new CountDownLatch(users);
        CountDownLatch start = new CountDownLatch(1);
        AtomicLong operations = new AtomicLong();
        AtomicLong reads = new AtomicLong();
        AtomicLong writes = new AtomicLong();
        AtomicLong failures = new AtomicLong();
        ConcurrentLinkedQueue<Long> latenciesNanos = new ConcurrentLinkedQueue<>();

        long durationNanos = TimeUnit.SECONDS.toNanos(durationSeconds);
        List<Future<?>> futures = new ArrayList<>();
        for (int user = 0; user < users; user++) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    long deadline = System.nanoTime() + durationNanos;
                    while (System.nanoTime() < deadline) {
                        int index = ThreadLocalRandom.current().nextInt(keys);
                        boolean write = ThreadLocalRandom.current().nextDouble(100.0) < writePercent;
                        long begin = System.nanoTime();
                        try {
                            if (write) {
                                int affected = jdbc.update("""
                                        UPDATE configuracao_sistema
                                           SET valor = CAST(CAST(valor AS UNSIGNED) + 1 AS CHAR)
                                         WHERE chave = ?
                                        """, key(index));
                                if (affected != 1) throw new IllegalStateException("UPDATE afetou " + affected + " linhas");
                                writes.incrementAndGet();
                            } else {
                                String value = jdbc.queryForObject(
                                        "SELECT valor FROM configuracao_sistema WHERE chave = ?",
                                        String.class, key(index));
                                if (value == null) throw new IllegalStateException("Leitura retornou null");
                                reads.incrementAndGet();
                            }
                        } catch (Throwable ex) {
                            failures.incrementAndGet();
                        } finally {
                            latenciesNanos.add(System.nanoTime() - begin);
                            operations.incrementAndGet();
                        }
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(ex);
                }
            }));
        }

        assertTrue(ready.await(15, TimeUnit.SECONDS), "Workers de carga não ficaram prontos.");
        long measuredStart = System.nanoTime();
        start.countDown();

        try {
            for (Future<?> future : futures) future.get(durationSeconds + 30L, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }
        double elapsedSeconds = (System.nanoTime() - measuredStart) / 1_000_000_000.0;

        List<Long> sorted = new ArrayList<>(latenciesNanos);
        Collections.sort(sorted);
        long p50Ms = nanosToMillis(percentile(sorted, 0.50));
        long p95Ms = nanosToMillis(percentile(sorted, 0.95));
        long p99Ms = nanosToMillis(percentile(sorted, 0.99));
        double errorRatePct = operations.get() == 0 ? 100.0 : failures.get() * 100.0 / operations.get();
        double rps = operations.get() / Math.max(0.001, elapsedSeconds);

        Long persistedWrites = jdbc.queryForObject(
                "SELECT COALESCE(SUM(CAST(valor AS UNSIGNED)), 0) FROM configuracao_sistema WHERE chave LIKE ?",
                Long.class, prefix + "%");

        String origemBanco = MYSQL_NATIVO
                ? "MySQL nativo via INTEGRATION_MYSQL_URL"
                : MYSQL.getDockerImageName() + " via Testcontainers";
        String report = String.format(Locale.ROOT, """
                SIG GTRAT - TESTE DE CARGA DO BANCO
                Gerado em: %s
                Banco: %s
                Usuários virtuais: %d
                Duração configurada: %ds
                Chaves concorridas: %d
                Percentual de escrita: %.2f%%

                Operações: %d
                Leituras concluídas: %d
                Escritas concluídas: %d
                Falhas: %d
                Taxa de erro: %.4f%%
                Throughput: %.2f ops/s
                P50: %dms
                P95: %dms
                P99: %dms
                Incrementos persistidos: %d
                Incrementos esperados: %d

                Limites:
                P95 <= %dms
                erro <= %.4f%%
                throughput >= %.2f ops/s
                """,
                Instant.now(), origemBanco, users, durationSeconds, keys, writePercent,
                operations.get(), reads.get(), writes.get(), failures.get(), errorRatePct, rps,
                p50Ms, p95Ms, p99Ms, persistedWrites == null ? -1 : persistedWrites, writes.get(),
                maxP95Ms, maxErrorRatePct, minRps);
        writeReport(report);
        System.out.println(report);

        assertTrue(operations.get() > 0, "Nenhuma operação foi executada.");
        assertEquals(writes.get(), persistedWrites,
                "Lost update ou escrita não persistida detectada durante a carga concorrente.");
        assertTrue(errorRatePct <= maxErrorRatePct,
                () -> "Taxa de erro " + errorRatePct + "% acima de " + maxErrorRatePct + "%");
        assertTrue(p95Ms <= maxP95Ms,
                () -> "P95 " + p95Ms + "ms acima de " + maxP95Ms + "ms");
        assertTrue(rps >= minRps,
                () -> "Throughput " + rps + " ops/s abaixo de " + minRps + " ops/s");
    }

    private String key(int index) {
        return prefix + "." + index;
    }

    private static long percentile(List<Long> sorted, double p) {
        if (sorted.isEmpty()) return Long.MAX_VALUE;
        int index = (int) Math.ceil(sorted.size() * p) - 1;
        return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1)));
    }

    private static long nanosToMillis(long nanos) {
        if (nanos == Long.MAX_VALUE) return Long.MAX_VALUE;
        return TimeUnit.NANOSECONDS.toMillis(nanos);
    }

    private static int intProperty(String name, int defaultValue, int min, int max) {
        int value = Integer.parseInt(System.getProperty(name, String.valueOf(defaultValue)));
        if (value < min || value > max) throw new IllegalArgumentException(name + " fora do intervalo " + min + ".." + max);
        return value;
    }

    private static long longProperty(String name, long defaultValue, long min, long max) {
        long value = Long.parseLong(System.getProperty(name, String.valueOf(defaultValue)));
        if (value < min || value > max) throw new IllegalArgumentException(name + " fora do intervalo " + min + ".." + max);
        return value;
    }

    private static double doubleProperty(String name, double defaultValue, double min, double max) {
        double value = Double.parseDouble(System.getProperty(name, String.valueOf(defaultValue)));
        if (value < min || value > max) throw new IllegalArgumentException(name + " fora do intervalo " + min + ".." + max);
        return value;
    }

    private static void writeReport(String report) throws IOException {
        Path path = Path.of("target", "relatorios", "relatorio-carga-banco.txt");
        Files.createDirectories(path.getParent());
        Files.writeString(path, report, StandardCharsets.UTF_8);
    }
}
