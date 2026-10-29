package com.example.siggtrat.dto;

import java.time.LocalDate;

public record ComposicaoContratualDTO(
        Long id,
        Long contratoId,
        Long lotacaoId,
        String lotacaoNome,
        Long jornadaId,
        String jornadaDescricao,
        Long cargoId,
        String cargoNome,
        Integer efetivoPrevisto,
        LocalDate dataInicioVigencia,
        LocalDate dataFimVigencia,
        boolean permiteNovaAlocacao,
        Long version
) {}
