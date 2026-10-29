package com.example.siggtrat.dto;

import java.time.LocalDate;

public record AlteracaoMatriculaVinculoRequisicao(
        LocalDate dataEfetiva,
        String novaMatricula,
        String justificativa,
        String evidencia,
        String autorizacao
) {}
