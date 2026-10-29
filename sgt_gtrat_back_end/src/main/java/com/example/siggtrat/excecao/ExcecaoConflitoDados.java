package com.example.siggtrat.excecao;

/**
 * Indica que a operação é válida, mas conflita com um registro ou regra já existente.
 */
public class ExcecaoConflitoDados extends RuntimeException {

    public ExcecaoConflitoDados(String message) {
        super(message);
    }
}
