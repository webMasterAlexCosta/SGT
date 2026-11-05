package com.example.siggtrat.dominio.acesso;

/** Operações que exigem concessão administrativa explícita e revogável. */
public enum TipoPermissaoExcepcional {
    CORRECAO_IDENTIFICADOR_CONTRATO,
    REDUCAO_CONTRATUAL_EXCEPCIONAL,
    TRANSFERENCIA_GESTAO_LOCAL,
    INCORPORACAO_PROVA_HISTORICA,
    ACESSO_DADOS_PESSOAIS_RESTRITOS,
    INTERVENCAO_NEGOCIAL_TECNICA
}
