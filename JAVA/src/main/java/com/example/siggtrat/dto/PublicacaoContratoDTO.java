package com.example.siggtrat.dto;

import java.time.LocalDate;

public record PublicacaoContratoDTO(
        LocalDate dataPublicacao,
        String referenciaSei,
        String justificativa,
        String tokenPrevia,
        Boolean confirmarImpacto
) {
    public PublicacaoContratoDTO(LocalDate dataPublicacao, String referenciaSei, String justificativa) {
        this(dataPublicacao, referenciaSei, justificativa, null, null);
    }
}
