package com.example.siggtrat.servico;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Justificativa humana do novo ato, distinta da narrativa da fonte e do motivo técnico. */
final class JustificativaFonteFrequencia {
    private JustificativaFonteFrequencia() { }

    static String validar(String valor, boolean obrigatoria) {
        String motivo = valor == null || valor.isBlank() ? null : valor.trim();
        if (obrigatoria && motivo == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Informe a justificativa da retificação ou do cancelamento da fonte publicada.");
        if (motivo != null && motivo.length() > 2000) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "A justificativa da fonte deve possuir no máximo 2000 caracteres.");
        return motivo;
    }
}
