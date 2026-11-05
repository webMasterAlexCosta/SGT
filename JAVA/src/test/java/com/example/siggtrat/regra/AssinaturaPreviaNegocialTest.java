package com.example.siggtrat.regra;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AssinaturaPreviaNegocialTest {

    @Test
    void mesmaFotografiaProduzMesmoToken() {
        String a = AssinaturaPreviaNegocial.sha256(List.of("contrato:10", "efetivo:100", "ocupacao:90"));
        String b = AssinaturaPreviaNegocial.sha256(List.of("contrato:10", "efetivo:100", "ocupacao:90"));
        assertEquals(a, b);
        assertEquals(64, a.length());
    }

    @Test
    void mudancaDeImpactoInvalidaTokenAnterior() {
        String anterior = AssinaturaPreviaNegocial.sha256(List.of("contrato:10", "efetivo:100", "ocupacao:90"));
        String novo = AssinaturaPreviaNegocial.sha256(List.of("contrato:10", "efetivo:100", "ocupacao:91"));
        assertNotEquals(anterior, novo);
    }
}
