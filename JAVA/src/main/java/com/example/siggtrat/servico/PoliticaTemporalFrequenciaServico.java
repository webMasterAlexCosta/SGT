package com.example.siggtrat.servico;

import com.example.siggtrat.constante.ConstantesDominio;
import org.springframework.stereotype.Service;

import java.time.*;

/** Regra única da janela FRE-002/FRE-003/FRE-013. */
@Service
public class PoliticaTemporalFrequenciaServico {
    private final ZoneId zona = ZoneId.of(ConstantesDominio.TIMEZONE_PADRAO);

    public ZonedDateTime instanteFechamento(LocalDate dataFrequencia) {
        if (dataFrequencia == null) throw new IllegalArgumentException("Data da frequência obrigatória.");
        return dataFrequencia.plusDays(7).atStartOfDay(zona);
    }

    public boolean fechada(LocalDate dataFrequencia, ZonedDateTime agora) {
        ZonedDateTime referencia = agora == null ? ZonedDateTime.now(zona) : agora.withZoneSameInstant(zona);
        return !referencia.isBefore(instanteFechamento(dataFrequencia));
    }

    public boolean abertaParaOperacional(LocalDate dataFrequencia, ZonedDateTime agora) {
        return !fechada(dataFrequencia, agora);
    }

    public LocalDateTime prazoLocal(LocalDate dataFrequencia) {
        return instanteFechamento(dataFrequencia).toLocalDateTime();
    }

    public ZoneId zona() { return zona; }
}
