package com.example.siggtrat.dto;

import java.time.LocalDate;

public record DesligamentoVinculoRequisicao(
        LocalDate dataFim,
        Long motivoDesligamentoId,
        String justificativa,
        String evidencia,
        String autorizacao,
        Boolean confirmarImpacto,
        String tokenPrevia
) {}
