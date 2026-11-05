package com.example.siggtrat.dto;

import java.time.LocalDate;
import java.util.List;

/** Prévia obrigatória para mudanças que possam repercutir em frequência e relatórios. */
public record PreviaImpactoAlocacaoDTO(
        String token,
        Long vinculoId,
        Long alocacaoAfetadaId,
        LocalDate dataEfetiva,
        LocalDate fimFaixaAtual,
        List<String> impactos,
        List<String> incompatibilidades
) {}
