package com.example.siggtrat.dto;

import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import java.time.Instant;
import java.time.LocalDate;

/** Recibo imutável da intenção; um reenvio conserva o evento e a versão originais. */
public record TransicaoCatalogoDTO(
        Long eventoId, TipoCatalogoTemporal tipoCatalogo, Long catalogoId, LocalDate dataEfetiva,
        boolean ativo, Instant registradoEm, Boolean ativoAtualAposRegistro, long versaoAtual, boolean reenvio) {}
