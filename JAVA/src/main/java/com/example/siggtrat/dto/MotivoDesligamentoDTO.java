package com.example.siggtrat.dto;

import com.example.siggtrat.dominio.vinculo.CategoriaTerminoVinculo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record MotivoDesligamentoDTO(
        Long id,
        @NotBlank @Size(max = 160) String descricao,
        @NotNull CategoriaTerminoVinculo categoria,
        Boolean ativo,
        Long gestaoLocalId,
        String gestaoLocalNome,
        Long superintendenciaId,
        String superintendenciaNome,
        String uf,
        Instant criadoEm,
        Long criadoPorId,
        String criadoPorUsername,
        Instant atualizadoEm,
        long version
) {}
