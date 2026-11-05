package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.ComposicaoContratual;
import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.entidade.EstadoNegocialContrato;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.hibernate.Session;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** A barreira observa o SQL real antes do DELETE; não há mock de repositório nem de serviço. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class ComposicaoPublicacaoConcorrenciaHttpIT extends ContratosHttpBase {
    @DynamicPropertySource
    static void barreiraSql(DynamicPropertyRegistry props) {
        props.add("spring.jpa.properties.hibernate.session_factory.statement_inspector", BarreiraDelete.class::getName);
    }

    public static class BarreiraDelete implements StatementInspector {
        static final ThreadLocal<Runnable> antesDoDelete = new ThreadLocal<>();
        @Override public String inspect(String sql) {
            Runnable acao = antesDoDelete.get();
            if (acao != null && sql.toLowerCase(java.util.Locale.ROOT).contains("delete from composicao_contratual")) {
                antesDoDelete.remove(); acao.run();
            }
            return sql;
        }
    }

    @Test
    @Timeout(value = 45, unit = TimeUnit.SECONDS)
    void publicacaoAguardaMutacaoDaComposicaoENaoPublicaFotografiaDepoisRemovida() throws Exception {
        var c = criarCenarioPersistido(); var inicio = RelogioSistema.hoje().plusDays(5);
        long contratoId = enviar(c.localA, "/api/contratos", rascunho(c, "PUBLICACAO-CONCORRENTE-" + c.contratoA.getId(),
                5, inicio, inicio.plusMonths(2)), 201, "GOV-003 rascunho para publicação concorrente").path("id").asLong();
        long composicaoId = enviar(c.localA, base(contratoId) + "/composicoes", composicao(c, c.cargoA.getId(), 5), 201,
                "CMP-003 composição completa antes da corrida").path("id").asLong();
        CompletableFuture<Long> entrouNoDelete = new CompletableFuture<>();
        CountDownLatch liberarDelete = new CountDownLatch(1);
        Runnable barreira = () -> {
            long conexao = em.unwrap(Session.class).doReturningWork(conexaoJdbc -> {
                try (var consulta = conexaoJdbc.createStatement(); var resultado = consulta.executeQuery("select connection_id()")) {
                    resultado.next(); return resultado.getLong(1);
                }
            });
            entrouNoDelete.complete(conexao);
            try {
                if (!liberarDelete.await(25, TimeUnit.SECONDS)) throw new AssertionError("Publicação não alcançou a transação de composição.");
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
        };
        var publicar = autenticar(post(base(contratoId) + "/publicar").contentType(MediaType.APPLICATION_JSON)
                .content(json(mapa("dataPublicacao", RelogioSistema.hoje(), "referenciaSei", "SEI-PUBLICACAO-CONCORRENTE"))), c.localA);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<MvcResult> exclusao = pool.submit(() -> {
                BarreiraDelete.antesDoDelete.set(barreira);
                try {
                    return executar(c.localA, delete(base(contratoId) + "/composicoes/" + composicaoId),
                            204, "GOV-004 exclusão iniciada enquanto o contrato ainda era rascunho");
                } finally { BarreiraDelete.antesDoDelete.remove(); }
            });
            long conexao = entrouNoDelete.get(10, TimeUnit.SECONDS);
            Future<MvcResult> publicacao = pool.submit(() -> mvc.perform(publicar).andReturn());
            boolean aguardouContrato = esperarPublicacaoOuLock(conexao, publicacao);
            liberarDelete.countDown();
            exclusao.get(15, TimeUnit.SECONDS);
            MvcResult resposta = publicacao.get(15, TimeUnit.SECONDS);
            assertAll(
                    () -> assertThat(aguardouContrato).as("MySQL deve serializar a publicação após a transação de composição").isTrue(),
                    () -> assertThat(resposta.getResponse().getStatus()).as(resposta.getResponse().getContentAsString()).isEqualTo(400),
                    () -> emTransacao(() -> {
                        assertThat(em.find(Contrato.class, contratoId).getEstadoNegocial()).isEqualTo(EstadoNegocialContrato.RASCUNHO);
                        assertThat(em.find(ComposicaoContratual.class, composicaoId)).isNull();
                        assertThat(em.createQuery("select count(e) from EventoCicloVidaContrato e where e.contrato.id=:id", Long.class)
                                .setParameter("id", contratoId).getSingleResult()).isZero();
                        return null;
                    })
            );
        } finally { liberarDelete.countDown(); pool.shutdownNow(); }
    }

    private boolean esperarPublicacaoOuLock(long conexao, Future<MvcResult> publicacao) throws Exception {
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < limite) {
            if (publicacao.isDone()) return false;
            long esperas = emTransacao(() -> ((Number) em.createNativeQuery("""
                    select count(*) from performance_schema.data_lock_waits espera
                    join performance_schema.threads bloqueador on bloqueador.THREAD_ID=espera.BLOCKING_THREAD_ID
                    where bloqueador.PROCESSLIST_ID=:conexao
                    """).setParameter("conexao", conexao).getSingleResult()).longValue());
            if (esperas > 0) return true;
            TimeUnit.MILLISECONDS.sleep(20);
        }
        throw new AssertionError("Publicação não concluiu nem esperou pelo bloqueio do Contrato.");
    }
}
