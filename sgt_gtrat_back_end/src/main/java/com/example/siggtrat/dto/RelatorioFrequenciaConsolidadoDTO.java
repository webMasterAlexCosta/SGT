package com.example.siggtrat.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record RelatorioFrequenciaConsolidadoDTO(
        boolean completo,
        String calculoVersao,
        LocalDate inicio,
        LocalDate fim,
        List<Long> contratos,
        List<Linha> registros,
        Totais totais,
        AvisoRevisaoRelatorioDTO revisao
) {
    public RelatorioFrequenciaConsolidadoDTO(boolean completo, String calculoVersao,
                                             LocalDate inicio, LocalDate fim, List<Long> contratos,
                                             List<Linha> registros, Totais totais) {
        this(completo, calculoVersao, inicio, fim, contratos, registros, totais,
                AvisoRevisaoRelatorioDTO.definitivo());
    }
    public record Linha(
            String chaveAlocacao,
            Long empregadoId,
            String nome,
            Long contratoId,
            String contrato,
            Long lotacaoId,
            String lotacao,
            Long jornadaId,
            String turno,
            Long cargoId,
            String cargo,
            Map<String, Dia> diario,
            Totais totais
    ) {}

    public record Dia(
            LocalDate data,
            boolean diaTrabalho,
            int minutosPrevistos,
            boolean lancado,
            String status,
            String entrada1,
            String saida1,
            String entrada2,
            String saida2,
            int minutosFisicos,
            int minutosNoturnosReais,
            int minutosNoturnosReduzidos,
            int minutosAdicionais,
            int minutosPagaveis,
            String calculoVersao,
            boolean ocorrenciaSobreposta
    ) {}

    public record Totais(
            int diasTrabalhaveis,
            int diasLancados,
            int presencas,
            int presencasParciais,
            int faltas,
            int atestados,
            int descansos,
            int semLancamento,
            int minutosPrevistos,
            int minutosFisicos,
            int minutosNoturnosReais,
            int minutosNoturnosReduzidos,
            int minutosAdicionais,
            int minutosPagaveis
    ) {}
}
