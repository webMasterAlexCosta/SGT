package com.example.siggtrat.configuracao;

import com.example.siggtrat.dto.MensagemApiDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Acrescenta correlação aos erros JSON sem envolver respostas de sucesso. */
@RestControllerAdvice
public class RespostaErroCorrelacionadaAdvice implements ResponseBodyAdvice<Object> {
    private final ObjectMapper mapper;
    public RespostaErroCorrelacionadaAdvice(ObjectMapper mapper) { this.mapper=mapper; }
    @Override public boolean supports(MethodParameter tipo, Class<? extends HttpMessageConverter<?>> conversor) { return true; }

    @Override public Object beforeBodyWrite(Object body, MethodParameter tipo, MediaType media,
            Class<? extends HttpMessageConverter<?>> conversor, ServerHttpRequest requisicao, ServerHttpResponse resposta) {
        if (!(resposta instanceof ServletServerHttpResponse servlet) || servlet.getServletResponse().getStatus()<400) return body;
        int status=servlet.getServletResponse().getStatus();
        String id=null;
        if (requisicao instanceof ServletServerHttpRequest req) {
            Object atributo=req.getServletRequest().getAttribute(FiltroIdentificadorRequisicao.REQUEST_ID_ATTR);
            if (atributo instanceof String texto && texto.matches("[A-Za-z0-9][A-Za-z0-9._:-]{0,63}")) id=texto;
        }
        if (id==null) id=UUID.randomUUID().toString();
        // O filtro já escreveu o header no servlet. O wrapper Spring usa addHeader
        // ao serializar: gravá-lo também no wrapper duplicaria o valor no HTTP real.
        servlet.getServletResponse().setHeader(FiltroIdentificadorRequisicao.REQUEST_ID_HEADER,id);
        var erro=new LinkedHashMap<String,Object>();
        if (body instanceof Map<?,?> mapa) mapa.forEach((k,v)->{if(k instanceof String chave) erro.put(chave,v);});
        else if (body instanceof MensagemApiDTO mensagem) {
            if(mensagem.message()!=null) erro.put("message",mensagem.message());
            if(mensagem.error()!=null) {erro.put("error",mensagem.error());erro.put("message",mensagem.error());}
        } else return body;
        erro.put("requestId",id);
        erro.putIfAbsent("status",status);
        erro.putIfAbsent("code","HTTP_"+status);
        if (org.springframework.http.converter.StringHttpMessageConverter.class.isAssignableFrom(conversor)) {
            try { resposta.getHeaders().setContentType(MediaType.APPLICATION_JSON);return mapper.writeValueAsString(erro); }
            catch(JsonProcessingException ex) {throw new IllegalStateException("Falha ao serializar erro correlacionado",ex);}
        }
        return erro;
    }
}
