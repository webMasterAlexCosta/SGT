package com.example.siggtrat.dto;

/** Recorte original e o recorte equivalente já validado na Gestão Local de destino. */
public record MapeamentoTransferenciaGestaoRequisicao(
        Long composicaoOrigemId,
        Long lotacaoOrigemId,
        Long jornadaOrigemId,
        Long cargoOrigemId,
        Long lotacaoDestinoId,
        Long jornadaDestinoId,
        Long cargoDestinoId
) {}
