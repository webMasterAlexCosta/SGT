package com.example.siggtrat.dto;

import java.util.List;

public record JornadaDTO(
        Long id,
        String descricao,
        List<JornadaDiaSemanaDTO> dias,
        List<RegraAdicionalDTO> regrasAdicionais,
        Long gestaoLocalId,
        String gestaoLocalNome,
        Long superintendenciaId,
        String superintendenciaNome,
        String uf,
        Boolean ativo,
        Long version
) {}
