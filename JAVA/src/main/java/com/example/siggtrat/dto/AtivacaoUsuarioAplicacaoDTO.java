package com.example.siggtrat.dto;

/** Resposta exibida uma única vez ao gestor que concluiu a ativação. */
public record AtivacaoUsuarioAplicacaoDTO(
        String message,
        String username,
        String temporaryPassword,
        boolean forceChangeOnNextLogin
) {}
