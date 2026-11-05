package com.example.siggtrat.dto;

import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import java.time.Instant;
import java.time.LocalDate;

/** Estado documentado nos dois cortes; null não representa ativo nem inativo. */
public record DisponibilidadeCatalogoDTO(
        TipoCatalogoTemporal tipoCatalogo, Long catalogoId, LocalDate dataReferencia, Instant conhecidoAte,
        Boolean ativo, boolean disponibilidadeComprovada, Long eventoFonteId,
        LocalDate dataEfetivaFonte, Instant registradoEmFonte, Boolean ativoAnterior) {}
