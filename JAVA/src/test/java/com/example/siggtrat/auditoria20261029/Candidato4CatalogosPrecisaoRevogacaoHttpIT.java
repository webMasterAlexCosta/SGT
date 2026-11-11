package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.infra.EsperaJdbcPostgres;
import com.example.siggtrat.infra.ObservadorAtorPostgres.Ator;
import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.example.siggtrat.entidade.Cargo;
import com.example.siggtrat.entidade.EventoDisponibilidadeCatalogo;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.LockModeType;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnectionFactory;
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
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Autoria independente: /root/revisao_final_pessoas, candidato4 congelado.
 * Manifesto SHA-256 13c84fd30b668335228704fd1028ddea11adad6f607458a7b7fa993dcbc64a89.
 * Hipóteses C4-PES-H01 (recibo temporal do catálogo) e C4-PES-H02 (revogação
 * durante espera no catálogo). Não compilado nem executado pelo revisor.
 * Usa a fixture HTTP compartilhada, que suprime somente o agendador de fundo;
 * não simula autorização, PostgreSQL, Redis, relógio, serviços ou transações.
 * Todas as fontes abaixo são declaradamente sintéticas, não acervo recuperado.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@TestPropertySource(properties = {"spring.cache.type=redis", "spring.sql.init.mode=never",
        "app.cache.redis.http-get.enabled=true"})
class Candidato4CatalogosPrecisaoRevogacaoHttpIT extends ContratosHttpBase {
    @Autowired private DataSource dataSource;
    @Autowired private RedisConnectionFactory redis;
    @Autowired private Environment ambiente;

