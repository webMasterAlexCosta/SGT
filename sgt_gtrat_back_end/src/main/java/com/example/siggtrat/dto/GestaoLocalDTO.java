package com.example.siggtrat.dto;

public record GestaoLocalDTO(
        Long id,
        String nome,
        String codigo,
        Boolean ativo,
        Long superintendenciaId,
        String superintendenciaNome,
        String uf
) {
}
