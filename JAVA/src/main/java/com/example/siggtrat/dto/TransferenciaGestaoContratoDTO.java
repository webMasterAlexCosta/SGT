package com.example.siggtrat.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Consulta auditável da transferência, inclusive o contexto mapeado. */
public record TransferenciaGestaoContratoDTO(
        Long id,
        Long contratoId,
        Long eventoContratoId,
        Long gestaoOrigemId,
        String gestaoOrigemNome,
        Long gestaoDestinoId,
        String gestaoDestinoNome,
        LocalDate dataEfetiva,
        String motivo,
        String referenciaSei,
        String autorizacaoAdministrativa,
        Instant registradoEm,
        Long responsavelId,
        String responsavelUsername,
        List<MapeamentoTransferenciaGestaoDTO> mapeamentos,
        Long tipoContratacaoDestinoId,
        String tipoContratacaoDestinoDescricao
) {}
