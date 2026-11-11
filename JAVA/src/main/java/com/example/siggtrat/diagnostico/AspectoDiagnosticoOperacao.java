package com.example.siggtrat.diagnostico;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 200)
public class AspectoDiagnosticoOperacao {
    @Around("execution(public * com.example.siggtrat..*(..)) && this(com.example.siggtrat.diagnostico.DiagnosticoOperacao)"
            + " && !execution(* com.example.siggtrat..*.loggerDiagnostico(..))")
    public Object registrar(ProceedingJoinPoint chamada) throws Throwable {
        var alvo = (DiagnosticoOperacao) chamada.getTarget();
        var logger = alvo.loggerDiagnostico();
        String operacao = AopUtils.getTargetClass(chamada.getTarget()).getSimpleName() + "." + chamada.getSignature().getName();
        long inicio = System.nanoTime();
        logger.debug("operacao={} requestId={} fase=iniciada", operacao, MDC.get("requestId"));
        try {
            Object resultado = chamada.proceed();
            long duracao = (System.nanoTime()-inicio)/1_000_000;
            if (resultado instanceof ResponseEntity<?> resposta && resposta.getStatusCode().isError()) {
                if (resposta.getStatusCode().is5xxServerError()) logger.error("operacao={} requestId={} status={} durationMs={}", operacao, MDC.get("requestId"), resposta.getStatusCode().value(), duracao);
                else logger.warn("operacao={} requestId={} status={} durationMs={}", operacao, MDC.get("requestId"), resposta.getStatusCode().value(), duracao);
            } else logger.debug("operacao={} requestId={} fase=concluida durationMs={}", operacao, MDC.get("requestId"), duracao);
            return resultado;
        } catch (Throwable erro) {
            DiagnosticoFalha.registrar(logger, operacao, erro);
            throw erro;
        }
    }
}
