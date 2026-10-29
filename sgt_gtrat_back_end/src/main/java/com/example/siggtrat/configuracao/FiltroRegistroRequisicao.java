package com.example.siggtrat.configuracao;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class FiltroRegistroRequisicao extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(FiltroRegistroRequisicao.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        long start = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long tookMs = (System.nanoTime() - start) / 1_000_000;
            int status = response.getStatus();

            Object reqUser = request.getAttribute("authenticatedUser");
            String user;
            if (reqUser instanceof String s && !s.isBlank()) {
                user = s;
            } else {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                user = (auth != null) ? auth.getName() : "anonymous";
            }

            if (!"OPTIONS".equalsIgnoreCase(request.getMethod())) {
                log.info("HTTP {} {} -> {} ({}ms) user={}",
                        request.getMethod(), request.getRequestURI(), status, tookMs, user);
            }
        }
    }
}
