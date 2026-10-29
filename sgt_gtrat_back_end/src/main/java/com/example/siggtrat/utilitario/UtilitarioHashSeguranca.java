package com.example.siggtrat.utilitario;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class UtilitarioHashSeguranca {

    private UtilitarioHashSeguranca() {}

    public static String sha256Hexadecimal(String input) {
        if (input == null) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 deve existir em qualquer JVM padrão
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
