package com.example.siggtrat.utilitario;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class UtilitarioFiltroContrato {

    private UtilitarioFiltroContrato() {
    }

    public static List<Long> normalizar(List<String> valores) {
        if (valores == null || valores.isEmpty()) return List.of();

        Set<Long> ids = new LinkedHashSet<>();
        for (String valor : valores) {
            if (valor == null || valor.isBlank()) continue;
            for (String parte : valor.split(",")) {
                String texto = parte.trim();
                if (texto.isEmpty()) continue;
                long id;
                try {
                    id = Long.parseLong(texto);
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("Identificador de contrato inválido: " + texto + ".", ex);
                }
                if (id <= 0) {
                    throw new IllegalArgumentException("O identificador do contrato deve ser positivo.");
                }
                ids.add(id);
            }
        }
        return new ArrayList<>(ids);
    }
}
