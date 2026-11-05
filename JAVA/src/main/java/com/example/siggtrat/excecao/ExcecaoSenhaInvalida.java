package com.example.siggtrat.excecao;

/**
 * Exceção lançada quando a senha fornecida pelo usuário não corresponde à
 * senha armazenada no banco de dados. Esta exceção indica uma tentativa
 * de autenticação inválida para endpoints que exigem confirmação de senha.
 */
public class ExcecaoSenhaInvalida extends RuntimeException {

    public ExcecaoSenhaInvalida(String message) {
        super(message);
    }
}