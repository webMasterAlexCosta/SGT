package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.constante.ConstantesDominio;
import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.entidade.GestaoLocal;
import com.example.siggtrat.entidade.SuperintendenciaEstadual;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.fasterxml.jackson.databind.JsonNode;
import com.example.siggtrat.infra.EsperaJdbcPostgres;
import com.example.siggtrat.infra.ObservadorAtorPostgres.Ator;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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
 * Hipótese C10-CON-H01; autoria independente /root/revisao_final_contratos.
 * Fonte C10 e58cd276bad1395ce910dd9fc466c50031d1579f12e8a69747ccfadcdfb95b99.
 * V2: reserva SP do seed sintético e restaura UF/nome/ativo no finally.
 * A SE alvo é criada pela própria fixture; o seed não recebe filhos/contas do caso.
 * Não restaura artificialmente @Version nem elimina recursos de outros cenários.
 * Executar serialmente em base descartável. V1 dd40b6… permanece não executada.
 * Não testa revogação do executor: ambos os Administradores continuam válidos.
 * O PUT legítimo de inativação espera o UPDATE do gestor estadual da fixture.
 * Antes de iniciar o POST, exige prova física de que o PUT já retém X na SE.
 * O POST então aguarda essa SE: após a cascata/commit, não pode criar filho ativo.
 * Premissas de preparação são verificadas antes dos oráculos de produto.
 * Sem sleeps como prova; sem alteração de produção ou execução pelo autor.
 */
