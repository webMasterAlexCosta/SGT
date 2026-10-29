package com.example.siggtrat.dto;

public record AvisoRevisaoRelatorioDTO(
        boolean sujeitoARevisao,
        long quantidadePendencias,
        String mensagem
) {
    public static AvisoRevisaoRelatorioDTO definitivo() {
        return new AvisoRevisaoRelatorioDTO(false, 0L, "Relatório sem pendências de retificação no recorte consultado.");
    }

    public static AvisoRevisaoRelatorioDTO de(long quantidade) {
        long total = Math.max(0L, quantidade);
        return total == 0L ? definitivo() : new AvisoRevisaoRelatorioDTO(
                true,
                total,
                "SUJEITO A REVISÃO — " + total + (total == 1L ? " pendência" : " pendências") + " de retificação."
        );
    }
}
