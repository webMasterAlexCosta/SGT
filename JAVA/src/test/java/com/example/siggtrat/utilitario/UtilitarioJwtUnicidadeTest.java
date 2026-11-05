package com.example.siggtrat.utilitario;

import com.example.siggtrat.configuracao.propriedades.PropriedadesTokenJwt;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import java.util.HashSet;
import static org.junit.jupiter.api.Assertions.*;

class UtilitarioJwtUnicidadeTest {
    @Test
    void cadaEmissaoCriaUmaSessaoDistintaMesmoNoMesmoSegundo() {
        UtilitarioJwt jwt = new UtilitarioJwt(new PropriedadesTokenJwt("segredo-apenas-de-teste-".repeat(4), 3600));
        var tokens = new HashSet<String>();
        var identificadores = new HashSet<String>();
        for (int i = 0; i < 5; i++) {
            String token = jwt.gerarToken(7L, true);
            assertTrue(jwt.validarToken(token));
            assertEquals(7L, jwt.extrairIdUsuario(token));
            assertTrue(jwt.segundoFatorVerificado(token));
            tokens.add(token);
            String identificador = jwt.extrairReivindicacao(token, Claims::getId);
            assertNotNull(identificador, "Rotação de sessão exige identificador único de emissão.");
            assertFalse(identificador.isBlank());
            identificadores.add(identificador);
        }
        assertEquals(5, tokens.size());
        assertEquals(5, identificadores.size());
    }
}
