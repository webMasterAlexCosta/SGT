package com.example.siggtrat.servico;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeradorSenhaTemporariaServicoTest {

    @Test
    void geraSenhaForteENaoReutilizaValoresNaAmostra() {
        GeradorSenhaTemporariaServico servico = new GeradorSenhaTemporariaServico();
        Set<String> senhas = new HashSet<>();

        for (int indice = 0; indice < 100; indice++) {
            String senha = servico.gerar();
            assertEquals(16, senha.length());
            assertTrue(senha.matches(".*[A-Z].*"));
            assertTrue(senha.matches(".*[a-z].*"));
            assertTrue(senha.matches(".*[0-9].*"));
            assertTrue(senha.matches(".*[@#$%&*!?].*"));
            assertTrue(senhas.add(senha));
        }
    }
}
