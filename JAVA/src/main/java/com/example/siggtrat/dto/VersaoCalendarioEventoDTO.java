package com.example.siggtrat.dto;

import java.time.Instant;
import java.time.LocalDate;

public record VersaoCalendarioEventoDTO(
        Long id, Long calendarioEventoId, int numeroVersao, String tipoOperacao,
        LocalDate dataEfetiva, Instant registradoEm, Long usuarioId, String usuarioUsername,
        String justificativa, String motivoTecnico, Long versaoAnteriorId,
        CalendarioEventoDTO fotografia, String hashAnterior, String hashVersao
) { }
