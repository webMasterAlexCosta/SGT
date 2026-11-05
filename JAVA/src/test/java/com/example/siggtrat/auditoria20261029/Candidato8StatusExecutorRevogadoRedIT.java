package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.entidade.StatusEmpregado;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
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
 * C8-PES-H03. Autoria independente: /root/revisao_final_pessoas.
 * Fonte C8 cdf471fbe344f4eb4e096a5e86b6f71226fe3ad3cde040baa6d6d4d4812e5545.
 * Fronteira administrativa concreta de uma das 77 rotas: preAuthorize inicial
 * não concede autorização permanente para concluir após revogação commitada.
 * Status sintético novo, não estrutural e sem empregados; nenhuma fonte histórica
 * é modificada. Outro Administrador ativo confirma a revogação por HTTP.
 * Proposta não compilada/executada pelo revisor; a execução será central.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@TestPropertySource(properties = {"spring.cache.type=redis", "spring.sql.init.mode=never",
        "app.cache.redis.http-get.enabled=true"})
class Candidato8StatusExecutorRevogadoRedIT extends ContratosHttpBase {
    @Autowired private DataSource dataSource;

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void atualizarStatusRevalidaAdministradorDepoisDaEsperaNoAlvo() throws Exception {
        try (var conexao = dataSource.getConnection()) {
            assertThat(conexao.getMetaData().getDatabaseProductName()).isEqualTo("MySQL");
            assertThat(conexao.getMetaData().getDatabaseProductVersion()).isEqualTo("8.0.45");
        }
        Cenario c = criarCenarioPersistido();
        UsuarioAplicacao executor = emTransacao(() -> usuario(PerfilAcesso.ADMINISTRADOR, null));
        String sufixo = UUID.randomUUID().toString().substring(0, 8).toUpperCase(java.util.Locale.ROOT);
        long id = dados(executar(executor, post("/api/status-empregados").contentType(MediaType.APPLICATION_JSON)
                        .content(json(mapa("descricao", "C8-S-" + sufixo))), 200,
                "Controle: Administrador cria status não estrutural sem dependências")).path("id").asLong();
        assertThat(id).isPositive();
        String rota = "/api/status-empregados/" + id;
        executar(executor, put(rota).contentType(MediaType.APPLICATION_JSON)
                        .content(json(mapa("descricao", "C8-CONTROLE-" + sufixo))), 200,
                "Controle positivo: o mesmo PUT confirma alteração autorizada antes da corrida");
        Estado antes = estado(id);
        assertThat(antes.descricao()).isEqualTo("C8-CONTROLE-" + sufixo);
        assertThat(antes.empregados()).isZero();
        var request = autenticar(put(rota).contentType(MediaType.APPLICATION_JSON)
                .content(json(mapa("descricao", "C8-REVOGADO-" + sufixo))), executor);
        var pool = Executors.newFixedThreadPool(2);
        CountDownLatch liberarStatus = new CountDownLatch(1);
        CompletableFuture<Long> conexaoBloqueadora = new CompletableFuture<>();
        try {
            Future<?> bloqueador = pool.submit(() -> emTransacao(() -> {
                em.find(StatusEmpregado.class, id, LockModeType.PESSIMISTIC_WRITE);
                conexaoBloqueadora.complete(((Number) em.createNativeQuery("select connection_id()")
                        .getSingleResult()).longValue());
                try {
                    if (!liberarStatus.await(30, TimeUnit.SECONDS))
                        throw new AssertionError("Limite da fixture: Status não liberado");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt(); throw new IllegalStateException(ex);
                }
                return null;
            }));
            long bloqueadora = conexaoBloqueadora.get(10, TimeUnit.SECONDS);
            Future<MvcResult> tentativa = pool.submit(() -> mvc.perform(request).andReturn());
            aguardarBloqueioReal(bloqueadora, tentativa);
            executar(c.admin, put("/api/usuarios/" + executor.getId()).contentType(MediaType.APPLICATION_JSON)
                            .content(json(mapa("active", false))), 200,
                    "Outro Administrador revoga o executor enquanto a atualização do Status aguarda lock real");
            assertThat(emTransacao(() -> {
                em.clear();
                return em.find(UsuarioAplicacao.class, executor.getId()).getActive();
            })).as("Revogação commitada e relida em transação independente").isFalse();
            assertThat(tentativa.isDone()).as("Escrita ainda espera após a revogação commitada").isFalse();
            liberarStatus.countDown(); bloqueador.get(10, TimeUnit.SECONDS);
            MvcResult resposta = tentativa.get(15, TimeUnit.SECONDS);
            Estado depois = estado(id);
            Path evidencia = Path.of("target/auditoria-http/candidato8-status-executor-revogado.json");
            Files.createDirectories(evidencia.getParent());
            Files.writeString(evidencia, json(mapa("hipotese", "C8-PES-H03",
                    "status", resposta.getResponse().getStatus(), "estadoAntes", antes, "estadoDepois", depois,
                    "espera_mysql_observada", true, "revogacao_commitou_antes_da_liberacao", true)));
            assertAll("A atualização administrativa exige executor ainda autorizado após a espera",
                    () -> assertThat(resposta.getResponse().getStatus()).isIn(401, 403, 404, 409),
                    () -> assertThat(depois).as("Status, versão e dependências permanecem idênticos").isEqualTo(antes));
        } finally {
            liberarStatus.countDown(); pool.shutdownNow(); pool.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private Estado estado(long id) {
        return emTransacao(() -> {
            em.clear();
            StatusEmpregado status = em.find(StatusEmpregado.class, id);
            assertThat(status).isNotNull();
            long empregados = em.createQuery("select count(e) from Empregado e where e.status.id=:id", Long.class)
                    .setParameter("id", id).getSingleResult();
            return new Estado(status.getId(), status.getVersion(), status.getDescricao(), empregados);
        });
    }

    private void aguardarBloqueioReal(long bloqueadora, Future<?> tentativa) throws Exception {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        CompletableFuture<Void> observada = new CompletableFuture<>();
        var observador = Executors.newSingleThreadScheduledExecutor();
        var consulta = observador.scheduleWithFixedDelay(() -> {
            try {
                Long esperas = jdbc.queryForObject("select count(*) from performance_schema.data_lock_waits w "
                        + "join performance_schema.threads t on t.THREAD_ID=w.BLOCKING_THREAD_ID "
                        + "where t.PROCESSLIST_ID=?", Long.class, bloqueadora);
                if (esperas != null && esperas > 0) observada.complete(null);
                else if (tentativa.isDone()) observada.completeExceptionally(
                        new AssertionError("Comando não alcançou a espera real no Status"));
            } catch (Throwable ex) { observada.completeExceptionally(ex); }
        }, 0, 25, TimeUnit.MILLISECONDS);
        try { observada.get(10, TimeUnit.SECONDS); }
        finally { consulta.cancel(true); observador.shutdownNow(); observador.awaitTermination(5, TimeUnit.SECONDS); }
    }

    private record Estado(long id, long versao, String descricao, long empregados) {}
}
