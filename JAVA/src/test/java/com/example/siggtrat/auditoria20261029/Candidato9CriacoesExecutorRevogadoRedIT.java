package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.dominio.acesso.TipoPermissaoExcepcional;
import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.example.siggtrat.entidade.*;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Hipóteses C9-CON-H01/H02/H03/H04, autoria /root/revisao_final_contratos.
 * Fonte C9 bb2bd19e2015cfdab1ca6bea8d07e7d038b820e5d59f2216eb44cf9144c085b6.
 * Quatro fronteiras distintas: novo Usuario/FK de SE; concessão/FK de Contrato;
 * novo Cargo/FK de Gestão (representante dos cinco catálogos); nova Regra/FK de
 * Jornada. Status.criar e Permissao.revogar foram descartados porque já retêm
 * PESSIMISTIC_READ no executor. Não há mudança de produção nem execução pelo autor.
 * O controle positivo e a tentativa usam o mesmo comando e perfil permitido.
 * A ordem depende da espera física no PRIMARY da tabela/ID informados e de
 * revogação HTTP commitada antes da liberação. Uma falha dessas premissas é
 * falha de preparação, não comprovação do defeito de autorização.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@TestPropertySource(properties = {"spring.cache.type=redis", "spring.sql.init.mode=never",
        "app.cache.redis.http-get.enabled=true"})
class Candidato9CriacoesExecutorRevogadoRedIT extends ContratosHttpBase {
    @Autowired private DataSource dataSource;

