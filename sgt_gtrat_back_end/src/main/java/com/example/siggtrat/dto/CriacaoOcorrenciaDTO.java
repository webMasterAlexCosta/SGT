package com.example.siggtrat.dto;

import java.time.LocalDate;

public record CriacaoOcorrenciaDTO(
        Long empregadoId,
        String motivo,
        LocalDate dataOcorrencia,
        String observacao,
        String tipoRo,
        String numeroSei,
        Integer dias
) {}
