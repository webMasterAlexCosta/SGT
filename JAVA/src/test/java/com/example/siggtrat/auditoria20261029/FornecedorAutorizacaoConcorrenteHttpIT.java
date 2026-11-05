package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.entidade.Fornecedor;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.utilitario.RelogioSistema;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * C8-CON-H02/H03 — regressão preservada após extração do cadastro operacional:
 * vínculo do fornecedor e atualização do mestre revalidam o executor após a raiz.
 * A entrada é Gestor Local, como exige FornecedorControlador;
 * Administrador aparece somente no ato HTTP de desativar esse executor.
 * Fonte C8 cdf471fbe344f4eb4e096a5e86b6f71226fe3ad3cde040baa6d6d4d4812e5545.
 * Técnica de observação real do MySQL deriva dos REDs C3→C4→C6.
 * Propostas independentes: nenhuma execução ou correção produtiva pelo revisor.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@TestPropertySource(properties = {"spring.cache.type=redis", "spring.sql.init.mode=never",
        "app.cache.redis.http-get.enabled=true"})
class FornecedorAutorizacaoConcorrenteHttpIT extends ContratosHttpBase {
    private static final String F = "/api/fornecedores";
    @Autowired private DataSource dataSource;

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void vinculoDeFornecedorRevalidaExecutorDepoisDaEsperaNoContrato() throws Exception {
        Cenario c = criarCenarioPersistido();
        long fornecedorOriginal = novoFornecedor(c, "Fornecedor original do rascunho");
        long fornecedorTentado = novoFornecedor(c, "Fornecedor pretendido depois da revogação");
        var inicio = RelogioSistema.hoje().plusDays(10);
        var req = rascunho(c, "FORN-REVOG-" + UUID.randomUUID(), 5, inicio, inicio.plusDays(30));
        req.put("cnpjEmpresa", estadoFornecedor(fornecedorOriginal).cnpj());
        long contratoId = enviar(c.localA, "/api/contratos", req, 201,
                "Controle positivo: Gestor Local cria rascunho próprio sem fato publicado").path("id").asLong();
        enviar(c.localA, base(contratoId) + "/composicoes", composicao(c, c.cargoA.getId(), 5), 201,
                "Preparação exclusiva deve permanecer se o vínculo de escrita for recusado");
        executar(c.localA, put("/api/contratos/" + contratoId + "/fornecedor/" + fornecedorOriginal), 200,
                "Controle positivo: perfil permitido vincula fornecedor ao rascunho pela mesma rota");
        EstadoContrato antes = estadoContrato(contratoId);
        assertThat(antes.fornecedorId()).isEqualTo(fornecedorOriginal);
        assertThat(antes.estado()).isEqualTo("RASCUNHO");
        assertThat(antes.composicoes()).isEqualTo(1);
        // Rascunho legítimo não possui evento/fotografia de publicação.
        assertThat(antes.eventos()).isZero();
        assertThat(antes.fotografias()).isZero();

        var pedido = autenticar(put("/api/contratos/" + contratoId + "/fornecedor/" + fornecedorTentado), c.localA);
        MvcResult resultado = tentarDepoisDeRevogacao(c, Contrato.class, contratoId, pedido);
        EstadoContrato depois = estadoContrato(contratoId);
        escreverEvidencia("C8-CON-H02", "vinculo", contratoId, resultado, antes, depois);
        assertAll("Vínculo de fornecedor não conserva autorização revogada durante espera no Contrato",
                () -> assertThat(resultado.getResponse().getStatus()).isIn(401, 403, 404, 409),
                () -> assertThat(depois)
                        .as("Fornecedor, versão, campos, composição, eventos e fotografias permanecem iguais")
                        .isEqualTo(antes));
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void atualizacaoDeFornecedorRevalidaExecutorDepoisDaEsperaNoFornecedor() throws Exception {
        Cenario c = criarCenarioPersistido();
        long fornecedorId = novoFornecedor(c, "Fornecedor cadastrado antes da revogação");
        String cnpj = estadoFornecedor(fornecedorId).cnpj();
        executar(c.localA, put(F + "/" + fornecedorId).contentType(MediaType.APPLICATION_JSON)
                        .content(json(dadosFornecedor("Fornecedor confirmado antes da revogação", cnpj,
                                "controle@example.test", true))), 200,
                "Controle positivo: Gestor Local permitido atualiza o mesmo fornecedor");
        // O controle positivo acima confirmou uma alteração operacional. A
        // versão e todos os seus dados precisam sobreviver à tentativa revogada.
        EstadoFornecedor antes = estadoFornecedor(fornecedorId);
        assertThat(antes.razaoSocial()).isEqualTo("Fornecedor confirmado antes da revogação");
        assertThat(antes.versao()).isGreaterThan(0);
        var pedido = autenticar(put(F + "/" + fornecedorId)
                .contentType(MediaType.APPLICATION_JSON).content(json(dadosFornecedor(
                        "Alteração tentada depois da revogação", cnpj, "tentativa@example.test", false))), c.localA);
        MvcResult resultado = tentarDepoisDeRevogacao(c, Fornecedor.class, fornecedorId, pedido);
        EstadoFornecedor depois = estadoFornecedor(fornecedorId);
        escreverEvidencia("C8-CON-H03", "mestre", fornecedorId, resultado, antes, depois);
        assertAll("Cadastro de fornecedor exige autorização corrente depois da espera na entidade",
                () -> assertThat(resultado.getResponse().getStatus()).isIn(401, 403, 404, 409),
                () -> assertThat(depois)
                        .as("Identidade, versão e todos os dados cadastrais permanecem idênticos")
                        .isEqualTo(antes));
    }

    private long novoFornecedor(Cenario c, String razao) throws Exception {
        // A aplicação exige quatorze dígitos. A identidade é explicitamente
        // sintética e exclusiva deste cenário, não um CNPJ documental autêntico.
        String cnpj = String.format(Locale.ROOT, "%014d",
                Math.floorMod(UUID.randomUUID().getMostSignificantBits(), 100_000_000_000_000L));
        return enviar(c.localA, F, dadosFornecedor(razao, cnpj, "origem@example.test", true),
                200, "Controle positivo: Gestor Local cria fornecedor sintético próprio da fixture")
                .path("id").asLong();
    }

    private Map<String, Object> dadosFornecedor(String razao, String cnpj, String email, boolean ativo) {
        return mapa("razaoSocial", razao, "cnpj", cnpj, "email", email, "ativo", ativo);
    }

    private MvcResult tentarDepoisDeRevogacao(Cenario c, Class<?> raiz, long id,
                                             MockHttpServletRequestBuilder pedido) throws Exception {
        var pool = Executors.newFixedThreadPool(2);
        CountDownLatch liberarRaiz = new CountDownLatch(1);
        CompletableFuture<Long> conexaoBloqueadora = new CompletableFuture<>();
        try {
            Future<?> bloqueador = pool.submit(() -> emTransacao(() -> {
                em.find(raiz, id, LockModeType.PESSIMISTIC_WRITE);
                conexaoBloqueadora.complete(((Number) em.createNativeQuery("select connection_id()")
                        .getSingleResult()).longValue());
                try {
                    if (!liberarRaiz.await(30, TimeUnit.SECONDS))
                        throw new AssertionError("Limite da fixture: raiz negocial não liberada");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(ex);
                }
                return null;
            }));
            long conexao = conexaoBloqueadora.get(10, TimeUnit.SECONDS);
            Future<MvcResult> tentativa = pool.submit(() -> mvc.perform(pedido).andReturn());
            aguardarBloqueioReal(conexao, tentativa);
            executar(c.admin, put("/api/usuarios/" + c.localA.getId())
                            .contentType(MediaType.APPLICATION_JSON).content(json(mapa("active", false))), 200,
                    "Administrador desativa o executor enquanto a mutação de fornecedor aguarda a raiz");
            emTransacao(() -> {
                assertThat(em.find(UsuarioAplicacao.class, c.localA.getId()).getActive())
                        .as("Desativação commitada em transação independente antes da liberação").isFalse();
                return null;
            });
            assertThat(tentativa.isDone()).as("Mutação ainda bloqueada depois da revogação commitada").isFalse();
            liberarRaiz.countDown();
            bloqueador.get(10, TimeUnit.SECONDS);
            return tentativa.get(15, TimeUnit.SECONDS);
        } finally {
            liberarRaiz.countDown();
            pool.shutdownNow();
            pool.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private void aguardarBloqueioReal(long conexaoBloqueadora, Future<?> tentativa) throws Exception {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        CompletableFuture<Void> esperaObservada = new CompletableFuture<>();
        var observador = Executors.newSingleThreadScheduledExecutor();
        var consulta = observador.scheduleWithFixedDelay(() -> {
            try {
                Long esperas = jdbc.queryForObject("select count(*) from performance_schema.data_lock_waits w "
                        + "join performance_schema.threads t on t.THREAD_ID=w.BLOCKING_THREAD_ID "
                        + "where t.PROCESSLIST_ID=?", Long.class, conexaoBloqueadora);
                if (esperas != null && esperas >= 1) esperaObservada.complete(null);
                else if (tentativa.isDone()) esperaObservada.completeExceptionally(
                        new AssertionError("Mutação de fornecedor não alcançou espera real na raiz controlada"));
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

    private void escreverEvidencia(String hipotese, String variante, long id, MvcResult resultado,
                                   Object antes, Object depois) throws Exception {
        Path path = Path.of("target/auditoria-http/fornecedor-autorizacao-" + variante + ".json");
        Files.createDirectories(path.getParent());
        Files.writeString(path, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapa(
                "hipotese", hipotese, "audit_round", System.getenv("SGT_AUDITORIA_RODADA"),
                "recorded_at", Instant.now().toString(), "controlePositivoStatus", 200,
                "raizId", id, "esperaMySqlObservada", true,
                "revogacaoCommitadaAntesDaLiberacao", true,
                "status", resultado.getResponse().getStatus(), "antes", antes, "depois", depois)));
    }

    private EstadoContrato estadoContrato(long id) {
        return emTransacao(() -> {
            Contrato ct = em.find(Contrato.class, id);
            return new EstadoContrato(ct.getId(), ct.getVersion(), ct.getEstadoNegocial().name(),
                    ct.getNumeroContrato(), ct.getAnoContrato(), ct.getDescricao(), ct.getAtivo(),
                    ct.getFornecedor() == null ? null : ct.getFornecedor().getId(),
                    ct.getCnpjEmpresa(), ct.getRazaoSocialEmpresa(), ct.getNomeFantasiaEmpresa(),
                    contarContrato("ComposicaoContratual", id), contarContrato("EventoCicloVidaContrato", id),
                    contarContrato("VersaoContratoNegocial", id));
        });
    }

    private long contarContrato(String entidade, long id) {
        return em.createQuery("select count(e) from " + entidade + " e where e.contrato.id=:id", Long.class)
                .setParameter("id", id).getSingleResult();
    }

    private EstadoFornecedor estadoFornecedor(long id) {
        return emTransacao(() -> {
            Fornecedor f = em.find(Fornecedor.class, id);
            return new EstadoFornecedor(f.getId(), f.getVersion(), f.getRazaoSocial(), f.getCnpj(),
                    f.getEmail(), f.getAtivo());
        });
    }

    private record EstadoContrato(long id, long versao, String estado, String numero, Integer ano,
                                  String descricao, Boolean ativo, Long fornecedorId, String cnpj,
                                  String razao, String fantasia, long composicoes, long eventos, long fotografias) {}
    private record EstadoFornecedor(long id, long versao, String razaoSocial, String cnpj, String email,
                                    Boolean ativo) {}
}
