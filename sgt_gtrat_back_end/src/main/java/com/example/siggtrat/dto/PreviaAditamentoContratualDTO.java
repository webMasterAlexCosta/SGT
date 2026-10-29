package com.example.siggtrat.dto;

import java.time.LocalDate;
import java.util.List;

public record PreviaAditamentoContratualDTO(
        Long contratoId,
        LocalDate dataEfetiva,
        Integer efetivoAnterior,
        Integer efetivoResultante,
        Integer efetivoOriginalPublicado,
        Integer pisoNormalReducao,
        boolean reducao,
        boolean excedeLimiteNormal25,
        boolean exigeAutorizacaoExcepcional,
        boolean possuiImpactoOcupacao,
        List<ImpactoComposicaoContratoDTO> impactosComposicao,
        List<String> alertas,
        String tokenPrevia
) {}
