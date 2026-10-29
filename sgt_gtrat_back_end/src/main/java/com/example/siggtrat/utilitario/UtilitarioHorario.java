package com.example.siggtrat.utilitario;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UtilitarioHorario {

    private static final Pattern HORARIO = Pattern.compile("^(?:[01]\\d|2[0-3]):[0-5]\\d$");
    private static final Pattern DURACAO = Pattern.compile("^(\\d{1,3}):([0-5]\\d)$");
    private static final Pattern DECIMAL = Pattern.compile("^\\d{1,3}(?:[.,]\\d{1,4})?$");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter
            .ofPattern("HH:mm", Locale.ROOT)
            .withResolverStyle(ResolverStyle.STRICT);

    private UtilitarioHorario() {}

    public static String normalizarHorario(String valor, String campo) {
        if (valor == null || valor.isBlank()) return null;
        String limpo = valor.trim();
        if (!HORARIO.matcher(limpo).matches()) {
            throw new IllegalArgumentException(nomeCampo(campo) + " deve estar no formato HH:mm, entre 00:00 e 23:59.");
        }
        try {
            LocalTime.parse(limpo, FORMATTER);
            return limpo;
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(nomeCampo(campo) + " contém um horário inválido.", ex);
        }
    }

    public static int horarioParaMinutos(String valor, String campo) {
        LocalTime horario = LocalTime.parse(normalizarHorario(valor, campo), FORMATTER);
        return horario.getHour() * 60 + horario.getMinute();
    }

    public static int duracaoParaMinutos(String valor, String campo) {
        if (valor == null || valor.isBlank()) return 0;
        String limpo = valor.trim();
        Matcher matcher = DURACAO.matcher(limpo);
        if (matcher.matches()) {
            long horas = Long.parseLong(matcher.group(1));
            long minutos = Long.parseLong(matcher.group(2));
            long total = Math.addExact(Math.multiplyExact(horas, 60L), minutos);
            if (total > Integer.MAX_VALUE) throw new IllegalArgumentException(nomeCampo(campo) + " excede o limite permitido.");
            return (int) total;
        }
        if (DECIMAL.matcher(limpo).matches()) {
            double horas = Double.parseDouble(limpo.replace(',', '.'));
            if (!Double.isFinite(horas) || horas < 0) {
                throw new IllegalArgumentException(nomeCampo(campo) + " contém uma duração inválida.");
            }
            double total = horas * 60.0;
            if (total > Integer.MAX_VALUE) throw new IllegalArgumentException(nomeCampo(campo) + " excede o limite permitido.");
            return (int) Math.round(total);
        }
        throw new IllegalArgumentException(nomeCampo(campo) + " deve usar HH:mm ou horas decimais não negativas.");
    }

    public static long calcularIntervaloMinutos(String entrada, String saida, String campoEntrada, String campoSaida) {
        String entradaNormalizada = normalizarHorario(entrada, campoEntrada);
        String saidaNormalizada = normalizarHorario(saida, campoSaida);
        if (entradaNormalizada == null && saidaNormalizada == null) return 0;
        if (entradaNormalizada == null || saidaNormalizada == null) {
            throw new IllegalArgumentException("Informe " + nomeCampo(campoEntrada) + " e " + nomeCampo(campoSaida) + " em conjunto.");
        }
        LocalTime ent = LocalTime.parse(entradaNormalizada, FORMATTER);
        LocalTime sai = LocalTime.parse(saidaNormalizada, FORMATTER);
        if (sai.isBefore(ent)) {
            return Duration.between(ent, LocalTime.MAX).toMinutes()
                    + Duration.between(LocalTime.MIN, sai).toMinutes() + 1;
        }
        return Duration.between(ent, sai).toMinutes();
    }

    public static void validarPar(String entrada, String saida, String campoEntrada, String campoSaida) {
        calcularIntervaloMinutos(entrada, saida, campoEntrada, campoSaida);
    }

    private static String nomeCampo(String campo) {
        return campo == null || campo.isBlank() ? "O campo de horário" : "O campo " + campo;
    }
}
