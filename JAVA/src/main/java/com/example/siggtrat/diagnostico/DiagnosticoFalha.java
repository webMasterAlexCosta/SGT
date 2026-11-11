package com.example.siggtrat.diagnostico;

import org.slf4j.Logger;
import org.slf4j.MDC;
import org.springframework.web.server.ResponseStatusException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Set;

/** Diagnóstico de código/SQLState, sem mensagens ou dumps que possam conter dados. */
public final class DiagnosticoFalha {
    private static final String FALHAS_DA_REQUISICAO = DiagnosticoFalha.class.getName() + ".falhas";

    private static final class FalhasDaRequisicao {
        private final Set<Throwable> erros = Collections.newSetFromMap(new IdentityHashMap<>());
    }

    private DiagnosticoFalha() { }

    public static String resumir(Throwable erro) {
        if (erro == null) return "tipo=ausente";
        Set<Throwable> visitados = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<String> tipos = new LinkedHashSet<>();
        Set<String> estados = new LinkedHashSet<>();
        Set<String> locais = new LinkedHashSet<>();
        for (Throwable atual=erro; atual!=null && visitados.size()<12 && visitados.add(atual); atual=atual.getCause()) {
            tipos.add(atual.getClass().getSimpleName());
            if (atual instanceof SQLException sql && sql.getSQLState()!=null && sql.getSQLState().matches("[A-Z0-9]{5}")) {
                estados.add(sql.getSQLState());
            }
            for (StackTraceElement frame: atual.getStackTrace()) {
                if (locais.size()>=8) break;
                if (frame.getClassName().startsWith("com.example.siggtrat.")) locais.add(frame.toString());
            }
        }
        return "tipos=" + tipos + " sqlStates=" + estados + " locais=" + locais;
    }

    public static int status(Throwable erro) {
        if (erro instanceof ResponseStatusException resposta) return resposta.getStatusCode().value();
        if (erro instanceof org.springframework.web.ErrorResponse resposta) return resposta.getStatusCode().value();
        if (erro instanceof com.example.siggtrat.excecao.ExcecaoSenhaAusente) return 428;
        if (erro instanceof com.example.siggtrat.excecao.ExcecaoSenhaInvalida
                || erro instanceof org.springframework.security.access.AccessDeniedException) return 403;
        if (erro instanceof com.example.siggtrat.excecao.ExcecaoUsuarioNaoAutenticado) return 401;
        if (erro instanceof org.springframework.beans.ConversionNotSupportedException) return 500;
        if (erro instanceof org.springframework.beans.TypeMismatchException) return 400;
        if (erro instanceof IllegalArgumentException || erro instanceof jakarta.validation.ConstraintViolationException
                || erro instanceof org.springframework.http.converter.HttpMessageNotReadableException
                || erro instanceof org.springframework.web.bind.MethodArgumentNotValidException) return 400;
        if (erro instanceof org.springframework.dao.DataIntegrityViolationException && resumir(erro).contains("23502")) return 400;
        if (erro instanceof org.springframework.security.core.AuthenticationException) return 401;
        if (erro instanceof com.example.siggtrat.excecao.ExcecaoConflitoDados
                || erro instanceof org.springframework.dao.DataIntegrityViolationException
                || erro instanceof org.springframework.dao.OptimisticLockingFailureException
                || erro instanceof org.springframework.dao.PessimisticLockingFailureException) return 409;
        return 500;
    }

    public static void registrar(Logger logger, String operacao, Throwable erro) {
        int status = status(erro);
        // Uma camada com log desabilitado não deve impedir o diagnóstico na próxima.
        if (status >= 500 ? !logger.isErrorEnabled() : !logger.isWarnEnabled()) return;
        if (!primeiraOcorrenciaNaRequisicao(erro)) return;
        String resumo = resumir(erro);
        if (status>=500) logger.error("operacao={} requestId={} status={} falha={}", operacao, MDC.get("requestId"), status, resumo);
        else logger.warn("operacao={} requestId={} status={} falha={}", operacao, MDC.get("requestId"), status, resumo);
    }

    private static boolean primeiraOcorrenciaNaRequisicao(Throwable erro) {
        var atributos = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        if (!(atributos instanceof org.springframework.web.context.request.ServletRequestAttributes servlet)
                || erro == null) return true;
        var requisicao = servlet.getRequest();
        // O estado vive somente nesta requisição; não retém exceções entre usuários ou tentativas.
        synchronized (requisicao) {
            var existente = requisicao.getAttribute(FALHAS_DA_REQUISICAO);
            FalhasDaRequisicao falhas;
            if (existente instanceof FalhasDaRequisicao registradas) {
                falhas = registradas;
            } else {
                falhas = new FalhasDaRequisicao();
                requisicao.setAttribute(FALHAS_DA_REQUISICAO, falhas);
            }
            return falhas.erros.add(erro);
        }
    }

    public static String mensagemPublica(String texto, String padrao) {
        if (texto == null || texto.isBlank()) return padrao;
        String limpa = texto.replaceAll("[\\p{Cntrl}]", " ").trim();
        return limpa.length()>400 ? limpa.substring(0,400) : limpa;
    }
}
