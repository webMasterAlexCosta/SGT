package com.example.siggtrat.configuracao;

import com.example.siggtrat.configuracao.propriedades.PropriedadesLimiteRequisicoes;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class FiltroLimitacaoRequisicoes extends OncePerRequestFilter {

    private static class EntradaLimiteRequisicoes {
        final Bucket bucket;
        long lastAccessTime;

        EntradaLimiteRequisicoes(Bucket bucket, long lastAccessTime) {
            this.bucket = bucket;
            this.lastAccessTime = lastAccessTime;
        }
    }

    private final Map<String, EntradaLimiteRequisicoes> cache = new ConcurrentHashMap<>();
    private final long capacity;
    private final long durationSeconds;
    private final long timeoutMs;

    public FiltroLimitacaoRequisicoes(PropriedadesLimiteRequisicoes properties) {
        this.capacity = properties.capacity();
        this.durationSeconds = properties.durationSeconds();
        this.timeoutMs = properties.timeoutMs();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String ip = resolverIpCliente(request);
        String principalName = request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : null;
        String rateKey = (principalName != null && !principalName.isBlank()) ? (principalName + "@" + ip) : ip;

        EntradaLimiteRequisicoes entry = cache.computeIfAbsent(rateKey, this::criarEntradaGrupo);
        entry.lastAccessTime = System.currentTimeMillis();

        if (entry.bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            com.example.siggtrat.diagnostico.ErroHttpCorrelacionado.escrever(request,response,429,
                    "RATE_LIMIT_EXCEEDED",String.format("Muitas requisições. Aguarde um momento. (%d req/%ds).",
                            capacity,durationSeconds),null);
        }
    }

    private EntradaLimiteRequisicoes criarEntradaGrupo(String ignoredKey) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillGreedy(capacity, Duration.ofSeconds(durationSeconds))
                .build();
        Bucket bucket = Bucket.builder()
                .addLimit(limit)
                .build();
        return new EntradaLimiteRequisicoes(bucket, System.currentTimeMillis());
    }

    private String resolverIpCliente(HttpServletRequest req) {
        // O servidor resolve proxies confiáveis; cabeçalho arbitrário não identifica o cliente.
        return req.getRemoteAddr();
    }

    @Scheduled(fixedRateString = "${app.ratelimit.cleanup-rate-ms}")
    public void limparGruposInativos() {
        long now = System.currentTimeMillis();
        cache.entrySet().removeIf(entry -> (now - entry.getValue().lastAccessTime) > timeoutMs);
    }
}
