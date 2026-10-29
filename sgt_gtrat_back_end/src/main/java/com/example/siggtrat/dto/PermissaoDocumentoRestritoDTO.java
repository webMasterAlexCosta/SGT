package com.example.siggtrat.dto;

import java.time.Instant;

public record PermissaoDocumentoRestritoDTO(
        Long id,
        Long documentoReferenciaId,
        String grupoLogicoId,
        Long usuarioBeneficiarioId,
        String usuarioBeneficiarioUsername,
        String finalidade,
        String justificativa,
        String autorizacaoAdministrativa,
        Instant concedidoEm,
        Long concedidoPorId,
        String concedidoPorUsername,
        Instant validoAte,
        Boolean ativo,
        Instant revogadoEm,
        Long revogadoPorId,
        String revogadoPorUsername,
        String motivoRevogacao,
        long version
) {}

