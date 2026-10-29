package com.example.siggtrat.dominio.alocacao;

/** Situação da alocação na data consultada, sem confundir auditoria com efeito temporal. */
public enum SituacaoTemporalAlocacao {
    PROGRAMADA,
    VIGENTE,
    ENCERRADA,
    CANCELADA,
    SEM_ALOCACAO
}
