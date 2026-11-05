package com.example.siggtrat.dto;

import java.time.LocalDate;

/** Escopo comprovado de um dia, independente da projeção atual do Empregado. */
public record ContextoComplementoFrequenciaDTO(LocalDate dataReferencia, Long contratoId, Long vinculoId,
        Long alocacaoId, Long gestaoLocalId, Long superintendenciaId, Long lotacaoId, Long jornadaId) { }
