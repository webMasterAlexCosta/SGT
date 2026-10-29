package com.example.siggtrat.dto;

public record ComparacaoEmissaoRelatorioDTO(
        MetadadosEmissaoRelatorioDTO emissaoHistorica,
        AvisoRevisaoRelatorioDTO revisaoVigente,
        String hashHistorico,
        String hashVigente,
        boolean alterado,
        Object conteudoHistorico,
        Object conteudoVigente
) { }
