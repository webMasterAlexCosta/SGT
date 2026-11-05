package com.example.siggtrat.dto;

import java.time.LocalDate;
import java.util.List;

public record PreviaRemanejamentoContratualDTO(
        Long contratoId,
        LocalDate dataEfetiva,
        Integer efetivoTotalAntes,
        Integer efetivoTotalDepois,
        Integer somaVariacoes,
        boolean totalPreservado,
        boolean possuiImpactoOcupacao,
        List<ImpactoComposicaoContratoDTO> impactosComposicao,
        List<String> alertas,
        String tokenPrevia
) {}
