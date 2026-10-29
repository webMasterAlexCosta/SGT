package com.example.siggtrat.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Página do relatório mensal de frequência.
 *
 * A paginação é por colaborador, não por célula/lançamento. Um colaborador pode
 * produzir mais de uma linha quando mudou de contrato, lotação, turno ou cargo
 * dentro do período consultado.
 */
public record RelatorioFrequenciaPaginaDTO(
        boolean completo,
        String calculoVersao,
        LocalDate inicio,
        LocalDate fim,
        List<Long> contratos,
        List<RelatorioFrequenciaConsolidadoDTO.Linha> registros,
        long totalColaboradores,
        int pagina,
        int tamanho,
        int totalPaginas,
        boolean primeira,
        boolean ultima,
        AvisoRevisaoRelatorioDTO revisao
) {
    public RelatorioFrequenciaPaginaDTO(boolean completo, String calculoVersao,
                                        LocalDate inicio, LocalDate fim, List<Long> contratos,
                                        List<RelatorioFrequenciaConsolidadoDTO.Linha> registros,
                                        long totalColaboradores, int pagina, int tamanho,
                                        int totalPaginas, boolean primeira, boolean ultima) {
        this(completo, calculoVersao, inicio, fim, contratos, registros, totalColaboradores,
                pagina, tamanho, totalPaginas, primeira, ultima, AvisoRevisaoRelatorioDTO.definitivo());
    }
}
