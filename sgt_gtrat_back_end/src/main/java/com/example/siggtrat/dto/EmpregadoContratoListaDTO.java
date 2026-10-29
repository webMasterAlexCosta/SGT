package com.example.siggtrat.dto;

public record EmpregadoContratoListaDTO(
        Long empregadoId,
        Long contratoId,
        String numeroContrato,
        String descricao,
        Boolean ativo
) {
}
