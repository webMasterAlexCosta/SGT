package com.example.siggtrat.diagnostico;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.example.siggtrat.configuracao.propriedades.*;
import com.example.siggtrat.entidade.RegistroAuditoria;
import com.example.siggtrat.repositorio.EmpregadoRepositorio;
import com.example.siggtrat.repositorio.FechamentoFrequenciaItemRepositorio;
import com.example.siggtrat.servico.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FalhasDuplasDiagnosticoTest {
    @TempDir Path temporario;
    private static final String PRIVADO="SENTINELA_FICTICIA_PRIVADA";
    private static ListAppender<ILoggingEvent> capturar(Logger logger) {
        var app=new ListAppender<ILoggingEvent>();app.start();logger.addAppender(app);return app;
    }
    private static void seguro(ListAppender<ILoggingEvent> app) {
        assertFalse(app.list.isEmpty());
        assertTrue(app.list.stream().allMatch(e->e.getThrowableProxy()==null&&!e.getFormattedMessage().contains(PRIVADO)),"Eventos não podem carregar Throwable/mensagem privada");
    }
    @Test void falhaDoProcessamentoERegistroDoErroMantemIdSemThrowable() {
        var logger=(Logger)LoggerFactory.getLogger(FechamentoFrequenciaProcessador.class);var app=capturar(logger);
        try {
            var estado=mock(FechamentoFrequenciaEstadoServico.class);
            when(estado.reivindicar(71L)).thenThrow(new IllegalStateException(PRIVADO+"-original"));
            doThrow(new IllegalStateException(PRIVADO+"-registro")).when(estado).falhar(eq(71L),any());
            var p=new FechamentoFrequenciaProcessador(estado,mock(FechamentoFrequenciaItemRepositorio.class),mock(EmpregadoRepositorio.class),
                    mock(FrequenciaServico.class),mock(FechamentoFrequenciaServico.class),new PropriedadesFechamentoFrequencia(1,10,1,1,10,1000,60),
                    mock(InvalidacaoCacheServico.class),mock(ElegibilidadeFechamentoFrequenciaServico.class));
            p.processar(71L);seguro(app);
            assertTrue(app.list.stream().anyMatch(e->e.getFormattedMessage().contains("71")));
        } finally {logger.detachAppender(app);app.stop();}
    }
    @Test void falhaBancoEFallbackDaAuditoriaNaoRegistraThrowableBruto() throws Exception {
        var logger=(Logger)LoggerFactory.getLogger(FilaRegistroAuditoria.class);var app=capturar(logger);
        try {
            var persistencia=mock(PersistenciaRegistroAuditoriaServico.class);
            Path destinoInvalido=Files.createDirectory(temporario.resolve(PRIVADO));
            var fila=new FilaRegistroAuditoria(persistencia,
                    new PropriedadesRegistroAuditoria(new PropriedadesRegistroAuditoria.Fila(1000),new PropriedadesRegistroAuditoria.Descarga(50,1000,1,1000)),
                    new PropriedadesAuditoria("segredo-apenas-ficticio",destinoInvalido,new PropriedadesAuditoria.ConteudoRequisicao(1024),"password"));
            for(int i=0;i<1000;i++)fila.enfileirarOuPersistir(new RegistroAuditoria());
            doAnswer(inv->{for(int i=0;i<50;i++)fila.enfileirarOuPersistir(new RegistroAuditoria());throw new IllegalStateException(PRIVADO+"-banco");})
                    .when(persistencia).salvarLote(anyList());
            fila.descarregar();seguro(app);
        } finally {logger.detachAppender(app);app.stop();}
    }
}
