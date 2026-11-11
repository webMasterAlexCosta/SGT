package com.example.siggtrat.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record FechamentoFrequenciaDTO(
        Long id,
        String status,
        LocalDate dataReferencia,
        Long lotacaoId,
        String lotacaoNome,
        Long jornadaId,
        String jornadaDescricao,
        List<Long> contratoIds,
        long totalPrevisto,
        long totalProcessado,
        long totalInserido,
        long totalAtualizado,
        long totalPreservado,
        long totalAlteracoes,
        boolean somenteAlteracoes,
        int percentual,
        int tentativa,
        boolean cancelamentoSolicitado,
        String mensagem,
        @com.fasterxml.jackson.annotation.JsonIgnore String detalheErro,
        LocalDateTime criadoEm,
        LocalDateTime iniciadoEm,
        LocalDateTime atualizadoEm,
        LocalDateTime concluidoEm
) {
}
