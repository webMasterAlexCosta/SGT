package com.example.siggtrat.dto;

/** Uma combinação válida para alimentar a seleção Contrato → Lotação → Turno → Cargo. */
public record OpcaoAlocacaoTemporalDTO(
        Long composicaoBaseId,
        Long lotacaoId,
        String lotacaoNome,
        Long jornadaId,
        String jornadaDescricao,
        Long cargoId,
        String cargoNome,
        int efetivoPrevisto,
        long ocupacaoAtual,
        long ocupacaoAposInclusao,
        long excedenteAposInclusao,
        boolean exigeJustificativaExcedente,
        boolean disponivelParaNovaAlocacao
) {}
