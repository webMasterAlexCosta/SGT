package com.example.siggtrat.servico;

import com.example.siggtrat.configuracao.NomesCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Arrays;

@Service
public class InvalidacaoCacheServico {

    private static final Logger log = LoggerFactory.getLogger(InvalidacaoCacheServico.class);

    private final CacheManager cacheManager;
    private final RevisaoFontesPainelServico revisaoFontes;

    public InvalidacaoCacheServico(CacheManager cacheManager, RevisaoFontesPainelServico revisaoFontes) {
        this.cacheManager = cacheManager;
        this.revisaoFontes = revisaoFontes;
    }

    /**
     * Limpa os caches somente após o commit quando existe uma transação ativa.
     * Assim uma operação que sofre rollback não remove valores válidos.
     */
    public void invalidarCachesDeNegocioAposCommit() {
        revisaoFontes.registrarMudanca();
        Runnable invalidacao = () -> invalidar(NomesCache.CACHES_NEGOCIO);

        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    invalidacao.run();
                }
            });
            return;
        }

        invalidacao.run();
    }

    public void invalidarPainel() {
        invalidar(
                NomesCache.PAINEL_GESTAO,
                NomesCache.PAINEL_STATS,
                NomesCache.PAINEL_RANKING,
                NomesCache.API_GET_RESPONSES
        );
    }

    public void invalidarPainelEFrequencias() {
        invalidar(
                NomesCache.PAINEL_GESTAO,
                NomesCache.PAINEL_STATS,
                NomesCache.PAINEL_RANKING,
                NomesCache.FREQUENCIAS_CONSULTA,
                NomesCache.API_GET_RESPONSES
        );
    }

    public void invalidar(String... cacheNames) {
        Arrays.stream(cacheNames)
                .distinct()
                .forEach(name -> {
                    Cache cache = cacheManager.getCache(name);
                    if (cache != null) {
                        try {
                            cache.clear();
                        } catch (RuntimeException exception) {
                            log.warn("Falha ao invalidar o cache '{}'; a operação principal foi preservada: {}",
                                    name, exception.getMessage());
                        }
                    }
                });
        log.debug("Caches invalidados: {}", Arrays.toString(cacheNames));
    }
}
