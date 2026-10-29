package com.example.siggtrat.dominio.alocacao;

/** Relação explícita entre eventos que alteram a mesma linha temporal de alocação. */
public enum TipoRelacaoEventoAlocacao {
    ORIGEM,
    SUBSTITUI,
    RETIFICA,
    CANCELA,
    AJUSTA
}
