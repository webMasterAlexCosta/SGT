package com.example.siggtrat.configuracao;

import com.example.siggtrat.configuracao.propriedades.PropriedadesAuditoria;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;


@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class FiltroCacheCorpoRequisicao extends OncePerRequestFilter {

    private final int maxBytes;

    public FiltroCacheCorpoRequisicao(PropriedadesAuditoria properties) {
        this.maxBytes = properties.payload().maxBytes();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        HttpServletRequest requestToUse = request;
        if (!(request instanceof ContentCachingRequestWrapper)) {
            requestToUse = new ContentCachingRequestWrapper(request, Math.max(1024, maxBytes));
        }
        filterChain.doFilter(requestToUse, response);
    }
}
