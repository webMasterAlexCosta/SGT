package com.example.siggtrat.regra;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RegraEfetivoContratualTest {

    @Test
    void contrato100TemPiso75MesmoDepoisDeAumento() {
        assertEquals(75, RegraEfetivoContratual.pisoNormal(100));
        assertFalse(RegraEfetivoContratual.reducaoExcedeLimiteNormal(100, 75));
        assertTrue(RegraEfetivoContratual.reducaoExcedeLimiteNormal(100, 74));
    }

    @Test
    void contrato101ArredondaPisoPara76() {
        assertEquals(76, RegraEfetivoContratual.pisoNormal(101));
    }

    @Test
    void remanejamentoExigeSomaZero() {
        assertTrue(RegraEfetivoContratual.remanejamentoPreservaTotal(List.of(-10, 4, 6)));
        assertFalse(RegraEfetivoContratual.remanejamentoPreservaTotal(List.of(-10, 4, 5)));
    }
}
