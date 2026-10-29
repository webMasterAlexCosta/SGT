package com.example.siggtrat.dto;

import com.example.siggtrat.dominio.documento.*;

import java.time.Instant;
import java.time.LocalDate;

public record EventoDocumentoComprobatorioDTO(
        Long id,
        Long documentoId,
        String grupoLogicoId,
        TipoEventoDocumento tipoEvento,
        LocalDate dataEfetiva,
        Instant registradoEm,
        Long responsavelId,
        String responsavelUsername,
        String motivo,
        EstadoDocumentoComprobatorio estadoAnterior,
        EstadoDocumentoComprobatorio estadoNovo,
        String referenciaSeiAnterior,
        String referenciaSeiNova,
        String sha256Anterior,
        String sha256Novo,
        String hashAnterior,
        String hashEvento
) {}

