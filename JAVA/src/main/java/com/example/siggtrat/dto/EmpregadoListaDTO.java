package com.example.siggtrat.dto;

import java.util.List;


public record EmpregadoListaDTO(
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
        String jornadaDescricao,
        List<EmpregadoContratoDTO> contratos
) {
}
