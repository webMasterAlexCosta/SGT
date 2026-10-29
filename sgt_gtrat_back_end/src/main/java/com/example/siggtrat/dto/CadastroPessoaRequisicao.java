package com.example.siggtrat.dto;

import java.time.LocalDate;

public record CadastroPessoaRequisicao(
        String nome,
        String cpf,
        String rg,
        String ctpsSerie,
        String nitPisPasep,
        LocalDate dataNascimento,
        String email,
        EnderecoDTO endereco
) {}
