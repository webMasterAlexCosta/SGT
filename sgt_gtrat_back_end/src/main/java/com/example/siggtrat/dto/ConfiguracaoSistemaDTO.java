package com.example.siggtrat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConfiguracaoSistemaDTO(
        @NotBlank @Size(max = 100) String chave,
        @NotBlank @Size(max = 255) String valor,
        @Size(max = 255) String descricao
) {}
