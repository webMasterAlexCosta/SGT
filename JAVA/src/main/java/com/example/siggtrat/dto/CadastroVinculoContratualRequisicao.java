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
        String autorizacaoExcedente,
        Long lotacaoId,
        Long jornadaId,
        Long cargoId
) {
    /** Mantém o contrato anterior de seleção por composição-base. */
    public CadastroVinculoContratualRequisicao(Long pessoaId, CadastroPessoaRequisicao pessoa,
                                                Long contratoId, LocalDate dataInicio, LocalDate dataFim,
                                                String matricula, Long composicaoId,
                                                Boolean confirmarInclusaoExcedente, String justificativaExcedente,
                                                String evidenciaExcedente, String autorizacaoExcedente) {
        this(pessoaId, pessoa, contratoId, dataInicio, dataFim, matricula, composicaoId,
                confirmarInclusaoExcedente, justificativaExcedente, evidenciaExcedente,
                autorizacaoExcedente, null, null, null);
    }

    public CadastroVinculoContratualRequisicao(Long pessoaId, CadastroPessoaRequisicao pessoa,
                                                Long contratoId, LocalDate dataInicio, LocalDate dataFim,
                                                String matricula, Long composicaoId) {
        this(pessoaId, pessoa, contratoId, dataInicio, dataFim, matricula, composicaoId,
                false, null, null, null);
    }
}
