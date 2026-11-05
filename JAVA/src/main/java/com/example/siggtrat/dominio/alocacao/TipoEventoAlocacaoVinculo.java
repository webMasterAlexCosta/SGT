package com.example.siggtrat.dominio.alocacao;

/** Fatos imutáveis que formam a trilha de auditoria da linha do tempo de Alocação. */
public enum TipoEventoAlocacaoVinculo {
    CRIACAO,
    ALTERACAO,
    RETIFICACAO,
    CANCELAMENTO_PROGRAMACAO,
    AJUSTE_LIMITE_VINCULO,
    TRANSFERENCIA_GESTAO,
    REPROCESSAMENTO_TRANSFERENCIA
}
