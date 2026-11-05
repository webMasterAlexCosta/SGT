package com.example.siggtrat.dto;

public record CredenciaisLoginDTO(
        String matricula,
        String password,
        String totp
) {}
