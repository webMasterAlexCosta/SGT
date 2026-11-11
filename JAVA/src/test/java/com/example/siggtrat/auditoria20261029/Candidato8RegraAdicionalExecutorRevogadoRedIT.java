package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.entidade.RegraAdicional;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Autoria: revisor independente /root/revisao_final_transversais.
 * Fonte lida: C8 cdf471fbe344f4eb4e096a5e86b6f71226fe3ad3cde040baa6d6d4d4812e5545.
 * Hipótese: obterJornadaGerenciavel autoriza antes do UPDATE da RegraAdicional;
 * a espera desse UPDATE não deve permitir commit após retirada da autoridade.
 * HTTP/JPA/PostgreSQL reais; sem mock, DDL manual ou execução pelo autor.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class Candidato8RegraAdicionalExecutorRevogadoRedIT extends ContratosHttpBase {
    @Autowired private DataSource dataSource;

    @Test
    @Timeout(value = 75, unit = TimeUnit.SECONDS)
    void atualizarRegraRevalidaExecutorDepoisDaEsperaDoAlvo() throws Exception {
        Cenario c = criarCenarioPersistido();
        JsonNode criada = dados(executar(c.localA,
                post("/api/jornadas/{id}/regras-adicionais", c.jornadaA.getId())
                        .contentType(APPLICATION_JSON).content(corpo(20)), 201,
                "Regra sintética própria para controle de autorização após espera"));
        long regraId = criada.path("id").asLong();
        assertThat(regraId).isPositive();
        JsonNode controle = dados(executar(c.localA,
                put("/api/regras-adicionais/{id}", regraId)
                        .contentType(APPLICATION_JSON).content(corpo(25)), 200,
                "Controle positivo: o mesmo gestor atualiza legalmente a mesma regra"));
        assertThat(controle.path("percentual").decimalValue()).isEqualByComparingTo("25");
        JsonNode antes = estado(regraId, c.localA.getId());
        var pedido = autenticar(put("/api/regras-adicionais/{id}", regraId)
                .contentType(APPLICATION_JSON).content(corpo(35)), c.localA);

        var pool = Executors.newFixedThreadPool(2);
        CountDownLatch liberar = new CountDownLatch(1);
        CompletableFuture<Long> conexaoBloqueadora = new CompletableFuture<>();
        try {
            Future<?> bloqueador = pool.submit(() -> emTransacao(() -> {
                assertThat(em.find(RegraAdicional.class, regraId, LockModeType.PESSIMISTIC_WRITE)).isNotNull();
                conexaoBloqueadora.complete(((Number) em.createNativeQuery("select pg_backend_pid()")
                        .getSingleResult()).longValue());
                try {
                    if (!liberar.await(40, TimeUnit.SECONDS))
                        throw new AssertionError("Limite da fixture: RegraAdicional não liberada");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt(); throw new IllegalStateException(ex);
                }
                return null;
            }));
            long conexao = conexaoBloqueadora.get(10, TimeUnit.SECONDS);
            Future<MvcResult> tentativa = pool.submit(() -> mvc.perform(pedido).andReturn());
            aguardarBloqueioReal(conexao, tentativa);

            executar(c.admin, put("/api/usuarios/" + c.localA.getId()).contentType(APPLICATION_JSON)
                    .content(json(mapa("perfil", "GESTOR_NACIONAL"))), 200,
                    "Outro Administrador confirma a retirada do perfil de Gestão Local antes de liberar a regra");
            emTransacao(() -> {
                UsuarioAplicacao atual = em.find(UsuarioAplicacao.class, c.localA.getId());
                assertThat(atual.getPerfil()).isEqualTo(PerfilAcesso.GESTOR_NACIONAL);
                assertThat(atual.getActive()).isTrue();
                return null;
            });
            assertThat(tentativa.isDone()).as("A atualização ainda aguarda após o commit da revogação").isFalse();
            liberar.countDown();
            bloqueador.get(10, TimeUnit.SECONDS);
            MvcResult resultado = tentativa.get(15, TimeUnit.SECONDS);
            JsonNode depois = estado(regraId, c.localA.getId());
            Path evidencia = Path.of("target/auditoria-http/candidato8-regra-adicional-executor.json");
            Files.createDirectories(evidencia.getParent());
            Files.writeString(evidencia, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapa(
                    "hipotese", "C8-TR-EXECUTOR-REGRA-ADICIONAL", "regraId", regraId,
                    "esperaMySqlObservada", true, "revogacaoCommitadaAntesDaLiberacao", true,
                    "status", resultado.getResponse().getStatus(), "antes", antes, "depois", depois)));
            assertAll("A atualização da regra exige autoridade corrente após esperar pelo alvo",
                    () -> assertThat(resultado.getResponse().getStatus()).isIn(401, 403, 404, 409),
                    () -> assertThat(depois).as("Regra, versão e trilha não mudam após retirada da autoridade")
                            .isEqualTo(antes));
        } finally {
            liberar.countDown();
            pool.shutdownNow();
            pool.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private String corpo(int percentual) throws Exception {
        return json(mapa("tipo", "ADICIONAL_TESTE", "inicio", "22:00", "fim", "05:00",
                "percentual", percentual, "ativo", true, "ordem", 1));
    }

    private JsonNode estado(long regraId, Long atorId) {
        return emTransacao(() -> {
            RegraAdicional r = em.find(RegraAdicional.class, regraId);
            assertThat(r).isNotNull();
            Object dados = mapa("id", r.getId(), "jornadaId", r.getJornada().getId(), "tipo", r.getTipo(),
                    "inicio", r.getInicio(), "fim", r.getFim(), "percentual", r.getPercentual(),
                    "ativo", r.getAtivo(), "ordem", r.getOrdem(), "version", r.getVersion());
            var trilha = em.createQuery("select a.id,a.action,a.entityType,a.entityId,a.detailsJson "
                            + "from RegistroAuditoria a where a.userId=:u and a.path=:p order by a.id", Object[].class)
                    .setParameter("u", atorId).setParameter("p", "/api/regras-adicionais/" + regraId)
                    .getResultList();
            return mapper.valueToTree(mapa("dados", dados, "trilhaDoRecurso", trilha));
        });
    }

    private void aguardarBloqueioReal(long conexaoBloqueadora, Future<?> tentativa) throws Exception {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        CompletableFuture<Void> observada = new CompletableFuture<>();
        var observador = Executors.newSingleThreadScheduledExecutor();
        var consulta = observador.scheduleWithFixedDelay(() -> {
            try {
                Long esperas = jdbc.queryForObject("""
SELECT count(*) FROM pg_stat_activity a WHERE a.datname=current_database() AND ? = ANY(pg_blocking_pids(a.pid))
""", Long.class, conexaoBloqueadora);
                if (esperas != null && esperas > 0) observada.complete(null);
                else if (tentativa.isDone()) observada.completeExceptionally(
                        new AssertionError("A atualização não alcançou a espera real da RegraAdicional"));
            } catch (Throwable ex) { observada.completeExceptionally(ex); }
        }, 0, 25, TimeUnit.MILLISECONDS);
        try { observada.get(10, TimeUnit.SECONDS); }
        finally {
            consulta.cancel(true); observador.shutdownNow(); observador.awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
