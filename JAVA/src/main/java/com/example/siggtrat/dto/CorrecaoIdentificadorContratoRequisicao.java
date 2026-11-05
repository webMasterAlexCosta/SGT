package com.example.siggtrat.dto;

import com.example.siggtrat.entidade.TipoRelacaoPrecedenciaNegocial;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record CorrecaoIdentificadorContratoRequisicao(
        @NotBlank @Size(max = 50) String novoNumero,
        @NotNull @Min(1900) @Max(9999) Integer novoAno,
        @NotNull LocalDate dataEfetiva,
        @NotNull LocalDate dataAto,
        @NotBlank @Size(max = 1000) String motivo,
        @NotBlank @Size(max = 120) String referenciaSei,
        @NotBlank @Size(max = 180) String autorizacaoAdministrativa,
        @NotBlank @Size(max = 80) String chaveIdempotencia,
        Long eventoPrecedenteId,
        TipoRelacaoPrecedenciaNegocial relacaoPrecedencia,
        Integer ordemDocumental
) {}
