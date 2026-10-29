package com.example.siggtrat.dto;

public record SubstituicaoDocumentoComprobatorioRequisicao(
        String titulo,
        String descricao,
        String referenciaSei,
        String motivo,
        String chaveIdempotencia
) {}

