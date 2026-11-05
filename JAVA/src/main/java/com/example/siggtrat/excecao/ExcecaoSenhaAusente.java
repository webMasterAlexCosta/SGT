package com.example.siggtrat.excecao;

/**
 * Exceção lançada quando o cabeçalho X-User-Password não é fornecido em uma
 * requisição que exige confirmação de senha. Esta exceção é tratada pelo
 * TratadorGlobalExcecoes para retornar uma resposta estruturada ao cliente.
 */
public class ExcecaoSenhaAusente extends RuntimeException {

    public ExcecaoSenhaAusente(String message) {
        super(message);
    }
}