/**
 * Proposta permanente recuperada em 2026-10-31 por /root/revisao_autorizacao.
 * Origem preservada: red_estrutura_hierarquia_diagnostico/Candidato10GestaoCriadaDuranteInativacaoSeRedIT.java.
 * A execução RED/GREEN desta proposta cabe à coordenação central; não foi executada pelo autor.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@TestPropertySource(properties = {"spring.cache.type=redis", "spring.sql.init.mode=never",
        "app.cache.redis.http-get.enabled=true"})
class GestaoLocalInativacaoSuperintendenciaConcorrenteHttpIT extends ContratosHttpBase {
    private static final String UF = "SP";
    @Autowired private DataSource dataSource;

    @Test
    @Timeout(value = 90, unit = TimeUnit.SECONDS)
    void criacaoDeGestaoNaoAtravessaInativacaoCommitadaDaSuperintendencia() throws Exception {
        try (var connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("PostgreSQL");
        }
        Cenario c = criarCenarioPersistido();
        UsuarioAplicacao outroAdmin = emTransacao(() -> {
            var u = usuario(PerfilAcesso.ADMINISTRADOR, null); em.flush(); return u;
        });
        SeedReservado seed = reservarUfDoSeed();
        var pool = Executors.newFixedThreadPool(3);
        CountDownLatch liberarUsuario = new CountDownLatch(1);
        CompletableFuture<Long> conexaoBarreira = new CompletableFuture<>();
        var atorInativacao=new Ator("cascata-inativacao");var atorCriacao=new Ator("cascata-criacao");
        try {
            JsonNode seCriada = enviar(c.admin, "/api/superintendencias",
                    mapa("uf", UF, "ativo", true), 201,
                    "Fixture explícita: SE própria na UF oficial reservada temporariamente");
            long sePropriaId = seCriada.path("id").asLong();
            assertThat(sePropriaId).isPositive().isNotEqualTo(seed.id());
            SuperintendenciaEstadual se = emTransacao(() -> em.find(SuperintendenciaEstadual.class, sePropriaId));
            assertThat(se.getUf()).isEqualTo(UF);
            assertThat(se.getAtivo()).isTrue();
            String suffix = UUID.randomUUID().toString().substring(0, 8);
            String codigoControle = "C10-CONTROLE-" + suffix;
            String codigoTentativa = "C10-ESPERA-" + suffix;
            JsonNode controle = enviar(c.admin, "/api/gestoes-locais",
                    corpoGestao(se.getId(), codigoControle), 201,
                    "Controle positivo: Admin cria Gestão ativa sob SE oficial ativa");
            long controleId = controle.path("id").asLong();
            assertThat(controleId).isPositive();
            assertThat(controle.path("ativo").asBoolean()).isTrue();
            UsuarioAplicacao estadual = emTransacao(() -> {
                var g = em.find(GestaoLocal.class, controleId);
                var u = usuario(PerfilAcesso.GESTOR_ESTADUAL, g);
                u.setGestaoLocal(null);
                em.flush(); return u;
            });
            JsonNode antes = fotografia(se.getId(), codigoTentativa, c.admin.getId(), outroAdmin.getId());
            assertThat(antes.path("se").path("ativo").asBoolean()).isTrue();
            assertThat(antes.path("idsTentativa").size()).isZero();
            assertThat(antes.path("gestoes").size()).isEqualTo(1);
            assertThat(antes.path("contasDaSe").size()).as("Uma única conta estadual na fixture isolada").isEqualTo(1);
            assertThat(antes.path("contasDaSe").get(0).get(0).asLong()).isEqualTo(estadual.getId());

            var pedidoInativacao = autenticar(put("/api/superintendencias/" + se.getId())
                    .contentType(APPLICATION_JSON).content(json(mapa("uf", se.getUf(), "ativo", false))), c.admin);
            var pedidoCriacao = autenticar(post("/api/gestoes-locais")
                    .contentType(APPLICATION_JSON).content(json(corpoGestao(se.getId(), codigoTentativa))), outroAdmin);
            Future<?> barreira = pool.submit(() -> emTransacao(() -> {
                assertThat(em.find(UsuarioAplicacao.class, estadual.getId(), LockModeType.PESSIMISTIC_WRITE))
                        .as("Conta existente da própria fixture, sem modificar seus campos").isNotNull();
                conexaoBarreira.complete(((Number) em.createNativeQuery("select pg_backend_pid()")
                        .getSingleResult()).longValue());
                try {
                    if (!liberarUsuario.await(45, TimeUnit.SECONDS))
                        throw new AssertionError("Fixture não liberou a conta da cascata");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt(); throw new IllegalStateException(ex);
                }
                return null;
            }));
            long conexao = conexaoBarreira.get(10, TimeUnit.SECONDS);
            Future<MvcResult> inativacao = pool.submit(() -> atorInativacao.executar(() -> mvc.perform(pedidoInativacao).andReturn()));
            List<Map<String, Object>> esperaCascata = EsperaJdbcPostgres.aguardar(new JdbcTemplate(dataSource),conexao,atorInativacao,
                    sql->sql.selecaoId("usuario_aplicacao",estadual.getId())
                            ||sql.alteracaoNaChave("usuario_aplicacao","id",estadual.getId()),
                    "usuario_aplicacao/"+estadual.getId(),inativacao);
            long conexaoInativacao = ((Number) esperaCascata.get(0).get("solicitante")).longValue();
            Future<MvcResult> criacao = pool.submit(() -> atorCriacao.executar(() -> mvc.perform(pedidoCriacao).andReturn()));
            List<Map<String, Object>> esperaCriacao = EsperaJdbcPostgres.aguardar(new JdbcTemplate(dataSource),conexaoInativacao,atorCriacao,
                    sql->sql.selecaoChave("superintendencia_estadual","id_superintendencia",se.getId())
                            ||sql.insercao("gestao_local","id_superintendencia",se.getId()),
                    "superintendencia_estadual/"+se.getId(),criacao);
            Instant ambasEsperasEm = Instant.now();
            assertThat(inativacao.isDone()).isFalse();
            assertThat(criacao.isDone()).isFalse();
            liberarUsuario.countDown();
            barreira.get(10, TimeUnit.SECONDS);
            MvcResult respostaInativacao = inativacao.get(20, TimeUnit.SECONDS);
            assertThat(respostaInativacao.getResponse().getStatus())
                    .as("Premissa: inativação HTTP legítima conclui e commita").isEqualTo(200);
            Instant inativacaoConcluidaEm = Instant.now();
            MvcResult respostaCriacao = criacao.get(15, TimeUnit.SECONDS);
            JsonNode depois = fotografia(se.getId(), codigoTentativa, c.admin.getId(), outroAdmin.getId());
            assertThat(depois.path("se").path("ativo").asBoolean())
                    .as("Inativação commitada e relida em transação independente").isFalse();
            assertThat(depois.path("contasDaSe").get(0).get(2).asBoolean())
                    .as("A cascata atingiu a conta que sustentou a barreira").isFalse();
            assertThat(depois.path("administradores")).as("Nenhum executor foi revogado ou rebaixado")
                    .isEqualTo(antes.path("administradores"));

            // Controle negativo sequencial distinto confirma a regra existente
            // de conflito409; não se trata de inventar política de criação.
            executar(outroAdmin, post("/api/gestoes-locais").contentType(APPLICATION_JSON)
                            .content(json(corpoGestao(se.getId(), "C10-NEGATIVO-" + suffix))), 409,
                    "Controle negativo: Gestão ativa não é criada sob SE já inativa");
            Path evidence = Path.of("target/auditoria-http/candidato10-gestao-inativacao-se.json");
            Files.createDirectories(evidence.getParent());
            Files.writeString(evidence, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapa(
                    "autor", "/root/revisao_final_contratos", "hipotese", "C10-CON-H01",
                    "source_manifest_origem_sha256", "e58cd276bad1395ce910dd9fc466c50031d1579f12e8a69747ccfadcdfb95b99",
                    "audit_round", System.getenv("SGT_AUDITORIA_RODADA"), "recorded_at", Instant.now().toString(),
                    "controlePositivoStatus", 201, "controleNegativoStatus", 409,
                    "seId", se.getId(), "ufOficial", se.getUf(), "usuarioBarreiraId", estadual.getId(),
                    "seedId", seed.id(), "ufTemporariaSeed", seed.ufTemporaria(), "versaoProposta", "v2",
                    "executorInativacaoId", c.admin.getId(), "executorCriacaoId", outroAdmin.getId(),
                    "codigoTentativa", codigoTentativa, "esperaCascata", esperaCascata,
                    "conflitoFisicoSeDuranteCriacao", esperaCriacao, "esperaCriacao", esperaCriacao,
                    "ambasEsperasObservadasEm", ambasEsperasEm.toString(),
                    "inativacaoConcluidaEm", inativacaoConcluidaEm.toString(),
                    "statusInativacao", respostaInativacao.getResponse().getStatus(),
                    "statusCriacao", respostaCriacao.getResponse().getStatus(),
                    "respostaCriacao", mapper.readTree(respostaCriacao.getResponse().getContentAsString()),
                    "antes", antes, "depois", depois,
                    "limite", "Estado fotografado após commits, antes de recolocar a SE própria em UF sintética e restaurar o seed; sem revogar os Administradores.")));

            assertAll("Criação revalida a atividade da SE depois da espera da FK",
                    () -> assertThat(respostaCriacao.getResponse().getStatus()).isEqualTo(409),
                    () -> assertThat(depois.path("idsTentativa").size())
                            .as("Nenhuma Gestão da tentativa pode persistir").isZero(),
                    () -> assertThat(depois.path("gestoesAtivas").asLong())
                            .as("Não pode existir Gestão ativa sob a SE inativa commitada").isZero());
        } finally {
            liberarUsuario.countDown();
            pool.shutdownNow();
            if (!pool.awaitTermination(10, TimeUnit.SECONDS))
                throw new AssertionError("Fixture não pode restaurar a UF enquanto houver comandos concorrentes em execução");
            restaurarUfDoSeed(seed);
        }
    }

    private SeedReservado reservarUfDoSeed() {
        return emTransacao(() -> {
            em.clear();
            List<SuperintendenciaEstadual> todas = em.createQuery(
                    "select s from SuperintendenciaEstadual s order by s.id", SuperintendenciaEstadual.class).getResultList();
            List<SuperintendenciaEstadual> oficiais = todas.stream().filter(s -> UF.equals(s.getUf())).toList();
            assertThat(oficiais).as("Seed sintético com exatamente uma SE/SP").hasSize(1);
            SuperintendenciaEstadual original = oficiais.get(0);
            String livre = ufSinteticaParaReserva();
            SeedReservado r = new SeedReservado(original.getId(), original.getNome(), original.getUf(),
                    original.getAtivo(), original.getVersion(), livre,
                    todas.stream().map(SuperintendenciaEstadual::getId).toList(), dependentesSeed(original.getId()));
            original.setUf(livre); em.flush();
            assertThat(original.getVersion()).isGreaterThan(r.versionAntes());
            assertThat(dependentesSeed(original.getId())).isEqualTo(r.dependentesAntes());
            return r;
        });
    }

    private String ufSinteticaParaReserva() {
        Set<String> usadas = new HashSet<>();
        em.createQuery("select s.uf from SuperintendenciaEstadual s", String.class).getResultList()
                .forEach(s -> usadas.add(s.toUpperCase(Locale.ROOT)));
        usadas.addAll(ConstantesDominio.UFS_BRASIL); usadas.add("YY");
        // Mesmo namespace ASCII cuja capacidade/colação já possui ensaio próprio.
        String alfabeto = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ!#$%&()+,-.;=@[]^_{}~";
        for (int a = 0; a < alfabeto.length(); a++) {
            for (int b = 0; b < alfabeto.length(); b++) {
                String candidata = "" + alfabeto.charAt(a) + alfabeto.charAt(b);
                if (!usadas.contains(candidata)) return candidata;
            }
        }
        throw new AssertionError("Fixture não encontrou UF sintética livre na base descartável");
    }

    private JsonNode dependentesSeed(long id) {
        var gestoes = em.createQuery("select g.id,g.nome,g.codigo,g.ativo,g.version,g.superintendencia.id "
                        + "from GestaoLocal g where g.superintendencia.id=:s order by g.id", Object[].class)
                .setParameter("s", id).getResultList();
        var contas = em.createQuery("select u.id,u.username,u.active,u.perfil,u.version,u.superintendencia.id "
                        + "from UsuarioAplicacao u where u.superintendencia.id=:s order by u.id", Object[].class)
                .setParameter("s", id).getResultList();
        return mapper.valueToTree(mapa("gestoes", gestoes, "contas", contas));
    }

    private void restaurarUfDoSeed(SeedReservado seed) throws Exception {
        JsonNode recibo = emTransacao(() -> {
            em.clear();
            List<SuperintendenciaEstadual> criadas = em.createQuery(
                    "select s from SuperintendenciaEstadual s where s.uf=:uf", SuperintendenciaEstadual.class)
                    .setParameter("uf", UF).getResultList();
            var remapeadas = new java.util.ArrayList<Map<String, Object>>();
            for (SuperintendenciaEstadual criada : criadas) {
                assertThat(seed.idsOriginais()).as("Só a SE criada pela própria fixture pode liberar SP")
                        .doesNotContain(criada.getId());
                String sintetica = ufSinteticaParaReserva();
                criada.setUf(sintetica); criada.setNome("SE sintética C10 " + criada.getId());
                em.flush();
                remapeadas.add(mapa("id", criada.getId(), "ufFinalSintetica", criada.getUf(),
                        "ativoPreservado", criada.getAtivo(), "version", criada.getVersion()));
            }
            SuperintendenciaEstadual original = em.find(
                    SuperintendenciaEstadual.class, seed.id(), LockModeType.PESSIMISTIC_WRITE);
            assertThat(original.getUf()).isEqualTo(seed.ufTemporaria());
            assertThat(dependentesSeed(original.getId())).as("Filhos/contas do seed não participam da corrida")
                    .isEqualTo(seed.dependentesAntes());
            long versaoDurante = original.getVersion();
            original.setUf(seed.ufOriginal()); original.setNome(seed.nomeOriginal()); original.setAtivo(seed.ativoOriginal());
            em.flush();
            assertThat(original.getUf()).isEqualTo(seed.ufOriginal());
            assertThat(original.getNome()).isEqualTo(seed.nomeOriginal());
            assertThat(original.getAtivo()).isEqualTo(seed.ativoOriginal());
            assertThat(original.getVersion()).isGreaterThan(versaoDurante);
            assertThat(dependentesSeed(original.getId())).isEqualTo(seed.dependentesAntes());
            return mapper.valueToTree(mapa("audit_round", System.getenv("SGT_AUDITORIA_RODADA"),
                    "recorded_at", Instant.now().toString(), "hipotese", "C10-CON-H01", "versaoProposta", "v2",
                    "seedId", seed.id(), "ufOriginal", seed.ufOriginal(), "ufTemporaria", seed.ufTemporaria(),
                    "ufRestaurada", original.getUf(), "nomeRestaurado", original.getNome(),
                    "ativoRestaurado", original.getAtivo(), "versionAntes", seed.versionAntes(),
                    "versionDurante", versaoDurante, "versionDepois", original.getVersion(),
                    "reposicaoArtificialDeVersion", false, "seedRemovido", false,
                    "dependentesSeedAntes", seed.dependentesAntes(), "dependentesSeedDepois", dependentesSeed(seed.id()),
                    "sePropriaRemapeadaAposFotografia", remapeadas));
        });
        Path path = Path.of("target/auditoria-http/candidato10-gestao-inativacao-se-custodia-seed.json");
        Files.createDirectories(path.getParent());
        Files.writeString(path, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(recibo));
    }

    private record SeedReservado(long id, String nomeOriginal, String ufOriginal, Boolean ativoOriginal,
                                 long versionAntes, String ufTemporaria, List<Long> idsOriginais,
                                 JsonNode dependentesAntes) { }

    private Map<String, Object> corpoGestao(long seId, String codigo) {
        return mapa("nome", "Gestão sintética " + codigo, "codigo", codigo,
                "ativo", true, "superintendenciaId", seId);
    }

    private JsonNode fotografia(long seId, String codigo, long admin1, long admin2) {
        return emTransacao(() -> {
            em.clear();
            var se = em.find(SuperintendenciaEstadual.class, seId);
            var gestoes = em.createQuery("select g.id,g.version,g.nome,g.codigo,g.ativo,g.superintendencia.id "
                            + "from GestaoLocal g where g.superintendencia.id=:s order by g.id", Object[].class)
                    .setParameter("s", seId).getResultList();
            var ids = em.createQuery("select g.id from GestaoLocal g where g.superintendencia.id=:s "
                            + "and g.codigo=:c order by g.id", Long.class)
                    .setParameter("s", seId).setParameter("c", codigo).getResultList();
            var ativos = em.createQuery("select count(g) from GestaoLocal g "
                            + "where g.superintendencia.id=:s and g.ativo=true", Long.class)
                    .setParameter("s", seId).getSingleResult();
            var contas = em.createQuery("select u.id,u.version,u.active,u.perfil,u.superintendencia.id "
                            + "from UsuarioAplicacao u where u.superintendencia.id=:s order by u.id", Object[].class)
                    .setParameter("s", seId).getResultList();
            var admins = em.createQuery("select u.id,u.version,u.active,u.perfil from UsuarioAplicacao u "
                            + "where u.id in :ids order by u.id", Object[].class)
                    .setParameter("ids", List.of(admin1, admin2)).getResultList();
            return mapper.valueToTree(mapa("se", mapa("id", se.getId(), "uf", se.getUf(), "ativo", se.getAtivo(),
                            "version", se.getVersion()), "gestoes", gestoes, "idsTentativa", ids,
                    "gestoesAtivas", ativos, "contasDaSe", contas, "administradores", admins));
        });
    }




}
