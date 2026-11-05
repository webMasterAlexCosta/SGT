package com.example.siggtrat.seguranca;

import com.example.siggtrat.configuracao.ConfiguracaoSeguranca;
import com.example.siggtrat.configuracao.FiltroLimitacaoRequisicoes;
import com.example.siggtrat.configuracao.propriedades.PropriedadesCors;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class SegurancaAdversarialEtapa12Test {

    @Test
    void corsRejeitaOrigemHostilMesmoComCredenciaisHabilitadas() {
        CorsConfiguration cors = cors("https://app.exemplo.gov.br,https://admin.exemplo.gov.br");

        assertNull(cors.checkOrigin("https://evil.example"));
        assertEquals("https://app.exemplo.gov.br", cors.checkOrigin("https://app.exemplo.gov.br"));
        assertTrue(Boolean.TRUE.equals(cors.getAllowCredentials()));
        assertFalse(cors.getAllowedOrigins().contains("*"));
    }

    @Test
    void corsNaoAutorizaTrace() {
        CorsConfiguration cors = cors("https://app.exemplo.gov.br");

        assertNull(cors.checkHttpMethod(org.springframework.http.HttpMethod.TRACE));
        assertNotNull(cors.checkHttpMethod(org.springframework.http.HttpMethod.GET));
    }

    @Test
    void cadeiaMantemTraceNegadoSessaoStatelessECabecalhosEndurecidos() throws Exception {
        String fonte = Files.readString(Path.of(
                "src/main/java/com/example/siggtrat/configuracao/ConfiguracaoSeguranca.java"));

        assertTrue(fonte.contains("HttpMethod.TRACE"));
        assertTrue(fonte.contains(".denyAll()"));
        assertTrue(fonte.contains("SessionCreationPolicy.STATELESS"));
        assertTrue(fonte.contains("FrameOptionsConfig::deny"));
        assertTrue(fonte.contains(".httpStrictTransportSecurity"));
        assertTrue(fonte.contains(".contentSecurityPolicy"));
        assertTrue(fonte.contains(".referrerPolicy"));
        assertTrue(fonte.contains(".permissionsPolicyHeader"));
    }

    private CorsConfiguration cors(String origens) {
        ConfiguracaoSeguranca configuracao = new ConfiguracaoSeguranca(
                mock(FiltroAutenticacaoJwt.class),
                mock(FiltroLimitacaoRequisicoes.class),
                mock(PontoEntradaAutenticacaoRest.class),
                mock(TratadorAcessoNegadoRest.class),
                new PropriedadesCors(origens));
        MockHttpServletRequest requisicao = new MockHttpServletRequest("GET", "/api/teste");
        CorsConfiguration cors = configuracao.fonteConfiguracaoCors().getCorsConfiguration(requisicao);
        assertNotNull(cors);
        return cors;
    }
}
