package com.example.siggtrat.dto;
import java.math.BigDecimal;
import java.time.LocalDate;
public record TabelaTarifariaGovernancaDTO(Long id, Long contratoId, String contrato, Long cargoId, String cargo, LocalDate vigenciaInicio, LocalDate vigenciaFim, BigDecimal valorHora, BigDecimal adicionalNoturnoPercent, Boolean ativo) {}
