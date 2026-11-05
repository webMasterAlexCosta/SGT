package com.example.siggtrat.dto;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZonedDateTime;

public record HistoricoVinculoContratoDTO(
        Long id,
        Long empregadoId,
        String empregadoNome,
        Long contratoId,
        String numeroContrato,
        String nomeContrato,
        String descricaoContrato,
        String tipoContrato,
        LocalDate contratoDataInicio,
        LocalDate contratoDataFim,
        Boolean contratoAtivo,
        Integer efetivoPrevisto,
        Long efetivoAtual,
        Long saldoEfetivo,
        Double percentualOcupacao,
        DayOfWeek diaSemanaReduzido,
        Integer horasDiaReduzido,
        Long gestaoLocalId,
        String gestaoLocalNome,
        Long superintendenciaId,
        String superintendenciaNome,
        String uf,
        ZonedDateTime dataInicio,
        ZonedDateTime dataFim,
        String observacao,
        boolean ativo,
        IdentidadeHistoricaResolvidaDTO identidadeHistorica,
        Long vinculoCanonicoId,
        LocalDate dataReferenciaIdentidade,
        java.time.Instant conhecidoAteIdentidade
) {}
