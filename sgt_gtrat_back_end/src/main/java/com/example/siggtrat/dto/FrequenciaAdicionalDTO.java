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
        Long frequenciaId
) {}