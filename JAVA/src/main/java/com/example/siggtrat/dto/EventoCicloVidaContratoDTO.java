package com.example.siggtrat.dto;

import com.example.siggtrat.entidade.SituacaoFatoNegocial;
import com.example.siggtrat.entidade.TipoEventoCicloVidaContrato;

import java.time.Instant;
import java.time.LocalDate;

public record EventoCicloVidaContratoDTO(
        Long id,
        TipoEventoCicloVidaContrato tipoEvento,
        String fatoLogicoId,
        String chaveNegocial,
        Integer versaoNegocial,
        Integer ordemDocumental,
        SituacaoFatoNegocial situacaoFato,
        LocalDate dataEfetiva,
        LocalDate dataAto,
        Instant registradoEm,
        Long usuarioResponsavelId,
        String usuarioResponsavelUsername,
        String motivo,
        String referenciaSei,
        String valorAnterior,
        String valorNovo
) {
}
