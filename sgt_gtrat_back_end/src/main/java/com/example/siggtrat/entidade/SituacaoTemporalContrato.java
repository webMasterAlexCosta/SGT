package com.example.siggtrat.entidade;

/**
 * Situação apresentada para uma data de referência.
 *
 * FUTURO, VIGENTE e ENCERRADO são derivados; CANCELADO e ANULADO decorrem
 * de eventos negociais explícitos.
 */
public enum SituacaoTemporalContrato {
    RASCUNHO,
    FUTURO,
    VIGENTE,
    ENCERRADO,
    CANCELADO,
    ANULADO
}
