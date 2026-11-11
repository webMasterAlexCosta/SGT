package com.example.siggtrat.servico;

import com.example.siggtrat.configuracao.NomesCache;
import com.example.siggtrat.dto.CacheDiagnosticoDTO;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
public class CacheDiagnosticoServico implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(CacheDiagnosticoServico.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }


    private final CacheManager cacheManager;
    private final ObjectProvider<RedisConnectionFactory> redisConnectionFactoryProvider;

    public CacheDiagnosticoServico(
            CacheManager cacheManager,
            ObjectProvider<RedisConnectionFactory> redisConnectionFactoryProvider
    ) {
        this.cacheManager = cacheManager;
        this.redisConnectionFactoryProvider = redisConnectionFactoryProvider;
    }

    public CacheDiagnosticoDTO status() {
        String cacheManagerNome = cacheManager.getClass().getSimpleName();
        boolean redisConfigurado = cacheManager.getClass().getName().contains("Redis");
        var caches = Arrays.asList(NomesCache.CACHES_NEGOCIO);

        RedisConnectionFactory factory = redisConnectionFactoryProvider.getIfAvailable();
        if (factory == null) {
            return new CacheDiagnosticoDTO(
                    cacheManagerNome,
                    redisConfigurado,
                    caches,
                    false,
                    null,
                    "RedisConnectionFactory não disponível."
            );
        }

        RedisConnection connection = null;
        try {
            connection = factory.getConnection();
            String ping = connection.ping();
            return new CacheDiagnosticoDTO(
                    cacheManagerNome,
                    redisConfigurado,
                    caches,
                    "PONG".equalsIgnoreCase(ping),
                    ping,
                    null
            );
        } catch (RuntimeException exception) {
            return new CacheDiagnosticoDTO(
                    cacheManagerNome,
                    redisConfigurado,
                    caches,
                    false,
                    null,
                    exception.getClass().getSimpleName() + ": " + exception.getMessage()
            );
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (RuntimeException ignored) {
                    // O diagnóstico já foi produzido; falha no close não deve mascará-lo.
                }
            }
        }
    }
}
