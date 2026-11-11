package com.example.siggtrat.dto;

import com.example.siggtrat.dominio.acesso.TipoPermissaoExcepcional;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record ConcessaoPermissaoExcepcionalRequisicao(
        @NotNull TipoPermissaoExcepcional tipo,
        @NotNull Long usuarioBeneficiarioId,
        Long contratoId,
        Long gestaoLocalId,
        @NotBlank String finalidade,
        @NotBlank String justificativa,
        @NotBlank String autorizacaoAdministrativa,
        @NotNull @Future Instant validoAte,
        Long ocorrenciaId
) {}
