package com.example.siggtrat.servico;

import com.example.siggtrat.configuracao.NomesCache;
import com.example.siggtrat.dto.EmpregadoFrequenciaDTO;
import com.example.siggtrat.dto.EmpregadoListaDTO;
import com.example.siggtrat.dto.FrequenciaDTO;
import com.example.siggtrat.dto.PaginaCacheDTO;
import com.example.siggtrat.dto.PainelGestaoDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Fronteira única para consultas grandes armazenadas no Redis.
 *
 * O Supplier só é executado quando não existe valor no cache. Mantê-lo em um
 * bean separado evita self-invocation das anotações @Cacheable e permite que
 * controllers/services conservem as regras atuais de segurança e montagem das
 * Specifications.
 */
@Service
public class ConsultaCacheRedisServico implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    @Override public org.slf4j.Logger loggerDiagnostico() { return log; }


    private static final Logger log = LoggerFactory.getLogger(ConsultaCacheRedisServico.class);

    @Cacheable(cacheNames = NomesCache.PAINEL_GESTAO, key = "#p0", sync = true)
    public PainelGestaoDTO painelGestao(
            String cacheKey,
            Supplier<PainelGestaoDTO> loader
    ) {
        return carregar("painel", cacheKey, loader);
    }

    @Cacheable(cacheNames = NomesCache.EMPREGADOS_RESUMO, key = "#p0", sync = true)
    public PaginaCacheDTO<EmpregadoListaDTO> empregadosResumo(
            String cacheKey,
            Supplier<PaginaCacheDTO<EmpregadoListaDTO>> loader
    ) {
        return carregar("lista resumida de empregados", cacheKey, loader);
    }

    @Cacheable(cacheNames = NomesCache.EMPREGADOS_RESUMO_TOTAL, key = "#p0", sync = true)
    public long empregadosResumoTotal(
            String cacheKey,
            LongSupplier loader
    ) {
        long inicio = System.nanoTime();
        log.debug("Redis MISS na contagem da lista de empregados: {}", cacheKey);
        try {
            return loader.getAsLong();
        } finally {
            log.debug("MISS da contagem de empregados concluído em {} ms: {}",
                    decorridoEmMs(inicio), cacheKey);
        }
    }

    @Cacheable(cacheNames = NomesCache.EMPREGADOS_FREQUENCIA, key = "#p0", sync = true)
    public PaginaCacheDTO<EmpregadoFrequenciaDTO> empregadosFrequencia(
            String cacheKey,
            Supplier<PaginaCacheDTO<EmpregadoFrequenciaDTO>> loader
    ) {
        return carregar("empregados da grade de frequência", cacheKey, loader);
    }

    @Cacheable(cacheNames = NomesCache.FREQUENCIAS_CONSULTA, key = "#p0", sync = true)
    public PaginaCacheDTO<FrequenciaDTO> frequencias(
            String cacheKey,
            Supplier<PaginaCacheDTO<FrequenciaDTO>> loader
    ) {
        return carregar("consulta de frequências", cacheKey, loader);
    }

    private <T> T carregar(String nome, String cacheKey, Supplier<T> loader) {
        long inicio = System.nanoTime();
        log.debug("Redis MISS em {}: {}", nome, cacheKey);
        try {
            return loader.get();
        } finally {
            log.debug("MISS de {} concluído em {} ms: {}", nome, decorridoEmMs(inicio), cacheKey);
        }
    }

    private long decorridoEmMs(long inicio) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicio);
    }
}
