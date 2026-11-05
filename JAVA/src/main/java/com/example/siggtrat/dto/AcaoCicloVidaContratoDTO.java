package com.example.siggtrat.dto;

import com.example.siggtrat.entidade.TipoRelacaoPrecedenciaNegocial;

import java.time.LocalDate;

/**
 * Corpo comum das operações explícitas do ciclo de vida contratual.
 *
 * Os campos de precedência são opcionais e só se tornam obrigatórios quando a
 * nova ocorrência conflita semanticamente com outro fato na mesma data.
 */
public record AcaoCicloVidaContratoDTO(
        LocalDate dataEfetiva,
        LocalDate dataAto,
        String motivo,
        String referenciaSei,
        Long eventoPrecedenteId,
        TipoRelacaoPrecedenciaNegocial relacaoPrecedencia,
        Integer ordemDocumental,
        String tokenPrevia,
        Boolean confirmarImpacto
) {
    public AcaoCicloVidaContratoDTO(LocalDate dataEfetiva, LocalDate dataAto, String motivo, String referenciaSei,
                                   Long eventoPrecedenteId, TipoRelacaoPrecedenciaNegocial relacaoPrecedencia,
                                   Integer ordemDocumental) {
        this(dataEfetiva, dataAto, motivo, referenciaSei, eventoPrecedenteId, relacaoPrecedencia, ordemDocumental, null, null);
    }
    /** Forma concisa para comandos que não precisam declarar precedência documental. */
    public AcaoCicloVidaContratoDTO(
            LocalDate dataEfetiva,
            LocalDate dataAto,
            String motivo,
            String referenciaSei
    ) {
        this(dataEfetiva, dataAto, motivo, referenciaSei, null, null, null);
    }
}
