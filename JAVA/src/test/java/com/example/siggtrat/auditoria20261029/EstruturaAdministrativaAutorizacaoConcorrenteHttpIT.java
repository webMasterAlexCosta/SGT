package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.entidade.GestaoLocal;
import com.example.siggtrat.entidade.SuperintendenciaEstadual;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.utilitario.UtilitarioHashSeguranca;
import com.fasterxml.jackson.databind.JsonNode;
import com.example.siggtrat.infra.EsperaJdbcPostgres;
import com.example.siggtrat.infra.ObservadorAtorPostgres.Ator;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Autoria: revisor independente /root/revisao_final_transversais.
 * Base C10 e58cd276bad1395ce910dd9fc466c50031d1579f12e8a69747ccfadcdfb95b99.
 * Três hipóteses delimitadas: INSERT/FK de Gestão na SE, UPDATE da Gestão,
 * e reativação da SE. Administrador executor, revogador e gestor beneficiado
 * são diferentes. A espera PostgreSQL é observada no registro e conexão bloqueadores.
 * O autor não executa JVM/SQL: esta classe é proposta aditiva para execução central.
 * Não usa mocks de autorização/banco nem altera UFs oficiais. A SE sintética
 * serve à ação de ativar, que não recebe nem valida uma nova UF.
 */
