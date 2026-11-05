package com.example.siggtrat.servico;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TotpServicoTest {

    private final TotpServico servico = new TotpServico();

    @Test
    void segredoGeradoPeloServicoEhUtilizavel() {
        String segredo = servico.gerarSegredo();

        assertNotNull(segredo);
        assertFalse(segredo.isBlank());
        assertTrue(servico.segredoUtilizavel(segredo));
    }

    @Test
    void segredoForaDoPadraoBase32NaoEhAceito() {
        String segredoInvalido = "67b4ef511d9342f4a82012df9998ac01";

        assertFalse(servico.segredoUtilizavel(segredoInvalido));
        assertFalse(servico.validarCodigo(segredoInvalido, 123456));
    }
}
