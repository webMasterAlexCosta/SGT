package com.example.siggtrat.dto;

import java.time.LocalDate;

/** Pessoa nova ou pessoaId existente, mais vínculo e alocação inicial, em uma transação. */
public record CadastroVinculoContratualRequisicao(
        Long pessoaId,
        CadastroPessoaRequisicao pessoa,
        Long contratoId,
        LocalDate dataInicio,
        LocalDate dataFim,
        String matricula,
        Long composicaoId,
        Boolean confirmarInclusaoExcedente,
        String justificativaExcedente,
        String evidenciaExcedente,
        String autorizacaoExcedente
) {
    public CadastroVinculoContratualRequisicao(Long pessoaId, CadastroPessoaRequisicao pessoa,
                                                Long contratoId, LocalDate dataInicio, LocalDate dataFim,
                                                String matricula, Long composicaoId) {
        this(pessoaId, pessoa, contratoId, dataInicio, dataFim, matricula, composicaoId,
                false, null, null, null);
    }
}
