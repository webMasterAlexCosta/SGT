package com.example.siggtrat.dto;

import java.time.LocalDate;

/** Revogação documentada de uma transferência ainda futura, sem apagar seu mapa. */
public record CancelamentoTransferenciaGestaoRequisicao(
        LocalDate dataEfetiva,
        LocalDate dataAto,
        String motivo,
        String referenciaSei,
        String autorizacaoAdministrativa,
        Boolean conferenciaDocumentalConfirmada,
        String versaoConferenciaDocumental
) {}
