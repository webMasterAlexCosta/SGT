package com.example.siggtrat.dto;

public record EmpregadoContratoDTO(
        Long id,
        String numeroContrato,
        String descricao,
        Boolean ativo
) {}