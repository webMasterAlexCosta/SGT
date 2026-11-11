package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.configuracao.NomesCache;
import com.example.siggtrat.configuracao.ServicoChaveCacheRedis;
import com.example.siggtrat.dto.RespostaCacheConsultaHttp;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** Regressão com PostgreSQL/Redis reais: uma chave herdada não reativa uma rota retirada. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CacheRotasRetiradasRedisHttpIT extends ContratosHttpBase {
    @Autowired CacheManager caches;
    @Autowired RedisConnectionFactory redis;
    @Autowired ServicoChaveCacheRedis chaves;

    @DynamicPropertySource
    static void redisReal(DynamicPropertyRegistry properties) {
        properties.add("spring.cache.type", () -> "redis");
        properties.add("spring.data.redis.host", () -> System.getenv().getOrDefault("REDIS_HOST", "127.0.0.1"));
        properties.add("spring.data.redis.port", () -> System.getenv().getOrDefault("REDIS_PORT", "6379"));
        properties.add("spring.data.redis.password", () -> System.getenv().getOrDefault("REDIS_PASSWORD", ""));
        properties.add("spring.data.redis.database", () -> System.getenv().getOrDefault("REDIS_DATABASE", "0"));
        properties.add("app.cache.redis.http-get.enabled", () -> "true");
    }

    @Test
    void entradaLegadaPermaneceNoRedisMasNaoSubstitui404Autenticado() throws Exception {
        try (var connection = redis.getConnection()) { assertEquals("PONG", connection.ping()); }
        var cache = caches.getCache(NomesCache.API_GET_RESPONSES);
        assertInstanceOf(RedisCache.class, cache, "Este teste exige o cache Redis real");
        var cenario = criarCenarioPersistido();
        assertTrue(mappings.getHandlerMethods().keySet().stream()
                .flatMap(info -> info.getPatternValues().stream())
                .noneMatch(path -> path.startsWith("/api/governanca-terceiros")));

        var primeira = executar(cenario.localA, get("/api/fornecedores").accept(APPLICATION_JSON), 200,
                "Controle operacional MISS na mesma conta de Gestão Local");
        var repetida = executar(cenario.localA, get("/api/fornecedores").accept(APPLICATION_JSON), 200,
                "Controle operacional HIT no Redis real");
        assertEquals("MISS", primeira.getResponse().getHeader("X-SGT-Cache"));
        assertEquals("HIT", repetida.getResponse().getHeader("X-SGT-Cache"));
        assertEquals(dados(primeira), dados(repetida));

        byte[] legado = "{\"status\":200,\"data\":[{\"evidencias\":\"LEGADO SINTETICO\"}]}"
                .getBytes(StandardCharsets.UTF_8);
        for (String sufixo : List.of("/riscos", "/resumo", "/fornecedores", "/processos",
                "/tabelas-tarifarias", "/folhas-horas", "/faturas", "", "/")) {
            String rota = "/api/governanca-terceiros" + sufixo;
            String chave;
            var anterior = SecurityContextHolder.getContext();
            try {
                var contexto = SecurityContextHolder.createEmptyContext();
                contexto.setAuthentication(new UsernamePasswordAuthenticationToken(cenario.localA.getUsername(), null,
                        List.of(new SimpleGrantedAuthority("PERFIL_GESTOR_LOCAL"))));
                SecurityContextHolder.setContext(contexto);
                chave = chaves.consulta("http-get", rota, "", "application/json");
            } finally {
                SecurityContextHolder.setContext(anterior);
            }
            // A fixture deve estar publicada antes do HTTP; Cache.put pode ser diferido.
            // Cache.putIfAbsent garante registro imediato e mantém intactas as verificações abaixo.
            cache.putIfAbsent(chave, new RespostaCacheConsultaHttp(200, "application/json", legado));
            assertArrayEquals(legado, cache.get(chave, RespostaCacheConsultaHttp.class).body());
            var retirada = executar(cenario.localA, get(rota).accept(APPLICATION_JSON), 404,
                    "Rota retirada deve chegar ao MVC apesar dos bytes legados no Redis: " + rota);
            assertNull(retirada.getResponse().getHeader("X-SGT-Cache"));
            assertFalse(retirada.getResponse().getContentAsString().contains("LEGADO SINTETICO"));
            assertArrayEquals(legado, cache.get(chave, RespostaCacheConsultaHttp.class).body(),
                    "A correção não elimina ou modifica o registro Redis herdado");
        }

        var anonima = executar(null, get("/api/fornecedores").accept(APPLICATION_JSON), 401,
                "Cache operacional não dispensa autenticação");
        var nacional = executar(cenario.nacional, get("/api/fornecedores").accept(APPLICATION_JSON), 403,
                "Cache operacional não amplia o perfil autorizado");
        assertNotEquals("HIT", anonima.getResponse().getHeader("X-SGT-Cache"));
        assertNotEquals("HIT", nacional.getResponse().getHeader("X-SGT-Cache"));
    }
}
