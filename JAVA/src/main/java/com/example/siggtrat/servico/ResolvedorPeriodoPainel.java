package com.example.siggtrat.servico;

import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/** Calendário exclusivo da visão analítica; não altera os períodos do efetivo legado. */
public final class ResolvedorPeriodoPainel {
    public enum Granularidade { DIARIO, SEMANAL, MENSAL, TRIMESTRAL, ANUAL }
    public record Periodo(String granularidade, LocalDate inicio, LocalDate fim, boolean parcial,
                          LocalDate anteriorInicio, LocalDate anteriorFim) {}
    public record Faixa(LocalDate inicio, LocalDate fim, String rotulo, boolean parcial) {}

    private ResolvedorPeriodoPainel() {}

    public static Periodo resolver(String tipo, LocalDate referencia) {
        Granularidade g = Granularidade.valueOf(tipo);
        LocalDate inicio = inicio(g, referencia);
        LocalDate proximo = deslocar(g, inicio, 1);
        boolean parcial = !referencia.equals(proximo.minusDays(1));
        LocalDate anterior = deslocar(g, inicio, -1);
        LocalDate limite = inicio.minusDays(1);
        LocalDate fimAnterior = parcial ? anterior.plusDays(ChronoUnit.DAYS.between(inicio, referencia)) : limite;
        if (fimAnterior.isAfter(limite)) fimAnterior = limite;
        return new Periodo(g.name(), inicio, referencia, parcial, anterior, fimAnterior);
    }

    public static List<Faixa> evolucao(String tipo, LocalDate referencia) {
        Granularidade g = Granularidade.valueOf(tipo);
        int quantidade = switch (g) { case DIARIO -> 14; case SEMANAL, MENSAL -> 12; case TRIMESTRAL -> 8; case ANUAL -> 5; };
        LocalDate atual = inicio(g, referencia);
        long diasDecorridos = ChronoUnit.DAYS.between(atual, referencia);
        boolean corteParcial = !referencia.equals(deslocar(g, atual, 1).minusDays(1));
        List<Faixa> faixas = new ArrayList<>();
        for (int i = quantidade - 1; i >= 0; i--) {
            LocalDate inicio = deslocar(g, atual, -i);
            LocalDate fim = deslocar(g, inicio, 1).minusDays(1);
            LocalDate corte = corteParcial ? inicio.plusDays(diasDecorridos) : fim;
            if (corte.isAfter(fim)) corte = fim;
            faixas.add(new Faixa(inicio, corte, rotulo(g, inicio), corte.isBefore(fim)));
        }
        return List.copyOf(faixas);
    }

    public static List<Faixa> heatmap(String tipo, LocalDate referencia) {
        Granularidade g = Granularidade.valueOf(tipo);
        LocalDate inicio = inicio(g, referencia);
        List<Faixa> faixas = new ArrayList<>();
        int quantidade = switch (g) { case DIARIO, SEMANAL -> 7; case MENSAL -> 5; case TRIMESTRAL -> 3; case ANUAL -> 12; };
        for (int i = 0; i < quantidade; i++) {
            LocalDate desde = switch (g) {
                case DIARIO -> referencia.minusDays(6 - i);
                case SEMANAL -> inicio.plusDays(i);
                case MENSAL -> inicio.plusDays(7L * i);
                case TRIMESTRAL, ANUAL -> inicio.plusMonths(i);
            };
            if (desde.isAfter(referencia)) break;
            LocalDate ate = switch (g) {
                case DIARIO, SEMANAL -> desde;
                case MENSAL -> desde.plusDays(6).isAfter(inicio.plusMonths(1).minusDays(1)) ? inicio.plusMonths(1).minusDays(1) : desde.plusDays(6);
                case TRIMESTRAL, ANUAL -> desde.plusMonths(1).minusDays(1);
            };
            boolean parcial = ate.isAfter(referencia);
            faixas.add(new Faixa(desde, parcial ? referencia : ate,
                    g == Granularidade.MENSAL ? "Dias " + desde.getDayOfMonth() + "–" + (parcial ? referencia : ate).getDayOfMonth() : desde.toString(), parcial));
        }
        return List.copyOf(faixas);
    }

    private static LocalDate inicio(Granularidade g, LocalDate ref) {
        return switch (g) {
            case DIARIO -> ref;
            case SEMANAL -> ref.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MENSAL -> ref.withDayOfMonth(1);
            case TRIMESTRAL -> LocalDate.of(ref.getYear(), ((ref.getMonthValue()-1)/3)*3+1, 1);
            case ANUAL -> ref.withDayOfYear(1);
        };
    }
    private static LocalDate deslocar(Granularidade g, LocalDate data, int vezes) {
        return switch (g) {
            case DIARIO -> data.plusDays(vezes); case SEMANAL -> data.plusWeeks(vezes);
            case MENSAL -> data.plusMonths(vezes); case TRIMESTRAL -> data.plusMonths(3L*vezes); case ANUAL -> data.plusYears(vezes);
        };
    }
    private static String rotulo(Granularidade g, LocalDate data) {
        return switch (g) {
            case ANUAL -> String.valueOf(data.getYear());
            case TRIMESTRAL -> ((data.getMonthValue()-1)/3+1) + "º tri/" + data.getYear();
            case MENSAL -> "%02d/%d".formatted(data.getMonthValue(), data.getYear());
            default -> "%02d/%02d/%d".formatted(data.getDayOfMonth(), data.getMonthValue(), data.getYear());
        };
    }
}
