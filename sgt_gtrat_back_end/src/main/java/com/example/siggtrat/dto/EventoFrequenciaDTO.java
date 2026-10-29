package com.example.siggtrat.dto;

import com.example.siggtrat.dominio.frequencia.TipoEventoFrequencia;

import java.time.Instant;
import java.time.LocalDate;

public record EventoFrequenciaDTO(
        Long id,
        Long frequenciaId,
        Integer numeroVersao,
        TipoEventoFrequencia tipoEvento,
        LocalDate dataReferencia,
        String snapshotAnterior,
        String snapshotNovo,
        String motivo,
        Long usuarioId,
        String usuarioUsername,
        Instant registradoEm,
        String hashAnterior,
        String hashEvento
) {}
