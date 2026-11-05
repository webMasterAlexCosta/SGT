package com.example.siggtrat.servico;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodigoQrServicoTest {

    private final CodigoQrServico servico = new CodigoQrServico();

    @Test
    void geraSvgEmDataUrlSemServicoExterno() {
        String dataUrl = servico.gerarDataUrl(
                "otpauth://totp/SGT-GTRAT:usuario?secret=JBSWY3DPEHPK3PXP&issuer=SGT-GTRAT"
        );

        assertTrue(dataUrl.startsWith("data:image/svg+xml;base64,"));
        String base64 = dataUrl.substring(dataUrl.indexOf(',') + 1);
        String svg = new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
        assertTrue(svg.startsWith("<svg"));
        assertTrue(svg.contains("<path"));
    }

    @Test
    void rejeitaConteudoVazio() {
        assertThrows(IllegalArgumentException.class, () -> servico.gerarDataUrl(" "));
    }
}
