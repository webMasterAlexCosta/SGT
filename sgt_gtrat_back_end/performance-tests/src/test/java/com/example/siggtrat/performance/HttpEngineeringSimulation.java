package com.example.siggtrat.performance;

import io.gatling.javaapi.core.Simulation;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.constantConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.global;
import static io.gatling.javaapi.core.CoreDsl.rampConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

/**
 * Teste profissional de carga HTTP com Gatling.
 *
 * <p>O cenário usa workload fechado: mantém um número configurável de usuários
 * concorrentes. Assim, "load.users" representa concorrência, e não apenas uma
 * quantidade total de requisições.</p>
 */
public final class HttpEngineeringSimulation extends Simulation {

    public HttpEngineeringSimulation() {
        String baseUrl = stringProperty("load.baseUrl", "http://localhost:8080");
        String endpoint = stringProperty("load.endpoint", "/api/autenticacao/superintendencias");
        String token = stringProperty("load.token", "").trim();

        int users = intProperty("load.users", 25, 1, 5_000);
        int rampSeconds = intProperty("load.rampSeconds", 5, 0, 3_600);
        int durationSeconds = intProperty("load.durationSeconds", 30, 1, 86_400);
        int thinkTimeMs = intProperty("load.thinkTimeMs", 1_000, 0, 60_000);
        int maxP95Ms = intProperty("load.maxP95Ms", 1_500, 1, 300_000);
        double maxFailedPct = doubleProperty("load.maxFailedPct", 1.0, 0.0, 100.0);

        var request = http("GET endpoint sob carga")
                .get(endpoint)
                .check(status().in(200, 201, 202, 203, 204, 205, 206));

        if (!token.isBlank()) {
            request = request.header("Authorization", token.startsWith("Bearer ") ? token : "Bearer " + token);
        }

        var userScenario = scenario("Usuário concorrente SIG-GTRAT")
                .exec(request);

        if (thinkTimeMs > 0) {
            userScenario = userScenario.pause(Duration.ofMillis(thinkTimeMs));
        }

        var protocol = http
                .baseUrl(baseUrl)
                .acceptHeader("application/json")
                .userAgentHeader("SIG-GTRAT-Gatling/3.15");

        var population = rampSeconds > 0
                ? userScenario.injectClosed(
                        rampConcurrentUsers(0).to(users).during(Duration.ofSeconds(rampSeconds)),
                        constantConcurrentUsers(users).during(Duration.ofSeconds(durationSeconds))
                )
                : userScenario.injectClosed(
                        constantConcurrentUsers(users).during(Duration.ofSeconds(durationSeconds))
                );

        setUp(population.protocols(protocol))
                .assertions(
                        global().failedRequests().percent().lte(maxFailedPct),
                        global().responseTime().percentile3().lte(maxP95Ms)
                );
    }

    private static String stringProperty(String name, String defaultValue) {
        return System.getProperty(name, defaultValue);
    }

    private static int intProperty(String name, int defaultValue, int min, int max) {
        String raw = System.getProperty(name);
        int value = raw == null || raw.isBlank() ? defaultValue : Integer.parseInt(raw.trim());
        if (value < min || value > max) {
            throw new IllegalArgumentException(name + " deve estar entre " + min + " e " + max + ".");
        }
        return value;
    }

    private static double doubleProperty(String name, double defaultValue, double min, double max) {
        String raw = System.getProperty(name);
        double value = raw == null || raw.isBlank() ? defaultValue : Double.parseDouble(raw.trim());
        if (value < min || value > max) {
            throw new IllegalArgumentException(name + " deve estar entre " + min + " e " + max + ".");
        }
        return value;
    }
}
