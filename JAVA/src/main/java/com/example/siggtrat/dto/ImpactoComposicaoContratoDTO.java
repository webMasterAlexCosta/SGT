package com.example.siggtrat.dto;

public record ImpactoComposicaoContratoDTO(
        Long composicaoId,
        Long lotacaoId,
        String lotacaoNome,
        Long jornadaId,
        String jornadaDescricao,
        Long cargoId,
        String cargoNome,
        Integer quantidadeAnterior,
        Integer variacao,
        Integer quantidadeResultante,
        Long colaboradoresAtuais,
        Long excedenteResultante,
        boolean exigeTratamento
) {}
