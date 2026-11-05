package com.example.siggtrat.regra;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConclusaoEfetivoContratualTest {
    @Test
    void pisoGlobalMantemArredondamentoNoMaiorEfetivoRepresentavel() {
        assertEquals(1_610_612_736, RegraEfetivoContratual.pisoNormal(Integer.MAX_VALUE));
        assertTrue(RegraEfetivoContratual.reducaoExcedeLimiteNormal(Integer.MAX_VALUE, 1_610_612_735));
        assertFalse(RegraEfetivoContratual.reducaoExcedeLimiteNormal(Integer.MAX_VALUE, 1_610_612_736));
    }

    @Test
    void variacoesQueSomamDoisElevadoA32NaoSaoTratadasComoRedistribuicaoNeutra() {
        assertFalse(RegraEfetivoContratual.remanejamentoPreservaTotal(List.of(Integer.MAX_VALUE, Integer.MAX_VALUE, 2)));
    }

    @Test
    void variacoesGrandesRealmenteCompensadasPreservamTotal() {
        assertTrue(RegraEfetivoContratual.remanejamentoPreservaTotal(
                List.of(Integer.MAX_VALUE, Integer.MAX_VALUE, -Integer.MAX_VALUE, -Integer.MAX_VALUE)));
    }
}
