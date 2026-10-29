package com.example.siggtrat.seguranca;

import com.example.siggtrat.configuracao.propriedades.PropriedadesSegurancaJwt;
import com.example.siggtrat.constante.ConstantesDominio;
import com.example.siggtrat.entidade.TokenAtivo;
import com.example.siggtrat.repositorio.TokenAtivoRepositorio;
import com.example.siggtrat.servico.DetalhesUsuarioPersonalizadoServico;
import com.example.siggtrat.utilitario.UtilitarioJwt;
import com.example.siggtrat.utilitario.UtilitarioHashSeguranca;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class FiltroAutenticacaoJwt extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(FiltroAutenticacaoJwt.class);

    private final long maxInactivityMinutes;
    private final long updateIntervalSeconds;

    private final UtilitarioJwt jwtUtil;
    private final DetalhesUsuarioPersonalizadoServico customUserDetailsServico;
    private final TokenAtivoRepositorio tokenAtivoRepositorio;

    private final Map<String, EntradaCacheToken> tokenCache = new ConcurrentHashMap<>();
    private final long cacheTtlMs;

    private static class EntradaCacheToken {
        final TokenAtivo tokenAtivo;
        final long expirationTime;

        EntradaCacheToken(TokenAtivo tokenAtivo, long expirationTime) {
            this.tokenAtivo = tokenAtivo;
            this.expirationTime = expirationTime;
        }

        boolean estaExpirada() {
            return System.currentTimeMillis() > expirationTime;
        }
    }

    public FiltroAutenticacaoJwt(UtilitarioJwt jwtUtil,
                                   DetalhesUsuarioPersonalizadoServico customUserDetailsServico,
                                   TokenAtivoRepositorio tokenAtivoRepositorio,
                                   PropriedadesSegurancaJwt properties) {
        this.jwtUtil = jwtUtil;
        this.customUserDetailsServico = customUserDetailsServico;
        this.tokenAtivoRepositorio = tokenAtivoRepositorio;
        this.maxInactivityMinutes = properties.maxInactivityMinutes();
        this.updateIntervalSeconds = properties.updateIntervalSeconds();
        this.cacheTtlMs = properties.cacheTtlMs();
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String jwt = extrairToken(request);

        if (jwt != null) {
            try {
                if (jwtUtil.validarToken(jwt)) {

                    TokenAtivo tokenAtivo = obterOuCarregarToken(jwt);

                    if (tokenAtivo != null) {

                        if (estaInativoHaMuitoTempo(tokenAtivo)) {
                            logger.info("Sessão encerrada por inatividade. Usuário: {}", tokenAtivo.getUsername());
                            tokenAtivoRepositorio.excluirPorTokenBruto(jwt);
                            tokenCache.remove(jwt);

                            request.setAttribute("AUTH_ERROR_CODE", "SESSION_IDLE_TIMEOUT");
                            request.setAttribute("AUTH_ERROR_DETAIL", "Sessão expirada por inatividade.");
                        }
                        else if (processarValidacaoDispositivo(request, tokenAtivo)) {

                            Long userId = jwtUtil.extrairIdUsuario(jwt);
                            if (definirContextoAutenticacao(request, userId)) {
                                request.setAttribute("twoFactorVerified", jwtUtil.segundoFatorVerificado(jwt));
                                // Consultas automáticas de sessão (touch=false) e GETs de
                                // polling não podem mascarar a inatividade humana. O cliente
                                // envia touch=true somente após interação real; mutações HTTP
                                // também contam como atividade por serem ações explícitas.
                                if (deveRegistrarAtividade(request)) {
                                    tentarAtualizarUltimoAcesso(tokenAtivo, jwt, ehRegistroAtividadeExplicita(request));
                                }
                            }
                        }
                    } else {
                        request.setAttribute("AUTH_ERROR_CODE", "SESSION_REPLACED");
                    }
                }
            } catch (JwtException e) {
                logger.debug("JWT Inválido/Expirado: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            } catch (Exception e) {
                logger.error("Erro interno no filtro", e);
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean estaInativoHaMuitoTempo(TokenAtivo tokenAtivo) {
        return atingiuLimiteInatividade(
                tokenAtivo.getLastSeenAt(),
                com.example.siggtrat.utilitario.RelogioSistema.agora(),
                maxInactivityMinutes
        );
    }

    /**
     * O limite é inclusivo: ao completar exatamente N minutos sem atividade,
     * a sessão já está inativa. Evita o atraso de quase um minuto causado por
     * ChronoUnit.MINUTES + comparação estrita.
     */
    static boolean atingiuLimiteInatividade(LocalDateTime lastSeenAt, LocalDateTime now, long maxMinutes) {
        if (lastSeenAt == null || now == null || maxMinutes <= 0) return false;
        return !lastSeenAt.plusMinutes(maxMinutes).isAfter(now);
    }

    static boolean ehRegistroAtividadeExplicita(HttpServletRequest request) {
        if (request == null) return false;
        String uri = request.getRequestURI();
        return uri != null
                && uri.endsWith("/api/autenticacao/sessao")
                && Boolean.parseBoolean(request.getParameter("touch"));
    }

    static boolean deveRegistrarAtividade(HttpServletRequest request) {
        if (request == null) return false;
        if (ehRegistroAtividadeExplicita(request)) return true;

        String uri = request.getRequestURI();
        if (uri != null && uri.endsWith("/api/autenticacao/encerrar-sessao")) return false;

        // GET/HEAD podem ser polling, prefetch ou atualização automática de
        // tela. Apenas mutações são usadas como fallback de atividade humana.
        String method = request.getMethod();
        return "POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method)
                || "PATCH".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method);
    }

    private void tentarAtualizarUltimoAcesso(TokenAtivo tokenAtivo, String jwt, boolean force) {
        LocalDateTime now = com.example.siggtrat.utilitario.RelogioSistema.agora();
        LocalDateTime last = tokenAtivo.getLastSeenAt();

        if (force || last == null || !last.plusSeconds(updateIntervalSeconds).isAfter(now)) {
            try {
                // Primeiro persiste. Se o banco falhar, não mantenha no cache uma
                // atividade que o armazenamento autoritativo nunca confirmou.
                tokenAtivoRepositorio.atualizarUltimoAcesso(tokenAtivo.getId(), now);
                tokenAtivo.setLastSeenAt(now);
                tokenCache.put(jwt, new EntradaCacheToken(tokenAtivo, System.currentTimeMillis() + cacheTtlMs));
            } catch (Exception e) {
                logger.debug("Não foi possível atualizar lastSeenAt (concorrência ignorada): {}", e.getMessage());
            }
        }
    }

    private TokenAtivo obterOuCarregarToken(String jwt) {
        EntradaCacheToken cachedEntry = tokenCache.get(jwt);
        if (cachedEntry != null && !cachedEntry.estaExpirada()) {
            return cachedEntry.tokenAtivo;
        }
        Optional<TokenAtivo> tokenOpt = tokenAtivoRepositorio.buscarPorTokenBruto(jwt);
        if (tokenOpt.isPresent()) {
            TokenAtivo t = tokenOpt.get();
            tokenCache.put(jwt, new EntradaCacheToken(t, System.currentTimeMillis() + cacheTtlMs));
            return t;
        } else {
            tokenCache.remove(jwt);
            return null;
        }
    }

    private boolean processarValidacaoDispositivo(HttpServletRequest request, TokenAtivo tokenAtivo) {
        String deviceId = extrairCookie(request, ConstantesDominio.COOKIE_DEVICE);
        if (deviceId == null || deviceId.isBlank()) {
            request.setAttribute("AUTH_ERROR_CODE", "SESSION_DEVICE_MISSING");
            request.setAttribute("AUTH_ERROR_DETAIL", "Sessão inválida: dispositivo não reconhecido.");
            return false;
        }
        String deviceHash = UtilitarioHashSeguranca.sha256Hexadecimal(deviceId);
        String expectedHash = tokenAtivo.getDeviceHash();
        if (expectedHash != null && !expectedHash.isBlank() && !expectedHash.equals(deviceHash)) {
            request.setAttribute("AUTH_ERROR_CODE", "SESSION_DEVICE_MISMATCH");
            request.setAttribute("AUTH_ERROR_DETAIL", "Sessão encerrada por acesso em outro dispositivo.");
            return false;
        }
        return true;
    }

    private boolean definirContextoAutenticacao(HttpServletRequest request, Long userId) {
        if (userId == null) return false;
        if (SecurityContextHolder.getContext().getAuthentication() != null) return true;

        UserDetails userDetails = this.customUserDetailsServico.carregarUsuarioPorId(userId);
        if (!userDetails.isEnabled()) {
            request.setAttribute("AUTH_ERROR_CODE", "ACCOUNT_INACTIVE");
            request.setAttribute("AUTH_ERROR_DETAIL", "A conta está inativa ou pendente de aprovação.");
            SecurityContextHolder.clearContext();
            return false;
        }
        if (!userDetails.isAccountNonLocked()) {
            request.setAttribute("AUTH_ERROR_CODE", "ACCOUNT_LOCKED");
            request.setAttribute("AUTH_ERROR_DETAIL", "A conta está temporariamente bloqueada.");
            SecurityContextHolder.clearContext();
            return false;
        }

        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
        request.setAttribute("authenticatedUser", userDetails.getUsername());
        request.setAttribute("authenticatedUserId", userId);
        return true;
    }

    /** Remove imediatamente do cache local uma sessão específica. */
    public void removerTokenDoCache(String jwt) {
        if (jwt == null || jwt.isBlank()) return;
        tokenCache.remove(jwt);
    }

    /** Remove imediatamente do cache local todas as sessões do usuário. */
    public void removerSessoesUsuarioDoCache(String username) {
        if (username == null || username.isBlank()) return;
        tokenCache.entrySet().removeIf(entry -> {
            TokenAtivo token = entry.getValue() != null ? entry.getValue().tokenAtivo : null;
            return token != null && username.equalsIgnoreCase(token.getUsername());
        });
    }

    private String extrairCookie(HttpServletRequest request, String name) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookie != null && name.equals(cookie.getName())) return cookie.getValue();
            }
        }
        return null;
    }

    private String extrairToken(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (ConstantesDominio.COOKIE_AUTH.equals(cookie.getName())) return cookie.getValue();
            }
        }
        String headerAuth = request.getHeader("Authorization");
        if (headerAuth != null && headerAuth.startsWith("Bearer ")) return headerAuth.substring(7);
        return null;
    }

    // =======================================================================
    // CORREÇÃO: Prevenção de Memory Leak com rotina de limpeza do cache
    // =======================================================================
    @org.springframework.scheduling.annotation.Scheduled(fixedRateString = "${app.security.jwt.cache-cleanup-ms}")
    public void limparCacheTokensExpirados() {
        int sizeBefore = tokenCache.size();

        tokenCache.entrySet().removeIf(entry -> entry.getValue().estaExpirada());

        int sizeAfter = tokenCache.size();
        if (sizeBefore != sizeAfter) {
            logger.debug("Cache de JWTs limpo: {} removidos. Tamanho atual: {}", (sizeBefore - sizeAfter), sizeAfter);
        }
    }
}