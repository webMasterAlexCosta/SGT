package com.example.siggtrat.diagnostico;

import com.example.siggtrat.configuracao.FiltroIdentificadorRequisicao;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Contrato JSON também para erros escritos antes do MVC; somente valores públicos deliberados. */
public final class ErroHttpCorrelacionado {
    private static final ObjectMapper MAPPER=new ObjectMapper();
    private ErroHttpCorrelacionado() { }

    public static Map<String,Object> corpo(HttpServletRequest request,HttpServletResponse response,
            int status,String codigo,String mensagem,String acao) {
        Object atributo=request.getAttribute(FiltroIdentificadorRequisicao.REQUEST_ID_ATTR);
        String id=atributo instanceof String texto && texto.matches("[A-Za-z0-9][A-Za-z0-9._:-]{0,63}")
                ? texto : UUID.randomUUID().toString();
        request.setAttribute(FiltroIdentificadorRequisicao.REQUEST_ID_ATTR,id);
        response.setHeader(FiltroIdentificadorRequisicao.REQUEST_ID_HEADER,id);
        var tipo=HttpStatus.resolve(status);
        Map<String,Object> body=new LinkedHashMap<>();
        body.put("timestamp",Instant.now().toString());body.put("status",status);
        body.put("error",tipo==null ? "HTTP Error" : tipo.getReasonPhrase());
        body.put("code",codigo);body.put("message",mensagem);body.put("requestId",id);
        body.put("path",request.getRequestURI());
        if(acao!=null)body.put("action",acao);
        return body;
    }
    public static void escrever(HttpServletRequest request,HttpServletResponse response,
            int status,String codigo,String mensagem,String acao) throws IOException {
        response.setStatus(status);response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        MAPPER.writeValue(response.getWriter(),corpo(request,response,status,codigo,mensagem,acao));
    }
}
