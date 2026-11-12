package com.example.siggtrat.dto;

import java.util.List;

/** Recorte único para Resumo, Contratos e Operação. */
public record PainelVisoesDTO(PainelAnaliticoDTO analitico, PainelGestaoDTO gestao,
                             List<PainelRankingDTO> ranking, PainelGestaoDTO.ResumoEfetivo efetivoAnterior) {}
