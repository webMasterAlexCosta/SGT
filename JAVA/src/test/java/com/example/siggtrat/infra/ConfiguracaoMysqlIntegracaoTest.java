package com.example.siggtrat.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.context.DynamicPropertyRegistry;

/**
 * RI06: exercita os dois registradores reais em JVMs com ambiente controlado.
 * Nenhuma conexão ou contexto Spring é iniciado: a configuração deve recusar
 * entradas incompletas antes de entregar uma identidade ao pool.
 */
class ConfiguracaoMysqlIntegracaoTest {
    private static final String HTTP = "com.example.siggtrat.auditoria20261029.AuditoriaHttpSuporte";
    private static final String JPA = "com.example.siggtrat.integration.MySqlIntegrationTestSupport";

    static Stream<Arguments> cenarios() {
        return Stream.of(HTTP, JPA).flatMap(suporte -> Stream.of(
                "SEM_USUARIO", "USUARIO_VAZIO", "SEM_SENHA", "SENHA_VAZIA", "ROOT",
                "USUARIO_NA_URL", "SENHA_NA_URL", "SEM_URL", "SEM_CONFIGURACAO",
                "PROPRIEDADES_VALIDAS", "AMBIENTE_VALIDO")
                .map(cenario -> Arguments.of(suporte, cenario)));
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("cenarios")
    void resolveIdentidadeExplicitaSemFallbackAdministrativo(String suporte, String cenario) throws Exception {
        String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        var processo = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-Xmx96m", "-XX:ActiveProcessorCount=1", "-cp", classpath,
                Probe.class.getName(), suporte, cenario).redirectErrorStream(true);
        processo.environment().keySet().removeIf(k -> k.startsWith("INTEGRATION_MYSQL_"));
        processo.environment().remove("JAVA_TOOL_OPTIONS");
        processo.environment().remove("JDK_JAVA_OPTIONS");
        if (cenario.equals("AMBIENTE_VALIDO")) {
            processo.environment().put("INTEGRATION_MYSQL_URL", "jdbc:mysql://127.0.0.1:33076/sgt8045_candidata");
            processo.environment().put("INTEGRATION_MYSQL_USER", "sgt_validacao_8045");
            processo.environment().put("INTEGRATION_MYSQL_PASSWORD", "senha-sintetica-probe");
        }
        Process filho = processo.start();
        boolean terminou = filho.waitFor(15, TimeUnit.SECONDS);
        if (!terminou) filho.destroyForcibly();
        assertThat(terminou).as("A configuração termina sem depender de Docker ou banco").isTrue();
        String saida = new String(filho.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        boolean valido = cenario.endsWith("VALIDAS") || cenario.endsWith("VALIDO");
        assertThat(filho.exitValue()).as("Cenário %s / %s: %s", suporte, cenario, saida)
                .isEqualTo(valido ? 0 : 23);
        if (valido) {
            assertThat(saida).contains("USUARIO=sgt_validacao_8045", "SENHA_EXPLICITA=true");
        } else {
            assertThat(saida).contains("CONFIGURACAO_RECUSADA");
        }
        assertThat(saida).doesNotContain("senha-sintetica-probe");
    }

    /** Processo filho sem aplicação, conexão ou container. */
    public static final class Probe {
        public static void main(String[] args) throws Throwable {
            String suporte = args[0];
            String cenario = args[1];
            if (!cenario.equals("AMBIENTE_VALIDO")) {
                System.setProperty("integration.mysql.url", "jdbc:mysql://127.0.0.1:33076/sgt8045_candidata");
                System.setProperty("integration.mysql.user", "sgt_validacao_8045");
                System.setProperty("integration.mysql.password", "senha-sintetica-probe");
            }
            switch (cenario) {
                case "SEM_USUARIO" -> System.clearProperty("integration.mysql.user");
                case "USUARIO_VAZIO" -> System.setProperty("integration.mysql.user", " ");
                case "SEM_SENHA" -> System.clearProperty("integration.mysql.password");
                case "SENHA_VAZIA" -> System.setProperty("integration.mysql.password", "");
                case "SEM_URL" -> System.clearProperty("integration.mysql.url");
                case "SEM_CONFIGURACAO" -> {
                    System.clearProperty("integration.mysql.url");
                    System.clearProperty("integration.mysql.user");
                    System.clearProperty("integration.mysql.password");
                }
                case "ROOT" -> System.setProperty("integration.mysql.user", "root");
                case "USUARIO_NA_URL" -> System.setProperty("integration.mysql.url",
                        "jdbc:mysql://127.0.0.1:33076/sgt8045_candidata?user=root");
                case "SENHA_NA_URL" -> System.setProperty("integration.mysql.url",
                        "jdbc:mysql://127.0.0.1:33076/sgt8045_candidata?password=senha-sintetica-probe");
                default -> { }
            }
            try {
                Class<?> tipo = Class.forName(suporte);
                var metodo = tipo.getDeclaredMethod(suporte.endsWith("AuditoriaHttpSuporte")
                        ? "banco" : "databaseProperties", DynamicPropertyRegistry.class);
                metodo.setAccessible(true);
                Map<String, Object> propriedades = new HashMap<>();
                metodo.invoke(null, (DynamicPropertyRegistry) (nome, valor) -> propriedades.put(nome, valor.get()));
                System.out.println("USUARIO=" + propriedades.get("spring.datasource.username"));
                System.out.println("SENHA_EXPLICITA=" + "senha-sintetica-probe".equals(
                        propriedades.get("spring.datasource.password")));
            } catch (Throwable falha) {
                while ((falha instanceof ExceptionInInitializerError || falha instanceof InvocationTargetException)
                        && falha.getCause() != null) falha = falha.getCause();
                if (falha instanceof IllegalArgumentException || falha instanceof IllegalStateException) {
                    System.out.println("CONFIGURACAO_RECUSADA=" + falha.getClass().getSimpleName());
                    System.exit(23);
                }
                throw falha;
            }
        }
    }
}
