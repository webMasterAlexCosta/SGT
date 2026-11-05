package com.example.siggtrat.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CalendarioEventoRequisicao(
        @NotNull(message = "A data do evento é obrigatória.")
        LocalDate data,

        @NotBlank(message = "O tipo do evento é obrigatório.")
        @Size(max = 40, message = "O tipo do evento deve possuir no máximo 40 caracteres.")
        String tipo,

        @Size(max = 120, message = "A descrição deve possuir no máximo 120 caracteres.")
        String descricao,

        @Size(max = 30, message = "A abrangência deve possuir no máximo 30 caracteres.")
        String abrangencia,

        @Size(max = 2, message = "A UF deve possuir no máximo 2 caracteres.")
        String uf,

        @Size(max = 80, message = "O município deve possuir no máximo 80 caracteres.")
        String municipio,

        Long lotacaoId,
        Long superintendenciaId,
        Long gestaoLocalId,
        Boolean ativo,
        @Size(max = 2000, message = "A justificativa deve possuir no máximo 2000 caracteres.")
        String justificativa
) {
    public CalendarioEventoRequisicao(LocalDate data, String tipo, String descricao, String abrangencia,
            String uf, String municipio, Long lotacaoId, Long superintendenciaId, Long gestaoLocalId, Boolean ativo) {
        this(data, tipo, descricao, abrangencia, uf, municipio, lotacaoId, superintendenciaId, gestaoLocalId, ativo, null);
    }
}
