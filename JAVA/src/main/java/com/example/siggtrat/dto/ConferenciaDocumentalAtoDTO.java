package com.example.siggtrat.dto;

/** Null significa desconhecido no histórico; não significa conferido nem dispensado. */
public record ConferenciaDocumentalAtoDTO(String versaoDeclaracao, String declaracao,
        String versaoIntencao, String intencaoCanonica, String hashIntencao,
        String autorizacaoAdministrativaAto, Long usoPermissaoExcepcionalId, Long permissaoExcepcionalId) {

}
