package com.example.siggtrat.dto;

import com.example.siggtrat.entidade.TipoRelacaoPrecedenciaNegocial;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Corrige um erro material; a troca real de empresa continua sendo aditamento. */
public record CorrecaoCnpjContratoRequisicao(
        @NotBlank @Size(max = 18) String cnpjCorrigido,
        @NotNull LocalDate dataEfetiva,
        @NotNull LocalDate dataAto,
        @NotBlank @Size(max = 1000) String motivo,
        @NotBlank @Size(max = 120) String referenciaSei,
        @NotBlank @Size(max = 80) String chaveIdempotencia,
        Long eventoPrecedenteId,
        TipoRelacaoPrecedenciaNegocial relacaoPrecedencia,
        Integer ordemDocumental
) {}