    @BeforeEach
    void exigirInfraestruturaReal() throws Exception {
        try (var conexao = dataSource.getConnection()) {
            assertThat(conexao.getMetaData().getDatabaseProductName()).isEqualTo("PostgreSQL");
            assertThat(conexao.getMetaData().getDatabaseProductVersion()).matches("18\\.6(?: \\(Debian [^()]+\\))?");
        }
        assertThat(ambiente.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("update");
        assertThat(ambiente.getProperty("spring.sql.init.mode")).isEqualTo("never");
        assertThat(ambiente.getProperty("spring.cache.type")).isEqualTo("redis");
        try (var conexao = redis.getConnection()) { assertThat(conexao.ping()).isEqualTo("PONG"); }
    }

    @Test
    void reciboDaTransicaoCoincideComMysqlESeuInstanteConheceExatamenteAPropriaFonte() throws Exception {
        Cenario c = criarCenarioPersistido();
        long id = novoCargo(c);
        LocalDate data = RelogioSistema.hoje().minusDays(1);
        Map<String, Object> req = requisicao(c, id, data);
        JsonNode previa = enviar(c.localA, rota(id) + "/previa", req, 200,
                "CAT007 prévia da fonte sintética retroativa, sem origem anterior presumida");
        req.put("tokenPrevia", previa.path("tokenPrevia").asText());
        req.put("confirmarImpacto", true);
        JsonNode recibo = enviar(c.localA, rota(id), req, 200,
                "CAT007 confirmar ato retroativo por HTTP autenticado com commit real");
        long eventoId = recibo.path("eventoId").asLong();
        Instant recebido = Instant.parse(recibo.path("registradoEm").asText());
        Instant persistido = emTransacao(() -> {
            em.clear();
            return em.find(EventoDisponibilidadeCatalogo.class, eventoId).getRegistradoEm();
        });
        JsonNode reenvio = enviar(c.localA, rota(id), req, 200,
                "CAT007 reenvio da mesma intenção deve conservar evento, versão e instante");
        JsonNode noRecibo = disponibilidade(c, id, data, recebido);
        JsonNode antesRecibo = disponibilidade(c, id, data, recebido.minusNanos(1));
        JsonNode noPersistido = disponibilidade(c, id, data, persistido);
        JsonNode antesPersistido = disponibilidade(c, id, data, persistido.minusNanos(1));
        Path evidencia = Path.of("target/auditoria-http/candidato4-catalogo-precisao.json");
        Files.createDirectories(evidencia.getParent());
        Files.writeString(evidencia, json(mapa("hipotese", "C4-PES-H01", "catalogoId", id,
                "eventoId", eventoId, "recibo", recibo, "registradoEm_mysql_nova_transacao", persistido,
                "reenvio", reenvio, "consulta_K_recibo", noRecibo,
                "consulta_K_recibo_menos_1ns", antesRecibo, "consulta_K_persistido", noPersistido,
                "consulta_K_persistido_menos_1ns", antesPersistido,
                "transporte", "MockMvc/dispatcher real; PostgreSQL18.6 e Redis reais; não TCP")));
        SoftAssertions.assertSoftly(soft -> {
            // Nenhuma tolerância/truncamento no oráculo: K é o instante anunciado.
            soft.assertThat(recebido).as("Recibo canônico é o mesmo instante gravado no PostgreSQL")
                    .isEqualTo(persistido);
            soft.assertThat(Instant.parse(reenvio.path("registradoEm").asText()))
                    .as("Reenvio não altera o instante do recibo original").isEqualTo(recebido);
            soft.assertThat(reenvio.path("eventoId").asLong()).isEqualTo(eventoId);
            soft.assertThat(reenvio.path("reenvio").asBoolean()).isTrue();
            soft.assertThat(noRecibo.path("disponibilidadeComprovada").asBoolean())
                    .as("A própria fonte já é conhecida no K anunciado no recibo").isTrue();
            soft.assertThat(noRecibo.path("eventoFonteId").asLong()).isEqualTo(eventoId);
            soft.assertThat(antesRecibo.path("disponibilidadeComprovada").asBoolean())
                    .as("Um nanossegundo antes de K a fonte ainda não era conhecida").isFalse();
            // Controles independentes isolam o limite SQL da normalização da escrita.
            soft.assertThat(noPersistido.path("eventoFonteId").asLong()).isEqualTo(eventoId);
            soft.assertThat(antesPersistido.path("disponibilidadeComprovada").asBoolean()).isFalse();
        });
        assertThat(contar(id)).as("Origem prospectiva e um único ato documentado, sem duplicação")
                .isEqualTo(2L);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void executorRevogadoDuranteEsperaNoCatalogoNaoPodeConfirmarAto(boolean rebaixarPerfil) throws Exception {
        Cenario c = criarCenarioPersistido();
        long id = novoCargo(c);
        Map<String, Object> req = requisicao(c, id, RelogioSistema.hoje());
        var pedido = autenticar(post(rota(id)).contentType(MediaType.APPLICATION_JSON).content(json(req)), c.localA);
        Estado antes = estado(id);
        var pool = Executors.newFixedThreadPool(2);
        CountDownLatch liberar = new CountDownLatch(1);
        CompletableFuture<Long> bloqueadora = new CompletableFuture<>();
        try {
            Future<?> bloqueador = pool.submit(() -> emTransacao(() -> {
                em.find(Cargo.class, id, LockModeType.PESSIMISTIC_WRITE);
                bloqueadora.complete(((Number) em.createNativeQuery("select pg_backend_pid()")
                        .getSingleResult()).longValue());
                try {
                    if (!liberar.await(25, TimeUnit.SECONDS)) throw new AssertionError("Fixture não liberou o Cargo");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt(); throw new IllegalStateException(ex);
                }
                return null;
            }));
            long conexao = bloqueadora.get(10, TimeUnit.SECONDS);
            var ator = new Ator("transicao-cargo");
            Future<MvcResult> tentativa = pool.submit(() -> ator.executar(() -> mvc.perform(pedido).andReturn()));
            var espera = EsperaJdbcPostgres.aguardar(new JdbcTemplate(dataSource), conexao, ator,
                    sql -> sql.selecaoChave("cargo", "id_cargo", id),
                    "Cargo da transição de disponibilidade", tentativa);
            var revogacao = rebaixarPerfil
                    ? mapa("perfil", "OPERACIONAL", "gestaoLocalId", c.gestaoA.getId(),
                            "lotacaoId", c.lotacaoA.getId(), "jornadaId", c.jornadaA.getId())
                    : mapa("active", false);
            executar(c.admin, put("/api/usuarios/" + c.localA.getId()).contentType(MediaType.APPLICATION_JSON)
                            .content(json(revogacao)), 200,
                    "Admin conclui revogação antes de liberar a confirmação bloqueada no Cargo");
            emTransacao(() -> {
                UsuarioAplicacao persistido = em.find(UsuarioAplicacao.class, c.localA.getId());
                if (rebaixarPerfil) assertThat(persistido.getPerfil()).isEqualTo(PerfilAcesso.OPERACIONAL);
                else assertThat(persistido.getActive()).isFalse();
                return null;
            });
            assertThat(tentativa.isDone()).as("Revogação commitou enquanto o ato ainda esperava").isFalse();
            liberar.countDown();
            bloqueador.get(10, TimeUnit.SECONDS);
            MvcResult resposta = tentativa.get(15, TimeUnit.SECONDS);
            Estado depois = estado(id);
            Path evidencia = Path.of("target/auditoria-http/candidato4-catalogo-revogacao-" + rebaixarPerfil + ".json");
            Files.createDirectories(evidencia.getParent());
            Files.writeString(evidencia, json(mapa("hipotese", "C4-PES-H02", "rebaixarPerfil", rebaixarPerfil,
                    "status", resposta.getResponse().getStatus(), "estadoAntes", antes, "estadoDepois", depois,
                    "esperaPostgresObservada", espera, "revogacao_commitou_antes_da_liberacao", true)));
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(resposta.getResponse().getStatus()).as("Revogação exige recusa sem mutação")
                        .isIn(401, 403, 404, 409);
                soft.assertThat(depois).as("Recusa conserva flag, versão e trilha do Cargo").isEqualTo(antes);
            });
        } finally {
            liberar.countDown(); pool.shutdownNow(); pool.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private long novoCargo(Cenario c) throws Exception {
        return enviar(c.localA, "/api/cargos", mapa("nome", "Cargo sintético C4 " + UUID.randomUUID(), "ativo", true),
                200, "Criar Cargo sintético sem vínculos ou programação para isolar o oráculo").path("id").asLong();
    }
    private Map<String, Object> requisicao(Cenario c, long id, LocalDate data) throws Exception {
        return mapa("dataEfetiva", data, "ativo", false,
                "versaoEsperada", consultar(c.localA, "/api/cargos/" + id, "Versão atual do Cargo").path("version").asLong(),
                "motivo", "Fonte documental sintética de auditoria C4",
                "evidencia", "SEI-SINTETICO-C4-CATALOGO", "autorizacao", "ATO-SINTETICO-C4-CATALOGO",
                "chaveIdempotencia", UUID.randomUUID().toString());
    }
    private JsonNode disponibilidade(Cenario c, long id, LocalDate data, Instant k) throws Exception {
        return dados(executar(c.localA, get("/api/catalogos-temporais/CARGO/" + id + "/disponibilidade")
                        .param("data", data.toString()).param("conhecidoAte", k.toString()), 200,
                "Disponibilidade com K literal; oráculo não arredonda o instante do recibo"));
    }
    private String rota(long id) { return "/api/catalogos-temporais/CARGO/" + id + "/transicoes"; }
    private long contar(long id) {
        return emTransacao(() -> em.createQuery("select count(e) from EventoDisponibilidadeCatalogo e "
                        + "where e.tipoCatalogo=:tipo and e.catalogoId=:id", Long.class)
                .setParameter("tipo", TipoCatalogoTemporal.CARGO).setParameter("id", id).getSingleResult());
    }
    private Estado estado(long id) {
        return emTransacao(() -> {
            Cargo cargo = em.find(Cargo.class, id);
            long eventos = em.createQuery("select count(e) from EventoDisponibilidadeCatalogo e "
                            + "where e.tipoCatalogo=:tipo and e.catalogoId=:id", Long.class)
                    .setParameter("tipo", TipoCatalogoTemporal.CARGO).setParameter("id", id).getSingleResult();
            return new Estado(cargo.getAtivo(), cargo.getVersion(), eventos);
        });
    }
    private record Estado(Boolean ativo, long versao, long eventos) {}
}
