package com.example.siggtrat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConfirmacaoRecuperacaoSenhaDTO(
        @NotBlank(message = "Token de recuperação obrigatório.")
        @Size(max = 256, message = "Token de recuperação inválido.")
        String token
) {
}
