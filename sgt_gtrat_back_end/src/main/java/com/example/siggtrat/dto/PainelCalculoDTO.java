package com.example.siggtrat.dto;

import java.time.LocalDateTime;

public record PainelCalculoDTO(
        Long id,
        String status,
        int progresso,
        boolean concluido,
        boolean desatualizado,
        String mensagem,
        LocalDateTime geradoEm,
        PainelGestaoDTO dados
) {}
