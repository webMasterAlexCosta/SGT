package com.example.siggtrat.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Visão de capacidade da composição. O campo desdeQuandoExcedente será
 * preenchido historicamente após a criação do Vínculo/Alocação temporal.
 */
public record ExcedenteComposicaoDTO(
        Long composicaoId,
        Long contratoId,
        Long lotacaoId,
        String lotacaoNome,
        Long jornadaId,
        String jornadaDescricao,
        Long cargoId,
        String cargoNome,
        Integer efetivoPrevisto,
        long quantidadeVinculadaAtual,
        long quantidadeExcedente,
        LocalDate desdeQuandoExcedente,
        boolean excedente,
        List<JustificativaExcedenteDTO> justificativas
) {}
