package com.example.siggtrat.dto;

import java.util.List;

/** Conhecimento e visibilidade independentes; o valor permanece no escalar da resposta. */
public record ProvaCampoHistoricoDTO(Estado estado, Visibilidade visibilidade, List<OrigemProvaCampoDTO> origens) {
    public enum Estado { VALOR_COMPROVADO, AUSENCIA_COMPROVADA, NAO_COMPROVADO }
    public enum Visibilidade { DISPONIVEL, RESTRITA }

    public ProvaCampoHistoricoDTO {
        if (visibilidade == Visibilidade.RESTRITA) {
            estado = null;
            origens = List.of();
        } else {
            origens = origens == null ? List.of() : List.copyOf(origens);
        }
    }
}
