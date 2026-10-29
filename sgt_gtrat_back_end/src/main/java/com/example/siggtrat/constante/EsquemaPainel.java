package com.example.siggtrat.constante;

/**
 * Versão do formato agregado do painel gerencial.
 *
 * A versão participa tanto da chave persistida no MySQL quanto da chave do
 * Redis. Alterá-la invalida caches de uma versão incompatível sem exigir
 * limpeza manual durante a implantação.
 */
public final class EsquemaPainel {
    public static final String VERSION = "4";
    public static final String CACHE_PREFIX = "schema" + VERSION + ":";

    private EsquemaPainel() {}
}
