package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.example.siggtrat.entidade.GestaoLocal;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.fasterxml.jackson.databind.JsonNode;
import com.example.siggtrat.infra.EsperaJdbcPostgres;
import com.example.siggtrat.infra.ObservadorAtorPostgres.Ator;
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
import java.time.DayOfWeek;
import java.time.Instant;
import java.util.ArrayList;
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
 * Autoria /root/revisao_final_contratos. C9-CON-H05/H06/H07/H08.
 * Base C9 bb2bd19e2015cfdab1ca6bea8d07e7d038b820e5d59f2216eb44cf9144c085b6.
 * Quatro criações adjacentes recebem controles e oráculos próprios; nenhum
 * resultado é herdado do caso Cargo. A barreira é a FK da Gestão existente,
 * com prova física PRIMARY/RECORD/ID e perda de perfil Local commitada.
 * Lotação exercita associação real de turno; Jornada cria sete dias e uma regra.
 * Motivo é catálogo contemporâneo sintético: não equivale a prova histórica.
 * Sem edição de produção, DDL, sleeps como prova ou execução Java pelo autor.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@TestPropertySource(properties = {"spring.cache.type=redis", "spring.sql.init.mode=never",
        "app.cache.redis.http-get.enabled=true"})
class Candidato9CriacoesCatalogosExecutorRevogadoRedIT extends ContratosHttpBase {
    private static final String BASE_SHA =
            "bb2bd19e2015cfdab1ca6bea8d07e7d038b820e5d59f2216eb44cf9144c085b6";
    @Autowired private DataSource dataSource;

