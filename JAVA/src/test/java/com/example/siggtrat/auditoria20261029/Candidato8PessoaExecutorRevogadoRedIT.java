package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.Empregado;
import com.example.siggtrat.entidade.Pessoa;
import com.example.siggtrat.entidade.UsuarioAplicacao;
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
 * C8-PES-H01. Autoria independente: /root/revisao_final_pessoas.
 * Fonte C8 cdf471fbe344f4eb4e096a5e86b6f71226fe3ad3cde040baa6d6d4d4812e5545.
 * COL-006:002: a correção excepcional exige executor ainda autorizado quando a
 * escrita prossegue. Opcionais é controle adjacente da mesma fronteira de Pessoa.
 * Coordenação derivada de C4-PES-H04; sem mock, sem modificar fonte histórica.
 * Proposta não compilada/executada pelo revisor; a execução será central.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@TestPropertySource(properties = {"spring.cache.type=redis", "spring.sql.init.mode=never",
        "app.cache.redis.http-get.enabled=true"})
class Candidato8PessoaExecutorRevogadoRedIT extends ContratosHttpBase {
    @Autowired private DataSource dataSource;

    @ParameterizedTest
    @ValueSource(strings = {"CPF", "OPCIONAIS"})
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void pessoaRevalidaExecutorDepoisDaEsperaAntesDeCorrigirIdentidadeOuOpcionais(String operacao) throws Exception {
        try (var conexao = dataSource.getConnection()) {
            assertThat(conexao.getMetaData().getDatabaseProductName()).isEqualTo("PostgreSQL");
            assertThat(conexao.getMetaData().getDatabaseProductVersion()).matches("18\\.6(?: \\(Debian [^()]+\\))?");
        }
        Cenario c = criarCenarioPersistido();
        long pessoaId = c.pessoaA.getId();
        String rota = "/api/pessoas/" + pessoaId;
        Map<String, Object> tentativaBody;
        if (operacao.equals("CPF")) {
            String controle = cpf(Math.toIntExact(933000000L + pessoaId * 10L + 1L));
            String tentativa = cpf(Math.toIntExact(933000000L + pessoaId * 10L + 2L));
            exigirCpfLivre(controle); exigirCpfLivre(tentativa);
            enviar(c.localA, rota + "/correcoes-identidade", correcao(controle), 200,
                    "Controle positivo: Gestor Local ativo confirma correção legítima de CPF pelo mesmo endpoint");
            assertThat(estado(c).cpf()).isEqualTo(controle);
            tentativaBody = correcao(tentativa);
        } else {
            executar(c.localA, put(rota + "/dados-opcionais").contentType(MediaType.APPLICATION_JSON)
                            .content(json(mapa("rg", "RG-C8-CONTROLE-" + pessoaId))), 200,
                    "Controle positivo: atualização opcional válida confirma e produz seu histórico");
            assertThat(estado(c).rg()).isEqualTo("RG-C8-CONTROLE-" + pessoaId);
            tentativaBody = mapa("rg", "RG-C8-REVOGADO-" + pessoaId);
        }
        Estado antes = estado(c);
        var pedido = operacao.equals("CPF")
                ? autenticar(post(rota + "/correcoes-identidade").contentType(MediaType.APPLICATION_JSON)
                    .content(json(tentativaBody)), c.localA)
                : autenticar(put(rota + "/dados-opcionais").contentType(MediaType.APPLICATION_JSON)
                    .content(json(tentativaBody)), c.localA);
        var pool = Executors.newFixedThreadPool(2);
        CountDownLatch liberarPessoa = new CountDownLatch(1);
        CompletableFuture<Long> conexaoBloqueadora = new CompletableFuture<>();
        try {
            Future<?> bloqueador = pool.submit(() -> emTransacao(() -> {
                em.find(Pessoa.class, pessoaId, LockModeType.PESSIMISTIC_WRITE);
                conexaoBloqueadora.complete(((Number) em.createNativeQuery("select pg_backend_pid()")
                        .getSingleResult()).longValue());
                try {
                    if (!liberarPessoa.await(30, TimeUnit.SECONDS))
                        throw new AssertionError("Limite da fixture: Pessoa não liberada");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt(); throw new IllegalStateException(ex);
                }
                return null;
            }));
            long bloqueadora = conexaoBloqueadora.get(10, TimeUnit.SECONDS);
            Future<MvcResult> tentativa = pool.submit(() -> mvc.perform(pedido).andReturn());
            aguardarBloqueioReal(bloqueadora, tentativa);
            executar(c.admin, put("/api/usuarios/" + c.localA.getId()).contentType(MediaType.APPLICATION_JSON)
                            .content(json(mapa("active", false))), 200,
                    "Admin desativa o executor por HTTP enquanto a escrita de Pessoa aguarda lock real");
            assertThat(emTransacao(() -> {
                em.clear();
                return em.find(UsuarioAplicacao.class, c.localA.getId()).getActive();
            })).as("Revogação commitada e relida em transação independente").isFalse();
            assertThat(tentativa.isDone()).as("Escrita ainda espera depois do commit da revogação").isFalse();
            liberarPessoa.countDown(); bloqueador.get(10, TimeUnit.SECONDS);
            MvcResult resposta = tentativa.get(15, TimeUnit.SECONDS);
            Estado depois = estado(c);
            Path evidencia = Path.of("target/auditoria-http/candidato8-pessoa-executor-"
                    + operacao.toLowerCase(Locale.ROOT) + ".json");
            Files.createDirectories(evidencia.getParent());
            Files.writeString(evidencia, json(mapa("hipotese", "C8-PES-H01", "operacao", operacao,
                    "status", resposta.getResponse().getStatus(), "estadoAntes", antes, "estadoDepois", depois,
                    "espera_mysql_observada", true, "revogacao_commitou_antes_da_liberacao", true)));
            assertAll("A escrita exige executor ainda autorizado depois de aguardar a Pessoa",
                    () -> assertThat(resposta.getResponse().getStatus()).isIn(401, 403, 404, 409),
                    () -> assertThat(depois).as("Pessoa, projeção, correções e história opcional não mudam")
                            .isEqualTo(antes));
        } finally {
            liberarPessoa.countDown(); pool.shutdownNow(); pool.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private Map<String, Object> correcao(String cpf) {
        return mapa("tipo", "CPF", "novoValor", cpf,
                "motivo", "Correção material sintética documentada para verificar autorização corrente",
                "evidencia", "SEI-SINTETICO-C8-PESSOA-REVOGACAO", "autorizacao", "ATO-SINTETICO-C8",
                "impactosEncontrados", "Preservar a identidade canônica, projeção e toda a trilha anterior");
    }

    private void exigirCpfLivre(String cpf) {
        emTransacao(() -> {
            for (String entidade : new String[]{"Pessoa", "Empregado"})
                assertThat(em.createQuery("select count(p) from " + entidade + " p where p.cpf=:cpf", Long.class)
                        .setParameter("cpf", cpf).getSingleResult()).isZero();
            return null;
        });
    }

    private Estado estado(Cenario c) {
        return emTransacao(() -> {
            em.clear();
            Pessoa p = em.find(Pessoa.class, c.pessoaA.getId());
            Empregado e = em.find(Empregado.class, c.empregadoA.getId());
            long correcoes = em.createQuery("select count(x) from CorrecaoPessoa x where x.pessoa.id=:p", Long.class)
                    .setParameter("p", p.getId()).getSingleResult();
            long historicos = em.createQuery("select count(x) from HistoricoEmpregado x where x.empregado.id=:e", Long.class)
                    .setParameter("e", e.getId()).getSingleResult();
            return new Estado(p.getId(), p.getVersion(), p.getCpf(), p.getNome(), p.getRg(),
                    e.getId(), e.getVersion(), e.getCpf(), e.getNome(), correcoes, historicos);
        });
    }

    private void aguardarBloqueioReal(long bloqueadora, Future<?> tentativa) throws Exception {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        CompletableFuture<Void> observada = new CompletableFuture<>();
        var observador = Executors.newSingleThreadScheduledExecutor();
        var consulta = observador.scheduleWithFixedDelay(() -> {
            try {
                Long esperas = jdbc.queryForObject("""
SELECT count(*) FROM pg_stat_activity a WHERE a.datname=current_database() AND ? = ANY(pg_blocking_pids(a.pid))
""", Long.class, bloqueadora);
                if (esperas != null && esperas > 0) observada.complete(null);
                else if (tentativa.isDone()) observada.completeExceptionally(
                        new AssertionError("Comando não alcançou a espera real na Pessoa"));
            } catch (Throwable ex) { observada.completeExceptionally(ex); }
        }, 0, 25, TimeUnit.MILLISECONDS);
        try { observada.get(10, TimeUnit.SECONDS); }
        finally { consulta.cancel(true); observador.shutdownNow(); observador.awaitTermination(5, TimeUnit.SECONDS); }
    }

    private record Estado(long pessoaId, long versaoPessoa, String cpf, String nome, String rg,
                          long empregadoId, long versaoEmpregado, String cpfEmpregado, String nomeEmpregado,
                          long correcoes, long historicosOpcionais) {}
}
