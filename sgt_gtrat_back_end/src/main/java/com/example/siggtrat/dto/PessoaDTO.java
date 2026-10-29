package com.example.siggtrat.dto;

import java.time.Instant;
import java.time.LocalDate;

public record PessoaDTO(
        Long id,
        String nome,
        String cpf,
        String rg,
        String ctpsSerie,
        String nitPisPasep,
        LocalDate dataNascimento,
        String email,
        EnderecoDTO endereco,
        Instant criadoEm,
        Instant atualizadoEm,
        long version
) {}
