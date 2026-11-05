package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.example.siggtrat.entidade.*;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * C8-PES-H02. Autoria independente: /root/revisao_final_pessoas.
 * Fonte C8 cdf471fbe344f4eb4e096a5e86b6f71226fe3ad3cde040baa6d6d4d4812e5545.
 * CAT-001/002: administração pelo Gestor Local exige autorização corrente
 * depois da espera no catálogo, inclusive em alteração de rótulo sem transição.
 * Os cinco PUT e as cinco inativações abaixo são rotas efetivamente expostas.
 * Tipo/Motivo inativam por PUT: seus controllers não possuem DELETE.
 * Fonte sintética contemporânea criada por HTTP; não recupera história legada.
 * Proposta não compilada/executada pelo revisor; a execução será central.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@TestPropertySource(properties = {"spring.cache.type=redis", "spring.sql.init.mode=never",
        "app.cache.redis.http-get.enabled=true"})
class Candidato8CatalogosMestreExecutorRevogadoRedIT extends ContratosHttpBase {
    @Autowired private DataSource dataSource;

    @ParameterizedTest(name = "{0}: {1}")
    @CsvSource({
            "cargos, PUT", "cargos, DELETE",
            "lotacoes, PUT", "lotacoes, DELETE",
            "jornadas, PUT", "jornadas, DELETE",
            "tipos-contratacao, PUT", "tipos-contratacao, INATIVAR_PUT",
            "motivos-desligamento, PUT", "motivos-desligamento, INATIVAR_PUT"
    })
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void cadastroMestreRevalidaExecutorAposEsperaAntesDeAlterarOuInativar(
            String recurso, String operacao) throws Exception {
        try (var conexao = dataSource.getConnection()) {
            assertThat(conexao.getMetaData().getDatabaseProductName()).isEqualTo("MySQL");
            assertThat(conexao.getMetaData().getDatabaseProductVersion()).isEqualTo("8.0.45");
        }
        Cenario c = criarCenarioPersistido();
        Catalogo catalogo = catalogo(recurso);
        String rotulo = "C8 " + recurso + " " + UUID.randomUUID().toString().substring(0, 8);
        long id = dados(executar(c.localA, post("/api/" + recurso).contentType(MediaType.APPLICATION_JSON)
                        .content(json(payload(recurso, c, rotulo, true))), recurso.equals("cargos") ? 200 : 201,
                "Controle: criação legítima de catálogo novo e sem fatos dependentes por HTTP")).path("id").asLong();
        assertThat(id).isPositive();
        assertThat(estado(catalogo, id).ativo()).isTrue();
        assertThat(estado(catalogo, id).eventos()).hasSize(1);
        String rota = "/api/" + recurso + "/" + id;
        Map<String, Object> tentativaBody;
        if (operacao.equals("PUT")) {
            String controle = rotulo + " CONTROLE";
            executar(c.localA, put(rota).contentType(MediaType.APPLICATION_JSON)
                            .content(json(payload(recurso, c, controle, true))), 200,
                    "Controle positivo: o mesmo PUT confirma alteração válida com executor ativo");
            assertThat(estado(catalogo, id).rotulo()).isEqualTo(recurso.equals("jornadas")
                    ? controle.toUpperCase(Locale.ROOT) : controle);
            tentativaBody = payload(recurso, c, rotulo + " REVOGADO", true);
        } else {
            executar(c.localA, pedido(rota, operacao, payload(recurso, c, rotulo, false)),
                    operacao.equals("DELETE") ? 204 : 200,
                    "Controle positivo: a mesma inativação confirma com executor ativo");
            assertThat(estado(catalogo, id).ativo()).isFalse();
            executar(c.localA, put(rota).contentType(MediaType.APPLICATION_JSON)
                            .content(json(payload(recurso, c, rotulo, true))), 200,
                    "Reativação legítima por HTTP prepara outra inativação independente");
            assertThat(estado(catalogo, id).ativo()).isTrue();
            assertThat(estado(catalogo, id).eventos()).hasSize(3);
            tentativaBody = payload(recurso, c, rotulo, false);
        }
        Estado antes = estado(catalogo, id);
        var request = autenticar(pedido(rota, operacao, tentativaBody), c.localA);
        var pool = Executors.newFixedThreadPool(2);
        CountDownLatch liberarCatalogo = new CountDownLatch(1);
        CompletableFuture<Long> conexaoBloqueadora = new CompletableFuture<>();
        try {
            Future<?> bloqueador = pool.submit(() -> emTransacao(() -> {
                em.find(catalogo.entidade(), id, LockModeType.PESSIMISTIC_WRITE);
                conexaoBloqueadora.complete(((Number) em.createNativeQuery("select connection_id()")
                        .getSingleResult()).longValue());
                try {
                    if (!liberarCatalogo.await(30, TimeUnit.SECONDS))
                        throw new AssertionError("Limite da fixture: catálogo não liberado");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt(); throw new IllegalStateException(ex);
                }
                return null;
            }));
            long bloqueadora = conexaoBloqueadora.get(10, TimeUnit.SECONDS);
            Future<MvcResult> tentativa = pool.submit(() -> mvc.perform(request).andReturn());
            aguardarBloqueioReal(bloqueadora, tentativa);
            executar(c.admin, put("/api/usuarios/" + c.localA.getId()).contentType(MediaType.APPLICATION_JSON)
                            .content(json(mapa("active", false))), 200,
                    "Admin revoga o executor durante a espera real de " + recurso + " " + operacao);
            assertThat(emTransacao(() -> {
                em.clear();
                return em.find(UsuarioAplicacao.class, c.localA.getId()).getActive();
            })).as("Revogação commitada e relida em transação independente").isFalse();
            assertThat(tentativa.isDone()).as("Comando ainda aguarda após o commit da revogação").isFalse();
            liberarCatalogo.countDown(); bloqueador.get(10, TimeUnit.SECONDS);
            MvcResult resposta = tentativa.get(15, TimeUnit.SECONDS);
            Estado depois = estado(catalogo, id);
            Path evidencia = Path.of("target/auditoria-http/candidato8-catalogo-mestre-executor-" + recurso
                    + "-" + operacao.toLowerCase(Locale.ROOT) + ".json");
            Files.createDirectories(evidencia.getParent());
            Files.writeString(evidencia, json(mapa("hipotese", "C8-PES-H02", "recurso", recurso,
                    "operacao", operacao, "status", resposta.getResponse().getStatus(),
                    "estadoAntes", antes, "estadoDepois", depois, "espera_mysql_observada", true,
                    "revogacao_commitou_antes_da_liberacao", true)));
            assertAll("Gestor revogado não confirma a administração do catálogo após a espera",
                    () -> assertThat(resposta.getResponse().getStatus()).isIn(401, 403, 404, 409),
                    () -> assertThat(depois).as("Catálogo, versão e toda a trilha permanecem idênticos")
                            .isEqualTo(antes));
        } finally {
            liberarCatalogo.countDown(); pool.shutdownNow(); pool.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private MockHttpServletRequestBuilder pedido(String rota, String operacao, Map<String, Object> body)
            throws Exception {
        return operacao.equals("DELETE") ? delete(rota)
                : put(rota).contentType(MediaType.APPLICATION_JSON).content(json(body));
    }

    /** Payloads explícitos equivalentes aos contratos positivos de CatalogosContratoHttpIT. */
    private Map<String, Object> payload(String recurso, Cenario c, String rotulo, boolean ativo) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put(recurso.equals("cargos") || recurso.equals("lotacoes") ? "nome" : "descricao", rotulo);
        body.put("ativo", ativo); body.put("gestaoLocalId", c.gestaoA.getId());
        if (recurso.equals("tipos-contratacao")) body.put("matriculaVinculoObrigatoria", false);
        if (recurso.equals("motivos-desligamento")) body.put("categoria", "DESLIGAMENTO_INDIVIDUAL");
        if (recurso.equals("lotacoes")) {
            body.put("municipio", "CURITIBA");
            body.put("turnos", List.of(Map.of("id", c.jornadaA.getId())));
        }
        if (recurso.equals("jornadas")) {
            List<Map<String, Object>> dias = new ArrayList<>();
            for (DayOfWeek dia : DayOfWeek.values()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("diaSemana", dia.name()); item.put("diaTrabalho", dia.getValue() <= 5);
                if (dia.getValue() <= 5) { item.put("entrada1", "08:00"); item.put("saida1", "16:00"); }
                dias.add(item);
            }
            body.put("dias", dias);
        }
        return body;
    }

    private Catalogo catalogo(String recurso) {
        return switch (recurso) {
            case "cargos" -> new Catalogo(Cargo.class, TipoCatalogoTemporal.CARGO);
            case "lotacoes" -> new Catalogo(Lotacao.class, TipoCatalogoTemporal.LOTACAO);
            case "jornadas" -> new Catalogo(Jornada.class, TipoCatalogoTemporal.JORNADA);
            case "tipos-contratacao" -> new Catalogo(TipoContratacao.class, TipoCatalogoTemporal.TIPO_CONTRATACAO);
            case "motivos-desligamento" -> new Catalogo(MotivoDesligamento.class, TipoCatalogoTemporal.MOTIVO_DESLIGAMENTO);
            default -> throw new IllegalArgumentException("Recurso fora do escopo literal: " + recurso);
        };
    }

    private Estado estado(Catalogo catalogo, long id) {
        return emTransacao(() -> {
            em.clear();
            Object item = em.find(catalogo.entidade(), id);
            assertThat(item).as("A identidade do catálogo permanece persistida").isNotNull();
            List<Evento> eventos = em.createQuery("select e from EventoDisponibilidadeCatalogo e "
                            + "where e.tipoCatalogo=:tipo and e.catalogoId=:id order by e.id",
                    EventoDisponibilidadeCatalogo.class).setParameter("tipo", catalogo.tipo())
                    .setParameter("id", id).getResultList().stream().map(e -> new Evento(e.getId(),
                            e.getDataEfetiva(), e.getAtivoAnterior(), e.getAtivo(), e.getRegistradoEm(),
                            e.getMotivo(), e.getEvidencia(), e.getAutorizacao(), e.getChaveOrigem(),
                            e.getChaveIdempotencia(), e.getHashIntencao(), e.getResponsavelId(),
                            e.getResponsavelUsername(), e.getAtivoAtualAposRegistro(), e.getVersaoCatalogoApos())).toList();
            if (item instanceof Cargo e)
                return new Estado(id, e.getVersion(), e.getNome(), e.getAtivo(), e.getGestaoLocal().getId(), eventos);
            if (item instanceof Lotacao e)
                return new Estado(id, e.getVersion(), e.getNome(), e.getAtivo(), e.getGestaoLocal().getId(), eventos);
            if (item instanceof Jornada e)
                return new Estado(id, e.getVersion(), e.getDescricao(), e.getAtivo(), e.getGestaoLocal().getId(), eventos);
            if (item instanceof TipoContratacao e)
                return new Estado(id, e.getVersion(), e.getDescricao(), e.getAtivo(), e.getGestaoLocal().getId(), eventos);
            if (item instanceof MotivoDesligamento e)
                return new Estado(id, e.getVersion(), e.getDescricao(), e.getAtivo(), e.getGestaoLocal().getId(), eventos);
            throw new IllegalArgumentException("Entidade inesperada: " + item.getClass().getName());
        });
    }

    private void aguardarBloqueioReal(long bloqueadora, Future<?> tentativa) throws Exception {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        CompletableFuture<Void> observada = new CompletableFuture<>();
        var observador = Executors.newSingleThreadScheduledExecutor();
        var consulta = observador.scheduleWithFixedDelay(() -> {
            try {
                Long esperas = jdbc.queryForObject("select count(*) from performance_schema.data_lock_waits w "
                        + "join performance_schema.threads t on t.THREAD_ID=w.BLOCKING_THREAD_ID "
                        + "where t.PROCESSLIST_ID=?", Long.class, bloqueadora);
                if (esperas != null && esperas > 0) observada.complete(null);
                else if (tentativa.isDone()) observada.completeExceptionally(
                        new AssertionError("Comando não alcançou a espera real no catálogo"));
            } catch (Throwable ex) { observada.completeExceptionally(ex); }
        }, 0, 25, TimeUnit.MILLISECONDS);
        try { observada.get(10, TimeUnit.SECONDS); }
        finally { consulta.cancel(true); observador.shutdownNow(); observador.awaitTermination(5, TimeUnit.SECONDS); }
    }

    private record Catalogo(Class<?> entidade, TipoCatalogoTemporal tipo) {}
    private record Estado(long id, long versao, String rotulo, Boolean ativo, long gestaoLocalId, List<Evento> eventos) {}
    private record Evento(long id, LocalDate dataEfetiva, Boolean ativoAnterior, Boolean ativo, Instant registradoEm,
                          String motivo, String evidencia, String autorizacao, String chaveOrigem,
                          String chaveIdempotencia, String hashIntencao, Long responsavelId,
                          String responsavelUsername, Boolean ativoAtualAposRegistro, Long versaoCatalogoApos) {}
}
