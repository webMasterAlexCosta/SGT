package com.example.siggtrat.dto;

public record AcaoVinculoContratualRequisicao(
        String justificativa,
        String evidencia,
        String autorizacao,
        Boolean confirmarImpacto,
        String tokenPrevia
) {}
