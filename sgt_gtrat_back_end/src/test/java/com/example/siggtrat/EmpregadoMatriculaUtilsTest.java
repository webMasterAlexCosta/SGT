package com.example.siggtrat;

import com.example.siggtrat.utilitario.UtilitarioMatriculaEmpregado;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmpregadoMatriculaUtilsTest {

    @Test
    void deveAceitarIdentificadorLivreDeAteTrintaCaracteres() {
        assertEquals("ABC-2026 / RJ", UtilitarioMatriculaEmpregado.normalizar("  ABC-2026 / RJ  "));
        assertEquals("12345678", UtilitarioMatriculaEmpregado.normalizar("12345678"));
    }

    @Test
    void deveConverterValorVazioEmNull() {
        assertNull(UtilitarioMatriculaEmpregado.normalizar(null));
        assertNull(UtilitarioMatriculaEmpregado.normalizar("   "));
    }

    @Test
    void deveRejeitarValorAcimaDeTrintaCaracteres() {
        String valor = "X".repeat(31);
        IllegalArgumentException erro = assertThrows(
                IllegalArgumentException.class,
                () -> UtilitarioMatriculaEmpregado.normalizar(valor)
        );
        assertTrue(erro.getMessage().contains("30"));
    }
}
