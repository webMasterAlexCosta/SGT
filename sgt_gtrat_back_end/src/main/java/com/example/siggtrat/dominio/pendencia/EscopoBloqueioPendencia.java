package com.example.siggtrat.dominio.pendencia;

/** A pendência nunca amplia seu bloqueio para além deste recorte explícito. */
public enum EscopoBloqueioPendencia {
    FREQUENCIA_PERIODO,
    OPERACAO_VINCULO,
    CONTRATO_TEMPORAL
}
