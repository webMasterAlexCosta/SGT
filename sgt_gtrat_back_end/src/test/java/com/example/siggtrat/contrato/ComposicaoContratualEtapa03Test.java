package com.example.siggtrat.contrato;

import com.example.siggtrat.entidade.ComposicaoContratual;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ComposicaoContratualEtapa03Test {

    @Test
    void composicaoValidaSomenteDentroDoPeriodo() {
        ComposicaoContratual c = new ComposicaoContratual();
        c.setDataInicioVigencia(LocalDate.of(2026, 1, 1));
        c.setDataFimVigencia(LocalDate.of(2026, 12, 31));
        c.setEfetivoPrevisto(10);

        assertFalse(c.validaEm(LocalDate.of(2025, 12, 31)));
        assertTrue(c.validaEm(LocalDate.of(2026, 1, 1)));
        assertTrue(c.validaEm(LocalDate.of(2026, 12, 31)));
        assertFalse(c.validaEm(LocalDate.of(2027, 1, 1)));
    }

    @Test
    void efetivoZeroNaoPermiteNovaAlocacao() {
        ComposicaoContratual c = new ComposicaoContratual();
        c.setDataInicioVigencia(LocalDate.of(2026, 1, 1));
        c.setEfetivoPrevisto(0);

        assertFalse(c.permiteNovaAlocacaoEm(LocalDate.of(2026, 9, 8)));

        c.setEfetivoPrevisto(1);
        assertTrue(c.permiteNovaAlocacaoEm(LocalDate.of(2026, 9, 8)));
    }
}
