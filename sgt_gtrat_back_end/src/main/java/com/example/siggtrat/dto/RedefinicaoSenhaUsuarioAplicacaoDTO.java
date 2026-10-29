package com.example.siggtrat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


public record RedefinicaoSenhaUsuarioAplicacaoDTO(
        @NotBlank(message = "Informe a nova senha temporária.")
        @Size(max = 72, message = "A nova senha deve ter no máximo 72 caracteres.") String newPassword
) {}
