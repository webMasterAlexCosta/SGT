package com.example.siggtrat.seguranca;

import com.example.siggtrat.configuracao.ConfiguracaoSeguranca;
import com.example.siggtrat.configuracao.FiltroLimitacaoRequisicoes;
import com.example.siggtrat.configuracao.propriedades.PropriedadesCors;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Cadeia real de segurança; somente a validação externa do JWT é substituída. */
class CsrfJwtCadeiaRegressaoTest {
    private AnnotationConfigWebApplicationContext contexto;
    private MockMvc mvc;
    private Cookie cookieCsrf;
    private String tokenCsrf;

    @BeforeEach
    void preparar() {
        contexto = new AnnotationConfigWebApplicationContext();
        contexto.setServletContext(new MockServletContext());
        contexto.register(ConfiguracaoSeguranca.class, ConfiguracaoTeste.class);
        contexto.refresh();
        mvc = MockMvcBuilders.webAppContextSetup(contexto)
                .addFilters(contexto.getBean("springSecurityFilterChain", Filter.class)).build();
    }

    @AfterEach
    void fechar() {
        SecurityContextHolder.clearContext();
        if (contexto != null) contexto.close();
    }

    @Test
    void getAutenticadoNaoInvalidaTokenCsrfDaSessaoJwtAtual() throws Exception {
        obterCsrf();
        assertEquals(200, executar(get("/api/teste-csrf/leitura"), true, false).getResponse().getStatus());

        assertEquals(200, executar(post("/api/teste-csrf/mutacao"), true, true).getResponse().getStatus(),
                "GET de uma sessão JWT já autenticada não deve consumir o CSRF obtido pelo cliente");
    }

    @Test
    void doisPostsSeguidosAceitamMesmoTokenCsrfNaMesmaSessaoJwt() throws Exception {
        obterCsrf();
        assertEquals(200, executar(post("/api/teste-csrf/mutacao"), true, true).getResponse().getStatus());

        assertEquals(200, executar(post("/api/teste-csrf/mutacao"), true, true).getResponse().getStatus(),
                "Uma escrita comum não deve exigir novo bootstrap CSRF para a escrita seguinte");
    }

    @Test
    void postSemCabecalhoCsrfContinuaBloqueadoComJwtValido() throws Exception {
        obterCsrf();

        assertEquals(403, executar(post("/api/teste-csrf/mutacao"), true, false).getResponse().getStatus());
    }

    @Test
    void autenticacaoJwtNaoCriaSessaoHttpNemAutorizaOutraRequisicaoSemJwt() throws Exception {
        MvcResult autenticada = executar(get("/api/teste-csrf/leitura"), true, false);
        assertEquals(200, autenticada.getResponse().getStatus());
        assertNull(autenticada.getRequest().getSession(false));

        assertEquals(401, executar(get("/api/teste-csrf/leitura"), false, false).getResponse().getStatus());
    }

    private void obterCsrf() throws Exception {
        MvcResult resultado = executar(get("/api/autenticacao/csrf"), true, false);
        assertEquals(200, resultado.getResponse().getStatus());
        tokenCsrf = resultado.getResponse().getContentAsString();
        assertNotNull(cookieCsrf);
        assertEquals(cookieCsrf.getValue(), tokenCsrf);
    }

    private MvcResult executar(MockHttpServletRequestBuilder requisicao, boolean autenticada,
                               boolean enviarCsrf) throws Exception {
        if (autenticada) requisicao.header("Authorization", "Bearer JWT-SINTETICO");
        if (cookieCsrf != null) requisicao.cookie(cookieCsrf);
        if (enviarCsrf) requisicao.header("X-XSRF-TOKEN", tokenCsrf);
        MvcResult resultado = mvc.perform(requisicao).andReturn();
        // Reproduz o navegador: Set-Cookie com Max-Age=0 remove o cookie.
        // Reenviar manualmente um cookie já removido esconderia esta regressão.
        for (Cookie cookie : resultado.getResponse().getCookies()) {
            if ("XSRF-TOKEN".equals(cookie.getName())) cookieCsrf = cookie.getMaxAge() == 0 ? null : cookie;
        }
        return resultado;
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    static class ConfiguracaoTeste {
        @Bean PropriedadesCors propriedadesCors() { return new PropriedadesCors("http://localhost"); }
        @Bean PontoEntradaAutenticacaoRest entrada() { return new PontoEntradaAutenticacaoRest(new ObjectMapper()); }
        @Bean TratadorAcessoNegadoRest acessoNegado() { return new TratadorAcessoNegadoRest(); }
        @Bean ControladorTeste controlador() { return new ControladorTeste(); }
        @Bean UserDetailsService usuarios() {
            return username -> User.withUsername(username).password("hash-sintetico").authorities("ROLE_ADMINISTRADOR").build();
        }
        @Bean FiltroLimitacaoRequisicoes limitacao() throws Exception {
            FiltroLimitacaoRequisicoes filtro = mock(FiltroLimitacaoRequisicoes.class);
            doAnswer(i -> {
                FilterChain cadeia = i.getArgument(2);
                cadeia.doFilter(i.getArgument(0), i.getArgument(1));
                return null;
            }).when(filtro).doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));
            return filtro;
        }
        @Bean FiltroAutenticacaoJwt jwt() throws Exception {
            FiltroAutenticacaoJwt filtro = mock(FiltroAutenticacaoJwt.class);
            doAnswer(i -> {
                HttpServletRequest requisicao = i.getArgument(0);
                if ("Bearer JWT-SINTETICO".equals(requisicao.getHeader("Authorization"))) {
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken("usuario-sintetico", null, List.of()));
                }
                FilterChain cadeia = i.getArgument(2);
                cadeia.doFilter(requisicao, i.getArgument(1));
                return null;
            }).when(filtro).doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));
            return filtro;
        }
    }

    @RestController
    static class ControladorTeste {
        @GetMapping("/api/autenticacao/csrf") String csrf(CsrfToken token) { return token.getToken(); }
        @GetMapping("/api/teste-csrf/leitura") String leitura() { return "ok"; }
        @PostMapping("/api/teste-csrf/mutacao") String mutacao() { return "ok"; }
    }
}
