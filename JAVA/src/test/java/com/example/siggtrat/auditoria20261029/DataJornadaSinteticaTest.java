package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.Jornada;
import com.example.siggtrat.entidade.JornadaDiaSemana;
import com.example.siggtrat.servico.PoliticaTemporalFrequenciaServico;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DataJornadaSinteticaTest {
    @ParameterizedTest
    @CsvSource({
            "2026-11-02, 2026-10-30",
            "2026-11-03, 2026-11-02",
            "2026-11-04, 2026-11-03",
            "2026-11-05, 2026-11-04",
            "2026-11-06, 2026-11-05",
            "2026-11-07, 2026-11-06",
            "2026-11-08, 2026-11-06"
    })
    void jornadaDeSegundaASextaEscolheUltimoDiaAnteriorNosSeteDiasDaSemana(
            LocalDate hoje, LocalDate esperada) {
        Jornada jornada = jornada(EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY));
        LocalDate obtida = DataJornadaSintetica.ultimoDiaDeTrabalhoAnterior(jornada, hoje);
        PoliticaTemporalFrequenciaServico politica = new PoliticaTemporalFrequenciaServico();
        assertAll(
                () -> assertEquals(esperada, obtida, "Datas esperadas explícitas incluem a virada do mês e o fim de semana"),
                () -> assertTrue(obtida.isBefore(hoje), "A fixture nunca lança hoje ou no futuro"),
                () -> assertTrue(jornada.getDias().stream().anyMatch(item -> item.getDiaSemana() == obtida.getDayOfWeek()
                        && Boolean.TRUE.equals(item.getDiaTrabalho()))),
                () -> assertTrue(politica.abertaParaOperacional(obtida,
                        hoje.atTime(LocalTime.MAX).atZone(politica.zona())),
                        "A data permanece dentro da política vigente durante todo o dia da execução"));
    }

    @Test
    void respeitaDiaDeTrabalhoDeclaradoSemPresumirFimDeSemanaLivre() {
        Jornada jornada = jornada(EnumSet.of(DayOfWeek.SATURDAY));
        assertEquals(LocalDate.of(2026, 10, 31), DataJornadaSintetica.ultimoDiaDeTrabalhoAnterior(
                jornada, LocalDate.of(2026, 11, 2)));
    }

    @Test
    void recusaJornadaSemDiaTrabalhadoSemModificarAMarcacaoDaFixture() {
        Jornada jornada = jornada(EnumSet.noneOf(DayOfWeek.class));
        assertThrows(IllegalStateException.class, () -> DataJornadaSintetica.ultimoDiaDeTrabalhoAnterior(
                jornada, LocalDate.of(2026, 11, 2)));
        assertTrue(jornada.getDias().stream().allMatch(item -> Boolean.FALSE.equals(item.getDiaTrabalho())));
    }

    @Test
    void naoEscolheHojeNemSeteDiasAtrasQuandoAJanelaJaFechou() {
        Jornada jornada = jornada(EnumSet.of(DayOfWeek.MONDAY));
        assertThrows(IllegalStateException.class, () -> DataJornadaSintetica.ultimoDiaDeTrabalhoAnterior(
                jornada, LocalDate.of(2026, 11, 2)));
    }

    private Jornada jornada(Set<DayOfWeek> trabalhados) {
        Jornada jornada = new Jornada();
        for (DayOfWeek dia : DayOfWeek.values()) {
            JornadaDiaSemana item = new JornadaDiaSemana();
            item.setJornada(jornada);
            item.setDiaSemana(dia);
            item.setDiaTrabalho(trabalhados.contains(dia));
            if (trabalhados.contains(dia)) {
                item.setEntrada1("08:00"); item.setSaida1("12:00");
                item.setEntrada2("13:00"); item.setSaida2("17:00");
            }
            jornada.getDias().add(item);
        }
        return jornada;
    }
}
