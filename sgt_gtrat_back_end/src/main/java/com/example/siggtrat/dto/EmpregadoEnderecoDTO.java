package com.example.siggtrat.dto;

public record EmpregadoEnderecoDTO(
        Long id,
        String logradouro,
        String numero,
        String bairro,
        String cidade,
        String uf,
        String cep
) {}