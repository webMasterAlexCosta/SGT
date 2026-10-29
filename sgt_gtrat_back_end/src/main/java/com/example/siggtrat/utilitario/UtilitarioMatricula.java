package com.example.siggtrat.utilitario;

public final class UtilitarioMatricula {

    public static final int QUANTIDADE_DIGITOS = 8;

    private UtilitarioMatricula() {
    }

    public static String apenasDigitos(String valor) {
        return valor == null ? "" : valor.replaceAll("\\D", "");
    }

    public static String normalizar(String valor) {
        if (valor == null || valor.isBlank()) return null;
        String digitos = apenasDigitos(valor);
        if (digitos.length() != QUANTIDADE_DIGITOS) {
            throw new IllegalArgumentException("A matrícula deve conter 8 dígitos no formato 8.961.474-5.");
        }
        return digitos;
    }

    public static String formatar(String valor) {
        String digitos = normalizar(valor);
        if (digitos == null) return null;
        return digitos.substring(0, 1)
                + "." + digitos.substring(1, 4)
                + "." + digitos.substring(4, 7)
                + "-" + digitos.substring(7, 8);
    }

    public static String formatarParaExibicao(String valor) {
        if (valor == null || valor.isBlank()) return valor;
        String digitos = apenasDigitos(valor);
        if (digitos.length() != QUANTIDADE_DIGITOS) return valor.trim();
        return formatar(digitos);
    }
}
