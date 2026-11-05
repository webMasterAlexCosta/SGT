package com.example.siggtrat.dto;

public record PainelRankingDTO(
        Long lotacaoId,
        String nome,
        Long turnoId,
        String turno,
        int escalados,
        int presencas,
        int faltas,
        int taxa
) {}