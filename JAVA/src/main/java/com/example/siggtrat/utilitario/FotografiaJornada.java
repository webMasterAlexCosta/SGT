package com.example.siggtrat.utilitario;

import com.example.siggtrat.entidade.Jornada;
import com.example.siggtrat.entidade.JornadaDiaSemana;

import java.time.DayOfWeek;
import java.util.Comparator;
import java.util.List;
import java.util.StringJoiner;

/** Formato estável e independente do catálogo para a semana publicada em uma alocação. */
public final class FotografiaJornada {
    private FotografiaJornada() { }

    public record Dia(DayOfWeek diaSemana, boolean diaTrabalho,
                      String entrada1, String saida1, String entrada2, String saida2) {
        public int minutosPrevistos() {
            if (!diaTrabalho) return 0;
            return Math.toIntExact(UtilitarioHorario.calcularIntervaloMinutos(entrada1, saida1,
                    "entrada histórica 1", "saída histórica 1")
                    + UtilitarioHorario.calcularIntervaloMinutos(entrada2, saida2,
                    "entrada histórica 2", "saída histórica 2"));
        }
    }

    public static String capturar(Jornada jornada) {
        if (jornada == null || jornada.getDias() == null || jornada.getDias().isEmpty()) return null;
        StringJoiner semana = new StringJoiner(";");
        jornada.getDias().stream().filter(d -> d.getDiaSemana() != null)
                .sorted(Comparator.comparing(JornadaDiaSemana::getDiaSemana)).forEach(d -> semana.add(
                        d.getDiaSemana().name() + "|" + Boolean.TRUE.equals(d.getDiaTrabalho()) + "|"
                                + texto(d.getEntrada1()) + "|" + texto(d.getSaida1()) + "|"
                                + texto(d.getEntrada2()) + "|" + texto(d.getSaida2())));
        return semana.toString();
    }

    public static List<Dia> dias(String fotografia) {
        if (fotografia == null || fotografia.isBlank()) return List.of();
        return java.util.Arrays.stream(fotografia.split(";"))
                .map(item -> {
                    String[] p = item.split("\\|", -1);
                    if (p.length != 6) throw new IllegalStateException("Fotografia semanal da alocação inválida.");
                    return new Dia(DayOfWeek.valueOf(p[0]), Boolean.parseBoolean(p[1]),
                            nulo(p[2]), nulo(p[3]), nulo(p[4]), nulo(p[5]));
                }).toList();
    }

    public static String minutosPorDia(String fotografia) {
        if (fotografia == null) return null;
        StringJoiner mapa = new StringJoiner(";");
        dias(fotografia).forEach(d -> mapa.add(d.diaSemana().name() + ":" + d.minutosPrevistos()));
        return mapa.toString();
    }

    private static String texto(String valor) { return valor == null ? "" : valor; }
    private static String nulo(String valor) { return valor.isEmpty() ? null : valor; }
}
