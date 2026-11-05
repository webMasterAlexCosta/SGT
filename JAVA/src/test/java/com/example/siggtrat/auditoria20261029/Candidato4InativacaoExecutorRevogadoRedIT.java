package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.utilitario.RelogioSistema;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * C4-CON-H01 — Hipótese independente: inativarAdministrativamente carrega a
 * autorização antes de aguardar a raiz e não a revalida depois da espera.
 * Oráculo: CON-009:001 e norma §5.25/§5.26, autorização corrente do executor.
 * Usa a técnica de sincronização MySQL de Candidato3ReplayIdentificadorExecutorRevogadoRedIT,
 * preservando sua origem; cenário e assertivas de inativação por este revisor.
 * Fonte auditada: manifesto C4 13c84fd30b668335228704fd1028ddea11adad6f607458a7b7fa993dcbc64a89.
 * Proposta não compilada nem executada por este revisor; execução reservada à raiz.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@TestPropertySource(properties = {"spring.cache.type=redis", "spring.sql.init.mode=never",
        "app.cache.redis.http-get.enabled=true"})
class Candidato4InativacaoExecutorRevogadoRedIT extends ContratosHttpBase {
    @Autowired private DataSource dataSource;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void inativacaoRevalidaExecutorAposEsperaDaRaiz(boolean rebaixarPerfil) throws Exception {
        Cenario c = criarCenarioPersistido();
        NovoContrato n = emTransacao(() -> {
            try { return publicado(c, 5, null, false); }
            catch (Exception ex) { throw new IllegalStateException(ex); }
        });
        String rota = base(n.id()) + "/inativar";
        Map<String, Object> requisicao = mapa("dataEfetiva", RelogioSistema.hoje(),
                "dataAto", RelogioSistema.hoje(),
                "motivo", "Inativação sintética para examinar autorização corrente após espera",
                "referenciaSei", "SEI-SINTETICO-C4-INATIVACAO-" + UUID.randomUUID());
        var previa = enviar(c.localA, base(n.id()) + "/previas/inativar", requisicao, 200,
                "Controle positivo: prévia autorizada antes da revogação");
        requisicao.put("tokenPrevia", previa.path("tokenPrevia").asText());
        requisicao.put("confirmarImpacto", true);
        Estado antes = estado(n.id(), c.localA.getId());
        assertThat(antes.ativo()).isTrue();

        // A sessão está validada antes de alcançar a espera observável na raiz.
        var pedido = autenticar(post(rota).contentType(MediaType.APPLICATION_JSON).content(json(requisicao)), c.localA);
        var pool = Executors.newFixedThreadPool(2);
        CountDownLatch liberarContrato = new CountDownLatch(1);
        CompletableFuture<Long> conexaoBloqueadora = new CompletableFuture<>();
        try {
            Future<?> bloqueador = pool.submit(() -> emTransacao(() -> {
                em.find(Contrato.class, n.id(), LockModeType.PESSIMISTIC_WRITE);
                conexaoBloqueadora.complete(((Number) em.createNativeQuery("select connection_id()")
                        .getSingleResult()).longValue());
                try {
                    if (!liberarContrato.await(30, TimeUnit.SECONDS))
                        throw new AssertionError("Limite da fixture: Contrato não liberado");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(ex);
                }
                return null;
            }));
            long conexao = conexaoBloqueadora.get(10, TimeUnit.SECONDS);
            Future<MvcResult> tentativa = pool.submit(() -> mvc.perform(pedido).andReturn());
            aguardarBloqueioReal(conexao, tentativa);

            Map<String, Object> revogacao = rebaixarPerfil
                    ? mapa("perfil", "OPERACIONAL", "gestaoLocalId", c.gestaoA.getId(),
                            "lotacaoId", c.lotacaoA.getId(), "jornadaId", c.jornadaA.getId())
                    : mapa("active", false);
            executar(c.admin, put("/api/usuarios/" + c.localA.getId())
                            .contentType(MediaType.APPLICATION_JSON).content(json(revogacao)), 200,
                    "Admin confirma a revogação antes de liberar o inativacao");
            emTransacao(() -> {
                UsuarioAplicacao persistido = em.find(UsuarioAplicacao.class, c.localA.getId());
                if (rebaixarPerfil) assertThat(persistido.getPerfil()).isEqualTo(PerfilAcesso.OPERACIONAL);
                else assertThat(persistido.getActive()).isFalse();
                return null;
            });
            assertThat(tentativa.isDone()).as("Inativação ainda aguarda a raiz após revogação commitada").isFalse();
            liberarContrato.countDown();
            bloqueador.get(10, TimeUnit.SECONDS);
            MvcResult resultado = tentativa.get(15, TimeUnit.SECONDS);
            Estado depois = estado(n.id(), c.localA.getId());

            Path evidencia = Path.of("target/auditoria-http/candidato4-inativacao-executor-"
                    + (rebaixarPerfil ? "rebaixado" : "desativado") + ".json");
            Files.createDirectories(evidencia.getParent());
            Files.writeString(evidencia, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapa(
                    "hipotese", "C4-CON-H01", "rebaixarPerfil", rebaixarPerfil,
                    "esperaMySqlObservada", true, "revogacaoCommitadaAntesDaLiberacao", true,
                    "statusInativação", resultado.getResponse().getStatus(), "antes", antes, "depois", depois)));
            assertAll("Inativação depende de executor ainda autorizado no instante da mutação",
                    () -> assertThat(resultado.getResponse().getStatus())
                            .as("Executor revogado enquanto aguardava a raiz deve ser recusado")
                            .isIn(401, 403, 404, 409),
                    () -> assertThat(depois).as("Sem mudança de habilitação, versão ou evento após revogação").isEqualTo(antes));
        } finally {
            liberarContrato.countDown();
            pool.shutdownNow();
            pool.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private void aguardarBloqueioReal(long conexaoBloqueadora, Future<?> tentativa) throws Exception {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        CompletableFuture<Void> esperaObservada = new CompletableFuture<>();
        var observador = Executors.newSingleThreadScheduledExecutor();
        // A cadência apenas limita consultas; a ordem depende exclusivamente
        // da espera real no performance_schema, nunca de um sleep presumido.
        var consulta = observador.scheduleWithFixedDelay(() -> {
            try {
                Long esperas = jdbc.queryForObject("select count(*) from performance_schema.data_lock_waits w "
                        + "join performance_schema.threads t on t.THREAD_ID=w.BLOCKING_THREAD_ID "
                        + "where t.PROCESSLIST_ID=?", Long.class, conexaoBloqueadora);
                if (esperas != null && esperas >= 1) esperaObservada.complete(null);
                else if (tentativa.isDone()) esperaObservada.completeExceptionally(
                        new AssertionError("Inativação não alcançou a espera real no Contrato"));
            } catch (Throwable ex) {
                esperaObservada.completeExceptionally(ex);
            }
        }, 0, 25, TimeUnit.MILLISECONDS);
        try { esperaObservada.get(10, TimeUnit.SECONDS); }
        finally {
            consulta.cancel(true);
            observador.shutdownNow();
            observador.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    private Estado estado(long contratoId, long usuarioId) {
        return emTransacao(() -> {
            Contrato contrato = em.find(Contrato.class, contratoId);
            return new Estado(contrato.getVersion(), contrato.getAtivo(),
                    contar("EventoCicloVidaContrato", contratoId),
                    contar("VersaoContratoNegocial", contratoId));
        });
    }

    private long contar(String entidade, long contratoId) {
        return em.createQuery("select count(x) from " + entidade + " x where x.contrato.id=:c", Long.class)
                .setParameter("c", contratoId).getSingleResult();
    }

    private record Estado(long versaoContrato, boolean ativo, long eventos, long versoes) {}
}
