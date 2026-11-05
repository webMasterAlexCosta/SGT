package com.example.siggtrat.dto;

import java.util.List;

/** Página enxuta do relatório de absenteísmo para bases de grande escala. */
public record RelatorioAbsenteismoPaginaDTO(
        List<RelatorioAbsenteismoDTO> registros,
        long totalColaboradores,
        int pagina,
        int tamanho,
        int totalPaginas,
        boolean primeira,
        boolean ultima,
        AvisoRevisaoRelatorioDTO revisao
) {
    public RelatorioAbsenteismoPaginaDTO(List<RelatorioAbsenteismoDTO> registros,
                                         long totalColaboradores, int pagina, int tamanho,
                                         int totalPaginas, boolean primeira, boolean ultima) {
        this(registros, totalColaboradores, pagina, tamanho, totalPaginas, primeira, ultima,
                AvisoRevisaoRelatorioDTO.definitivo());
    }
}
