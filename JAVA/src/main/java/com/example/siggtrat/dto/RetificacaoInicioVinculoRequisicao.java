package com.example.siggtrat.dto;

import java.time.LocalDate;

public record RetificacaoInicioVinculoRequisicao(
        LocalDate novoInicio,
        String justificativa,
        String evidencia,
        String autorizacao,
        Boolean confirmarImpacto,
        String tokenPrevia,
        Boolean confirmarInclusaoExcedente
) {
    public RetificacaoInicioVinculoRequisicao(LocalDate novoInicio, String justificativa, String evidencia,
                                              String autorizacao, Boolean confirmarImpacto, String tokenPrevia) {
        this(novoInicio, justificativa, evidencia, autorizacao, confirmarImpacto, tokenPrevia, false);
    }
}
