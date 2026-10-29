package com.example.siggtrat.excecao;

/**
 * Exceção lançada quando uma operação protegida é executada sem que o
 * usuário esteja autenticado no contexto de segurança. Esta exceção
 * normalmente resulta em um status HTTP 401 (Unauthorized).
 */
public class ExcecaoUsuarioNaoAutenticado extends RuntimeException {

    public ExcecaoUsuarioNaoAutenticado(String message) {
        super(message);
    }
}