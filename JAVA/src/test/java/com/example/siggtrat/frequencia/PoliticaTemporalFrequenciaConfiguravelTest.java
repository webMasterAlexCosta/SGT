package com.example.siggtrat.frequencia;

import com.example.siggtrat.servico.PoliticaTemporalFrequenciaServico;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.*;

import static org.junit.jupiter.api.Assertions.*;

class PoliticaTemporalFrequenciaConfiguravelTest {
    @ParameterizedTest
    @ValueSource(ints = {7, 14, 21, 30})
    void limiteCivilConfiguradoValeNoFusoDaGestaoMesmoComEntradaEmOutroFuso(int dias) {
        var politica = new PoliticaTemporalFrequenciaServico(dias);
        LocalDate data = LocalDate.of(2026, 10, 1);
        ZonedDateTime limite = data.plusDays(dias).atStartOfDay(ZoneId.of("America/Sao_Paulo"));
        assertEquals(dias, politica.diasJanela());
        assertEquals(limite, politica.instanteFechamento(data));
        assertEquals(limite.toLocalDateTime(), politica.prazoLocal(data));
        assertTrue(politica.abertaParaOperacional(data, limite.minusNanos(1)));
        assertFalse(politica.fechada(data, limite.minusNanos(1)));
        assertFalse(politica.abertaParaOperacional(data, limite));
        assertTrue(politica.fechada(data, limite));
        assertTrue(politica.fechada(data, limite.plusNanos(1)));
        assertTrue(politica.fechada(data, limite.withZoneSameInstant(ZoneId.of("Asia/Tokyo"))));
        assertEquals(data, politica.limiteFechamento(limite));
    }

    @ParameterizedTest
    @ValueSource(ints = {7, 14, 21, 30})
    void janelaMaiorNaoAutorizaDiaFuturoNoFusoNegocial(int dias) {
        var politica = new PoliticaTemporalFrequenciaServico(dias);
        ZonedDateTime agora = ZonedDateTime.of(2026, 11, 6, 23, 59, 59, 0, politica.zona());
        assertTrue(politica.abertaParaOperacional(agora.toLocalDate(), agora));
        assertFalse(politica.abertaParaOperacional(agora.toLocalDate().plusDays(1), agora));
        assertFalse(politica.abertaParaOperacional(agora.toLocalDate().plusDays(1),
                agora.withZoneSameInstant(ZoneOffset.UTC)));
    }

    @ParameterizedTest
    @ValueSource(ints = {Integer.MIN_VALUE, -1, 0})
    void configuracaoNaoPositivaFalhaNaInicializacao(int dias) {
        assertThrows(IllegalArgumentException.class, () -> new PoliticaTemporalFrequenciaServico(dias));
    }

    @Test
    void valorPadraoSegueReferenciaHumanaTrinta() {
        assertEquals(30, new PoliticaTemporalFrequenciaServico().diasJanela());
    }
}
