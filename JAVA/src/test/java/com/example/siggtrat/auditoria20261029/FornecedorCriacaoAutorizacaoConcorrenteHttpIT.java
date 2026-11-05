package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.Fornecedor;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Adjacência delimitada pela revisão de Contratos; teste independente de Pessoas.
 * Fonte C10 e58cd276bad1395ce910dd9fc466c50031d1579f12e8a69747ccfadcdfb95b99.
 * O POST de Fornecedor pode esperar na UNIQUE de CNPJ depois da autorização HTTP.
 * A barreira bloqueia a linha sintética de controle já confirmada, troca seu
 * CNPJ apenas na transação concorrente e sempre desfaz essa troca por rollback.
 * Isso materializa o lock da linha existente; nenhuma fonte histórica participa.
 * A premissa exige espera física observada, desativação HTTP por outro Admin
 * efetivamente commitada e rollback da barreira antes da avaliação da tentativa.
 * Falha dessas premissas é preparação insuficiente, não confirmação do defeito.
 * Preserva o predicado físico e os oráculos anteriores. A presença do índice
 * único é premissa explícita; revogação só ocorre depois da espera observada.
 * Autor /root/revisao_final_pessoas; execução central posterior pela raiz.
 */
/**
 * Proposta permanente recuperada em 2026-10-31 por /root/revisao_autorizacao.
 * Origem preservada: red_estrutura_hierarquia_diagnostico/Candidato10FornecedorCriacaoExecutorRevogadoV2RedIT.java.
 * A execução RED/GREEN desta proposta cabe à coordenação central; não foi executada pelo autor.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@TestPropertySource(properties = {"spring.cache.type=redis", "spring.sql.init.mode=never",
        "app.cache.redis.http-get.enabled=true"})
class FornecedorCriacaoAutorizacaoConcorrenteHttpIT extends ContratosHttpBase {
    private static final String ROTA = "/api/fornecedores";
    @Autowired private DataSource dataSource;

    @Test
    @Timeout(value = 75, unit = TimeUnit.SECONDS)
    void criacaoRevalidaExecutorAposEsperaNoCnpjDesfeitoPorRollback() throws Exception {
        try (var conexao = dataSource.getConnection()) {
            assertThat(conexao.getMetaData().getDatabaseProductName()).isEqualTo("MySQL");
        }
        exigirIndiceUnico();
        Cenario c = criarCenarioPersistido();
        assertThat(c.admin.getId()).isNotEqualTo(c.localA.getId());
        String cnpjControle = cnpjSinteticoLivre();
        String identificador = UUID.randomUUID().toString();
        String nomeControle = "Controle permitido C10 " + identificador;
        String nomeTentativa = "Tentativa revogada C10 " + identificador;

        JsonNode controle = enviar(c.localA, ROTA,
                mapa("razaoSocial", nomeControle, "cnpj", cnpjControle, "email", "controle@example.test"),
                200, "Controle positivo separado: Gestor Local cria fornecedor pelo mesmo POST");
        long controleId = controle.path("id").asLong();
        assertThat(controleId).isPositive();
        String cnpjTentativa = cnpjSinteticoLivre();
        assertThat(cnpjTentativa).isNotEqualTo(cnpjControle);
        JsonNode antes = estado(controleId, cnpjTentativa);
        assertThat(antes.path("controle").path("razaoSocial").asText()).isEqualTo(nomeControle);
        assertThat(antes.path("controle").path("cnpj").asText()).isEqualTo(cnpjControle);
        assertThat(antes.path("tentativas")).isEmpty();
        var pedido = autenticar(post(ROTA).contentType(APPLICATION_JSON).content(json(
                mapa("razaoSocial", nomeTentativa, "cnpj", cnpjTentativa, "email", "tentativa@example.test"))), c.localA);

        var pool = Executors.newFixedThreadPool(3);
        CountDownLatch liberar = new CountDownLatch(1);
        CompletableFuture<Barreira> preparada = new CompletableFuture<>();
        try {
            Future<Barreira> bloqueador = pool.submit(() -> new TransactionTemplate(transactionManager).execute(status -> {
                try {
                    Fornecedor controleBloqueado = em.find(Fornecedor.class, controleId, LockModeType.PESSIMISTIC_WRITE);
                    assertThat(controleBloqueado).as("A linha sintética de controle existe antes da barreira").isNotNull();
                    assertThat(controleBloqueado.getCnpj()).isEqualTo(cnpjControle);
                    int alteradas = em.createQuery(
                                    "update Fornecedor set cnpj=:cnpj where id=:id")
                            .setParameter("cnpj", cnpjTentativa).setParameter("id", controleId).executeUpdate();
                    assertThat(alteradas).as("A barreira alterou exatamente sua linha sintética no MySQL").isEqualTo(1);
                    String chaveConfirmada = String.valueOf(em.createNativeQuery(
                                    "select cnpj from fornecedor where id_fornecedor=:id")
                            .setParameter("id", controleId).getSingleResult());
                    assertThat(chaveConfirmada).as("SELECT escalar confirma a chave efetivamente escrita").isEqualTo(cnpjTentativa);
                    long linhaNoIndice = ((Number) em.createNativeQuery(
                                    "select id_fornecedor from fornecedor force index (uk_fornecedor_cnpj) "
                                            + "where cnpj=:cnpj for update")
                            .setParameter("cnpj", cnpjTentativa).getSingleResult()).longValue();
                    assertThat(linhaNoIndice).as("A UNIQUE disputada aponta para a linha bloqueada").isEqualTo(controleId);
                    Barreira b = new Barreira(((Number) em.createNativeQuery("select connection_id()")
                            .getSingleResult()).longValue(), controleId, chaveConfirmada, linhaNoIndice);
                    Path preparo = Path.of("target/auditoria-http/fornecedor-criacao-barreira-confirmada.json");
                    Files.createDirectories(preparo.getParent());
                    Files.writeString(preparo, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapa(
                            "audit_round", System.getenv("SGT_AUDITORIA_RODADA"), "recorded_at", Instant.now().toString(),
                            "metodo", "UPDATE parametrizado, SELECT escalar e leitura UNIQUE FOR UPDATE",
                            "linhasAlteradas", alteradas, "barreira", b,
                            "rollbackObrigatorio", true, "somenteFixtureSintetica", true)));
                    preparada.complete(b);
                    if (!liberar.await(40, TimeUnit.SECONDS))
                        throw new AssertionError("Limite da fixture: barreira UNIQUE não liberada");
                    status.setRollbackOnly();
                    return b;
                } catch (Throwable ex) {
                    preparada.completeExceptionally(ex);
                    if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
                    throw new IllegalStateException(ex);
                }
            }));
            Barreira barreira = preparada.get(10, TimeUnit.SECONDS);
            Future<MvcResult> tentativa = pool.submit(() -> mvc.perform(pedido).andReturn());
            List<Map<String, Object>> espera = aguardarUniqueReal(barreira.conexao(), cnpjTentativa, tentativa);

            Future<?> revogacao = pool.submit(() -> {
                executar(c.admin, put("/api/usuarios/" + c.localA.getId()).contentType(APPLICATION_JSON)
                                .content(json(mapa("active", false))), 200,
                        "Outro Admin desativa o executor enquanto o INSERT aguarda a UNIQUE de CNPJ");
                return null;
            });
            revogacao.get(12, TimeUnit.SECONDS);
            emTransacao(() -> {
                em.clear();
                assertThat(em.find(UsuarioAplicacao.class, c.localA.getId()).getActive())
                        .as("Revogação commitada, relida em transação independente antes do rollback da barreira")
                        .isFalse();
                return null;
            });
            assertThat(tentativa.isDone()).as("O POST continua esperando depois da revogação commitada").isFalse();
            liberar.countDown();
            assertThat(bloqueador.get(10, TimeUnit.SECONDS)).isEqualTo(barreira);
            boolean barreiraRevertida = emTransacao(() -> {
                em.clear();
                Fornecedor controleRestaurado = em.find(Fornecedor.class, barreira.fornecedorId());
                return controleRestaurado != null && cnpjControle.equals(controleRestaurado.getCnpj());
            });
            assertThat(barreiraRevertida).as("Rollback devolveu o CNPJ original à mesma linha sintética de controle").isTrue();
            MvcResult resultado = tentativa.get(15, TimeUnit.SECONDS);
            JsonNode depois = estado(controleId, cnpjTentativa);

            Path arquivo = Path.of("target/auditoria-http/candidato10-fornecedor-criacao-executor-revogado-v2.json");
            Files.createDirectories(arquivo.getParent());
            Files.writeString(arquivo, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapa(
                    "autor", "/root/revisao_final_pessoas", "origemHipotese", "/root/revisao_final_contratos",
                    "hipotese", "C10-FORNECEDOR-CRIACAO-EXECUTOR", "audit_round", System.getenv("SGT_AUDITORIA_RODADA"),
                    "recorded_at", Instant.now().toString(), "rota", ROTA, "controlePositivoStatus", 200,
                    "controleId", controleId, "executorId", c.localA.getId(), "revogadorId", c.admin.getId(),
                    "cnpjSinteticoTentativa", cnpjTentativa, "barreira", barreira,
                    "esperaMySqlObservada", true, "esperaFisica", espera,
                    "revogacaoCommitadaAntesDoRollback", true, "barreiraRestauradaAposRollback", barreiraRevertida,
                    "status", resultado.getResponse().getStatus(), "antes", antes, "depois", depois)));

            assertAll("Autorização revogada não sobrevive à espera na UNIQUE desfeita por rollback",
                    () -> assertThat(resultado.getResponse().getStatus()).isIn(401, 403),
                    () -> assertThat(depois.path("tentativas"))
                            .as("A tentativa recusada não deixa inserção própria para o CNPJ já livre").isEmpty(),
                    () -> assertThat(depois)
                            .as("Fornecedor de controle, versão e todos seus campos permanecem intactos").isEqualTo(antes));
        } finally {
            liberar.countDown();
            pool.shutdownNow();
            pool.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private JsonNode estado(long controleId, String cnpjTentativa) {
        return emTransacao(() -> {
            em.clear();
            Fornecedor controle = em.find(Fornecedor.class, controleId);
            assertThat(controle).isNotNull();
            List<Map<String, Object>> tentativas = em.createQuery(
                            "select f from Fornecedor f where f.cnpj=:cnpj order by f.id", Fornecedor.class)
                    .setParameter("cnpj", cnpjTentativa).getResultList().stream().map(this::foto).toList();
            return mapper.valueToTree(mapa("controle", foto(controle), "tentativas", tentativas));
        });
    }

    private Map<String, Object> foto(Fornecedor f) {
        return mapa("id", f.getId(), "version", f.getVersion(), "razaoSocial", f.getRazaoSocial(),
                "cnpj", f.getCnpj(), "email", f.getEmail(), "ativo", f.getAtivo());
    }

    private String cnpjSinteticoLivre() {
        // CNPJ de fixture com 14 dígitos, como exige o serviço; não é documento autêntico.
        for (int n = 0; n < 100; n++) {
            String cnpj = String.format(Locale.ROOT, "%014d",
                    Math.floorMod(UUID.randomUUID().getMostSignificantBits(), 100_000_000_000_000L));
            long quantidade = emTransacao(() -> em.createQuery(
                            "select count(f) from Fornecedor f where f.cnpj=:cnpj", Long.class)
                    .setParameter("cnpj", cnpj).getSingleResult());
            if (quantidade == 0) return cnpj;
        }
        throw new AssertionError("Não foi possível reservar identidade sintética livre para a fixture");
    }

    private void exigirIndiceUnico() {
        Long quantidade = new JdbcTemplate(dataSource).queryForObject(
                "select count(*) from information_schema.statistics where table_schema=database() "
                        + "and table_name='fornecedor' and index_name='uk_fornecedor_cnpj' "
                        + "and non_unique=0 and column_name='cnpj' and seq_in_index=1", Long.class);
        assertThat(quantidade).as("Premissa de schema Hibernate: UNIQUE física do CNPJ").isEqualTo(1L);
    }

    private List<Map<String, Object>> aguardarUniqueReal(long bloqueadora, String cnpj,
                                                        Future<?> tentativa) throws Exception {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        CompletableFuture<List<Map<String, Object>>> observada = new CompletableFuture<>();
        List<Map<String, Object>> amostras = new CopyOnWriteArrayList<>();
        AtomicReference<String> ultimaAssinatura = new AtomicReference<>();
        AtomicReference<String> desfecho = new AtomicReference<>("OBSERVACAO_INICIADA");
        var observador = Executors.newSingleThreadScheduledExecutor();
        var consulta = observador.scheduleWithFixedDelay(() -> {
            try {
                // Diagnóstico deliberadamente amplo, mas limitado ao PID da barreira.
                // Os ENGINE_LOCK_ID vêm da própria relação de espera, sem exigir
                // previamente igualdade de LOCK_DATA ou sequer presença dos dois
                // registros em data_locks nessa amostra. Não autoriza revogação.
                List<Map<String, Object>> todasEsperas = jdbc.queryForList("select "
                        + "w.ENGINE as engine_espera, w.REQUESTING_ENGINE_LOCK_ID as lock_solicitado_id, "
                        + "w.BLOCKING_ENGINE_LOCK_ID as lock_bloqueador_id, "
                        + "w.REQUESTING_THREAD_ID as thread_solicitante, w.BLOCKING_THREAD_ID as thread_bloqueador, "
                        + "t.PROCESSLIST_ID as conexao_bloqueadora, rt.PROCESSLIST_ID as conexao_solicitante, "
                        + "r.OBJECT_SCHEMA as esquema_solicitado, r.OBJECT_NAME as tabela_solicitada, "
                        + "r.INDEX_NAME as indice_solicitado, r.LOCK_TYPE as tipo_solicitado, "
                        + "r.LOCK_MODE as modo_solicitado, r.LOCK_STATUS as estado_solicitado, r.LOCK_DATA as dados_solicitados, "
                        + "b.OBJECT_SCHEMA as esquema_bloqueador, b.OBJECT_NAME as tabela_bloqueadora, "
                        + "b.INDEX_NAME as indice_bloqueador, b.LOCK_TYPE as tipo_bloqueador, "
                        + "b.LOCK_MODE as modo_bloqueador, b.LOCK_STATUS as estado_bloqueador, b.LOCK_DATA as dados_bloqueadores "
                        + "from performance_schema.data_lock_waits w "
                        + "join performance_schema.threads t on t.THREAD_ID=w.BLOCKING_THREAD_ID "
                        + "left join performance_schema.threads rt on rt.THREAD_ID=w.REQUESTING_THREAD_ID "
                        + "left join performance_schema.data_locks r on r.ENGINE_LOCK_ID=w.REQUESTING_ENGINE_LOCK_ID and r.ENGINE=w.ENGINE "
                        + "left join performance_schema.data_locks b on b.ENGINE_LOCK_ID=w.BLOCKING_ENGINE_LOCK_ID and b.ENGINE=w.ENGINE "
                        + "where t.PROCESSLIST_ID=? order by w.REQUESTING_ENGINE_LOCK_ID, w.BLOCKING_ENGINE_LOCK_ID", bloqueadora);
                List<Map<String, Object>> locksDaBarreira = jdbc.queryForList("select "
                        + "l.ENGINE as engine, l.ENGINE_LOCK_ID as lock_id, l.ENGINE_TRANSACTION_ID as transacao, "
                        + "l.THREAD_ID as thread_id, t.PROCESSLIST_ID as conexao, l.OBJECT_SCHEMA as esquema, "
                        + "l.OBJECT_NAME as tabela, l.INDEX_NAME as indice, l.LOCK_TYPE as tipo, "
                        + "l.LOCK_MODE as modo, l.LOCK_STATUS as estado, l.LOCK_DATA as dados "
                        + "from performance_schema.data_locks l "
                        + "join performance_schema.threads t on t.THREAD_ID=l.THREAD_ID "
                        + "where t.PROCESSLIST_ID=? order by l.ENGINE_LOCK_ID", bloqueadora);

                // Predicado V1 preservado: apenas ele satisfaz a premissa e deixa
                // o chamador prosseguir para a revogação HTTP e seus oráculos.
                List<Map<String, Object>> linhas = jdbc.queryForList("select w.REQUESTING_THREAD_ID as solicitante, "
                        + "w.BLOCKING_THREAD_ID as bloqueador, r.OBJECT_SCHEMA as esquema, r.OBJECT_NAME as tabela, "
                        + "r.INDEX_NAME as indice, r.LOCK_DATA as registro, r.LOCK_MODE as modo_solicitado, "
                        + "b.LOCK_MODE as modo_detido, r.LOCK_STATUS as estado "
                        + "from performance_schema.data_lock_waits w "
                        + "join performance_schema.threads t on t.THREAD_ID=w.BLOCKING_THREAD_ID "
                        + "join performance_schema.data_locks r on r.ENGINE_LOCK_ID=w.REQUESTING_ENGINE_LOCK_ID and r.ENGINE=w.ENGINE "
                        + "join performance_schema.data_locks b on b.ENGINE_LOCK_ID=w.BLOCKING_ENGINE_LOCK_ID and b.ENGINE=w.ENGINE "
                        + "where t.PROCESSLIST_ID=? and r.OBJECT_SCHEMA=database() and r.OBJECT_NAME='fornecedor' "
                        + "and r.INDEX_NAME='uk_fornecedor_cnpj' and r.LOCK_TYPE='RECORD' "
                        + "and r.LOCK_DATA like concat('%', ?, '%') "
                        + "and r.LOCK_STATUS='WAITING' and b.LOCK_STATUS='GRANTED' "
                        + "and b.OBJECT_SCHEMA=r.OBJECT_SCHEMA and b.OBJECT_NAME=r.OBJECT_NAME "
                        + "and b.INDEX_NAME=r.INDEX_NAME and b.LOCK_DATA=r.LOCK_DATA", bloqueadora, cnpj);
                Map<String, Object> estado = mapa("esperasDoPid", todasEsperas, "locksDoPid", locksDaBarreira,
                        "linhasDoPredicadoV1", linhas, "tentativaTerminou", tentativa.isDone());
                String assinatura = mapper.writeValueAsString(estado);
                if (!assinatura.equals(ultimaAssinatura.getAndSet(assinatura))) {
                    amostras.add(mapa("instante", Instant.now().toString(), "estado", estado));
                }
                if (!linhas.isEmpty()) {
                    desfecho.set("PREMISSA_FISICA_V1_OBSERVADA");
                    observada.complete(List.copyOf(linhas));
                } else if (tentativa.isDone()) {
                    desfecho.set("PEDIDO_TERMINOU_SEM_PREMISSA");
                    observada.completeExceptionally(new AssertionError("POST não alcançou espera física na UNIQUE do CNPJ sintético"));
                }
            } catch (Throwable ex) {
                desfecho.set("ERRO_DE_OBSERVACAO: " + ex.getClass().getName() + ": " + ex.getMessage());
                observada.completeExceptionally(ex);
            }
        }, 0, 25, TimeUnit.MILLISECONDS);
        try { return observada.get(10, TimeUnit.SECONDS); }
        catch (java.util.concurrent.TimeoutException ex) {
            desfecho.set("TIMEOUT_SEM_PREMISSA_V1; NAO_HOUVE_REVOGACAO_NEM_ORACULO_NEGOCIAL");
            throw ex;
        }
        finally {
            consulta.cancel(true);
            observador.shutdownNow();
            observador.awaitTermination(5, TimeUnit.SECONDS);
            Path arquivo = Path.of("target/auditoria-http/candidato10-fornecedor-observacao-unique-v2.json");
            Files.createDirectories(arquivo.getParent());
            Files.writeString(arquivo, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapa(
                    "autor", "/root/revisao_final_pessoas", "audit_round", System.getenv("SGT_AUDITORIA_RODADA"),
                    "recorded_at", Instant.now().toString(), "conexaoBloqueadora", bloqueadora,
                    "cnpjSinteticoEsperado", cnpj, "predicadoV1Preservado", true,
                    "premissaFisicaEncontrada", "PREMISSA_FISICA_V1_OBSERVADA".equals(desfecho.get()),
                    "desfecho", desfecho.get(), "amostrasDistintas", amostras,
                    "limite", "Diagnóstico da premissa, não comprovação de escrita após revogação.")));
        }
    }

    private record Barreira(long conexao, long fornecedorId, String chaveConfirmada, long linhaNoIndice) {}
}
