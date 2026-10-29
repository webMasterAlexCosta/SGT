package com.example.siggtrat.dto;

public record AditamentoComposicaoRequisicao(
        Long composicaoId,
        Long lotacaoId,
        Long jornadaId,
        Long cargoId,
        Integer quantidadeResultante
) {}
