package com.example.siggtrat.dto;

import java.time.LocalDate;

public record PublicacaoContratoDTO(
        LocalDate dataPublicacao,
        String referenciaSei,
        String justificativa
) {}
