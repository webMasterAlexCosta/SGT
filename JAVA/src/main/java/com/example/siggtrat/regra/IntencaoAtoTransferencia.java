package com.example.siggtrat.regra;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;

/** Snapshot legível da intenção; fronteiras e digest usam o codec comum53. Não comprova o teor de documento externo. */
public final class IntencaoAtoTransferencia {
    public static final String VERSAO = "transferencia-gestao-intencao-v1";
    private static final ObjectMapper JSON = new ObjectMapper();
    private IntencaoAtoTransferencia() { }
    public record Material(String versao, String canonico, String hash) { }
    public static Material capturar(Map<String, Object> campos) {
        try {
            String canonico = JSON.writeValueAsString(ordenar(campos));
            return new Material(VERSAO, canonico, MaterialHashEventos.sha256(VERSAO,
                    MaterialHashEventos.texto(canonico)));
        } catch (JsonProcessingException erro) {
            throw new IllegalStateException("Não foi possível registrar a intenção da transferência.", erro);
        }
    }
    private static Object ordenar(Object valor) {
        if (valor instanceof Map<?, ?> mapa) {
            Map<String, Object> ordenado = new TreeMap<>();
            mapa.forEach((chave, item) -> ordenado.put((String) chave, ordenar(item)));
            return ordenado;
        }
        if (valor instanceof Collection<?> lista) return lista.stream().map(IntencaoAtoTransferencia::ordenar).toList();
        return valor;
    }
}
