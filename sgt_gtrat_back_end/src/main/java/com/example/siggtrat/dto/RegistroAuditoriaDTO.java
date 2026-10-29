package com.example.siggtrat.dto;

import java.time.Instant;

public record RegistroAuditoriaDTO(
        Long id,
        Instant createdAt,
        String requestId,
        Long userId,
        String username,
        String action,
        String entityType,
        Long entityId,
        String entityLabel,
        String ip,
        String userAgent,
        String httpMethod,
        String path,
        String queryString,
        String payloadJson,
        Boolean payloadTruncated,
        String detailsJson
) {}
