package com.example.siggtrat.dto;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

public record ComposicaoContratualLeituraDTO(@JsonUnwrapped ComposicaoContratualDTO dados,
        String lotacaoMunicipio, String jornadaSemanal, LocalDate dataReferencia, Instant conhecidoAte,
        Map<String, ProvaCampoHistoricoDTO> provaCampos) {
    public ComposicaoContratualLeituraDTO { provaCampos = Map.copyOf(provaCampos); }
}
