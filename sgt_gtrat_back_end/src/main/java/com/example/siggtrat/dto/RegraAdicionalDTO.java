package com.example.siggtrat.dto;

import java.math.BigDecimal;

public record RegraAdicionalDTO(
        Long id,
        String tipo,
        String inicio,
        String fim,
        BigDecimal percentual,
        Boolean ativo,
        Integer ordem
) {}