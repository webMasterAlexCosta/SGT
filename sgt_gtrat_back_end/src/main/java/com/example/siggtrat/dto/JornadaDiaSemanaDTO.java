package com.example.siggtrat.dto;

import java.time.DayOfWeek;

public record JornadaDiaSemanaDTO(
        Long id,
        DayOfWeek diaSemana,
        Boolean diaTrabalho,
        String entrada1,
        String saida1,
        String entrada2,
        String saida2
) {}