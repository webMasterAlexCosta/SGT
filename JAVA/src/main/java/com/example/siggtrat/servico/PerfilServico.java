package com.example.siggtrat.servico;

import com.example.siggtrat.configuracao.propriedades.PropriedadesArmazenamentoArquivo;
import com.example.siggtrat.configuracao.propriedades.PropriedadesAutenticacao;
import com.example.siggtrat.dto.*;
import com.example.siggtrat.configuracao.propriedades.PropriedadesSegundoFator;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.repositorio.UsuarioAplicacaoRepositorio;
import com.example.siggtrat.repositorio.TokenAtivoRepositorio;
import com.example.siggtrat.seguranca.FiltroAutenticacaoJwt;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@org.springframework.stereotype.Service
public class PerfilServico implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(PerfilServico.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }


    private static final long MAX_AVATAR_BYTES = 5L * 1024L * 1024L;
    private static final Set<String> AVATAR_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final UsuarioAplicacaoRepositorio userRepositorio;
    private final PasswordEncoder passwordEncoder;
    private final TotpServico totpServico;
    private final CodigoQrServico qrCodeServico;
    private final TokenAtivoRepositorio tokenAtivoRepositorio;
    private final FiltroAutenticacaoJwt jwtAuthenticationFilter;

    private final String uploadDir;
    private final int totpPeriodSeconds;
    private final int passwordMinLength;

    public PerfilServico(UsuarioAplicacaoRepositorio userRepositorio,
                             PasswordEncoder passwordEncoder,
                             TotpServico totpServico,
                             CodigoQrServico qrCodeServico,
                             TokenAtivoRepositorio tokenAtivoRepositorio,
                             FiltroAutenticacaoJwt jwtAuthenticationFilter,
                             PropriedadesArmazenamentoArquivo fileStorageProperties,
                             PropriedadesSegundoFator twoFactorProperties,
                             PropriedadesAutenticacao authProperties) {
        this.userRepositorio = userRepositorio;
        this.passwordEncoder = passwordEncoder;
        this.totpServico = totpServico;
        this.qrCodeServico = qrCodeServico;
        this.tokenAtivoRepositorio = tokenAtivoRepositorio;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.uploadDir = fileStorageProperties.uploadDir().toString();
        this.totpPeriodSeconds = twoFactorProperties.periodSeconds();
        this.passwordMinLength = authProperties.passwordMinLength();
    }
    public ResponseEntity<?> statusSegundoFator() {
        UsuarioAplicacao user = obterUsuarioAutenticado();
        if (user == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(new StatusSegundoFatorDTO(Boolean.TRUE.equals(user.getTwoFactorEnabled()), user.getPendingSecret2fa() != null));
    }
    public ResponseEntity<?> configurarSegundoFator(SenhaAtualDTO payload) {
        UsuarioAplicacao user = obterUsuarioAutenticado();
        if (user == null) return ResponseEntity.status(401).build();
        String password = payload != null ? payload.currentPassword() : null;
        if (!senhaConfere(password, user.getPassword())) {
            return ResponseEntity.badRequest().body(MensagemApiDTO.sucesso("Senha atual incorreta."));
        }
        if (Boolean.TRUE.equals(user.getTwoFactorEnabled())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(MensagemApiDTO.sucesso("O segundo fator já está ativo. Desative-o antes de configurar outro autenticador."));
        }
        String secret = totpServico.gerarSegredo();
        String conta = URLEncoder.encode(user.getUsername(), StandardCharsets.UTF_8);
        String issuer = URLEncoder.encode("SGT-GTRAT", StandardCharsets.UTF_8);
        String uri = "otpauth://totp/SGT-GTRAT:" + conta + "?secret=" + secret
                + "&issuer=" + issuer + "&digits=6&period=" + totpPeriodSeconds;
        String qrCodeDataUrl = qrCodeServico.gerarDataUrl(uri);
        user.setPendingSecret2fa(secret);
        userRepositorio.saveAndFlush(user);
        return ResponseEntity.ok(new ConfiguracaoSegundoFatorDTO(secret, uri, qrCodeDataUrl, totpPeriodSeconds, "Adicione a conta no autenticador e confirme um código para ativar."));
    }
    public ResponseEntity<?> ativarSegundoFator(TokenTotpDTO payload) {
        UsuarioAplicacao user = obterUsuarioAutenticado();
        if (user == null) return ResponseEntity.status(401).build();
        String token = payload != null ? payload.token() : null;
        if (user.getPendingSecret2fa() == null || token == null || !token.matches("\\d{6}")
                || !totpServico.validarCodigo(user.getPendingSecret2fa(), Integer.parseInt(token))) {
            return ResponseEntity.badRequest().body(MensagemApiDTO.sucesso("Código TOTP inválido ou expirado."));
        }
        user.setSecret2fa(user.getPendingSecret2fa());
        user.setPendingSecret2fa(null);
        user.setTwoFactorEnabled(true);
        userRepositorio.saveAndFlush(user);
        return ResponseEntity.ok(new ResultadoSegundoFatorDTO(true, "Segundo fator ativado."));
    }
    public ResponseEntity<?> desativarSegundoFator(DesativacaoSegundoFatorDTO payload) {
        UsuarioAplicacao user = obterUsuarioAutenticado();
        if (user == null) return ResponseEntity.status(401).build();
        String password = payload != null ? payload.currentPassword() : null;
        String token = payload != null ? payload.token() : null;
        if (!senhaConfere(password, user.getPassword()) || token == null || !token.matches("\\d{6}")
                || !totpServico.validarCodigo(user.getSecret2fa(), Integer.parseInt(token))) {
            return ResponseEntity.badRequest().body(MensagemApiDTO.sucesso("Senha ou código TOTP inválido."));
        }
        user.setTwoFactorEnabled(false);
        user.setSecret2fa(null);
        user.setPendingSecret2fa(null);
        userRepositorio.saveAndFlush(user);
        return ResponseEntity.ok(new ResultadoSegundoFatorDTO(false, "Segundo fator desativado."));
    }
    public ResponseEntity<?> alterarSenha(AlteracaoSenhaPerfilDTO payload) {
        UsuarioAplicacao user = obterUsuarioAutenticado();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        if (!senhaConfere(payload.currentPassword(), user.getPassword())) {
            return ResponseEntity.badRequest().body(MensagemApiDTO.sucesso("Senha atual incorreta."));
        }
        if (payload.newPassword().length() < passwordMinLength) {
            return ResponseEntity.badRequest().body(MensagemApiDTO.sucesso("A nova senha deve ter no mínimo " + passwordMinLength + " caracteres."));
        }
        if (payload.newPassword().length() > 72) {
            return ResponseEntity.badRequest().body(MensagemApiDTO.sucesso("A nova senha deve ter no máximo 72 caracteres."));
        }
        if (senhaConfere(payload.newPassword(), user.getPassword())) {
            return ResponseEntity.badRequest().body(MensagemApiDTO.sucesso("A nova senha deve ser diferente da senha atual."));
        }

        if (Boolean.TRUE.equals(user.getTwoFactorEnabled())) {
            if (payload.token() == null || !payload.token().matches("\\d{6}")
                    || !totpServico.validarCodigo(user.getSecret2fa(), Integer.parseInt(payload.token()))) {
                return ResponseEntity.badRequest().body(MensagemApiDTO.sucesso("Código de segurança (2FA) incorreto ou expirado."));
            }
        }

        user.setPassword(passwordEncoder.encode(payload.newPassword()));
        user.setForceChangePassword(false);
        user.setFailedAttempts(0);
        user.setLockoutEnd(null);
        userRepositorio.saveAndFlush(user);

        // A sessão autenticada atual permanece válida após a troca da própria senha.
        // O autenticar já trabalha com uma única sessão ativa por usuário, então não há
        // outra sessão concorrente para revogar aqui. Logout continua revogando o token.
        return ResponseEntity.ok(new ResultadoAlteracaoSenhaDTO("Senha alterada com sucesso.", false));
    }
    public ResponseEntity<?> validarSenha(ValidacaoSenhaDTO payload) {
        UsuarioAplicacao user = obterUsuarioAutenticado();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        String password = payload != null ? payload.password() : null;
        if (password == null || password.isBlank() || password.length() > 72) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResultadoValidacaoSenhaDTO(false, "Informe uma senha válida."));
        }

        if (senhaConfere(password, user.getPassword())) {
            return ResponseEntity.ok(new ResultadoValidacaoSenhaDTO(true, "Senha validada com sucesso."));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResultadoValidacaoSenhaDTO(false, "Senha incorreta. Acesso negado."));
        }
    }
    public ResponseEntity<?> enviarAvatar(MultipartFile file) {
        UsuarioAplicacao user = obterUsuarioAutenticado();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(MensagemApiDTO.sucesso("Selecione uma imagem válida."));
        }
        if (file.getSize() > MAX_AVATAR_BYTES) {
            return ResponseEntity.badRequest().body(MensagemApiDTO.sucesso("A imagem deve possuir no máximo 5 MB."));
        }

        String contentType = file.getContentType() == null
                ? ""
                : file.getContentType().trim().toLowerCase(Locale.ROOT);
        if (!AVATAR_CONTENT_TYPES.contains(contentType)) {
            return ResponseEntity.badRequest().body(MensagemApiDTO.sucesso("Formato inválido. Utilize JPG, PNG ou WebP."));
        }

        try {
            Path raizUpload = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(raizUpload);

            String extensao = switch (contentType) {
                case "image/png" -> ".png";
                case "image/webp" -> ".webp";
                default -> ".jpg";
            };

            String nomeArquivo = "avatar_" + user.getId() + "_" + System.currentTimeMillis() + extensao;
            Path targetLocation = raizUpload.resolve(nomeArquivo).normalize();
            if (!targetLocation.startsWith(raizUpload)) {
                return ResponseEntity.badRequest().body(MensagemApiDTO.sucesso("Nome de arquivo inválido."));
            }
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            String fotoUrl = "/api/perfil/avatar/" + nomeArquivo;
            user.setFotoUrl(fotoUrl);
            userRepositorio.save(user);

            return ResponseEntity.ok(new ResultadoEnvioAvatarDTO(fotoUrl, "Avatar atualizado com sucesso"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(MensagemApiDTO.sucesso("Erro ao salvar imagem"));
        }
    }
    public ResponseEntity<Resource> obterAvatar(String nomeArquivo) {
        try {
            if (nomeArquivo == null || !nomeArquivo.matches("avatar_\\d+_\\d+\\.(?:jpg|png|webp)")) {
                return ResponseEntity.notFound().build();
            }
            Path raizUpload = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path filePath = raizUpload.resolve(nomeArquivo).normalize();
            if (!filePath.startsWith(raizUpload)) return ResponseEntity.notFound().build();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + nomeArquivo + "\"")
                        .body(resource);
            }
        } catch (Exception e) {
            // Ignora erro e cai no notFound
        }
        return ResponseEntity.notFound().build();
    }

    private boolean senhaConfere(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null || encodedPassword.isBlank()) return false;
        try {
            return passwordEncoder.matches(rawPassword, encodedPassword);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private UsuarioAplicacao obterUsuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        return userRepositorio.findByUsername(auth.getName()).orElse(null);
    }
}
