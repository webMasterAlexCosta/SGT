package com.example.siggtrat.configuracao;

import com.example.siggtrat.entidade.ConfiguracaoSistema;
import com.example.siggtrat.regra.MaterialHashEventos;
import com.example.siggtrat.repositorio.ConfiguracaoSistemaRepositorio;
import com.example.siggtrat.servico.ConfiguracaoSistemaServico;
import com.example.siggtrat.configuracao.propriedades.PropriedadesSistema;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.boot.web.server.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.boot.web.server.servlet.ServletWebServerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Ramos de decisão e ordem do refresh reais; repositório/EM são doubles, sem rede nem banco. */
class GuardaFormatoHashEventosTest {
    private record Ambiente(ConfiguracaoSistemaRepositorio repo, EntityManager em,
                            PlatformTransactionManager tm, GuardaFormatoHashEventos guarda,
                            AtomicBoolean commitConcluido, ConfiguracaoSistema marcador) { }

    @Test
    void marcadorIgualNaoMudaMetadadoNemPercorreOuRegravaEventos() {
        Ambiente a = ambiente(MaterialHashEventos.FORMATO, true, true);
        a.guarda.afterSingletonsInstantiated();
        verify(a.repo, never()).saveAndFlush(any());
        verifyNoInteractions(a.em);
        assertEquals(MaterialHashEventos.FORMATO, a.marcador.obterValor());
        assertEquals("fixture", a.marcador.getDescricao());
        verify(a.tm).commit(any());
    }

    @Test
    void ausenteComDuasFamiliasVaziasGravaSomenteChaveTecnicaNova() {
        Ambiente a = ambiente(null, false, false);
        a.guarda.afterSingletonsInstantiated();
        verify(a.em).persist(argThat(c -> c instanceof ConfiguracaoSistema cfg
                && MaterialHashEventos.CHAVE_MARCADOR.equals(cfg.getChave())
                && MaterialHashEventos.FORMATO.equals(cfg.obterValor())));
        verify(a.em).flush();
        verify(a.tm).commit(any());
        verify(a.repo, never()).saveAndFlush(any());
        verify(a.em, never()).merge(any());
        verify(a.tm).getTransaction(argThat(t -> t.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW));
    }

    @Test
    void marcadorDivergenteRecusaSemTrocarValorOuEscreverEventos() {
        Ambiente a = ambiente("FORMATO_DIFERENTE", false, false);
        var erro = assertThrows(IllegalStateException.class, a.guarda::afterSingletonsInstantiated);
        assertEquals("SGT_HASH_EVENTOS_MARCADOR_DIVERGENTE", erro.getMessage());
        verify(a.repo, never()).saveAndFlush(any());
        verifyNoInteractions(a.em);
        assertEquals("FORMATO_DIFERENTE", a.marcador.obterValor());
        assertEquals("fixture", a.marcador.getDescricao());
        verify(a.tm).rollback(any());
    }

    @Test
    void semMarcadorComQualquerFamiliaExistenteNaoEscreveMarcador() {
        for (boolean vinculo : List.of(true, false)) {
            Ambiente a = ambiente(null, vinculo, !vinculo);
            var erro = assertThrows(IllegalStateException.class, a.guarda::afterSingletonsInstantiated);
            assertEquals("SGT_HASH_EVENTOS_SEM_MARCADOR_COM_ACERVO", erro.getMessage());
            verify(a.repo, never()).saveAndFlush(any());
            verify(a.em, never()).persist(any());
            verify(a.tm).rollback(any());
        }
    }

    @Test
    void falhaDePersistenciaNaoPermiteProsseguirSemMarcadorConfirmado() {
        Ambiente a = ambiente(null, false, false);
        doThrow(new IllegalStateException("falha sintética")).when(a.em).flush();
        var erro = assertThrows(IllegalStateException.class, a.guarda::afterSingletonsInstantiated);
        assertEquals("SGT_HASH_EVENTOS_INICIALIZACAO_FALHOU", erro.getMessage());
        verify(a.tm).rollback(any());
    }

    @Test
    void recusaImpedeStartRunnersEReadinessEmboraObjetoWebServerSejaCriado() {
        Ambiente a = ambiente("FORMATO_DIFERENTE", false, false);
        ServidorSemRede servidor = new ServidorSemRede();
        AtomicBoolean runner = new AtomicBoolean(), ready = new AtomicBoolean(), aceitando = new AtomicBoolean();
        SpringApplication app = aplicacao(a, servidor, runner, ready, aceitando);
        assertThrows(RuntimeException.class, () -> app.run(argumentos()));
        assertTrue(servidor.criado, "Boot pode construir o objeto antes de finalizar os singletons");
        assertFalse(servidor.iniciado);
        assertFalse(runner.get()); assertFalse(ready.get()); assertFalse(aceitando.get());
        verify(a.tm, never()).commit(any());
    }

    @Test
    void controlePositivoConfirmaCommitAntesDeStartRunnersEReadiness() {
        Ambiente a = ambiente(MaterialHashEventos.FORMATO, false, false);
        ServidorSemRede servidor = new ServidorSemRede();
        AtomicBoolean runner = new AtomicBoolean(), ready = new AtomicBoolean(), aceitando = new AtomicBoolean();
        try (var contexto = aplicacao(a, servidor, runner, ready, aceitando).run(argumentos())) {
            assertTrue(servidor.criado); assertTrue(servidor.iniciado);
            assertTrue(a.commitConcluido.get());
            assertTrue(runner.get()); assertTrue(ready.get()); assertTrue(aceitando.get());
        }
    }

