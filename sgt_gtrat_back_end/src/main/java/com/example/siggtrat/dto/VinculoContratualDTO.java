package com.example.siggtrat.dto;

import com.example.siggtrat.dominio.vinculo.*;
import java.time.*;

public record VinculoContratualDTO(
        Long id,
        Long pessoaId,
        String pessoaNome,
        String pessoaCpf,
        Long contratoId,
        String contratoNumero,
        Integer contratoAno,
        Long gestaoLocalId,
        String gestaoLocalNome,
        LocalDate dataInicio,
        LocalDate dataFim,
        String matricula,
        EstadoNegocialVinculo estadoNegocial,
        SituacaoTemporalVinculo situacaoTemporal,
        Long motivoDesligamentoId,
        String motivoDesligamentoDescricao,
        Instant desligamentoRegistradoEm,
        Long composicaoInicialId,
        Long lotacaoInicialId,
        Long jornadaInicialId,
        Long cargoInicialId,
        long version
) {}
