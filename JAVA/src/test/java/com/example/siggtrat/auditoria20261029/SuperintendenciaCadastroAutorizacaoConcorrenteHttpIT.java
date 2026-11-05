package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.constante.ConstantesDominio;
import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.entidade.SuperintendenciaEstadual;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
 * Autoria: revisor independente /root/revisao_final_transversais.
 * C10 e58cd276bad1395ce910dd9fc466c50031d1579f12e8a69747ccfadcdfb95b99.
 * Executar serialmente em banco descartável dedicado, pela coordenação central.
 * A UF oficial SP do seed é reservada em UF sintética livre só durante o caso;
 * finally restaura UF/nome/ativo e registra o avanço natural de @Version.
 * Não repõe versão antiga, não remove seed nem modifica suas contas/Gestões.
 * POST espera UNIQUE de SP ocupada provisoriamente pela atualização da linha
 * do seed já reservada em UF sintética; toda essa atualização sofre rollback.
 * PUT espera a PK de uma SE oficial criada exclusivamente pelo próprio teste.
 * Sem mocks para banco, autorização, transações ou coordenação da espera.
 */
/**
 * Proposta permanente recuperada em 2026-10-31 por /root/revisao_autorizacao.
 * Origem preservada: red_estrutura_hierarquia_diagnostico/Candidato10SuperintendenciaCadastroExecutorRevogadoRedIT.java.
 * A execução RED/GREEN desta proposta cabe à coordenação central; não foi executada pelo autor.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SuperintendenciaCadastroAutorizacaoConcorrenteHttpIT extends ContratosHttpBase {
    private static final String BASE_SHA =
            "e58cd276bad1395ce910dd9fc466c50031d1579f12e8a69747ccfadcdfb95b99";
    private static final String UF = "SP";
    @Autowired private DataSource dataSource;

    @ParameterizedTest(name = "cadastroSuperintendencia={0}")
    @ValueSource(strings = {"CRIAR", "ATUALIZAR"})
    @Timeout(value = 100, unit = TimeUnit.SECONDS)
    void cadastroDeSuperintendenciaRevalidaAdministradorDepoisDaEspera(String fluxo) throws Exception {
        exigirIndiceUnico();
        Cenario c = criarCenarioPersistido();
        UsuarioAplicacao ator = emTransacao(() -> {
            UsuarioAplicacao novo = usuario(PerfilAcesso.ADMINISTRADOR, null);
            em.flush(); return novo;
        });
        assertThat(ator.getId()).isNotEqualTo(c.admin.getId());
        SeedReservado seed = reservarUfDoSeed();
        boolean seguroRestaurar = true;
        try {
            JsonNode criada = dados(executar(ator, post("/api/superintendencias").contentType(APPLICATION_JSON)
                    .content(json(mapa("uf", UF, "ativo", true))), 201,
                    "Controle positivo: Administrador cria uma SE de UF oficial temporariamente livre"));
            long controleId = criada.path("id").asLong();
            assertThat(controleId).isPositive().isNotEqualTo(seed.id());
            conferirSe(controleId, true);
            boolean criar = fluxo.equals("CRIAR");
            String rota = criar ? "/api/superintendencias" : "/api/superintendencias/" + controleId;
            if (criar) {
                emTransacao(() -> {
                    em.remove(em.find(SuperintendenciaEstadual.class, controleId)); em.flush(); return null;
                });
            } else {
                executar(ator, put(rota).contentType(APPLICATION_JSON)
                        .content(json(mapa("uf", UF, "ativo", false))), 200,
                        "Controle positivo: Administrador inativa a SE própria do teste sem alterar sua UF");
                conferirSe(controleId, false);
                emTransacao(() -> {
                    em.find(SuperintendenciaEstadual.class, controleId).setAtivo(true);
                    em.flush(); return null;
                });
                conferirSe(controleId, true);
            }
            MockHttpServletRequestBuilder pedido = autenticar((criar ? post(rota) : put(rota))
                    .contentType(APPLICATION_JSON).content(json(mapa("uf", UF, "ativo", criar))), ator);
            JsonNode antes = estado(seed, ator.getId(), rota);
            var pool = Executors.newFixedThreadPool(2);
            CountDownLatch liberar = new CountDownLatch(1);
            CompletableFuture<Barreira> pronta = new CompletableFuture<>();
            seguroRestaurar = false;
            try {
                Future<?> bloqueador = pool.submit(() -> new TransactionTemplate(transactionManager).execute(status -> {
                    try {
                        SuperintendenciaEstadual alvo;
                        if (criar) {
                            alvo = em.find(SuperintendenciaEstadual.class, seed.id(), LockModeType.PESSIMISTIC_WRITE);
                            assertThat(alvo).as("A barreira usa a linha já reservada, não INSERT implícito").isNotNull();
                            assertThat(alvo.getUf()).isEqualTo(seed.ufTemporaria());
                            int alteradas = em.createQuery(
                                            "update SuperintendenciaEstadual set uf=:uf,nome=:nome where id=:id")
                                    .setParameter("uf", UF).setParameter("nome", "SE/" + UF)
                                    .setParameter("id", seed.id()).executeUpdate();
                            assertThat(alteradas).as("A barreira alterou apenas a linha reservada no MySQL").isEqualTo(1);
                            String ufConfirmada = String.valueOf(em.createNativeQuery(
                                            "select uf from superintendencia_estadual where id_superintendencia=:id")
                                    .setParameter("id", seed.id()).getSingleResult());
                            assertThat(ufConfirmada).as("SELECT escalar confirma a UF efetivamente escrita").isEqualTo(UF);
                            long linhaNoIndice = ((Number) em.createNativeQuery(
                                            "select id_superintendencia from superintendencia_estadual "
                                                    + "force index (uk_superintendencia_estadual_uf) where uf=:uf for update")
                                    .setParameter("uf", UF).getSingleResult()).longValue();
                            assertThat(linhaNoIndice).as("A UNIQUE disputada aponta para o seed reservado").isEqualTo(seed.id());
                        } else {
                            alvo = em.find(SuperintendenciaEstadual.class, controleId, LockModeType.PESSIMISTIC_WRITE);
                        }
                        assertThat(alvo).isNotNull();
                        long conexao = ((Number) em.createNativeQuery("select connection_id()")
                                .getSingleResult()).longValue();
                        if (criar) {
                            gravar("superintendencia-criacao-barreira-confirmada", mapa(
                                    "audit_round", System.getenv("SGT_AUDITORIA_RODADA"), "recorded_at", Instant.now().toString(),
                                    "metodo", "UPDATE parametrizado, SELECT escalar e leitura UNIQUE FOR UPDATE",
                                    "conexao", conexao, "linha", alvo.getId(), "ufConfirmada", UF,
                                    "rollbackObrigatorio", true, "somenteFixtureSintetica", true));
                        }
                        pronta.complete(new Barreira(conexao, alvo.getId()));
                        if (!liberar.await(50, TimeUnit.SECONDS))
                            throw new AssertionError("Limite da fixture: SE bloqueadora não liberada");
                        // A reserva UNIQUE desaparece antes de a tentativa poder inserir SP.
                        // Também desfaz qualquer alteração acidental do bloqueador do PUT.
                        status.setRollbackOnly();
                        return null;
                    } catch (Throwable ex) {
                        pronta.completeExceptionally(ex);
                        if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
                        throw new IllegalStateException(ex);
                    }
                }));
                Barreira b = pronta.get(10, TimeUnit.SECONDS);
                Future<MvcResult> tentativa = pool.submit(() -> mvc.perform(pedido).andReturn());
                List<Map<String, Object>> espera = aguardarBloqueioReal(b, criar, tentativa);
                Instant esperaObservadaEm = Instant.now();
                executar(c.admin, put("/api/usuarios/" + ator.getId()).contentType(APPLICATION_JSON)
                        .content(json(mapa("perfil", "GESTOR_NACIONAL"))), 200,
                        "Outro Administrador confirma o rebaixamento antes de remover a barreira física da SE");
                JsonNode executorDepois = emTransacao(() -> {
                    em.clear();
                    UsuarioAplicacao atual = em.find(UsuarioAplicacao.class, ator.getId());
                    assertThat(atual.getPerfil()).isEqualTo(PerfilAcesso.GESTOR_NACIONAL);
                    assertThat(atual.getActive()).isTrue();
                    assertThat(atual.getSuperintendencia()).isNull();
                    assertThat(atual.getGestaoLocal()).isNull();
                    return mapper.valueToTree(mapa("id", atual.getId(), "perfil", atual.getPerfil(),
                            "active", atual.getActive(), "version", atual.getVersion()));
                });
                Instant revogacaoConferidaEm = Instant.now();
                assertThat(tentativa.isDone()).as("Tentativa ainda bloqueada após o commit da revogação").isFalse();
                Instant liberacaoEm = Instant.now();
                liberar.countDown();
                bloqueador.get(10, TimeUnit.SECONDS);
                MvcResult resultado = tentativa.get(15, TimeUnit.SECONDS);
                JsonNode depois = estado(seed, ator.getId(), rota);
                gravar("candidato10-se-cadastro-executor-" + fluxo, mapa(
                        "audit_round", System.getenv("SGT_AUDITORIA_RODADA"), "recorded_at", Instant.now().toString(),
                        "source_manifest_origem_sha256", BASE_SHA, "hipotese", "C10-TR-SE-CADASTRO-EXECUTOR-" + fluxo,
                        "fluxo", fluxo, "metodo", criar ? "POST" : "PUT", "rota", rota,
                        "executorId", ator.getId(), "revogadorId", c.admin.getId(), "controlePositivoId", controleId,
                        "seedId", seed.id(), "ufOficial", UF, "ufTemporariaSeed", seed.ufTemporaria(),
                        "barreiraId", b.id(), "barreiraSofreuRollback", true,
                        "esperaMySqlObservada", true, "esperaDataLockWaits", espera,
                        "esperaObservadaEm", esperaObservadaEm, "revogacaoConferidaEm", revogacaoConferidaEm,
                        "liberacaoEm", liberacaoEm, "revogacaoCommitadaAntesDaLiberacao", true,
                        "executorAposRevogacao", executorDepois, "status", resultado.getResponse().getStatus(),
                        "antes", antes, "depois", depois));
                assertAll("O cadastro de SE exige Administrador corrente depois da espera: " + fluxo,
                        () -> assertThat(resultado.getResponse().getStatus()).isIn(401, 403, 404, 409),
                        () -> assertThat(depois).as("SEs e trilha ficam iguais; a revogação não pode ser ultrapassada")
                                .isEqualTo(antes));
            } finally {
                liberar.countDown(); pool.shutdownNow();
                seguroRestaurar = pool.awaitTermination(10, TimeUnit.SECONDS);
                if (!seguroRestaurar)
                    throw new AssertionError("Não restaurar seed enquanto houver pedido concorrente em execução");
            }
        } finally {
            if (seguroRestaurar) restaurarUfDoSeed(seed, fluxo);
            else gravar("candidato10-se-cadastro-custodia-seed-" + fluxo, mapa(
                    "audit_round", System.getenv("SGT_AUDITORIA_RODADA"), "recorded_at", Instant.now().toString(),
                    "fluxo", fluxo, "seedId", seed.id(), "ufOriginal", seed.ufOriginal(),
                    "ufTemporaria", seed.ufTemporaria(), "restauracaoPendente", true,
                    "motivo", "Preparação falhou: executor concorrente não encerrou; não restaurar enquanto ele escreve."));
        }
    }

    private SeedReservado reservarUfDoSeed() {
        return emTransacao(() -> {
            em.clear();
            List<SuperintendenciaEstadual> todas = em.createQuery("select s from SuperintendenciaEstadual s order by s.id",
                    SuperintendenciaEstadual.class).getResultList();
            List<SuperintendenciaEstadual> oficiais = todas.stream().filter(s -> UF.equals(s.getUf())).toList();
            assertThat(oficiais).as("O seed deve conter exatamente uma SE/SP").hasSize(1);
            SuperintendenciaEstadual original = oficiais.get(0);
            Set<String> usadas = new HashSet<>();
            todas.forEach(s -> usadas.add(s.getUf().toUpperCase(Locale.ROOT)));
            String livre = null;
            for (char a = 'A'; a <= 'Z' && livre == null; a++) {
                for (char b = 'A'; b <= 'Z'; b++) {
                    String candidata = "" + a + b;
                    if (!usadas.contains(candidata) && !ConstantesDominio.UFS_BRASIL.contains(candidata)) {
                        livre = candidata; break;
                    }
                }
            }
            assertThat(livre).as("UF sintética ASCII livre sob a collation real").isNotNull();
            SeedReservado r = new SeedReservado(original.getId(), original.getNome(), original.getUf(),
                    original.getAtivo(), original.getVersion(), livre, todas.stream().map(SuperintendenciaEstadual::getId).toList());
            original.setUf(livre); em.flush();
            assertThat(original.getVersion()).isGreaterThan(r.versionAntes());
            return r;
        });
    }

    private void restaurarUfDoSeed(SeedReservado seed, String fluxo) throws Exception {
        JsonNode recibo = emTransacao(() -> {
            em.clear();
            List<Long> removidas = new ArrayList<>();
            List<SuperintendenciaEstadual> criadas = em.createQuery("select s from SuperintendenciaEstadual s where s.uf=:uf",
                    SuperintendenciaEstadual.class).setParameter("uf", UF).getResultList();
            for (SuperintendenciaEstadual criada : criadas) {
                assertThat(seed.idsOriginais()).as("A limpeza só remove a SE criada pelo teste").doesNotContain(criada.getId());
                removidas.add(criada.getId()); em.remove(criada);
            }
            em.flush();
            SuperintendenciaEstadual original = em.find(SuperintendenciaEstadual.class, seed.id(), LockModeType.PESSIMISTIC_WRITE);
            assertThat(original.getUf()).isEqualTo(seed.ufTemporaria());
            long versaoDurante = original.getVersion();
            original.setUf(seed.ufOriginal()); original.setNome(seed.nomeOriginal()); original.setAtivo(seed.ativoOriginal());
            em.flush();
            assertThat(original.getUf()).isEqualTo(seed.ufOriginal());
            assertThat(original.getNome()).isEqualTo(seed.nomeOriginal());
            assertThat(original.getAtivo()).isEqualTo(seed.ativoOriginal());
            assertThat(original.getVersion()).isGreaterThan(versaoDurante);
            return mapper.valueToTree(mapa("audit_round", System.getenv("SGT_AUDITORIA_RODADA"),
                    "recorded_at", Instant.now().toString(), "fluxo", fluxo, "seedId", seed.id(),
                    "ufAntes", seed.ufOriginal(), "nomeAntes", seed.nomeOriginal(), "ativoAntes", seed.ativoOriginal(),
                    "ufTemporaria", seed.ufTemporaria(), "ufRestaurada", original.getUf(),
                    "nomeRestaurado", original.getNome(), "ativoRestaurado", original.getAtivo(),
                    "versionAntes", seed.versionAntes(), "versionDurante", versaoDurante,
                    "versionDepois", original.getVersion(), "reposicaoArtificialDeVersion", false,
                    "idsSeCriadasRemovidas", removidas, "seedRemovido", false));
        });
        gravar("candidato10-se-cadastro-custodia-seed-" + fluxo, recibo);
    }

    private void conferirSe(long id, boolean ativa) {
        emTransacao(() -> {
            em.clear();
            SuperintendenciaEstadual s = em.find(SuperintendenciaEstadual.class, id);
            assertThat(s).isNotNull(); assertThat(s.getUf()).isEqualTo(UF);
            assertThat(s.getNome()).isEqualTo("SE/" + UF); assertThat(s.getAtivo()).isEqualTo(ativa);
            return null;
        });
    }

    private void exigirIndiceUnico() {
        Long quantidade = new JdbcTemplate(dataSource).queryForObject(
                "select count(*) from information_schema.statistics where table_schema=database() "
                        + "and table_name='superintendencia_estadual' and index_name='uk_superintendencia_estadual_uf' "
                        + "and non_unique=0 and column_name='uf' and seq_in_index=1", Long.class);
        assertThat(quantidade).as("Premissa de schema Hibernate: UNIQUE física da UF").isEqualTo(1L);
    }

    private JsonNode estado(SeedReservado seed, long atorId, String rota) {
        return emTransacao(() -> {
            em.clear();
            var todas = em.createQuery("select s.id,s.nome,s.uf,s.ativo,s.version from SuperintendenciaEstadual s order by s.id",
                    Object[].class).getResultList();
            var trilha = em.createQuery("select a.id,a.action,a.entityType,a.entityId,a.detailsJson "
                            + "from RegistroAuditoria a where a.userId=:u and a.path=:p order by a.id", Object[].class)
                    .setParameter("u", atorId).setParameter("p", rota).getResultList();
            assertThat(em.find(SuperintendenciaEstadual.class, seed.id()).getUf()).isEqualTo(seed.ufTemporaria());
            return mapper.valueToTree(mapa("superintendencias", todas, "trilhaDoRecurso", trilha));
        });
    }

    private List<Map<String, Object>> aguardarBloqueioReal(Barreira b, boolean criar, Future<?> tentativa) throws Exception {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        var observador = Executors.newSingleThreadScheduledExecutor();
        CompletableFuture<List<Map<String, Object>>> observada = new CompletableFuture<>();
        String indice = criar ? "uk_superintendencia_estadual_uf" : "PRIMARY";
        var consulta = observador.scheduleWithFixedDelay(() -> {
            try {
                List<Map<String, Object>> esperas = jdbc.queryForList(
                        "select rt.PROCESSLIST_ID as requisitante,bt.PROCESSLIST_ID as bloqueador,"
                                + "r.OBJECT_NAME as tabela,r.INDEX_NAME as indice,r.LOCK_TYPE as tipo,"
                                + "r.LOCK_MODE as modo,r.LOCK_DATA as registro,b.LOCK_MODE as modoBloqueador,"
                                + "b.LOCK_DATA as registroBloqueador from performance_schema.data_lock_waits w "
                                + "join performance_schema.threads bt on bt.THREAD_ID=w.BLOCKING_THREAD_ID "
                                + "join performance_schema.threads rt on rt.THREAD_ID=w.REQUESTING_THREAD_ID "
                                + "join performance_schema.data_locks r on r.ENGINE=w.ENGINE and r.ENGINE_LOCK_ID=w.REQUESTING_ENGINE_LOCK_ID "
                                + "join performance_schema.data_locks b on b.ENGINE=w.ENGINE and b.ENGINE_LOCK_ID=w.BLOCKING_ENGINE_LOCK_ID "
                                + "where bt.PROCESSLIST_ID=? and r.OBJECT_SCHEMA=database() and r.OBJECT_NAME='superintendencia_estadual' "
                                + "and b.INDEX_NAME=? and b.LOCK_TYPE='RECORD'", b.conexao(), indice);
                List<Map<String, Object>> exatas = esperas.stream().filter(e -> {
                    String dado = String.valueOf(e.get("registroBloqueador"));
                    return criar ? dado.contains("'" + UF + "'") : dado.equals(Long.toString(b.id()));
                }).toList();
                if (!exatas.isEmpty()) observada.complete(exatas);
                else if (tentativa.isDone()) observada.completeExceptionally(new AssertionError(
                        "Tentativa terminou sem espera no índice/registro esperado: " + indice + "/" + (criar ? UF : b.id())));
            } catch (Throwable ex) { observada.completeExceptionally(ex); }
        }, 0, 25, TimeUnit.MILLISECONDS);
        try { return observada.get(10, TimeUnit.SECONDS); }
        finally {
            consulta.cancel(true); observador.shutdownNow(); observador.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    private void gravar(String nome, Object conteudo) throws Exception {
        Path p = Path.of("target/auditoria-http/" + nome + ".json");
        Files.createDirectories(p.getParent());
        Files.writeString(p, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(conteudo));
    }

    private record Barreira(long conexao, long id) { }
    private record SeedReservado(long id, String nomeOriginal, String ufOriginal, Boolean ativoOriginal,
                                 long versionAntes, String ufTemporaria, List<Long> idsOriginais) { }
}
