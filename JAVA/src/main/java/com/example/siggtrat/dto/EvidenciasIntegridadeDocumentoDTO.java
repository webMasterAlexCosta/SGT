package com.example.siggtrat.dto;

import java.util.List;

/** Expõe as evidências preservadas sem alterar ou reassinar eventos históricos. */
public record EvidenciasIntegridadeDocumentoDTO(
        Long documentoId,
        String grupoLogicoId,
        String estado,
        boolean cadeiaIntegra,
        List<Evento> eventos
) {
    public EvidenciasIntegridadeDocumentoDTO { eventos = List.copyOf(eventos); }

    public record Evento(
            EventoDocumentoComprobatorioDTO eventoOriginal,
            String hashArmazenado,
            String hashCalculado,
            boolean hashConfere,
            boolean encadeamentoConfere
    ) {}
}
