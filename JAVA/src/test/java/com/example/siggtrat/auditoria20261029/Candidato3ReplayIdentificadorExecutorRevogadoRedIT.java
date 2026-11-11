package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.infra.EsperaJdbcPostgres;
import com.example.siggtrat.infra.ObservadorAtorPostgres.Ator;

import com.example.siggtrat.dominio.acesso.PerfilAcesso;
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
import java.util.Map;
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
 * Hipótese C3-TR-H01: o retorno idempotente da correção precede a revalidação
 * introduzida no consumo da permissão. O ator é revogado enquanto o replay
 * aguarda a raiz; a primeira correção e um replay autorizado são controles.
 * Sem mocks de autorização/persistência e sem avanço artificial de relógio.
 * Fonte examinada: feba6053969643b81a1363baf77398e3721280b0a871814ffa057a3ecb5c4f05.
 * Reservado à execução central. Não executado/compilado por este revisor.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@TestPropertySource(properties = {"spring.cache.type=redis", "spring.sql.init.mode=never",
        "app.cache.redis.http-get.enabled=true"})
class Candidato3ReplayIdentificadorExecutorRevogadoRedIT extends ContratosHttpBase {
    @Autowired private DataSource dataSource;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void replayRevalidaExecutorAposEsperaDaRaiz(boolean rebaixarPerfil) throws Exception {
        Cenario c = criarCenarioPersistido();
        NovoContrato n = emTransacao(() -> {
            try { return publicado(c, 5, null, false); }
            catch (Exception ex) { throw new IllegalStateException(ex); }
        });
        conceder(c, n.id(), "CORRECAO_IDENTIFICADOR_CONTRATO");
        String rota = base(n.id()) + "/correcoes-identificador";
        String numero = ("REPLAY-C3-" + UUID.randomUUID().toString().substring(0, 8)).toUpperCase(Locale.ROOT);
        Map<String, Object> requisicao = mapa("novoNumero", numero, "novoAno", n.inicio().getYear(),
                "dataEfetiva", RelogioSistema.hoje(), "dataAto", RelogioSistema.hoje(),
                "motivo", "Correção sintética para examinar autorização corrente do reenvio",
                "referenciaSei", "SEI-SINTETICO-C3-REPLAY-" + numero,
                "autorizacaoAdministrativa", "ATO-HTTP", "chaveIdempotencia", UUID.randomUUID().toString());
        var original = enviar(c.localA, rota, requisicao, 201,
                "Controle positivo: correção original autorizada e confirmada");
        Estado antes = estado(n.id(), c.localA.getId());
        var replayAutorizado = enviar(c.localA, rota, requisicao, 201,
                "Controle positivo: mesma intenção retorna a correção existente");
        assertThat(replayAutorizado.path("id").asLong()).isEqualTo(original.path("id").asLong());
        assertThat(estado(n.id(), c.localA.getId())).isEqualTo(antes);
        assertThat(antes.correcoes()).isEqualTo(1);
        assertThat(antes.usos()).isEqualTo(1);

        // A sessão está validada antes de alcançar a espera observável na raiz.
        var pedido = autenticar(post(rota).contentType(MediaType.APPLICATION_JSON).content(json(requisicao)), c.localA);
        var pool = Executors.newFixedThreadPool(2);
        CountDownLatch liberarContrato = new CountDownLatch(1);
        CompletableFuture<Long> conexaoBloqueadora = new CompletableFuture<>();
        try {
            Future<?> bloqueador = pool.submit(() -> emTransacao(() -> {
                em.find(Contrato.class, n.id(), LockModeType.PESSIMISTIC_WRITE);
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
            var ator = new Ator("replay-identificador-contrato");
            Future<MvcResult> tentativa = pool.submit(() -> ator.executar(() -> mvc.perform(pedido).andReturn()));
            var espera = EsperaJdbcPostgres.aguardar(new JdbcTemplate(dataSource), conexao, ator,
                    sql -> sql.selecaoChave("contrato", "id_contrato", n.id()),
                    "Replay de identificador: Contrato da fixture", tentativa);

            Map<String, Object> revogacao = rebaixarPerfil
                    ? mapa("perfil", "OPERACIONAL", "gestaoLocalId", c.gestaoA.getId(),
                            "lotacaoId", c.lotacaoA.getId(), "jornadaId", c.jornadaA.getId())
                    : mapa("active", false);
            executar(c.admin, put("/api/usuarios/" + c.localA.getId())
                            .contentType(MediaType.APPLICATION_JSON).content(json(revogacao)), 200,
                    "Admin confirma a revogação antes de liberar o replay");
            emTransacao(() -> {
                UsuarioAplicacao persistido = em.find(UsuarioAplicacao.class, c.localA.getId());
                if (rebaixarPerfil) assertThat(persistido.getPerfil()).isEqualTo(PerfilAcesso.OPERACIONAL);
                else assertThat(persistido.getActive()).isFalse();
                return null;
            });
            assertThat(tentativa.isDone()).as("Replay ainda aguarda a raiz após revogação commitada").isFalse();
            liberarContrato.countDown();
            bloqueador.get(10, TimeUnit.SECONDS);
            MvcResult resultado = tentativa.get(15, TimeUnit.SECONDS);
            Estado depois = estado(n.id(), c.localA.getId());

            Path evidencia = Path.of("target/auditoria-http/candidato3-replay-executor-"
                    + (rebaixarPerfil ? "rebaixado" : "desativado") + ".json");
            Files.createDirectories(evidencia.getParent());
            Files.writeString(evidencia, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapa(
                    "hipotese", "C3-TR-H01", "rebaixarPerfil", rebaixarPerfil,
                    "esperaPostgresObservada", espera, "revogacaoCommitadaAntesDaLiberacao", true,
                    "statusReplay", resultado.getResponse().getStatus(), "antes", antes, "depois", depois)));
            assertAll("Reenvio preserva idempotência e depende de executor ainda autorizado",
                    () -> assertThat(resultado.getResponse().getStatus())
                            .as("Executor revogado antes do retorno idempotente deve ser recusado")
                            .isIn(401, 403, 404, 409),
                    () -> assertThat(depois).as("Sem correção, evento, documento, versão ou uso novo").isEqualTo(antes));
        } finally {
            liberarContrato.countDown();
            pool.shutdownNow();
            pool.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private Estado estado(long contratoId, long usuarioId) {
        return emTransacao(() -> {
            Contrato contrato = em.find(Contrato.class, contratoId);
            return new Estado(contrato.getVersion(), contrato.getNumeroContrato(),
                    contar("CorrecaoIdentificadorContrato", contratoId),
                    contar("EventoCicloVidaContrato", contratoId),
                    contar("VersaoContratoNegocial", contratoId),
                    contar("DocumentoComprobatorio", contratoId),
                    em.createQuery("select count(u) from UsoPermissaoExcepcional u where u.usuarioId=:u "
                                    + "and u.tipoRecurso='Contrato' and u.recursoId=:c", Long.class)
                            .setParameter("u", usuarioId).setParameter("c", contratoId).getSingleResult());
        });
    }

    private long contar(String entidade, long contratoId) {
        return em.createQuery("select count(x) from " + entidade + " x where x.contrato.id=:c", Long.class)
                .setParameter("c", contratoId).getSingleResult();
    }

    private record Estado(long versaoContrato, String numero, long correcoes, long eventos,
                          long versoes, long documentos, long usos) {}
}
