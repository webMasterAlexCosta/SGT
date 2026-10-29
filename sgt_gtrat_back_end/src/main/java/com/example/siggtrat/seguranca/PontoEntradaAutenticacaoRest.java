package com.example.siggtrat.seguranca;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

@Component
public class PontoEntradaAutenticacaoRest implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public PontoEntradaAutenticacaoRest(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {

        // Garante que o navegador não cacheie esta resposta de erro
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        String code = (String) request.getAttribute("AUTH_ERROR_CODE");
        String customError = (String) request.getAttribute("AUTH_ERROR_DETAIL");

        if (code == null || code.isBlank()) code = "UNAUTHORIZED";
        String message = (customError != null && !customError.isBlank())
                ? customError
                : "Acesso não autorizado. Faça autenticar novamente.";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", 401);
        body.put("error", "Unauthorized");
        body.put("code", code);
        body.put("message", message);
        body.put("action", "LOGOUT");
        body.put("path", request.getRequestURI());

        objectMapper.writeValue(response.getWriter(), body);
    }
}