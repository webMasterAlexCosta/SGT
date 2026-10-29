package com.example.siggtrat.dto;

public record ResultadoRedefinicaoSenhaDTO(
        String message,
        boolean forceChangeOnNextLogin
) {}
