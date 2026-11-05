package com.example.siggtrat.servico;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;

/** Gera credenciais temporárias fortes que são reveladas uma única vez. */
@Service
public class GeradorSenhaTemporariaServico {

    private static final String MAIUSCULAS = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String MINUSCULAS = "abcdefghijkmnopqrstuvwxyz";
    private static final String NUMEROS = "23456789";
    private static final String ESPECIAIS = "@#$%&*!?";
    private static final String TODOS = MAIUSCULAS + MINUSCULAS + NUMEROS + ESPECIAIS;
    private static final int TAMANHO = 16;

    private final SecureRandom random = new SecureRandom();

    public String gerar() {
        char[] senha = new char[TAMANHO];
        senha[0] = sortear(MAIUSCULAS);
        senha[1] = sortear(MINUSCULAS);
        senha[2] = sortear(NUMEROS);
        senha[3] = sortear(ESPECIAIS);
        for (int indice = 4; indice < senha.length; indice++) {
            senha[indice] = sortear(TODOS);
        }
        for (int indice = senha.length - 1; indice > 0; indice--) {
            int destino = random.nextInt(indice + 1);
            char temporario = senha[indice];
            senha[indice] = senha[destino];
            senha[destino] = temporario;
        }
        return new String(senha);
    }

    private char sortear(String alfabeto) {
        return alfabeto.charAt(random.nextInt(alfabeto.length()));
    }
}
