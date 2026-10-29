package com.example.siggtrat.servico;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalculadoraJornadaNoturnaServicoTest {

    @Mock ConfiguracaoServico configuracaoServico;
    private CalculadoraJornadaNoturnaServico calculadora;

    @BeforeEach
    void configurar() {
        when(configuracaoServico.obterHoraInicioNoturno()).thenReturn(LocalTime.of(22, 0));
        when(configuracaoServico.obterHoraFimNoturno()).thenReturn(LocalTime.of(5, 0));
        when(configuracaoServico.obterMinutosPorHoraNoturna()).thenReturn(52.5);
        calculadora = new CalculadoraJornadaNoturnaServico(configuracaoServico);
    }

    @Test
    void turnoDas22As05EquivaleAOitoHorasNoturnas() {
        var resultado = calculadora.calcular("22:00", "05:00", null, null);

        assertEquals(420, resultado.minutosFisicos());
        assertEquals(420, resultado.minutosNoturnosReais());
        assertEquals(480, resultado.minutosNoturnosReduzidos());
        assertEquals(480, resultado.minutosPagaveisBase());
        assertEquals(8.0, resultado.horasNoturnasReduzidas());
        assertFalse(resultado.prorrogacaoNoturna());
    }

    @Test
    void seisHorasNoturnasMantemFracaoExataMesmoComMinutosPagaveisInteiros() {
        var resultado = calculadora.calcular("22:00", "04:00", null, null);

        assertEquals(360, resultado.minutosFisicos());
        assertEquals(360, resultado.minutosNoturnosReais());
        assertEquals(411, resultado.minutosNoturnosReduzidos());
        assertEquals(411, resultado.minutosPagaveisBase());
        assertEquals(6.857142857142857, resultado.horasNoturnasReduzidas(), 1e-12);
    }
}
