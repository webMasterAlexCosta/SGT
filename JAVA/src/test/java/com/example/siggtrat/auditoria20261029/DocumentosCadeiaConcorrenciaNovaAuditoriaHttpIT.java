package com.example.siggtrat.auditoria20261029;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Duas transações reais; a primeira é suspensa após o serviço e antes do commit. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class DocumentosCadeiaConcorrenciaNovaAuditoriaHttpIT extends ContratosHttpBase {

    @Test
    @Timeout(value = 45, unit = TimeUnit.SECONDS)
    void duasConcessoesConcorrentesNaoBifurcamCadeiaDoMesmoDocumento() throws Exception {
        var c = criarCenarioPersistido();
        var n = emTransacao(() -> {
            try { return publicado(c, 10, null, true); }
            catch (Exception e) { throw new IllegalStateException(e); }
        });
        long publicacao = consultar(c.localA, base(n.id()) + "/eventos", "Evento original confirmado").get(0).path("id").asLong();
        var doc = dados(executar(c.localA, multipart(base(n.id()) + "/documentos")
                .file(metadados(mapa("tipoFato", "CONTRATO_ORIGINAL", "fatoId", publicacao,
                        "titulo", "Documento restrito da corrida", "referenciaSei", "SEI-CONCORRENCIA",
                        "classificacaoAcesso", "PESSOAL_RESTRITO", "dataEfetiva", n.inicio()))),
                201, "Preparar documento com cadeia íntegra e transação confirmada"));
        String grupo = doc.path("grupoLogicoId").asText();
        String path = base(n.id()) + "/documentos/" + doc.path("id").asLong();
        assertThat(grupo).isNotBlank();
        var inicial = dados(executar(c.localA, get(path + "/evidencias-integridade").param("finalidade", "conferir origem"),
                200, "Cadeia inicial comprovadamente íntegra antes da concorrência"));
        assertThat(inicial.path("cadeiaIntegra").asBoolean()).as(inicial.toPrettyString()).isTrue();
        var primeiraReq = autenticar(post(path + "/permissoes").contentType(APPLICATION_JSON)
                .content(json(mapa("usuarioId", c.nacional.getId(), "finalidade", "auditoria nacional",
                        "justificativa", "Primeira concessão válida", "autorizacaoAdministrativa", "ATO-1",
                        "validoAte", Instant.now().plusSeconds(3600)))), c.admin);
        var segundaReq = autenticar(post(path + "/permissoes").contentType(APPLICATION_JSON)
                .content(json(mapa("usuarioId", c.estadualA.getId(), "finalidade", "auditoria estadual",
                        "justificativa", "Segunda concessão válida", "autorizacaoAdministrativa", "ATO-2",
                        "validoAte", Instant.now().plusSeconds(3600)))), c.admin);
        CompletableFuture<Long> primeiraLeitura = new CompletableFuture<>();
        CountDownLatch liberarPrimeira = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<MvcResult> primeira = pool.submit(() -> emTransacao(() -> {
                try {
                    MvcResult resultado = mvc.perform(primeiraReq).andReturn();
                    assertThat(resultado.getResponse().getStatus()).as(resultado.getResponse().getContentAsString()).isEqualTo(200);
                    primeiraLeitura.complete(((Number) em.createNativeQuery("select connection_id()").getSingleResult()).longValue());
                    if (!liberarPrimeira.await(25, TimeUnit.SECONDS)) throw new AssertionError("Barreira da cadeia não liberada.");
                    return resultado;
                } catch (Exception e) {
                    primeiraLeitura.completeExceptionally(e);
                    throw new IllegalStateException(e);
                }
            }));
            long conexao = primeiraLeitura.get(10, TimeUnit.SECONDS);
            Future<MvcResult> segunda = pool.submit(() -> mvc.perform(segundaReq).andReturn());
            long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            boolean observada = false;
            while (System.nanoTime() < limite) {
                if (segunda.isDone()) { observada = true; break; }
                long esperas = emTransacao(() -> ((Number) em.createNativeQuery("""
                        select count(*) from performance_schema.data_lock_waits espera
                        join performance_schema.threads bloqueador on bloqueador.THREAD_ID=espera.BLOCKING_THREAD_ID
                        where bloqueador.PROCESSLIST_ID=:conexao
                        """).setParameter("conexao", conexao).getSingleResult()).longValue());
                if (esperas > 0) { observada = true; break; }
                TimeUnit.MILLISECONDS.sleep(20);
            }
            assertThat(observada).as("Segunda transação conclui ou espera lock real antes da liberação").isTrue();
            liberarPrimeira.countDown();
            var r1 = primeira.get(15, TimeUnit.SECONDS); var r2 = segunda.get(15, TimeUnit.SECONDS);
            assertThat(r1.getResponse().getStatus()).as(r1.getResponse().getContentAsString()).isEqualTo(200);
            assertThat(r2.getResponse().getStatus()).as(r2.getResponse().getContentAsString()).isEqualTo(200);
            emTransacao(() -> {
                Long total = em.createQuery("select count(p) from PermissaoDocumentoRestrito p where p.grupoLogicoId=:grupo", Long.class)
                        .setParameter("grupo", grupo).getSingleResult();
                assertThat(total).isEqualTo(2L);
                return null;
            });
            var evidencia = dados(executar(c.localA, get(path + "/evidencias-integridade").param("finalidade", "conferir cadeia"),
                    200, "DOC-010/AUD-001 duas concessões confirmadas conservam encadeamento íntegro"));
            assertThat(evidencia.path("cadeiaIntegra").asBoolean()).as(evidencia.toPrettyString()).isTrue();
            assertThat(evidencia.path("eventos").size()).isEqualTo(3);
        } finally {
            liberarPrimeira.countDown(); pool.shutdownNow();
        }
    }
}
