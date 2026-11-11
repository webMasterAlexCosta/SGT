package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.infra.EsperaJdbcPostgres;
import com.example.siggtrat.infra.ObservadorAtorPostgres.Ator;

import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
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
 * Autoria: revisor independente transversal C4, hipótese C4-TR-H01.
 * Fonte lida: manifesto 13c84fd30b668335228704fd1028ddea11adad6f607458a7b7fa993dcbc64a89.
 * Teste proposto; não compilado nem executado pelo autor. PostgreSQL/HTTP reais;
 * nenhuma substituição de persistência/autorização, nenhum DDL manual.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class Candidato4FrequenciaExecutorRevogadoRedIT extends ContratosHttpBase {
    @Autowired private DataSource dataSource;

    @ParameterizedTest(name = "lote={0}, rebaixarPerfil={1}, replay={2}")
    @CsvSource({"false,false,false", "true,false,false", "true,true,false", "true,false,true"})
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void frequenciaRevalidaExecutorAposEsperaDaRaiz(boolean lote, boolean rebaixarPerfil, boolean replay) throws Exception {
        Cenario c = criarCenarioPersistido();
        String rota = lote ? "/api/frequencias/lote" : "/api/frequencias";
        Map<String,Object> controle = item(c, DATA.minusDays(1));
        executar(c.localA, post(rota).contentType(APPLICATION_JSON)
                .content(json(lote ? List.of(controle) : controle)), 200,
                "Controle positivo: o mesmo ator e contexto registram frequência antes da revogação");
        Map<String,Object> comando = item(c, DATA);
        if (replay) {
            executar(c.localA, post(rota).contentType(APPLICATION_JSON).content(json(List.of(comando))), 200,
                    "Controle positivo: gravação original do reenvio");
            Estado original = estado(c.vinculoA.getId());
            executar(c.localA, post(rota).contentType(APPLICATION_JSON).content(json(List.of(comando))), 200,
                    "Controle positivo: replay autorizado é idempotente");
            assertThat(estado(c.vinculoA.getId())).isEqualTo(original);
        }
        Estado antes = estado(c.vinculoA.getId());
        var pedido = autenticar(post(rota).contentType(APPLICATION_JSON)
                .content(json(lote ? List.of(comando) : comando)), c.localA);
        var pool = Executors.newFixedThreadPool(2);
        CountDownLatch liberar = new CountDownLatch(1);
        CompletableFuture<Long> conexaoBloqueadora = new CompletableFuture<>();
        try {
            Future<?> bloqueador = pool.submit(() -> emTransacao(() -> {
                em.find(Contrato.class, c.contratoA.getId(), LockModeType.PESSIMISTIC_WRITE);
                conexaoBloqueadora.complete(((Number) em.createNativeQuery("select pg_backend_pid()")
                        .getSingleResult()).longValue());
                try {
                    if (!liberar.await(30, TimeUnit.SECONDS)) throw new AssertionError("Limite da fixture: raiz não liberada");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt(); throw new IllegalStateException(ex);
                }
                return null;
            }));
            long conexao = conexaoBloqueadora.get(10, TimeUnit.SECONDS);
            var ator = new Ator("frequencia-" + (lote ? "lote" : "individual") + (replay ? "-replay" : ""));
            Future<MvcResult> tentativa = pool.submit(() -> ator.executar(() -> mvc.perform(pedido).andReturn()));
            var espera = EsperaJdbcPostgres.aguardar(new JdbcTemplate(dataSource), conexao, ator,
                    sql -> sql.selecaoChave("contrato", "id_contrato", c.contratoA.getId()),
                    "Frequência/replay: Contrato da fixture", tentativa);
            Map<String,Object> revogacao = rebaixarPerfil ? mapa("perfil", "GESTOR_NACIONAL") : mapa("active", false);
            executar(c.admin, put("/api/usuarios/" + c.localA.getId()).contentType(APPLICATION_JSON)
                    .content(json(revogacao)), 200, "Revogação administrativa confirmada antes de liberar o lock");
            emTransacao(() -> {
                UsuarioAplicacao u = em.find(UsuarioAplicacao.class, c.localA.getId());
                if (rebaixarPerfil) assertThat(u.getPerfil()).isEqualTo(PerfilAcesso.GESTOR_NACIONAL);
                else assertThat(u.getActive()).isFalse();
                return null;
            });
            assertThat(tentativa.isDone()).as("A escrita ainda aguarda a raiz após a revogação commitada").isFalse();
            liberar.countDown(); bloqueador.get(10, TimeUnit.SECONDS);
            MvcResult resultado = tentativa.get(15, TimeUnit.SECONDS);
            Estado depois = estado(c.vinculoA.getId());
            Path evidencia = Path.of("target/auditoria-http/candidato4-frequencia-executor-"
                    + lote + "-" + rebaixarPerfil + "-" + replay + ".json");
            Files.createDirectories(evidencia.getParent());
            Files.writeString(evidencia, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapa(
                    "hipotese", "C4-TR-H01", "lote", lote, "rebaixarPerfil", rebaixarPerfil, "replay", replay,
                    "esperaPostgresObservada", espera, "revogacaoCommitadaAntesDaLiberacao", true,
                    "status", resultado.getResponse().getStatus(), "antes", antes, "depois", depois)));
            assertAll("FRE-018/PER-010/AUD-005: autorização presente vale também após espera e em replay",
                    () -> assertThat(resultado.getResponse().getStatus()).isIn(401, 403, 404, 409),
                    () -> assertThat(depois).as("Sem nova frequência, versão ou evento após revogação").isEqualTo(antes));
        } finally {
            liberar.countDown(); pool.shutdownNow(); pool.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private Map<String,Object> item(Cenario c, java.time.LocalDate data) {
        return mapa("vinculoContratualId", c.vinculoA.getId(), "dataReferencia", data,
                "status", "FALTA", "justificativaRetificacao", "Conferência sintética do executor corrente",
                "chaveIdempotencia", "c4-tr-" + UUID.randomUUID());
    }

    private Estado estado(long vinculoId) {
        return emTransacao(() -> new Estado(
                em.createQuery("select count(f) from Frequencia f where f.vinculoContratual.id=:v", Long.class)
                        .setParameter("v", vinculoId).getSingleResult(),
                em.createQuery("select count(e) from EventoFrequencia e where e.frequencia.vinculoContratual.id=:v", Long.class)
                        .setParameter("v", vinculoId).getSingleResult(),
                em.createQuery("select coalesce(sum(f.versaoNegocial),0) from Frequencia f where f.vinculoContratual.id=:v", Long.class)
                        .setParameter("v", vinculoId).getSingleResult()));
    }

    private record Estado(long frequencias, long eventos, long somaVersoes) { }
}
