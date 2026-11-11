package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.configuracao.NomesCache;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ScanOptions;

import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Caracterização HTTP local sequencial; não representa carga concorrente nem corpus nacional. */
final class CaracterizacaoHttpJar {
    private CaracterizacaoHttpJar() {}
    @FunctionalInterface interface Leitura { HttpResponse<byte[]> executar(String caminho) throws Exception; }
    @FunctionalInterface interface Oraculo { void conferir(String nome, HttpResponse<byte[]> resposta) throws Exception; }
    record Rota(String nome, String caminho, boolean cacheHttp) {}

    static Map<String, Object> executar(Process jar, List<Rota> rotas, Leitura leitura, Oraculo oraculo,
                                       RecursosJarObservador recursos) throws Exception {
        assertTrue(jar.isAlive());
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(obrigatorio("REDIS_HOST"),
                Integer.parseInt(obrigatorio("REDIS_PORT")));
        assertEquals("127.0.0.1", config.getHostName(), "Somente o laboratório local exclusivo");
        config.setPassword(obrigatorio("REDIS_PASSWORD"));
        config.setDatabase(Integer.parseInt(obrigatorio("REDIS_DATABASE")));
        assertEquals(0, config.getDatabase());
        LettuceConnectionFactory fabrica = new LettuceConnectionFactory(config);
        try {
            fabrica.afterPropertiesSet(); fabrica.start();
            try (RedisConnection redis = fabrica.getConnection()) {
                validarInstancia(redis);
                List<Map<String, Object>> amostras = new ArrayList<>();
                List<Map<String, Object>> limpezas = new ArrayList<>();
                long rssMax = 0;
                for (Rota rota : rotas) for (int ciclo = 1; ciclo <= 3; ciclo++) {
                    validarInstancia(redis);
                    long removidas = limparSomenteCachesNegocio(redis);
                    assertEquals(0, contarCache(redis, NomesCache.API_GET_RESPONSES));
                    assertEquals(0, contarCache(redis, NomesCache.FREQUENCIAS_CONSULTA));
                    limpezas.add(Map.of("rota", rota.nome(), "ciclo", ciclo, "chavesRemovidas", removidas));
                    for (int repeticao = 0; repeticao < 3; repeticao++) {
                        if (recursos != null) recursos.iniciarJanela();
                        long cpuAntes = cpu(jar), inicio = System.nanoTime();
                        HttpResponse<byte[]> resposta = leitura.executar(rota.caminho());
                        long nanos = System.nanoTime() - inicio, cpuDepois = cpu(jar);
                        Map<String, Object> recursosJanela = recursos == null
                                ? Map.of("estado", "NAO_SOLICITADO_SEM_ACEITE_DE_RECURSOS") : recursos.terminarJanela();
                        long rss = rss(jar); rssMax = Math.max(rssMax, rss);
                        assertEquals(200, resposta.statusCode(), rota.nome());
                        oraculo.conferir(rota.nome(), resposta);
                        String cache = resposta.headers().firstValue("X-SGT-Cache").orElse("BYPASS");
                        if (rota.cacheHttp()) assertEquals(repeticao == 0 ? "MISS" : "HIT", cache, rota.nome());
                        else assertEquals("BYPASS", cache, "Rota sensível/streaming não deve usar cache HTTP");
                        long cacheFrequencias = contarCache(redis, NomesCache.FREQUENCIAS_CONSULTA);
                        if (rota.nome().equals("frequencia")) assertTrue(cacheFrequencias > 0,
                                "Cache paginado interno aquecido; autorização continua sendo revalidada por HTTP");
                        Map<String, Object> a = new LinkedHashMap<>();
                        a.put("rota", rota.nome()); a.put("ciclo", ciclo); a.put("repeticao", repeticao);
                        a.put("estadoAplicacao", repeticao == 0 ? "FRIO" : "QUENTE");
                        a.put("nanos", nanos); a.put("status", resposta.statusCode()); a.put("bytes", resposta.body().length);
                        a.put("cacheHttp", cache); a.put("cacheFrequenciasChaves", cacheFrequencias);
                        a.put("cpuJarNanos", cpuDepois - cpuAntes); a.put("rssJarBytesAmostrado", rss);
                        a.put("recursosJvmPoolPostgres", recursosJanela);
                        amostras.add(Map.copyOf(a));
                    }
                }
                List<Map<String, Object>> resumos = new ArrayList<>();
                for (Rota rota : rotas) for (String estado : List.of("FRIO", "QUENTE")) {
                    var grupo = amostras.stream().filter(a -> a.get("rota").equals(rota.nome())
                            && a.get("estadoAplicacao").equals(estado)).toList();
                    var tempos = grupo.stream().mapToLong(a -> (Long) a.get("nanos")).sorted().toArray();
                    long soma = java.util.Arrays.stream(tempos).sum();
                    resumos.add(Map.of("rota", rota.nome(), "estadoAplicacao", estado, "amostras", tempos.length,
                            "p50Ms", percentil(tempos, .50), "p95Ms", percentil(tempos, .95), "p99Ms", percentil(tempos, .99),
                            "requisicoesPorSegundoEmServico", tempos.length * 1_000_000_000d / soma,
                            "errosHttp", 0));
                }
                return Map.of("escopo", "HTTP local real, um usuário, concorrência1, três ciclos por rota; sem SLA inferido",
                        "perfilRecursos", recursos == null ? "NAO_SOLICITADO" : "JMX_LOCAL_POSTGRES_REAL",
                        "cacheFrio", "Caches de negócio Redis vazios; não limpa sessões, tokens, revisões, SO nem PostgreSQL",
                        "percentis", "Nearest rank; amostra pequena, descritiva, sem extrapolação nacional",
                        "rssJarMaxAmostradoBytes", rssMax, "limpezas", limpezas, "resumos", resumos, "amostras", amostras);
            }
        } finally { fabrica.destroy(); }
    }

