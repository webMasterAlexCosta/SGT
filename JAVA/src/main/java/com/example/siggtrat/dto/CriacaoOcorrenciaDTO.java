package com.example.siggtrat.dto;

import java.time.LocalDate;

public record CriacaoOcorrenciaDTO(
        Long empregadoId,
        String motivo,
        LocalDate dataOcorrencia,
        String observacao,
        String tipoRo,
        String numeroSei,
        Integer dias,
        java.util.List<String> classificacoesAnexos,
        String chaveIdempotencia
) {
    public CriacaoOcorrenciaDTO(Long empregadoId, String motivo, java.time.LocalDate dataOcorrencia,
                               String observacao, String tipoRo, String numeroSei, Integer dias) {
        this(empregadoId, motivo, dataOcorrencia, observacao, tipoRo, numeroSei, dias, null, null);
    }
}
