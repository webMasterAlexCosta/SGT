package com.example.siggtrat.dto;

public record AcaoVinculoContratualRequisicao(
        String justificativa,
        String evidencia,
        String autorizacao,
        Boolean confirmarImpacto,
        String tokenPrevia,
        Boolean confirmarInclusaoExcedente
) {
    public AcaoVinculoContratualRequisicao(String justificativa, String evidencia, String autorizacao,
                                          Boolean confirmarImpacto, String tokenPrevia) {
        this(justificativa, evidencia, autorizacao, confirmarImpacto, tokenPrevia, false);
    }
}
