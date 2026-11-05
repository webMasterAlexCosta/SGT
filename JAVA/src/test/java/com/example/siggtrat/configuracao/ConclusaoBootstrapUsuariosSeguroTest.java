package com.example.siggtrat.configuracao;

import com.example.siggtrat.repositorio.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Contrato de configuração: criar contas privilegiadas exige escolha e segredo explícitos. */
class ConclusaoBootstrapUsuariosSeguroTest {
    private static final String GLOBAL = "app.seed.usuarios-globais-enabled";
    private static final String GESTORES = "app.seed.gestores-se-gestao-local-enabled";
    private static final String SENHA_GLOBAL = "app.seed.usuarios-globais-initial-password";
    private static final String SENHA_GESTORES = "app.seed.gestores-initial-password";

    @Test
    void configuracaoSemFlagsNaoRegistraBootstrapDeContasPrivilegiadas() {
        contexto().run(ctx -> {
            assertNull(ctx.getStartupFailure());
            assertFalse(ctx.containsBean("initUsuariosGlobais"));
            assertFalse(ctx.containsBean("initUsuariosGestoresSeEGestaoLocal"));
        });
    }

    @Test
    void defaultsDoArquivoDaAplicacaoMantemBootstrapDeContasDesabilitado() throws Exception {
        Map<String, Object> propriedades = propriedadesDeUsuarios();
        contexto().withInitializer(ctx -> ctx.getEnvironment().getPropertySources()
                .addLast(new MapPropertySource("defaults-reais-da-aplicacao", propriedades))).run(ctx -> {
            assertNull(ctx.getStartupFailure());
            assertFalse(ctx.containsBean("initUsuariosGlobais"));
            assertFalse(ctx.containsBean("initUsuariosGestoresSeEGestaoLocal"));
        });
    }

    @Test
    void defaultsDaAplicacaoNaoFornecemSenhaInicialPublica() throws Exception {
        Map<String, Object> propriedades = propriedadesDeUsuarios();
        MockEnvironment ambiente = new MockEnvironment();
        ambiente.getPropertySources().addLast(new MapPropertySource("defaults-reais-da-aplicacao", propriedades));
        assertEquals("", ambiente.getProperty(SENHA_GLOBAL));
        assertEquals("", ambiente.getProperty(SENHA_GESTORES));
    }

    @ParameterizedTest
    @ValueSource(strings = {"initUsuariosGlobais", "initUsuariosGestoresSeEGestaoLocal"})
    void ativacaoSemSenhaNaoUsaFallbackDaAnotacao(String bean) {
        contexto().withPropertyValues(GLOBAL + "=true", GESTORES + "=true").run(ctx -> {
            assertNull(ctx.getStartupFailure());
            IllegalStateException erro = assertThrows(IllegalStateException.class,
                    () -> ctx.getBean(bean, CommandLineRunner.class).run());
            assertTrue(erro.getMessage().toLowerCase().contains("senha"));
            verify(ctx.getBean(UsuarioAplicacaoRepositorio.class), never()).save(any());
        });
    }

    private ApplicationContextRunner contexto() {
        return new ApplicationContextRunner().withUserConfiguration(InicializadorDados.class)
                .withPropertyValues("app.seed.jornadas-operacionais-enabled=false",
                        "app.seed.unidades-operacionais-enabled=false", "auth.password-min-length=8")
                .withBean(StatusEmpregadoRepositorio.class, () -> mock(StatusEmpregadoRepositorio.class))
                .withBean(ConfiguracaoSistemaRepositorio.class, () -> mock(ConfiguracaoSistemaRepositorio.class))
                .withBean(SuperintendenciaEstadualRepositorio.class, () -> mock(SuperintendenciaEstadualRepositorio.class))
                .withBean(GestaoLocalRepositorio.class, () -> mock(GestaoLocalRepositorio.class))
                .withBean(UsuarioAplicacaoRepositorio.class, () -> mock(UsuarioAplicacaoRepositorio.class))
                .withBean(PasswordEncoder.class, () -> mock(PasswordEncoder.class))
                .withBean(PlatformTransactionManager.class, () -> {
                    PlatformTransactionManager tx = mock(PlatformTransactionManager.class);
                    when(tx.getTransaction(any(TransactionDefinition.class))).thenReturn(new SimpleTransactionStatus());
                    return tx;
                });
    }

    private Map<String, Object> propriedadesDeUsuarios() throws Exception {
        Properties fonte = PropertiesLoaderUtils.loadProperties(new ClassPathResource("application.properties"));
        Map<String, Object> resultado = new LinkedHashMap<>();
        for (String chave : new String[]{GLOBAL, GESTORES, SENHA_GLOBAL, SENHA_GESTORES}) {
            resultado.put(chave, fonte.getProperty(chave));
        }
        return resultado;
    }
}
