package com.example.siggtrat.dto;

import java.time.Instant;
import java.time.LocalDate;

public record CorrecaoIdentificadorContratoDTO(
        Long id,
        Long contratoId,
        Long eventoId,
        Long permissaoId,
        String numeroAnterior,
        Integer anoAnterior,
        String numeroNovo,
        Integer anoNovo,
        LocalDate dataEfetiva,
        LocalDate dataAto,
        String motivo,
        String referenciaSei,
        String autorizacaoAdministrativa,
        String chaveIdempotencia,
        Instant registradoEm,
        Long responsavelId,
        String responsavel
) {}
