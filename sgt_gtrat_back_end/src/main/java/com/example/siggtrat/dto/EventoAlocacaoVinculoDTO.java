package com.example.siggtrat.dto;

import com.example.siggtrat.dominio.alocacao.*;

import java.time.Instant;
import java.time.LocalDate;

/** Auditoria imutável e encadeada de uma alteração de Alocação. */
public record EventoAlocacaoVinculoDTO(
        Long id,
        Long vinculoId,
        Long alocacaoNovaId,
        TipoEventoAlocacaoVinculo tipoEvento,
        TipoRelacaoEventoAlocacao relacaoPrecedencia,
        Long eventoPrecedenteId,
        LocalDate dataEfetiva,
        Instant registradoEm,
        Long responsavelId,
        String responsavelUsername,
        String justificativa,
        String evidencia,
        String autorizacao,
        Long alocacaoAnteriorId,
        LocalDate inicioAnterior,
        LocalDate fimAnterior,
        EstadoNegocialAlocacao estadoAnterior,
        Long composicaoAnteriorId,
        Long lotacaoAnteriorId,
        String lotacaoAnteriorNome,
        Long jornadaAnteriorId,
        String jornadaAnteriorDescricao,
        Long cargoAnteriorId,
        String cargoAnteriorNome,
        LocalDate inicioNovo,
        LocalDate fimNovo,
        EstadoNegocialAlocacao estadoNovo,
        Long composicaoNovaId,
        Long lotacaoNovaId,
        String lotacaoNovaNome,
        Long jornadaNovaId,
        String jornadaNovaDescricao,
        Long cargoNovoId,
        String cargoNovoNome,
        Long transferenciaGestaoId,
        Long gestaoOrigemIdFotografia,
        String gestaoOrigemNomeFotografia,
        Long gestaoDestinoIdFotografia,
        String gestaoDestinoNomeFotografia,
        String hashAnterior,
        String hashEvento
) {}
