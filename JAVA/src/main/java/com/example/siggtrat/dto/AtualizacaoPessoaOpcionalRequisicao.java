package com.example.siggtrat.dto;

import java.time.LocalDate;

/** Null mantém o valor atual; limpeza exige o indicador explícito correspondente. */
public record AtualizacaoPessoaOpcionalRequisicao(
        String rg, Boolean limparRg,
        String ctpsSerie, Boolean limparCtpsSerie,
        String nitPisPasep, Boolean limparNitPisPasep,
        LocalDate dataNascimento, Boolean limparDataNascimento,
        String email, Boolean limparEmail,
        EnderecoDTO endereco, Boolean limparEndereco
) {}
