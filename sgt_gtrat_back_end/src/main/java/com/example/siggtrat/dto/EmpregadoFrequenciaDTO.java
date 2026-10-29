package com.example.siggtrat.dto;

public record EmpregadoFrequenciaDTO(
        Long id,
        String nome,
        String matricula,
        String cpf,
        String lotacaoNome,
        Long lotacaoId,
        String turnoDescricao,
        Long turnoId,
        String cargoNome
) {}