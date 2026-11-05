package com.example.siggtrat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AlteracaoSenhaPerfilDTO(
        @NotBlank(message = "Informe a senha atual.")
        @Size(max = 72, message = "A senha atual deve ter no máximo 72 caracteres.") String currentPassword,
        @NotBlank(message = "Informe a nova senha.")
        @Size(max = 72, message = "A nova senha deve ter no máximo 72 caracteres.") String newPassword,
        @Pattern(regexp = "(?:\\d{6})?", message = "O código de segurança deve conter 6 dígitos.")
        String token
) {}
