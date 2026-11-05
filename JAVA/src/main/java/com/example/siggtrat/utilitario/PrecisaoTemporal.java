package com.example.siggtrat.utilitario;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** Instantes de fatos novos e limites de conhecimento na precisão do MySQL. */
public final class PrecisaoTemporal {

    private PrecisaoTemporal() {}

    /** Canoniza antes da persistência, do hash e da resposta, sem alterar fatos já gravados. */
    public static Instant instantePersistivelMicros(Instant instante) {
        return instante == null ? null : instante.truncatedTo(ChronoUnit.MICROS);
    }

    public static Instant agoraMicros() {
        return instantePersistivelMicros(Instant.now());
    }

    /**
     * Preserva {@code registradoEm <= conhecidoAte} para registros armazenados em
     * microssegundos, impedindo que o JDBC arredonde o limite para o futuro.
     * Nulo permanece nulo: a política de ausência de K pertence ao chamador.
     */
    public static Instant limiteInclusivoMicros(Instant conhecidoAte) {
        return instantePersistivelMicros(conhecidoAte);
    }
}
