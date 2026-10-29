package com.example.siggtrat.dto;

import java.util.List;

public record CacheDiagnosticoDTO(
        String cacheManager,
        boolean redisConfigurado,
        List<String> caches,
        boolean conectado,
        String ping,
        String erro
) {}
