package com.example.siggtrat.dto;
import java.math.BigDecimal;
import java.time.LocalDate;
public record FolhaHorasGovernancaDTO(Long id, Long contratoId, String contrato, LocalDate inicio, LocalDate fim, String status, Integer minutosFisicos, Integer minutosNoturnos, Integer minutosAdicionais, Integer minutosPagaveis, BigDecimal valorApurado, String calculoVersao, String submetidoPor, String aprovadoPor, String motivoRejeicao) {}
