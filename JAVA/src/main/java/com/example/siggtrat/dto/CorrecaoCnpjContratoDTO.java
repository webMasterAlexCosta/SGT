package com.example.siggtrat.dto;

import java.time.Instant;
import java.time.LocalDate;

public record CorrecaoCnpjContratoDTO(Long id, Long contratoId, Long eventoId, String cnpjAnterior,
        String cnpjCorrigido, LocalDate dataEfetiva, LocalDate dataAto, String motivo, String referenciaSei,
        String chaveIdempotencia, Long responsavelId, String responsavelUsername, Instant registradoEm) {}
