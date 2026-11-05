package com.example.siggtrat.configuracao;

import com.fasterxml.jackson.databind.ObjectMapper;


import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.Optional;

@ControllerAdvice
public class TratadorRespostaSucesso implements ResponseBodyAdvice<Object> {

    private final ObjectMapper objectMapper;

    public TratadorRespostaSucesso(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {

        Optional<HttpStatus> statusOpt = resolverStatus(response);
        HttpStatus status = statusOpt.isPresent() ? statusOpt.get() : HttpStatus.OK;

        if (status.is4xxClientError() || status.is5xxServerError()) {
            return body;
        }

        if (status == HttpStatus.NO_CONTENT) {
            return body;
        }

        if (body instanceof RespostaApi) {
            return body;
        }

        if (body != null && ehBinarioOuFluxo(body)) {
            return body;
        }

        String path = resolverCaminho(request);
        String requestId = resolverIdRequisicao(request);
        Object meta = resolverMetadados(body);

        RespostaApi<Object> envelope = criarEnvelope(status, body, path, requestId, meta);

        if (StringHttpMessageConverter.class.isAssignableFrom(selectedConverterType)
                || MediaType.TEXT_PLAIN.includes(selectedContentType)) {
            try {
                response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                return objectMapper.writeValueAsString(envelope);
            } catch (Exception e) {
                return body;
            }
        }

        return envelope;
    }

    private RespostaApi<Object> criarEnvelope(HttpStatus status, Object data, String path, String requestId, Object meta) {
        String msg;
        if (status == HttpStatus.CREATED) msg = "Created";
        else msg = "OK";
        return new RespostaApi<>(status.value(), msg, data, path, requestId, meta);
    }

    private Optional<HttpStatus> resolverStatus(ServerHttpResponse response) {
        if (response instanceof ServletServerHttpResponse) {
            ServletServerHttpResponse servletResponse = (ServletServerHttpResponse) response;
            int sc = servletResponse.getServletResponse().getStatus();
            try {
                return Optional.of(HttpStatus.valueOf(sc));
            } catch (Exception ignored) {
            }
        }
        return Optional.empty();
    }

    private String resolverCaminho(ServerHttpRequest request) {
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            return servletRequest.getServletRequest().getRequestURI();
        }
        return request.getURI().getPath();
    }

    private String resolverIdRequisicao(ServerHttpRequest request) {
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            HttpServletRequest req = servletRequest.getServletRequest();

            Object attr = req.getAttribute(FiltroIdentificadorRequisicao.REQUEST_ID_ATTR);
            if (attr != null) return attr.toString();

            String header = req.getHeader(FiltroIdentificadorRequisicao.REQUEST_ID_HEADER);
            if (header != null && !header.isEmpty()) return header.trim();
        }

        String hdr = request.getHeaders().getFirst(FiltroIdentificadorRequisicao.REQUEST_ID_HEADER);
        if (hdr != null && !hdr.isEmpty()) return hdr.trim();
        return null;
    }

    private Object resolverMetadados(Object body) {
        if (body instanceof Page) {
            return MetadadosPagina.from((Page<?>) body);
        }
        return null;
    }

    private boolean ehBinarioOuFluxo(Object body) {
        if (body instanceof byte[]) return true;

        String cn = body.getClass().getName();
        return cn.equals("org.springframework.core.io.Resource")
                || cn.startsWith("org.springframework.core.io")
                || cn.equals("org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody")
                || cn.contains("InputStream")
                || cn.contains("Resource");
    }
}
