package com.example.siggtrat.utilitario;

public final class UtilitarioMatriculaEmpregado {

    public static final int TAMANHO_MAXIMO = 30;

    private UtilitarioMatriculaEmpregado() {
    }

    public static String normalizar(String valor) {
        if (valor == null || valor.isBlank()) return null;

        String normalizado = valor.trim();
        if (normalizado.length() > TAMANHO_MAXIMO) {
            throw new IllegalArgumentException(
                    "A matrícula deve ter no máximo " + TAMANHO_MAXIMO + " caracteres."
            );
        }
        return normalizado;
    }
}
