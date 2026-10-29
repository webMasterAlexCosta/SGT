package com.example.siggtrat.dto;

public record RelatorioAbsenteismoDTO(
        Long empregadoId,
        String nome,
        String cargo,
        String lotacao,
        String turno,
        int faltas,
        int atestados,
        int dsrPerdido,
        int spells,
        double taxaIndividual,
        double disrupcao,
        int diasTrabalhaveis,
        int minutosPrevistos
) {}