/**
 * Proposta permanente recuperada em 2026-10-31 por /root/revisao_autorizacao.
 * Origem preservada: red_estrutura_fornecedor/Candidato10EstruturaAdministrativaExecutorRevogadoRedIT.java.
 * A execução RED/GREEN desta proposta cabe à coordenação central; não foi executada pelo autor.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class EstruturaAdministrativaAutorizacaoConcorrenteHttpIT extends ContratosHttpBase {
    private static final String BASE_SHA =
            "e58cd276bad1395ce910dd9fc466c50031d1579f12e8a69747ccfadcdfb95b99";
    @Autowired private DataSource dataSource;

    @ParameterizedTest(name = "estruturaAdministrativa={0}")
    @ValueSource(strings = {"GESTAO_CRIAR", "GESTAO_ATUALIZAR", "SUPERINTENDENCIA_ATIVAR"})
    @Timeout(value = 90, unit = TimeUnit.SECONDS)
    void escritaEstruturalRevalidaAdministradorDepoisDaEspera(String fluxo) throws Exception {
        Cenario c = criarCenarioPersistido();
        UsuarioAplicacao ator = emTransacao(() -> {
            UsuarioAplicacao novo = usuario(PerfilAcesso.ADMINISTRADOR, null);
            em.flush(); return novo;
        });
        assertThat(List.of(ator.getId(), c.admin.getId(), c.estadualA.getId())).doesNotHaveDuplicates();
        Preparacao p = prepararControlePositivo(fluxo, c, ator);
        var pedido = autenticar(comando(p), ator);
        JsonNode antes = estado(c, ator.getId(), p.rota());
        var pool = Executors.newFixedThreadPool(2);
        CountDownLatch liberar = new CountDownLatch(1);
        CompletableFuture<Long> conexaoBloqueadora = new CompletableFuture<>();
        var atorJdbc = new Ator("EstruturaAdministrativaAutorizacaoConcorrenteHttpIT-" + fluxo);
        try {
            Future<?> bloqueador = pool.submit(() -> emTransacao(() -> {
                String pk = fluxo.equals("GESTAO_ATUALIZAR") ? "id_gestao_local" : "id_superintendencia";
                assertThat(em.createNativeQuery("select "+pk+" from "+p.tabelaEsperada()+" where "+pk+"=:id for update")
                        .setParameter("id",p.registroBloqueado()).getSingleResult()).isNotNull();
                conexaoBloqueadora.complete(((Number) em.createNativeQuery("select pg_backend_pid()")
                        .getSingleResult()).longValue());
                try {
                    if (!liberar.await(50, TimeUnit.SECONDS))
                        throw new AssertionError("Limite da fixture: estrutura não liberada");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt(); throw new IllegalStateException(ex);
                }
                return null;
            }));
            long conexao = conexaoBloqueadora.get(10, TimeUnit.SECONDS);
            Future<MvcResult> tentativa = pool.submit(() -> atorJdbc.executar(() -> mvc.perform(pedido).andReturn()));
            List<Map<String, Object>> espera = EsperaJdbcPostgres.aguardar(new JdbcTemplate(dataSource),conexao,atorJdbc,
                    sql -> sql.selecaoChave(p.tabelaEsperada(),
                            fluxo.equals("GESTAO_ATUALIZAR") ? "id_gestao_local" : "id_superintendencia",p.registroBloqueado())
                            || (fluxo.equals("GESTAO_CRIAR") && sql.insercao("gestao_local","id_superintendencia",p.registroBloqueado())),
                    p.tabelaEsperada()+"/"+p.registroBloqueado(),tentativa);
            Instant esperaObservadaEm = Instant.now();

            executar(c.admin, put("/api/usuarios/" + ator.getId()).contentType(APPLICATION_JSON)
                    .content(json(mapa("perfil", "GESTOR_NACIONAL"))), 200,
                    "Outro Administrador retira o perfil do executor enquanto a escrita estrutural espera no PostgreSQL");
            JsonNode executorDepoisDaRevogacao = emTransacao(() -> {
                em.clear();
                UsuarioAplicacao atual = em.find(UsuarioAplicacao.class, ator.getId());
                assertThat(atual.getPerfil()).isEqualTo(PerfilAcesso.GESTOR_NACIONAL);
                assertThat(atual.getActive()).isTrue();
                assertThat(atual.getSuperintendencia()).isNull();
                assertThat(atual.getGestaoLocal()).isNull();
                assertThat(em.find(UsuarioAplicacao.class, c.admin.getId()).getPerfil())
                        .isEqualTo(PerfilAcesso.ADMINISTRADOR);
                return mapper.valueToTree(mapa("id", atual.getId(), "perfil", atual.getPerfil(),
                        "active", atual.getActive(), "version", atual.getVersion()));
            });
            Instant revogacaoConferidaEm = Instant.now();
            assertThat(tentativa.isDone()).as("A escrita continua esperando após o commit da revogação").isFalse();
            Instant liberacaoEm = Instant.now();
            liberar.countDown();
            bloqueador.get(10, TimeUnit.SECONDS);
            MvcResult resultado = tentativa.get(15, TimeUnit.SECONDS);
            JsonNode depois = estado(c, ator.getId(), p.rota());

            Path evidencia = Path.of("target/auditoria-http/candidato10-estrutura-executor-" + fluxo + ".json");
            Files.createDirectories(evidencia.getParent());
            Files.writeString(evidencia, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapa(
                    "audit_round", System.getenv("SGT_AUDITORIA_RODADA"), "recorded_at", Instant.now().toString(),
                    "source_manifest_origem_sha256", BASE_SHA,
                    "hipotese", "C10-TR-ESTRUTURA-EXECUTOR-" + fluxo,
                    "fluxo", fluxo, "metodo", p.metodo(), "rota", p.rota(),
                    "executorId", ator.getId(), "revogadorId", c.admin.getId(),
                    "gestorEstadualBeneficiadoId", c.estadualA.getId(), "controlePositivoId", p.controleId(),
                    "tabelaEsperada", p.tabelaEsperada(), "registroBloqueado", p.registroBloqueado(),
                    "esperaPostgresObservada", true, "esperaDataLockWaits", espera,
                    "esperaObservadaEm", esperaObservadaEm, "revogacaoConferidaEm", revogacaoConferidaEm,
                    "liberacaoEm", liberacaoEm, "revogacaoCommitadaAntesDaLiberacao", true,
                    "executorAposRevogacao", executorDepoisDaRevogacao,
                    "status", resultado.getResponse().getStatus(), "antes", antes, "depois", depois)));
            assertAll("A escrita estrutural exige autoridade corrente após a espera: " + fluxo,
                    () -> assertThat(resultado.getResponse().getStatus()).isIn(401, 403, 404, 409),
                    () -> assertThat(depois).as("SEs, Gestões, contas, credenciais, sessões e trilha ficam iguais")
                            .isEqualTo(antes));
        } finally {
            liberar.countDown();
            pool.shutdownNow();
            pool.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private Preparacao prepararControlePositivo(String fluxo, Cenario c, UsuarioAplicacao ator) throws Exception {
        if (fluxo.equals("SUPERINTENDENCIA_ATIVAR")) {
            colocarSuperintendenciaInativa(c);
            String rota = "/api/superintendencias/" + c.seA.getId() + "/ativar";
            JsonNode controle = dados(executar(ator, post(rota), 200,
                    "Controle positivo: Administrador reativa SE inativa e o gestor estadual único"));
            assertThat(controle.path("username").asText()).isEqualTo(c.estadualA.getUsername());
            assertThat(controle.path("forceChangeOnNextLogin").asBoolean()).isTrue();
            assertThat(controle.path("temporaryPassword").asText()).isNotBlank();
            emTransacao(() -> {
                em.clear();
                assertThat(em.find(SuperintendenciaEstadual.class, c.seA.getId()).getAtivo()).isTrue();
                UsuarioAplicacao gestor = em.find(UsuarioAplicacao.class, c.estadualA.getId());
                assertThat(gestor.getActive()).isTrue();
                assertThat(gestor.getForceChangePassword()).isTrue();
                assertThat(passwordEncoder.matches(controle.path("temporaryPassword").asText(), gestor.getPassword())).isTrue();
                assertThat(em.find(GestaoLocal.class, c.gestaoA.getId()).getAtivo()).isFalse();
                return null;
            });
            // Reestabelece somente a pré-condição da mesma ação na fixture própria.
            // Nenhuma UF oficial nem fonte documental é alterada.
            colocarSuperintendenciaInativa(c);
            return new Preparacao(rota, "POST", null, "superintendencia_estadual", c.seA.getId(), c.seA.getId());
        }
        boolean criar = fluxo.equals("GESTAO_CRIAR");
        String rota = criar ? "/api/gestoes-locais" : "/api/gestoes-locais/" + c.gestaoA.getId();
        String codigoControle = criar ? codigoNovo() : c.gestaoA.getCodigo();
        String nomeControle = "Controle estrutural " + UUID.randomUUID().toString().substring(0, 8);
        Map<String, Object> bodyControle = gestao(c, nomeControle, codigoControle);
        var controlePedido = (criar ? post(rota) : put(rota)).contentType(APPLICATION_JSON).content(json(bodyControle));
        JsonNode controle = dados(executar(ator, controlePedido, criar ? 201 : 200,
                "Controle positivo: o Administrador escreve a Gestão Local na SE ativa"));
        long id = controle.path("id").asLong();
        assertThat(id).isPositive();
        emTransacao(() -> {
            em.clear();
            GestaoLocal atual = em.find(GestaoLocal.class, id);
            assertThat(atual.getNome()).isEqualTo(nomeControle);
            assertThat(atual.getCodigo()).isEqualTo(codigoControle.toUpperCase(Locale.ROOT));
            assertThat(atual.getSuperintendencia().getId()).isEqualTo(c.seA.getId());
            assertThat(atual.getAtivo()).isTrue();
            return null;
        });
        Map<String, Object> tentativa = gestao(c, "Alteração concorrente " + UUID.randomUUID().toString().substring(0, 8),
                criar ? codigoNovo() : codigoControle);
        return new Preparacao(rota, criar ? "POST" : "PUT", tentativa,
                criar ? "superintendencia_estadual" : "gestao_local", criar ? c.seA.getId() : c.gestaoA.getId(), id);
    }

    private Map<String, Object> gestao(Cenario c, String nome, String codigo) {
        return mapa("nome", nome, "codigo", codigo, "ativo", true, "superintendenciaId", c.seA.getId());
    }

    private String codigoNovo() { return "C10-" + UUID.randomUUID().toString().substring(0, 20).toUpperCase(Locale.ROOT); }

    private AbstractMockHttpServletRequestBuilder<?> comando(Preparacao p) throws Exception {
        var r = p.metodo().equals("PUT") ? put(p.rota()) : post(p.rota());
        return p.body() == null ? r : r.contentType(APPLICATION_JSON).content(json(p.body()));
    }

    private void colocarSuperintendenciaInativa(Cenario c) {
        emTransacao(() -> {
            em.clear();
            em.find(SuperintendenciaEstadual.class, c.seA.getId()).setAtivo(false);
            em.createQuery("select g from GestaoLocal g where g.superintendencia.id=:s", GestaoLocal.class)
                    .setParameter("s", c.seA.getId()).getResultList().forEach(g -> g.setAtivo(false));
            em.createQuery("select u from UsuarioAplicacao u where u.superintendencia.id=:s", UsuarioAplicacao.class)
                    .setParameter("s", c.seA.getId()).getResultList().forEach(u -> u.setActive(false));
            em.flush(); return null;
        });
    }

    private JsonNode estado(Cenario c, long atorId, String rota) {
        return emTransacao(() -> {
            em.clear();
            List<Long> ses = List.of(c.seA.getId(), c.seB.getId());
            var superintendencias = em.createQuery("select s.id,s.nome,s.uf,s.ativo,s.version "
                            + "from SuperintendenciaEstadual s where s.id in :ses order by s.id", Object[].class)
                    .setParameter("ses", ses).getResultList();
            var gestoes = em.createQuery("select g.id,g.nome,g.codigo,g.ativo,g.superintendencia.id,g.version "
                            + "from GestaoLocal g where g.superintendencia.id in :ses order by g.id", Object[].class)
                    .setParameter("ses", ses).getResultList();
            var contas = em.createQuery("select u from UsuarioAplicacao u where u.superintendencia.id in :ses order by u.id",
                            UsuarioAplicacao.class).setParameter("ses", ses).getResultList();
            var usuarios = contas.stream().map(u -> mapa("id", u.getId(), "username", u.getUsername(),
                    "perfil", u.getPerfil(), "active", u.getActive(), "forceChange", u.getForceChangePassword(),
                    "version", u.getVersion(), "credencialSha256", UtilitarioHashSeguranca.sha256Hexadecimal(u.getPassword()),
                    "twoFactorEnabled", u.getTwoFactorEnabled(), "failedAttempts", u.getFailedAttempts(),
                    "lockoutEnd", u.getLockoutEnd(), "retroactiveUnlockExpireAt", u.getRetroactiveUnlockExpireAt(),
                    "superintendenciaId", u.getSuperintendencia().getId(),
                    "gestaoId", u.getGestaoLocal() == null ? null : u.getGestaoLocal().getId())).toList();
            var sessoes = em.createQuery("select t.id,t.username,t.createdAt,t.expireAt,t.lastSeenAt,t.version "
                            + "from TokenAtivo t where t.username in :usuarios order by t.id", Object[].class)
                    .setParameter("usuarios", contas.stream().map(UsuarioAplicacao::getUsername).toList()).getResultList();
            var trilha = em.createQuery("select a.id,a.action,a.entityType,a.entityId,a.detailsJson "
                            + "from RegistroAuditoria a where a.userId=:u and a.path=:p order by a.id", Object[].class)
                    .setParameter("u", atorId).setParameter("p", rota).getResultList();
            return mapper.valueToTree(mapa("superintendencias", superintendencias, "gestoes", gestoes,
                    "usuarios", usuarios, "sessoes", sessoes, "trilhaDoRecurso", trilha));
        });
    }



    private record Preparacao(String rota, String metodo, Map<String, Object> body,
                              String tabelaEsperada, long registroBloqueado, long controleId) { }
}
