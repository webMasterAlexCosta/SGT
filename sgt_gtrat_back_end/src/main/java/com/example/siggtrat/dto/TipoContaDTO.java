package com.example.siggtrat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TipoContaDTO(
        Long id,
        @NotBlank(message = "A descrição do tipo de conta é obrigatória.") @Size(max = 30) String descricao,
        Long version
) {}
