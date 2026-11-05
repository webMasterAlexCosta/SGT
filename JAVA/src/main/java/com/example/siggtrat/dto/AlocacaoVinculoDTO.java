package com.example.siggtrat.dto;

import com.example.siggtrat.dominio.alocacao.EstadoNegocialAlocacao;

import java.time.Instant;
import java.time.LocalDate;

/** Faixa consultável de Alocação, inclusive quando já foi substituída/cancelada. */
public record AlocacaoVinculoDTO(
        Long id,
        Long vinculoId,
        Long composicaoBaseId,
        Long lotacaoId,
        String lotacaoNome,
        Long jornadaId,
        String jornadaDescricao,
        Long cargoId,
        String cargoNome,
        LocalDate dataInicio,
        LocalDate dataFim,
        EstadoNegocialAlocacao estadoNegocial,
        Instant criadoEm,
        Long criadoPorId,
        String criadoPorUsername,
        long version
) {}
