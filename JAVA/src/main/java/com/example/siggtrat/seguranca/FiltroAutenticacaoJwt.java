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
    private final com.example.siggtrat.servico.RevogacaoSessaoServico revogacaoSessaoServico;

    // Este cache reduz gravações repetidas de atividade; nunca autoriza uma
    // sessão. A existência e o último acesso autoritativos vêm do banco.
    private final Map<String, EntradaAtividade> atividadeCache = new ConcurrentHashMap<>();
    private final long cacheTtlMs;

    private record EntradaAtividade(String username, LocalDateTime ultimoRegistro, long expirationTime) {
        boolean estaExpirada() {
            return System.currentTimeMillis() > expirationTime;
        }
    }

    public FiltroAutenticacaoJwt(UtilitarioJwt jwtUtil,
                                   DetalhesUsuarioPersonalizadoServico customUserDetailsServico,
                                   TokenAtivoRepositorio tokenAtivoRepositorio,
                                   PropriedadesSegurancaJwt properties,
                                    com.example.siggtrat.servico.RevogacaoSessaoServico revogacaoSessaoServico) {
        this.revogacaoSessaoServico = revogacaoSessaoServico;
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

                    TokenAtivo tokenAtivo = carregarTokenAutoritativo(jwt);

                    if (tokenAtivo != null) {

                        if (estaInativoHaMuitoTempo(tokenAtivo)) {
                            logger.info("Sessão encerrada por inatividade. Usuário: {}", tokenAtivo.getUsername());
                            revogacaoSessaoServico.revogarToken(jwt);
                            atividadeCache.remove(jwt);

                            request.setAttribute("AUTH_ERROR_CODE", "SESSION_IDLE_TIMEOUT");
                            request.setAttribute("AUTH_ERROR_DETAIL", "Sessão expirada por inatividade.");
                        }
                        else if (processarValidacaoDispositivo(request, tokenAtivo)) {

                            Long userId = jwtUtil.extrairIdUsuario(jwt);
                            if (definirContextoAutenticacao(request, userId)) {
                                if (trocaSenhaObrigatoria() && !permiteAcessoAntesDaTroca(request)) {
                                    com.example.siggtrat.diagnostico.ErroHttpCorrelacionado.escrever(request,response,403,
                                            "PASSWORD_CHANGE_REQUIRED","Altere a senha inicial antes de acessar o sistema.",
                                            "CHANGE_PASSWORD");
                                    return;
                                }
                                request.setAttribute("twoFactorVerified", jwtUtil.segundoFatorVerificado(jwt));
                                // Consultas automáticas de sessão (touch=false) e GETs de
                                // polling não podem mascarar a inatividade humana. O cliente
                                // envia touch=true somente após interação real; mutações HTTP
                                // também contam como atividade por serem ações explícitas.
                                if (deveRegistrarAtividade(request)) {
                                    if (!tentarAtualizarUltimoAcesso(tokenAtivo, jwt,
                                            ehRegistroAtividadeExplicita(request))) {
                                        SecurityContextHolder.clearContext();
                                        request.setAttribute("AUTH_ERROR_CODE", "SESSION_REPLACED");
                                    }
                                }
                            }
                        }
                    } else {
                        request.setAttribute("AUTH_ERROR_CODE", "SESSION_REPLACED");
                    }
                }
            } catch (JwtException e) {
                logger.debug("FiltroAutenticacaoJwt: JWT Inválido/Expirado falha={}", com.example.siggtrat.diagnostico.DiagnosticoFalha.resumir(e));
                SecurityContextHolder.clearContext();
            } catch (Exception e) {
                logger.error("FiltroAutenticacaoJwt: Erro interno no filtro falha={}", com.example.siggtrat.diagnostico.DiagnosticoFalha.resumir(e));
                SecurityContextHolder.clearContext();
            }
        }

        // O contexto não é uma sessão HTTP. Salvá-lo no próprio request permite
        // que o redispatch ASYNC do streaming preserve a autenticação já validada.
        new org.springframework.security.web.context.RequestAttributeSecurityContextRepository()
                .saveContext(SecurityContextHolder.getContext(), request, response);
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

    private boolean tentarAtualizarUltimoAcesso(TokenAtivo tokenAtivo, String jwt, boolean force) {
        LocalDateTime now = com.example.siggtrat.utilitario.RelogioSistema.agora();
        LocalDateTime last = tokenAtivo.getLastSeenAt();
        EntradaAtividade atividade = atividadeCache.get(jwt);
        if (!force && atividade != null && !atividade.estaExpirada()
                && (last == null || atividade.ultimoRegistro().isAfter(last))) {
            last = atividade.ultimoRegistro();
        }

        if (force || last == null || !last.plusSeconds(updateIntervalSeconds).isAfter(now)) {
            // Zero linhas significa que outra transação revogou a sessão entre
            // a consulta e o touch. Não recrie uma autorização em memória.
            if (tokenAtivoRepositorio.atualizarUltimoAcesso(tokenAtivo.getId(), now) == 0) {
                atividadeCache.remove(jwt);
                return false;
            }
            tokenAtivo.setLastSeenAt(now);
            atividadeCache.put(jwt, new EntradaAtividade(tokenAtivo.getUsername(), now,
                    System.currentTimeMillis() + cacheTtlMs));
        }
        return true;
    }

    private TokenAtivo carregarTokenAutoritativo(String jwt) {
        // A revogação pode ocorrer em outra instância. O cache de atividade
        // nunca substitui esta leitura, inclusive durante seu TTL.
        Optional<TokenAtivo> tokenOpt = tokenAtivoRepositorio.buscarPorTokenBruto(jwt);
        if (tokenOpt.isEmpty()) atividadeCache.remove(jwt);
        return tokenOpt.orElse(null);
    }

    private boolean trocaSenhaObrigatoria() {
        var autenticacao = SecurityContextHolder.getContext().getAuthentication();
        return autenticacao != null
                && autenticacao.getPrincipal() instanceof DetalhesUsuarioAutenticado usuario
                && usuario.isTrocaSenhaObrigatoria();
    }

    private boolean permiteAcessoAntesDaTroca(HttpServletRequest request) {
        String caminho = request.getRequestURI();
        String contexto = request.getContextPath();
        if (contexto != null && !contexto.isEmpty() && caminho.startsWith(contexto)) {
            caminho = caminho.substring(contexto.length());
        }
        if (!caminho.startsWith("/api/") || "OPTIONS".equalsIgnoreCase(request.getMethod())) return true;
        if ("GET".equalsIgnoreCase(request.getMethod()) || "HEAD".equalsIgnoreCase(request.getMethod())) {
            return "/api/autenticacao/csrf".equals(caminho)
                    || "/api/autenticacao/sessao".equals(caminho)
                    || "/api/usuarios/meu-perfil".equals(caminho);
        }
        return "POST".equalsIgnoreCase(request.getMethod())
                && ("/api/autenticacao/alterar-senha-inicial".equals(caminho)
                    || "/api/autenticacao/encerrar-sessao".equals(caminho)
                    || "/api/autenticacao/autenticar".equals(caminho));
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

    /** Remove metadados locais de atividade de uma sessão revogada no banco. */
    public void removerTokenDoCache(String jwt) {
        if (jwt == null || jwt.isBlank()) return;
        atividadeCache.remove(jwt);
    }

    /** Remove metadados locais de atividade das sessões do usuário. */
    public void removerSessoesUsuarioDoCache(String username) {
        if (username == null || username.isBlank()) return;
        atividadeCache.entrySet().removeIf(entry -> username.equalsIgnoreCase(entry.getValue().username()));
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
        int sizeBefore = atividadeCache.size();

        atividadeCache.entrySet().removeIf(entry -> entry.getValue().estaExpirada());

        int sizeAfter = atividadeCache.size();
        if (sizeBefore != sizeAfter) {
            logger.debug("Cache de atividade limpo: {} removidos. Tamanho atual: {}", (sizeBefore - sizeAfter), sizeAfter);
        }
    }
}
