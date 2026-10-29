package com.example.siggtrat.load;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Teste de carga HTTP manual. O nome da classe não termina em Test para não ser
 * executado pelo `mvn test` normal.
 *
 * Exemplo:
 * mvn -Dtest=com.example.siggtrat.load.HttpLoad -Dload.users=50 -Dload.durationSeconds=60 test
 */
public class HttpLoad {

    @Test
    void executarCargaHttpComMetricasDeDisponibilidadeIntegridadeELatencia() throws Exception {
        LoadTestConfig config = LoadTestConfig.fromSystemProperties();

        System.out.printf(
                "Iniciando carga: %s %s | usuários=%d | warmup=%ds | duração=%ds%n",
                config.method(), config.uri(), config.virtualUsers(), config.warmupSeconds(), config.durationSeconds()
        );

        LoadTestResult result = new HttpLoadRunner(config).run();
        File report = new LoadTestPdfReport().write(config, result);

        System.out.printf(
                "Carga finalizada: total=%d 2xx=%d 429=%d 4xx=%d 5xx=%d transporte=%d RPS=%.2f P95=%.2fms integridade=%d%n",
                result.totalRequests(), result.successful2xx(), result.throttled429(), result.clientErrors4xx(),
                result.serverErrors5xx(), result.transportErrors(), result.requestsPerSecond(), result.p95Ms(),
                result.integrityMismatches()
        );
        System.out.println("Relatório PDF de carga gerado em: " + report.getPath());

        assertTrue(result.approved(), () -> "Teste de carga reprovado: " + String.join("; ", result.thresholdViolations()));
    }
}
