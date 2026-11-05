package com.example.siggtrat.dto;

/**
 * Representação mínima de uma resposta JSON GET armazenada no Redis.
 *
 * O cache guarda os bytes já serializados pelo Spring MVC, evitando alterar os
 * DTOs ou as regras de negócio dos controllers. Headers sensíveis não são
 * persistidos; apenas status, content type e corpo são reutilizados.
 */
public record RespostaCacheConsultaHttp(
        int status,
        String contentType,
        byte[] body
) {
}
