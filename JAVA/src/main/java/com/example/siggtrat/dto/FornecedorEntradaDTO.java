package com.example.siggtrat.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record FornecedorEntradaDTO(String razaoSocial, String cnpj,
        @Email(message = "Informe um e-mail válido.")
        @Size(max = 180, message = "O e-mail deve conter até 180 caracteres.") String email,
        Boolean ativo) {}
