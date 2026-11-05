package com.example.siggtrat.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record VersaoComplementoFrequenciaDTO(Long id, String tipoComplemento, Long complementoId,
        Integer numeroVersao, String tipoOperacao, LocalDate dataEfetiva, Instant registradoEm,
        Long usuarioId, String usuarioUsername, String justificativa, String motivoTecnico,
        Long versaoAnteriorId, JsonNode fotografia, List<ContextoComplementoFrequenciaDTO> contextos,
        String hashAnterior, String hashVersao) { }
