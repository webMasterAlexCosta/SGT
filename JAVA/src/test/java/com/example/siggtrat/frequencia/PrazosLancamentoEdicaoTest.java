package com.example.siggtrat.frequencia;
import com.example.siggtrat.entidade.Frequencia;
import com.example.siggtrat.servico.PoliticaTemporalFrequenciaServico;
import org.junit.jupiter.api.Test;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
class PrazosLancamentoEdicaoTest {
 @Test void relogiosIndependentesELimiteExclusivo() {
  var p=new PoliticaTemporalFrequenciaServico(7,72);var f=new Frequencia();f.setDataReferencia(LocalDate.of(2026,11,10));
  var primeiro=ZonedDateTime.of(2026,11,16,23,0,0,0,p.zona()).toInstant();p.capturarPrazos(f,primeiro);
  assertEquals(LocalDateTime.of(2026,11,17,0,0),p.prazoLocal(f.getDataReferencia()));
  assertEquals(primeiro.plus(Duration.ofHours(72)),f.getPrazoEdicaoEm());
  assertFalse(p.edicaoEncerrada(f,f.getPrazoEdicaoEm().minusNanos(1)));
  assertTrue(p.edicaoEncerrada(f,f.getPrazoEdicaoEm()));
  assertFalse(p.edicaoEncerrada(f,p.instanteFechamento(f.getDataReferencia()).toInstant()));
 }
 @Test void configuracaoPosteriorEEdicoesNaoMudamFotografia() {
  var f=new Frequencia();f.setDataReferencia(LocalDate.of(2026,11,10));var primeiro=Instant.parse("2026-11-12T17:00:00Z");
  new PoliticaTemporalFrequenciaServico(7,72).capturarPrazos(f,primeiro);
  new PoliticaTemporalFrequenciaServico(30,168).capturarPrazos(f,primeiro.plusSeconds(3600));
  assertEquals(primeiro,f.getRegistradoEm());assertEquals(primeiro.plus(Duration.ofHours(72)),f.getPrazoEdicaoEm());
  assertEquals(Instant.parse("2026-11-17T03:00:00Z"),f.getPrazoLancamentoEm());
 }
 @Test void parametrosPositivosEDiarioPadrao() {
  assertThrows(IllegalArgumentException.class,()->new PoliticaTemporalFrequenciaServico(0,72));
  assertThrows(IllegalArgumentException.class,()->new PoliticaTemporalFrequenciaServico(7,0));
  assertEquals(7,new PoliticaTemporalFrequenciaServico().diasJanela());
 }
}
