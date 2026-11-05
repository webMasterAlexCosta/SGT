package com.example.siggtrat.cache;

import com.example.siggtrat.configuracao.FiltroCacheConsultaHttpRedis;
import com.example.siggtrat.configuracao.NomesCache;
import com.example.siggtrat.configuracao.ServicoChaveCacheRedis;
import com.example.siggtrat.dto.RespostaCacheConsultaHttp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/** A entrada legada permanece armazenada, mas não pode substituir a ausência do handler. */
class CacheRotasRetiradasTest {
    private final ConcurrentMapCacheManager caches = new ConcurrentMapCacheManager(NomesCache.API_GET_RESPONSES);
    private final ServicoChaveCacheRedis chaves = mock(ServicoChaveCacheRedis.class);
    private final FiltroCacheConsultaHttpRedis filtro = new FiltroCacheConsultaHttpRedis(caches, chaves, true, 32_768);
    private final byte[] corpo = "{\"data\":[{\"evidencias\":\"LEGADO SINTETICO\"}]}".getBytes(StandardCharsets.UTF_8);

    @AfterEach
    void limparContexto() { SecurityContextHolder.clearContext(); }

    private void prepararEntrada() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("gestor.sintetico", null, List.of()));
        when(chaves.consulta(anyString(), any(), any(), any())).thenReturn("entrada-legada-sintetica");
        caches.getCache(NomesCache.API_GET_RESPONSES).put("entrada-legada-sintetica",
                new RespostaCacheConsultaHttp(200, "application/json", corpo));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/governanca-terceiros", "/api/governanca-terceiros/",
            "/api/governanca-terceiros/resumo", "/api/governanca-terceiros/fornecedores",
            "/api/governanca-terceiros/processos", "/api/governanca-terceiros/riscos",
            "/api/governanca-terceiros/tabelas-tarifarias", "/api/governanca-terceiros/folhas-horas",
            "/api/governanca-terceiros/faturas"})
    void respostaLegadaNaoSubstituiRotaRetirada(String uri) throws Exception {
        prepararEntrada();
        var resposta = new MockHttpServletResponse();
        AtomicInteger chamadas = new AtomicInteger();
        filtro.doFilter(new MockHttpServletRequest("GET", uri), resposta, (request, response) -> {
            chamadas.incrementAndGet();
            ((jakarta.servlet.http.HttpServletResponse) response).setStatus(404);
            response.setContentType("application/json");
            response.getWriter().write("{\"status\":404}");
        });
        assertEquals(404, resposta.getStatus(), "O filtro deve deixar a cadeia resolver a rota ausente");
        assertEquals(1, chamadas.get());
        assertNull(resposta.getHeader("X-SGT-Cache"));
        assertFalse(resposta.getContentAsString().contains("LEGADO SINTETICO"));
        assertArrayEquals(corpo, caches.getCache(NomesCache.API_GET_RESPONSES)
                .get("entrada-legada-sintetica", RespostaCacheConsultaHttp.class).body(),
                "Desvio de leitura não autoriza apagar a entrada antiga");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/fornecedores", "/api/governanca-terceiros-extra", "/api/governanca-terceiros2"})
    void rotasForaDoNamespaceRetiradoContinuamRecebendoHit(String uri) throws Exception {
        prepararEntrada();
        var resposta = new MockHttpServletResponse();
        filtro.doFilter(new MockHttpServletRequest("GET", uri), resposta,
                (request, response) -> fail("A entrada operacional válida deve continuar atendida pelo cache"));
        assertEquals(200, resposta.getStatus());
        assertEquals("HIT", resposta.getHeader("X-SGT-Cache"));
        assertArrayEquals(corpo, resposta.getContentAsByteArray());
    }
}
