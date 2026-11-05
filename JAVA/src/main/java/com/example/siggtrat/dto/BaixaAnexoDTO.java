package com.example.siggtrat.dto;

import org.springframework.core.io.Resource;

public record BaixaAnexoDTO(
        Resource resource,
        String contentType,
        String disposition,
        String filename
) {}
