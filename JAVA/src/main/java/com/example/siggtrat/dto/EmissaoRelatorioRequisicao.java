package com.example.siggtrat.dto;

import com.example.siggtrat.dominio.relatorio.FormatoEmissaoRelatorio;
import com.example.siggtrat.dominio.relatorio.TipoRelatorioAdministrativo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record EmissaoRelatorioRequisicao(
        @NotNull TipoRelatorioAdministrativo tipo,
        @NotNull FormatoEmissaoRelatorio formato,
        @NotNull LocalDate inicio,
        @NotNull LocalDate fim,
        List<Long> contratoIds,
        Long lotacaoId,
        Long jornadaId,
        @NotBlank @Size(max = 120) String chaveIdempotencia
) { }
