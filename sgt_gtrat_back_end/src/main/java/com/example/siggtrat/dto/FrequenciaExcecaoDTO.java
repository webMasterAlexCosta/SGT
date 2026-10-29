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
        String observacao
) {}