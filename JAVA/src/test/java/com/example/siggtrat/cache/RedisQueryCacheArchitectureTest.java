package com.example.siggtrat.cache;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RedisQueryCacheArchitectureTest {

    private String ler(String relativo) throws Exception {
        return Files.readString(Path.of("src/main/java", relativo));
    }

    @Test
    void workerDoPainelUsaCaminhoCacheado() throws Exception {
        String processor = ler("com/example/siggtrat/servico/PainelCalculoProcessador.java");
        String cache = ler("com/example/siggtrat/servico/ConsultaCacheRedisServico.java");
        assertTrue(processor.contains("calcularGestaoCacheado"));
        assertTrue(cache.contains("NomesCache.PAINEL_GESTAO"));
    }

    @Test
    void endpointsPesadosDaFrequenciaUsamRedisESkipCount() throws Exception {
        String empregados = ler("com/example/siggtrat/servico/EmpregadoServico.java");
        String controller = ler("com/example/siggtrat/controlador/FrequenciaControlador.java");
        String servico = ler("com/example/siggtrat/servico/FrequenciaServico.java");
        assertTrue(empregados.contains("empregadosFrequencia"));
        assertTrue(controller.contains("boolean skipCount"));
        assertTrue(servico.contains("frequenciaConsultaRepositorio"));
        assertTrue(servico.contains("redisQueryCacheServico.frequencias"));
    }

    @Test
    void listaDeEmpregadosCacheiaPaginaETotalSeparadamente() throws Exception {
        String empregados = ler("com/example/siggtrat/servico/EmpregadoServico.java");
        String nomes = ler("com/example/siggtrat/configuracao/NomesCache.java");
        assertTrue(empregados.contains("empregadosResumoTotal"));
        assertTrue(empregados.contains("empregadosResumo("));
        assertTrue(nomes.contains("EMPREGADOS_RESUMO_TOTAL"));
    }

    @Test
    void getsJsonAutenticadosPossuemCacheRedisSeguro() throws Exception {
        String filtro = ler("com/example/siggtrat/configuracao/FiltroCacheConsultaHttpRedis.java");
        String nomes = ler("com/example/siggtrat/configuracao/NomesCache.java");
        String config = ler("com/example/siggtrat/configuracao/ConfiguracaoCacheRedis.java");

        assertTrue(filtro.contains("\"GET\".equalsIgnoreCase"));
        assertTrue(filtro.contains("/api/autenticacao/"));
        assertTrue(filtro.contains("/api/perfil/"));
        assertTrue(filtro.contains("/api/usuarios/meu-perfil"));
        assertTrue(filtro.contains("/api/frequencias/fechamentos/"));
        assertTrue(filtro.contains("/api/painel/calculos/"));
        assertTrue(filtro.contains("/api/relatorios/exportacoes/"),
                "polling e download assíncronos não podem passar pelo ContentCachingResponseWrapper");
        assertTrue(filtro.contains("uri.endsWith(\"/exportacao\")"),
                "downloads diretos PDF/XLSX também devem ignorar o cache HTTP de JSON");
        assertTrue(filtro.contains("cacheKeyServico.consulta"));
        assertTrue(filtro.contains("APPLICATION_JSON_VALUE"));
        assertTrue(nomes.contains("API_GET_RESPONSES"));
        assertTrue(config.contains("RespostaCacheConsultaHttp.class"));
    }

    @Test
    void existeDiagnosticoDeConectividadeComRedis() throws Exception {
        String controller = ler("com/example/siggtrat/controlador/CacheDiagnosticoControlador.java");
        String servico = ler("com/example/siggtrat/servico/CacheDiagnosticoServico.java");
        String config = ler("com/example/siggtrat/configuracao/ConfiguracaoCacheRedis.java");
        assertTrue(controller.contains("/api/cache"));
        assertTrue(servico.contains("connection.ping()"));
        assertTrue(controller.contains("PERFIL_ADMINISTRADOR"));
        assertTrue(config.contains("CacheErrorHandler"));
        assertTrue(config.contains("Consulta seguirá sem cache"));
    }
}
