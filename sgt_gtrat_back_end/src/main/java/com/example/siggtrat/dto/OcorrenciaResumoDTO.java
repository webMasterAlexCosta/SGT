package com.example.siggtrat.dto;

import java.time.LocalDate;
import java.util.List;

public record OcorrenciaResumoDTO(
        Long id,
        EmpregadoResumo empregado,
        LocalDate dataOcorrencia,
        String motivo,
        String tipoRo,
        String numeroSei,
        String observacao,
        Integer quantidadeDias,
        Integer diasSuspensao,
        Integer diasAfastamento,
        List<AnexoResumo> anexos
) {
    public record EmpregadoResumo(Long id, String nome) {}
    public record AnexoResumo(Long id, String nomeOriginal, String tipoArquivo) {}
}