    private static void validarInstancia(RedisConnection redis) {
        var info = redis.serverCommands().info("server"); assertNotNull(info);
        assertEquals(obrigatorio("SGT_INTEGRATION_REDIS_PID"), info.getProperty("process_id"));
        assertEquals(obrigatorio("SGT_INTEGRATION_REDIS_RUN_ID"), info.getProperty("run_id"));
    }

    private static long limparSomenteCachesNegocio(RedisConnection redis) {
        long removidas = 0;
        for (String cache : NomesCache.CACHES_NEGOCIO) {
            List<byte[]> chaves = chaves(redis, cache);
            for (byte[] chave : chaves) {
                Long n = redis.keyCommands().unlink(chave); assertNotNull(n); removidas += n;
            }
            assertEquals(0, contarCache(redis, cache));
        }
        return removidas;
    }

    private static long contarCache(RedisConnection redis, String cache) { return chaves(redis, cache).size(); }

    private static List<byte[]> chaves(RedisConnection redis, String cache) {
        String prefixo = "sgt:cache:" + cache + ":";
        List<byte[]> chaves = new ArrayList<>();
        try (var cursor = redis.keyCommands().scan(ScanOptions.scanOptions().match(prefixo + "*").count(100).build())) {
            while (cursor.hasNext()) {
                byte[] chave = cursor.next();
                assertTrue(new String(chave, StandardCharsets.UTF_8).startsWith(prefixo));
                chaves.add(chave);
            }
        }
        return chaves;
    }

    private static String obrigatorio(String nome) {
        String valor = System.getenv(nome); assertNotNull(valor, "Ambiente explícito: " + nome);
        assertFalse(valor.isBlank(), "Ambiente explícito: " + nome); return valor;
    }
    private static long cpu(Process processo) {
        return processo.info().totalCpuDuration().orElseThrow(() -> new AssertionError("CPU do JAR indisponível")).toNanos();
    }
    private static long rss(Process processo) throws Exception {
        for (String linha : Files.readAllLines(Path.of("/proc", Long.toString(processo.pid()), "status"))) {
            if (linha.startsWith("VmRSS:")) return Long.parseLong(linha.trim().split("\\s+")[1]) * 1024;
        }
        throw new AssertionError("RSS do JAR indisponível");
    }
    private static double percentil(long[] tempos, double p) { return tempos[(int) Math.ceil(tempos.length * p) - 1] / 1_000_000d; }
}
