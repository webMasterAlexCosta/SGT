package com.example.siggtrat.dto;

import com.example.siggtrat.dominio.vinculo.*;
import java.time.*;

public record PosicaoVinculoTemporalDTO(
        Long vinculoId,
        Long pessoaId,
        Long contratoId,
        LocalDate dataReferencia,
        Instant conhecidoAte,
        LocalDate dataInicio,
        LocalDate dataFim,
        String matricula,
        EstadoNegocialVinculo estadoNegocial,
        SituacaoTemporalVinculo situacaoTemporal,
        Long eventoConhecidoMaisRecenteId
) {}
