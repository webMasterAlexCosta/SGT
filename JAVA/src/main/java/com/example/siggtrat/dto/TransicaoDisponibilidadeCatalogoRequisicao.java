package com.example.siggtrat.dto;

import java.time.LocalDate;

public record TransicaoDisponibilidadeCatalogoRequisicao(
        LocalDate dataEfetiva, Boolean ativo, String motivo, String evidencia, String autorizacao,
        Long versaoEsperada, String chaveIdempotencia, Boolean confirmarImpacto, String tokenPrevia) {}
