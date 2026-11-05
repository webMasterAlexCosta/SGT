package com.example.siggtrat.dto;

import com.example.siggtrat.entidade.TipoRelacaoPrecedenciaNegocial;

import java.time.LocalDate;
import java.util.List;

/** Corpo da operação excepcional de transferência de responsabilidade de Gestão Local. */
public record TransferenciaGestaoContratoRequisicao(
        LocalDate dataEfetiva,
        LocalDate dataAto,
        Long gestaoLocalDestinoId,
        String motivo,
        String referenciaSei,
        String autorizacaoAdministrativa,
        List<MapeamentoTransferenciaGestaoRequisicao> mapeamentos,
        Long eventoPrecedenteId,
        TipoRelacaoPrecedenciaNegocial relacaoPrecedencia,
        Integer ordemDocumental,
        Long tipoContratacaoDestinoId,
        String tokenPrevia,
        Boolean confirmarImpacto
) {}
