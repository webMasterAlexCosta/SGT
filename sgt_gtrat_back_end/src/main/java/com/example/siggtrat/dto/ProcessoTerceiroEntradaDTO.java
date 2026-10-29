package com.example.siggtrat.dto;
import java.time.LocalDate;
public record ProcessoTerceiroEntradaDTO(Long empregadoId, Long contratoId, String tipo, LocalDate prazo, String checklistJson, String observacao) {}
