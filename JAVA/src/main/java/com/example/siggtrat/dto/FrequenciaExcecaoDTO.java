package com.example.siggtrat.dto;

import java.time.LocalDate;

public record FrequenciaExcecaoDTO(
        Long id,
        Long empregadoId,
        String empregadoNome,
        LocalDate dataInicio,
        LocalDate dataFim,
        String tipo,
        String status,
        String observacao,
        String justificativa
) {
    public FrequenciaExcecaoDTO(Long id, Long empregadoId, String empregadoNome, LocalDate dataInicio,
            LocalDate dataFim, String tipo, String status, String observacao) {
        this(id, empregadoId, empregadoNome, dataInicio, dataFim, tipo, status, observacao, null);
    }
}
