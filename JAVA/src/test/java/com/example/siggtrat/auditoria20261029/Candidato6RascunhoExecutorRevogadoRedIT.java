package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.infra.EsperaJdbcPostgres;
import com.example.siggtrat.infra.ObservadorAtorPostgres.Ator;
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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * C6-CON-H01 — hipótese independente: PUT/DELETE do rascunho leem o ator antes
 * de uma espera de escrita e não passam por revalidação após essa espera.
 * Norma/critério: CON-009:001, autorização corrente para escrita contratual.
 * Coordenação e observador derivados de Candidato4InativacaoExecutorRevogadoRedIT;
 * preserva a proveniência C3→C4 da técnica de observação real do PostgreSQL.
 * Fonte auditada: C6 b0796984cf93131ce89ba3634e479eb390dc7a9b3de5dc2fa1d7a944208a340e.
 * Somente proposta: não compilada/executada por este revisor, nem correção produtiva.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@TestPropertySource(properties = {"spring.cache.type=redis", "spring.sql.init.mode=never",
        "app.cache.redis.http-get.enabled=true"})
class Candidato6RascunhoExecutorRevogadoRedIT extends ContratosHttpBase {
    @Autowired private DataSource dataSource;

    @ParameterizedTest
    @ValueSource(strings = {"PUT", "DELETE"})
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void mutacaoDeRascunhoRevalidaExecutorDepoisDaEsperaDeEscrita(String operacao) throws Exception {
        Cenario c = criarCenarioPersistido();
        var inicio = RelogioSistema.hoje().plusDays(10);
        var dados = rascunho(c, "RASC-REVOG-" + UUID.randomUUID(), 5, inicio, inicio.plusDays(30));
        dados.put("descricao", "Preparação original que deve sobreviver à revogação");
        long contratoId = enviar(c.localA, "/api/contratos", dados, 201,
                "Controle positivo: Gestor ativo cria rascunho isolado, nunca publicado").path("id").asLong();
        enviar(c.localA, base(contratoId) + "/composicoes", composicao(c, c.cargoA.getId(), 5), 201,
                "Dependência exclusiva de preparação deve permanecer se a escrita for recusada");
        Estado antes = estado(contratoId);
        assertThat(antes.existe()).isTrue();
        assertThat(antes.estadoNegocial()).isEqualTo("RASCUNHO");
        assertThat(antes.eventos()).isZero();
        assertThat(antes.versoes()).isZero();
        assertThat(antes.composicoes()).isEqualTo(1);

        dados.put("descricao", "Tentativa de alterar preparação após revogação commitada");
        var pedido = operacao.equals("PUT")
                ? autenticar(put(base(contratoId)).contentType(MediaType.APPLICATION_JSON).content(json(dados)), c.localA)
                : autenticar(delete(base(contratoId)), c.localA);
        var pool = Executors.newFixedThreadPool(2);
        CountDownLatch liberarContrato = new CountDownLatch(1);
        CompletableFuture<Long> conexaoBloqueadora = new CompletableFuture<>();
        try {
            Future<?> bloqueador = pool.submit(() -> emTransacao(() -> {
                em.find(Contrato.class, contratoId, LockModeType.PESSIMISTIC_WRITE);
                conexaoBloqueadora.complete(((Number) em.createNativeQuery("select pg_backend_pid()")
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
            var ator = new Ator("rascunho-" + operacao);
            Future<MvcResult> tentativa = pool.submit(() -> ator.executar(() -> mvc.perform(pedido).andReturn()));
            var espera = EsperaJdbcPostgres.aguardar(new JdbcTemplate(dataSource), conexao, ator,
                    sql -> sql.selecaoChave("contrato", "id_contrato", contratoId),
                    "Contrato RASCUNHO em alteração/descarte", tentativa);

            executar(c.admin, put("/api/usuarios/" + c.localA.getId())
                            .contentType(MediaType.APPLICATION_JSON).content(json(mapa("active", false))), 200,
                    "Admin desativa o executor por API antes de liberar a escrita do rascunho");
            emTransacao(() -> {
                assertThat(em.find(UsuarioAplicacao.class, c.localA.getId()).getActive())
                        .as("Revogação já commitada, conferida em transação independente").isFalse();
                return null;
            });
            assertThat(tentativa.isDone()).as("Escrita ainda bloqueada depois da revogação commitada").isFalse();
            liberarContrato.countDown();
            bloqueador.get(10, TimeUnit.SECONDS);
            MvcResult resultado = tentativa.get(15, TimeUnit.SECONDS);
            Estado depois = estado(contratoId);

            Path evidencia = Path.of("target/auditoria-http/candidato6-rascunho-executor-"
                    + operacao.toLowerCase(Locale.ROOT) + ".json");
            Files.createDirectories(evidencia.getParent());
            Files.writeString(evidencia, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapa(
                    "hipotese", "C6-CON-H01", "operacao", operacao,
                    "esperaPostgresObservada", espera, "revogacaoCommitadaAntesDaLiberacao", true,
                    "status", resultado.getResponse().getStatus(), "antes", antes, "depois", depois)));
            assertAll("Rascunho exige executor ainda autorizado depois de aguardar a escrita",
                    () -> assertThat(resultado.getResponse().getStatus())
                            .as("Executor desativado enquanto aguardava deve ser recusado")
                            .isIn(401, 403, 404, 409),
                    () -> assertThat(depois)
                            .as("Sem alterar/remover contrato, composição, versão, evento ou fotografia após revogação")
                            .isEqualTo(antes));
        } finally {
            liberarContrato.countDown();
            pool.shutdownNow();
            pool.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private Estado estado(long contratoId) {
        return emTransacao(() -> {
            Contrato contrato = em.find(Contrato.class, contratoId);
            return new Estado(contrato != null, contrato == null ? null : contrato.getVersion(),
                    contrato == null ? null : contrato.getAtivo(),
                    contrato == null ? null : contrato.getEstadoNegocial().name(),
                    contrato == null ? null : contrato.getNumeroContrato(),
                    contrato == null ? null : contrato.getAnoContrato(),
                    contrato == null ? null : contrato.getDescricao(),
                    contrato == null ? null : contrato.getGestaoLocal().getId(),
                    contar("EventoCicloVidaContrato", contratoId), contar("VersaoContratoNegocial", contratoId),
                    contar("ComposicaoContratual", contratoId));
        });
    }

    private long contar(String entidade, long contratoId) {
        return em.createQuery("select count(x) from " + entidade + " x where x.contrato.id=:c", Long.class)
                .setParameter("c", contratoId).getSingleResult();
    }

    private record Estado(boolean existe, Long versaoContrato, Boolean ativo, String estadoNegocial,
                          String numero, Integer ano, String descricao, Long gestaoId,
                          long eventos, long versoes, long composicoes) {}
}
