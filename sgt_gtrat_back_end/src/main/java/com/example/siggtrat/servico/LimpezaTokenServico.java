package com.example.siggtrat.servico;

import com.example.siggtrat.configuracao.propriedades.PropriedadesSegurancaJwt;
import com.example.siggtrat.repositorio.TokenAtivoRepositorio;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

@Service
public class LimpezaTokenServico {

    private static final Logger logger = LoggerFactory.getLogger(LimpezaTokenServico.class);
    private final TokenAtivoRepositorio repositorio;

    private final int batchSize;

    public LimpezaTokenServico(TokenAtivoRepositorio repositorio,
                               PropriedadesSegurancaJwt properties) {
        this.repositorio = repositorio;
        this.batchSize = properties.cleanup().batchSize();
    }

    // O cron de limpeza de tokens expirados pode ser configurado via propriedade
    // `app.security.jwt.cleanup.cron`. Caso a propriedade não seja definida, o valor
    // padrão executará a rotina a cada hora ("0 0 * * * ?").
    @Scheduled(cron = "${app.security.jwt.cleanup.cron}")
    @org.springframework.retry.annotation.Retryable(
            value = java.lang.Exception.class,
            maxAttemptsExpression = "${jwt.cleanup.retry-attempts}",
            backoff = @org.springframework.retry.annotation.Backoff(
                    delayExpression = "${jwt.cleanup.retry-delay-ms}",
                    multiplier = 2.0
            )
    )
    public void limparTokensExpirados() {
        LocalDateTime now = com.example.siggtrat.utilitario.RelogioSistema.agora();
        logger.info("Executando limpeza de tokens expirados (Batch strategy): {}", now);

        int totalDeleted = 0;
        int deletedInLastBatch;

        try {
            do {
                deletedInLastBatch = repositorio.excluirLoteTokensExpirados(now, batchSize);
                totalDeleted += deletedInLastBatch;
            } while (deletedInLastBatch > 0);

            logger.info("Limpeza concluída. Total de tokens removidos: {}", totalDeleted);
        } catch (Exception e) {
            logger.error("Erro ao limpar tokens em batch", e);
        }
    }
}