package com.example.siggtrat.frequencia;

import com.example.siggtrat.configuracao.propriedades.PropriedadesFechamentoFrequencia;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FechamentoFrequenciaPropertiesTest {

    @Test
    void aceitaConfiguracaoCoerente() {
        PropriedadesFechamentoFrequencia p = new PropriedadesFechamentoFrequencia(500, 100_000, 1, 2, 100, 10_000, 120);
        assertEquals(500, p.chunkSize());
        assertEquals(100_000, p.maxAlteracoes());
    }

    @Test
    void rejeitaCorePoolMaiorQueMaxPool() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> new PropriedadesFechamentoFrequencia(500, 100_000, 3, 2, 100, 10_000, 120));
        assertTrue(erro.getMessage().contains("workers"));
    }
}
