package com.example.siggtrat.seguranca;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class TratadorAcessoNegadoRest implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {

        com.example.siggtrat.diagnostico.ErroHttpCorrelacionado.escrever(request,response,403,
                "ACCESS_DENIED","Você não tem permissão para realizar esta ação.",null);
    }
}