    @ParameterizedTest(name = "catalogo={0}")
    @ValueSource(strings = {"TIPO", "LOTACAO", "JORNADA", "MOTIVO"})
    @Timeout(value = 75, unit = TimeUnit.SECONDS)
    void criacaoDeCatalogoExigeExecutorCorrenteDepoisDaEsperaNaGestao(String fluxo) throws Exception {
        try (var conexao = dataSource.getConnection()) {
            assertThat(conexao.getMetaData().getDatabaseProductName()).isEqualTo("PostgreSQL");
        }
        Plano p = preparar(criarCenarioPersistido(), fluxo);
        Cenario c = p.cenario();
        JsonNode antes = estado(p);
        assertThat(antes.path("registros").size()).isEqualTo(1);
        assertThat(antes.path("idsTentativa").size()).isZero();
        assertThat(antes.path("eventos").size()).as("Origem contemporânea real do controle").isEqualTo(1);
        if (fluxo.equals("LOTACAO")) assertThat(antes.path("turnos").size()).isEqualTo(1);
        if (fluxo.equals("JORNADA")) {
            assertThat(antes.path("dias").size()).isEqualTo(7);
            assertThat(antes.path("regras").size()).isEqualTo(1);
        }
        assertThat(c.localA.getPerfil()).isEqualTo(PerfilAcesso.GESTOR_LOCAL);
        var pedido = autenticar(post(p.rota()).contentType(APPLICATION_JSON).content(json(p.corpo())), c.localA);
        var pool = Executors.newFixedThreadPool(3);
        CountDownLatch liberar = new CountDownLatch(1);
        CompletableFuture<Long> conexaoBloqueadora = new CompletableFuture<>();
        var atorJdbc = new Ator("Candidato9CriacoesCatalogosExecutorRevogadoRedIT-" + fluxo);
        try {
            Future<?> bloqueador = pool.submit(() -> emTransacao(() -> {
                // FOR UPDATE conflita também com KEY SHARE da FK; NO KEY UPDATE não conflita.
                assertThat(em.createNativeQuery("select id_gestao_local from gestao_local where id_gestao_local=:id for update")
                        .setParameter("id",c.gestaoA.getId()).getSingleResult()).isNotNull();
                conexaoBloqueadora.complete(((Number) em.createNativeQuery("select pg_backend_pid()")
                        .getSingleResult()).longValue());
                try {
                    if (!liberar.await(40, TimeUnit.SECONDS))
                        throw new AssertionError("Limite da fixture: Gestão não liberada");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt(); throw new IllegalStateException(ex);
                }
                return null;
            }));
            long conexao = conexaoBloqueadora.get(10, TimeUnit.SECONDS);
            Future<MvcResult> tentativa = pool.submit(() -> atorJdbc.executar(() -> mvc.perform(pedido).andReturn()));
            List<Map<String, Object>> espera = EsperaJdbcPostgres.aguardar(new JdbcTemplate(dataSource),conexao,atorJdbc,
                    sql -> sql.selecaoChave("gestao_local","id_gestao_local",c.gestaoA.getId())
                            || sql.insercao(switch(fluxo) {
                                case "TIPO" -> "tipo_contratacao";
                                case "LOTACAO" -> "lotacao";
                                case "JORNADA" -> "jornada";
                                case "MOTIVO" -> "motivo_desligamento";
                                default -> throw new AssertionError(fluxo);
                            },"id_gestao_local",c.gestaoA.getId()),
                    "gestao_local/"+c.gestaoA.getId(),tentativa);
            Instant esperaObservadaEm = Instant.now();
            Future<?> revogacao = pool.submit(() -> {
                executar(c.admin, put("/api/usuarios/" + c.localA.getId()).contentType(APPLICATION_JSON)
                                .content(json(mapa("perfil", "GESTOR_NACIONAL"))), 200,
                        "Outro Admin retira a competência local durante a espera na FK da Gestão");
                return null;
            });
            revogacao.get(12, TimeUnit.SECONDS);
            JsonNode executorAposRevogacao = emTransacao(() -> {
                em.clear();
                UsuarioAplicacao atual = em.find(UsuarioAplicacao.class, c.localA.getId());
                assertThat(atual.getPerfil()).isEqualTo(PerfilAcesso.GESTOR_NACIONAL);
                assertThat(atual.getActive()).isTrue();
                assertThat(atual.getGestaoLocal()).isNull();
                return mapper.valueToTree(mapa("id", atual.getId(), "perfil", atual.getPerfil(),
                        "active", atual.getActive(), "gestaoLocalId", null, "version", atual.getVersion()));
            });
            Instant revogacaoConferidaEm = Instant.now();
            assertThat(tentativa.isDone()).as("Criação ainda bloqueada depois da revogação commitada").isFalse();
            Instant liberacaoEm = Instant.now();
            liberar.countDown();
            bloqueador.get(10, TimeUnit.SECONDS);
            MvcResult resultado = tentativa.get(15, TimeUnit.SECONDS);
            JsonNode depois = estado(p);
            Path arquivo = Path.of("target/auditoria-http/candidato9-criacao-catalogo-executor-" + fluxo + ".json");
            Files.createDirectories(arquivo.getParent());
            Files.writeString(arquivo, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapa(
                    "autor", "/root/revisao_final_contratos", "hipotese", p.hipotese(), "fluxo", fluxo,
                    "base_source_manifest_sha256", BASE_SHA,
                    "audit_round", System.getenv("SGT_AUDITORIA_RODADA"), "recorded_at", Instant.now().toString(),
                    "metodo", "POST", "rota", p.rota(), "controlePositivoStatus", 201,
                    "executorId", c.localA.getId(), "revogadorId", c.admin.getId(),
                    "tabelaBloqueada", "gestao_local", "registroBloqueado", c.gestaoA.getId(),
                    "esperaPostgresObservada", true, "esperaFisica", espera,
                    "esperaObservadaEm", esperaObservadaEm, "revogacaoConferidaEm", revogacaoConferidaEm,
                    "liberacaoEm", liberacaoEm, "executorAposRevogacao", executorAposRevogacao,
                    "revogacaoCommitadaAntesDaLiberacao", true,
                    "status", resultado.getResponse().getStatus(), "antes", antes, "depois", depois)));
            assertAll("Criação do catálogo exige competência corrente: " + fluxo,
                    () -> assertThat(resultado.getResponse().getStatus()).isIn(401, 403, 404, 409),
                    () -> assertThat(depois.path("idsTentativa").size()).isZero(),
                    () -> assertThat(depois).as("Item, dependências, eventos e Gestão capturados permanecem iguais")
                            .isEqualTo(antes));
        } finally {
            liberar.countDown(); pool.shutdownNow(); pool.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private Plano preparar(Cenario c, String fluxo) throws Exception {
        String sufixo = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
        String controle = "C9 " + fluxo + " CONTROLE " + sufixo;
        String tentativa = "C9 " + fluxo + " TENTATIVA " + sufixo;
        String rota = switch (fluxo) {
            case "TIPO" -> "/api/tipos-contratacao";
            case "LOTACAO" -> "/api/lotacoes";
            case "JORNADA" -> "/api/jornadas";
            case "MOTIVO" -> "/api/motivos-desligamento";
            default -> throw new IllegalArgumentException(fluxo);
        };
        String hipotese = switch (fluxo) {
            case "TIPO" -> "C9-CON-H05";
            case "LOTACAO" -> "C9-CON-H06";
            case "JORNADA" -> "C9-CON-H07";
            case "MOTIVO" -> "C9-CON-H08";
            default -> throw new IllegalArgumentException(fluxo);
        };
        JsonNode confirmado = enviar(c.localA, rota, corpo(c, fluxo, controle), 201,
                "Controle positivo explícito de criação válida " + fluxo + " na Gestão responsável");
        assertThat(confirmado.path("id").asLong()).isPositive();
        assertThat(confirmado.path(fluxo.equals("LOTACAO") ? "nome" : "descricao").asText()).isEqualTo(controle);
        if (fluxo.equals("TIPO")) assertThat(confirmado.path("matriculaVinculoObrigatoria").asBoolean()).isTrue();
        if (fluxo.equals("MOTIVO")) assertThat(confirmado.path("categoria").asText()).isEqualTo("OUTRO");
        return new Plano(hipotese, c, fluxo, rota, controle, tentativa, corpo(c, fluxo, tentativa));
    }

    private Map<String, Object> corpo(Cenario c, String fluxo, String nome) {
        return switch (fluxo) {
            case "TIPO" -> mapa("descricao", nome, "ativo", true, "matriculaVinculoObrigatoria", true,
                    "gestaoLocalId", c.gestaoA.getId());
            case "LOTACAO" -> mapa("nome", nome, "ativo", true, "gestaoLocalId", c.gestaoA.getId(),
                    "municipio", "MUNICIPIO SINTETICO C9", "turnos", List.of(mapa("id", c.jornadaA.getId())));
            case "JORNADA" -> mapa("descricao", nome, "ativo", true, "gestaoLocalId", c.gestaoA.getId(),
                    "dias", diasValidos(), "regrasAdicionais", List.of(mapa("tipo", "EXTRA", "inicio", "08:00",
                            "fim", "09:00", "percentual", 50, "ativo", true, "ordem", 1)));
            case "MOTIVO" -> mapa("descricao", nome, "categoria", "OUTRO", "ativo", true,
                    "gestaoLocalId", c.gestaoA.getId());
            default -> throw new IllegalArgumentException(fluxo);
        };
    }

    private List<Map<String, Object>> diasValidos() {
        List<Map<String, Object>> dias = new ArrayList<>();
        for (DayOfWeek dia : DayOfWeek.values()) {
            boolean trabalho = dia.getValue() <= 5;
            dias.add(mapa("diaSemana", dia.name(), "diaTrabalho", trabalho,
                    "entrada1", trabalho ? "08:00" : null, "saida1", trabalho ? "12:00" : null,
                    "entrada2", trabalho ? "13:00" : null, "saida2", trabalho ? "17:00" : null));
        }
        return dias;
    }

    private JsonNode estado(Plano p) {
        return emTransacao(() -> {
            em.clear();
            return switch (p.fluxo()) {
                case "TIPO" -> estadoTipo(p);
                case "LOTACAO" -> estadoLotacao(p);
                case "JORNADA" -> estadoJornada(p);
                case "MOTIVO" -> estadoMotivo(p);
                default -> throw new IllegalArgumentException(p.fluxo());
            };
        });
    }

    private JsonNode estadoTipo(Plano p) {
        var registros = registros(p, "TipoContratacao", "descricao", "e.id,e.version,e.descricao,e.ativo,"
                + "e.gestaoLocal.id,e.matriculaVinculoObrigatoria,e.criadoEm,e.criadoPorId,e.criadoPorUsername,e.atualizadoEm");
        return mapper.valueToTree(resumo(p, "TipoContratacao", "descricao", TipoCatalogoTemporal.TIPO_CONTRATACAO, registros));
    }

    private JsonNode estadoLotacao(Plano p) {
        var registros = registros(p, "Lotacao", "nome", "e.id,e.version,e.nome,e.ativo,e.gestaoLocal.id,e.municipio");
        var dados = resumo(p, "Lotacao", "nome", TipoCatalogoTemporal.LOTACAO, registros);
        var turnos = em.createQuery("select l.id,j.id,j.version,j.descricao,j.ativo,j.gestaoLocal.id "
                        + "from Lotacao l join l.turnos j where l.gestaoLocal.id=:g and l.nome in :ns order by l.id,j.id", Object[].class)
                .setParameter("g", p.cenario().gestaoA.getId()).setParameter("ns", nomes(p)).getResultList();
        dados.put("turnos", turnos);
        return mapper.valueToTree(dados);
    }

    private JsonNode estadoJornada(Plano p) {
        var registros = registros(p, "Jornada", "descricao", "e.id,e.version,e.descricao,e.ativo,e.gestaoLocal.id");
        var dados = resumo(p, "Jornada", "descricao", TipoCatalogoTemporal.JORNADA, registros);
        var dias = em.createQuery("select d.id,d.jornada.id,d.diaSemana,d.diaTrabalho,d.entrada1,d.saida1,d.entrada2,d.saida2 "
                        + "from JornadaDiaSemana d where d.jornada.gestaoLocal.id=:g "
                        + "and d.jornada.descricao in :ns order by d.id", Object[].class)
                .setParameter("g", p.cenario().gestaoA.getId()).setParameter("ns", nomes(p)).getResultList();
        var regras = em.createQuery("select r.id,r.version,r.jornada.id,r.tipo,r.inicio,r.fim,r.percentual,r.ativo,r.ordem "
                        + "from RegraAdicional r where r.jornada.gestaoLocal.id=:g "
                        + "and r.jornada.descricao in :ns order by r.id", Object[].class)
                .setParameter("g", p.cenario().gestaoA.getId()).setParameter("ns", nomes(p)).getResultList();
        dados.put("dias", dias); dados.put("regras", regras);
        return mapper.valueToTree(dados);
    }

    private JsonNode estadoMotivo(Plano p) {
        var registros = registros(p, "MotivoDesligamento", "descricao", "e.id,e.version,e.descricao,e.categoria,e.ativo,"
                + "e.gestaoLocal.id,e.criadoEm,e.criadoPorId,e.criadoPorUsername,e.atualizadoEm");
        return mapper.valueToTree(resumo(p, "MotivoDesligamento", "descricao", TipoCatalogoTemporal.MOTIVO_DESLIGAMENTO, registros));
    }

    private List<Object[]> registros(Plano p, String entidade, String atributo, String campos) {
        // Nomes de entidade/atributo são constantes dos quatro métodos acima.
        return em.createQuery("select " + campos + " from " + entidade + " e where e.gestaoLocal.id=:g "
                        + "and e." + atributo + " in :ns order by e.id", Object[].class)
                .setParameter("g", p.cenario().gestaoA.getId()).setParameter("ns", nomes(p)).getResultList();
    }

    private Map<String, Object> resumo(Plano p, String entidade, String atributo, TipoCatalogoTemporal tipo,
                                       List<Object[]> registros) {
        var ids = em.createQuery("select e.id from " + entidade + " e where e.gestaoLocal.id=:g "
                        + "and e." + atributo + "=:n order by e.id", Long.class)
                .setParameter("g", p.cenario().gestaoA.getId()).setParameter("n", p.tentativa()).getResultList();
        var eventos = em.createQuery("select e.id,e.tipoCatalogo,e.catalogoId,e.dataEfetiva,e.registradoEm,"
                        + "e.ativoAnterior,e.ativo,e.responsavelId,e.responsavelUsername,e.motivo,e.chaveOrigem,"
                        + "e.evidencia,e.autorizacao,e.chaveIdempotencia,e.hashIntencao,e.ativoAtualAposRegistro,e.versaoCatalogoApos "
                        + "from EventoDisponibilidadeCatalogo e where e.tipoCatalogo=:t and e.catalogoId in "
                        + "(select x.id from " + entidade + " x where x.gestaoLocal.id=:g "
                        + "and x." + atributo + " in :ns) order by e.id", Object[].class)
                .setParameter("t", tipo).setParameter("g", p.cenario().gestaoA.getId())
                .setParameter("ns", nomes(p)).getResultList();
        GestaoLocal g = em.find(GestaoLocal.class, p.cenario().gestaoA.getId());
        return mapa("registros", registros, "idsTentativa", ids, "eventos", eventos,
                "gestao", mapa("id", g.getId(), "version", g.getVersion(), "nome", g.getNome(),
                        "codigo", g.getCodigo(), "ativo", g.getAtivo(), "superintendenciaId", g.getSuperintendencia().getId()));
    }

    private List<String> nomes(Plano p) { return List.of(p.controle(), p.tentativa()); }




    private record Plano(String hipotese, Cenario cenario, String fluxo, String rota,
                         String controle, String tentativa, Map<String, Object> corpo) {}
}
