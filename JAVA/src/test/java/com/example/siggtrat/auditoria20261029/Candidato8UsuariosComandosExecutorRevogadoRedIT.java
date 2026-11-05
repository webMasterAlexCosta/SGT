package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.utilitario.UtilitarioHashSeguranca;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Autoria: revisor independente /root/revisao_final_transversais.
 * Base C8 cdf471fbe344f4eb4e096a5e86b6f71226fe3ad3cde040baa6d6d4d4812e5545.
 * Cinco callsites mutáveis de obterUsuarioOuFalhar ainda autorizam antes do
 * UPDATE do alvo. Cada parâmetro possui controle positivo e espera MySQL real.
 * A revogação usa outro Admin, com commit conferido antes da liberação do alvo.
 * Nenhum DDL, mock, serviço externo ou execução Java pelo autor.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class Candidato8UsuariosComandosExecutorRevogadoRedIT extends ContratosHttpBase {
    @Autowired private DataSource dataSource;

    @ParameterizedTest(name = "comandoUsuario={0}")
    @ValueSource(strings = {"CONCEDER_RETROATIVO", "REVOGAR_RETROATIVO", "REDEFINIR_SENHA", "ATIVAR", "EXCLUIR"})
    @Timeout(value = 75, unit = TimeUnit.SECONDS)
    void comandoAdjacenteRevalidaExecutorDepoisDaEsperaDoUsuarioAlvo(String fluxo) throws Exception {
        Cenario c = criarCenarioPersistido();
        long alvoId = c.operacionalA.getId();
        prepararControlePositivo(c, alvoId, fluxo);
        String rota = rota(alvoId, fluxo);
        JsonNode antes = estado(alvoId, c.localA.getId(), rota);
        var pedido = autenticar(pedido(alvoId, fluxo, "SenhaTentativaRevogada@2026"), c.localA);

        var pool = Executors.newFixedThreadPool(2);
        CountDownLatch liberar = new CountDownLatch(1);
        CompletableFuture<Long> conexaoBloqueadora = new CompletableFuture<>();
        try {
            Future<?> bloqueador = pool.submit(() -> emTransacao(() -> {
                assertThat(em.find(UsuarioAplicacao.class, alvoId, LockModeType.PESSIMISTIC_WRITE)).isNotNull();
                conexaoBloqueadora.complete(((Number) em.createNativeQuery("select connection_id()")
                        .getSingleResult()).longValue());
                try {
                    if (!liberar.await(40, TimeUnit.SECONDS))
                        throw new AssertionError("Limite da fixture: usuário alvo não liberado");
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
                    "Outro Admin retira a autoridade local antes de liberar a escrita do usuário alvo");
            emTransacao(() -> {
                em.clear();
                UsuarioAplicacao atual = em.find(UsuarioAplicacao.class, c.localA.getId());
                assertThat(atual.getPerfil()).isEqualTo(PerfilAcesso.GESTOR_NACIONAL);
                assertThat(atual.getActive()).isTrue();
                return null;
            });
            assertThat(tentativa.isDone()).as("O comando ainda aguarda após o commit da revogação").isFalse();
            liberar.countDown();
            bloqueador.get(10, TimeUnit.SECONDS);
            MvcResult resultado = tentativa.get(15, TimeUnit.SECONDS);
            JsonNode depois = estado(alvoId, c.localA.getId(), rota);
            Path evidencia = Path.of("target/auditoria-http/candidato8-usuario-comando-executor-" + fluxo + ".json");
            Files.createDirectories(evidencia.getParent());
            Files.writeString(evidencia, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapa(
                    "hipotese", "C8-TR-EXECUTOR-USUARIO-ADJACENTE", "fluxo", fluxo, "usuarioAlvoId", alvoId,
                    "esperaMySqlObservada", true, "revogacaoCommitadaAntesDaLiberacao", true,
                    "status", resultado.getResponse().getStatus(), "antes", antes, "depois", depois)));
            assertAll("O comando exige autoridade corrente após esperar pelo usuário alvo: " + fluxo,
                    () -> assertThat(resultado.getResponse().getStatus()).isIn(401, 403, 404, 409),
                    () -> assertThat(depois).as("Conta, senha, prazo, versão, sessões e trilha ficam inalterados")
                            .isEqualTo(antes));
        } finally {
            liberar.countDown();
            pool.shutdownNow();
            pool.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private void prepararControlePositivo(Cenario c, long alvoId, String fluxo) throws Exception {
        String base = "/api/usuarios/" + alvoId;
        switch (fluxo) {
            case "CONCEDER_RETROATIVO", "REVOGAR_RETROATIVO" -> {
                executar(c.localA, post(base + "/conceder-acesso-retroativo"), 200,
                        "Controle positivo: o Gestor concede permissão ao Operacional da própria Gestão");
                conferirPrazo(alvoId, true);
                executar(c.localA, post(base + "/revogar-acesso-retroativo"), 200,
                        "Controle positivo: o Gestor revoga a mesma permissão");
                conferirPrazo(alvoId, false);
                if (fluxo.equals("REVOGAR_RETROATIVO")) {
                    executar(c.localA, post(base + "/conceder-acesso-retroativo"), 200,
                            "Preparação: permissão válida antes da tentativa concorrente de revogação");
                    conferirPrazo(alvoId, true);
                }
            }
            case "REDEFINIR_SENHA" -> {
                executar(c.localA, pedido(alvoId, fluxo, "SenhaControlePermitida@2026"), 200,
                        "Controle positivo: o Gestor redefine legalmente a senha do seu Operacional");
                emTransacao(() -> {
                    em.clear();
                    UsuarioAplicacao alvo = em.find(UsuarioAplicacao.class, alvoId);
                    assertThat(passwordEncoder.matches("SenhaControlePermitida@2026", alvo.getPassword())).isTrue();
                    assertThat(alvo.getForceChangePassword()).isTrue();
                    return null;
                });
            }
            case "ATIVAR", "EXCLUIR" -> {
                executar(c.localA, delete(base), 200,
                        "Controle positivo: desativação lógica do Operacional pela própria Gestão");
                conferirAtivo(alvoId, false);
                executar(c.localA, post(base + "/ativar"), 200,
                        "Controle positivo: a Gestão responsável reativa o Operacional");
                conferirAtivo(alvoId, true);
                if (fluxo.equals("ATIVAR")) {
                    executar(c.localA, delete(base), 200,
                            "Preparação: conta inativa antes da tentativa concorrente de ativação");
                    conferirAtivo(alvoId, false);
                }
            }
            default -> throw new IllegalArgumentException(fluxo);
        }
    }

    private void conferirPrazo(long id, boolean concedido) {
        emTransacao(() -> {
            em.clear();
            var prazo = em.find(UsuarioAplicacao.class, id).getRetroactiveUnlockExpireAt();
            if (concedido) assertThat(prazo).isNotNull();
            else assertThat(prazo).isNull();
            return null;
        });
    }

    private void conferirAtivo(long id, boolean ativo) {
        emTransacao(() -> {
            em.clear();
            assertThat(em.find(UsuarioAplicacao.class, id).getActive()).isEqualTo(ativo);
            return null;
        });
    }

    private String rota(long id, String fluxo) {
        return "/api/usuarios/" + id + switch (fluxo) {
            case "CONCEDER_RETROATIVO" -> "/conceder-acesso-retroativo";
            case "REVOGAR_RETROATIVO" -> "/revogar-acesso-retroativo";
            case "REDEFINIR_SENHA" -> "/redefinir-senha";
            case "ATIVAR" -> "/ativar";
            case "EXCLUIR" -> "";
            default -> throw new IllegalArgumentException(fluxo);
        };
    }

    private MockHttpServletRequestBuilder pedido(long id, String fluxo, String novaSenha) throws Exception {
        if (fluxo.equals("EXCLUIR")) return delete(rota(id, fluxo));
        var pedido = post(rota(id, fluxo));
        if (fluxo.equals("REDEFINIR_SENHA"))
            pedido.contentType(APPLICATION_JSON).content(json(mapa("newPassword", novaSenha)));
        return pedido;
    }

    private JsonNode estado(long alvoId, Long atorId, String rota) {
        return emTransacao(() -> {
            em.clear();
            UsuarioAplicacao alvo = em.find(UsuarioAplicacao.class, alvoId);
            assertThat(alvo).isNotNull();
            Object dados = mapa("id", alvo.getId(), "matricula", alvo.getMatricula(), "email", alvo.getEmail(),
                    "active", alvo.getActive(), "perfil", alvo.getPerfil(), "version", alvo.getVersion(),
                    "gestaoId", alvo.getGestaoLocal().getId(),
                    "retroactiveUnlockExpireAt", alvo.getRetroactiveUnlockExpireAt(),
                    "senhaPersistidaDigest", UtilitarioHashSeguranca.sha256Hexadecimal(alvo.getPassword()),
                    "forceChangePassword", alvo.getForceChangePassword(),
                    "failedAttempts", alvo.getFailedAttempts(), "lockoutEnd", alvo.getLockoutEnd());
            var sessoes = em.createQuery("select t.id from TokenAtivo t where t.username=:u order by t.id", Long.class)
                    .setParameter("u", alvo.getUsername()).getResultList();
            var trilha = em.createQuery("select a.id,a.action,a.entityType,a.entityId,a.detailsJson "
                            + "from RegistroAuditoria a where a.userId=:u and a.path=:p order by a.id", Object[].class)
                    .setParameter("u", atorId).setParameter("p", rota).getResultList();
            return mapper.valueToTree(mapa("dados", dados, "sessoesAlvoIds", sessoes, "trilhaDoRecurso", trilha));
        });
    }

    private void aguardarBloqueioReal(long conexaoBloqueadora, Future<?> tentativa) throws Exception {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        CompletableFuture<Void> observada = new CompletableFuture<>();
        var observador = Executors.newSingleThreadScheduledExecutor();
        var consulta = observador.scheduleWithFixedDelay(() -> {
            try {
                Long esperas = jdbc.queryForObject("select count(*) from performance_schema.data_lock_waits w "
                        + "join performance_schema.threads t on t.THREAD_ID=w.BLOCKING_THREAD_ID "
                        + "where t.PROCESSLIST_ID=?", Long.class, conexaoBloqueadora);
                if (esperas != null && esperas > 0) observada.complete(null);
                else if (tentativa.isDone()) observada.completeExceptionally(
                        new AssertionError("O comando não alcançou a espera real do usuário alvo"));
            } catch (Throwable ex) { observada.completeExceptionally(ex); }
        }, 0, 25, TimeUnit.MILLISECONDS);
        try { observada.get(10, TimeUnit.SECONDS); }
        finally {
            consulta.cancel(true); observador.shutdownNow(); observador.awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
