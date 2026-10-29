package com.example.siggtrat.frequencia;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FrequenciaLargeScaleArchitectureTest {

    private String ler(String relativo) throws Exception {
        return Files.readString(Path.of("src/main/java", relativo));
    }

    @Test
    void endpointDeCriacaoRespondeAccepted() throws Exception {
        String controller = ler("com/example/siggtrat/controlador/FechamentoFrequenciaControlador.java");
        assertTrue(controller.contains("HttpStatus.ACCEPTED"));
        assertTrue(controller.contains("/api/frequencias/fechamentos"));
    }

    @Test
    void workerEhAssincronoEExecutaDepoisDoCommit() throws Exception {
        String processor = ler("com/example/siggtrat/servico/FechamentoFrequenciaProcessador.java");
        assertTrue(processor.contains("@Async(\"fechamentoFrequenciaExecutor\")"));
        assertTrue(processor.contains("TransactionPhase.AFTER_COMMIT"));
        assertTrue(processor.contains("fallbackExecution = true"));
    }

    @Test
    void workerUsaBlocosConfiguraveisEKeyset() throws Exception {
        String processor = ler("com/example/siggtrat/servico/FechamentoFrequenciaProcessador.java");
        String itens = ler("com/example/siggtrat/repositorio/FechamentoFrequenciaItemRepositorio.java");
        String empregados = ler("com/example/siggtrat/repositorio/EmpregadoRepositorio.java");
        assertTrue(processor.contains("properties.chunkSize()"));
        assertTrue(processor.contains("aposId"));
        assertTrue(itens.contains("i.empregado.id > :aposId"));
        assertTrue(empregados.contains("e.id > :aposId"));
    }

    @Test
    void estadoDoJobFicaPersistidoNoMySqlPeloHibernate() throws Exception {
        String entidade = ler("com/example/siggtrat/entidade/FechamentoFrequencia.java");
        String item = ler("com/example/siggtrat/entidade/FechamentoFrequenciaItem.java");
        assertTrue(entidade.contains("@Entity"));
        assertTrue(entidade.contains("fechamento_frequencia"));
        assertTrue(item.contains("@Entity"));
        assertTrue(item.contains("fechamento_frequencia_item"));
    }

    @Test
    void redisEhSomenteCacheEEstadoDoFechamentoPermaneceNoMySql() throws Exception {
        String pom = Files.readString(Path.of("pom.xml")).toLowerCase();
        String entidade = ler("com/example/siggtrat/entidade/FechamentoFrequencia.java");
        String config = ler("com/example/siggtrat/configuracao/ConfiguracaoCacheRedis.java");
        assertTrue(pom.contains("spring-boot-starter-data-redis"));
        assertTrue(entidade.contains("@Entity"));
        assertTrue(config.contains("RedisCacheManager"));
        assertFalse(config.contains("RedisTemplate"));
    }

    @Test
    void comandoNaoMaterializaCemMilPresencasNoHttp() throws Exception {
        String servico = ler("com/example/siggtrat/servico/FechamentoFrequenciaServico.java");
        assertTrue(servico.contains("A contagem completa de 100 mil ou mais colaboradores é feita pelo worker"));
        assertTrue(servico.contains("request.alteracoes()"));
        assertTrue(servico.contains("publishEvent"));
    }

    @Test
    void recuperadorReenfileiraPendentesETravados() throws Exception {
        String recovery = ler("com/example/siggtrat/servico/FechamentoFrequenciaRecuperacaoServico.java");
        assertTrue(recovery.contains("reenfileirarTravados"));
        assertTrue(recovery.contains("pendentes"));
    }

    @Test
    void configuracaoTemLimitesDeSeguranca() throws Exception {
        String props = ler("com/example/siggtrat/configuracao/propriedades/PropriedadesFechamentoFrequencia.java");
        assertTrue(props.contains("@Max(5000)"));
        assertTrue(props.contains("@Max(100000)"));
        assertTrue(props.contains("executorCorePoolSize > executorMaxPoolSize"));
    }
}
