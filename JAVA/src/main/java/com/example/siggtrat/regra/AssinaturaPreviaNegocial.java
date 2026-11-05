package com.example.siggtrat.regra;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;

/**
 * Assinatura técnica de uma prévia de operação crítica.
 *
 * Não participa da precedência negocial. Serve exclusivamente para detectar
 * alteração do estado relevante entre a prévia apresentada ao usuário e a
 * confirmação transacional.
 */
public final class AssinaturaPreviaNegocial {

    private AssinaturaPreviaNegocial() {
    }

    public static String sha256(Collection<String> partes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            if (partes != null) {
                for (String parte : partes) {
                    digest.update(normalizar(parte).getBytes(StandardCharsets.UTF_8));
                    digest.update((byte) 0x1f);
                }
            }
            byte[] bytes = digest.digest();
            StringBuilder out = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) out.append(String.format("%02x", b));
            return out.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível.", e);
        }
    }

    public static String valor(Object valor) {
        return valor == null ? "<null>" : String.valueOf(valor);
    }

    private static String normalizar(String valor) {
        return valor == null ? "<null>" : valor;
    }
}
