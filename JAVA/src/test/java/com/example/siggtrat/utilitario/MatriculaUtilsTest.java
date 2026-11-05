package com.example.siggtrat.utilitario;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MatriculaUtilsTest {

    @Test
    void normalizaEFormataMatriculaCanonica() {
        assertEquals("89614745", UtilitarioMatricula.normalizar("8.961.474-5"));
        assertEquals("8.961.474-5", UtilitarioMatricula.formatar("89614745"));
    }

    @Test
    void rejeitaQuantidadeIncorretaDeDigitos() {
        assertThrows(IllegalArgumentException.class,
                () -> UtilitarioMatricula.normalizar("1234567"));
    }
}
