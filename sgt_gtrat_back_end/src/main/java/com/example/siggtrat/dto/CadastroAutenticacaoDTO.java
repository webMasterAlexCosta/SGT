package com.example.siggtrat.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CadastroAutenticacaoDTO(
        @NotBlank(message = "Informe o usuário.") String username,
        @NotBlank(message = "Informe o e-mail institucional.")
        @Email(message = "Informe um e-mail válido.") String email,
        @NotBlank(message = "Informe a matrícula.")
        @Pattern(regexp = "(?:\\D*\\d){8}\\D*", message = "A matrícula deve possuir 8 dígitos.")
        String matricula,
        @NotNull(message = "Selecione a Gestão Estadual/UF.") Long superintendenciaId,
        @NotNull(message = "Selecione a Gestão Local.") Long gestaoLocalId
) {}
