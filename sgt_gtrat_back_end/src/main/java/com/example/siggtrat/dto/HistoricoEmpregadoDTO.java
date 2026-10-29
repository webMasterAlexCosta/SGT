package com.example.siggtrat.dto;

import java.time.LocalDateTime;

public record HistoricoEmpregadoDTO(
        Long id,
        EmpregadoReferenciaDTO empregado,
        LocalDateTime dataEvento,
        String descricao,
        UsuarioReferenciaDTO usuario,
        String hashIntegridade,
        long version
) {}
