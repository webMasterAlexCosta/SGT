package com.example.siggtrat.dto;

import java.time.Instant;

public record TipoContratacaoDTO(
        Long id,
        String descricao,
        Boolean ativo,
        Boolean matriculaVinculoObrigatoria,
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
) {
}
