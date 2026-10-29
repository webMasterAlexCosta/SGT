package com.example.siggtrat.regra;

import com.example.siggtrat.utilitario.RelogioSistema;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegraDisponibilidadeCatalogoTest {

    @Test
    void permiteCatalogosAtivosEmQualquerData() {
        assertTrue(RegraDisponibilidadeCatalogo.permiteUso(RelogioSistema.hoje().plusYears(1), true, true, true));
    }

    @Test
    void bloqueiaCatalogoInativoSemHistoricoMesmoEmFatoRetroativo() {
        assertFalse(RegraDisponibilidadeCatalogo.permiteUso(RelogioSistema.hoje().minusDays(1), false));
    }

    @Test
    void bloqueiaCatalogoInativoNoPresente() {
        assertFalse(RegraDisponibilidadeCatalogo.permiteUso(RelogioSistema.hoje(), true, false));
    }

    @Test
    void bloqueiaCatalogoInativoNoFuturo() {
        assertFalse(RegraDisponibilidadeCatalogo.permiteUso(RelogioSistema.hoje().plusDays(1), false, true));
    }
}
