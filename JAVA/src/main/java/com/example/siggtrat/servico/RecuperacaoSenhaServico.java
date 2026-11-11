package com.example.siggtrat.servico;

import com.example.siggtrat.configuracao.propriedades.PropriedadesRecuperacaoSenha;
import com.example.siggtrat.dto.ValidacaoTokenRecuperacaoSenhaDTO;
import com.example.siggtrat.entidade.RecuperacaoSenhaToken;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.repositorio.RecuperacaoSenhaTokenRepositorio;
import com.example.siggtrat.repositorio.TokenAtivoRepositorio;
import com.example.siggtrat.repositorio.UsuarioAplicacaoRepositorio;
import com.example.siggtrat.seguranca.FiltroAutenticacaoJwt;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.example.siggtrat.utilitario.UtilitarioHashSeguranca;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.HtmlUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@Service
public class RecuperacaoSenhaServico implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    @Override public org.slf4j.Logger loggerDiagnostico() { return logger; }


    private static final Logger logger = LoggerFactory.getLogger(RecuperacaoSenhaServico.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String RESPOSTA_GENERICA =
            "Se o e-mail estiver vinculado a uma conta ativa, enviaremos as instruções de recuperação.";
    private static final String RESPOSTA_SENHA_TEMPORARIA =
            "Solicitação confirmada. Enviamos uma senha temporária para seu e-mail. Use-a no login e cadastre uma nova senha.";

    private final UsuarioAplicacaoRepositorio usuarioRepositorio;
    private final RecuperacaoSenhaTokenRepositorio tokenRepositorio;
    private final TokenAtivoRepositorio tokenAtivoRepositorio;
    private final PasswordEncoder passwordEncoder;
    private final EmailGmailServico emailServico;
    private final GeradorSenhaTemporariaServico geradorSenhaTemporaria;
    private final FiltroAutenticacaoJwt filtroJwt;
    private final PropriedadesRecuperacaoSenha propriedades;

    public RecuperacaoSenhaServico(
            UsuarioAplicacaoRepositorio usuarioRepositorio,
            RecuperacaoSenhaTokenRepositorio tokenRepositorio,
            TokenAtivoRepositorio tokenAtivoRepositorio,
            PasswordEncoder passwordEncoder,
            EmailGmailServico emailServico,
            GeradorSenhaTemporariaServico geradorSenhaTemporaria,
            FiltroAutenticacaoJwt filtroJwt,
            PropriedadesRecuperacaoSenha propriedades
    ) {
        this.usuarioRepositorio = usuarioRepositorio;
        this.tokenRepositorio = tokenRepositorio;
        this.tokenAtivoRepositorio = tokenAtivoRepositorio;
        this.passwordEncoder = passwordEncoder;
        this.emailServico = emailServico;
        this.geradorSenhaTemporaria = geradorSenhaTemporaria;
        this.filtroJwt = filtroJwt;
        this.propriedades = propriedades;
    }

    /**
     * Sempre devolve a mesma mensagem. Isso evita enumeração de contas por e-mail.
     */
    public String solicitar(String emailInformado, HttpServletRequest request) {
        String email = normalizarEmail(emailInformado);
        Optional<UsuarioAplicacao> usuarioOpt = usuarioRepositorio.findByEmailIgnoreCase(email);
        if (usuarioOpt.isEmpty() || !Boolean.TRUE.equals(usuarioOpt.get().getActive())) {
            return RESPOSTA_GENERICA;
        }

        UsuarioAplicacao usuario = usuarioOpt.get();
        LocalDateTime agora = RelogioSistema.agora();
        Optional<RecuperacaoSenhaToken> ultimo = tokenRepositorio.findTopByUsuario_IdOrderByCreatedAtDesc(usuario.getId());
        if (ultimo.isPresent()
                && ultimo.get().getCreatedAt() != null
                && ultimo.get().getCreatedAt().plusSeconds(propriedades.intervaloMinimoSolicitacaoSegundos()).isAfter(agora)) {
            return RESPOSTA_GENERICA;
        }

        String tokenBruto = gerarTokenSeguro();
        RecuperacaoSenhaToken token = new RecuperacaoSenhaToken();
        token.setTokenHash(UtilitarioHashSeguranca.sha256Hexadecimal(tokenBruto));
        token.setUsuario(usuario);
        token.setCreatedAt(agora);
        token.setExpireAt(agora.plusMinutes(propriedades.tokenMinutos()));
        token.setRequestIpHash(hashIp(request));

        // Somente um link válido por usuário. O token bruto existe apenas em memória
        // até o envio do e-mail pelo Gmail ser concluído.
        tokenRepositorio.deleteByUsuario_Id(usuario.getId());
        tokenRepositorio.saveAndFlush(token);

        String link = construirLink(tokenBruto);
        boolean enviado = emailServico.enviarHtml(
                usuario.getEmail(),
                "Recuperação de senha | SGT",
                construirEmailConfirmacaoHtml(link)
        );
        if (!enviado) {
            if (token.getId() != null) {
                tokenRepositorio.deleteById(token.getId());
            } else {
                tokenRepositorio.deleteByUsuario_Id(usuario.getId());
            }
            logger.error("Não foi possível entregar o e-mail de recuperação para o usuário id={}", usuario.getId());
        }
        return RESPOSTA_GENERICA;
    }

    @Transactional(readOnly = true)
    public ValidacaoTokenRecuperacaoSenhaDTO validar(String tokenBruto) {
        if (tokenBruto == null || tokenBruto.isBlank()) {
            return new ValidacaoTokenRecuperacaoSenhaDTO(false, "Link de recuperação inválido.");
        }
        String hash = UtilitarioHashSeguranca.sha256Hexadecimal(tokenBruto.trim());
        Optional<RecuperacaoSenhaToken> tokenOpt = tokenRepositorio.findByTokenHash(hash);
        if (tokenOpt.isEmpty() || !tokenEstaValido(tokenOpt.get(), RelogioSistema.agora())) {
            return new ValidacaoTokenRecuperacaoSenhaDTO(
                    false,
                    "Este link de recuperação é inválido, expirou ou já foi utilizado."
            );
        }
        return new ValidacaoTokenRecuperacaoSenhaDTO(true, "Link de recuperação válido.");
    }

    /**
     * Consome o link, gera uma senha temporária e a envia por um segundo e-mail.
     * A senha em texto puro só existe em memória durante esta transação.
     */
    @Transactional
    public String confirmar(String tokenBruto) {
        String hash = UtilitarioHashSeguranca.sha256Hexadecimal(tokenBruto == null ? "" : tokenBruto.trim());
        RecuperacaoSenhaToken token = tokenRepositorio.buscarParaConsumo(hash)
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Link de recuperação inválido ou expirado."));

        LocalDateTime agora = RelogioSistema.agora();
        if (!tokenEstaValido(token, agora)) {
            throw new ResponseStatusException(BAD_REQUEST, "Link de recuperação inválido ou expirado.");
        }

        UsuarioAplicacao usuario = token.getUsuario();
        if (!Boolean.TRUE.equals(usuario.getActive())) {
            throw new ResponseStatusException(BAD_REQUEST, "Não foi possível recuperar a senha desta conta.");
        }

        String senhaTemporaria = gerarSenhaTemporariaDiferenteDaAtual(usuario);

        usuario.setPassword(passwordEncoder.encode(senhaTemporaria));
        usuario.setForceChangePassword(true);
        usuario.setFailedAttempts(0);
        usuario.setLockoutEnd(null);
        usuarioRepositorio.saveAndFlush(usuario);

        token.setUsedAt(agora);
        tokenRepositorio.saveAndFlush(token);
        tokenRepositorio.excluirOutrosTokensDoUsuario(usuario.getId(), token.getId());

        boolean enviado = emailServico.enviarHtml(
                usuario.getEmail(),
                "Sua senha temporária | SGT",
                construirEmailSenhaTemporariaHtml(senhaTemporaria)
        );
        if (!enviado) {
            // A exceção faz rollback do hash da nova senha e do consumo do token.
            throw new ResponseStatusException(
                    SERVICE_UNAVAILABLE,
                    "Não foi possível enviar a senha temporária. Tente utilizar o link novamente em instantes."
            );
        }

        // A recuperação invalida todas as sessões previamente emitidas.
        tokenAtivoRepositorio.deleteByUsername(usuario.getUsername());
        filtroJwt.removerSessoesUsuarioDoCache(usuario.getUsername());

        return RESPOSTA_SENHA_TEMPORARIA;
    }

    @Scheduled(cron = "${app.recuperacao-senha.cleanup-cron:0 30 3 * * ?}")
    @Transactional
    public void limparTokensAntigos() {
        LocalDateTime agora = RelogioSistema.agora();
        int removidos = tokenRepositorio.limparExpiradosEConsumidos(agora, agora.minusDays(1));
        if (removidos > 0) {
            logger.info("Limpeza de recuperação de senha removeu {} token(s).", removidos);
        }
    }

    private String gerarSenhaTemporariaDiferenteDaAtual(UsuarioAplicacao usuario) {
        for (int tentativa = 0; tentativa < 5; tentativa++) {
            String candidata = geradorSenhaTemporaria.gerar();
            if (!passwordEncoder.matches(candidata, usuario.getPassword())) {
                return candidata;
            }
        }
        throw new IllegalStateException("Não foi possível gerar uma senha temporária diferente da senha atual.");
    }

    private boolean tokenEstaValido(RecuperacaoSenhaToken token, LocalDateTime agora) {
        return token.getUsedAt() == null
                && token.getExpireAt() != null
                && token.getExpireAt().isAfter(agora);
    }

    private String construirLink(String tokenBruto) {
        String base = propriedades.frontendUrl().trim().replaceAll("/+$", "");
        return base + "/recuperar-senha?token="
                + URLEncoder.encode(tokenBruto, StandardCharsets.UTF_8);
    }

    private String construirEmailConfirmacaoHtml(String link) {
        long minutos = propriedades.tokenMinutos();
        String linkSeguro = HtmlUtils.htmlEscape(link);
        return layoutEmail(
                "Recuperação de senha",
                "Confirme que foi você",
                "Recebemos uma solicitação para recuperar o acesso ao <strong>SGT</strong>.",
                """
                <p style="margin:0 0 22px;color:#334155;font-size:15px;line-height:1.65;">
                  Para sua segurança, a senha atual ainda não foi alterada. Clique no botão abaixo para confirmar a solicitação.
                  O link expira em <strong>%d minutos</strong> e só pode ser usado uma vez.
                </p>
                <table role="presentation" cellspacing="0" cellpadding="0" border="0" style="margin:0 auto 24px;">
                  <tr>
                    <td style="border-radius:8px;background:#FFD400;">
                      <a href="%s" style="display:inline-block;padding:14px 26px;color:#003B71;text-decoration:none;font-weight:700;font-size:15px;">
                        Confirmar recuperação de senha
                      </a>
                    </td>
                  </tr>
                </table>
                <p style="margin:0;color:#64748b;font-size:13px;line-height:1.6;">
                  Se você não solicitou esta recuperação, ignore esta mensagem. Sua senha continuará inalterada.
                </p>
                """.formatted(minutos, linkSeguro)
        );
    }

    private String construirEmailSenhaTemporariaHtml(String senhaTemporaria) {
        String senhaSegura = HtmlUtils.htmlEscape(senhaTemporaria);
        return layoutEmail(
                "Acesso temporário",
                "Sua senha temporária está pronta",
                "A recuperação foi confirmada com sucesso.",
                """
                <p style="margin:0 0 18px;color:#334155;font-size:15px;line-height:1.65;">
                  Use a senha abaixo para entrar no <strong>SGT</strong>:
                </p>
                <div style="margin:0 0 22px;padding:18px;border:1px solid #dbe5ee;border-left:5px solid #FFD400;border-radius:8px;background:#f8fafc;text-align:center;">
                  <div style="margin-bottom:6px;color:#64748b;font-size:12px;text-transform:uppercase;letter-spacing:.08em;font-weight:700;">Senha temporária</div>
                  <div style="font-family:Consolas,'Courier New',monospace;color:#003B71;font-size:22px;font-weight:700;letter-spacing:.04em;word-break:break-all;">%s</div>
                </div>
                <p style="margin:0 0 12px;color:#334155;font-size:15px;line-height:1.65;">
                  No primeiro login, o sistema exigirá o cadastro de uma <strong>nova senha pessoal</strong> antes de liberar o acesso normal.
                </p>
                <p style="margin:0;color:#64748b;font-size:13px;line-height:1.6;">
                  Não compartilhe esta senha. Todas as sessões anteriores da sua conta foram encerradas por segurança.
                </p>
                """.formatted(senhaSegura)
        );
    }

    private String layoutEmail(String etiqueta, String titulo, String introducao, String conteudo) {
        return """
                <!doctype html>
                <html lang="pt-BR">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width,initial-scale=1">
                  <title>%s</title>
                </head>
                <body style="margin:0;padding:0;background:#eef3f7;font-family:Arial,Helvetica,sans-serif;color:#1f2937;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="background:#eef3f7;padding:32px 14px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="max-width:620px;background:#ffffff;border-radius:14px;overflow:hidden;box-shadow:0 8px 28px rgba(15,23,42,.10);">
                          <tr>
                            <td style="height:7px;background:#FFD400;font-size:0;line-height:0;">&nbsp;</td>
                          </tr>
                          <tr>
                            <td style="padding:26px 34px 18px;border-bottom:1px solid #e7edf3;">
                              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0">
                                <tr>
                                  <td valign="middle">
                                    <img src="cid:%s" alt="Correios" style="display:block;max-width:145px;height:auto;border:0;">
                                  </td>
                                  <td align="right" valign="middle" style="color:#003B71;font-size:12px;font-weight:700;letter-spacing:.05em;text-transform:uppercase;">
                                    SGT
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:34px;">
                              <div style="display:inline-block;margin-bottom:12px;padding:6px 10px;border-radius:999px;background:#e8f1f8;color:#003B71;font-size:11px;font-weight:700;text-transform:uppercase;letter-spacing:.06em;">%s</div>
                              <h1 style="margin:0 0 12px;color:#003B71;font-size:25px;line-height:1.25;">%s</h1>
                              <p style="margin:0 0 22px;color:#475569;font-size:15px;line-height:1.65;">%s</p>
                              %s
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:20px 34px;background:#003B71;color:#ffffff;font-size:12px;line-height:1.6;">
                              Mensagem automática de segurança do SGT. Por favor, não responda a este e-mail.
                            </td>
                          </tr>
                        </table>
                        <div style="max-width:620px;padding:14px 8px 0;color:#7c8a99;font-size:11px;line-height:1.5;text-align:center;">
                          Correios do Brasil • Segurança de acesso
                        </div>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(
                HtmlUtils.htmlEscape(titulo),
                EmailGmailServico.CID_LOGO_CORREIOS,
                HtmlUtils.htmlEscape(etiqueta),
                HtmlUtils.htmlEscape(titulo),
                introducao,
                conteudo
        );
    }

    private static String gerarTokenSeguro() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String normalizarEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static String hashIp(HttpServletRequest request) {
        if (request == null) return null;
        String ip = request.getRemoteAddr();
        return ip == null || ip.isBlank() ? null : UtilitarioHashSeguranca.sha256Hexadecimal(ip);
    }
}
