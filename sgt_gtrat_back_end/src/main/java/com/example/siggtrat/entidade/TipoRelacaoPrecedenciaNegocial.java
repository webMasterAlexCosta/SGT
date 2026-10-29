package com.example.siggtrat.entidade;

/**
 * Relação negocial explícita entre fatos temporais.
 *
 * A ordem técnica de persistência (ID/timestamp) nunca substitui esta relação.
 */
public enum TipoRelacaoPrecedenciaNegocial {
    SUCEDE,
    RETIFICA,
    CANCELA,
    SUBSTITUI,
    MANTEM_COEXISTE
}
