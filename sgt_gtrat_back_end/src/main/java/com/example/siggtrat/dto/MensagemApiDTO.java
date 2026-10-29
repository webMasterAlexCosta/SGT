package com.example.siggtrat.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MensagemApiDTO(String message, String error) {
    public static MensagemApiDTO sucesso(String message) { return new MensagemApiDTO(message, null); }
    public static MensagemApiDTO erro(String error) { return new MensagemApiDTO(null, error); }
}
