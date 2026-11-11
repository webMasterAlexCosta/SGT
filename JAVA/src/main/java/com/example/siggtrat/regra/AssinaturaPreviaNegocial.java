package com.example.siggtrat.regra;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

/** Prévia e intenções em base nova: compartilha o codec de53, com domínios separados. */
public final class AssinaturaPreviaNegocial {
    public static final String CHAVE_MARCADOR = "FORMATO_HASH_PREVIAS_E_INTENCOES";
    public static final String FORMATO = "SGT-PREVIAS-INTENCOES-TIPADO-V1";
    private AssinaturaPreviaNegocial() { }

    public static String sha256(Collection<?> partes) {
        return sha256("sgt.previa.generica.v1", partes);
    }
    public static String sha256(String dominio, Collection<?> partes) {
        return MaterialHashEventos.sha256(dominio, campo(partes));
    }
    /** Lista posicional que aceita null: não usar List.of em campos opcionais brutos. */
    public static List<Object> partes(Object... valores) {
        return valores == null ? null : Collections.unmodifiableList(new ArrayList<>(Arrays.asList(valores)));
    }
    private static MaterialHashEventos.Campo campo(Object valor) {
        if (valor == null) return MaterialHashEventos.texto(null);
        if (valor instanceof String s) return MaterialHashEventos.texto(s);
        if (valor instanceof Long n) return MaterialHashEventos.longo(n);
        if (valor instanceof Integer n) return MaterialHashEventos.inteiro(n);
        if (valor instanceof LocalDate d) return MaterialHashEventos.data(d);
        if (valor instanceof Enum<?> e) return MaterialHashEventos.enumeracao(e);
        if (valor instanceof Boolean b) return MaterialHashEventos.grupo(MaterialHashEventos.texto("java.lang.Boolean"), MaterialHashEventos.inteiro(b ? 1 : 0));
        if (valor instanceof Instant i) return MaterialHashEventos.grupo(MaterialHashEventos.texto("java.time.Instant"), MaterialHashEventos.texto(i.toString()));
        if (valor instanceof UUID u) return MaterialHashEventos.grupo(MaterialHashEventos.texto("java.util.UUID"), MaterialHashEventos.texto(u.toString()));
        if (valor instanceof Collection<?> lista) return MaterialHashEventos.grupo(MaterialHashEventos.texto("java.util.Collection"), MaterialHashEventos.grupo(lista.stream().map(AssinaturaPreviaNegocial::campo).toArray(MaterialHashEventos.Campo[]::new)));
        throw new IllegalArgumentException("SGT_PREVIA_TIPO_NAO_SUPORTADO");
    }
    /** Compatibilidade de outros produtores legados: NÃO usar para material novo desta família. */
    public static String valor(Object valor) { return valor == null ? "<null>" : String.valueOf(valor); }
}
