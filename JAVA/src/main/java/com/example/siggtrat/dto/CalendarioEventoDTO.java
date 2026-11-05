package com.example.siggtrat.dto;

import java.time.LocalDate;

public record CalendarioEventoDTO(
        Long id,
        LocalDate data,
        String tipo,
        String descricao,
        String abrangencia,
        String uf,
        String municipio,
        SuperintendenciaDTO superintendencia,
        GestaoLocalDTO gestaoLocal,
        Long lotacaoId,
        Boolean ativo,
        long version,
        boolean cancelado
) {
    public CalendarioEventoDTO(Long id, LocalDate data, String tipo, String descricao, String abrangencia,
            String uf, String municipio, SuperintendenciaDTO superintendencia, GestaoLocalDTO gestaoLocal,
            Long lotacaoId, Boolean ativo, long version) {
        this(id, data, tipo, descricao, abrangencia, uf, municipio, superintendencia, gestaoLocal,
                lotacaoId, ativo, version, false);
    }
}
