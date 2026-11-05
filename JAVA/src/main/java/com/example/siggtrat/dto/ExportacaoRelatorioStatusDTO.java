package com.example.siggtrat.dto;

import java.time.Instant;
import java.util.UUID;

/** Estado observável de uma exportação longa de relatório. */
public record ExportacaoRelatorioStatusDTO(
        UUID id,
        String relatorio,
        String formato,
        String nomeArquivo,
        String status,
        String etapa,
        String mensagem,
        long processados,
        long totalEstimado,
        int percentual,
        long segundosDecorridos,
        Long segundosRestantesEstimados,
        long tamanhoBytes,
        boolean concluido,
        boolean falhou,
        boolean podeBaixar,
        Instant iniciadoEm,
        Instant atualizadoEm
) {
}
