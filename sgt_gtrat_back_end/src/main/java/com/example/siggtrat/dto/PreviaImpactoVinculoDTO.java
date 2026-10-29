package com.example.siggtrat.dto;

import java.time.LocalDate;
import java.util.List;

public record PreviaImpactoVinculoDTO(
        String token,
        Long vinculoId,
        LocalDate valorAtual,
        LocalDate valorProposto,
        List<String> impactos,
        List<Long> vinculosConflitantes
) {}
