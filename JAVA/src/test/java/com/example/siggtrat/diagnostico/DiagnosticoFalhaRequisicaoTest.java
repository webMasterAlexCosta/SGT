package com.example.siggtrat.diagnostico;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.example.siggtrat.configuracao.TratadorGlobalExcecoes;
import com.example.siggtrat.controlador.TipoContratacaoControlador;
import com.example.siggtrat.repositorio.TipoContratacaoRepositorio;
import com.example.siggtrat.servico.AcessoOrganizacionalServico;
import com.example.siggtrat.servico.TipoContratacaoServico;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DiagnosticoFalhaRequisicaoTest {
    private final List<Logger> loggers = List.of(TipoContratacaoServico.class,
            TipoContratacaoControlador.class, TratadorGlobalExcecoes.class).stream()
            .map(tipo -> (Logger) LoggerFactory.getLogger(tipo)).toList();
    private final ListAppender<ILoggingEvent> eventos = new ListAppender<>();

    @BeforeEach
    void capturar() {
        eventos.start();
        loggers.forEach(logger -> logger.addAppender(eventos));
        novaRequisicao();
    }

    @AfterEach
    void limpar() {
        loggers.forEach(logger -> logger.detachAppender(eventos));
        eventos.stop();
        RequestContextHolder.resetRequestAttributes();
        MDC.clear();
    }

    @Test
    void mesmaFalhaPropagadaPorServicoControllerETratadorGeraUmDiagnostico() {
        MDC.put("requestId", "req-diagnostico-unico");
        var erro = new ResponseStatusException(HttpStatus.CONFLICT, "SENTINELA_PRIVADA",
                new SQLException("PARAMETRO_PRIVADO", "40001"));
        var acesso = mock(AcessoOrganizacionalServico.class);
        when(acesso.usuarioAtual()).thenThrow(erro);
        var servico = proxy(new TipoContratacaoServico(mock(TipoContratacaoRepositorio.class), acesso));
        var controlador = proxy(new TipoContratacaoControlador(servico));

        assertSame(erro, assertThrows(ResponseStatusException.class, () -> controlador.buscarPorId(97L)));
        assertEquals(HttpStatus.CONFLICT, new TratadorGlobalExcecoes().tratarStatusResposta(erro).getStatusCode());

        assertEquals(1, eventos.list.size(), "A mesma falha não deve ser repetida por cada camada");
        String mensagem = eventos.list.getFirst().getFormattedMessage();
        assertTrue(mensagem.contains("req-diagnostico-unico"));
        assertTrue(mensagem.contains("40001"));
        assertFalse(mensagem.contains("SENTINELA_PRIVADA") || mensagem.contains("PARAMETRO_PRIVADO"));
        assertNull(eventos.list.getFirst().getThrowableProxy());
    }

    @Test
    void falhasDiferentesNaMesmaRequisicaoContinuamRegistradas() {
        DiagnosticoFalha.registrar(loggers.getFirst(), "processar", new IllegalStateException("original"));
        DiagnosticoFalha.registrar(loggers.getFirst(), "compensar", new IllegalStateException("compensação"));
        assertEquals(2, eventos.list.size());
    }

    @Test
    void mesmaExcecaoEmRequisicoesDistintasNaoPerdeDiagnostico() {
        var erro = new IllegalStateException("reutilizada");
        DiagnosticoFalha.registrar(loggers.getFirst(), "primeira", erro);
        novaRequisicao();
        DiagnosticoFalha.registrar(loggers.getFirst(), "segunda", erro);
        assertEquals(2, eventos.list.size());
    }

    @Test
    void camadaComLogDesabilitadoNaoOcultaDiagnosticoNaCamadaSeguinte() {
        Logger primeiro = loggers.getFirst();
        var nivelAnterior = primeiro.getLevel();
        var erro = new IllegalStateException("mesma falha");
        try {
            primeiro.setLevel(ch.qos.logback.classic.Level.OFF);
            DiagnosticoFalha.registrar(primeiro, "servico", erro);
            DiagnosticoFalha.registrar(loggers.get(1), "controlador", erro);
            assertEquals(1, eventos.list.size());
        } finally {
            primeiro.setLevel(nivelAnterior);
        }
    }

    @Test
    void operacoesSemRequisicaoHttpContinuamRegistradas() {
        RequestContextHolder.resetRequestAttributes();
        var erro = new IllegalStateException("operação de fundo");
        DiagnosticoFalha.registrar(loggers.getFirst(), "tentativa1", erro);
        DiagnosticoFalha.registrar(loggers.getFirst(), "tentativa2", erro);
        assertEquals(2, eventos.list.size());
    }

    private void novaRequisicao() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    private <T> T proxy(T alvo) {
        var fabrica = new AspectJProxyFactory(alvo);
        fabrica.setProxyTargetClass(true);
        fabrica.addAspect(new AspectoDiagnosticoOperacao());
        return fabrica.getProxy();
    }
}
