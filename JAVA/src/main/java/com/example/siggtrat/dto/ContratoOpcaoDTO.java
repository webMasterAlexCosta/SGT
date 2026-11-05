package com.example.siggtrat.dto;

public record ContratoOpcaoDTO(
        Long id,
        String numeroContrato,
        String descricao,
        Boolean ativo,
        Long quantidadeColaboradores
) {
}
