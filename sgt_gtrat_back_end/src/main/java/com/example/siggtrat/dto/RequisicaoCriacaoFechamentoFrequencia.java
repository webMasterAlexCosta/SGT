package com.example.siggtrat.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.List;

public record RequisicaoCriacaoFechamentoFrequencia(
        @NotNull LocalDate dataReferencia,
        @NotNull @Positive Long lotacaoId,
        @NotNull @Positive Long jornadaId,
        List<@Positive Long> contratoIds,
        List<@Valid FrequenciaDTO> alteracoes
) {
    public RequisicaoCriacaoFechamentoFrequencia {
        contratoIds = contratoIds == null ? List.of() : contratoIds;
        alteracoes = alteracoes == null ? List.of() : alteracoes;
    }
}
