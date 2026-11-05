package com.example.siggtrat.entidade;

/**
 * Estado negocial persistido do contrato.
 *
 * FUTURO/VIGENTE/ENCERRADO não são gravados aqui: são situações temporais
 * derivadas da vigência e da data consultada.
 */
public enum EstadoNegocialContrato {
    RASCUNHO,
    PUBLICADO,
    CANCELADO,
    ANULADO
}
