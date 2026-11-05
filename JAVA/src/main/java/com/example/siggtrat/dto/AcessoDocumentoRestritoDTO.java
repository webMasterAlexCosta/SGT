package com.example.siggtrat.dto;

import com.example.siggtrat.dominio.documento.TipoAcessoDocumento;

import java.time.Instant;

public record AcessoDocumentoRestritoDTO(
        Long id,
        Long documentoId,
        TipoAcessoDocumento tipoAcesso,
        String finalidade,
        Long usuarioId,
        String usuarioUsername,
        String perfisFotografia,
        Long permissaoId,
        Instant acessadoEm
) {}

