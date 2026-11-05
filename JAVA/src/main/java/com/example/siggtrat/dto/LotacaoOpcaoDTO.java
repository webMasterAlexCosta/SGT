package com.example.siggtrat.dto;

import java.util.List;

public record LotacaoOpcaoDTO(
        Long id,
        String nome,
        List<JornadaResumoDTO> turnos
) {
}
