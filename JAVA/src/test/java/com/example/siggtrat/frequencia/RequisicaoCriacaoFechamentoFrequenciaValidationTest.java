package com.example.siggtrat.frequencia;

import com.example.siggtrat.dto.RequisicaoCriacaoFechamentoFrequencia;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RequisicaoCriacaoFechamentoFrequenciaValidationTest {

    private static jakarta.validation.ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void abrirValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void fecharValidator() {
        factory.close();
    }

    @Test
    void listasNulasViraramListasVazias() {
        var request = new RequisicaoCriacaoFechamentoFrequencia(LocalDate.of(2026, 8, 6), 1L, 2L, null, null);
        assertNotNull(request.contratoIds());
        assertNotNull(request.alteracoes());
        assertTrue(request.contratoIds().isEmpty());
        assertTrue(request.alteracoes().isEmpty());
    }

    @Test
    void requestValidoNaoTemViolacoes() {
        var request = new RequisicaoCriacaoFechamentoFrequencia(LocalDate.of(2026, 8, 6), 1L, 2L, List.of(3L), List.of());
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void dataLotacaoEJornadaSaoObrigatorias() {
        var request = new RequisicaoCriacaoFechamentoFrequencia(null, null, null, List.of(), List.of());
        assertEquals(3, validator.validate(request).size());
    }

    @Test
    void idsPrecisamSerPositivos() {
        var request = new RequisicaoCriacaoFechamentoFrequencia(LocalDate.of(2026, 8, 6), 0L, -2L, List.of(0L, -1L), List.of());
        assertTrue(validator.validate(request).size() >= 4);
    }
}
