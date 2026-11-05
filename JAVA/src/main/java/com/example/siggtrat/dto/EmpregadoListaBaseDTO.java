package com.example.siggtrat.dto;


public record EmpregadoListaBaseDTO(
        Long id,
        String nome,
        String matricula,
        String cpf,
        Long statusId,
        String statusDescricao,
        Long lotacaoId,
        String lotacaoNome,
        Long cargoId,
        String cargoNome,
        Long jornadaId,
        String jornadaDescricao
) {
}
