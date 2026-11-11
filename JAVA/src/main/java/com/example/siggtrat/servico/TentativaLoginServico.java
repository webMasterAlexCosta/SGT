package com.example.siggtrat.servico;

import com.example.siggtrat.configuracao.propriedades.PropriedadesSegurancaLogin;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TentativaLoginServico implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(TentativaLoginServico.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }


    private final int maxAttempts;
    private final long blockDurationMs;
    private final Map<String, Tentativa> attemptsCache = new ConcurrentHashMap<>();

    public TentativaLoginServico(PropriedadesSegurancaLogin properties) {
        this.maxAttempts = properties.maxAttempts();
        this.blockDurationMs = properties.blockDurationMs();
    }

    private static class Tentativa {
        int count;
        long lockReleaseTime;
        long lastFailureTime;

        Tentativa(int count, long lockReleaseTime, long lastFailureTime) {
            this.count = count;
            this.lockReleaseTime = lockReleaseTime;
            this.lastFailureTime = lastFailureTime;
        }
    }

    @Scheduled(fixedRateString = "${autenticar.attempt.cleanup-rate-ms}")
    public void limparCacheExpirado() {
        long now = System.currentTimeMillis();
        attemptsCache.entrySet().removeIf(entry -> {
            Tentativa attempt = entry.getValue();
            boolean isLocked = attempt.lockReleaseTime > 0;
            return isLocked
                    ? now >= attempt.lockReleaseTime
                    : now >= attempt.lastFailureTime + blockDurationMs;
        });
    }

    public long obterTempoBloqueioRestante(String key) {
        if (key == null) return 0;

        Tentativa attempt = attemptsCache.get(key);
        if (attempt == null) return 0;

        if (attempt.lockReleaseTime > 0) {
            long now = System.currentTimeMillis();
            if (now < attempt.lockReleaseTime) {
                return (attempt.lockReleaseTime - now) / 1000;
            }
            attemptsCache.remove(key);
        }
        return 0;
    }

    public boolean estaBloqueado(String key) {
        return key != null && obterTempoBloqueioRestante(key) > 0;
    }

    public void registrarFalhaLogin(String key) {
        if (key == null) return;

        long now = System.currentTimeMillis();
        attemptsCache.compute(key, (ignored, atual) -> {
            Tentativa attempt = atual;
            boolean bloqueioExpirado = attempt != null
                    && attempt.lockReleaseTime > 0 && now >= attempt.lockReleaseTime;
            boolean janelaExpirada = attempt != null
                    && attempt.lockReleaseTime == 0
                    && now >= attempt.lastFailureTime + blockDurationMs;
            if (attempt == null || bloqueioExpirado || janelaExpirada) {
                attempt = new Tentativa(0, 0, now);
            }
            attempt.count++;
            attempt.lastFailureTime = now;
            if (attempt.count >= maxAttempts) attempt.lockReleaseTime = now + blockDurationMs;
            return attempt;
        });
    }

    public void registrarSucessoLogin(String key) {
        if (key != null) {
            attemptsCache.remove(key);
        }
    }
}
