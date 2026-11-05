package com.example.siggtrat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StatusEmpregadoDTO(
        Long id,
        @NotBlank(message = "A descrição do status é obrigatória.") @Size(max = 30) String descricao,
        long version
) {}
