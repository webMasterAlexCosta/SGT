package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.entidade.ConfiguracaoSistema;
import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
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
 * Hipóteses específicas: calendário depois de Contrato, usuário depois de UPDATE
 * do alvo e configuração depois de UPDATE da chave. Não compilado/executado pelo
 * autor. Usa HTTP/JPA/PostgreSQL reais, sem mock de autorização, sem DDL manual.
 * O ajuste global de configuração usado como fixture é restaurado no finally.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class Candidato8ComandosRestantesExecutorRevogadoRedIT extends ContratosHttpBase {
    private static final String CHAVE_CONFIGURACAO = "HORAS_DIA_TRABALHO";
    @Autowired private DataSource dataSource;

    @ParameterizedTest
    @ValueSource(strings = {"CALENDARIO", "USUARIO", "CONFIGURACAO"})
    @Timeout(value = 75, unit = TimeUnit.SECONDS)
    void comandoRevalidaExecutorDepoisDaEsperaDoRecurso(String fluxo) throws Exception {
        Cenario c = criarCenarioPersistido();
        UsuarioAplicacao ator = fluxo.equals("CONFIGURACAO")
                ? emTransacao(() -> { var u = usuario(PerfilAcesso.ADMINISTRADOR, null); em.flush(); return u; })
                : c.localA;
        ConfiguracaoAnterior configuracaoAnterior = fluxo.equals("CONFIGURACAO")
                ? configuracaoAnterior() : null;
        var pool = Executors.newFixedThreadPool(2);
        CountDownLatch liberar = new CountDownLatch(1);
        CompletableFuture<Long> conexaoBloqueadora = new CompletableFuture<>();
        try {
            executar(ator, comando(fluxo, c, true), fluxo.equals("CALENDARIO") ? 201 : 200,
                    "Controle positivo: o mesmo ator altera legalmente o recurso antes da retirada de autoridade");
            JsonNode antes = estado(fluxo, c, ator);
            var pedido = autenticar(comando(fluxo, c, false), ator);
            Future<?> bloqueador = pool.submit(() -> emTransacao(() -> {
                bloquearRecurso(fluxo, c);
                conexaoBloqueadora.complete(((Number) em.createNativeQuery("select pg_backend_pid()")
                        .getSingleResult()).longValue());
                try {
                    if (!liberar.await(40, TimeUnit.SECONDS))
                        throw new AssertionError("Limite da fixture: recurso não liberado");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt(); throw new IllegalStateException(ex);
                }
                return null;
            }));
            long conexao = conexaoBloqueadora.get(10, TimeUnit.SECONDS);
            Future<MvcResult> tentativa = pool.submit(() -> mvc.perform(pedido).andReturn());
            aguardarBloqueioReal(conexao, tentativa);

            // Retira o perfil pertinente, evitando cascata de desativação de
            // subordinados que pudesse disputar o próprio usuário alvo da fixture.
            executar(c.admin, put("/api/usuarios/" + ator.getId()).contentType(APPLICATION_JSON)
                    .content(json(mapa("perfil", "GESTOR_NACIONAL"))), 200,
                    "Outro Administrador retira a autoridade do ator e confirma antes de liberar o recurso");
            emTransacao(() -> {
                UsuarioAplicacao atual = em.find(UsuarioAplicacao.class, ator.getId());
                assertThat(atual.getPerfil()).isEqualTo(PerfilAcesso.GESTOR_NACIONAL);
                assertThat(atual.getActive()).isTrue();
                return null;
            });
            assertThat(tentativa.isDone()).as("O comando continua aguardando depois do commit da revogação").isFalse();
            liberar.countDown();
            bloqueador.get(10, TimeUnit.SECONDS);
            MvcResult resultado = tentativa.get(15, TimeUnit.SECONDS);
            JsonNode depois = estado(fluxo, c, ator);
            Path evidencia = Path.of("target/auditoria-http/candidato8-comando-executor-" + fluxo + ".json");
            Files.createDirectories(evidencia.getParent());
            Files.writeString(evidencia, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapa(
                    "hipotese", "C8-TR-EXECUTOR-" + fluxo, "fluxo", fluxo,
                    "esperaMySqlObservada", true, "revogacaoCommitadaAntesDaLiberacao", true,
                    "status", resultado.getResponse().getStatus(), "antes", antes, "depois", depois)));
            assertAll("A autoridade corrente deve sobreviver à espera de escrita",
                    () -> assertThat(resultado.getResponse().getStatus()).isIn(401, 403, 404, 409),
                    () -> assertThat(depois).as("Fonte e trilha do recurso permanecem iguais após retirada da autoridade")
                            .isEqualTo(antes));
        } finally {
            liberar.countDown();
            pool.shutdownNow();
            pool.awaitTermination(10, TimeUnit.SECONDS);
            if (fluxo.equals("CONFIGURACAO")) restaurarConfiguracao(configuracaoAnterior);
        }
    }

    private AbstractMockHttpServletRequestBuilder<?> comando(String fluxo, Cenario c, boolean controle) throws Exception {
        return switch (fluxo) {
            case "CALENDARIO" -> post("/api/calendario-eventos").contentType(APPLICATION_JSON)
                    .content(json(mapa("data", controle ? DATA.minusDays(1) : DATA,
                            "tipo", "FERIADO", "abrangencia", "LOTACAO", "lotacaoId", c.lotacaoA.getId(),
                            "descricao", "Fonte sintética para conferir autoridade após espera", "ativo", true)));
            case "USUARIO" -> put("/api/usuarios/" + c.operacionalA.getId()).contentType(APPLICATION_JSON)
                    .content(json(mapa("email", (controle ? "controle-" : "alteracao-")
                            + UUID.randomUUID() + "@correios.com.br")));
            case "CONFIGURACAO" -> post("/api/sistema/configuracoes").contentType(APPLICATION_JSON)
                    .content(json(List.of(mapa("chave", CHAVE_CONFIGURACAO, "valor", controle ? "8" : "7",
                            "descricao", "Configuração sintética para conferir autoridade após espera"))));
            default -> throw new IllegalArgumentException(fluxo);
        };
    }

    private void bloquearRecurso(String fluxo, Cenario c) {
        Object recurso = switch (fluxo) {
            case "CALENDARIO" -> em.find(Contrato.class, c.contratoA.getId(), LockModeType.PESSIMISTIC_WRITE);
            case "USUARIO" -> em.find(UsuarioAplicacao.class, c.operacionalA.getId(), LockModeType.PESSIMISTIC_WRITE);
            case "CONFIGURACAO" -> em.find(ConfiguracaoSistema.class, CHAVE_CONFIGURACAO, LockModeType.PESSIMISTIC_WRITE);
            default -> throw new IllegalArgumentException(fluxo);
        };
        assertThat(recurso).isNotNull();
    }

    private JsonNode estado(String fluxo, Cenario c, UsuarioAplicacao ator) {
        return emTransacao(() -> {
            Object dados;
            String rota;
            if (fluxo.equals("CALENDARIO")) {
                dados = mapa("fontes", em.createQuery("select e.id,e.data,e.descricao,e.ativo,e.cancelado "
                                + "from CalendarioEvento e where e.gestaoLocal.id=:g order by e.id", Object[].class)
                        .setParameter("g", c.gestaoA.getId()).getResultList(),
                        "versoes", em.createQuery("select v.id,v.numeroVersao,v.tipoOperacao,v.fotografiaJson,v.hashVersao "
                                + "from VersaoCalendarioEvento v where v.calendarioEvento.gestaoLocal.id=:g order by v.id", Object[].class)
                        .setParameter("g", c.gestaoA.getId()).getResultList());
                rota = "/api/calendario-eventos";
            } else if (fluxo.equals("USUARIO")) {
                UsuarioAplicacao u = em.find(UsuarioAplicacao.class, c.operacionalA.getId());
                dados = mapa("id", u.getId(), "matricula", u.getMatricula(), "email", u.getEmail(),
                        "active", u.getActive(), "perfil", u.getPerfil(), "version", u.getVersion(),
                        "gestaoId", u.getGestaoLocal() == null ? null : u.getGestaoLocal().getId());
                rota = "/api/usuarios/" + c.operacionalA.getId();
            } else {
                ConfiguracaoSistema conf = em.find(ConfiguracaoSistema.class, CHAVE_CONFIGURACAO);
                dados = mapa("chave", conf.getChave(), "valor", conf.obterValor(), "descricao", conf.getDescricao());
                rota = "/api/sistema/configuracoes";
            }
            List<Object[]> trilha = em.createQuery("select a.id,a.action,a.entityType,a.entityId,a.detailsJson "
                            + "from RegistroAuditoria a where a.userId=:u and a.path=:p order by a.id", Object[].class)
                    .setParameter("u", ator.getId()).setParameter("p", rota).getResultList();
            return mapper.valueToTree(mapa("dados", dados, "trilhaDoRecurso", trilha));
        });
    }

    private ConfiguracaoAnterior configuracaoAnterior() {
        return emTransacao(() -> {
            ConfiguracaoSistema conf = em.find(ConfiguracaoSistema.class, CHAVE_CONFIGURACAO);
            return conf == null ? new ConfiguracaoAnterior(false, null, null)
                    : new ConfiguracaoAnterior(true, conf.obterValor(), conf.getDescricao());
        });
    }

    private void restaurarConfiguracao(ConfiguracaoAnterior anterior) {
        emTransacao(() -> {
            ConfiguracaoSistema atual = em.find(ConfiguracaoSistema.class, CHAVE_CONFIGURACAO);
            if (!anterior.existia()) { if (atual != null) em.remove(atual); }
            else if (atual == null) em.persist(new ConfiguracaoSistema(CHAVE_CONFIGURACAO, anterior.valor(), anterior.descricao()));
            else { atual.setValor(anterior.valor()); atual.setDescricao(anterior.descricao()); }
            em.flush(); return null;
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
                        new AssertionError("O comando não alcançou a espera real do recurso"));
            } catch (Throwable ex) { observada.completeExceptionally(ex); }
        }, 0, 25, TimeUnit.MILLISECONDS);
        try { observada.get(10, TimeUnit.SECONDS); }
        finally {
            consulta.cancel(true); observador.shutdownNow(); observador.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    private record ConfiguracaoAnterior(boolean existia, String valor, String descricao) { }
}
