package com.example.siggtrat.configuracao;

import com.example.siggtrat.dto.ContratoDTO;
import com.example.siggtrat.dto.ContratoOpcaoDTO;
import com.example.siggtrat.dto.RespostaCacheConsultaHttp;
import com.example.siggtrat.dto.EmpregadoFrequenciaDTO;
import com.example.siggtrat.dto.EmpregadoListaDTO;
import com.example.siggtrat.dto.FrequenciaDTO;
import com.example.siggtrat.dto.PaginaCacheDTO;
import com.example.siggtrat.dto.LotacaoOpcaoDTO;
import com.example.siggtrat.dto.PainelGestaoDTO;
import com.example.siggtrat.dto.PainelRankingDTO;
import com.example.siggtrat.dto.EstatisticasPainelDTO;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = true)
@SuppressWarnings("removal")
public class ConfiguracaoCacheRedis implements CachingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(ConfiguracaoCacheRedis.class);

    private final RedisConnectionFactory connectionFactory;
    private final ObjectMapper applicationObjectMapper;
    private final Duration painelTtl;
    private final Duration empregadosResumoTtl;
    private final Duration empregadosFrequenciaTtl;
    private final Duration frequenciasTtl;
    private final Duration httpGetTtl;

    public ConfiguracaoCacheRedis(
            RedisConnectionFactory connectionFactory,
            ObjectMapper applicationObjectMapper,
            @Value("${app.cache.redis.ttl.painel:15m}") Duration painelTtl,
            @Value("${app.cache.redis.ttl.empregados-resumo:90s}") Duration empregadosResumoTtl,
            @Value("${app.cache.redis.ttl.empregados-frequencia:5m}") Duration empregadosFrequenciaTtl,
            @Value("${app.cache.redis.ttl.frequencias:2m}") Duration frequenciasTtl,
            @Value("${app.cache.redis.ttl.http-get:45s}") Duration httpGetTtl
    ) {
        this.connectionFactory = connectionFactory;
        this.applicationObjectMapper = applicationObjectMapper;
        this.painelTtl = positivo(painelTtl, Duration.ofMinutes(15));
        this.empregadosResumoTtl = positivo(empregadosResumoTtl, Duration.ofSeconds(90));
        this.empregadosFrequenciaTtl = positivo(empregadosFrequenciaTtl, Duration.ofMinutes(5));
        this.frequenciasTtl = positivo(frequenciasTtl, Duration.ofMinutes(2));
        this.httpGetTtl = positivo(httpGetTtl, Duration.ofSeconds(45));
    }

    @Bean
    @Override
    public CacheManager cacheManager() {
        ObjectMapper cacheObjectMapper = applicationObjectMapper.copy();
        cacheObjectMapper.findAndRegisterModules();
        cacheObjectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        cacheObjectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        cacheObjectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        RedisSerializationContext.SerializationPair<String> keySerializer =
                RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer());

        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .computePrefixWith(cacheName -> "sgt:cache:" + cacheName + ":")
                .serializeKeysWith(keySerializer)
                .entryTtl(Duration.ofMinutes(5))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> configurations = new LinkedHashMap<>();
        configurations.put(
                NomesCache.PAINEL_GESTAO,
                tipado(defaults, cacheObjectMapper, cacheObjectMapper.constructType(PainelGestaoDTO.class), painelTtl)
        );
        configurations.put(
                NomesCache.PAINEL_STATS,
                tipado(defaults, cacheObjectMapper, cacheObjectMapper.constructType(EstatisticasPainelDTO.class), Duration.ofMinutes(2))
        );
        configurations.put(
                NomesCache.PAINEL_RANKING,
                tipado(defaults, cacheObjectMapper, listaDe(cacheObjectMapper, PainelRankingDTO.class), Duration.ofMinutes(5))
        );
        configurations.put(
                NomesCache.CONTRATOS,
                tipado(defaults, cacheObjectMapper, listaDe(cacheObjectMapper, ContratoDTO.class), Duration.ofMinutes(10))
        );
        configurations.put(
                NomesCache.CONTRATOS_OPCOES,
                tipado(defaults, cacheObjectMapper, listaDe(cacheObjectMapper, ContratoOpcaoDTO.class), Duration.ofMinutes(5))
        );
        configurations.put(
                NomesCache.CONTRATO_POR_ID,
                tipado(defaults, cacheObjectMapper, cacheObjectMapper.constructType(ContratoDTO.class), Duration.ofMinutes(10))
        );
        configurations.put(
                NomesCache.LOTACOES_OPCOES,
                tipado(defaults, cacheObjectMapper, listaDe(cacheObjectMapper, LotacaoOpcaoDTO.class), Duration.ofMinutes(10))
        );
        configurations.put(
                NomesCache.EMPREGADOS_RESUMO,
                tipado(defaults, cacheObjectMapper,
                        paginaDe(cacheObjectMapper, EmpregadoListaDTO.class), empregadosResumoTtl)
        );
        configurations.put(
                NomesCache.EMPREGADOS_RESUMO_TOTAL,
                tipado(defaults, cacheObjectMapper,
                        cacheObjectMapper.constructType(Long.class), empregadosResumoTtl)
        );
        configurations.put(
                NomesCache.EMPREGADOS_FREQUENCIA,
                tipado(defaults, cacheObjectMapper,
                        paginaDe(cacheObjectMapper, EmpregadoFrequenciaDTO.class), empregadosFrequenciaTtl)
        );
        configurations.put(
                NomesCache.FREQUENCIAS_CONSULTA,
                tipado(defaults, cacheObjectMapper,
                        paginaDe(cacheObjectMapper, FrequenciaDTO.class), frequenciasTtl)
        );
        configurations.put(
                NomesCache.API_GET_RESPONSES,
                tipado(defaults, cacheObjectMapper,
                        cacheObjectMapper.constructType(RespostaCacheConsultaHttp.class), httpGetTtl)
        );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaults)
                .withInitialCacheConfigurations(configurations)
                .disableCreateOnMissingCache()
                .build();
    }

    private JavaType listaDe(ObjectMapper mapper, Class<?> elementType) {
        return mapper.getTypeFactory().constructCollectionType(ArrayList.class, elementType);
    }

    private JavaType paginaDe(ObjectMapper mapper, Class<?> elementType) {
        return mapper.getTypeFactory().constructParametricType(PaginaCacheDTO.class, elementType);
    }

    private Duration positivo(Duration valor, Duration padrao) {
        return valor == null || valor.isZero() || valor.isNegative() ? padrao : valor;
    }

    private RedisCacheConfiguration tipado(
            RedisCacheConfiguration base,
            ObjectMapper mapper,
            JavaType javaType,
            Duration ttl
    ) {
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(mapper, javaType);
        RedisSerializationContext.SerializationPair<Object> values =
                RedisSerializationContext.SerializationPair.fromSerializer(serializer);
        return base.serializeValuesWith(values).entryTtl(ttl);
    }

    /**
     * Redis acelera a aplicação, mas não é a fonte da verdade. Em falhas de
     * leitura/escrita do cache, registra o problema e deixa o método consultar o
     * MySQL normalmente.
     */
    @Bean
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Falha ao ler o cache Redis '{}' (chave {}). Consulta seguirá sem cache: {}",
                        cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn("Falha ao gravar o cache Redis '{}' (chave {}): {}",
                        cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Falha ao invalidar o cache Redis '{}' (chave {}): {}",
                        cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("Falha ao limpar o cache Redis '{}': {}", cache.getName(), exception.getMessage());
            }
        };
    }
}
