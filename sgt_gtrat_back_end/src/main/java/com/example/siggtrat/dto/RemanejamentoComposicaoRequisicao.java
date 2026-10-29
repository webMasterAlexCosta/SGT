package com.example.siggtrat.dto;

public record RemanejamentoComposicaoRequisicao(
        Long composicaoId,
        Long lotacaoId,
        Long jornadaId,
        Long cargoId,
        Integer variacao
) {}
