package com.example.siggtrat.configuracao.demonstracao;

import org.junit.jupiter.api.Test;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.mock.env.MockEnvironment;
import java.util.Properties;
import static org.junit.jupiter.api.Assertions.*;

class SelecaoBancoDemonstracaoRjTest {
    private EnvironmentPostProcessor seletor() {
        return assertDoesNotThrow(() -> (EnvironmentPostProcessor) Class.forName(
                "com.example.siggtrat.configuracao.demonstracao.SelecaoBancoDemonstracaoRj").getConstructor().newInstance(),
                "Seletor do banco RJ ainda não implementado");
    }
    private MockEnvironment ambiente() {
        return new MockEnvironment().withProperty("spring.datasource.url", "jdbc:postgresql://127.0.0.1:5432/sgt")
                .withProperty("spring.datasource.username", "sgt_app").withProperty("spring.datasource.password", "normal-ficticia")
                .withProperty("APP_DEMO_RJ_DB_URL", "jdbc:postgresql://127.0.0.1:5432/sgt_demo_rj")
                .withProperty("APP_DEMO_RJ_DB_USER", "sgt_demo_app").withProperty("APP_DEMO_RJ_DB_PASSWORD", "demo-ficticia");
    }
    @Test void padraoDesligadoPreservaBancoNormal() {
        var e=ambiente();seletor().postProcessEnvironment(e,null);
        assertEquals("jdbc:postgresql://127.0.0.1:5432/sgt",e.getProperty("spring.datasource.url"));
        assertEquals("normal-ficticia",e.getProperty("spring.datasource.password"));
    }
    @Test void ligadoSelecionaSomenteCredenciaisDemo() {
        var e=ambiente().withProperty("APP_DEMO_RJ_ENABLED","true");seletor().postProcessEnvironment(e,null);
        assertEquals("jdbc:postgresql://127.0.0.1:5432/sgt_demo_rj",e.getProperty("spring.datasource.url"));
        assertEquals("sgt_demo_app",e.getProperty("spring.datasource.username"));
        assertEquals("demo-ficticia",e.getProperty("spring.datasource.password"));
        assertEquals("true",e.getProperty("app.demo-rj.enabled"));
    }
    @Test void falseNaoExigeConfiguracaoDemo() {
        var e=new MockEnvironment().withProperty("APP_DEMO_RJ_ENABLED","false").withProperty("spring.datasource.url","normal");
        seletor().postProcessEnvironment(e,null);assertEquals("normal",e.getProperty("spring.datasource.url"));
    }
    @Test void recusaMesmoBancoMesmoComAliasEQueryDiferente() {
        var e=ambiente().withProperty("APP_DEMO_RJ_ENABLED","true")
                .withProperty("APP_DEMO_RJ_DB_URL","jdbc:postgresql://localhost:5432/sgt?currentSchema=outra");
        assertThrows(IllegalStateException.class,()->seletor().postProcessEnvironment(e,null));
        assertEquals("normal-ficticia",e.getProperty("spring.datasource.password"));
    }
    @Test void recusaDemonstracaoEmProducao() {
        var e=ambiente().withProperty("APP_DEMO_RJ_ENABLED","true");e.setActiveProfiles("prod");
        assertThrows(IllegalStateException.class,()->seletor().postProcessEnvironment(e,null));
    }
    @Test void recusaSenhaDemoAusenteSemRecorrerASenhaNormal() {
        var e=ambiente().withProperty("APP_DEMO_RJ_ENABLED","true").withProperty("APP_DEMO_RJ_DB_PASSWORD","");
        assertThrows(IllegalStateException.class,()->seletor().postProcessEnvironment(e,null));
        assertEquals("normal-ficticia",e.getProperty("spring.datasource.password"));
    }
    @Test void mensagemDeConfiguracaoInvalidaNaoExibeUrlOuSenha() {
        var e=ambiente().withProperty("APP_DEMO_RJ_ENABLED","true").withProperty("APP_DEMO_RJ_DB_URL","nao-e-jdbc-demo-ficticia");
        var error=assertThrows(IllegalStateException.class,()->seletor().postProcessEnvironment(e,null));
        assertFalse(error.getMessage().contains("nao-e-jdbc"));assertFalse(error.getMessage().contains("demo-ficticia"));
    }
    @Test void separaRedisDoBancoNormal() {
        var e=ambiente().withProperty("APP_DEMO_RJ_ENABLED","true").withProperty("spring.data.redis.database","0");
        seletor().postProcessEnvironment(e,null);assertEquals("15",e.getProperty("spring.data.redis.database"));
    }
    @Test void recusaMesmoRedisNosDoisModos() {
        var e=ambiente().withProperty("APP_DEMO_RJ_ENABLED","true").withProperty("spring.data.redis.database","15");
        assertThrows(IllegalStateException.class,()->seletor().postProcessEnvironment(e,null));
    }
    @Test void registradoNoBootstrapAntesDoDatasource() throws Exception {
        var stream=getClass().getClassLoader().getResourceAsStream("META-INF/spring.factories");
        assertNotNull(stream,"Seletor precisa estar registrado no bootstrap");
        var p=new Properties();try(stream){p.load(stream);}
        assertTrue(p.getProperty("org.springframework.boot.EnvironmentPostProcessor","").contains("SelecaoBancoDemonstracaoRj"));
    }
}
