package com.example.siggtrat.servico;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class TotpServico implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(TotpServico.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }


    private static final Pattern BASE32 = Pattern.compile("^[A-Z2-7]+={0,6}$");
    private static final int TAMANHO_MINIMO_SEGREDO = 16;

    private final GoogleAuthenticator gAuth;

    public TotpServico() {
        this.gAuth = new GoogleAuthenticator();
    }

    public String gerarSegredo() {
        GoogleAuthenticatorKey key = gAuth.createCredentials();
        return key.getKey();
    }

    /** Retorna o código TOTP atual para um segredo Base32 válido. */
    public int obterCodigoAtual(String segredo) {
        String normalizado = normalizarEValidar(segredo);
        return gAuth.getTotpPassword(normalizado);
    }

    /**
     * Valida o código sem propagar erros de formato ou decodificação do segredo.
     */
    public boolean validarCodigo(String segredo, int codigo) {
        if (codigo < 0 || codigo > 999_999) return false;
        try {
            return gAuth.authorize(normalizarEValidar(segredo), codigo);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    /** Verifica formato, origem compatível e capacidade de gerar um TOTP. */
    public boolean segredoUtilizavel(String segredo) {
        try {
            obterCodigoAtual(segredo);
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private String normalizarEValidar(String segredo) {
        if (segredo == null || segredo.isBlank()) {
            throw new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio("Segredo de segurança não configurado.");
        }
        String normalizado = segredo.replace(" ", "").trim().toUpperCase(Locale.ROOT);
        if (normalizado.length() < TAMANHO_MINIMO_SEGREDO
                || !BASE32.matcher(normalizado).matches()) {
            throw new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio("Segredo TOTP inválido.");
        }
        return normalizado;
    }
}
