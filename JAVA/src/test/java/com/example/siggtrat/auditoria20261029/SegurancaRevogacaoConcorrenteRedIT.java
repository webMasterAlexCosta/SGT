package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.acesso.TipoPermissaoExcepcional;
import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.entidade.PermissaoExcepcional;
import com.example.siggtrat.entidade.UsoPermissaoExcepcional;
import com.example.siggtrat.utilitario.RelogioSistema;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Candidato SEG-003. Dois comandos HTTP e duas transações MySQL reais.
 * A barreira AOP apenas pausa depois da consulta JPA real; não substitui banco,
 * repositório, autorização nem retorno. Aceita as duas ordens serializáveis:
 * uso conclui antes da revogação, ou revogação conclui e impede novo uso.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import(SegurancaRevogacaoConcorrenteRedIT.ConfiguracaoBarreira.class)
class SegurancaRevogacaoConcorrenteRedIT extends ContratosHttpBase {
    @Autowired BarreiraConsultaReal barreira;

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void revogacaoConfirmadaAntesDoConsumoDeveImpedirAtoOuSerSerializadaComEle() throws Exception {
        Cenario c = criarCenarioPersistido();
        NovoContrato n = emTransacao(() -> {
            try { return publicado(c, 5, null, false); }
            catch (Exception erro) { throw new IllegalStateException(erro); }
        });
        String numeroAntes = emTransacao(() -> em.find(Contrato.class, n.id()).getNumeroContrato());
        var concedida = enviar(c.admin, "/api/permissoes-excepcionais", mapa(
                "tipo", "CORRECAO_IDENTIFICADOR_CONTRATO", "usuarioBeneficiarioId", c.localA.getId(),
                "contratoId", n.id(), "finalidade", "Prova sintética concorrente",
                "justificativa", "Autorização sujeita a revogação concorrente",
                "autorizacaoAdministrativa", "ATO-SEG-REVOGACAO",
                "validoAte", Instant.now().plusSeconds(3600)), 201, "Concessão original confirmada");
        long permissaoId = concedida.path("id").asLong();
        String novoNumero = "SEG-RACE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(java.util.Locale.ROOT);
        String comando = json(mapa("novoNumero", novoNumero, "novoAno", n.inicio().getYear(),
                "dataEfetiva", RelogioSistema.hoje(), "dataAto", RelogioSistema.hoje(),
                "motivo", "Correção com consumo de concessão concorrente",
                "referenciaSei", "SEI-SEG-REVOGACAO", "autorizacaoAdministrativa", "ATO-SEG-REVOGACAO",
                "chaveIdempotencia", UUID.randomUUID().toString()));
        var pedidoAto = autenticar(post(base(n.id()) + "/correcoes-identificador")
                .contentType(APPLICATION_JSON).content(comando), c.localA);
        var pedidoRevogacao = autenticar(post("/api/permissoes-excepcionais/" + permissaoId + "/revogar")
                .contentType(APPLICATION_JSON).content(json(mapa("motivo", "Ato administrativo retira a concessão"))), c.admin);
        barreira.armar(c.localA.getId(), permissaoId);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<MvcResult> ato = pool.submit(() -> mvc.perform(pedidoAto).andReturn());
            long conexaoAto = barreira.consultaConcluida.get(12, TimeUnit.SECONDS);
            Future<MvcResult> revogacao = pool.submit(() -> mvc.perform(pedidoRevogacao).andReturn());
            boolean revogacaoConcluiuAntesDoConsumo = false;
            boolean esperaReal = false;
            long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (System.nanoTime() < limite) {
                if (revogacao.isDone()) { revogacaoConcluiuAntesDoConsumo = true; break; }
                long esperas = emTransacao(() -> ((Number) em.createNativeQuery("""
                        select count(*) from performance_schema.data_lock_waits espera
                        join performance_schema.threads bloqueador on bloqueador.THREAD_ID=espera.BLOCKING_THREAD_ID
                        where bloqueador.PROCESSLIST_ID=:conexao
                        """).setParameter("conexao", conexaoAto).getSingleResult()).longValue());
                if (esperas > 0) { esperaReal = true; break; }
                TimeUnit.MILLISECONDS.sleep(30);
            }
            assertTrue(revogacaoConcluiuAntesDoConsumo || esperaReal,
                    "A revogação deve concluir ou disputar um lock observado antes de liberar o comando.");
            if (revogacaoConcluiuAntesDoConsumo) {
                assertEquals(200, revogacao.get(1, TimeUnit.SECONDS).getResponse().getStatus());
                emTransacao(() -> {
                    assertFalse(Boolean.TRUE.equals(em.find(PermissaoExcepcional.class, permissaoId).getAtivo()),
                            "Nova conexão prova commit da revogação antes de liberar o uso.");
                    assertEquals(0L, em.createQuery("select count(u) from UsoPermissaoExcepcional u where u.permissao.id=:p", Long.class)
                            .setParameter("p", permissaoId).getSingleResult());
                    return null;
                });
            }
            barreira.liberar.countDown();
            MvcResult resultadoAto = ato.get(20, TimeUnit.SECONDS);
            MvcResult resultadoRevogacao = revogacao.get(20, TimeUnit.SECONDS);
            Estado estado = emTransacao(() -> {
                PermissaoExcepcional p = em.find(PermissaoExcepcional.class, permissaoId);
                List<UsoPermissaoExcepcional> usos = em.createQuery(
                                "select u from UsoPermissaoExcepcional u where u.permissao.id=:p", UsoPermissaoExcepcional.class)
                        .setParameter("p", permissaoId).getResultList();
                long correcoes = em.createQuery("select count(x) from CorrecaoIdentificadorContrato x where x.contrato.id=:c", Long.class)
                        .setParameter("c", n.id()).getSingleResult();
                return new Estado(em.find(Contrato.class, n.id()).getNumeroContrato(), p.getRevogadoEm(),
                        usos.size(), usos.isEmpty() ? null : usos.get(0).getUsadoEm(), correcoes);
            });
            Path evidencia = Path.of("target/auditoria-http/seguranca-revogacao-concorrente.json");
            Files.createDirectories(evidencia.getParent());
            Files.writeString(evidencia, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapa(
                    "revogacaoConfirmadaAntesDoConsumo", revogacaoConcluiuAntesDoConsumo,
                    "esperaMySqlObservada", esperaReal, "atoStatus", resultadoAto.getResponse().getStatus(),
                    "revogacaoStatus", resultadoRevogacao.getResponse().getStatus(),
                    "numeroAnterior", numeroAntes, "numeroFinal", estado.numero(),
                    "revogadoEm", estado.revogadoEm(), "usos", estado.usos(),
                    "usadoEm", estado.usadoEm(), "correcoes", estado.correcoes())));
            assertEquals(200, resultadoRevogacao.getResponse().getStatus());
            if (revogacaoConcluiuAntesDoConsumo) {
                assertAll(
                        () -> assertTrue(List.of(403, 409).contains(resultadoAto.getResponse().getStatus()),
                                "O ato precisa recusar a autorização que já foi revogada antes de seu consumo."),
                        () -> assertEquals(numeroAntes, estado.numero(), "Recusa deve preservar o identificador."),
                        () -> assertEquals(0L, estado.correcoes(), "Sem correção excepcional após revogação confirmada."),
                        () -> assertEquals(0, estado.usos(), "Não registrar uso novo de concessão revogada."));
            } else {
                assertEquals(201, resultadoAto.getResponse().getStatus(), "Serialização admite o ato que detém a concessão primeiro.");
                assertEquals(novoNumero, estado.numero());
                assertEquals(1, estado.usos());
                assertNotNull(estado.usadoEm());
                assertNotNull(estado.revogadoEm());
                assertFalse(estado.usadoEm().isAfter(estado.revogadoEm()));
            }
        } finally {
            barreira.liberar.countDown();
            barreira.ativa.set(false);
            pool.shutdownNow();
        }
    }

    private record Estado(String numero, Instant revogadoEm, int usos, Instant usadoEm, long correcoes) { }

    @TestConfiguration(proxyBeanMethods = false)
    static class ConfiguracaoBarreira {
        @Bean BarreiraConsultaReal barreiraConsultaReal() { return new BarreiraConsultaReal(); }
    }

    @Aspect
    static class BarreiraConsultaReal {
        @PersistenceContext EntityManager em;
        final AtomicBoolean ativa = new AtomicBoolean();
        final CompletableFuture<Long> consultaConcluida = new CompletableFuture<>();
        final CountDownLatch liberar = new CountDownLatch(1);
        Long usuarioId;
        Long permissaoId;

        void armar(Long usuario, Long permissao) { usuarioId = usuario; permissaoId = permissao; ativa.set(true); }

        @Around("execution(* com.example.siggtrat.repositorio.PermissaoExcepcionalRepositorio.buscarAtivas(..))")
        Object depoisDaConsultaReal(ProceedingJoinPoint consulta) throws Throwable {
            Object resultado = consulta.proceed();
            Object[] argumentos = consulta.getArgs();
            if (usuarioId != null && usuarioId.equals(argumentos[0])
                    && argumentos[1] == TipoPermissaoExcepcional.CORRECAO_IDENTIFICADOR_CONTRATO
                    && ativa.compareAndSet(true, false)) {
                assertTrue(resultado instanceof List<?> lista && lista.stream()
                        .anyMatch(x -> x instanceof PermissaoExcepcional p && permissaoId.equals(p.getId())),
                        "A barreira deve vir após uma concessão real consultada no MySQL.");
                consultaConcluida.complete(((Number) em.createNativeQuery("select connection_id()").getSingleResult()).longValue());
                if (!liberar.await(30, TimeUnit.SECONDS)) throw new AssertionError("Barreira de autorização não liberada.");
            }
            return resultado;
        }
    }
}
