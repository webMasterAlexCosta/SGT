package com.example.siggtrat.entidade;

/**
 * Situação corrente de conveniência do fato. A reconstrução bitemporal não se
 * apoia somente neste campo: relações e data/hora de conhecimento também são
 * consideradas pelo motor temporal.
 */
public enum SituacaoFatoNegocial {
    VIGENTE,
    RETIFICADO,
    CANCELADO,
    SUBSTITUIDO
}
