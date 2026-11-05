package com.example.siggtrat.dto;

import java.util.List;

/**
 * Diagnóstico calculado sobre a prova integral preservada, sem regravar nem
 * reassinar a cadeia. eventoOriginal mantém a identidade do evento, mas pode
 * apresentar conteúdo minimizado conforme sua flag e a autorização do leitor.
 */
public record EvidenciasIntegridadeFrequenciaDTO(
        Long frequenciaId,
        String estado,
        boolean cadeiaIntegra,
        List<Evento> eventos
) {
    public EvidenciasIntegridadeFrequenciaDTO { eventos = List.copyOf(eventos); }
    public record Evento(
            EventoFrequenciaDTO eventoOriginal,
            String hashArmazenado,
            String hashCalculado,
            boolean hashConfere,
            boolean encadeamentoConfere
    ) {}
}
