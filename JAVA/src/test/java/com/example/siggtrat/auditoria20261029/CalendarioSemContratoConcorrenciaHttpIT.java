package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.entidade.CalendarioEvento;
import com.example.siggtrat.entidade.GestaoLocal;
import com.example.siggtrat.entidade.SuperintendenciaEstadual;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.servico.InvalidacaoEmissaoRelatorioServico;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** AUD-005: ausencia de Contrato nao elimina exclusao mutua da identidade do calendario. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CalendarioSemContratoConcorrenciaHttpIT extends AuditoriaHttpSuporte {
    @MockitoSpyBean InvalidacaoEmissaoRelatorioServico invalidacao;

    @Test
    @Timeout(value = 45, unit = TimeUnit.SECONDS)
    void cadastrosConcorrentesSemContratoNaoPublicamDuasFontesIguais() throws Exception {
        Cenario base = criarCenarioPersistido();
        Preparado p = emTransacao(() -> {
            GestaoLocal gestao = gestao("Calendario sem contratos " + UUID.randomUUID(),
                    em.find(SuperintendenciaEstadual.class, base.seA.getId()));
            var turno = jornada(gestao);
            var lotacao = lotacao(gestao, turno);
            UsuarioAplicacao primeiro = usuario(PerfilAcesso.GESTOR_LOCAL, gestao);
            UsuarioAplicacao segundo = usuario(PerfilAcesso.GESTOR_LOCAL, gestao);
            em.flush();
            assertEquals(0L, em.createQuery("select count(c) from Contrato c where c.gestaoLocal.id=:id", Long.class)
                    .setParameter("id", gestao.getId()).getSingleResult());
            return new Preparado(gestao.getId(), lotacao.getId(), primeiro, segundo);
        });
        String corpo = json(Map.of("data", DATA, "tipo", "FERIADO", "abrangencia", "LOTACAO",
                "lotacaoId", p.lotacaoId(), "ativo", true));
        var req1 = autenticar(post("/api/calendario-eventos").contentType(APPLICATION_JSON).content(corpo), p.primeiro());
        var req2 = autenticar(post("/api/calendario-eventos").contentType(APPLICATION_JSON).content(corpo), p.segundo());
        CompletableFuture<Long> primeiraFonteInserida = new CompletableFuture<>();
        CountDownLatch liberarPrimeira = new CountDownLatch(1);
        AtomicBoolean primeira = new AtomicBoolean(true);
        doAnswer(invocacao -> {
            CalendarioEvento evento = invocacao.getArgument(1);
            Object resultado = invocacao.callRealMethod();
            if (evento != null && p.lotacaoId().equals(evento.getLotacaoId()) && primeira.compareAndSet(true, false)) {
                assertNotNull(evento.getId(), "A barreira vem depois do INSERT real da primeira raiz");
                primeiraFonteInserida.complete(((Number) em.createNativeQuery("select pg_backend_pid()").getSingleResult()).longValue());
                if (!liberarPrimeira.await(25, TimeUnit.SECONDS)) throw new AssertionError("Barreira de concorrencia nao foi liberada.");
            }
            return resultado;
        }).when(org.springframework.test.util.AopTestUtils.<InvalidacaoEmissaoRelatorioServico>getUltimateTargetObject(invalidacao)).invalidarPorCalendario(isNull(), any(CalendarioEvento.class), anyString(), any(UsuarioAplicacao.class));
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<MvcResult> um = pool.submit(() -> mvc.perform(req1).andReturn());
            long conexao = primeiraFonteInserida.get(10, TimeUnit.SECONDS);
            Future<MvcResult> dois = pool.submit(() -> mvc.perform(req2).andReturn());
            long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            boolean observado = false;
            while (System.nanoTime() < limite) {
                if (dois.isDone()) { observado = true; break; }
                long esperas = emTransacao(() -> ((Number) em.createNativeQuery("""
SELECT count(*) FROM pg_stat_activity a WHERE a.datname=current_database() AND :conexao = ANY(pg_blocking_pids(a.pid))
""").setParameter("conexao", conexao).getSingleResult()).longValue());
                if (esperas > 0) { observado = true; break; }
                TimeUnit.MILLISECONDS.sleep(20);
            }
            assertTrue(observado, "A segunda transacao deve concluir ou disputar um lock real antes do primeiro commit");
            liberarPrimeira.countDown();
            MvcResult r1 = um.get(15, TimeUnit.SECONDS), r2 = dois.get(15, TimeUnit.SECONDS);
            List<Integer> estados = List.of(r1.getResponse().getStatus(), r2.getResponse().getStatus());
            long raizes = emTransacao(() -> em.createQuery("select count(e) from CalendarioEvento e "
                            + "where e.gestaoLocal.id=:g and e.lotacao.id=:l and e.data=:d and e.cancelado=false", Long.class)
                    .setParameter("g", p.gestaoId()).setParameter("l", p.lotacaoId()).setParameter("d", DATA).getSingleResult());
            assertAll(
                    () -> assertTrue(estados.contains(201), "Um dos pedidos deve publicar a fonte"),
                    () -> assertTrue(estados.contains(409), "O reenvio concorrente deve reconhecer a fonte ja publicada: " + estados),
                    () -> assertEquals(1L, raizes, "Duas transacoes nao podem publicar duas raizes da mesma identidade sem Contrato para travar"));
        } finally {
            liberarPrimeira.countDown();
            pool.shutdownNow();
        }
    }

    private record Preparado(Long gestaoId, Long lotacaoId, UsuarioAplicacao primeiro, UsuarioAplicacao segundo) { }
}
