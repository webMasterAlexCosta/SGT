package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.PermissaoDocumentoRestrito;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Revisor independente; duas transações PostgreSQL reais, sem mock de banco/autorização. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import(Candidato2DocumentoRevogacaoConcorrenteRedIT.Configuracao.class)
class Candidato2DocumentoRevogacaoConcorrenteRedIT extends ContratosHttpBase {
    @Autowired BarreiraDocumento barreira;

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void revogacaoDocumentalConfirmadaAntesDoConsumoImpedeEntregaOuEsperaLeitura() throws Exception {
        Cenario c = criarCenarioPersistido();
        long doc = dados(executar(c.localA, multipart(base(c.contratoA.getId()) + "/documentos")
                .file(metadados(mapa("tipoFato", "VINCULO", "fatoId", c.vinculoA.getId(),
                        "titulo", "Fonte restrita da revisão independente", "descricao", "MARCADOR-RESTRITO-" + UUID.randomUUID(),
                        "referenciaSei", "SEI-REVOGACAO-" + UUID.randomUUID(), "classificacaoAcesso", "PESSOAL_RESTRITO",
                        "dataEfetiva", DATA))), 201, "Fonte publicada da própria raiz")).path("id").asLong();
        String rota = base(c.contratoA.getId()) + "/documentos/" + doc;
        long permissao = enviar(c.admin, rota + "/permissoes", mapa("usuarioId", c.nacional.getId(),
                "finalidade", "Auditoria sintética", "justificativa", "Conferência da revogação",
                "autorizacaoAdministrativa", "ATO-REVISOR", "validoAte", Instant.now().plusSeconds(3600)),
                200, "Concessão documental específica").path("id").asLong();
        assertEquals(doc, consultar(c.nacional, rota + "?finalidade=auditoria",
                "Controle positivo antes da revogação").path("id").asLong());
        long acessosAntes = emTransacao(() -> em.createQuery(
                "select count(a) from AcessoDocumentoRestrito a where a.permissaoId=:p", Long.class)
                .setParameter("p", permissao).getSingleResult());
        barreira.armar(c.nacional.getId(), permissao);
        var leituraHttp = autenticar(get(rota).param("finalidade", "conferência concorrente"), c.nacional);
        var revogacaoHttp = autenticar(post(rota + "/permissoes/" + permissao + "/revogar")
                .contentType(APPLICATION_JSON).content(json(mapa("motivo", "Retirada concorrente da autorização",
                        "autorizacaoAdministrativa", "ATO-REVOGAR-REVISOR"))), c.admin);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<MvcResult> leitura = pool.submit(() -> mvc.perform(leituraHttp).andReturn());
            long conexao = barreira.consulta.get(12, TimeUnit.SECONDS);
            Future<MvcResult> revogacao = pool.submit(() -> mvc.perform(revogacaoHttp).andReturn());
            boolean concluidaAntes = false;
            boolean esperaReal = false;
            long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (System.nanoTime() < limite) {
                if (revogacao.isDone()) { concluidaAntes = true; break; }
                long esperas = emTransacao(() -> ((Number) em.createNativeQuery("""
SELECT count(*) FROM pg_stat_activity a WHERE a.datname=current_database() AND :conexao = ANY(pg_blocking_pids(a.pid))
""").setParameter("conexao", conexao).getSingleResult()).longValue());
                if (esperas > 0) { esperaReal = true; break; }
                TimeUnit.MILLISECONDS.sleep(30);
            }
            assertTrue(concluidaAntes || esperaReal, "Deve haver commit ou espera real observada no PostgreSQL.");
            if (concluidaAntes) {
                assertEquals(200, revogacao.get(1, TimeUnit.SECONDS).getResponse().getStatus());
                emTransacao(() -> {
                    assertFalse(Boolean.TRUE.equals(em.find(PermissaoDocumentoRestrito.class, permissao).getAtivo()));
                    assertEquals(acessosAntes, em.createQuery(
                            "select count(a) from AcessoDocumentoRestrito a where a.permissaoId=:p", Long.class)
                            .setParameter("p", permissao).getSingleResult());
                    return null;
                });
            }
            barreira.liberar.countDown();
            MvcResult resultado = leitura.get(20, TimeUnit.SECONDS);
            MvcResult retirada = revogacao.get(20, TimeUnit.SECONDS);
            long acessosDepois = emTransacao(() -> em.createQuery(
                    "select count(a) from AcessoDocumentoRestrito a where a.permissaoId=:p", Long.class)
                    .setParameter("p", permissao).getSingleResult());
            Path arquivo = Path.of("target/auditoria-http/candidato2-documento-revogacao.json");
            Files.createDirectories(arquivo.getParent());
            Files.writeString(arquivo, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapa(
                    "revogacaoCommitadaAntesDoConsumo", concluidaAntes, "esperaMySqlObservada", esperaReal,
                    "statusLeitura", resultado.getResponse().getStatus(), "statusRevogacao", retirada.getResponse().getStatus(),
                    "acessosAntes", acessosAntes, "acessosDepois", acessosDepois)));
            assertEquals(200, retirada.getResponse().getStatus());
            if (concluidaAntes) {
                assertAll(() -> assertTrue(List.of(403, 404, 409).contains(resultado.getResponse().getStatus()),
                                "Revogação já confirmada deve impedir a entrega ainda não consumida."),
                        () -> assertEquals(acessosAntes, acessosDepois, "Não consumir concessão já revogada."));
            } else {
                assertEquals(200, resultado.getResponse().getStatus(), "Leitura pode terminar antes da revogação serializada.");
                assertEquals(acessosAntes + 1, acessosDepois);
            }
            executar(c.nacional, get(rota).param("finalidade", "auditoria"), 403,
                    "Controle negativo sequencial após revogação");
        } finally {
            barreira.liberar.countDown();
            barreira.ativa.set(false);
            pool.shutdownNow();
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Configuracao {
        @Bean BarreiraDocumento barreiraDocumento() { return new BarreiraDocumento(); }
    }
    @Aspect
    static class BarreiraDocumento {
        @PersistenceContext EntityManager em;
        final AtomicBoolean ativa = new AtomicBoolean();
        final CompletableFuture<Long> consulta = new CompletableFuture<>();
        final CountDownLatch liberar = new CountDownLatch(1);
        Long usuarioId;
        Long permissaoId;
        void armar(Long usuario, Long permissao) { usuarioId = usuario; permissaoId = permissao; ativa.set(true); }
        @Around("execution(* com.example.siggtrat.repositorio.PermissaoDocumentoRestritoRepositorio.buscarAtivas(..))")
        Object aposConsulta(ProceedingJoinPoint chamada) throws Throwable {
            Object resultado = chamada.proceed();
            if (usuarioId != null && usuarioId.equals(chamada.getArgs()[1]) && ativa.compareAndSet(true, false)) {
                assertTrue(resultado instanceof List<?> lista && lista.stream().anyMatch(
                        p -> p instanceof PermissaoDocumentoRestrito x && permissaoId.equals(x.getId())));
                consulta.complete(((Number) em.createNativeQuery("select pg_backend_pid()").getSingleResult()).longValue());
                if (!liberar.await(30, TimeUnit.SECONDS)) throw new AssertionError("Barreira documental não liberada.");
            }
            return resultado;
        }
    }
}
