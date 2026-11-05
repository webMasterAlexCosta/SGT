package com.example.siggtrat.dto;

import java.time.Instant;
import java.time.LocalDate;

/** Evidência auditável que autorizou uma inclusão acima da capacidade. */
public record JustificativaExcedenteDTO(
        Long eventoId,
        Long vinculoId,
        LocalDate dataEfetiva,
        Instant registradoEm,
        String responsavelUsername,
        String justificativa,
        String evidencia,
        String autorizacao,
        int efetivoPrevisto,
        long ocupacaoAposInclusao,
        String origemEvento
) {
    public JustificativaExcedenteDTO(Long eventoId, Long vinculoId, LocalDate dataEfetiva,
                                     Instant registradoEm, String responsavelUsername, String justificativa,
                                     String evidencia, String autorizacao, int efetivoPrevisto,
                                     long ocupacaoAposInclusao) {
        this(eventoId, vinculoId, dataEfetiva, registradoEm, responsavelUsername, justificativa,
                evidencia, autorizacao, efetivoPrevisto, ocupacaoAposInclusao, "VINCULO");
    }
}
