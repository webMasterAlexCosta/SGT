package com.example.siggtrat.servico;

import com.example.siggtrat.configuracao.propriedades.PropriedadesSegurancaLogin;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TentativaLoginServicoTest {

    @Test
    void deveBloquearAoAtingirOLimiteConfigurado() {
        PropriedadesSegurancaLogin properties = new PropriedadesSegurancaLogin(2, 60_000);
        TentativaLoginServico servico = new TentativaLoginServico(properties);

        servico.registrarFalhaLogin("usuario@127.0.0.1");
        assertFalse(servico.estaBloqueado("usuario@127.0.0.1"));

        servico.registrarFalhaLogin("usuario@127.0.0.1");
        assertTrue(servico.estaBloqueado("usuario@127.0.0.1"));
    }
}
