package com.example.siggtrat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EnderecoDTO(
        Long id,
        @NotBlank(message = "O logradouro é obrigatório.") @Size(max = 150) String logradouro,
        @Size(max = 10) String numero,
        @Size(max = 100) String bairro,
        @Size(max = 100) String cidade,
        @Size(max = 2) String uf,
        @Size(max = 10) String cep,
        long version
) {}
