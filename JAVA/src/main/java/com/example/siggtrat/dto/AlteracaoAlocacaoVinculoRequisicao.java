package com.example.siggtrat.dto;

import java.time.LocalDate;

/**
 * Seleciona uma combinação válida da composição temporal e indica o primeiro
 * dia da nova faixa. Os identificadores do recorte são aceitos para combinações
 * originadas em aditamento que não possuem composição-base persistida.
 */
public record AlteracaoAlocacaoVinculoRequisicao(
        LocalDate dataEfetiva,
        Long composicaoId,
        Long lotacaoId,
        Long jornadaId,
        Long cargoId,
        String justificativa,
        String evidencia,
        String autorizacao,
        Boolean confirmarImpacto,
        String tokenPrevia,
        Boolean confirmarPrecedenciaUltimoDia,
        Boolean confirmarInclusaoExcedente
) {
    public AlteracaoAlocacaoVinculoRequisicao(LocalDate dataEfetiva, Long composicaoId,
                                               Long lotacaoId, Long jornadaId, Long cargoId,
                                               String justificativa, String evidencia, String autorizacao,
                                               Boolean confirmarImpacto, String tokenPrevia,
                                               Boolean confirmarPrecedenciaUltimoDia) {
        this(dataEfetiva, composicaoId, lotacaoId, jornadaId, cargoId, justificativa, evidencia,
                autorizacao, confirmarImpacto, tokenPrevia, confirmarPrecedenciaUltimoDia, false);
    }

    public AlteracaoAlocacaoVinculoRequisicao(LocalDate dataEfetiva, Long composicaoId,
                                               Long lotacaoId, Long jornadaId, Long cargoId,
                                               String justificativa, String evidencia, String autorizacao,
                                               Boolean confirmarImpacto, String tokenPrevia) {
        this(dataEfetiva, composicaoId, lotacaoId, jornadaId, cargoId, justificativa, evidencia,
                autorizacao, confirmarImpacto, tokenPrevia, false);
    }
}
