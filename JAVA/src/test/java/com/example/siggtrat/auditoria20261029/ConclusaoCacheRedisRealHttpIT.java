package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.*;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** MySQL e Redis reais. O cache não é substituído por ConcurrentMapCacheManager. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ConclusaoCacheRedisRealHttpIT extends ContratosHttpBase {
    @Autowired CacheManager caches;
    @Autowired RedisConnectionFactory redis;

    @DynamicPropertySource
    static void redisReal(DynamicPropertyRegistry p) {
        p.add("spring.cache.type", () -> "redis");
        p.add("spring.data.redis.host", () -> System.getenv().getOrDefault("REDIS_HOST", "127.0.0.1"));
        p.add("spring.data.redis.port", () -> System.getenv().getOrDefault("REDIS_PORT", "6379"));
        p.add("spring.data.redis.password", () -> System.getenv().getOrDefault("REDIS_PASSWORD", ""));
        p.add("spring.data.redis.database", () -> System.getenv().getOrDefault("REDIS_DATABASE", "0"));
        p.add("app.cache.redis.http-get.enabled", () -> "true");
    }

    @AfterEach
    void limparCachesDaInstanciaSintetica() {
        for (String nome : caches.getCacheNames()) {
            var cache = caches.getCache(nome);
            if (cache != null) cache.clear();
        }
    }

    @Test
    void painelEfetivoTemMissHitSemCompartilharDetalhamentoComPresidencia() throws Exception {
        try (var conexao = redis.getConnection()) { assertEquals("PONG", conexao.ping()); }
        Cenario c = criarCenarioPersistido();
        String path = "/api/painel/efetivo?referencia=" + DATA + "&periodo=DIARIO&contratoId=" + c.contratoA.getId();
        var primeira = executar(c.nacional, get(path), 200, "Redis real: primeira consulta nacional");
        var segunda = executar(c.nacional, get(path), 200, "Redis real: repetição nacional");
        assertEquals("MISS", primeira.getResponse().getHeader("X-SGT-Cache"));
        assertEquals("HIT", segunda.getResponse().getHeader("X-SGT-Cache"));
        assertEquals(dados(primeira), dados(segunda));
        assertFalse(dados(primeira).path("quadroDimensionado").isEmpty());
        var presidencia = executar(c.presidente, get(path), 200, "Presidência usa chave e capacidades próprias");
        assertEquals("MISS", presidencia.getResponse().getHeader("X-SGT-Cache"));
        assertTrue(dados(presidencia).path("quadroDimensionado").isEmpty());
        var repetida = executar(c.presidente, get(path), 200, "HIT da Presidência conserva minimização");
        assertEquals("HIT", repetida.getResponse().getHeader("X-SGT-Cache"));
        assertTrue(dados(repetida).path("quadroDimensionado").isEmpty());
    }

    @Test
    void paginaEmRedisNaoSobreviveARevogacaoDaLotacaoDoOperacional() throws Exception {
        Cenario c = criarCenarioPersistido();
        enviar(c.localA, "/api/frequencias", mapa("vinculoContratualId", c.vinculoA.getId(),
                "dataReferencia", DATA, "status", "PRESENCA", "horasTrabalhadas", "04:00",
                "horarioSaida", "12:00", "observacao", "Prova da lotação anterior",
                "justificativaRetificacao", "Registro sintético com prova de autorização",
                "chaveIdempotencia", "redis-real-" + UUID.randomUUID()), 200, "Frequência persistida no MySQL");
        String path = "/api/frequencias?data=" + DATA + "&contratoId=" + c.contratoA.getId();
        assertEquals(1, consultar(c.operacionalA, path, "Página operacional inicial").path("content").size());
        assertEquals(1, consultar(c.operacionalA, path, "Página operacional armazenada no Redis").path("content").size());
        emTransacao(() -> {
            Lotacao nova = new Lotacao("Outra lotação de cache " + UUID.randomUUID());
            nova.setGestaoLocal(em.getReference(GestaoLocal.class, c.gestaoA.getId()));
            nova.setMunicipio("Município sintético");
            nova.getTurnos().add(em.getReference(Jornada.class, c.jornadaA.getId()));
            em.persist(nova);
            em.find(UsuarioAplicacao.class, c.operacionalA.getId()).setLotacao(nova);
            return null;
        });
        var revogada = consultar(c.operacionalA, path,
                "Revogação persistida do escopo deve valer sem depender de expiração do cache");
        assertEquals(0, revogada.path("totalElements").asLong(), revogada.toString());
        assertTrue(revogada.path("content").isEmpty());
        assertEquals(1, consultar(c.localA, path, "Gestor autorizado continua lendo o mesmo fato").path("content").size());
    }

    @Test
    void transferenciaRetroativaInvalidaPaginasPositivaENegativaNoRedis() throws Exception {
        Cenario c = criarCenarioPersistido();
        conceder(c, c.contratoA.getId(), "TRANSFERENCIA_GESTAO_LOCAL");
        long id = enviar(c.localA, "/api/frequencias", mapa("vinculoContratualId", c.vinculoA.getId(),
                "dataReferencia", DATA, "status", "PRESENCA", "horasTrabalhadas", "04:00",
                "horarioSaida", "12:00", "justificativaRetificacao", "Prova anterior ao ato retroativo",
                "chaveIdempotencia", "redis-transferencia-" + UUID.randomUUID()), 200,
                "Frequência confirmada antes do conhecimento da transferência").path("id").asLong();
        String path = "/api/frequencias?data=" + DATA + "&contratoId=" + c.contratoA.getId();
        for (int repeticao = 0; repeticao < 2; repeticao++) {
            assertEquals(1, consultar(c.localA, path, "Cache positivo da origem").path("totalElements").asLong());
            assertEquals(0, consultar(c.localB, path, "Cache negativo do destino").path("totalElements").asLong());
        }
        enviar(c.localA, base(c.contratoA.getId()) + "/transferir-gestao", mapa(
                "dataEfetiva", DATA, "dataAto", RelogioSistema.hoje(),
                "gestaoLocalDestinoId", c.gestaoB.getId(), "tipoContratacaoDestinoId", c.tipoB.getId(),
                "motivo", "Documento comprova responsabilidade do destino na data consultada",
                // CON-023/PER-012: conceder(...) criou a autorização ATO-HTTP.
                "referenciaSei", "SEI-REDIS-TRANSFERENCIA", "autorizacaoAdministrativa", "ATO-HTTP",
                "mapeamentos", List.of(mapa("composicaoOrigemId", c.composicaoA.getId(),
                        "lotacaoDestinoId", c.lotacaoB.getId(), "jornadaDestinoId", c.jornadaB.getId(),
                        "cargoDestinoId", c.cargoB.getId()))), 200, "Commit temporal invalida os dois caches");
        for (int repeticao = 0; repeticao < 2; repeticao++) {
            assertEquals(0, consultar(c.localA, path, "Origem revogada, inclusive em HIT").path("totalElements").asLong());
            var destino = consultar(c.localB, path, "Destino autorizado, inclusive em HIT");
            assertEquals(1, destino.path("totalElements").asLong());
            var linha = destino.path("content").get(0);
            assertEquals(id, linha.path("id").asLong());
            assertEquals(c.gestaoB.getId().longValue(), linha.path("escopoHistoricoEfetivo").path("gestaoLocalId").asLong());
            assertEquals(c.gestaoA.getId().longValue(), linha.path("gestaoLocalId").asLong(),
                    "A fotografia original permanece identificada separadamente");
        }
        assertEquals(c.gestaoA.getId(), emTransacao(() -> em.find(Frequencia.class, id).getGestaoLocalSnapshotId()));
    }
}
