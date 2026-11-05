package com.example.siggtrat.dto;

public record MapeamentoTransferenciaGestaoDTO(
        Long id,
        Long composicaoOrigemId,
        Long lotacaoOrigemId,
        String lotacaoOrigemNome,
        Long jornadaOrigemId,
        String jornadaOrigemDescricao,
        Long cargoOrigemId,
        String cargoOrigemNome,
        Long lotacaoDestinoId,
        String lotacaoDestinoNome,
        Long jornadaDestinoId,
        String jornadaDestinoDescricao,
        Long cargoDestinoId,
        String cargoDestinoNome
) {}
