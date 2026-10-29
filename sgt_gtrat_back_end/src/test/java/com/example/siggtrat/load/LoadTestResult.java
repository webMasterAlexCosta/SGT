package com.example.siggtrat.load;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record LoadTestResult(
        Instant startedAt,
        Instant finishedAt,
        Duration elapsed,
        long totalRequests,
        long successful2xx,
        long throttled429,
        long clientErrors4xx,
        long serverErrors5xx,
        long transportErrors,
        long integrityMismatches,
        long bytesReceived,
        double requestsPerSecond,
        double p50Ms,
        double p90Ms,
        double p95Ms,
        double p99Ms,
        double maxMs,
        Map<Integer, Long> statusCodes,
        List<String> thresholdViolations
) {
    public boolean approved() {
        return thresholdViolations.isEmpty();
    }

    public double serverErrorRatePct() {
        return rate(serverErrors5xx, totalRequests);
    }

    public double transportErrorRatePct() {
        return rate(transportErrors, totalRequests);
    }

    public double clientErrorRatePct() {
        return rate(clientErrors4xx, totalRequests);
    }

    public double throttleRatePct() {
        return rate(throttled429, totalRequests);
    }

    public double integrityErrorRatePct() {
        return rate(integrityMismatches, successful2xx);
    }

    private static double rate(long value, long base) {
        return base <= 0 ? 0.0 : value * 100.0 / base;
    }
}
