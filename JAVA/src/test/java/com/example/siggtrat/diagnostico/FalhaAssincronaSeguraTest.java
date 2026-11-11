package com.example.siggtrat.diagnostico;

import com.example.siggtrat.entidade.FechamentoFrequencia;
import com.example.siggtrat.entidade.PainelCalculo;
import com.example.siggtrat.entidade.PainelCalculoStatus;
import com.example.siggtrat.repositorio.FechamentoFrequenciaRepositorio;
import com.example.siggtrat.repositorio.PainelCalculoRepositorio;
import com.example.siggtrat.servico.FechamentoFrequenciaEstadoServico;
import com.example.siggtrat.servico.PainelCalculoEstadoServico;
import com.example.siggtrat.servico.RevisaoFontesPainelServico;
import com.example.siggtrat.dto.FechamentoFrequenciaDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FalhaAssincronaSeguraTest {
    private static final String SEGREDO="SEGREDO_FICTICIO_ERRO_ASYNC";
    @Test void fechamentoNaoArmazenaMensagemTecnicaComoMensagemPublica() {
        var repo=mock(FechamentoFrequenciaRepositorio.class);var f=new FechamentoFrequencia();f.setId(71L);
        when(repo.buscarPorIdParaAtualizacao(71L)).thenReturn(Optional.of(f));
        new FechamentoFrequenciaEstadoServico(repo).falhar(71L,new IllegalStateException("password="+SEGREDO));
        assertFalse(f.getMensagem().contains(SEGREDO));assertFalse(f.getDetalheErro().contains(SEGREDO));
        assertTrue(f.getMensagem().contains("71"),"Identificador útil para suporte");
    }
    @Test void painelNaoPublicaMensagemOuStackPrivados() {
        var repo=mock(PainelCalculoRepositorio.class);var p=new PainelCalculo();p.setId(72L);p.setStatus(PainelCalculoStatus.PROCESSANDO);p.setExecucaoToken("execucao-ficticia");
        when(repo.buscarPorIdParaAtualizacao(72L)).thenReturn(Optional.of(p));
        var descritor=new PainelCalculoEstadoServico.Descritor(72L,LocalDate.of(2026,1,1),1,"execucao-ficticia","revisao",null);
        new PainelCalculoEstadoServico(repo,mock(RevisaoFontesPainelServico.class)).falhar(descritor,new IllegalStateException("jdbc:interno "+SEGREDO));
        assertFalse(p.getMensagem().contains(SEGREDO));assertFalse(p.getDetalheErro().contains(SEGREDO));
    }
    @Test void detalheInternoDoFechamentoNaoIntegraJsonPublico() throws Exception {
        var componentes=FechamentoFrequenciaDTO.class.getRecordComponents();var args=new Object[componentes.length];var tipos=new Class<?>[args.length];
        for(int i=0;i<args.length;i++) {tipos[i]=componentes[i].getType();if(tipos[i]==long.class)args[i]=0L;else if(tipos[i]==int.class)args[i]=0;else if(tipos[i]==boolean.class)args[i]=false;if(componentes[i].getName().equals("detalheErro"))args[i]=SEGREDO;}
        var dto=FechamentoFrequenciaDTO.class.getDeclaredConstructor(tipos).newInstance(args);
        String json=new ObjectMapper().findAndRegisterModules().writeValueAsString(dto);
        assertFalse(json.contains(SEGREDO)||json.contains("detalheErro"));
    }
    @Test void logDoFechamentoConservaReferenciaEQuantidadeSemCausaPrivada() {
        var logger=(ch.qos.logback.classic.Logger)org.slf4j.LoggerFactory.getLogger(FechamentoFrequenciaEstadoServico.class);
        var app=new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();app.start();logger.addAppender(app);
        try {
            var repo=mock(FechamentoFrequenciaRepositorio.class);var f=new FechamentoFrequencia();f.setId(71L);f.setTotalProcessado(13L);
            when(repo.buscarPorIdParaAtualizacao(71L)).thenReturn(Optional.of(f));
            new FechamentoFrequenciaEstadoServico(repo).falhar(71L,new IllegalStateException(SEGREDO));
            assertTrue(app.list.stream().anyMatch(e->e.getFormattedMessage().contains("FECHAMENTO-71")&&e.getFormattedMessage().contains("processados=13")));
            assertTrue(app.list.stream().allMatch(e->e.getThrowableProxy()==null&&!e.getFormattedMessage().contains(SEGREDO)));
        } finally {logger.detachAppender(app);app.stop();}
    }
    @Test void desserializacaoDoPainelConservaReferenciaSemPayload() {
        var logger=(ch.qos.logback.classic.Logger)org.slf4j.LoggerFactory.getLogger(com.example.siggtrat.servico.PainelCalculoServico.class);
        var app=new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();app.start();logger.addAppender(app);
        try {
            var servico=mock(com.example.siggtrat.servico.PainelCalculoServico.class,org.mockito.Mockito.CALLS_REAL_METHODS);
            org.springframework.test.util.ReflectionTestUtils.setField(servico,"objectMapper",new ObjectMapper());
            assertNull(org.springframework.test.util.ReflectionTestUtils.invokeMethod(servico,"desserializar","{"+SEGREDO,72L));
            assertTrue(app.list.stream().anyMatch(e->e.getFormattedMessage().contains("PAINEL-72")));
            assertTrue(app.list.stream().allMatch(e->e.getThrowableProxy()==null&&!e.getFormattedMessage().contains(SEGREDO)));
        } finally {logger.detachAppender(app);app.stop();}
    }
}
