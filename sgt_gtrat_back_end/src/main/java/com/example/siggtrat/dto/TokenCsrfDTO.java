package com.example.siggtrat.dto;

/** Contrato do token CSRF entregue ao cliente SPA. */
public record TokenCsrfDTO(String headerName, String parameterName, String token) {}
