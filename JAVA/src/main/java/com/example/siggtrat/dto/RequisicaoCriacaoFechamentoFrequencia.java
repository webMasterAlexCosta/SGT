package com.example.siggtrat.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record RequisicaoCriacaoFechamentoFrequencia(
        @NotNull LocalDate dataReferencia,
        @NotNull @Positive Long lotacaoId,
        @NotNull @Positive Long jornadaId,
        List<@Positive Long> contratoIds,
        List<@Valid FrequenciaDTO> alteracoes,
        @Size(max = 1000) String justificativaRetificacao,
        @Size(max = 120) String chaveIdempotencia
) {
    public RequisicaoCriacaoFechamentoFrequencia(LocalDate dataReferencia, Long lotacaoId, Long jornadaId,
                                                List<Long> contratoIds, List<FrequenciaDTO> alteracoes) {
        this(dataReferencia, lotacaoId, jornadaId, contratoIds, alteracoes, null, null);
    }

    public RequisicaoCriacaoFechamentoFrequencia {
        contratoIds = contratoIds == null ? List.of() : contratoIds;
        alteracoes = alteracoes == null ? List.of() : alteracoes;
    }
}
