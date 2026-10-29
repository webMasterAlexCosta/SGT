package com.example.siggtrat.dto;

public record EmpregadoContaDTO(
        Long id,
        String numeroConta,
        Long agenciaId,
        String agenciaNumero,
        Long bancoId,
        String bancoNome,
        Long tipoContaId,
        String tipoContaDescricao
) {}