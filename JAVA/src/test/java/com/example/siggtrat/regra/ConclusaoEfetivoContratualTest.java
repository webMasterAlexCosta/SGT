package com.example.siggtrat.regra;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;

class ConclusaoEfetivoContratualTest {
    @Test
    void pisoGlobalMantemArredondamentoNoMaiorEfetivoRepresentavel() {
        assertEquals(1_610_612_736, RegraEfetivoContratual.pisoNormal(Integer.MAX_VALUE));
        assertTrue(RegraEfetivoContratual.reducaoExcedeLimiteNormal(Integer.MAX_VALUE, 1_610_612_735));
        assertFalse(RegraEfetivoContratual.reducaoExcedeLimiteNormal(Integer.MAX_VALUE, 1_610_612_736));
    }

    /** Amostras de regressão; a demonstração universal separada não depende de enumerar o domínio. */
    @Test
    void pisoNormalConfereResiduosELimitesComOraculoBigIntegerIndependente() {
        List<Integer> amostras = List.of(0, 1, 2, 3, 4, 5, 6, 7, 99, 100, 101,
                28_633_113, 28_633_114, 28_633_115, 28_633_116, 40_000_000,
                Integer.MAX_VALUE - 3, Integer.MAX_VALUE - 2, Integer.MAX_VALUE - 1, Integer.MAX_VALUE);
        for (int original : amostras) {
            // Oráculo racional 3/4, sem 75*n, +99, long ou helper de produção.
            BigInteger[] divisao = BigInteger.valueOf(original).multiply(BigInteger.valueOf(3))
                    .divideAndRemainder(BigInteger.valueOf(4));
            int esperado = divisao[0].add(divisao[1].signum() == 0 ? BigInteger.ZERO : BigInteger.ONE).intValueExact();
            assertEquals(esperado, RegraEfetivoContratual.pisoNormal(original), "original=" + original);
            assertFalse(RegraEfetivoContratual.reducaoExcedeLimiteNormal(original, esperado), "No piso: " + original);
            if (esperado > 0) assertTrue(RegraEfetivoContratual.reducaoExcedeLimiteNormal(original, esperado - 1),
                    "Imediatamente abaixo do piso: " + original);
        }
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
