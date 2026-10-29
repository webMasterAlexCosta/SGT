package com.example.siggtrat.dto;

import com.example.siggtrat.dominio.alocacao.SituacaoTemporalAlocacao;

import java.time.Instant;
import java.time.LocalDate;

/** Resultado bitemporal da Alocação que valia na data informada. */
public record PosicaoAlocacaoTemporalDTO(
        Long vinculoId,
        LocalDate dataReferencia,
        Instant conhecidoAte,
        SituacaoTemporalAlocacao situacaoTemporal,
        Long alocacaoId,
        Long composicaoBaseId,
        Long lotacaoId,
        String lotacaoNome,
        Long jornadaId,
        String jornadaDescricao,
        Long cargoId,
        String cargoNome,
        LocalDate dataInicio,
        LocalDate dataFim,
        Long eventoConhecidoMaisRecenteId
) {}
