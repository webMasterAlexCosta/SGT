package com.example.siggtrat.utilitario;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ContratoFiltroUtilsTest {

    @Test
    void aceitaParametrosRepetidosEVirgulaSemDuplicar() {
        assertEquals(
                List.of(10L, 20L, 30L),
                UtilitarioFiltroContrato.normalizar(List.of("10", "20,30", "10"))
        );
    }

    @Test
    void rejeitaContratoInvalido() {
        assertThrows(
                IllegalArgumentException.class,
                () -> UtilitarioFiltroContrato.normalizar(List.of("10,abc"))
        );
    }
}
