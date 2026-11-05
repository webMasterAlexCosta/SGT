package com.example.siggtrat.dto;

import java.util.List;

/**
 * Indicadores nacionais/gerenciais calculados sobre toda a população filtrada,
 * sem manter todos os colaboradores simultaneamente em memória.
 */
public record RelatorioAbsenteismoResumoDTO(
        long faltas,
        long atestados,
        long dsrPerdido,
        long totalColaboradores,
        long diasTrabalhaveis,
        long alertasDisrupcao,
        double taxaGeral,
        List<RelatorioAbsenteismoDTO> maioresTaxas,
        AvisoRevisaoRelatorioDTO revisao
) {
    public RelatorioAbsenteismoResumoDTO(long faltas, long atestados, long dsrPerdido,
                                         long totalColaboradores, long diasTrabalhaveis,
                                         long alertasDisrupcao, double taxaGeral,
                                         List<RelatorioAbsenteismoDTO> maioresTaxas) {
        this(faltas, atestados, dsrPerdido, totalColaboradores, diasTrabalhaveis,
                alertasDisrupcao, taxaGeral, maioresTaxas, AvisoRevisaoRelatorioDTO.definitivo());
    }

    public RelatorioAbsenteismoResumoDTO comRevisao(AvisoRevisaoRelatorioDTO aviso) {
        return new RelatorioAbsenteismoResumoDTO(faltas, atestados, dsrPerdido, totalColaboradores,
                diasTrabalhaveis, alertasDisrupcao, taxaGeral, maioresTaxas, aviso);
    }
}
