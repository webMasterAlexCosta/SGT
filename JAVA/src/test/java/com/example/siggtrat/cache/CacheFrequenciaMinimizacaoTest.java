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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/** O cache externo não pode omitir a revalidação/auditoria de uma leitura pessoal. */
class CacheFrequenciaMinimizacaoTest {
    private final FiltroCacheConsultaHttpRedis filtro = new FiltroCacheConsultaHttpRedis(
            new ConcurrentMapCacheManager(NomesCache.API_GET_RESPONSES),
            new ServicoChaveCacheRedis(mock(UsuarioAplicacaoRepositorio.class), mock(com.example.siggtrat.servico.RevisaoFontesPainelServico.class)), true, 32_768);

    @AfterEach
    void limpar() { SecurityContextHolder.clearContext(); }

    @ParameterizedTest
    @ValueSource(strings = {"/api/frequencias", "/api/frequencias/mensal", "/api/frequencias/intervalo",
            "/api/frequencias/7/eventos", "/api/frequencias/7/evidencias-integridade",
            "/api/painel/analitico", "/api/painel/analitico/registros", "/api/painel/visoes"})
    void novaLeituraRevalidaPermissaoExpiradaSemReutilizarRespostaCompleta(String rota) throws Exception {
        autenticar();
        AtomicInteger consultasAuditadas = new AtomicInteger();
        for (int leitura = 1; leitura <= 2; leitura++) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", rota);
            request.setParameter("contratoId", "3");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filtro.doFilter(request, response, (req, res) -> {
                int consulta = consultasAuditadas.incrementAndGet();
                res.setContentType("application/json");
                // A permissão expirou entre as duas requisições, sem mutação
                // que pudesse invalidar o cache HTTP por meio de um POST.
                res.getWriter().write(consulta == 1 ? "{\"observacao\":\"DADO-PESSOAL\"}" : "{\"observacao\":null}");
            });
            assertEquals(leitura == 1 ? "{\"observacao\":\"DADO-PESSOAL\"}" : "{\"observacao\":null}",
                    response.getContentAsString());
        }
        assertEquals(2, consultasAuditadas.get(), "Ambas as consultas precisam chegar à autorização vigente");
    }

    @Test
    void consultaComumMantemCacheHttp() throws Exception {
        autenticar();
        AtomicInteger consultas = new AtomicInteger();
        for (int leitura = 0; leitura < 2; leitura++) {
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
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "gestor.nacional", null, List.of(new SimpleGrantedAuthority("ROLE_GESTOR_NACIONAL"))));
    }
}
