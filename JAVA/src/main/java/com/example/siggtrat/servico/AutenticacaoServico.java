package com.example.siggtrat.servico;

import com.example.siggtrat.configuracao.propriedades.PropriedadesAutenticacao;
import com.example.siggtrat.constante.ConstantesDominio;
import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.dto.ResumoUsuarioAplicacaoDTO;
import com.example.siggtrat.dto.AlteracaoSenhaInicialAutenticacaoDTO;
import com.example.siggtrat.dto.CredenciaisLoginDTO;
import com.example.siggtrat.dto.CadastroAutenticacaoDTO;
import com.example.siggtrat.dto.RenovacaoSessaoDTO;
import com.example.siggtrat.dto.AutenticacaoSuperintendenciaDTO;
import com.example.siggtrat.dto.AutenticacaoGestaoLocalDTO;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.entidade.VinculoGestaoUsuarioAplicacao;
import com.example.siggtrat.entidade.GestaoLocal;
import com.example.siggtrat.entidade.Jornada;
import com.example.siggtrat.entidade.Lotacao;
import com.example.siggtrat.entidade.SuperintendenciaEstadual;
import com.example.siggtrat.entidade.TokenAtivo;
import com.example.siggtrat.repositorio.UsuarioAplicacaoRepositorio;
import com.example.siggtrat.repositorio.GestaoLocalRepositorio;
import com.example.siggtrat.repositorio.JornadaRepositorio;
import com.example.siggtrat.repositorio.LotacaoRepositorio;
import com.example.siggtrat.repositorio.SuperintendenciaEstadualRepositorio;
import com.example.siggtrat.repositorio.TokenAtivoRepositorio;
import com.example.siggtrat.seguranca.FiltroAutenticacaoJwt;
import com.example.siggtrat.servico.EscopoAcessoUsuarioServico;
import com.example.siggtrat.servico.TentativaLoginServico;
import com.example.siggtrat.servico.TotpServico;
import com.example.siggtrat.utilitario.UtilitarioJwt;
import com.example.siggtrat.utilitario.UtilitarioHashSeguranca;
import com.example.siggtrat.utilitario.UtilitarioMatricula;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@org.springframework.stereotype.Service
@Transactional
public class AutenticacaoServico implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    @Override public org.slf4j.Logger loggerDiagnostico() { return log; }


    private static final Logger log = LoggerFactory.getLogger(AutenticacaoServico.class);

    private final String genericError;
    private final long deviceCookieMaxAgeSeconds;
    private final int passwordMinLength;

    private final AuthenticationManager authenticationManager;
    private final UsuarioAplicacaoRepositorio userRepositorio;
    private final TokenAtivoRepositorio tokenAtivoRepositorio;
    private final PasswordEncoder passwordEncoder;
    private final UtilitarioJwt jwtUtil;
    private final TentativaLoginServico loginAttemptServico;
    private final LotacaoRepositorio lotacaoRepositorio;
    private final JornadaRepositorio jornadaRepositorio;
    private final SuperintendenciaEstadualRepositorio superintendenciaRepositorio;
    private final GestaoLocalRepositorio gestaoLocalRepositorio;
    private final EscopoAcessoUsuarioServico userAccessScopeServico;
    private final FiltroAutenticacaoJwt jwtAuthenticationFilter;
    private final TotpServico totpServico;
    private final RevogacaoSessaoServico revogacaoSessaoServico;

    public AutenticacaoServico(
            AuthenticationManager authenticationManager,
            UsuarioAplicacaoRepositorio userRepositorio,
            TokenAtivoRepositorio tokenAtivoRepositorio,
            PasswordEncoder passwordEncoder,
            UtilitarioJwt jwtUtil,
            TentativaLoginServico loginAttemptServico,
            LotacaoRepositorio lotacaoRepositorio,
            JornadaRepositorio jornadaRepositorio,
            SuperintendenciaEstadualRepositorio superintendenciaRepositorio,
            GestaoLocalRepositorio gestaoLocalRepositorio,
            EscopoAcessoUsuarioServico userAccessScopeServico,
            FiltroAutenticacaoJwt jwtAuthenticationFilter,
            TotpServico totpServico,
            PropriedadesAutenticacao authProperties,
            RevogacaoSessaoServico revogacaoSessaoServico
    ) {
        this.revogacaoSessaoServico = revogacaoSessaoServico;
        this.authenticationManager = authenticationManager;
        this.userRepositorio = userRepositorio;
        this.tokenAtivoRepositorio = tokenAtivoRepositorio;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.loginAttemptServico = loginAttemptServico;
        this.lotacaoRepositorio = lotacaoRepositorio;
        this.jornadaRepositorio = jornadaRepositorio;
        this.superintendenciaRepositorio = superintendenciaRepositorio;
        this.gestaoLocalRepositorio = gestaoLocalRepositorio;
        this.userAccessScopeServico = userAccessScopeServico;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.totpServico = totpServico;
        this.genericError = authProperties.genericError();
        this.deviceCookieMaxAgeSeconds = authProperties.deviceCookieMaxAgeSeconds();
        this.passwordMinLength = authProperties.passwordMinLength();
    }
    public ResponseEntity<?> alterarSenhaInicial(AlteracaoSenhaInicialAutenticacaoDTO data, HttpServletRequest request) {

        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<UsuarioAplicacao> userOpt = userRepositorio.buscarPorUsernameParaAtualizacao(currentUsername);

        if (userOpt.isEmpty()) {
            return erro(HttpStatus.NOT_FOUND, "Usuário não encontrado.");
        }

        UsuarioAplicacao user = userOpt.get();
        // O filtro autorizou antes de obtermos o lock. Uma inativação ou
        // redefinição administrativa pode ter revogado essa sessão na espera.
        String tokenAtual = extrairTokenAutenticacao(request);
        boolean sessaoAtiva = tokenAtual != null && !tokenAtual.isBlank()
                && jwtUtil.validarToken(tokenAtual)
                && tokenAtivoRepositorio.buscarPorTokenBruto(tokenAtual)
                        .filter(token -> currentUsername.equals(token.getUsername())).isPresent();
        if (!Boolean.TRUE.equals(user.getActive()) || !sessaoAtiva) {
            return erro(HttpStatus.UNAUTHORIZED,
                    "Sessão expirada ou substituída. Autentique-se novamente.");
        }
        if (!Boolean.TRUE.equals(user.getForceChangePassword())) {
            return erro(HttpStatus.FORBIDDEN,
                    "A troca inicial não está pendente. Utilize a alteração de senha do perfil.");
        }
        String newPassword = data != null ? data.newPassword() : null;

        if (newPassword == null || newPassword.length() < passwordMinLength) {
            return erro(HttpStatus.BAD_REQUEST, "A nova senha deve ter no mínimo " + passwordMinLength + " caracteres.");
        }
        if (newPassword.length() > 72) {
            return erro(HttpStatus.BAD_REQUEST, "A nova senha deve ter no máximo 72 caracteres.");
        }
        if (senhaConfere(newPassword, user.getPassword())) {
            return erro(HttpStatus.BAD_REQUEST, "A nova senha deve ser diferente da senha temporária atual.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setForceChangePassword(false);
        user.setFailedAttempts(0);
        user.setLockoutEnd(null);
        // O segredo TOTP só nasce no fluxo de inscrição e nunca é enviado como
        // código pelo servidor. Primeiro acesso não ativa um fator inexistente.
        user.setSecret2fa(null);
        user.setPendingSecret2fa(null);
        user.setTwoFactorEnabled(false);

        userRepositorio.saveAndFlush(user);

        String deviceId = extrairValorCookie(request, ConstantesDominio.COOKIE_DEVICE);
        if (deviceId == null || deviceId.isBlank()) {
            deviceId = UUID.randomUUID().toString();
        }
        String deviceHash = UtilitarioHashSeguranca.sha256Hexadecimal(deviceId);

        boolean isSecure = request.isSecure();

        ResponseCookie deviceCookie = ResponseCookie.from(ConstantesDominio.COOKIE_DEVICE, deviceId)
                .httpOnly(true)
                .secure(isSecure)
                .path("/")
                .maxAge(deviceCookieMaxAgeSeconds)
                .sameSite("Strict")
                .build();

        String token = jwtUtil.gerarToken(user.getId());
        Date expireAtDate = jwtUtil.extrairExpiracao(token);
        LocalDateTime expireAtLocal = expireAtDate.toInstant().atZone(ZoneId.of("UTC")).toLocalDateTime();

        tokenAtivoRepositorio.deleteByUsername(user.getUsername());
        jwtAuthenticationFilter.removerSessoesUsuarioDoCache(user.getUsername());

        LocalDateTime now = com.example.siggtrat.utilitario.RelogioSistema.agora();

        TokenAtivo tokenAtivo = TokenAtivo.deTokenBruto(token, user.getUsername(), now, expireAtLocal);
        tokenAtivo.setDeviceHash(deviceHash);
        tokenAtivo.setIpAddress(obterIpCliente(request));
        tokenAtivo.setUserAgent(Optional.ofNullable(request.getHeader("User-Agent")).orElse(""));
        tokenAtivo.setLastSeenAt(now);
        tokenAtivoRepositorio.save(tokenAtivo);

        long expirationSeconds = Math.max(
                1L,
                (expireAtDate.getTime() - System.currentTimeMillis()) / 1000L
        );

        ResponseCookie cookie = ResponseCookie.from(ConstantesDominio.COOKIE_AUTH, token)
                .httpOnly(true)
                .secure(isSecure)
                .path("/")
                .maxAge(expirationSeconds)
                .sameSite("Strict")
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, cookie.toString());
        headers.add(HttpHeaders.SET_COOKIE, deviceCookie.toString());
        invalidarCookieCsrf(headers, request);

        return ResponseEntity.ok()
                .headers(headers)
                .body(Map.of("message", "Senha alterada com sucesso! Acesso liberado."));
    }

    private boolean jornadaPertenceLotacao(Lotacao lotacao, Jornada jornada) {
        if (lotacao == null || lotacao.getId() == null || jornada == null || jornada.getId() == null) {
            return false;
        }
        Long lotId = lotacao.getId();
        Long jornadaId = jornada.getId();

        if (jornada.getLotacoes() != null && !jornada.getLotacoes().isEmpty()) {
            boolean match = jornada.getLotacoes().stream()
                    .filter(Objects::nonNull)
                    .anyMatch(l -> l.getId() != null && l.getId().equals(lotId));
            if (match) return true;
        }

        Lotacao lot = lotacaoRepositorio.findById(lotId).orElse(lotacao);
        return lot.getTurnos() != null && lot.getTurnos().stream()
                .anyMatch(t -> t != null && t.getId() != null && t.getId().equals(jornadaId));
    }


    private Long extrairId(Object raw) {
        if (raw == null) return null;
        if (raw instanceof Number number) return number.longValue();
        String text = raw.toString().trim();
        if (text.isBlank()) return null;
        return Long.parseLong(text);
    }

    @SuppressWarnings("unchecked")
    private List<VinculoGestaoUsuarioAplicacao> resolverEscoposGestao(Map<String, ?> payload) {
        Object rawScopes = payload.get("vinculosGestao");
        if (!(rawScopes instanceof Collection<?> scopesRaw)) return null;

        List<VinculoGestaoUsuarioAplicacao> scopes = new ArrayList<>();
        Set<String> unique = new LinkedHashSet<>();
        for (Object rawItem : scopesRaw) {
            if (!(rawItem instanceof Map<?, ?> item)) continue;

            Long lotacaoId = extrairId(item.get("lotacaoId"));
            if (lotacaoId == null && item.get("lotacao") instanceof Map<?, ?> lotMap) {
                lotacaoId = extrairId(lotMap.get("id"));
            }

            Long jornadaId = extrairId(item.get("jornadaId"));
            if (jornadaId == null && item.get("jornada") instanceof Map<?, ?> jornadaMap) {
                jornadaId = extrairId(jornadaMap.get("id"));
            }

            if (lotacaoId == null && jornadaId == null) continue;
            if (lotacaoId == null || jornadaId == null) {
                throw new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio("Cada vínculo de gestão deve informar lotação e turno.");
            }

            Lotacao lotacao = lotacaoRepositorio.findById(lotacaoId).orElse(null);
            Jornada jornada = jornadaRepositorio.findById(jornadaId).orElse(null);
            if (lotacao == null) throw new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio("Lotação inválida.");
            if (jornada == null) throw new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio("Turno/Jornada inválido.");
            if (!jornadaPertenceLotacao(lotacao, jornada)) {
                throw new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio("O turno/jornada selecionado não está habilitado para a lotação informada.");
            }

            String key = lotacaoId + ":" + jornadaId;
            if (unique.add(key)) {
                scopes.add(new VinculoGestaoUsuarioAplicacao(null, lotacao, jornada));
            }
        }
        return scopes;
    }

    private void aplicarEscoposGestao(UsuarioAplicacao user, List<VinculoGestaoUsuarioAplicacao> scopes) {
        if (scopes == null) return;
        user.setVinculosGestao(scopes);
        if (!scopes.isEmpty()) {
            VinculoGestaoUsuarioAplicacao primary = scopes.get(0);
            user.setLotacao(primary.getLotacao());
            user.setJornada(primary.getJornada());
        } else {
            user.setLotacao(null);
            user.setJornada(null);
        }
    }

    private void adicionarDadosEscopoUsuario(Map<String, Object> target, UsuarioAplicacao user) {
        ResumoUsuarioAplicacaoDTO dto = MapeadorUsuarioAplicacaoDTO.deEntidade(user);
        EscopoAcessoUsuarioServico.Escopo primary = userAccessScopeServico.obterEscopoPrincipal(user);
        target.put("matricula", dto.matricula());
        target.put("turno", primary != null ? primary.jornadaDescricao() : "");
        target.put("jornadaId", primary != null ? primary.jornadaId() : null);
        target.put("lotacaoNome", primary != null ? primary.lotacaoNome() : "");
        target.put("lotacaoId", primary != null ? primary.lotacaoId() : null);
        target.put("vinculosGestao", dto.vinculosGestao());
        target.put("superintendencia", dto.superintendencia());
        target.put("gestaoLocal", dto.gestaoLocal());
        target.put("superintendenciaId", dto.superintendencia() != null ? dto.superintendencia().id() : null);
        target.put("superintendenciaNome", dto.superintendencia() != null ? dto.superintendencia().label() : null);
        target.put("uf", dto.superintendencia() != null ? dto.superintendencia().uf() : null);
        target.put("gestaoLocalId", dto.gestaoLocal() != null ? dto.gestaoLocal().id() : null);
        target.put("gestaoLocalNome", dto.gestaoLocal() != null ? dto.gestaoLocal().label() : null);
        target.put("fotoUrl", dto.fotoUrl());
        target.put("createdAt", dto.createdAt());
        target.put("twoFactorEnabled", Boolean.TRUE.equals(user.getTwoFactorEnabled()));
    }
    public ResponseEntity<?> informacoesSessao(HttpServletRequest request,
                                         boolean touch) {

        String token = extrairValorCookie(request, ConstantesDominio.COOKIE_AUTH);
        if (token == null || token.isBlank()) {
            return erro(HttpStatus.UNAUTHORIZED,
                    "Sessão inexistente. Faça autenticar novamente.",
                    Map.of(
                            "code", "SESSION_MISSING",
                            "action", "LOGIN",
                            "details", Map.of(
                                    "cookie", ConstantesDominio.COOKIE_AUTH,
                                    "hint", "Cookie de autenticação não encontrado."
                            )
                    ));
        }

        if (!jwtUtil.validarToken(token)) {
            Date exp = null;
            try { exp = jwtUtil.extrairExpiracao(token); } catch (Exception ignored) {}

            String code = "SESSION_INVALID";
            String msg = "Sessão inválida. Faça autenticar novamente.";
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("hint", "Token inválido ou expirado.");

            if (exp != null && exp.before(new Date())) {
                code = "SESSION_EXPIRED";
                msg = "Sua sessão expirou. Faça autenticar novamente.";
                details.put("expiredAt", exp.toInstant().toString());
            }

            return erro(HttpStatus.UNAUTHORIZED,
                    msg,
                    Map.of("code", code, "action", "LOGIN", "details", details));
        }

        Long userId;
        Date expDate;
        try {
            userId = jwtUtil.extrairIdUsuario(token);
            expDate = jwtUtil.extrairExpiracao(token);
        } catch (Exception e) {
            return erro(HttpStatus.UNAUTHORIZED,
                    "Sessão inválida. Faça autenticar novamente.",
                    Map.of(
                            "code", "SESSION_CLAIMS_ERROR",
                            "action", "LOGIN",
                            "details", Map.of("hint", "Não foi possível ler os dados do token.")
                    ));
        }

        Optional<TokenAtivo> tokenDbOpt = tokenAtivoRepositorio.buscarPorTokenBruto(token);
        if (tokenDbOpt.isEmpty()) {
            return erro(HttpStatus.UNAUTHORIZED,
                    "Sua sessão foi encerrada porque sua conta foi acessada em outro dispositivo.",
                    Map.of(
                            "code", "SESSION_REPLACED",
                            "action", "LOGOUT",
                            "details", Map.of(
                                    "hint", "Faça autenticar novamente. Se não foi você, altere sua senha.",
                                    "tokenStatus", "NOT_ACTIVE_IN_DB"
                            )
                    ));
        }

        TokenAtivo tokenDb = tokenDbOpt.get();

        String deviceId = extrairValorCookie(request, ConstantesDominio.COOKIE_DEVICE);
        if (deviceId == null || deviceId.isBlank()) {
            return erro(HttpStatus.UNAUTHORIZED,
                    "Sessão inválida neste dispositivo. Faça autenticar novamente.",
                    Map.of(
                            "code", "SESSION_DEVICE_MISSING",
                            "action", "LOGOUT",
                            "details", Map.of(
                                    "hint", "Cookie de dispositivo ausente.",
                                    "cookie", ConstantesDominio.COOKIE_DEVICE
                            )
                    ));
        }

        String deviceHash = UtilitarioHashSeguranca.sha256Hexadecimal(deviceId);
        if (tokenDb.getDeviceHash() != null && !tokenDb.getDeviceHash().equals(deviceHash)) {
            return erro(HttpStatus.UNAUTHORIZED,
                    "Sessão inválida neste dispositivo. Faça autenticar novamente.",
                    Map.of(
                            "code", "SESSION_DEVICE_MISMATCH",
                            "action", "LOGOUT",
                            "details", Map.of(
                                    "hint", "Token ativo não pertence a este navegador/PC.",
                                    "reason", "DEVICE_HASH_MISMATCH"
                            )
                    ));
        }

        Optional<UsuarioAplicacao> userOpt = userRepositorio.findById(userId);
        if (userOpt.isEmpty()) {
            return erro(HttpStatus.UNAUTHORIZED,
                    "Usuário não encontrado para esta sessão.",
                    Map.of(
                            "code", "USER_NOT_FOUND",
                            "action", "LOGOUT",
                            "details", Map.of("userId", userId, "hint", "Conta pode ter sido removida.")
                    ));
        }

        UsuarioAplicacao user = userOpt.get();

        if (user.getActive() != null && !user.getActive()) {
            return erro(HttpStatus.FORBIDDEN,
                    "Conta inativa ou pendente de aprovação.",
                    Map.of(
                            "code", "ACCOUNT_INACTIVE",
                            "action", "LOGOUT",
                            "details", Map.of("hint", "A conta foi desativada após o autenticar.")
                    ));
        }

        // O parâmetro touch é consumido no FiltroAutenticacaoJwt. Assim, a
        // atualização de lastSeenAt ocorre em um único ponto e consultas
        // periódicas com touch=false nunca renovam a inatividade.

        PerfilAcesso perfil = user.getPerfil() != null ? user.getPerfil() : PerfilAcesso.OPERACIONAL;
        boolean forceChange = (user.getForceChangePassword() != null && user.getForceChangePassword());
        boolean twoFactorVerified = Boolean.TRUE.equals(user.getTwoFactorEnabled()) && jwtUtil.segundoFatorVerificado(token);

        long expEpochMs = expDate.getTime();
        long remainingSeconds = Math.max(0L, (expEpochMs - System.currentTimeMillis()) / 1000L);

        Map<String, Object> session = new LinkedHashMap<>();
        session.put("authenticated", true);

        Map<String, Object> userInfo = new LinkedHashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("perfil", perfil);
        userInfo.put("autoridade", perfil.autoridade());
        userInfo.put("forceChangePassword", forceChange);
        userInfo.put("twoFactorVerified", twoFactorVerified);
        adicionarDadosEscopoUsuario(userInfo, user);

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("expiresAtUtc", expDate.toInstant().toString());
        meta.put("remainingSeconds", remainingSeconds);
        meta.put("boundToDevice", true);
        meta.put("lastSeenAt", tokenDb.getLastSeenAt() != null ? tokenDb.getLastSeenAt().toString() : null);
        meta.put("loginIp", tokenDb.getIpAddress());
        meta.put("userAgent", tokenDb.getUserAgent());

        session.put("version", 1);
        session.put("user", userInfo);
        session.put("session", meta);

        return ResponseEntity.ok(session);
    }

    // --- NOVO ENDPOINT DE RENOVAÇÃO ---
    public ResponseEntity<?> renovarSessao(RenovacaoSessaoDTO payload, HttpServletRequest request) {
        String password = payload != null ? payload.password() : null;
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        if (password == null || password.isBlank() || password.length() > 72) {
            return erro(HttpStatus.BAD_REQUEST, "Informe uma senha válida para renovar a sessão.");
        }

        Optional<UsuarioAplicacao> userOpt = userRepositorio.buscarPorUsernameParaAtualizacao(username);
        if (userOpt.isEmpty()) {
            return erro(HttpStatus.UNAUTHORIZED, "Usuário não encontrado.");
        }

        UsuarioAplicacao user = userOpt.get();

        String oldToken = extrairTokenAutenticacao(request);
        boolean sessaoAtiva = oldToken != null && !oldToken.isBlank()
                && jwtUtil.validarToken(oldToken)
                && tokenAtivoRepositorio.buscarPorTokenBruto(oldToken)
                        .filter(token -> username.equals(token.getUsername())).isPresent();
        if (!Boolean.TRUE.equals(user.getActive()) || !sessaoAtiva) {
            return erro(HttpStatus.UNAUTHORIZED,
                    "Sessão expirada ou substituída. Autentique-se novamente.");
        }

        if (!senhaConfere(password, user.getPassword())) {
            return erro(HttpStatus.BAD_REQUEST, "Senha incorreta.");
        }

        // Procedimento de geração de novo token
        String deviceId = extrairValorCookie(request, ConstantesDominio.COOKIE_DEVICE);
        if (deviceId == null || deviceId.isBlank()) {
            deviceId = UUID.randomUUID().toString();
        }
        String deviceHash = UtilitarioHashSeguranca.sha256Hexadecimal(deviceId);

        boolean isSecure = request.isSecure();

        ResponseCookie deviceCookie = ResponseCookie.from(ConstantesDominio.COOKIE_DEVICE, deviceId)
                .httpOnly(true)
                .secure(isSecure)
                .path("/")
                .maxAge(deviceCookieMaxAgeSeconds)
                .sameSite("Strict")
                .build();

        boolean twoFactorVerified = Boolean.TRUE.equals(user.getTwoFactorEnabled())
                && jwtUtil.segundoFatorVerificado(oldToken);

        String token = jwtUtil.gerarToken(user.getId(), twoFactorVerified);
        Date expireAtDate = jwtUtil.extrairExpiracao(token);

        LocalDateTime expireAtLocal = expireAtDate.toInstant()
                .atZone(ZoneId.of("UTC"))
                .toLocalDateTime();

        long expirationSeconds = Math.max(
                1L,
                (expireAtDate.getTime() - System.currentTimeMillis()) / 1000L
        );

        // O lock do usuário abrange a revalidação acima e a substituição abaixo.
        // Uma segunda renovação aguardando este commit encontrará o token antigo
        // revogado e não poderá criar uma sessão paralela.
        tokenAtivoRepositorio.deleteByUsername(username);
        jwtAuthenticationFilter.removerSessoesUsuarioDoCache(username);

        LocalDateTime now = com.example.siggtrat.utilitario.RelogioSistema.agora();
        TokenAtivo tokenAtivo = TokenAtivo.deTokenBruto(token, username, now, expireAtLocal);
        tokenAtivo.setDeviceHash(deviceHash);
        tokenAtivo.setIpAddress(obterIpCliente(request));
        tokenAtivo.setUserAgent(Optional.ofNullable(request.getHeader("User-Agent")).orElse(""));
        tokenAtivo.setLastSeenAt(now);
        tokenAtivoRepositorio.save(tokenAtivo);

        ResponseCookie cookie = ResponseCookie.from(ConstantesDominio.COOKIE_AUTH, token)
                .httpOnly(true)
                .secure(isSecure)
                .path("/")
                .maxAge(expirationSeconds)
                .sameSite("Strict")
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, cookie.toString());
        headers.add(HttpHeaders.SET_COOKIE, deviceCookie.toString());

        return ResponseEntity.ok()
                .headers(headers)
                .body(Map.of("message", "Sessão renovada com sucesso."));
    }
    public ResponseEntity<?> autenticar(CredenciaisLoginDTO loginData, HttpServletRequest request) {
        String matriculaInformada = loginData != null ? loginData.matricula() : null;
        String password = loginData != null ? loginData.password() : null;
        String codigoTotp = loginData != null ? loginData.totp() : null;

        final String matricula;
        try {
            matricula = UtilitarioMatricula.normalizar(matriculaInformada);
        } catch (IllegalArgumentException ex) {
            return erro(HttpStatus.UNAUTHORIZED, genericError);
        }

        if (matricula == null || password == null || password.isBlank() || password.length() > 72) {
            return erro(HttpStatus.UNAUTHORIZED, genericError);
        }

        long remainingSeconds = loginAttemptServico.obterTempoBloqueioRestante(matricula);
        if (remainingSeconds > 0) {
            return erro(HttpStatus.TOO_MANY_REQUESTS, "Muitas tentativas. Aguarde.", Map.of("lockout", remainingSeconds));
        }

        Optional<UsuarioAplicacao> userOpt = userRepositorio.buscarPorMatriculaParaAtualizacao(matricula);
        if (userOpt.isEmpty()) {
            senhaConfere(password, ConstantesDominio.DUMMY_HASH);
            loginAttemptServico.registrarFalhaLogin(matricula);
            return erro(HttpStatus.UNAUTHORIZED, genericError);
        }

        UsuarioAplicacao user = userOpt.get();
        String username = user.getUsername();

        if (user.getActive() != null && !user.getActive()) {
            return erro(HttpStatus.FORBIDDEN, "Conta pendente de aprovação ou inativa.");
        }

        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(matricula, password)
            );

            boolean twoFactorVerified = false;
            if (Boolean.TRUE.equals(user.getTwoFactorEnabled())) {
                boolean codigoInformado = codigoTotp != null && !codigoTotp.isBlank();

                if (!codigoInformado) {
                    return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                            "requiresTwoFactor", true,
                            "canSkipTwoFactor", false,
                            "message", "Informe o código do aplicativo autenticador."
                    ));
                }

                if (codigoInformado) {
                    if (!codigoTotp.matches("\\d{6}")
                            || user.getSecret2fa() == null
                            || !totpServico.validarCodigo(user.getSecret2fa(), Integer.parseInt(codigoTotp))) {
                        loginAttemptServico.registrarFalhaLogin(matricula);
                        return erro(HttpStatus.UNAUTHORIZED, genericError);
                    }
                    twoFactorVerified = true;
                }
            }

            loginAttemptServico.registrarSucessoLogin(matricula);

            String autoridade = auth.getAuthorities().stream()
                    .findFirst()
                    .map(GrantedAuthority::getAuthority)
                    .orElse(ConstantesDominio.PERFIL_OPERACIONAL);

            String deviceId = extrairValorCookie(request, ConstantesDominio.COOKIE_DEVICE);
            if (deviceId == null || deviceId.isBlank()) {
                deviceId = UUID.randomUUID().toString();
            }
            String deviceHash = UtilitarioHashSeguranca.sha256Hexadecimal(deviceId);

            boolean isSecure = request.isSecure();

            ResponseCookie deviceCookie = ResponseCookie.from(ConstantesDominio.COOKIE_DEVICE, deviceId)
                    .httpOnly(true)
                    .secure(isSecure)
                    .path("/")
                    .maxAge(deviceCookieMaxAgeSeconds)
                    .sameSite("Strict")
                    .build();

            String token = jwtUtil.gerarToken(user.getId(), twoFactorVerified);
            Date expireAtDate = jwtUtil.extrairExpiracao(token);

            LocalDateTime expireAtLocal = expireAtDate.toInstant()
                    .atZone(ZoneId.of("UTC"))
                    .toLocalDateTime();

            long expirationSeconds = Math.max(
                    1L,
                    (expireAtDate.getTime() - System.currentTimeMillis()) / 1000L
            );

            tokenAtivoRepositorio.deleteByUsername(username);
            jwtAuthenticationFilter.removerSessoesUsuarioDoCache(username);

            LocalDateTime now = com.example.siggtrat.utilitario.RelogioSistema.agora();

            TokenAtivo tokenAtivo = TokenAtivo.deTokenBruto(token, username, now, expireAtLocal);
            tokenAtivo.setDeviceHash(deviceHash);
            tokenAtivo.setIpAddress(obterIpCliente(request));
            tokenAtivo.setUserAgent(Optional.ofNullable(request.getHeader("User-Agent")).orElse(""));
            tokenAtivo.setLastSeenAt(now);
            tokenAtivoRepositorio.save(tokenAtivo);

            ResponseCookie cookie = ResponseCookie.from(ConstantesDominio.COOKIE_AUTH, token)
                    .httpOnly(true)
                    .secure(isSecure)
                    .path("/")
                    .maxAge(expirationSeconds)
                    .sameSite("Strict")
                    .build();

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("message", "Sucesso");
            responseBody.put("user", username);
            responseBody.put("username", username);
            responseBody.put("perfil", user.getPerfil());
            responseBody.put("autoridade", autoridade);
            adicionarDadosEscopoUsuario(responseBody, user);
            responseBody.put("twoFactorVerified", twoFactorVerified);

            boolean mustChange = (user.getForceChangePassword() != null && user.getForceChangePassword());
            responseBody.put("forceChangePassword", mustChange);

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.SET_COOKIE, cookie.toString());
            headers.add(HttpHeaders.SET_COOKIE, deviceCookie.toString());
            invalidarCookieCsrf(headers, request);

            return ResponseEntity.ok().headers(headers).body(responseBody);

        } catch (AuthenticationException e) {
            loginAttemptServico.registrarFalhaLogin(matricula);

            long lockTime = loginAttemptServico.obterTempoBloqueioRestante(matricula);
            if (lockTime > 0) {
                return erro(HttpStatus.TOO_MANY_REQUESTS, "Muitas tentativas falhas.", Map.of("lockout", lockTime));
            }
            return erro(HttpStatus.UNAUTHORIZED, genericError);
        }
    }

    /** Relação pública e enxuta de Gestões Estaduais ativas para solicitar acesso. */
    public ResponseEntity<List<AutenticacaoSuperintendenciaDTO>> listarSuperintendenciasParaCadastro() {
        List<AutenticacaoSuperintendenciaDTO> itens = superintendenciaRepositorio.findByAtivoTrueOrderByUfAsc().stream()
                .filter(se -> !gestaoLocalRepositorio
                        .findBySuperintendenciaIdAndAtivoTrueOrderByNomeAsc(se.getId()).isEmpty())
                .map(se -> new AutenticacaoSuperintendenciaDTO(se.getId(), se.getNome(), se.getUf()))
                .toList();
        return ResponseEntity.ok(itens);
    }

    /**
     * Retorna somente unidades locais ativas da Gestão Estadual informada. O
     * identificador estadual também é validado no cadastro; este endpoint não
     * substitui a autorização e a integridade aplicadas no comando de gravação.
     */
    public ResponseEntity<List<AutenticacaoGestaoLocalDTO>> listarGestoesLocaisParaCadastro(
            Long superintendenciaId
    ) {
        if (superintendenciaId == null) {
            return ResponseEntity.badRequest().body(List.of());
        }
        SuperintendenciaEstadual se = superintendenciaRepositorio.findById(superintendenciaId)
                .filter(item -> Boolean.TRUE.equals(item.getAtivo()))
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Gestão Estadual inválida ou inativa."));
        List<AutenticacaoGestaoLocalDTO> itens = gestaoLocalRepositorio
                .findBySuperintendenciaIdAndAtivoTrueOrderByNomeAsc(se.getId())
                .stream()
                .map(item -> new AutenticacaoGestaoLocalDTO(
                        item.getId(), item.getNome(), item.getCodigo(), se.getId(), se.getUf()))
                .toList();
        return ResponseEntity.ok(itens);
    }

    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<?> registrar(CadastroAutenticacaoDTO userData) {
        try {
            if (userData == null) {
                return erro(HttpStatus.BAD_REQUEST, "Informe os dados para solicitar o acesso.");
            }
            String username = textoCadastro(userData.username());
            String email = textoCadastro(userData.email());
            String rawMatricula = textoCadastro(userData.matricula());
            Long superintendenciaId = userData.superintendenciaId();
            Long gestaoLocalId = userData.gestaoLocalId();

            if (username == null) {
                return erro(HttpStatus.BAD_REQUEST, "O autenticar é obrigatório.");
            }
            username = username.toLowerCase(Locale.ROOT);

            email = email == null ? null : email.toLowerCase(Locale.ROOT);
            if (email == null || !email.matches("^[a-z0-9._%+\\-]+@correios\\.com\\.br$")) {
                return erro(HttpStatus.BAD_REQUEST, "Permitido apenas e-mail institucional @correios.com.br.");
            }

            String matriculaLimpa;
            try {
                matriculaLimpa = UtilitarioMatricula.normalizar(rawMatricula);
            } catch (IllegalArgumentException ex) {
                return erro(HttpStatus.BAD_REQUEST, ex.getMessage());
            }
            if (matriculaLimpa == null) {
                return erro(HttpStatus.BAD_REQUEST, "Informe a matrícula com 8 dígitos.");
            }
            if (superintendenciaId == null) {
                return erro(HttpStatus.BAD_REQUEST, "Selecione a Gestão Estadual/UF.");
            }
            if (gestaoLocalId == null) {
                return erro(HttpStatus.BAD_REQUEST, "Selecione a Gestão Local.");
            }

            if (userRepositorio.findByUsername(username).isPresent()) {
                return erro(HttpStatus.BAD_REQUEST, "Login já cadastrado.");
            }
            if (userRepositorio.existsByEmailIgnoreCase(email)) {
                return erro(HttpStatus.BAD_REQUEST, "E-mail já cadastrado.");
            }
            if (userRepositorio.existsByMatricula(matriculaLimpa)) {
                return erro(HttpStatus.BAD_REQUEST, "Matrícula já cadastrada.");
            }

            SuperintendenciaEstadual se = superintendenciaRepositorio.findById(superintendenciaId)
                    .filter(item -> Boolean.TRUE.equals(item.getAtivo()))
                    .orElse(null);
            if (se == null) {
                return erro(HttpStatus.BAD_REQUEST, "Gestão Estadual inválida ou inativa.");
            }

            GestaoLocal gestaoLocal = gestaoLocalRepositorio.findById(gestaoLocalId).orElse(null);
            if (gestaoLocal == null || !Boolean.TRUE.equals(gestaoLocal.getAtivo())) {
                return erro(HttpStatus.BAD_REQUEST, "Gestão Local inválida ou inativa.");
            }
            if (gestaoLocal.getSuperintendencia() == null
                    || !Objects.equals(se.getId(), gestaoLocal.getSuperintendencia().getId())) {
                return erro(HttpStatus.BAD_REQUEST,
                        "A Gestão Local selecionada não pertence à Gestão Estadual/UF informada.");
            }

            UsuarioAplicacao newUser = new UsuarioAplicacao();
            newUser.setUsername(username);
            newUser.setEmail(email);
            newUser.setMatricula(matriculaLimpa);
            // A senha real será definida por um Gestor Local durante a aprovação.
            newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            newUser.setPerfil(PerfilAcesso.OPERACIONAL);
            newUser.setSuperintendencia(se);
            newUser.setGestaoLocal(gestaoLocal);
            newUser.setLotacao(null);
            newUser.setJornada(null);
            newUser.setActive(false);
            newUser.setForceChangePassword(true);

            userRepositorio.save(newUser);

            return ResponseEntity.ok(Map.of(
                    "message", "Solicitação enviada para " + gestaoLocal.getNome() + " da " + se.getNome()
                            + ". Aguarde a definição do escopo e a ativação por um Gestor Local responsável.",
                    "superintendencia", se.getNome(),
                    "gestaoLocal", gestaoLocal.getNome()
            ));

        } catch (NumberFormatException e) {
            return erro(HttpStatus.BAD_REQUEST, "Gestão Estadual inválida.");
        } catch (DataIntegrityViolationException e) {
            return erro(HttpStatus.CONFLICT, "Já existe um usuário com o mesmo e-mail ou matrícula.");
        } catch (Exception e) {
            log.error("AutenticacaoServico: Erro interno ao solicitar acesso. falha={}", com.example.siggtrat.diagnostico.DiagnosticoFalha.resumir(e));
            return erro(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno ao solicitar o acesso.");
        }
    }

    private String textoCadastro(Object valor) {
        if (valor == null) return null;
        String texto = String.valueOf(valor).trim();
        return texto.isBlank() ? null : texto;
    }
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.SUPPORTS)
    public ResponseEntity<?> encerrarSessao(HttpServletRequest request) {
        String token = extrairTokenAutenticacao(request);
        if (token != null && !token.isBlank()) {
            revogacaoSessaoServico.revogarToken(token);
            jwtAuthenticationFilter.removerTokenDoCache(token);
        }

        boolean isSecure = request.isSecure();

        ResponseCookie clearJwt = ResponseCookie.from(ConstantesDominio.COOKIE_AUTH, "")
                .httpOnly(true)
                .secure(isSecure)
                .path("/")
                .maxAge(0)
                .sameSite("Strict")
                .build();

        ResponseCookie clearDevice = ResponseCookie.from(ConstantesDominio.COOKIE_DEVICE, "")
                .httpOnly(true)
                .secure(isSecure)
                .path("/")
                .maxAge(0)
                .sameSite("Strict")
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, clearJwt.toString());
        headers.add(HttpHeaders.SET_COOKIE, clearDevice.toString());
        invalidarCookieCsrf(headers, request);
        headers.setCacheControl("no-store, no-cache, must-revalidate");
        headers.setPragma("no-cache");

        return ResponseEntity.ok().headers(headers).body(Map.of("message", "Logout efetuado"));
    }

    /**
     * O cliente refaz o bootstrap CSRF após mudança real de autenticação.
     * Usa o mesmo nome e caminho do CookieCsrfTokenRepository da cadeia de
     * segurança. Requisições comuns e renovação do mesmo contexto preservam o token.
     */
    private void invalidarCookieCsrf(HttpHeaders headers, HttpServletRequest request) {
        ResponseCookie csrf = ResponseCookie.from("XSRF-TOKEN", "")
                .httpOnly(false).secure(request.isSecure()).path("/").maxAge(0).build();
        headers.add(HttpHeaders.SET_COOKIE, csrf.toString());
    }

    private boolean senhaConfere(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null || encodedPassword.isBlank()) return false;
        try {
            return passwordEncoder.matches(rawPassword, encodedPassword);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private ResponseEntity<Map<String, Object>> erro(HttpStatus status, String message) {
        return erro(status, message, null);
    }

    private ResponseEntity<Map<String, Object>> erro(HttpStatus status, String message, Map<String, Object> extra) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", com.example.siggtrat.utilitario.RelogioSistema.agora().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        if (extra != null && !extra.isEmpty()) body.putAll(extra);
        return ResponseEntity.status(status).body(body);
    }

    private String extrairValorCookie(HttpServletRequest request, String cookieName) {
        if (request == null || request.getCookies() == null) return null;
        for (Cookie c : request.getCookies()) {
            if (c != null && cookieName.equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }

    private String extrairTokenAutenticacao(HttpServletRequest request) {
        String cookieToken = extrairValorCookie(request, ConstantesDominio.COOKIE_AUTH);
        if (cookieToken != null && !cookieToken.isBlank()) return cookieToken;
        if (request == null) return null;

        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String bearer = authorization.substring(7).trim();
            return bearer.isBlank() ? null : bearer;
        }
        return null;
    }

    private String obterIpCliente(HttpServletRequest request) {
        if (request == null) return "";
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) return realIp.trim();
        return Optional.ofNullable(request.getRemoteAddr()).orElse("");
    }
}
