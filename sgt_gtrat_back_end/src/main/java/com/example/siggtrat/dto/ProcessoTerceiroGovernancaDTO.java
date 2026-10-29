package com.example.siggtrat.dto;
import java.time.LocalDate;
public record ProcessoTerceiroGovernancaDTO(Long id, Long empregadoId, String empregado, Long contratoId, String contrato, String tipo, String status, LocalDate prazo, String checklistJson, String observacao) {}
