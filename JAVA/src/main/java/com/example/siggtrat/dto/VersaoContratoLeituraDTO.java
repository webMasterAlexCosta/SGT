package com.example.siggtrat.dto;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import java.util.Map;

/** Extensão exclusiva da leitura; o DTO da fotografia de escrita/hash não é alterado. */
public record VersaoContratoLeituraDTO(@JsonUnwrapped VersaoContratoNegocialDTO dados,
                                      Map<String, ProvaCampoHistoricoDTO> provaCampos) {
    public VersaoContratoLeituraDTO { provaCampos = Map.copyOf(provaCampos); }
}
