package com.example.siggtrat.dto;
import java.math.BigDecimal;
import java.time.LocalDate;
public record TabelaTarifariaGovernancaEntradaDTO(Long contratoId, Long cargoId, LocalDate vigenciaInicio, LocalDate vigenciaFim, BigDecimal valorHora, BigDecimal adicionalNoturnoPercent) {}
