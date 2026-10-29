package com.example.siggtrat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TelefoneDTO(
        Long id,
        @NotBlank(message = "O número do telefone é obrigatório.") @Size(max = 20) String numero,
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) EmpregadoReferenciaDTO empregado,
        long version
) {}
