package com.example.siggtrat.servico;

import com.example.siggtrat.configuracao.propriedades.PropriedadesAuditoria;
import com.example.siggtrat.configuracao.propriedades.PropriedadesRegistroAuditoria;
import com.example.siggtrat.entidade.RegistroAuditoria;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.repositorio.RegistroAuditoriaRepositorio;
import com.example.siggtrat.repositorio.UsuarioAplicacaoRepositorio;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuditoriaCommitRegressaoTest {
    @TempDir Path temporario;
    private final List<RegistroAuditoria> persistidos = new ArrayList<>();
    private final List<Connection> conexoesPersistencia = new ArrayList<>();
    private final DataSource fonte = mock(DataSource.class, invocation -> {
        if (invocation.getMethod().getName().equals("getConnection")) {
            Connection conexao = mock(Connection.class);
            when(conexao.getAutoCommit()).thenReturn(true);
            return conexao;
        }
        return Answers.RETURNS_DEFAULTS.answer(invocation);
    });
    private final DataSourceTransactionManager gerenciador = new DataSourceTransactionManager(fonte);
    private final TransactionTemplate transacao = new TransactionTemplate(gerenciador);

    @Test
    void operacaoSomenteApareceNaAuditoriaDepoisDoCommit() {
        Contexto contexto = criarContexto();
        transacao.executeWithoutResult(status -> {
            contexto.servico.registrarAcaoComoUsuario("CONFIRMAR", "Contrato", 1L, "Contrato", Map.of(), ator());
            contexto.fila.descarregar();
            assertTrue(persistidos.isEmpty(), "Auditoria não pode antecipar confirmação ainda reversível.");
        });
        contexto.fila.descarregar();
        assertEquals(1, persistidos.size());
        assertEquals("CONFIRMAR", persistidos.get(0).getAction());
        assertEquals("gestor.teste", persistidos.get(0).getUsername());
    }

    @Test
    void rollbackNaoPublicaAuditoriaDeAcaoQueNaoAconteceu() {
        Contexto contexto = criarContexto();
        transacao.executeWithoutResult(status -> {
            contexto.servico.registrarAcao("CONFIRMAR", "Contrato", 1L, "Contrato", Map.of());
            status.setRollbackOnly();
        });
        contexto.fila.descarregar();
        assertTrue(persistidos.isEmpty());
    }

    @Test
    void tentativaRevertidaSeguidaDeSucessoRegistraSomenteOSucesso() {
        Contexto contexto = criarContexto();
        assertThrows(IllegalStateException.class, () -> transacao.executeWithoutResult(status -> {
            contexto.servico.registrarAcao("EMITIR", "Relatorio", 1L, "Tentativa revertida", Map.of());
            throw new IllegalStateException("concorrência");
        }));
        transacao.executeWithoutResult(status -> contexto.servico.registrarAcao(
                "EMITIR", "Relatorio", 2L, "Emissão confirmada", Map.of()));
        contexto.fila.descarregar();
        assertEquals(1, persistidos.size());
        assertEquals(2L, persistidos.get(0).getEntityId());
    }

    @Test
    void operacaoSemTransacaoContinuaPublicandoAuditoria() {
        Contexto contexto = criarContexto();
        contexto.servico.registrarAcao("CONSULTAR", "Pessoa", 7L, "Pessoa", Map.of());
        contexto.fila.descarregar();
        assertEquals(1, persistidos.size());
    }

    @Test
    void payloadJsonTruncadoNaoVazaCredencialNoRegistro() throws Exception {
        Contexto contexto = criarContexto();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/teste");
        request.setContentType("application/json");
        request.setContent("{\"password\":\"valor-sensivel-de-teste\",\"resto\":".getBytes(StandardCharsets.UTF_8));
        ContentCachingRequestWrapper wrapper = new ContentCachingRequestWrapper(request, 4096);
        wrapper.getInputStream().readAllBytes();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(wrapper));
        try {
            contexto.servico.registrarAcao("ALTERAR", "Pessoa", 1L, "Pessoa", Map.of());
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
        contexto.fila.descarregar();
        assertEquals(1, persistidos.size());
        assertFalse(persistidos.get(0).getPayloadJson().contains("valor-sensivel-de-teste"));
    }

    @Test
    void contingenciaPersisteEventoEstruturadoQuandoFilaEBancoEstaoIndisponiveis() throws Exception {
        Contexto contexto = criarContexto();
        doThrow(new IllegalStateException("banco indisponível")).when(contexto.repositorio).saveAndFlush(any());
        for (int i = 0; i < 1000; i++) {
            RegistroAuditoria evento = new RegistroAuditoria();
            evento.setAction("PREENCHER");
            contexto.fila.enfileirarOuPersistir(evento);
        }
        RegistroAuditoria evento = new RegistroAuditoria();
        evento.setAction("CONFIRMAR");
        evento.setEntityType("Contrato");
        evento.setEntityId(42L);
        evento.setUsername("gestor.teste");
        contexto.fila.enfileirarOuPersistir(evento);
        Path arquivo = temporario.resolve("auditoria.jsonl");
        assertTrue(Files.exists(arquivo), "Contingência deve preservar evento quando a fila está cheia.");
        var json = new ObjectMapper().readTree(Files.readAllLines(arquivo).get(0));
        assertEquals("CONFIRMAR", json.get("action").asText());
        assertEquals(42L, json.get("entityId").asLong());
        assertEquals("gestor.teste", json.get("username").asText());
    }

    @Test
    void fallbackAposCommitConfirmaAuditoriaEmTransacaoIndependente() throws Exception {
        Contexto contexto = criarContexto();
        for (int i = 0; i < 1000; i++) {
            RegistroAuditoria evento = new RegistroAuditoria();
            evento.setAction("PREENCHER");
            contexto.fila.enfileirarOuPersistir(evento);
        }
        AtomicReference<Connection> conexaoNegocio = new AtomicReference<>();
        transacao.executeWithoutResult(status -> {
            conexaoNegocio.set(DataSourceUtils.getConnection(fonte));
            contexto.servico.registrarAcao("CONFIRMAR", "Contrato", 42L, "Contrato", Map.of());
            assertTrue(persistidos.isEmpty());
        });
        assertEquals(1, persistidos.size());
        assertEquals(42L, persistidos.get(0).getEntityId());
        assertNotSame(conexaoNegocio.get(), conexoesPersistencia.get(0));
        verify(conexaoNegocio.get()).commit();
        verify(conexoesPersistencia.get(0)).commit();
    }

    private Contexto criarContexto() {
        RegistroAuditoriaRepositorio repositorio = mock(RegistroAuditoriaRepositorio.class, invocation -> {
            if (invocation.getMethod().getName().startsWith("saveAll")) {
                Iterable<RegistroAuditoria> lote = invocation.getArgument(0);
                lote.forEach(persistidos::add);
                return new ArrayList<>(persistidos);
            }
            if (invocation.getMethod().getName().equals("saveAndFlush")) {
                RegistroAuditoria evento = invocation.getArgument(0);
                persistidos.add(evento);
                conexoesPersistencia.add(DataSourceUtils.getConnection(fonte));
                return evento;
            }
            return Answers.RETURNS_DEFAULTS.answer(invocation);
        });
        PropriedadesAuditoria propriedades = new PropriedadesAuditoria("segredo-teste", temporario.resolve("auditoria.jsonl"),
                new PropriedadesAuditoria.ConteudoRequisicao(4096), "(?i).*(password|senha|token|secret).*" );
        ProxyFactory fabrica = new ProxyFactory(new PersistenciaRegistroAuditoriaServico(repositorio));
        fabrica.setProxyTargetClass(true);
        fabrica.addAdvice(new TransactionInterceptor(gerenciador, new AnnotationTransactionAttributeSource()));
        PersistenciaRegistroAuditoriaServico persistencia = (PersistenciaRegistroAuditoriaServico) fabrica.getProxy();
        FilaRegistroAuditoria fila = new FilaRegistroAuditoria(persistencia,
                new PropriedadesRegistroAuditoria(new PropriedadesRegistroAuditoria.Fila(1000),
                        new PropriedadesRegistroAuditoria.Descarga(50, 1000, 2, 100)), propriedades);
        RegistroAuditoriaServico servico = new RegistroAuditoriaServico(persistencia,
                mock(UsuarioAplicacaoRepositorio.class), new ObjectMapper(), fila, propriedades);
        return new Contexto(servico, fila, repositorio);
    }

    private UsuarioAplicacao ator() {
        UsuarioAplicacao usuario = new UsuarioAplicacao();
        usuario.setId(1L);
        usuario.setUsername("gestor.teste");
        return usuario;
    }

    private record Contexto(RegistroAuditoriaServico servico, FilaRegistroAuditoria fila,
                            RegistroAuditoriaRepositorio repositorio) { }

}
