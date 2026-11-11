package com.example.siggtrat.configuracao;

import com.example.siggtrat.dto.RespostaCacheConsultaHttp;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Cache HTTP distribuído para consultas GET JSON autenticadas.
 *
 * Segurança e consistência:
 * - a chave contém o usuário + authorities, além da rota e parâmetros;
 * - endpoints de autenticação/sessão/perfil nunca são armazenados;
 * - somente respostas 200 JSON, sem Set-Cookie/Content-Disposition, entram no Redis;
 * - respostas grandes são ignoradas;
 * - qualquer falha do Redis faz a requisição seguir normalmente para o backend.
 *
 * A invalidação ocorre junto com os demais caches de negócio por meio de
 * {@link AspectoInvalidacaoCacheMutacaoApi}.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 50)
public class FiltroCacheConsultaHttpRedis extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(FiltroCacheConsultaHttpRedis.class);
    private static final String CACHE_HEADER = "X-SGT-Cache";

    private final CacheManager cacheManager;
    private final ServicoChaveCacheRedis cacheKeyServico;
    private final boolean enabled;
    private final int maxResponseBytes;

    public FiltroCacheConsultaHttpRedis(
            CacheManager cacheManager,
            ServicoChaveCacheRedis cacheKeyServico,
            @Value("${app.cache.redis.http-get.enabled:true}") boolean enabled,
            @Value("${app.cache.redis.http-get.max-response-bytes:2097152}") int maxResponseBytes
    ) {
        this.cacheManager = cacheManager;
        this.cacheKeyServico = cacheKeyServico;
        this.enabled = enabled;
        this.maxResponseBytes = Math.max(16 * 1024, maxResponseBytes);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!enabled || !"GET".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String uri = request.getRequestURI();
        if (uri == null || !uri.startsWith("/api/") || uri.indexOf('%') >= 0) {
            return true;
        }

        // Uma entrada Redis de versão anterior pode sobreviver à retirada do
        // controlador. Preservar os bytes antigos não autoriza servir a API
        // retirada: a requisição deve alcançar a segurança e o MVC atuais.
        if (uri.equals("/api/governanca-terceiros") || uri.startsWith("/api/governanca-terceiros/")) {
            return true;
        }

        // Estes endpoints possuem estado de sessão, credenciais ou conteúdo que
        // não deve ser reaproveitado por uma camada de cache HTTP.
        return uri.startsWith("/api/autenticacao/")
                // Uma grafia codificada pode chegar ao mesmo controlador após a
                // decodificação MVC. Nunca decidir a autorização a partir de um HIT.
                // Provas históricas podem referenciar fontes pessoais restritas.
                || uri.contains("/provas-historicas")
                || uri.contains("/identidade-historica")
                || uri.contains("/provas-nome")
                || uri.contains("/nome-historico")
                || uri.contains("/contexto-historico")
                || uri.startsWith("/api/perfil/")
                || uri.equals("/api/usuarios/meu-perfil")
                || uri.startsWith("/api/cache/")
                // Cada leitura documental/restrita revalida a permissão vigente
                // e registra o acesso. Hits de cache pulavam ambas as etapas.
                // O mesmo desvio evita copiar downloads documentais para a heap.
                || uri.matches("/api/contratos/[^/]+/documentos(?:/.*)?")
                || uri.matches("/api/pessoas/[^/]+/dados-restritos/?")
                // Ocorrências e anexos reconstroem escopo efetivo e revalidam concessões em toda leitura.
                || uri.equals("/api/ocorrencias")
                || uri.startsWith("/api/ocorrencias/")
                || uri.equals("/api/historicos-empregados")
                || uri.startsWith("/api/historicos-empregados/")
                || uri.equals("/api/frequencias-excecoes")
                || uri.startsWith("/api/frequencias-excecoes/")
                || uri.equals("/api/frequencias-adicionais")
                || uri.startsWith("/api/frequencias-adicionais/")
                // Frequências/eventos podem elevar o conteúdo pessoal por uma
                // concessão vigente. Cada GET precisa revalidar e auditar seu
                // uso, inclusive após expiração sem qualquer mutação HTTP.
                // O cache paginado interno é preservado antes da minimização.
                || uri.equals("/api/frequencias")
                || uri.startsWith("/api/frequencias/")
                // Endpoints de polling mudam por processamento assíncrono e
                // precisam refletir progresso imediatamente.
                || uri.startsWith("/api/frequencias/fechamentos/")
                || uri.startsWith("/api/painel/calculos/")
                // Exportações assíncronas possuem estado mutável e os arquivos
                // podem ter centenas de MB. Nunca envolver essas rotas com
                // ContentCachingResponseWrapper: isso copiaria o relatório inteiro
                // para a heap antes de liberá-lo ao navegador e pode encerrar a
                // resposta com ERR_INVALID_RESPONSE em exportações grandes.
                || uri.startsWith("/api/relatorios/exportacoes/")
                // Mantém também os endpoints de download direto mantidos durante a transição do frontend fora do
                // cache HTTP. Eles retornam PDF/XLSX por streaming, não JSON.
                || (uri.startsWith("/api/relatorios/") && uri.endsWith("/exportacao"));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            filterChain.doFilter(request, response);
            return;
        }

        Cache cache = cacheManager.getCache(NomesCache.API_GET_RESPONSES);
        if (cache == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = construirChave(request);
        boolean bypassLeitura = requisicaoPedeAtualizacao(request);

        if (!bypassLeitura) {
            try {
                RespostaCacheConsultaHttp cached = cache.get(key, RespostaCacheConsultaHttp.class);
                if (cached != null && cached.body() != null) {
                    escreverRespostaCacheada(response, cached);
                    return;
                }
            } catch (RuntimeException ex) {
                log.warn("FiltroCacheConsultaHttpRedis: Falha ao ler cache HTTP GET do Redis. Consulta seguirá para a aplicação falha={}", com.example.siggtrat.diagnostico.DiagnosticoFalha.resumir(ex));
            }
        }

        ContentCachingResponseWrapper wrapper = new ContentCachingResponseWrapper(response);
        try {
            filterChain.doFilter(request, wrapper);
            byte[] body = wrapper.getContentAsByteArray();

            if (podeArmazenar(wrapper, body)) {
                RespostaCacheConsultaHttp value = new RespostaCacheConsultaHttp(
                        wrapper.getStatus(),
                        wrapper.getContentType(),
                        Arrays.copyOf(body, body.length)
                );
                try {
                    cache.put(key, value);
                    wrapper.setHeader(CACHE_HEADER, bypassLeitura ? "REFRESH" : "MISS");
                } catch (RuntimeException ex) {
                    log.warn("FiltroCacheConsultaHttpRedis: Falha ao gravar cache HTTP GET no Redis. Resposta original será preservada falha={}", com.example.siggtrat.diagnostico.DiagnosticoFalha.resumir(ex));
                }
            }
        } finally {
            wrapper.copyBodyToResponse();
        }
    }

    private String construirChave(HttpServletRequest request) {
        String parametros = new TreeMap<>(request.getParameterMap()).entrySet().stream()
                .map(this::canonizarParametro)
                .collect(Collectors.joining("\u001e"));

        String accept = valorSeguro(request.getHeader(HttpHeaders.ACCEPT));
        return cacheKeyServico.consulta(
                "http-get",
                request.getRequestURI(),
                parametros,
                accept
        );
    }

    private String canonizarParametro(Map.Entry<String, String[]> entry) {
        String valores = entry.getValue() == null
                ? ""
                : Arrays.stream(entry.getValue())
                .map(this::valorSeguro)
                .collect(Collectors.joining("\u001f"));
        return valorSeguro(entry.getKey()) + "=" + valores;
    }

    private String valorSeguro(String value) {
        if (value == null) return "";
        // Remove apenas os separadores internos usados na chave para impedir
        // colisões artificiais; o conteúdo ainda será protegido por SHA-256.
        return value.replace("\u001e", " ").replace("\u001f", " ");
    }

    private boolean requisicaoPedeAtualizacao(HttpServletRequest request) {
        String cacheControl = valorSeguro(request.getHeader(HttpHeaders.CACHE_CONTROL)).toLowerCase(Locale.ROOT);
        String pragma = valorSeguro(request.getHeader("Pragma")).toLowerCase(Locale.ROOT);
        return cacheControl.contains("no-cache") || cacheControl.contains("no-store") || pragma.contains("no-cache");
    }

    private boolean podeArmazenar(ContentCachingResponseWrapper response, byte[] body) {
        if (response.getStatus() != HttpServletResponse.SC_OK || body == null || body.length == 0) {
            return false;
        }
        if (body.length > maxResponseBytes) {
            return false;
        }
        if (response.getHeader(HttpHeaders.SET_COOKIE) != null) {
            return false;
        }

        // Cache-Control/no-store gerado pelo Spring Security protege o cache do
        // navegador. Este cache é interno, segregado por usuário e nunca é
        // enviado como autorização para clientes, portanto não deve ser
        // bloqueado por esse header HTTP.
        if (response.getHeader(HttpHeaders.CONTENT_DISPOSITION) != null) {
            return false;
        }

        String contentType = valorSeguro(response.getContentType()).toLowerCase(Locale.ROOT);
        return contentType.startsWith(MediaType.APPLICATION_JSON_VALUE)
                || contentType.contains("+json");
    }

    private void escreverRespostaCacheada(HttpServletResponse response, RespostaCacheConsultaHttp cached) throws IOException {
        response.setStatus(cached.status());
        if (cached.contentType() != null && !cached.contentType().isBlank()) {
            response.setContentType(cached.contentType());
        } else {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        }
        response.setHeader(CACHE_HEADER, "HIT");
        response.setContentLength(cached.body().length);
        response.getOutputStream().write(cached.body());
        response.getOutputStream().flush();
    }
}
