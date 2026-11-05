package com.example.siggtrat.regra;

import java.time.LocalDate;

/** Regra única de disponibilidade temporal dos catálogos operacionais. */
public final class RegraDisponibilidadeCatalogo {
    private RegraDisponibilidadeCatalogo() {}

    public static boolean permiteUso(LocalDate dataEfetiva, Boolean... estadosAtivos) {
        if (dataEfetiva == null || estadosAtivos == null || estadosAtivos.length == 0) return false;
        boolean todosAtivos = true;
        for (Boolean estado : estadosAtivos) todosAtivos &= Boolean.TRUE.equals(estado);
        return todosAtivos;
    }
}
