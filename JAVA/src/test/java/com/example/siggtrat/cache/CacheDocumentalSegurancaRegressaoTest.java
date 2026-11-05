package com.example.siggtrat.cache;

import com.example.siggtrat.configuracao.FiltroCacheConsultaHttpRedis;
import com.example.siggtrat.configuracao.NomesCache;
import com.example.siggtrat.configuracao.ServicoChaveCacheRedis;
import com.example.siggtrat.repositorio.UsuarioAplicacaoRepositorio;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class CacheDocumentalSegurancaRegressaoTest {
    private final FiltroCacheConsultaHttpRedis filtro = new FiltroCacheConsultaHttpRedis(
            new ConcurrentMapCacheManager(NomesCache.API_GET_RESPONSES),
            new ServicoChaveCacheRedis(mock(UsuarioAplicacaoRepositorio.class), mock(com.example.siggtrat.servico.RevisaoFontesPainelServico.class)), true, 32_768);

    @AfterEach void limpar() { SecurityContextHolder.clearContext(); }

    @ParameterizedTest
    @ValueSource(strings = {"/api/contratos/1/documentos", "/api/contratos/1/documentos/2",
            "/api/contratos/1/documentos/2/eventos", "/api/contratos/1/documentos/2/permissoes",
            "/api/contratos/1/documentos/2/acessos", "/api/pessoas/7/dados-restritos"})
    void todaConsultaProtegidaRevalidaPermissaoEAuditoria(String rota) throws Exception {
        autenticar();
        AtomicInteger consultasAuditadas = new AtomicInteger();
        for (int i = 1; i <= 2; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", rota);
            request.setParameter("finalidade", "auditoria contratual");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filtro.doFilter(request, response, (req, res) -> {
                res.setContentType("application/json");
                res.getWriter().write("{\"acesso\":" + consultasAuditadas.incrementAndGet() + "}");
            });
            assertEquals("{\"acesso\":" + i + "}", response.getContentAsString());
            assertNull(response.getHeader("X-SGT-Cache"));
        }
        assertEquals(2, consultasAuditadas.get());
    }

    @Test
    void downloadDocumentalSegueSemBufferDoArquivoInteiroNaHeap() throws Exception {
        autenticar();
        MockHttpServletResponse response = new MockHttpServletResponse();
        filtro.doFilter(new MockHttpServletRequest("GET", "/api/contratos/1/documentos/2/arquivo"),
                response, (req, res) -> {
                    assertFalse(res instanceof ContentCachingResponseWrapper,
                            "Download deve ser transmitido sem materialização integral pelo cache HTTP.");
                    res.setContentType("application/pdf");
                    res.getOutputStream().write(new byte[]{1, 2, 3});
                });
        assertArrayEquals(new byte[]{1, 2, 3}, response.getContentAsByteArray());
    }

    @Test
    void consultaComumContinuaUsandoCache() throws Exception {
        autenticar();
        AtomicInteger consultas = new AtomicInteger();
        for (int i = 0; i < 2; i++) {
            filtro.doFilter(new MockHttpServletRequest("GET", "/api/catalogos"),
                    new MockHttpServletResponse(), (req, res) -> {
                        consultas.incrementAndGet();
                        res.setContentType("application/json");
                        res.getWriter().write("{\"ok\":true}");
                    });
        }
        assertEquals(1, consultas.get());
    }

    private void autenticar() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("usuario.teste", null, List.of()));
    }
}
