package com.example.siggtrat.dto;

import java.time.LocalDateTime;

public record HistoricoEmpregadoDTO(
        Long id,
        EmpregadoReferenciaDTO empregado,
        LocalDateTime dataEvento,
        String descricao,
        UsuarioReferenciaDTO usuario,
        String hashIntegridade,
        long version,
        java.time.LocalDate dataEfetiva,
        Integer versaoAssinatura,
        Long ocorrenciaId
) {
    public HistoricoEmpregadoDTO(Long id, EmpregadoReferenciaDTO empregado, LocalDateTime dataEvento,
                                String descricao, UsuarioReferenciaDTO usuario, String hashIntegridade, long version,
                                java.time.LocalDate dataEfetiva, Integer versaoAssinatura) {
        this(id, empregado, dataEvento, descricao, usuario, hashIntegridade, version, dataEfetiva, versaoAssinatura, null);
    }
    public HistoricoEmpregadoDTO(Long id, EmpregadoReferenciaDTO empregado, LocalDateTime dataEvento,
                                String descricao, UsuarioReferenciaDTO usuario, String hashIntegridade, long version) {
        this(id, empregado, dataEvento, descricao, usuario, hashIntegridade, version, null, null, null);
    }
}
