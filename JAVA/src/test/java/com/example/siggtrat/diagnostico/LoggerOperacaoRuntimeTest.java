package com.example.siggtrat.diagnostico;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.example.siggtrat.controlador.TipoContratacaoControlador;
import com.example.siggtrat.servico.TipoContratacaoServico;
import com.example.siggtrat.servico.AcessoOrganizacionalServico;
import com.example.siggtrat.repositorio.TipoContratacaoRepositorio;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import java.sql.SQLException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class LoggerOperacaoRuntimeTest {
    @AfterEach void limpar(){MDC.clear();}
    private <T>T proxy(T alvo){var f=new AspectJProxyFactory(alvo);f.setProxyTargetClass(true);f.addAspect(new AspectoDiagnosticoOperacao());return f.getProxy();}
    @Test void controllerRegistraOperacaoConcluidaSemSerializarRetornoOuArgumentos(){
        var logger=(Logger)LoggerFactory.getLogger(TipoContratacaoControlador.class);var nivel=logger.getLevel();logger.setLevel(Level.DEBUG);
        var app=new ListAppender<ILoggingEvent>();app.start();logger.addAppender(app);
        try {
            MDC.put("requestId","req-logger-42");var service=mock(TipoContratacaoServico.class);
            var retorno=List.<com.example.siggtrat.dto.TipoContratacaoDTO>of();when(service.listar()).thenReturn(retorno);
            assertSame(retorno,proxy(new TipoContratacaoControlador(service)).listar());
            String logs=app.list.stream().map(ILoggingEvent::getFormattedMessage).reduce("",(a,b)->a+"\n"+b);
            assertTrue(logs.contains("listar")&&logs.contains("concluida")&&logs.contains("durationMs")&&logs.contains("req-logger-42"),logs);
        } finally {logger.detachAppender(app);logger.setLevel(nivel);app.stop();}
    }
    @Test void serviceRegistraCausaSqlELocalSemMensagemSenhaOuThrowableERelancaMesmoErro(){
        var logger=(Logger)LoggerFactory.getLogger(TipoContratacaoServico.class);var nivel=logger.getLevel();logger.setLevel(Level.DEBUG);
        var app=new ListAppender<ILoggingEvent>();app.start();logger.addAppender(app);
        try {
            MDC.put("requestId","req-logger-99");var acesso=mock(AcessoOrganizacionalServico.class);
            var erro=new IllegalStateException("password=SEGREDO_FICTICIO_NAO_LOGAR",new SQLException("JWT_FICTICIO_NAO_LOGAR","40001"));
            when(acesso.usuarioAtual()).thenThrow(erro);
            var servico=proxy(new TipoContratacaoServico(mock(TipoContratacaoRepositorio.class),acesso));
            assertSame(erro,assertThrows(IllegalStateException.class,()->servico.buscarPorId(97L)));
            String logs=app.list.stream().map(ILoggingEvent::getFormattedMessage).reduce("",(a,b)->a+"\n"+b);
            assertTrue(logs.contains("buscarPorId")&&logs.contains("IllegalStateException")&&logs.contains("40001")&&logs.contains("req-logger-99"),logs);
            assertFalse(logs.contains("SEGREDO_FICTICIO_NAO_LOGAR")||logs.contains("JWT_FICTICIO_NAO_LOGAR"));
            assertTrue(app.list.stream().allMatch(e->e.getThrowableProxy()==null),"Sem dump de mensagens privadas da cadeia Throwable");
        } finally {logger.detachAppender(app);logger.setLevel(nivel);app.stop();}
    }
}
