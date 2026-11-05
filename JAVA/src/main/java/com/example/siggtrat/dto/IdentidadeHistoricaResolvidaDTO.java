package com.example.siggtrat.dto;
import java.time.*;
public record IdentidadeHistoricaResolvidaDTO(String nome, boolean comprovado, String tipoFonte,
 Long fonteId, Instant fonteRegistradaEm, LocalDate inicioComprovado, LocalDate fimComprovado) {
 public static IdentidadeHistoricaResolvidaDTO ausente() {
  return new IdentidadeHistoricaResolvidaDTO(null, false, "AUSENTE", null, null, null, null);
 }
}
