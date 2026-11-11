package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.Fornecedor;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.fasterxml.jackson.databind.JsonNode;
import com.example.siggtrat.infra.EsperaJdbcPostgres;
import com.example.siggtrat.infra.ObservadorAtorPostgres.Ator;
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
            assertThat(conexao.getMetaData().getDatabaseProductName()).isEqualTo("PostgreSQL");
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
        var atorJdbc=new Ator("fornecedor-cnpj");
        try {
            Future<Barreira> bloqueador = pool.submit(() -> new TransactionTemplate(transactionManager).execute(status -> {
                try {
                    Fornecedor controleBloqueado = em.find(Fornecedor.class, controleId, LockModeType.PESSIMISTIC_WRITE);
                    assertThat(controleBloqueado).as("A linha sintética de controle existe antes da barreira").isNotNull();
                    assertThat(controleBloqueado.getCnpj()).isEqualTo(cnpjControle);
                    int alteradas = em.createQuery(
                                    "update Fornecedor set cnpj=:cnpj where id=:id")
                            .setParameter("cnpj", cnpjTentativa).setParameter("id", controleId).executeUpdate();
                    assertThat(alteradas).as("A barreira alterou exatamente sua linha sintética no PostgreSQL").isEqualTo(1);
                    String chaveConfirmada = String.valueOf(em.createNativeQuery(
                                    "select cnpj from fornecedor where id_fornecedor=:id")
                            .setParameter("id", controleId).getSingleResult());
                    assertThat(chaveConfirmada).as("SELECT escalar confirma a chave efetivamente escrita").isEqualTo(cnpjTentativa);
                    long linhaNoIndice = ((Number) em.createNativeQuery(
                                    "select id_fornecedor from fornecedor "
                                            + "where cnpj=:cnpj for update")
                            .setParameter("cnpj", cnpjTentativa).getSingleResult()).longValue();
                    assertThat(linhaNoIndice).as("A UNIQUE disputada aponta para a linha bloqueada").isEqualTo(controleId);
                    Barreira b = new Barreira(((Number) em.createNativeQuery("select pg_backend_pid()")
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
            Future<MvcResult> tentativa = pool.submit(() -> atorJdbc.executar(() -> mvc.perform(pedido).andReturn()));
            List<Map<String, Object>> espera = EsperaJdbcPostgres.aguardar(new JdbcTemplate(dataSource),barreira.conexao(),atorJdbc,
                    sql->sql.insercao("fornecedor","cnpj",cnpjTentativa),"fornecedor/cnpj sintético",tentativa);

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
                    "esperaPostgresObservada", true, "esperaFisica", espera,
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
        assertThat(EsperaJdbcPostgres.indiceUnico(new JdbcTemplate(dataSource),"fornecedor","uk_fornecedor_cnpj","cnpj"))
                .as("UNIQUE física simples, válida e pronta de CNPJ gerada pelo Hibernate").isTrue();
    }



    private record Barreira(long conexao, long fornecedorId, String chaveConfirmada, long linhaNoIndice) {}
}
