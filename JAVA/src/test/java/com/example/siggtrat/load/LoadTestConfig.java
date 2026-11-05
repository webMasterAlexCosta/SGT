package com.example.siggtrat.load;

import java.net.URI;
import java.time.Duration;
import java.util.Locale;

public record LoadTestConfig(
        URI uri,
        String method,
        String requestBody,
        String contentType,
        String bearerToken,
        int virtualUsers,
        int warmupSeconds,
        int durationSeconds,
        Duration requestTimeout,
        boolean verifyStableBodyHash,
        double maxP95Ms,
        double maxServerErrorRatePct,
        double maxTransportErrorRatePct,
        double maxClientErrorRatePct,
        double maxThrottleRatePct,
        double maxIntegrityErrorRatePct,
        double minRequestsPerSecond
) {

    public static LoadTestConfig fromSystemProperties() {
        String baseUrl = property("load.baseUrl", "http://localhost:8080").replaceAll("/+$", "");
        String endpoint = property("load.endpoint", "/api/autenticacao/superintendencias");
        if (!endpoint.startsWith("/")) endpoint = "/" + endpoint;

        String method = property("load.method", "GET").trim().toUpperCase(Locale.ROOT);
        boolean unsafe = Boolean.parseBoolean(property("load.allowUnsafeMethods", "false"));
        if (!unsafe && !method.equals("GET") && !method.equals("HEAD")) {
            throw new IllegalArgumentException(
                    "Método " + method + " pode alterar dados. Use -Dload.allowUnsafeMethods=true somente quando isso for intencional."
            );
        }

        int users = positiveInt("load.users", 25);
        int warmup = nonNegativeInt("load.warmupSeconds", 3);
        int duration = positiveInt("load.durationSeconds", 30);
        int timeoutMs = positiveInt("load.timeoutMs", 5000);

        return new LoadTestConfig(
                URI.create(baseUrl + endpoint),
                method,
                property("load.body", ""),
                property("load.contentType", "application/json"),
                property("load.token", ""),
                users,
                warmup,
                duration,
                Duration.ofMillis(timeoutMs),
                Boolean.parseBoolean(property("load.verifyStableBodyHash", "true")),
                positiveDouble("load.maxP95Ms", 1500.0),
                nonNegativeDouble("load.maxServerErrorRatePct", 1.0),
                nonNegativeDouble("load.maxTransportErrorRatePct", 1.0),
                nonNegativeDouble("load.maxClientErrorRatePct", 1.0),
                nonNegativeDouble("load.maxThrottleRatePct", 25.0),
                nonNegativeDouble("load.maxIntegrityErrorRatePct", 0.0),
                nonNegativeDouble("load.minRps", 0.0)
        );
    }

    private static String property(String name, String defaultValue) {
        String value = System.getProperty(name);
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private static int positiveInt(String name, int defaultValue) {
        int value = Integer.parseInt(property(name, Integer.toString(defaultValue)));
        if (value <= 0) throw new IllegalArgumentException(name + " deve ser maior que zero.");
        return value;
    }

    private static int nonNegativeInt(String name, int defaultValue) {
        int value = Integer.parseInt(property(name, Integer.toString(defaultValue)));
        if (value < 0) throw new IllegalArgumentException(name + " não pode ser negativo.");
        return value;
    }

    private static double positiveDouble(String name, double defaultValue) {
        double value = Double.parseDouble(property(name, Double.toString(defaultValue)));
        if (!(value > 0.0)) throw new IllegalArgumentException(name + " deve ser maior que zero.");
        return value;
    }

    private static double nonNegativeDouble(String name, double defaultValue) {
        double value = Double.parseDouble(property(name, Double.toString(defaultValue)));
        if (value < 0.0) throw new IllegalArgumentException(name + " não pode ser negativo.");
        return value;
    }
}
