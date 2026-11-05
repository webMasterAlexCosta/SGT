package com.example.siggtrat.painel;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PainelEscalaGrandeArchitectureTest {

    private String ler(String relativo) throws Exception {
        return Files.readString(Path.of("src/main/java", relativo));
    }

    @Test
    void painelPossuiEndpointAssincronoPersistido() throws Exception {
        String controller = ler("com/example/siggtrat/controlador/PainelCalculoControlador.java");
        String entidade = ler("com/example/siggtrat/entidade/PainelCalculo.java");
        assertTrue(controller.contains("/api/painel/calculos"));
        assertTrue(controller.contains("ResponseEntity.status"));
        assertTrue(entidade.contains("@Entity"));
        assertTrue(entidade.contains("painel_calculo"));
        assertTrue(entidade.contains("LONGTEXT"));
    }

    @Test
    void calculoNaoDependeDoSecurityContextDaThreadAssincrona() throws Exception {
        String servico = ler("com/example/siggtrat/servico/PainelServico.java");
        String processor = ler("com/example/siggtrat/servico/PainelCalculoProcessador.java");
        assertTrue(servico.contains("PainelCalculoContexto"));
        assertTrue(servico.contains("calcularGestao("));
        assertTrue(processor.contains("descritor.contexto()"));
    }

    @Test
    void workerTemExecutorDedicadoEPosCommit() throws Exception {
        String processor = ler("com/example/siggtrat/servico/PainelCalculoProcessador.java");
        String config = ler("com/example/siggtrat/configuracao/ConfiguracaoAssincrona.java");
        assertTrue(processor.contains("@Async(\"painelExecutor\")"));
        assertTrue(processor.contains("TransactionPhase.AFTER_COMMIT"));
        assertTrue(config.contains("@Bean(name = \"painelExecutor\")"));
        assertTrue(config.contains("setCorePoolSize(1)"));
    }

    @Test
    void cachePersistidoUsaStaleWhileRevalidate() throws Exception {
        String servico = ler("com/example/siggtrat/servico/PainelCalculoServico.java");
        assertTrue(servico.contains("cacheMinutos"));
        assertTrue(servico.contains("payloadValido"));
        assertTrue(servico.contains("dadosPersistidos != null"));
        assertTrue(servico.contains("HttpStatus.ACCEPTED"));
        assertTrue(servico.contains("desatualizado"));
    }

    @Test
    void recuperadorReenfileiraPendentesETravados() throws Exception {
        String recovery = ler("com/example/siggtrat/servico/PainelCalculoRecuperacaoServico.java");
        assertTrue(recovery.contains("buscarProcessamentosTravados"));
        assertTrue(recovery.contains("findTop20ByStatusOrderByAtualizadoEmAsc"));
        assertTrue(recovery.contains("EventoPainelCalculoSolicitado"));
    }

    @Test
    void consultasDeFrequenciaDoPainelPossuemIndicesCompostos() throws Exception {
        String frequencia = ler("com/example/siggtrat/entidade/Frequencia.java");
        String adicional = ler("com/example/siggtrat/entidade/FrequenciaAdicional.java");
        assertTrue(frequencia.contains("idx_frequencia_data_status_empregado"));
        assertTrue(frequencia.contains("idx_frequencia_empregado_data_status"));
        assertTrue(adicional.contains("idx_freq_adicional_data_tipo_status_emp"));
    }

    @Test
    void sqlDoPainelNaoAplicaFuncaoAoStatusDaFrequencia() throws Exception {
        String repositorio = ler("com/example/siggtrat/repositorio/PainelRepositorio.java");
        assertFalse(repositorio.contains("UPPER(TRIM(f.status))"));
        assertFalse(repositorio.contains("UPPER(TRIM(f2.status))"));
        assertTrue(repositorio.contains("f.status IN"));
    }

    @Test
    void painelUsaRedisComoCacheSemSubstituirPersistenciaDoMySql() throws Exception {
        String pom = Files.readString(Path.of("pom.xml")).toLowerCase();
        String painel = ler("com/example/siggtrat/servico/PainelServico.java");
        String entidade = ler("com/example/siggtrat/entidade/PainelCalculo.java");
        String redisConfig = ler("com/example/siggtrat/configuracao/ConfiguracaoCacheRedis.java");
        assertTrue(pom.contains("spring-boot-starter-data-redis"));
        assertTrue(painel.contains("@Cacheable"));
        assertTrue(redisConfig.contains("Jackson2JsonRedisSerializer"));
        assertTrue(redisConfig.contains("constructCollectionType"));
        assertFalse(redisConfig.contains("GenericJackson2JsonRedisSerializer"));
        assertTrue(entidade.contains("@Entity"));
        assertTrue(entidade.contains("painel_calculo"));
    }

    @Test
    void payloadDoPainelNaoPodeConcluirSemDadosERecuperaCachesDaVersaoIntermediaria() throws Exception {
        String dto = ler("com/example/siggtrat/dto/PainelGestaoDTO.java");
        String servico = ler("com/example/siggtrat/servico/PainelCalculoServico.java");
        String processor = ler("com/example/siggtrat/servico/PainelCalculoProcessador.java");
        String state = ler("com/example/siggtrat/servico/PainelCalculoEstadoServico.java");

        assertTrue(dto.contains("@JsonAlias(\"registrosLancados\") long registrosEsperados"));
        assertFalse(dto.contains("@JsonProperty(\"registrosEsperados\")"));
        assertTrue(servico.contains("normalizarRegistrosLancados"));
        assertTrue(servico.contains("EsquemaPainel.VERSION"));
        assertTrue(servico.contains("reenfileirando automaticamente"));
        assertTrue(processor.contains("terminou sem produzir indicadores"));
        assertTrue(state.contains("Não é permitido concluir o painel sem payload válido"));
    }
}
