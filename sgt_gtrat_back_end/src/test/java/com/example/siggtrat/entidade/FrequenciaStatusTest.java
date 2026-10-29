package com.example.siggtrat.entidade;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class FrequenciaStatusTest {

    @Test
    void normalizaStatusConhecido() {
        Frequencia frequencia = new Frequencia();
        frequencia.setDataReferencia(LocalDate.of(2026, 8, 6));
        frequencia.setStatus(" presenca ");

        frequencia.antesPersistirOuAtualizar();

        assertEquals("PRESENCA", frequencia.getStatus());
        assertEquals(8, frequencia.getMes());
        assertEquals(2026, frequencia.getAno());
        assertNotNull(frequencia.getUpdatedAt());
    }

    @Test
    void rejeitaStatusDesconhecido() {
        Frequencia frequencia = new Frequencia();
        frequencia.setDataReferencia(LocalDate.of(2026, 8, 6));
        frequencia.setStatus("PRESENCAAA");

        assertThrows(IllegalStateException.class, frequencia::antesPersistirOuAtualizar);
    }

    @Test
    void naoConverteNullRealEmTexto() {
        Frequencia frequencia = new Frequencia();
        frequencia.setDataReferencia(LocalDate.of(2026, 8, 6));
        frequencia.setStatus(null);

        frequencia.antesPersistirOuAtualizar();

        assertNull(frequencia.getStatus());
    }
}
