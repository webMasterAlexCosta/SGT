package com.example.siggtrat.dto;

import java.util.List;

public record LotacaoDTO(
        Long id,
        String nome,
        List<JornadaResumoDTO> turnos,
        Long gestaoLocalId,
        String gestaoLocalNome,
        Long superintendenciaId,
        String superintendenciaNome,
        String uf,
        String municipio,
        Boolean ativo
) {}
