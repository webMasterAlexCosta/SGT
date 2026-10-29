package com.example.siggtrat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cadastro de Cargo no escopo da Gestão Local.
 * O campo ativo controla disponibilidade operacional sem apagar o histórico.
 */
public record CargoDTO(
        Long id,
        @NotBlank(message = "O nome do cargo é obrigatório.") @Size(max = 100) String nome,
        @Size(max = 20) String cbo,
        @Size(max = 255) String descricao,
        Boolean ativo,
        Long gestaoLocalId,
        String gestaoLocalNome,
        Long superintendenciaId,
        String superintendenciaNome,
        String uf,
        Long version
) {}
