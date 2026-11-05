package com.example.siggtrat.dto;

import java.time.LocalDate;

public record DesligamentoVinculoRequisicao(
        LocalDate dataFim,
        Long motivoDesligamentoId,
        String justificativa,
        String evidencia,
        String autorizacao,
        Boolean confirmarImpacto,
        String tokenPrevia,
        Boolean confirmarPrecedenciaUltimoDia,
        Boolean confirmarInclusaoExcedente
) {
    public DesligamentoVinculoRequisicao(LocalDate dataFim, Long motivoDesligamentoId,
                                         String justificativa, String evidencia, String autorizacao,
                                         Boolean confirmarImpacto, String tokenPrevia,
                                         Boolean confirmarPrecedenciaUltimoDia) {
        this(dataFim, motivoDesligamentoId, justificativa, evidencia, autorizacao,
                confirmarImpacto, tokenPrevia, confirmarPrecedenciaUltimoDia, false);
    }

    /** Mantém os chamadores anteriores; a confirmação só é exigida se houver colisão no último dia. */
    public DesligamentoVinculoRequisicao(LocalDate dataFim, Long motivoDesligamentoId,
                                         String justificativa, String evidencia, String autorizacao,
                                         Boolean confirmarImpacto, String tokenPrevia) {
        this(dataFim, motivoDesligamentoId, justificativa, evidencia, autorizacao,
                confirmarImpacto, tokenPrevia, false, false);
    }
}
