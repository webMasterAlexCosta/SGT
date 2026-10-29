package com.example.siggtrat.load;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

public final class HttpLoadRunner {

    private final LoadTestConfig config;
    private final HttpClient client;

    public HttpLoadRunner(LoadTestConfig config) {
        this.config = config;
        this.client = HttpClient.newBuilder()
                .connectTimeout(config.requestTimeout())
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    public LoadTestResult run() throws InterruptedException {
        if (config.warmupSeconds() > 0) {
            executePhase(config.warmupSeconds(), false);
        }
        return executePhase(config.durationSeconds(), true);
    }

    private LoadTestResult executePhase(int durationSeconds, boolean collectResult) throws InterruptedException {
        Metrics metrics = new Metrics();
        ExecutorService workers = Executors.newFixedThreadPool(config.virtualUsers());
        CountDownLatch ready = new CountDownLatch(config.virtualUsers());
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(config.virtualUsers());
        long durationNanos = TimeUnit.SECONDS.toNanos(durationSeconds);

        for (int i = 0; i < config.virtualUsers(); i++) {
            workers.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    long deadline = System.nanoTime() + durationNanos;
                    while (System.nanoTime() < deadline && !Thread.currentThread().isInterrupted()) {
                        executeOne(metrics);
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        if (!ready.await(Math.max(10, config.requestTimeout().toSeconds() + 5), TimeUnit.SECONDS)) {
            workers.shutdownNow();
            throw new IllegalStateException("Os usuários virtuais não ficaram prontos dentro do prazo.");
        }

        Instant startedAt = Instant.now();
        long startedNanos = System.nanoTime();
        start.countDown();

        long waitSeconds = durationSeconds + Math.max(10, config.requestTimeout().toSeconds() + 5);
        boolean finished = done.await(waitSeconds, TimeUnit.SECONDS);
        if (!finished) workers.shutdownNow();
        workers.shutdown();
        workers.awaitTermination(Math.max(5, config.requestTimeout().toSeconds()), TimeUnit.SECONDS);

        Instant finishedAt = Instant.now();
        Duration elapsed = Duration.ofNanos(Math.max(1, System.nanoTime() - startedNanos));
        if (!collectResult) return null;

        return metrics.toResult(config, startedAt, finishedAt, elapsed);
    }

    private void executeOne(Metrics metrics) {
        HttpRequest request = buildRequest();
        long start = System.nanoTime();
        try {
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            double latencyMs = (System.nanoTime() - start) / 1_000_000.0;
            metrics.recordResponse(response.statusCode(), response.body(), latencyMs, config.verifyStableBodyHash());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            metrics.recordTransportError((System.nanoTime() - start) / 1_000_000.0);
        } catch (Exception ex) {
            metrics.recordTransportError((System.nanoTime() - start) / 1_000_000.0);
        }
    }

    private HttpRequest buildRequest() {
        HttpRequest.Builder builder = HttpRequest.newBuilder(config.uri())
                .timeout(config.requestTimeout())
                .header("Accept", "application/json")
                .header("X-Load-Test", "sig-gtrat")
                .header("X-Request-Id", "load-" + java.util.UUID.randomUUID());

        if (!config.bearerToken().isBlank()) {
            String token = config.bearerToken().startsWith("Bearer ")
                    ? config.bearerToken()
                    : "Bearer " + config.bearerToken();
            builder.header("Authorization", token);
        }

        return switch (config.method()) {
            case "GET" -> builder.GET().build();
            case "HEAD" -> builder.method("HEAD", HttpRequest.BodyPublishers.noBody()).build();
            case "POST", "PUT", "PATCH" -> builder
                    .header("Content-Type", config.contentType())
                    .method(config.method(), HttpRequest.BodyPublishers.ofString(config.requestBody()))
                    .build();
            case "DELETE" -> builder.DELETE().build();
            default -> throw new IllegalArgumentException("Método HTTP não suportado pelo teste: " + config.method());
        };
    }

    private static final class Metrics {
        private final LongAdder total = new LongAdder();
        private final LongAdder success = new LongAdder();
        private final LongAdder throttled = new LongAdder();
        private final LongAdder clientErrors = new LongAdder();
        private final LongAdder serverErrors = new LongAdder();
        private final LongAdder transportErrors = new LongAdder();
        private final LongAdder integrityMismatches = new LongAdder();
        private final LongAdder bytesReceived = new LongAdder();
        private final ConcurrentHashMap<Integer, LongAdder> statusCodes = new ConcurrentHashMap<>();
        private final ConcurrentLinkedQueue<Double> latenciesMs = new ConcurrentLinkedQueue<>();
        private final AtomicReference<String> baselineHash = new AtomicReference<>();

        void recordResponse(int status, byte[] body, double latencyMs, boolean verifyStableHash) {
            total.increment();
            latenciesMs.add(latencyMs);
            statusCodes.computeIfAbsent(status, ignored -> new LongAdder()).increment();
            if (body != null) bytesReceived.add(body.length);

            if (status >= 200 && status < 300) {
                success.increment();
                if (verifyStableHash && body != null) verifyHash(body);
            } else if (status == 429) {
                throttled.increment();
            } else if (status >= 400 && status < 500) {
                clientErrors.increment();
            } else if (status >= 500) {
                serverErrors.increment();
            }
        }

        void recordTransportError(double latencyMs) {
            total.increment();
            transportErrors.increment();
            latenciesMs.add(latencyMs);
        }

        private void verifyHash(byte[] body) {
            String hash = sha256(body);
            String baseline = baselineHash.get();
            if (baseline == null) {
                baselineHash.compareAndSet(null, hash);
                baseline = baselineHash.get();
            }
            if (!hash.equals(baseline)) integrityMismatches.increment();
        }

        LoadTestResult toResult(LoadTestConfig config, Instant startedAt, Instant finishedAt, Duration elapsed) {
            List<Double> ordered = new ArrayList<>(latenciesMs);
            ordered.sort(Comparator.naturalOrder());
            long totalValue = total.sum();
            double seconds = Math.max(0.001, elapsed.toNanos() / 1_000_000_000.0);

            Map<Integer, Long> statuses = new LinkedHashMap<>();
            statusCodes.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(e -> statuses.put(e.getKey(), e.getValue().sum()));

            LoadTestResult provisional = new LoadTestResult(
                    startedAt,
                    finishedAt,
                    elapsed,
                    totalValue,
                    success.sum(),
                    throttled.sum(),
                    clientErrors.sum(),
                    serverErrors.sum(),
                    transportErrors.sum(),
                    integrityMismatches.sum(),
                    bytesReceived.sum(),
                    totalValue / seconds,
                    percentile(ordered, 0.50),
                    percentile(ordered, 0.90),
                    percentile(ordered, 0.95),
                    percentile(ordered, 0.99),
                    ordered.isEmpty() ? 0.0 : ordered.get(ordered.size() - 1),
                    Collections.unmodifiableMap(statuses),
                    List.of()
            );

            List<String> violations = evaluateThresholds(config, provisional);
            return new LoadTestResult(
                    provisional.startedAt(), provisional.finishedAt(), provisional.elapsed(),
                    provisional.totalRequests(), provisional.successful2xx(), provisional.throttled429(),
                    provisional.clientErrors4xx(), provisional.serverErrors5xx(), provisional.transportErrors(),
                    provisional.integrityMismatches(), provisional.bytesReceived(), provisional.requestsPerSecond(),
                    provisional.p50Ms(), provisional.p90Ms(), provisional.p95Ms(), provisional.p99Ms(), provisional.maxMs(),
                    provisional.statusCodes(), Collections.unmodifiableList(violations)
            );
        }

        private static List<String> evaluateThresholds(LoadTestConfig config, LoadTestResult r) {
            List<String> violations = new ArrayList<>();
            if (r.totalRequests() == 0) violations.add("Nenhuma requisição foi concluída.");
            if (r.p95Ms() > config.maxP95Ms()) {
                violations.add(String.format(LocaleHolder.LOCALE, "P95 %.2f ms > limite %.2f ms", r.p95Ms(), config.maxP95Ms()));
            }
            if (r.serverErrorRatePct() > config.maxServerErrorRatePct()) {
                violations.add(String.format(LocaleHolder.LOCALE, "5xx %.3f%% > limite %.3f%%", r.serverErrorRatePct(), config.maxServerErrorRatePct()));
            }
            if (r.transportErrorRatePct() > config.maxTransportErrorRatePct()) {
                violations.add(String.format(LocaleHolder.LOCALE, "erros de transporte %.3f%% > limite %.3f%%", r.transportErrorRatePct(), config.maxTransportErrorRatePct()));
            }
            if (r.clientErrorRatePct() > config.maxClientErrorRatePct()) {
                violations.add(String.format(LocaleHolder.LOCALE, "4xx (exceto 429) %.3f%% > limite %.3f%%", r.clientErrorRatePct(), config.maxClientErrorRatePct()));
            }
            if (r.throttleRatePct() > config.maxThrottleRatePct()) {
                violations.add(String.format(LocaleHolder.LOCALE, "429 %.3f%% > limite %.3f%%", r.throttleRatePct(), config.maxThrottleRatePct()));
            }
            if (config.verifyStableBodyHash() && r.integrityErrorRatePct() > config.maxIntegrityErrorRatePct()) {
                violations.add(String.format(LocaleHolder.LOCALE, "divergência de conteúdo %.3f%% > limite %.3f%%", r.integrityErrorRatePct(), config.maxIntegrityErrorRatePct()));
            }
            if (config.minRequestsPerSecond() > 0 && r.requestsPerSecond() < config.minRequestsPerSecond()) {
                violations.add(String.format(LocaleHolder.LOCALE, "RPS %.2f < mínimo %.2f", r.requestsPerSecond(), config.minRequestsPerSecond()));
            }
            return violations;
        }

        private static double percentile(List<Double> values, double percentile) {
            if (values.isEmpty()) return 0.0;
            int index = (int) Math.ceil(percentile * values.size()) - 1;
            return values.get(Math.max(0, Math.min(index, values.size() - 1)));
        }

        private static String sha256(byte[] bytes) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(bytes);
                StringBuilder sb = new StringBuilder(hash.length * 2);
                for (byte b : hash) sb.append(String.format("%02x", b));
                return sb.toString();
            } catch (Exception ex) {
                throw new IllegalStateException("Não foi possível calcular SHA-256 da resposta.", ex);
            }
        }
    }

    private static final class LocaleHolder {
        private static final java.util.Locale LOCALE = java.util.Locale.US;
    }
}
