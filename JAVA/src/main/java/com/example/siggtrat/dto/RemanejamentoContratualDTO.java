package com.example.siggtrat.dto;

import com.example.siggtrat.entidade.SituacaoFatoNegocial;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record RemanejamentoContratualDTO(
        Long id,
        Long contratoId,
        Long eventoId,
        String identificacao,
        Integer versaoInstrumento,
        LocalDate dataEfetiva,
        String justificativa,
        String referenciaSei,
        Instant registradoEm,
        Long usuarioResponsavelId,
        String usuarioResponsavelUsername,
        SituacaoFatoNegocial situacao,
        Integer somaVariacoes,
        List<ImpactoComposicaoContratoDTO> itens
) {}
