package com.example.siggtrat.dto;

import java.time.Instant;
import java.time.LocalDate;

/** Identidade e contexto elegíveis da frequência em D/K; não representa o cadastro histórico integral. */
public record VinculoElegivelFrequenciaDTO(
        Long id, Long pessoaId, String pessoaNome, String pessoaCpf,
        Long contratoId, String contratoNumero, Integer contratoAno, String matricula,
        LocalDate dataReferencia, Instant conhecidoAte, Long gestaoLocalId,
        Long alocacaoId, Long lotacaoId, Long jornadaId, Long cargoId
) {}
