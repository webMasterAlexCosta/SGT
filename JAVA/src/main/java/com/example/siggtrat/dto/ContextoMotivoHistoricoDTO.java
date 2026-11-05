package com.example.siggtrat.dto;
import java.time.*;
import com.example.siggtrat.dominio.vinculo.CategoriaTerminoVinculo;
public record ContextoMotivoHistoricoDTO(String descricao, CategoriaTerminoVinculo categoria,
 boolean comprovado, String tipoFonte, Long fonteId, Instant fonteRegistradaEm,
 LocalDate inicioComprovado, LocalDate fimComprovado) {
 public static ContextoMotivoHistoricoDTO ausente() {
  return new ContextoMotivoHistoricoDTO(null, null, false, "AUSENTE", null, null, null, null);
 }
}
