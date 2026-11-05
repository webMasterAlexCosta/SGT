package com.example.siggtrat.dto;

import java.time.Instant;

/** Fato original e retirada operacional, sem alterar o contrato da listagem ativa. */
public record OcorrenciaHistoricoDTO(
        OcorrenciaResumoDTO ocorrencia,
        boolean cancelada,
        Instant canceladoEm,
        Long canceladoPorId,
        String canceladoPorUsername,
        String motivoCancelamento
) {}
