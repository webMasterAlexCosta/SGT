package com.example.siggtrat.dominio.acesso;

/** Classifica internamente quanto do cadastro pessoal pode ser exposto ao perfil atual. */
public enum NivelExposicaoDadosPessoais {
    COMPLETO,
    OPERACIONAL_MINIMIZADO,
    AUDITORIA_MINIMIZADA,
    INSTITUCIONAL_SEM_DADOS_PESSOAIS
}
