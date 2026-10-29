package com.example.siggtrat.dto;

import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.dominio.acesso.TipoPermissaoExcepcional;

import java.time.Instant;

public record PermissaoExcepcionalDTO(
        Long id,
        TipoPermissaoExcepcional tipo,
        Long usuarioBeneficiarioId,
        String usuarioBeneficiario,
        PerfilAcesso perfilBeneficiario,
        Long contratoId,
        String contrato,
        Long gestaoLocalId,
        String gestaoLocal,
        String finalidade,
        String justificativa,
        String autorizacaoAdministrativa,
        Instant concedidoEm,
        Long concedidoPorId,
        String concedidoPor,
        Instant validoAte,
        boolean ativa,
        Instant revogadoEm,
        Long revogadoPorId,
        String revogadoPor,
        String motivoRevogacao,
        long version
) {}
