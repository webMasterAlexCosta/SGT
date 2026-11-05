package com.example.siggtrat.regra;

import java.util.Collection;
import java.util.Objects;

/**
 * Regras matemáticas puras do efetivo contratual.
 *
 * Mantidas fora de serviços/JPA para que o limite de redução e a invariância
 * do remanejamento possam ser testados sem infraestrutura.
 */
public final class RegraEfetivoContratual {
    private RegraEfetivoContratual() {}

    /** Piso normal = teto de 75% do efetivo originalmente publicado. */
    public static int pisoNormal(int efetivoOriginalPublicado) {
        if (efetivoOriginalPublicado < 0) {
            throw new IllegalArgumentException("Efetivo originalmente publicado não pode ser negativo.");
        }
        // ceil(original * 0.75), sem ponto flutuante.
        return (int) ((efetivoOriginalPublicado * 75L + 99) / 100);
    }

    public static boolean reducaoExcedeLimiteNormal(int efetivoOriginalPublicado, int efetivoResultante) {
        if (efetivoResultante < 0) {
            throw new IllegalArgumentException("Efetivo resultante não pode ser negativo.");
        }
        return efetivoResultante < pisoNormal(efetivoOriginalPublicado);
    }

    public static long somaVariacoes(Collection<Integer> variacoes) {
        if (variacoes == null) return 0;
        return variacoes.stream().filter(Objects::nonNull).mapToLong(Integer::longValue).sum();
    }

    public static boolean remanejamentoPreservaTotal(Collection<Integer> variacoes) {
        return somaVariacoes(variacoes) == 0;
    }
}
