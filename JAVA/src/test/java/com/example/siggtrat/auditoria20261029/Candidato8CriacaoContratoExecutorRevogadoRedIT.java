package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.entidade.TipoContratacao;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
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
 * C8-CON-H01 — hipótese independente: criar autoriza o ator antes da espera
 * em TipoContratacao e não o revalida antes do INSERT do novo contrato.
 * Critério CON-009:001; norma de autorização corrente do executor.
 * Fonte C8 cdf471fbe344f4eb4e096a5e86b6f71226fe3ad3cde040baa6d6d4d4812e5545.
 * Coordenação derivada dos REDs C3→C4→C6: somente uma espera efetivamente
 * observada em pg_stat_activity/pg_locks autoriza avançar para a revogação.
 * Sem alteração produtiva, sem fabricação de fato legado e sem execução
 * Maven/Java/PostgreSQL por este revisor; execução exclusivamente pela raiz.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@TestPropertySource(properties = {"spring.cache.type=redis", "spring.sql.init.mode=never",
        "app.cache.redis.http-get.enabled=true"})
class Candidato8CriacaoContratoExecutorRevogadoRedIT extends ContratosHttpBase {
    @Autowired private DataSource dataSource;

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void criacaoRevalidaExecutorDepoisDaEsperaNoTipoDeContratacao() throws Exception {
        Cenario c = criarCenarioPersistido();
        var inicio = RelogioSistema.hoje().plusDays(10);
        int ano = inicio.getYear();
        String controleNumero = ("NOVO-CONTROLE-" + UUID.randomUUID()).toUpperCase(Locale.ROOT);
        var controle = rascunho(c, controleNumero, 5, inicio, inicio.plusDays(30));
        long controleId = enviar(c.localA, "/api/contratos", controle, 201,
                "Controle positivo: mesmo Gestor, Gestão, Tipo e campos criam rascunho válido antes da revogação")
                .path("id").asLong();
        assertThat(idsComNumero(controleNumero, ano)).containsExactly(controleId);

        String numero = ("NOVO-REVOG-" + UUID.randomUUID()).toUpperCase(Locale.ROOT);
        var dados = rascunho(c, numero, 5, inicio, inicio.plusDays(30));
        Estado antes = estado(controleId, numero, ano);
        assertThat(antes.controleExiste()).isTrue();
        assertThat(antes.contratosComNumeroExclusivo()).isEmpty();

        var pedido = autenticar(post("/api/contratos").contentType(MediaType.APPLICATION_JSON)
                .content(json(dados)), c.localA);
        var pool = Executors.newFixedThreadPool(2);
        CountDownLatch liberarTipo = new CountDownLatch(1);
        CompletableFuture<Long> conexaoBloqueadora = new CompletableFuture<>();
        try {
            Future<?> bloqueador = pool.submit(() -> emTransacao(() -> {
                // Esta transação bloqueia somente o Tipo usado pela criação.
                // Não toca Usuario, Contrato nem flags do catálogo.
                em.find(TipoContratacao.class, c.tipoA.getId(), LockModeType.PESSIMISTIC_WRITE);
                conexaoBloqueadora.complete(((Number) em.createNativeQuery("select pg_backend_pid()")
                        .getSingleResult()).longValue());
                try {
                    if (!liberarTipo.await(30, TimeUnit.SECONDS))
                        throw new AssertionError("Limite da fixture: Tipo de Contratação não liberado");
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
                    "Admin desativa o executor por API enquanto a criação aguarda o Tipo");
            emTransacao(() -> {
                assertThat(em.find(UsuarioAplicacao.class, c.localA.getId()).getActive())
                        .as("Revogação já commitada, conferida em outra transação antes da liberação").isFalse();
                return null;
            });
            assertThat(tentativa.isDone()).as("Criação permanece bloqueada após a revogação commitada").isFalse();
            liberarTipo.countDown();
            bloqueador.get(10, TimeUnit.SECONDS);
            MvcResult resultado = tentativa.get(15, TimeUnit.SECONDS);
            Estado depois = estado(controleId, numero, ano);

            Path evidencia = Path.of("target/auditoria-http/candidato8-criacao-contrato-executor-revogado.json");
            Files.createDirectories(evidencia.getParent());
            Files.writeString(evidencia, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapa(
                    "hipotese", "C8-CON-H01", "audit_round", System.getenv("SGT_AUDITORIA_RODADA"),
                    "recorded_at", Instant.now().toString(), "controlePositivoStatus", 201,
                    "controleContratoId", controleId, "tipoBloqueadoId", c.tipoA.getId(),
                    "numeroExclusivo", numero, "ano", ano,
                    "esperaMySqlObservada", true, "revogacaoCommitadaAntesDaLiberacao", true,
                    "status", resultado.getResponse().getStatus(), "antes", antes, "depois", depois)));
            assertAll("Criar contrato exige executor ainda autorizado depois de aguardar o catálogo",
                    () -> assertThat(resultado.getResponse().getStatus())
                            .as("Executor desativado enquanto aguardava o Tipo deve ser recusado")
                            .isIn(401, 403, 404, 409),
                    () -> assertThat(depois.contratosComNumeroExclusivo())
                            .as("Nenhum contrato com o número exclusivo deve persistir depois da recusa")
                            .isEmpty(),
                    () -> assertThat(depois).as("O controle positivo permanece e a tentativa não cria fato")
                            .isEqualTo(antes));
        } finally {
            liberarTipo.countDown();
            pool.shutdownNow();
            pool.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private void aguardarBloqueioReal(long conexaoBloqueadora, Future<?> tentativa) throws Exception {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        CompletableFuture<Void> esperaObservada = new CompletableFuture<>();
        var observador = Executors.newSingleThreadScheduledExecutor();
        // A cadência limita consultas. A evidência de espera é a linha real do
        // PostgreSQL ligada à conexão que mantém PWRITE do Tipo, não um sleep.
        var consulta = observador.scheduleWithFixedDelay(() -> {
            try {
                Long esperas = jdbc.queryForObject("""
SELECT count(*) FROM pg_stat_activity a WHERE a.datname=current_database() AND ? = ANY(pg_blocking_pids(a.pid))
""", Long.class, conexaoBloqueadora);
                if (esperas != null && esperas >= 1) esperaObservada.complete(null);
                else if (tentativa.isDone()) esperaObservada.completeExceptionally(
                        new AssertionError("Criação não alcançou a espera real no Tipo de Contratação"));
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

    private List<Long> idsComNumero(String numero, int ano) {
        return emTransacao(() -> em.createQuery(
                        "select c.id from Contrato c where c.numeroContrato=:numero and c.anoContrato=:ano order by c.id",
                        Long.class).setParameter("numero", numero).setParameter("ano", ano).getResultList());
    }

    private Estado estado(long controleId, String numero, int ano) {
        return emTransacao(() -> {
            Contrato controle = em.find(Contrato.class, controleId);
            List<Long> ids = em.createQuery(
                            "select c.id from Contrato c where c.numeroContrato=:numero and c.anoContrato=:ano order by c.id",
                            Long.class).setParameter("numero", numero).setParameter("ano", ano).getResultList();
            return new Estado(controle != null, controle == null ? null : controle.getVersion(), List.copyOf(ids));
        });
    }

    private record Estado(boolean controleExiste, Long controleVersao, List<Long> contratosComNumeroExclusivo) {}
}
