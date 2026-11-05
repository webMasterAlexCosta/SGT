package com.example.siggtrat.dto;

import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import java.time.LocalDate;
import java.util.List;

public record PreviaTransicaoCatalogoDTO(
        TipoCatalogoTemporal tipoCatalogo, Long catalogoId, LocalDate dataEfetiva,
        Boolean ativoAnterior, boolean ativo, boolean ativoAtualAposRegistro,
        long versaoAtual, String tokenPrevia, List<String> impactos) {}
