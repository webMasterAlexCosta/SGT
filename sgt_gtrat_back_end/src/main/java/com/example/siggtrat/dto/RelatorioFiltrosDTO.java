package com.example.siggtrat.dto;

import java.util.List;

public record RelatorioFiltrosDTO(
        List<ContratoOpcaoDTO> contratos,
        List<LotacaoOpcaoDTO> lotacoes,
        List<JornadaResumoDTO> turnos
) {}
