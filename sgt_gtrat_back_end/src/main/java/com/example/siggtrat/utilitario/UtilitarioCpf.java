package com.example.siggtrat.utilitario;

public final class UtilitarioCpf {

    private UtilitarioCpf() {}

    public static String somenteDigitos(String value) {
        if (value == null) return null;
        return value.replaceAll("\\D", "");
    }

    public static boolean ehValido(String cpf) {
        if (cpf == null) return false;
        String d = somenteDigitos(cpf);
        if (d == null || d.length() != 11) return false;
        // Rejeita sequências do tipo 00000000000
        boolean allEqual = true;
        for (int i = 1; i < d.length(); i++) {
            if (d.charAt(i) != d.charAt(0)) {
                allEqual = false;
                break;
            }
        }
        if (allEqual) return false;

        int dig1 = calcularDigito(d, 9, 10);
        int dig2 = calcularDigito(d, 10, 11);

        return dig1 == Character.getNumericValue(d.charAt(9))
                && dig2 == Character.getNumericValue(d.charAt(10));
    }

    private static int calcularDigito(String d, int len, int weightStart) {
        int sum = 0;
        int weight = weightStart;
        for (int i = 0; i < len; i++) {
            sum += Character.getNumericValue(d.charAt(i)) * weight--;
        }
        int mod = (sum * 10) % 11;
        return mod == 10 ? 0 : mod;
    }
}