    @ParameterizedTest(name = "criacao={0}")
    @ValueSource(strings = {"USUARIO", "PERMISSAO", "CARGO", "REGRA"})
    @Timeout(value = 75, unit = TimeUnit.SECONDS)
    void criacaoExigeExecutorCorrenteDepoisDaEsperaNaReferencia(String fluxo) throws Exception {
        try (var conexao = dataSource.getConnection()) {
            assertThat(conexao.getMetaData().getDatabaseProductName()).isEqualTo("MySQL");
        }
        Plano p = preparar(criarCenarioPersistido(), fluxo);
        JsonNode antes = p.estado().get();
        assertThat(antes.path("registros").size()).as("Controle positivo realmente persistiu uma entidade").isEqualTo(1);
        assertThat(antes.path("idsTentativa").size()).as("Identidade exclusiva ainda não existe").isZero();

        var pedido = autenticar(post(p.rota()).contentType(APPLICATION_JSON).content(json(p.corpo())), p.executor());
        // Nos catálogos, retirar o perfil local também limpa a FK do executor
        // para Gestão: a revogação não precisa modificar a referência bloqueada.
        boolean retirarPerfilLocal = p.executor().getPerfil() == PerfilAcesso.GESTOR_LOCAL;
        Map<String, Object> revogacaoBody = retirarPerfilLocal
                ? mapa("perfil", "GESTOR_NACIONAL") : mapa("active", false);
        var pool = Executors.newFixedThreadPool(3);
        CountDownLatch liberar = new CountDownLatch(1);
        CompletableFuture<Long> conexaoBloqueadora = new CompletableFuture<>();
        try {
            Future<?> bloqueador = pool.submit(() -> emTransacao(() -> {
                // Somente a referência já existente: nenhum campo nem Usuario é alterado.
                assertThat(em.find(p.raiz(), p.raizId(), LockModeType.PESSIMISTIC_WRITE)).isNotNull();
                conexaoBloqueadora.complete(((Number) em.createNativeQuery("select connection_id()")
                        .getSingleResult()).longValue());
                try {
                    if (!liberar.await(40, TimeUnit.SECONDS))
                        throw new AssertionError("Limite da fixture: referência não liberada");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt(); throw new IllegalStateException(ex);
                }
                return null;
            }));
            long conexao = conexaoBloqueadora.get(10, TimeUnit.SECONDS);
            Future<MvcResult> tentativa = pool.submit(() -> mvc.perform(pedido).andReturn());
            List<Map<String, Object>> espera = aguardarBloqueioReal(conexao, p.tabela(), p.raizId(), tentativa);

            // Outro Admin e outra transação. O limite evita deixar a fixture
            // presa caso uma guarda já retenha corretamente o executor.
            Future<?> revogacao = pool.submit(() -> {
                executar(p.revogador(), put("/api/usuarios/" + p.executor().getId())
                                .contentType(APPLICATION_JSON).content(json(revogacaoBody)), 200,
                        "Outro Admin retira a autoridade do executor enquanto o INSERT aguarda a referência");
                return null;
            });
            revogacao.get(12, TimeUnit.SECONDS);
            emTransacao(() -> {
                em.clear();
                UsuarioAplicacao atual = em.find(UsuarioAplicacao.class, p.executor().getId());
                if (retirarPerfilLocal) {
                    assertThat(atual.getPerfil()).isEqualTo(PerfilAcesso.GESTOR_NACIONAL);
                    assertThat(atual.getGestaoLocal()).isNull();
                    assertThat(atual.getActive()).isTrue();
                } else {
                    assertThat(atual.getActive())
                            .as("Desativação já commitada, relida em transação nova antes da liberação").isFalse();
                }
                return null;
            });
            assertThat(tentativa.isDone()).as("Pedido ainda bloqueado após a revogação commitada").isFalse();
            liberar.countDown();
            bloqueador.get(10, TimeUnit.SECONDS);
            MvcResult resultado = tentativa.get(15, TimeUnit.SECONDS);
            JsonNode depois = p.estado().get();

            Path arquivo = Path.of("target/auditoria-http/candidato9-criacao-executor-" + fluxo + ".json");
            Files.createDirectories(arquivo.getParent());
            Files.writeString(arquivo, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapa(
                    "autor", "/root/revisao_final_contratos", "hipotese", p.hipotese(), "fluxo", fluxo,
                    "audit_round", System.getenv("SGT_AUDITORIA_RODADA"), "recorded_at", Instant.now().toString(),
                    "controlePositivoStatus", p.statusControle(), "executorId", p.executor().getId(),
                    "tipoRevogacao", retirarPerfilLocal ? "PERDA_PERFIL_LOCAL" : "DESATIVACAO_ADMIN",
                    "revogadorId", p.revogador().getId(), "tabelaBloqueada", p.tabela(), "registroBloqueado", p.raizId(),
                    "esperaMySqlObservada", true, "esperaFisica", espera,
                    "revogacaoCommitadaAntesDaLiberacao", true,
                    "status", resultado.getResponse().getStatus(), "antes", antes, "depois", depois)));
            assertAll("Criar exige autoridade corrente depois da espera: " + fluxo,
                    () -> assertThat(resultado.getResponse().getStatus()).isIn(401, 403, 404, 409),
                    () -> assertThat(depois.path("idsTentativa").size())
                            .as("Tentativa revogada não deixa nova entidade").isZero(),
                    () -> assertThat(depois)
                            .as("Controle, referência, versões e fatos capturados permanecem iguais").isEqualTo(antes));
        } finally {
            liberar.countDown();
            pool.shutdownNow();
            pool.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private Plano preparar(Cenario c, String fluxo) throws Exception {
        return switch (fluxo) {
            case "USUARIO" -> prepararUsuario(c);
            case "PERMISSAO" -> prepararPermissao(c);
            case "CARGO" -> prepararCargo(c);
            case "REGRA" -> prepararRegra(c);
            default -> throw new IllegalArgumentException(fluxo);
        };
    }

    private Plano prepararUsuario(Cenario c) throws Exception {
        UsuarioAplicacao outroAdmin = outroAdministrador();
        String controle = "c9-controle-" + UUID.randomUUID().toString().replace("-", "");
        String tentativa = "c9-tentativa-" + UUID.randomUUID().toString().replace("-", "");
        var controleBody = dadosUsuario(controle, matriculaLivre(), c.seA.getId());
        assertThat(enviar(c.admin, "/api/usuarios", controleBody, 201,
                "Controle positivo: Admin cria Gestor Estadual na SE informada").path("id").asLong()).isPositive();
        var tentativaBody = dadosUsuario(tentativa, matriculaLivre(), c.seA.getId());
        return new Plano("C9-CON-H01", c.admin, outroAdmin, SuperintendenciaEstadual.class,
                c.seA.getId(), "superintendencia_estadual", "/api/usuarios", tentativaBody, 201,
                () -> estadoUsuarios(c.seA.getId(), controle, tentativa));
    }

    private Map<String, Object> dadosUsuario(String username, String matricula, long seId) {
        return mapa("username", username, "email", username + "@correios.com.br", "matricula", matricula,
                "password", SENHA, "perfil", "GESTOR_ESTADUAL", "superintendenciaId", seId, "active", true);
    }

    private String matriculaLivre() {
        // Faixa abaixo de 30 milhões, fora da sequência dos helpers legados.
        // Não consumir o maior valor do banco evita colisão com a próxima fixture.
        for (;;) {
            String numero = String.format(Locale.ROOT, "%08d", 10_000_000L
                    + Math.floorMod(UUID.randomUUID().getMostSignificantBits(), 10_000_000L));
            long quantidade = emTransacao(() -> em.createQuery(
                            "select count(u) from UsuarioAplicacao u where u.matricula=:m", Long.class)
                    .setParameter("m", numero).getSingleResult());
            if (quantidade == 0) return numero;
        }
    }

    private Plano prepararPermissao(Cenario c) throws Exception {
        UsuarioAplicacao outroAdmin = outroAdministrador();
        assertThat(enviar(c.admin, "/api/permissoes-excepcionais",
                dadosPermissao(c.localB.getId(), c.contratoB.getId()), 201,
                "Controle positivo: Admin concede correção ao Gestor responsável pelo contrato B")
                .path("id").asLong()).isPositive();
        return new Plano("C9-CON-H02", c.admin, outroAdmin, Contrato.class,
                c.contratoA.getId(), "contrato", "/api/permissoes-excepcionais",
                dadosPermissao(c.localA.getId(), c.contratoA.getId()), 201,
                () -> estadoPermissoes(c));
    }

    private Map<String, Object> dadosPermissao(long beneficiarioId, long contratoId) {
        return mapa("tipo", "CORRECAO_IDENTIFICADOR_CONTRATO", "usuarioBeneficiarioId", beneficiarioId,
                "contratoId", contratoId, "finalidade", "Controle sintético C9 de autorização corrente",
                "justificativa", "Ato sintético explicitamente limitado ao beneficiário e contrato deste cenário",
                "autorizacaoAdministrativa", "ATO-SINTETICO-C9", "validoAte", Instant.now().plusSeconds(7200));
    }

    private Plano prepararCargo(Cenario c) throws Exception {
        String controle = "Cargo C9 controle " + UUID.randomUUID().toString().substring(0, 8);
        String tentativa = "Cargo C9 tentativa " + UUID.randomUUID().toString().substring(0, 8);
        assertThat(enviar(c.localA, "/api/cargos", mapa("nome", controle, "ativo", true), 200,
                "Controle positivo: Gestor Local cria Cargo e origem contemporânea na própria Gestão")
                .path("id").asLong()).isPositive();
        JsonNode estado = estadoCargos(c.gestaoA.getId(), controle, tentativa);
        assertThat(estado.path("trilha").size()).as("Controle possui um evento de disponibilidade").isEqualTo(1);
        return new Plano("C9-CON-H03", c.localA, c.admin, GestaoLocal.class,
                c.gestaoA.getId(), "gestao_local", "/api/cargos", mapa("nome", tentativa, "ativo", true), 200,
                () -> estadoCargos(c.gestaoA.getId(), controle, tentativa));
    }

    private Plano prepararRegra(Cenario c) throws Exception {
        String rota = "/api/jornadas/" + c.jornadaA.getId() + "/regras-adicionais";
        assertThat(enviar(c.localA, rota, dadosRegra(90), 201,
                "Controle positivo: Gestor Local cria regra válida no turno da própria Gestão")
                .path("id").asLong()).isPositive();
        return new Plano("C9-CON-H04", c.localA, c.admin, Jornada.class, c.jornadaA.getId(),
                "jornada", rota, dadosRegra(91), 201, () -> estadoRegras(c.jornadaA.getId()));
    }

    private Map<String, Object> dadosRegra(int ordem) {
        return mapa("tipo", "EXTRA", "inicio", "08:00", "fim", "09:00", "percentual", 50,
                "ativo", true, "ordem", ordem);
    }

    private UsuarioAplicacao outroAdministrador() {
        // Fixture de autoridade independente, usando o helper existente; não
        // confundir sua preparação com a criação HTTP que é objeto do primeiro caso.
        return emTransacao(() -> {
            UsuarioAplicacao admin = usuario(PerfilAcesso.ADMINISTRADOR, null);
            em.flush(); return admin;
        });
    }

    private JsonNode estadoUsuarios(long seId, String controle, String tentativa) {
        return emTransacao(() -> {
            em.clear();
            var registros = em.createQuery("select u.id,u.username,u.email,u.matricula,u.perfil,u.active,"
                            + "u.version,u.superintendencia.id,u.forceChangePassword from UsuarioAplicacao u "
                            + "where u.username=:c or u.username=:t order by u.id", Object[].class)
                    .setParameter("c", controle).setParameter("t", tentativa).getResultList();
            var ids = em.createQuery("select u.id from UsuarioAplicacao u where u.username=:t order by u.id", Long.class)
                    .setParameter("t", tentativa).getResultList();
            return mapper.valueToTree(mapa("registros", registros, "idsTentativa", ids,
                    "referencia", resumoRaiz(SuperintendenciaEstadual.class, seId)));
        });
    }

    private JsonNode estadoPermissoes(Cenario c) {
        return emTransacao(() -> {
            em.clear();
            var tipo = TipoPermissaoExcepcional.CORRECAO_IDENTIFICADOR_CONTRATO;
            var contratos = List.of(c.contratoA.getId(), c.contratoB.getId());
            var registros = em.createQuery("select p.id,p.version,p.tipo,p.usuarioBeneficiario.id,p.contrato.id,"
                            + "p.ativo,p.concedidoEm,p.concedidoPorId,p.concedidoPorUsername,p.validoAte,"
                            + "p.finalidade,p.justificativa,p.autorizacaoAdministrativa,p.revogadoEm "
                            + "from PermissaoExcepcional p where p.tipo=:t and p.contrato.id in :cs order by p.id", Object[].class)
                    .setParameter("t", tipo).setParameter("cs", contratos).getResultList();
            var ids = em.createQuery("select p.id from PermissaoExcepcional p where p.tipo=:t "
                            + "and p.contrato.id=:c order by p.id", Long.class)
                    .setParameter("t", tipo).setParameter("c", c.contratoA.getId()).getResultList();
            var usos = em.createQuery("select u.id from UsoPermissaoExcepcional u "
                            + "where u.permissao.contrato.id in :cs order by u.id", Long.class)
                    .setParameter("cs", contratos).getResultList();
            return mapper.valueToTree(mapa("registros", registros, "idsTentativa", ids, "usos", usos,
                    "referencia", resumoRaiz(Contrato.class, c.contratoA.getId())));
        });
    }

    private JsonNode estadoCargos(long gestaoId, String controle, String tentativa) {
        return emTransacao(() -> {
            em.clear();
            var nomes = List.of(controle, tentativa);
            var registros = em.createQuery("select c.id,c.nome,c.ativo,c.version,c.gestaoLocal.id,c.cbo,c.descricao "
                            + "from Cargo c where c.gestaoLocal.id=:g and c.nome in :ns order by c.id", Object[].class)
                    .setParameter("g", gestaoId).setParameter("ns", nomes).getResultList();
            var ids = em.createQuery("select c.id from Cargo c where c.gestaoLocal.id=:g "
                            + "and c.nome=:n order by c.id", Long.class)
                    .setParameter("g", gestaoId).setParameter("n", tentativa).getResultList();
            var eventos = em.createQuery("select e.id,e.catalogoId,e.dataEfetiva,e.registradoEm,e.ativo,"
                            + "e.ativoAnterior,e.responsavelId,e.responsavelUsername,e.motivo,e.chaveOrigem "
                            + "from EventoDisponibilidadeCatalogo e where e.tipoCatalogo=:t and e.catalogoId in "
                            + "(select c.id from Cargo c where c.gestaoLocal.id=:g and c.nome in :ns) order by e.id", Object[].class)
                    .setParameter("t", TipoCatalogoTemporal.CARGO).setParameter("g", gestaoId)
                    .setParameter("ns", nomes).getResultList();
            return mapper.valueToTree(mapa("registros", registros, "idsTentativa", ids, "trilha", eventos,
                    "referencia", resumoRaiz(GestaoLocal.class, gestaoId)));
        });
    }

    private JsonNode estadoRegras(long jornadaId) {
        return emTransacao(() -> {
            em.clear();
            var registros = em.createQuery("select r.id,r.version,r.jornada.id,r.tipo,r.inicio,r.fim,"
                            + "r.percentual,r.ativo,r.ordem from RegraAdicional r "
                            + "where r.jornada.id=:j order by r.id", Object[].class)
                    .setParameter("j", jornadaId).getResultList();
            var ids = em.createQuery("select r.id from RegraAdicional r where r.jornada.id=:j "
                            + "and r.ordem=91 order by r.id", Long.class)
                    .setParameter("j", jornadaId).getResultList();
            return mapper.valueToTree(mapa("registros", registros, "idsTentativa", ids,
                    "referencia", resumoRaiz(Jornada.class, jornadaId)));
        });
    }

    private Object resumoRaiz(Class<?> classe, long id) {
        Object raiz = em.find(classe, id);
        if (raiz instanceof SuperintendenciaEstadual e)
            return mapa("id", e.getId(), "version", e.getVersion(), "nome", e.getNome(), "uf", e.getUf(), "ativo", e.getAtivo());
        if (raiz instanceof GestaoLocal e)
            return mapa("id", e.getId(), "version", e.getVersion(), "nome", e.getNome(), "codigo", e.getCodigo(), "ativo", e.getAtivo());
        if (raiz instanceof Jornada e)
            return mapa("id", e.getId(), "version", e.getVersion(), "descricao", e.getDescricao(), "ativo", e.getAtivo());
        if (raiz instanceof Contrato e)
            return mapa("id", e.getId(), "version", e.getVersion(), "numero", e.getNumeroContrato(),
                    "ano", e.getAnoContrato(), "estado", e.getEstadoNegocial(), "ativo", e.getAtivo());
        throw new AssertionError("Referência da fixture ausente: " + classe.getSimpleName() + "/" + id);
    }

    private List<Map<String, Object>> aguardarBloqueioReal(long bloqueadora, String tabela, long id,
                                                          Future<?> tentativa) throws Exception {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        CompletableFuture<List<Map<String, Object>>> observada = new CompletableFuture<>();
        var observador = Executors.newSingleThreadScheduledExecutor();
        var consulta = observador.scheduleWithFixedDelay(() -> {
            try {
                List<Map<String, Object>> linhas = jdbc.queryForList("select w.REQUESTING_THREAD_ID as solicitante, "
                        + "w.BLOCKING_THREAD_ID as bloqueador, r.OBJECT_SCHEMA as esquema, r.OBJECT_NAME as tabela, "
                        + "r.INDEX_NAME as indice, r.LOCK_DATA as registro, r.LOCK_MODE as modo_solicitado, "
                        + "b.LOCK_MODE as modo_detido, r.LOCK_STATUS as estado "
                        + "from performance_schema.data_lock_waits w "
                        + "join performance_schema.threads t on t.THREAD_ID=w.BLOCKING_THREAD_ID "
                        + "join performance_schema.data_locks r on r.ENGINE_LOCK_ID=w.REQUESTING_ENGINE_LOCK_ID and r.ENGINE=w.ENGINE "
                        + "join performance_schema.data_locks b on b.ENGINE_LOCK_ID=w.BLOCKING_ENGINE_LOCK_ID and b.ENGINE=w.ENGINE "
                        + "where t.PROCESSLIST_ID=? and r.OBJECT_SCHEMA=database() and r.OBJECT_NAME=? "
                        + "and r.INDEX_NAME='PRIMARY' and r.LOCK_TYPE='RECORD' and r.LOCK_DATA=? "
                        + "and r.LOCK_STATUS='WAITING' and b.LOCK_STATUS='GRANTED' "
                        + "and b.OBJECT_SCHEMA=r.OBJECT_SCHEMA and b.OBJECT_NAME=r.OBJECT_NAME "
                        + "and b.INDEX_NAME=r.INDEX_NAME and b.LOCK_DATA=r.LOCK_DATA",
                        bloqueadora, tabela, Long.toString(id));
                if (!linhas.isEmpty()) observada.complete(List.copyOf(linhas));
                else if (tentativa.isDone()) observada.completeExceptionally(
                        new AssertionError("Comando não alcançou espera física em " + tabela + "/" + id));
            } catch (Throwable ex) { observada.completeExceptionally(ex); }
        }, 0, 25, TimeUnit.MILLISECONDS);
        try { return observada.get(10, TimeUnit.SECONDS); }
        finally {
            consulta.cancel(true);
            observador.shutdownNow();
            observador.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    private record Plano(String hipotese, UsuarioAplicacao executor, UsuarioAplicacao revogador,
                         Class<?> raiz, long raizId, String tabela, String rota,
                         Map<String, Object> corpo, int statusControle, Supplier<JsonNode> estado) {}
}
