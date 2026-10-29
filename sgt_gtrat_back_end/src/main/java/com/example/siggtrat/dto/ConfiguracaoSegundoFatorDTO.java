package com.example.siggtrat.dto;
public record ConfiguracaoSegundoFatorDTO(String secret, String otpauthUri, String qrCodeDataUrl, int periodSeconds, String message) {}
