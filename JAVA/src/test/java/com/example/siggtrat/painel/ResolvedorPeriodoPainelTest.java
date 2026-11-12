package com.example.siggtrat.painel;

import com.example.siggtrat.servico.ResolvedorPeriodoPainel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class ResolvedorPeriodoPainelTest {
    @Test void mapaDiarioTemContextoSeteDiasMasKpiSoDiaDaReferencia() {
        LocalDate ref = LocalDate.of(2026,11,10);
        var colunas = ResolvedorPeriodoPainel.heatmap("DIARIO", ref);
        assertEquals(7, colunas.size());
        assertEquals(ref.minusDays(6), colunas.get(0).inicio());
        assertEquals(ref, colunas.get(6).fim());
        assertEquals(ref,ResolvedorPeriodoPainel.resolver("DIARIO",ref).inicio());
    }
    @ParameterizedTest
    @CsvSource({"DIARIO,2026-11-10,2026-11-10,2026-11-09,2026-11-09",
        "SEMANAL,2026-11-10,2026-11-09,2026-11-02,2026-11-03",
        "MENSAL,2026-11-10,2026-11-01,2026-10-01,2026-10-10",
        "TRIMESTRAL,2026-11-10,2026-10-01,2026-07-01,2026-08-10",
        "ANUAL,2026-11-10,2026-01-01,2025-01-01,2025-11-10",
        "SEMANAL,2027-01-01,2026-12-28,2026-12-21,2026-12-25",
        "MENSAL,2024-03-31,2024-03-01,2024-02-01,2024-02-29",
        "MENSAL,2026-04-30,2026-04-01,2026-03-01,2026-03-31",
        "ANUAL,2024-02-29,2024-01-01,2023-01-01,2023-03-01",
        "ANUAL,2024-12-31,2024-01-01,2023-01-01,2023-12-31"})
    void resolveCalendarioEComparacao(String tipo, LocalDate ref, LocalDate inicio, LocalDate anteriorInicio, LocalDate anteriorFim) {
        var periodo = ResolvedorPeriodoPainel.resolver(tipo, ref);
        assertEquals(inicio, periodo.inicio());
        assertEquals(ref, periodo.fim());
        assertEquals(anteriorInicio, periodo.anteriorInicio());
        assertEquals(anteriorFim, periodo.anteriorFim());
    }

    @Test void heatmapMensalParticionaMesSemPerderDia31() {
        var colunas = ResolvedorPeriodoPainel.heatmap("MENSAL", LocalDate.of(2026, 10, 31));
        assertEquals(5, colunas.size());
        assertEquals(LocalDate.of(2026,10,29), colunas.get(4).inicio());
        assertEquals(LocalDate.of(2026,10,31), colunas.get(4).fim());
        assertEquals(31, colunas.stream().mapToLong(c -> java.time.temporal.ChronoUnit.DAYS.between(c.inicio(),c.fim())+1).sum());
    }

    @Test void rejeitaGranularidadeInvalida() {
        assertThrows(IllegalArgumentException.class, () -> ResolvedorPeriodoPainel.resolver("90", LocalDate.of(2026,11,10)));
    }

    @Test void contextoHistoricoLimitaPontosEIdentificaParcial() {
        var serie = ResolvedorPeriodoPainel.evolucao("ANUAL", LocalDate.of(2026,11,10));
        assertEquals(5, serie.size());
        assertEquals(LocalDate.of(2022,1,1), serie.get(0).inicio());
        assertEquals(LocalDate.of(2026,11,10), serie.get(4).fim());
        assertTrue(serie.get(4).parcial());
        assertTrue(serie.get(0).parcial());
        assertEquals(java.time.temporal.ChronoUnit.DAYS.between(serie.get(4).inicio(),serie.get(4).fim()),java.time.temporal.ChronoUnit.DAYS.between(serie.get(0).inicio(),serie.get(0).fim()));
    }
}
