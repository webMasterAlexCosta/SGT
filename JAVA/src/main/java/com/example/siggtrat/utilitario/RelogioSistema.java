package com.example.siggtrat.utilitario;

import com.example.siggtrat.constante.ConstantesDominio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

public final class RelogioSistema {
    public static final ZoneId ZONA = ZoneId.of(ConstantesDominio.TIMEZONE_PADRAO);

    private RelogioSistema() {}

    public static LocalDate hoje() {
        return LocalDate.now(ZONA);
    }

    public static LocalDateTime agora() {
        return LocalDateTime.now(ZONA);
    }
}
