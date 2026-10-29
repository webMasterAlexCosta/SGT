package com.example.siggtrat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ContaDTO(
        Long id,
        @NotBlank(message = "O número da conta é obrigatório.") String numeroConta,
        @NotNull(message = "A agência é obrigatória.") AgenciaReferenciaDTO agencia,
        @NotNull(message = "O tipo de conta é obrigatório.") TipoContaDTO tipoConta,
        long version
) {}
