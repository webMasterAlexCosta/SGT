package com.example.siggtrat.dto;

import com.example.siggtrat.entidade.TipoRelacaoPrecedenciaNegocial;

import java.time.Instant;

public record RelacaoPrecedenciaEventoContratoDTO(
        Long id,
        Long eventoFonteId,
        Long eventoAlvoId,
        TipoRelacaoPrecedenciaNegocial tipoRelacao,
        Instant registradoEm,
        Long usuarioResponsavelId,
        String usuarioResponsavelUsername,
        String motivo
) {}
