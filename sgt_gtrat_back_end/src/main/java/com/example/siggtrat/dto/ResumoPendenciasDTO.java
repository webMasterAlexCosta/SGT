package com.example.siggtrat.dto;

public record ResumoPendenciasDTO(
        long abertas,
        long emTratamento,
        long resolvidas,
        long retificacoesFrequencia,
        long finsNaturaisVigencia
) { }
