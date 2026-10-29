package com.example.siggtrat.dto;
import java.math.BigDecimal;
public record FaturaGovernancaDTO(Long id, Long folhaHorasId, String contrato, String status, BigDecimal valorBruto, BigDecimal glosas, BigDecimal valorLiquido, String numeroDocumento) {}
