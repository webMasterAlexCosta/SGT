package com.example.siggtrat.configuracao;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;


@Component
@Order(Ordered.HIGHEST_PRECEDENCE) // Executa ANTES de tudo
public class FiltroIdentificadorRequisicao extends OncePerRequestFilter {

    // Constantes públicas (para uso em outros filtros/logs se necessário)
    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String REQUEST_ID_ATTR = "requestId"; // Renomeado para evitar conflito de nomes comuns

    // Headers vindos do Frontend (http.js)
    private static final String CLIENT_HEADER = "X-Client";
    private static final String CLIENT_VERSION_HEADER = "X-Client-Version";
    private static final String TIMEZONE_HEADER = "X-Timezone";

    // Chaves do MDC (Logback)
    private static final String MDC_KEY = "requestId";
    private static final String MDC_CLIENT_KEY = "clientOrigin";
    private static final String MDC_CLIENT_VERSION_KEY = "clientVersion";
    private static final String MDC_USER_IP = "userIp";
    private static final String MDC_USER_TIMEZONE = "userTimezone";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String requestId = request.getHeader(REQUEST_ID_HEADER);
            if (!StringUtils.hasText(requestId)) {
                requestId = UUID.randomUUID().toString();
            }

            MDC.put(MDC_KEY, requestId);
            request.setAttribute(REQUEST_ID_ATTR, requestId);

            String clientOrigin = request.getHeader(CLIENT_HEADER);
            MDC.put(MDC_CLIENT_KEY, StringUtils.hasText(clientOrigin) ? clientOrigin : "unknown");

            String clientVersion = request.getHeader(CLIENT_VERSION_HEADER);
            MDC.put(MDC_CLIENT_VERSION_KEY, StringUtils.hasText(clientVersion) ? clientVersion : "unknown");

            String remoteAddr = request.getHeader("X-Forwarded-For");
            if (!StringUtils.hasText(remoteAddr)) {
                remoteAddr = request.getRemoteAddr();
            }
            MDC.put(MDC_USER_IP, remoteAddr);

            String timezone = request.getHeader(TIMEZONE_HEADER);
            MDC.put(MDC_USER_TIMEZONE, StringUtils.hasText(timezone) ? timezone : "UTC");

            response.setHeader(REQUEST_ID_HEADER, requestId);

            filterChain.doFilter(request, response);

        } finally {
            MDC.clear();
        }
    }
}