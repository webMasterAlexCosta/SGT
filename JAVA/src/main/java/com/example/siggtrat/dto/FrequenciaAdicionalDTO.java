package com.example.siggtrat.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FrequenciaAdicionalDTO(
        Long id,
        Long empregadoId,
        String empregadoNome,
        LocalDate dataReferencia,
        String tipo,
        Integer minutos,
        BigDecimal quantidade,
        String status,
        String justificativa,
        Long frequenciaId,
        String origem
) {
    public FrequenciaAdicionalDTO(Long id, Long empregadoId, String empregadoNome, LocalDate dataReferencia,
            String tipo, Integer minutos, BigDecimal quantidade, String status, String justificativa, Long frequenciaId) {
        this(id, empregadoId, empregadoNome, dataReferencia, tipo, minutos, quantidade, status, justificativa,
                frequenciaId, frequenciaId == null ? "MANUAL" : "DERIVADO");
    }
}
