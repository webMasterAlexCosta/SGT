package com.example.siggtrat.dto;

import java.time.Instant;

public record ConcessaoAcessoDocumentoRequisicao(
        Long usuarioId,
        String finalidade,
        String justificativa,
        String autorizacaoAdministrativa,
        Instant validoAte
) {}

