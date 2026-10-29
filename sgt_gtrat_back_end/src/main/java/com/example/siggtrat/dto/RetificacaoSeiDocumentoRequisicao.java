package com.example.siggtrat.dto;

public record RetificacaoSeiDocumentoRequisicao(
        String novaReferenciaSei,
        String motivo,
        String chaveIdempotencia
) {}

