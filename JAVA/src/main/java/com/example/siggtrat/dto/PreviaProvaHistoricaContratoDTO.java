package com.example.siggtrat.dto;

import java.time.LocalDate;

public record PreviaProvaHistoricaContratoDTO(String tokenPrevia, Long contratoId,
        Long versaoContrato, LocalDate dataEfetiva, boolean recuperaPublicacao,
        int composicoesComprovadas, String efeito) { }