    @Test
    void listagemOperacionalExcluiSomenteMarcadorNovoEPreservaDemaisChaves() {
        Ambiente a = ambiente(MaterialHashEventos.FORMATO, false, false);
        List<String> esperadas = List.of("HORAS_DIA_TRABALHO", "HORA_INICIO_NOTURNO", "MINUTOS_HORA_NOTURNA",
                "HORA_FIM_NOTURNO", "FORMATO_HASH_OUTRA_FAMILIA");
        var linhas = new java.util.ArrayList<ConfiguracaoSistema>();
        for (String chave : esperadas) linhas.add(new ConfiguracaoSistema(chave, "valor", "fixture"));
        linhas.add(new ConfiguracaoSistema(MaterialHashEventos.CHAVE_MARCADOR, MaterialHashEventos.FORMATO, "técnico"));
        when(a.repo.findAll()).thenReturn(linhas);
        var servico = new ConfiguracaoSistemaServico(a.repo, mock(PropriedadesSistema.class), null);
        assertEquals(esperadas, servico.listar().stream().map(c -> c.chave()).toList());
    }

    @Configuration(proxyBeanMethods = false)
    static class ConfiguracaoMinima { }

    private String[] argumentos() {
        return new String[]{"--spring.config.location=optional:classpath:/guarda-formato-vazio.properties",
                "--spring.main.banner-mode=off", "--logging.level.root=WARN"};
    }

    private SpringApplication aplicacao(Ambiente a, ServidorSemRede servidor, AtomicBoolean runner,
                                        AtomicBoolean ready, AtomicBoolean aceitando) {
        SpringApplication app = new SpringApplication(ConfiguracaoMinima.class);
        app.setWebApplicationType(WebApplicationType.SERVLET);
        app.setRegisterShutdownHook(false); app.setLogStartupInfo(false);
        // Controle adversarial: a guarda @Lazy(false) precisa continuar eager.
        app.setLazyInitialization(true);
        app.setApplicationContextFactory(tipo -> new AnnotationConfigServletWebServerApplicationContext());
        app.addInitializers(c -> {
            var contexto = (AnnotationConfigServletWebServerApplicationContext) c;
            contexto.registerBean("entityManagerFactory", Object.class, Object::new);
            contexto.registerBean(ConfiguracaoSistemaRepositorio.class, () -> a.repo);
            contexto.registerBean(EntityManager.class, () -> a.em);
            contexto.registerBean(PlatformTransactionManager.class, () -> a.tm);
            contexto.registerBean(ServletWebServerFactory.class, () -> initializers -> {
                servidor.criado = true;
                servidor.antesDeIniciar = () -> assertTrue(a.commitConcluido.get(), "Commit precede start()");
                return servidor;
            });
            contexto.registerBean(ApplicationRunner.class, () -> args -> {
                assertTrue(a.commitConcluido.get()); runner.set(true);
            });
            contexto.register(GuardaFormatoHashEventos.class);
        });
        app.addListeners((ApplicationListener<ApplicationEvent>) evento -> {
            if (evento instanceof ApplicationReadyEvent) ready.set(true);
            if (evento instanceof AvailabilityChangeEvent<?> mudanca
                    && ReadinessState.ACCEPTING_TRAFFIC.equals(mudanca.getState())) aceitando.set(true);
        });
        return app;
    }

    private static final class ServidorSemRede implements WebServer {
        boolean criado, iniciado;
        Runnable antesDeIniciar;
        @Override public void start() { antesDeIniciar.run(); iniciado = true; }
        @Override public void stop() { }
        @Override public int getPort() { return -1; }
    }

    @SuppressWarnings("unchecked")
    private Ambiente ambiente(String marcador, boolean possuiVinculo, boolean possuiAlocacao) {
        ConfiguracaoSistemaRepositorio repo = mock(ConfiguracaoSistemaRepositorio.class);
        EntityManager em = mock(EntityManager.class);
        PlatformTransactionManager tm = mock(PlatformTransactionManager.class);
        AtomicBoolean commitConcluido = new AtomicBoolean();
        when(tm.getTransaction(any())).thenAnswer(i -> new SimpleTransactionStatus());
        doAnswer(i -> { commitConcluido.set(true); return null; }).when(tm).commit(any());
        ConfiguracaoSistema linha = marcador == null ? null
                : new ConfiguracaoSistema(MaterialHashEventos.CHAVE_MARCADOR, marcador, "fixture");
        when(repo.buscarPorChaveParaAtualizacao(MaterialHashEventos.CHAVE_MARCADOR)).thenReturn(Optional.ofNullable(linha));
        // Estes casos exercitam a família53; a nova família tem marcador explicitamente compatível.
        when(repo.buscarPorChaveParaAtualizacao(com.example.siggtrat.regra.AssinaturaPreviaNegocial.CHAVE_MARCADOR))
                .thenReturn(Optional.of(new ConfiguracaoSistema(com.example.siggtrat.regra.AssinaturaPreviaNegocial.CHAVE_MARCADOR,
                        com.example.siggtrat.regra.AssinaturaPreviaNegocial.FORMATO, "fixture-compatibilidade-previas")));
        if (marcador == null) {
            TypedQuery<Long> v = mock(TypedQuery.class), a = mock(TypedQuery.class);
            when(em.createQuery("select e.id from EventoVinculoContratual e", Long.class)).thenReturn(v);
            when(em.createQuery("select e.id from EventoAlocacaoVinculo e", Long.class)).thenReturn(a);
            when(v.setMaxResults(1)).thenReturn(v); when(a.setMaxResults(1)).thenReturn(a);
            when(v.getResultList()).thenReturn(possuiVinculo ? List.of(1L) : List.of());
            when(a.getResultList()).thenReturn(possuiAlocacao ? List.of(2L) : List.of());
        }
        return new Ambiente(repo, em, tm, new GuardaFormatoHashEventos(repo, em, tm), commitConcluido, linha);
    }
}
