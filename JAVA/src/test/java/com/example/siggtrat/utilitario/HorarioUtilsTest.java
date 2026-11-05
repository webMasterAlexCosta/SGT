package com.example.siggtrat.utilitario;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HorarioUtilsTest {

    @Test
    void calculaTurnoNoturnoValido() {
        assertEquals(480L, UtilitarioHorario.calcularIntervaloMinutos(
                "22:00", "06:00", "entrada", "saída"));
    }

    @Test
    void rejeitaHorarioForaDoFormato() {
        assertThrows(IllegalArgumentException.class,
                () -> UtilitarioHorario.normalizarHorario("25:00", "entrada"));
        assertThrows(IllegalArgumentException.class,
                () -> UtilitarioHorario.normalizarHorario("9:00", "entrada"));
    }

    @Test
    void rejeitaParDeBatidasIncompleto() {
        assertThrows(IllegalArgumentException.class,
                () -> UtilitarioHorario.validarPar("08:00", null, "entrada", "saída"));
    }
}
