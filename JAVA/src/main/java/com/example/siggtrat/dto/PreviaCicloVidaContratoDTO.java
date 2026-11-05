package com.example.siggtrat.dto;

import java.time.LocalDate;
import java.util.List;

/** Impactos concretos exibidos antes da confirmação de uma ação contratual. */
public record PreviaCicloVidaContratoDTO(
        Long contratoId, String operacao, LocalDate dataEfetiva, LocalDate dataAto, String motivo,
        Long usuarioResponsavelId, String usuarioResponsavelUsername, String autorizacaoNecessaria,
        int quantidadeVinculosAfetados, List<VinculoAfetado> vinculosAfetados,
        List<ProgramacaoAfetada> programacoesFuturasAfetadas,
        long quantidadeFrequenciasAfetadas, List<LocalDate> datasComFrequenciasAfetadas,
        List<String> impactosConhecidos, boolean exigeConfirmacao, String tokenPrevia,
        int limiteDetalhes, boolean vinculosTruncados,
        long quantidadeProgramacoesFuturasAfetadas, boolean programacoesTruncadas,
        long quantidadeDatasComFrequenciasAfetadas, boolean datasComFrequenciasTruncadas
) {
    public record VinculoAfetado(Long vinculoId, Long pessoaId, String pessoaNome, String matricula,
                                LocalDate dataInicio, LocalDate dataFim, boolean programado) {}
    public record ProgramacaoAfetada(String tipo, Long id, Long vinculoId, LocalDate dataEfetiva, String descricao) {}
}
