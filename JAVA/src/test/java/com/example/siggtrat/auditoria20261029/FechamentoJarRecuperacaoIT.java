package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.*;
import com.example.siggtrat.infra.ConfiguracaoPostgresIntegracao;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Opt-in: PostgreSQL/Redis isolados explícitos; HTTP/Tomcat do JAR corrente.
 * Cinco critérios e uma preparação explicitamente opt-in; preparação não é aceite.
 * O contexto herdado só prepara JPA/observa SELECTs. Os JARs não têm mocks,
 * agentes, hooks, scheduler substituído ou acesso ao dotenv operacional.
 * Não comprova UI/LAN, produção nem carga nacional.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class FechamentoJarRecuperacaoIT extends ContratosHttpBase {
    private static final int QUANTIDADE = 200;
    private static final Duration PRAZO = Duration.ofSeconds(120);
    private static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");
    private static final Path PREPARO = diretorioPrivado("preparo-");
    private static final String SCHEMA_ISOLADO = System.getenv("SGT_INTEGRATION_SCHEMA");
    private static final String API = "/api/frequencias/fechamentos";
    @Autowired private JdbcTemplate jdbc;
    private final List<Filho> filhos = new ArrayList<>();
    private Path evidencias;
    private Path jar;
    private String hashJar;
    private long recuperacaoId;
    private long recuperacaoInicio;

    @DynamicPropertySource
    static void armazenamentoPreparador(DynamicPropertyRegistry propriedades) {
        propriedades.add("file.upload-dir", () -> PREPARO.resolve("uploads").toString());
        propriedades.add("audit.fallback-log", () -> PREPARO.resolve("auditoria-fallback.jsonl").toString());
        propriedades.add("app.relatorios.temp.diretorio", () -> PREPARO.resolve("relatorios").toString());
        if (SCHEMA_ISOLADO != null) {
            if (!SCHEMA_ISOLADO.matches("sgt_auditoria_[a-z0-9_]+"))
                throw new IllegalArgumentException("Schema explícito deve identificar o laboratório de auditoria");
            URI banco = URI.create(ConfiguracaoPostgresIntegracao.carregar().url().substring("jdbc:".length()));
            assertEquals("127.0.0.1", banco.getHost());
            assertNotNull(banco.getQuery(), "URL deve selecionar explicitamente o schema isolado");
            assertTrue(Arrays.asList(banco.getQuery().split("&")).contains("currentSchema=" + SCHEMA_ISOLADO));
            propriedades.add("spring.jpa.properties.hibernate.default_schema", () -> SCHEMA_ISOLADO);
            propriedades.add("spring.jpa.properties.hibernate.hbm2ddl.create_namespaces", () -> "true");
        }
    }

    @Test
    @Timeout(value = 4, unit = TimeUnit.MINUTES)
    void controleDuzentosChunkUmConcluiEmAteCentoEVinteSegundos() throws Exception {
        controle(1);
    }

    @Test
    @Timeout(value = 4, unit = TimeUnit.MINUTES)
    void controleDuzentosChunkQuinhentosConcluiEmAteCentoEVinteSegundos() throws Exception {
        controle(500);
    }

    private void controle(int chunk) throws Exception {
        Massa massa = preparar("controle-chunk-" + chunk);
        long inicio = 0;
        long id = 0;
        try {
            Filho filho = iniciar("controle", chunk);
            try (Sessao sessao = login(filho, massa.c().localA)) {
                inicio = System.nanoTime();
                JsonNode criado = sessao.enviar("POST", API, comando(massa), 202);
                id = criado.path("id").asLong();
                assertTrue(id > 0);
                JsonNode terminal = aguardarTerminal(sessao, id, inicio + PRAZO.toNanos());
                long duracaoAteTerminalMs = decorridoMs(inicio);
                assertEquals("CONCLUIDO", terminal.path("status").asText());
                assertEquals(QUANTIDADE, terminal.path("totalPrevisto").asLong());
                assertEquals(QUANTIDADE, terminal.path("totalProcessado").asLong());
                assertEquals(QUANTIDADE, terminal.path("totalInserido").asLong());
                assertEquals(0, terminal.path("totalAtualizado").asLong());
                assertEquals(0, terminal.path("totalPreservado").asLong());
                assertEquals(100, terminal.path("percentual").asInt());
                Fotografia confirmada = fotografar(id, massa);
                conferirDuzentos(massa, confirmada);
                conferirContadores(confirmada, QUANTIDADE, QUANTIDADE);
                gravar("controle-terminal.json", mapa("chunk", chunk, "quantidade", QUANTIDADE,
                        "prazoSegundos", 120, "duracaoPostAteTerminalMs", duracaoAteTerminalMs,
                        "terminalHttp", terminal, "confirmado", confirmada));
                JsonNode repetido = sessao.enviar("POST", API, comando(massa), 202);
                assertEquals(id, repetido.path("id").asLong());
                assertEquals(1, repetido.path("tentativa").asInt());
                assertEquals(confirmada, fotografar(id, massa), "Segundo clique terminal não altera o job ou suas provas");
            }
        } finally {
            finalizar(massa, id, inicio);
        }
    }

    @Test
    @Timeout(value = 7, unit = TimeUnit.MINUTES)
    void cancelamentoComPrefixoReprocessaERecuperaMesmoJobEmDoisExecutores() throws Exception {
        Massa massa = preparar("cancelamento-reinicio-dois-executores");
        try {
            Preparacao preparacao = prepararCancelamentoEQueda(massa);
            long id = preparacao.id();
            long inicioReprocessamento = preparacao.inicioReprocessamento();
            Fotografia cancelado = preparacao.cancelado();
            Fotografia antesDaMorte = preparacao.antesDaMorte();
            // Após o EOF/rollback de A, uma nova transação retém somente o job conhecido.
            try (Barreira barreira = new Barreira(FechamentoFrequencia.class, id)) {
                Filho executorB = iniciar("recuperador-b", 1);
                Filho executorC = iniciar("recuperador-c", 1);
                assertTrue(executorB.processo().isAlive() && executorC.processo().isAlive());
                List<Map<String, String>> disputa = aguardarEspera(barreira.pid,
                        Set.of(executorB.aplicacao(), executorC.aplicacao()), "fechamento_frequencia");
                gravar("dois-executores-disputando-o-job-conhecido.json", mapa("jobId", id,
                        "tentativa", 2, "pidBloqueador", barreira.pid, "disputaFisica", disputa));
                // Fechar a barreira libera os dois executores reais. Nenhum POST os reenfileira.
            }
            Filho executorB = filhos.get(filhos.size() - 2);
            Filho executorC = filhos.get(filhos.size() - 1);
            long inicioRetomada = System.nanoTime();
            try (Sessao observadorB = login(executorB, massa.c().localA);
                 Sessao observadorC = new Sessao(executorC.origem(), observadorB.cookies)) {
                JsonNode terminal = aguardarTerminal(observadorB, id, inicioRetomada + PRAZO.toNanos());
                long retomadaAteTerminalMs = decorridoMs(inicioRetomada);
                assertTrue(executorB.processo().isAlive() && executorC.processo().isAlive(),
                        "O gate exige dois executores saudáveis, mesmo se um deles já terminou o job");
                assertEquals("CONCLUIDO", terminal.path("status").asText());
                assertEquals(2, terminal.path("tentativa").asInt());
                assertEquals(id, observadorC.enviar("GET", API + "/" + id, null, 200).path("id").asLong());
                Fotografia concluido = fotografar(id, massa);
                preservar(antesDaMorte, concluido);
                conferirDuzentos(massa, concluido);
                // FechamentoFrequenciaServico.reprocessar zera contadores/cursor e conta apenas faltantes.
                int faltantesNaTentativa = QUANTIDADE - cancelado.frequencias().size();
                conferirContadores(concluido, faltantesNaTentativa, faltantesNaTentativa);
                assertEquals(QUANTIDADE, cancelado.frequencias().size()
                        + Long.parseLong(concluido.job().get("total_processado")));
                assertEquals("2", concluido.job().get("tentativa"));
                assertNull(concluido.job().get("detalhe_erro"));
                gravar("recuperacao-terminal.json", mapa("terminal", concluido,
                        "prefixoCancelado", cancelado.frequencias().size(), "faltantesTentativa2", faltantesNaTentativa,
                        "prazoPorControleSegundos", 120, "retomadaAutomaticaAteTerminalMs", retomadaAteTerminalMs,
                        "reprocessamentoIncluindoBootsEBarreirasMs", decorridoMs(inicioReprocessamento),
                        "postAdicionalParaRetomar", false));

                // Uma nova janela do recuperador, com ambos vivos, não pode reconcluir ou duplicar efeitos.
                long versaoTerminal = Long.parseLong(concluido.job().get("version"));
                long observarAte = System.nanoTime() + Duration.ofSeconds(3).toNanos();
                do {
                    JsonNode visto = observadorC.enviar("GET", API + "/" + id, null, 200);
                    assertEquals("CONCLUIDO", visto.path("status").asText());
                    Thread.sleep(150);
                } while (System.nanoTime() < observarAte);
                Fotografia estavel = fotografar(id, massa);
                assertTrue(executorB.processo().isAlive() && executorC.processo().isAlive(),
                        "Corrida de startup não pode ser aprovada com apenas uma JVM sobrevivente");
                assertEquals(versaoTerminal, Long.parseLong(estavel.job().get("version")));
                assertEquals(concluido, estavel, "Dois schedulers ativos não regravam o terminal ou seu prefixo");
                gravar("terminal-estavel-dois-executores.json", estavel);
            }
        } finally {
            finalizar(massa, recuperacaoId, recuperacaoInicio);
        }
    }

    @Test
    @org.junit.jupiter.api.Tag("fixture-preparation")
    @org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "SGT_PREPARE_RECOVERY_HANDOFF", matches = "true")
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    void prepararFixtureOptInParaRecuperacaoEmSegundaFase() throws Exception {
        Massa massa = preparar("preparacao-explicita-segunda-fase");
        try {
            Preparacao preparacao = prepararCancelamentoEQueda(massa);
            escreverHandoff(massa, preparacao);
        } finally {
            finalizar(massa, recuperacaoId, recuperacaoInicio);
        }
    }

    private record Preparacao(long id, long inicioReprocessamento, Fotografia cancelado,
                              Fotografia antesDaMorte, Fotografia aposMorte,
                              String canonicoAntesDaMorte, String canonicoAposMorte) {}

    private Preparacao prepararCancelamentoEQueda(Massa massa) throws Exception {
        assertTrue(org.mockito.Mockito.mockingDetails(taskScheduler).isMock(),
                "O preparador deve conservar o TaskScheduler mock herdado; somente os JARs podem recuperar o job");
        gravar("preparador-sem-scheduler-ativo.json", mapa("taskSchedulerMockHerdadoConfirmado", true,
                "momento", "antesDaCriacaoDoJob"));
        Filho primeiro = iniciar("original", 1);
        try (Sessao sessao = login(primeiro, massa.c().localA)) {
            long inicio = System.nanoTime();
            recuperacaoInicio = inicio;
            long id = sessao.enviar("POST", API, comando(massa), 202).path("id").asLong();
            recuperacaoId = id;
            aguardarPrefixo(sessao, id, inicio + PRAZO.toNanos());
            Fotografia antesDoCancelamento;
            try (Barreira barreira = new Barreira(massa, null)) {
                aguardarEspera(barreira.pid, Set.of(primeiro.aplicacao()), "contrato");
                antesDoCancelamento = fotografar(id, massa);
                conferirPrefixoParcial(antesDoCancelamento, 1);
                assertEquals("PROCESSANDO", antesDoCancelamento.job().get("status"));
                gravar("prefixo-antes-cancelamento.json", antesDoCancelamento);
                JsonNode resposta = sessao.enviar("POST", API + "/" + id + "/cancelamento", null, 202);
                assertTrue(resposta.path("cancelamentoSolicitado").asBoolean());
                Fotografia pedidoConfirmado = fotografar(id, massa);
                assertEquals("1", pedidoConfirmado.job().get("cancelamento_solicitado"));
                assertEquals(antesDoCancelamento.frequencias(), pedidoConfirmado.frequencias());
                assertEquals(antesDoCancelamento.eventos(), pedidoConfirmado.eventos());
                gravar("cancelamento-confirmado-com-worker-bloqueado.json", pedidoConfirmado);
            }
            JsonNode canceladoHttp = aguardarTerminal(sessao, id, inicio + PRAZO.toNanos());
            assertEquals("CANCELADO", canceladoHttp.path("status").asText());
            Fotografia cancelado = fotografar(id, massa);
            conferirPrefixoParcial(cancelado, 1);
            assertEquals("CANCELADO", cancelado.job().get("status"));
            preservar(antesDoCancelamento, cancelado);
            conferirContadores(cancelado, cancelado.frequencias().size(), QUANTIDADE);
            gravar("cancelado-terminal.json", cancelado);

            long inicioReprocessamento = System.nanoTime();
            JsonNode reprocessamento = sessao.enviar("POST", API + "/" + id + "/reprocessamento", null, 202);
            assertEquals(id, reprocessamento.path("id").asLong());
            assertEquals(2, reprocessamento.path("tentativa").asInt());
            aguardarPrefixo(sessao, id, inicioReprocessamento + PRAZO.toNanos());
            Fotografia antesDaMorte;
            Fotografia aposMorte;
            String canonicoAntesDaMorte;
            String canonicoAposMorte;
            // Ordem real do worker: Contrato/Pessoa/Vínculo/Empregado, e job só ao confirmar o bloco.
            // Antes da morte, somente o Contrato pausa o próximo bloco.
            try (Barreira barreira = new Barreira(massa, null)) {
                aguardarEspera(barreira.pid, Set.of(primeiro.aplicacao()), "contrato");
                antesDaMorte = fotografar(id, massa);
                conferirPrefixoParcial(antesDaMorte, 2);
                assertEquals("PROCESSANDO", antesDaMorte.job().get("status"));
                assertTrue(antesDaMorte.frequencias().size() > cancelado.frequencias().size());
                preservar(cancelado, antesDaMorte);
                gravar("prefixo-confirmado-antes-da-morte.json", antesDaMorte);
                canonicoAntesDaMorte = snapshotCanonico(id, massa);
                primeiro.matar();
                // O SELECT de lock precisa retornar para o PostgreSQL perceber o socket já morto.
                barreira.close();
                aguardarAusencia(primeiro.aplicacao());
                aposMorte = fotografar(id, massa);
                assertEquals(antesDaMorte, aposMorte, "Morte da única JVM executora conserva todo o estado confirmado");
                gravar("apos-morte-e-rollback.json", aposMorte);
                canonicoAposMorte = snapshotCanonico(id, massa);
                assertEquals(canonicoAntesDaMorte, canonicoAposMorte, "Snapshot PostgreSQL canônico preservado após morte e rollback");
                gravar("antes-morte-canonico.json", mapper.readTree(canonicoAntesDaMorte));
                gravar("apos-morte-canonico.json", mapper.readTree(canonicoAposMorte));
            }
            return new Preparacao(id, inicioReprocessamento, cancelado, antesDaMorte, aposMorte,
                    canonicoAntesDaMorte, canonicoAposMorte);
        }
    }

    private String snapshotCanonico(long id, Massa massa) {
        return jdbc.queryForObject(RecuperacaoJarSegundaFase.SNAPSHOT_SQL, String.class,
                id, massa.data().toString(), massa.c().lotacaoA.getId(), massa.c().jornadaA.getId(),
                massa.c().contratoA.getId(), massa.c().localA.getId());
    }

    private void escreverHandoff(Massa massa, Preparacao preparacao) throws Exception {
        assertTrue(filhos.stream().noneMatch(f -> f.processo().isAlive()), "A preparação não pode deixar JVM filha ativa");
        assertEquals(1, filhos.size());
        Filho original = filhos.get(0);
        assertEquals(1, original.chunk());
        assertEquals(15, original.staleAfterSeconds());
        assertEquals(1000, original.recoveryDelayMs());
        assertEquals("2", preparacao.aposMorte().job().get("tentativa"));
        assertEquals("PROCESSANDO", preparacao.aposMorte().job().get("status"));
        assertEquals(hashJar, hashArquivo(jar));
        String identidadeBruta = jdbc.queryForObject(RecuperacaoJarSegundaFase.IDENTIDADE_SQL, String.class);
        JsonNode identidade = RecuperacaoJarSegundaFase.identidadeSemCredenciais(identidadeBruta);
        assertEquals(180006, identidade.path("versionNum").asInt());
        assertTrue(identidade.path("database").asText().startsWith("sgt_20261109_"));
        for (String flag : List.of("databaseOwnedByRole", "schemaOwnedByRole", "schemaUsage", "schemaCreate",
                "roleLimited", "noMemberships", "noAdministrativeMembership", "sameSessionRole"))
            assertTrue(identidade.path(flag).asBoolean(), "Identidade isolada não comprovada: " + flag);
        Map<String, String> classes = new TreeMap<>();
        Path pacote = Path.of("target/test-classes/com/example/siggtrat/auditoria20261029");
        try (var arquivos = Files.list(pacote)) {
            for (Path arquivo : arquivos.filter(p -> p.getFileName().toString()
                    .matches("RecuperacaoJarSegundaFase(?:\\$[A-Za-z0-9]+)?\\.class")).toList())
                classes.put(arquivo.getFileName().toString(), hash(Files.readAllBytes(arquivo)));
        }
        assertTrue(classes.containsKey("RecuperacaoJarSegundaFase.class"), "Compilar o observador antes da preparação");
        Path template = Path.of("src/test/resources/auditoria-endpoints.env").toAbsolutePath();
        Map<String, Object> config = mapa("chunk", 1, "staleAfterSeconds", 15, "recoveryDelayMs", 1000,
                "heapMiB", 192, "metaspaceMiB", 192, "poolMax", 2,
                "envTemplate", template.toString(), "envTemplateSha256", hash(Files.readAllBytes(template)),
                "uploadDir", evidencias.resolve("uploads").toString(), "relatorioDir", evidencias.resolve("relatorios").toString());
        for (String chave : List.of("REDIS_HOST", "REDIS_PORT", "REDIS_DATABASE")) {
            String valor = System.getenv(chave);
            assertNotNull(valor, "Infraestrutura isolada explícita: " + chave);
            assertFalse(valor.isBlank()); config.put(chave, valor);
        }
        Map<String, Object> handoff = mapa("schemaVersion", 1, "status", "PREPARATION_ONLY_NOT_ACCEPTANCE",
                "preparationIsAcceptance", false, "jobId", preparacao.id(), "tentativa", 2, "population", QUANTIDADE,
                "data", massa.data().toString(), "lotacaoId", massa.c().lotacaoA.getId(), "jornadaId", massa.c().jornadaA.getId(),
                "contratoId", massa.c().contratoA.getId(), "gestaoId", massa.c().gestaoA.getId(),
                "autor", mapa("id", massa.c().localA.getId(), "matricula", massa.c().localA.getMatricula(),
                        "username", massa.c().localA.getUsername(), "perfil", massa.c().localA.getPerfil().name()),
                "empregados", massa.empregados().stream().sorted().toList(), "vinculos", massa.vinculos().stream().sorted().toList(),
                "justificativa", massa.justificativa(), "prefixoCancelado", preparacao.cancelado().frequencias().size(),
                "cancelado", preparacao.cancelado(), "antesDaMorte", preparacao.antesDaMorte(), "aposMorte", preparacao.aposMorte(),
                "snapshotAntesMorte", mapper.readTree(preparacao.canonicoAntesDaMorte()),
                "snapshotAposMorte", mapper.readTree(preparacao.canonicoAposMorte()),
                "snapshotAposMorteSha256", hash(preparacao.canonicoAposMorte().getBytes(StandardCharsets.UTF_8)),
                "databaseIdentity", identidade, "jar", jar.toString(), "jarSha256", hashJar,
                "javaDirectory", Path.of("").toAbsolutePath().toString(), "config", config, "classHashes", classes);
        gravar("handoff-recuperacao-fase2.json", handoff);
        gravar("handoff-recuperacao-fase2.sha256.json", mapa("sha256",
                hash(Files.readAllBytes(evidencias.resolve("handoff-recuperacao-fase2.json")))));
    }

    @Test
    @Timeout(value = 4, unit = TimeUnit.MINUTES)
    void revogacaoRealDoCriadorDuranteBlocoRecusaNovosEfeitosEPreservaPrefixo() throws Exception {
        Massa massa = preparar("revogacao-criador");
        long id = 0;
        long inicio = 0;
        try {
            Filho filho = iniciar("revogacao", 1);
            try (Sessao criador = login(filho, massa.c().localA);
                 Sessao administrador = login(filho, massa.c().admin);
                 Sessao leitor = login(filho, massa.c().nacional)) {
                inicio = System.nanoTime();
                id = criador.enviar("POST", API, comando(massa), 202).path("id").asLong();
                aguardarPrefixo(criador, id, inicio + PRAZO.toNanos());
                Fotografia prefixo;
                try (Barreira barreira = new Barreira(massa, null)) {
                    aguardarEspera(barreira.pid, Set.of(filho.aplicacao()), "contrato");
                    prefixo = fotografar(id, massa);
                    conferirPrefixoParcial(prefixo, 1);
                    assertEquals("PROCESSANDO", prefixo.job().get("status"));
                    gravar("prefixo-antes-da-revogacao.json", prefixo);
                    administrador.enviar("PUT", "/api/usuarios/" + massa.c().localA.getId(), mapa("active", false), 200);
                    emTransacao(() -> {
                        em.clear();
                        assertFalse(em.find(UsuarioAplicacao.class, massa.c().localA.getId()).getActive());
                        assertEquals(0L, em.createQuery("select count(t) from TokenAtivo t where t.username=:u", Long.class)
                                .setParameter("u", massa.c().localA.getUsername()).getSingleResult());
                        return null;
                    });
                    criador.enviar("GET", "/api/autenticacao/sessao", null, 401);
                    Fotografia commitRevogacao = fotografar(id, massa);
                    assertEquals(prefixo.frequencias(), commitRevogacao.frequencias());
                    assertEquals(prefixo.eventos(), commitRevogacao.eventos());
                    assertEquals(prefixo.historicos(), commitRevogacao.historicos());
                    gravar("revogacao-http-confirmada-antes-de-liberar.json", mapa("criadorId", massa.c().localA.getId(),
                            "activeConfirmado", false, "sessoesAtivasConfirmadas", 0, "estado", commitRevogacao));
                }
                JsonNode terminal = aguardarTerminal(leitor, id, inicio + PRAZO.toNanos());
                long duracaoAteRecusaMs = decorridoMs(inicio);
                // processarBlocoFechamento revalida após os locks; o catch do processador chama EstadoServico.falhar.
                assertEquals("ERRO", terminal.path("status").asText(), "Revogação é recusa do bloco, não conclusão ou cancelamento");
                Fotografia recusado = fotografar(id, massa);
                assertEquals(prefixo.frequencias(), recusado.frequencias(), "Nenhuma frequência nova depois do commit da revogação");
                assertEquals(prefixo.eventos(), recusado.eventos());
                assertEquals(prefixo.historicos(), recusado.historicos());
                preservar(prefixo, recusado);
                conferirContadores(recusado, prefixo.frequencias().size(), QUANTIDADE);
                assertEquals("ERRO", recusado.job().get("status"));
                assertNotNull(recusado.job().get("detalhe_erro"));
                assertFalse(recusado.job().get("detalhe_erro").isBlank());
                assertTrue(filho.processo().isAlive());
                gravar("revogacao-terminal-sem-novos-efeitos.json", mapa("terminalHttp", terminal,
                        "terminalPersistido", recusado, "duracaoPostAteRecusaMs", duracaoAteRecusaMs));
            }
        } finally {
            finalizar(massa, id, inicio);
        }
    }

    @Test
    @Timeout(value = 4, unit = TimeUnit.MINUTES)
    void morteDuranteChunkQuinhentosReverteFlushesIntermediariosSemCommitParcial() throws Exception {
        Massa massa = preparar("atomicidade-flush-chunk500");
        long id = 0;
        long inicio = 0;
        try {
            Filho filho = iniciar("atomicidade-flush", 500);
            assertEquals(300, filho.staleAfterSeconds());
            assertEquals(30000, filho.recoveryDelayMs());
            List<Long> ordenados = massa.empregados().stream().sorted().toList();
            long empregadoAlvo = ordenados.get(99);
            long vinculoAlvo = emTransacao(() -> em.createQuery(
                            "select v.id from VinculoContratual v, Empregado e where e.id=:empregado "
                                    + "and v.pessoa=e.pessoaCanonica and v.contrato.id=:contrato", Long.class)
                    .setParameter("empregado", empregadoAlvo)
                    .setParameter("contrato", massa.c().contratoA.getId()).getSingleResult());
            try (Sessao sessao = login(filho, massa.c().localA)) {
                JsonNode criada = sessao.enviar("POST", "/api/frequencias", mapa(
                        "vinculoContratualId", vinculoAlvo, "dataReferencia", massa.data(), "status", "FALTA",
                        "justificativaRetificacao", "Fonte sintética original anterior ao ensaio de rollback",
                        "chaveIdempotencia", "original-rollback-" + UUID.randomUUID()), 200);
                long frequenciaAlvo = criada.path("id").asLong();
                assertTrue(frequenciaAlvo > 0);
                List<Map<String, String>> auditoriaOriginal = aguardarAuditoriaOriginal(massa.c().localA.getId(), frequenciaAlvo);
                Fotografia original = fotografar(0, massa);
                assertEquals(1, original.frequencias().size());
                assertEquals(1, original.eventos().size());
                assertEquals(1, original.historicos().size());
                assertEquals(String.valueOf(frequenciaAlvo), original.frequencias().get(0).get("id"));
                assertEquals(String.valueOf(empregadoAlvo), original.frequencias().get(0).get("id_empregado"));
                assertEquals("FALTA", original.frequencias().get(0).get("status"));
                gravar("atomicidade-fonte-original-confirmada.json", mapa("estado", original,
                        "auditoriaGlobalFrequenciaSave", auditoriaOriginal, "empregadoAlvo", empregadoAlvo,
                        "frequenciaAlvo", frequenciaAlvo, "posicaoOrdenada", 100));

                // Só a Frequência é retida: os locks globais de Contrato/Pessoa/Vínculo/Empregado continuam livres.
                try (Barreira barreira = new Barreira(frequenciaAlvo)) {
                    Map<String, Object> pedido = comando(massa);
                    pedido.put("alteracoes", List.of(mapa("empregado", mapa("id", empregadoAlvo), "status", "PRESENCA",
                            "justificativaRetificacao", "Alteração sintética que deve ser integralmente revertida",
                            "chaveIdempotencia", "alteracao-rollback-" + UUID.randomUUID())));
                    inicio = System.nanoTime();
                    JsonNode aceito = sessao.enviar("POST", API, pedido, 202);
                    id = aceito.path("id").asLong();
                    assertTrue(id > 0);
                    assertFalse(aceito.path("somenteAlteracoes").asBoolean(), "É um fechamento completo, não uma tarefa de um item");
                    long limite = inicio + PRAZO.toNanos();
                    List<Map<String, String>> espera = aguardarEspera(barreira.pid,
                            Set.of(filho.aplicacao()), "frequencia", limite);
                    assertTrue(System.nanoTime() <= limite, "O alvo da posição100 deve ser alcançado até120s desde o POST");
                    long tempoAteAlvoMs = decorridoMs(inicio);
                    Fotografia bloqueado = fotografar(id, massa);
                    assertEquals("PROCESSANDO", bloqueado.job().get("status"));
                    assertEquals("0", bloqueado.job().get("somente_alteracoes"));
                    assertEquals("1", bloqueado.job().get("tentativa"));
                    assertEquals("200", bloqueado.job().get("total_previsto"));
                    assertEquals("1", bloqueado.job().get("total_alteracoes"));
                    for (String contador : List.of("total_processado", "total_inserido", "total_atualizado", "total_preservado"))
                        assertEquals("0", bloqueado.job().get(contador), "Flush intermediário não confirma o contador: " + contador);
                    assertNull(bloqueado.job().get("ultimo_empregado_id_processado"));
                    assertEquals(original.frequencias(), bloqueado.frequencias(), "As99 frequências anteriores ainda não estão confirmadas");
                    assertEquals(original.eventos(), bloqueado.eventos());
                    assertEquals(original.historicos(), bloqueado.historicos());
                    List<Map<String, String>> auditoriaDurante = auditoriaFrequenciaSave(massa.c().localA.getId());
                    assertEquals(auditoriaOriginal, auditoriaDurante, "Auditoria global também permanece restrita à fonte original");
                    gravar("atomicidade-flushes-ainda-na-mesma-transacao.json", mapa("estado", bloqueado,
                            "auditoriaGlobalFrequenciaSave", auditoriaDurante, "esperaFisica", espera,
                            "pidBloqueador", barreira.pid, "frequenciaAlvo", frequenciaAlvo, "posicaoOrdenada", 100,
                            "limitesFlushClearInferidosDaOrdem", List.of(25, 50, 75),
                            "baseDaInferencia", "Posição100 no laço ordenado, contextos de25 e bloqueio físico na frequência original",
                            "tempoPostAteAlvoMs", tempoAteAlvoMs));

                    filho.matar();
                    // Sem cliente capaz de enviar COMMIT, liberar o SELECT permite detectar EOF e reverter o bloco.
                    barreira.close();
                    aguardarAusencia(filho.aplicacao());
                    Fotografia aposRollback = fotografar(id, massa);
                    List<Map<String, String>> auditoriaDepois = auditoriaFrequenciaSave(massa.c().localA.getId());
                    assertEquals(bloqueado, aposRollback, "Morte do JAR reverte todo o bloco e não altera o job confirmado");
                    assertEquals(original.frequencias(), aposRollback.frequencias());
                    assertEquals(original.eventos(), aposRollback.eventos());
                    assertEquals(original.historicos(), aposRollback.historicos());
                    assertEquals(auditoriaOriginal, auditoriaDepois, "Nenhum FREQUENCIA_SAVE parcial foi publicado ou confirmado");
                    assertEquals(hashJar, hashArquivo(jar));
                    gravar("atomicidade-rollback-integral-apos-morte.json", mapa("estado", aposRollback,
                            "auditoriaGlobalFrequenciaSave", auditoriaDepois, "jarSha256", hashJar,
                            "prova", "ADVERSARIAL_SEM_TERMINAL_INTENCIONAL", "chunk", 500, "populacao", 200,
                            "posicaoBloqueada", 100, "jvmFilhaEncerrada", !filho.processo().isAlive(),
                            "limiteAteAlvoSegundos", 120, "tempoPostAteAlvoMs", tempoAteAlvoMs));
                }
            }
        } finally {
            finalizar(massa, id, inicio);
        }
    }

    private List<Map<String, String>> auditoriaFrequenciaSave(long criadorId) {
        return linhas("SELECT * FROM registro_auditoria WHERE user_id=? "
                + "AND action='FREQUENCIA_SAVE' AND entity_type='Frequencia' ORDER BY id", criadorId);
    }

    private List<Map<String, String>> aguardarAuditoriaOriginal(long criadorId, long frequenciaId) throws InterruptedException {
        long limite = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        do {
            List<Map<String, String>> auditoria = auditoriaFrequenciaSave(criadorId);
            if (!auditoria.isEmpty()) {
                assertEquals(1, auditoria.size(), "A fonte original é a única escrita de frequência deste criador");
                assertEquals(String.valueOf(frequenciaId), auditoria.get(0).get("entity_id"));
                return auditoria;
            }
            Thread.sleep(100);
        } while (System.nanoTime() < limite);
        return fail("A auditoria global original não foi confirmada antes do POST do job; não comparar contra uma fila atrasada");
    }

    private record Massa(Cenario c, LocalDate data, Set<Long> empregados, Set<Long> vinculos, String justificativa) {}
    private record Fotografia(Map<String, String> job, List<Map<String, String>> frequencias,
                              List<Map<String, String>> eventos, List<Map<String, String>> historicos) {}

    private Massa preparar(String nome) throws Exception {
        evidencias = diretorioPrivado(nome + "-");
        Files.createFile(evidencias.resolve("http.jsonl"), permissaoArquivo());
        jar = Path.of("target/sig-gtrat-backend-0.0.4-operacional-20261101.jar").toAbsolutePath();
        assertTrue(Files.isRegularFile(jar), "O root deve empacotar o candidato antes da execução opt-in");
        hashJar = hashArquivo(jar);
        conferirClasses();
        if (SCHEMA_ISOLADO != null) {
            assertEquals(SCHEMA_ISOLADO, jdbc.queryForObject("SELECT current_schema()", String.class));
            assertEquals(Boolean.TRUE, jdbc.queryForObject("SELECT pg_get_userbyid(nspowner)=current_user "
                    + "FROM pg_namespace WHERE nspname=current_schema()", Boolean.class));
        }
        assertEquals(0L, jdbc.queryForObject("SELECT count(*) FROM fechamento_frequencia "
                + "WHERE status IN ('PENDENTE','PROCESSANDO')", Long.class),
                "Cada cenário adversarial exige contexto sem jobs anteriores recuperáveis; não purgar para satisfazer o teste");
        Cenario c = criarCenarioPersistido();
        Massa massa = emTransacao(() -> {
            Contrato contrato = em.find(Contrato.class, c.contratoA.getId());
            ComposicaoContratual composicao = em.find(ComposicaoContratual.class, c.composicaoA.getId());
            contrato.setEfetivoPrevisto(QUANTIDADE);
            contrato.setEfetivoOriginalPublicado(QUANTIDADE);
            composicao.setEfetivoPrevisto(QUANTIDADE);
            em.createQuery("select v from VersaoContratoNegocial v where v.contrato.id=:id", VersaoContratoNegocial.class)
                    .setParameter("id", contrato.getId()).getResultList().forEach(v -> v.setEfetivoPrevisto(QUANTIDADE));
            Set<String> cpfs = new HashSet<>(em.createQuery("select p.cpf from Pessoa p", String.class).getResultList());
            Set<Long> empregados = new LinkedHashSet<>(List.of(c.empregadoA.getId()));
            Set<Long> vinculos = new LinkedHashSet<>(List.of(c.vinculoA.getId()));
            int semente = 91000000;
            for (int n = 1; n < QUANTIDADE; n++) {
                String documento;
                do { documento = cpf(semente++); } while (!cpfs.add(documento));
                Pessoa pessoa = pessoa("Pessoa fechamento JAR " + UUID.randomUUID(), documento);
                VinculoContratual vinculo = vinculo(pessoa, contrato);
                AlocacaoVinculo alocacao = alocacao(vinculo, composicao);
                empregados.add(projecao.projetar(vinculo, alocacao).getId());
                vinculos.add(vinculo.getId());
            }
            em.flush(); em.clear();
            assertEquals(QUANTIDADE, empregados.size());
            assertEquals(QUANTIDADE, vinculos.size());
            LocalDate hoje = LocalDate.now(ZONA);
            assertFalse(hoje.minusDays(20).isBefore(INICIO));
            assertFalse(hoje.isAfter(FIM));
            LocalDate data = null;
            for (int d = 1; d <= 20; d++) {
                LocalDate candidata = hoje.minusDays(d);
                if (candidata.getDayOfWeek().getValue() > 5) continue;
                long calendario = em.createQuery("select count(e) from CalendarioEvento e where e.data=:data "
                                + "and e.ativo=true and e.cancelado=false and upper(e.tipo) in ('FERIADO','DIA_DESCANSO')", Long.class)
                        .setParameter("data", candidata).getSingleResult();
                if (calendario == 0) { data = candidata; break; }
            }
            assertNotNull(data, "Fixture precisa de um dia útil sem evento de calendário na cobertura do contrato");
            return new Massa(c, data, Set.copyOf(empregados), Set.copyOf(vinculos),
                    "Conferência sintética coletiva de duzentos vínculos " + UUID.randomUUID());
        });
        assertEquals(0, fotografar(0, massa).frequencias().size());
        gravar("fixture-sem-credenciais.json", mapa("sintetica", true, "dataReferencia", massa.data().toString(),
                "quantidade", QUANTIDADE, "empregados", massa.empregados(), "vinculos", massa.vinculos(),
                "contratoId", c.contratoA.getId(), "gestaoId", c.gestaoA.getId(),
                "jarSha256", hashJar, "janelaPadraoDias", 7, "horasEdicao", 72));
        return massa;
    }

    private Map<String, Object> comando(Massa massa) {
        Cenario c = massa.c();
        return mapa("dataReferencia", massa.data(), "lotacaoId", c.lotacaoA.getId(), "jornadaId", c.jornadaA.getId(),
                "contratoIds", List.of(c.contratoA.getId()), "alteracoes", List.of(),
                "justificativaRetificacao", massa.justificativa(), "chaveIdempotencia", "closure-" + c.contratoA.getId());
    }

    private JsonNode aguardarTerminal(Sessao sessao, long id, long limite) throws Exception {
        JsonNode ultimo = null;
        long processadoAnterior = -1;
        Integer tentativa = null;
        do {
            ultimo = sessao.enviar("GET", API + "/" + id, null, 200);
            assertEquals(id, ultimo.path("id").asLong());
            int atual = ultimo.path("tentativa").asInt();
            if (tentativa == null) tentativa = atual;
            assertEquals(tentativa.intValue(), atual, "Recuperação automática não cria nova tentativa");
            long processados = ultimo.path("totalProcessado").asLong();
            assertTrue(processados >= processadoAnterior, "Progresso não retrocede dentro da mesma tentativa");
            processadoAnterior = processados;
            if (System.nanoTime() > limite) break;
            if (Set.of("CONCLUIDO", "CANCELADO", "ERRO").contains(ultimo.path("status").asText())) return ultimo;
            Thread.sleep(150);
        } while (System.nanoTime() < limite);
        gravar("timeout-job-" + id + ".json", mapa("limiteSegundos", 120, "ultimoHttp", ultimo,
                "atividade", atividadeSanitizada()));
        return fail("Fechamento não atingiu terminal no prazo de120s; evidência parcial privada preservada");
    }

    private void aguardarPrefixo(Sessao sessao, long id, long limite) throws Exception {
        do {
            JsonNode atual = sessao.enviar("GET", API + "/" + id, null, 200);
            if (System.nanoTime() > limite) break;
            long n = atual.path("totalProcessado").asLong();
            if (n > 0 && n < atual.path("totalPrevisto").asLong()) return;
            assertFalse(Set.of("CONCLUIDO", "CANCELADO", "ERRO").contains(atual.path("status").asText()),
                    "É preciso capturar prefixo confirmado e não terminal");
            Thread.sleep(30);
        } while (System.nanoTime() < limite);
        fail("Não houve prefixo confirmado N>0 na janela de120s");
    }

    private Fotografia fotografar(long id, Massa massa) {
        TransactionTemplate leitura = new TransactionTemplate(transactionManager);
        leitura.setReadOnly(true);
        leitura.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        return leitura.execute(status -> {
            List<Map<String, String>> jobs = linhas("SELECT * FROM fechamento_frequencia WHERE id=?", id);
            Cenario c = massa.c();
            Object[] escopo = {java.sql.Date.valueOf(massa.data()), c.lotacaoA.getId(), c.jornadaA.getId(), c.contratoA.getId()};
            var frequencias = linhas("SELECT f.* FROM frequencia f WHERE f.data_referencia=? AND f.lotacao_snapshot_id=? "
                    + "AND f.jornada_snapshot_id=? AND f.contrato_snapshot_id=? ORDER BY f.id", escopo);
            var eventos = linhas("SELECT e.* FROM evento_frequencia e JOIN frequencia f ON f.id=e.id_frequencia "
                    + "WHERE f.data_referencia=? AND f.lotacao_snapshot_id=? AND f.jornada_snapshot_id=? "
                    + "AND f.contrato_snapshot_id=? ORDER BY e.id_evento_frequencia", escopo);
            var historicos = linhas("SELECT h.* FROM historico_empregado h WHERE h.id_empregado IN "
                    + "(SELECT e.id_empregado FROM empregado e JOIN vinculo_contratual v ON v.id_pessoa=e.id_pessoa_canonica "
                    + "WHERE v.id_contrato=?) ORDER BY h.id_historico", c.contratoA.getId());
            return new Fotografia(jobs.isEmpty() ? Map.of() : jobs.get(0), frequencias, eventos, historicos);
        });
    }

    private List<Map<String, String>> linhas(String sql, Object... argumentos) {
        return jdbc.query(sql, (rs, numero) -> {
            Map<String, String> linha = new TreeMap<>();
            var meta = rs.getMetaData();
            for (int c = 1; c <= meta.getColumnCount(); c++) {
                Object valor = rs.getObject(c);
                linha.put(meta.getColumnLabel(c), valor instanceof Boolean b ? (b ? "1" : "0")
                        : valor instanceof Number n ? new java.math.BigDecimal(n.toString()).stripTrailingZeros().toPlainString()
                        : rs.getString(c));
            }
            return linha;
        }, argumentos);
    }

    private void conferirDuzentos(Massa massa, Fotografia foto) {
        assertEquals(QUANTIDADE, foto.frequencias().size());
        assertEquals(QUANTIDADE, foto.eventos().size(), "Um único evento por frequência criada; nenhum duplo commit");
        assertEquals(massa.vinculos(), foto.frequencias().stream().map(l -> Long.valueOf(l.get("id_vinculo_contratual"))).collect(Collectors.toSet()));
        assertEquals(massa.empregados(), foto.frequencias().stream().map(l -> Long.valueOf(l.get("id_empregado"))).collect(Collectors.toSet()));
        assertEquals(QUANTIDADE, foto.frequencias().stream().map(l -> l.get("id_pessoa")).distinct().count());
        assertEquals(QUANTIDADE, foto.eventos().stream().map(l -> l.get("hash_evento")).distinct().count());
        assertEquals(QUANTIDADE, foto.eventos().stream().map(l -> l.get("chave_idempotencia")).distinct().count());
        for (var linha : foto.frequencias()) {
            assertEquals("PRESENCA", linha.get("status"));
            assertEquals(massa.c().localA.getId().toString(), linha.get("autor_original_id"));
            assertEquals("1", linha.get("versao_negocial"));
        }
        for (var evento : foto.eventos()) {
            assertEquals("1", evento.get("numero_versao"));
            assertEquals(massa.c().localA.getId().toString(), evento.get("usuario_id"));
            assertEquals(massa.c().localA.getUsername(), evento.get("usuario_username"));
            assertEquals(massa.justificativa(), evento.get("motivo"));
        }
    }

    private void conferirPrefixoParcial(Fotografia foto, int tentativa) {
        assertEquals(String.valueOf(tentativa), foto.job().get("tentativa"));
        assertTrue(foto.frequencias().size() > 0 && foto.frequencias().size() < QUANTIDADE);
        assertEquals(foto.frequencias().size(), foto.eventos().size());
    }

    private void conferirContadores(Fotografia foto, long processados, long previstos) {
        assertEquals(String.valueOf(processados), foto.job().get("total_processado"));
        assertEquals(String.valueOf(processados), foto.job().get("total_inserido"));
        assertEquals(String.valueOf(previstos), foto.job().get("total_previsto"));
        assertEquals("0", foto.job().get("total_atualizado"));
        assertEquals("0", foto.job().get("total_preservado"));
        long ultimo = foto.frequencias().stream().mapToLong(l -> Long.parseLong(l.get("id_empregado"))).max().orElseThrow();
        assertEquals(String.valueOf(ultimo), foto.job().get("ultimo_empregado_id_processado"));
    }

    private void preservar(Fotografia antes, Fotografia depois) {
        for (var linha : antes.frequencias()) assertTrue(depois.frequencias().contains(linha), "Frequência confirmada integralmente preservada");
        for (var linha : antes.eventos()) assertTrue(depois.eventos().contains(linha), "Evento confirmado integralmente preservado");
        for (var linha : antes.historicos()) assertTrue(depois.historicos().contains(linha), "Trilha histórica confirmada integralmente preservada");
        for (String campo : List.of("id", "chave_escopo", "data_referencia", "id_lotacao", "id_jornada",
                "contrato_ids", "id_usuario_criador", "criado_em", "justificativa_retificacao", "chave_idempotencia"))
            assertEquals(antes.job().get(campo), depois.job().get(campo), "Identidade/intenção do job: " + campo);
    }

    /** Transação sem DML: retém apenas SELECT FOR UPDATE nas PKs conhecidas. */
    private final class Barreira implements AutoCloseable {
        private final EntityManager entidade = em.getEntityManagerFactory().createEntityManager();
        private final long pid;
        private Barreira(long frequenciaId) {
            try {
                entidade.getTransaction().begin();
                assertNotNull(entidade.find(Frequencia.class, frequenciaId, LockModeType.PESSIMISTIC_WRITE));
                pid = ((Number) entidade.createNativeQuery("SELECT pg_backend_pid()").getSingleResult()).longValue();
            } catch (RuntimeException | AssertionError falha) {
                close(); throw falha;
            }
        }
        private Barreira(Class<FechamentoFrequencia> tipoJob, long jobId) {
            try {
                entidade.getTransaction().begin();
                assertNotNull(entidade.find(tipoJob, jobId, LockModeType.PESSIMISTIC_WRITE));
                assertEquals(1L, ((Number) entidade.createNativeQuery("SELECT count(*) FROM fechamento_frequencia "
                                + "WHERE status IN ('PENDENTE','PROCESSANDO')").getSingleResult()).longValue(),
                        "A barreira de recuperação retém somente a PK do único job ativo conhecido");
                pid = ((Number) entidade.createNativeQuery("SELECT pg_backend_pid()").getSingleResult()).longValue();
            } catch (RuntimeException | AssertionError falha) {
                close(); throw falha;
            }
        }
        private Barreira(Massa massa, Long job) {
            try {
                entidade.getTransaction().begin();
                assertNotNull(entidade.find(Contrato.class, massa.c().contratoA.getId(), LockModeType.PESSIMISTIC_WRITE));
                if (job != null) {
                    assertNotNull(entidade.find(FechamentoFrequencia.class, job, LockModeType.PESSIMISTIC_WRITE));
                    assertEquals(1L, ((Number) entidade.createNativeQuery("SELECT count(*) FROM fechamento_frequencia "
                                    + "WHERE status IN ('PENDENTE','PROCESSANDO')").getSingleResult()).longValue(),
                            "Há somente o job conhecido ativo neste banco; a espera física não pode pertencer a outro job");
                }
                pid = ((Number) entidade.createNativeQuery("SELECT pg_backend_pid()").getSingleResult()).longValue();
            } catch (RuntimeException | AssertionError falha) {
                close(); throw falha;
            }
        }
        @Override public void close() {
            if (!entidade.isOpen()) return;
            try {
                if (entidade.getTransaction().isActive()) entidade.getTransaction().rollback();
            } finally {
                entidade.close();
            }
        }
    }

    private List<Map<String, String>> aguardarEspera(long bloqueador, Set<String> aplicacoes, String tabela) throws Exception {
        return aguardarEspera(bloqueador, aplicacoes, tabela, System.nanoTime() + Duration.ofSeconds(20).toNanos());
    }

    private List<Map<String, String>> aguardarEspera(long bloqueador, Set<String> aplicacoes, String tabela, long limite) throws Exception {
        do {
            var esperas = linhas("""
                WITH RECURSIVE fila(pid) AS (
                  SELECT a.pid FROM pg_stat_activity a WHERE a.datname=current_database()
                    AND ?=ANY(pg_blocking_pids(a.pid))
                  UNION
                  SELECT a.pid FROM pg_stat_activity a JOIN fila b ON b.pid=ANY(pg_blocking_pids(a.pid))
                    WHERE a.datname=current_database()
                )
                SELECT a.pid,a.application_name,a.wait_event_type,a.wait_event,a.query,
                       pg_blocking_pids(a.pid)::text AS bloqueadores,
                       EXISTS(SELECT 1 FROM pg_locks l WHERE l.pid=a.pid
                         AND l.locktype='tuple' AND l.relation=to_regclass(?)) AS tupla_tabela_alvo
                FROM pg_stat_activity a JOIN fila f ON f.pid=a.pid
                """, bloqueador, tabela);
            List<Map<String, String>> prova = new ArrayList<>();
            for (var espera : esperas) {
                if (!aplicacoes.contains(espera.get("application_name"))) continue;
                String sql = espera.get("query").toLowerCase(Locale.ROOT).replace('"', ' ');
                boolean alvo = sql.matches("(?s).*\\b(?:from|update)\\s+(?:[a-z0-9_]+\\.)?" + tabela + "\\b.*")
                        && (sql.contains("for update") || sql.contains("for no key update") || sql.stripLeading().startsWith("update "));
                // pg_stat_activity.query é truncável (1 KiB por padrão). A fila
                // vem do bloqueador conhecido, e pg_locks identifica a tupla da
                // tabela alvo mesmo quando FROM/FOR UPDATE não cabem no texto.
                if (!"1".equals(espera.get("tupla_tabela_alvo"))
                        || !"Lock".equals(espera.get("wait_event_type"))) continue;
                Map<String, String> sanitizada = new TreeMap<>(espera);
                sanitizada.remove("query"); sanitizada.put("querySha256", hash(sql.getBytes(StandardCharsets.UTF_8)));
                sanitizada.put("criterioSqlAnteriorReconheceria", Boolean.toString(alvo));
                sanitizada.put("textoConsultaBytes", Integer.toString(sql.getBytes(StandardCharsets.UTF_8).length));
                sanitizada.put("tabelaAlvo", tabela); prova.add(sanitizada);
            }
            if (prova.stream().map(l -> l.get("application_name")).collect(Collectors.toSet()).containsAll(aplicacoes)) return prova;
            Thread.sleep(75);
        } while (System.nanoTime() < limite);
        gravar("disputa-nao-observada-" + tabela + ".json", atividadeSanitizada());
        return fail("Não houve disputa física dos executores nas PKs conhecidas; PIDs vivos não satisfazem o gate");
    }

    private List<Map<String, String>> atividadeSanitizada() {
        return linhas("SELECT pid,application_name,state,wait_event_type,wait_event,"
                + "pg_blocking_pids(pid)::text AS bloqueadores FROM pg_stat_activity "
                + "WHERE datname=current_database() ORDER BY pid");
    }

    private void aguardarAusencia(String aplicacao) throws InterruptedException {
        long limite = System.nanoTime() + Duration.ofSeconds(8).toNanos();
        do {
            Long quantidade = jdbc.queryForObject("SELECT count(*) FROM pg_stat_activity WHERE datname=current_database() AND application_name=?", Long.class, aplicacao);
            if (Objects.equals(0L, quantidade)) return;
            Thread.sleep(50);
        } while (System.nanoTime() < limite);
        fail("Conexões da JVM filha morta não terminaram; não prosseguir com recuperação ambígua");
    }

    private record Filho(Process processo, URI origem, String aplicacao, Path log,
                         int chunk, long staleAfterSeconds, long recoveryDelayMs) implements AutoCloseable {
        private void matar() throws InterruptedException {
            assertTrue(processo.isAlive(), "Somente a JVM filha conhecida pode receber morte controlada");
            processo.destroyForcibly();
            assertTrue(processo.waitFor(8, TimeUnit.SECONDS));
            assertFalse(processo.isAlive());
        }
        @Override public void close() throws InterruptedException {
            if (!processo.isAlive()) return;
            processo.destroy();
            if (!processo.waitFor(10, TimeUnit.SECONDS)) matar();
        }
    }

    private Filho iniciar(String nome, int chunk) throws Exception {
        assertEquals(hashJar, hashArquivo(jar), "JVMs antes/depois usam os mesmos bytes");
        assertTrue(filhos.stream().filter(f -> f.processo().isAlive()).count() < 2, "No máximo dois filhos ativos");
        boolean recuperacaoAcelerada = Set.of("original", "recuperador-b", "recuperador-c").contains(nome);
        long staleAfterSeconds = recuperacaoAcelerada ? 15 : 300;
        long recoveryDelayMs = recuperacaoAcelerada ? 1000 : 30000;
        int porta;
        try (ServerSocket reserva = new ServerSocket(0, 0, InetAddress.getByName("127.0.0.1"))) { porta = reserva.getLocalPort(); }
        String aplicacao = "closure-" + nome + "-" + UUID.randomUUID().toString().substring(0, 8);
        Path log = Files.createFile(evidencias.resolve(nome + ".log"), permissaoArquivo());
        Path arquivos = evidencias.resolve("uploads");
        List<String> comando = new ArrayList<>(List.of(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-Xmx192m", "-XX:MaxMetaspaceSize=192m", "-XX:ActiveProcessorCount=2", "-XX:+UseSerialGC", "-Duser.timezone=America/Sao_Paulo",
                "-jar", jar.toString(), "--env-file=./src/test/resources/auditoria-endpoints.env",
                "--server.address=127.0.0.1", "--server.port=" + porta, "--server.ssl.enabled=false",
                "--spring.main.banner-mode=off", "--spring.jpa.hibernate.ddl-auto=update", "--spring.sql.init.mode=never",
                "--spring.jpa.open-in-view=false", "--spring.jpa.show-sql=false", "--spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true",
                "--spring.datasource.hikari.maximum-pool-size=2", "--spring.datasource.hikari.minimum-idle=1",
                "--logging.level.root=WARN", "--logging.level.com.example.siggtrat=WARN",
                "--logging.level.org.hibernate.SQL=OFF", "--logging.level.org.hibernate.orm.jdbc.bind=OFF",
                "--app.email.enabled=false", "--app.email.gmail.enabled=false", "--spring.cache.type=redis",
                "--app.seed.unidades-operacionais-enabled=false", "--app.seed.jornadas-operacionais-enabled=false",
                "--app.seed.usuarios-globais-enabled=false", "--app.seed.gestores-se-gestao-local-enabled=false",
                "--file.upload-dir=" + arquivos, "--audit.fallback-log=" + evidencias.resolve(nome + "-audit-fallback.jsonl"),
                "--app.relatorios.temp.diretorio=" + evidencias.resolve("relatorios"),
                "--frequencia.fechamento.chunk-size=" + chunk, "--frequencia.fechamento.executor-core-pool-size=1",
                "--frequencia.fechamento.executor-max-pool-size=1", "--frequencia.fechamento.recovery-delay-ms=" + recoveryDelayMs,
                "--frequencia.fechamento.stale-after-seconds=" + staleAfterSeconds));
        if (SCHEMA_ISOLADO != null)
            comando.add("--spring.jpa.properties.hibernate.default_schema=" + SCHEMA_ISOLADO);
        ProcessBuilder builder = new ProcessBuilder(comando).redirectErrorStream(true).redirectOutput(log.toFile());
        var ambiente = builder.environment(); ambiente.clear(); ambiente.put("TZ", "America/Sao_Paulo");
        var banco = ConfiguracaoPostgresIntegracao.carregar();
        String url = banco.url() + (banco.url().contains("?") ? "&" : "?") + "ApplicationName=" + aplicacao;
        ambiente.put("DB_URL", url); ambiente.put("DB_USER", banco.usuario()); ambiente.put("DB_PASS", banco.senha());
        ambiente.put("SPRING_DATASOURCE_URL", url); ambiente.put("SPRING_DATASOURCE_USERNAME", banco.usuario());
        ambiente.put("SPRING_DATASOURCE_PASSWORD", banco.senha());
        for (String chave : List.of("REDIS_HOST", "REDIS_PORT", "REDIS_PASSWORD", "REDIS_DATABASE")) {
            String valor = System.getenv(chave);
            assertNotNull(valor, "Infraestrutura isolada explícita: " + chave);
            assertFalse(valor.isBlank()); ambiente.put(chave, valor);
        }
        ambiente.put("REDIS_HTTP_GET_ENABLED", "true");
        Filho filho = new Filho(builder.start(), URI.create("http://127.0.0.1:" + porta), aplicacao, log,
                chunk, staleAfterSeconds, recoveryDelayMs);
        filhos.add(filho);
        gravar("processos.json", filhos.stream().map(f -> mapa("pid", f.processo().pid(), "aplicacao", f.aplicacao(),
                "origem", f.origem().toString(), "jarSha256", hashJar, "chunk", f.chunk(),
                "staleAfterSeconds", f.staleAfterSeconds(), "recoveryDelayMs", f.recoveryDelayMs(),
                "heapMiB", 192, "metaspaceMiB", 192)).toList());
        try (HttpClient cliente = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build()) {
            long limite = System.nanoTime() + Duration.ofSeconds(55).toNanos();
            do {
                assertTrue(filho.processo().isAlive(), "JAR encerrou antes da prontidão; log privado: " + log);
                try {
                    var r = cliente.send(HttpRequest.newBuilder(filho.origem().resolve("/api/autenticacao/csrf"))
                            .timeout(Duration.ofSeconds(1)).GET().build(), HttpResponse.BodyHandlers.discarding());
                    if (r.statusCode() == 200) return filho;
                } catch (java.io.IOException iniciando) { /* Socket indisponível durante o boot real. */ }
                Thread.sleep(100);
            } while (System.nanoTime() < limite);
            fail("Tomcat não ficou pronto em55s; diagnóstico privado preservado");
            return filho;
        }
    }

    private Sessao login(Filho filho, UsuarioAplicacao ator) throws Exception {
        Sessao sessao = new Sessao(filho.origem());
        try {
            sessao.csrf();
            JsonNode autenticado = sessao.enviar("POST", "/api/autenticacao/autenticar",
                    mapa("matricula", ator.getMatricula(), "password", SENHA), 200);
            assertEquals(ator.getUsername(), autenticado.path("username").asText());
            assertEquals(ator.getPerfil().name(), autenticado.path("perfil").asText());
            assertTrue(sessao.cookies.getCookieStore().getCookies().stream().anyMatch(c -> c.getName().equals("autenticacao")));
            sessao.csrf();
            return sessao;
        } catch (Exception | AssertionError falha) { sessao.close(); throw falha; }
    }

    private final class Sessao implements AutoCloseable {
        private final URI origem;
        private final CookieManager cookies;
        private final HttpClient cliente;
        private String tokenCsrf;
        private Sessao(URI origem) { this(origem, new CookieManager(null, CookiePolicy.ACCEPT_ORIGINAL_SERVER)); }
        private Sessao(URI origem, CookieManager cookies) {
            this.origem = origem;
            this.cookies = cookies;
            cliente = HttpClient.newBuilder().cookieHandler(cookies).connectTimeout(Duration.ofSeconds(3))
                    .followRedirects(HttpClient.Redirect.NEVER).build();
        }
        private void csrf() throws Exception {
            JsonNode token = enviar("GET", "/api/autenticacao/csrf", null, 200);
            assertEquals("X-XSRF-TOKEN", token.path("headerName").asText());
            tokenCsrf = token.path("token").asText(); assertFalse(tokenCsrf.isBlank());
        }
        private JsonNode enviar(String metodo, String caminho, Object corpo, int esperado) throws Exception {
            long inicio = System.nanoTime();
            var request = HttpRequest.newBuilder(origem.resolve(caminho)).timeout(Duration.ofSeconds(15))
                    .header("Accept", "application/json").header("Cache-Control", "no-cache")
                    .header("User-Agent", "SGT-FechamentoJarRecuperacaoIT");
            if (!metodo.equals("GET")) {
                assertNotNull(tokenCsrf); request.header("X-XSRF-TOKEN", tokenCsrf).header("X-User-Password", SENHA);
            }
            if (corpo != null) request.header("Content-Type", "application/json");
            var resposta = cliente.send(request.method(metodo, corpo == null ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(json(corpo), StandardCharsets.UTF_8)).build(), HttpResponse.BodyHandlers.ofByteArray());
            Map<String, Object> registro = mapa("instante", Instant.now().toString(), "metodo", metodo, "caminho", caminho,
                    "esperado", esperado, "recebido", resposta.statusCode(), "duracaoMs", decorridoMs(inicio));
            JsonNode raiz = mapper.readTree(resposta.body());
            JsonNode dados = raiz.hasNonNull("data") ? raiz.get("data") : raiz;
            if (caminho.startsWith(API) && dados.hasNonNull("id") && dados.hasNonNull("tentativa")) {
                registro.put("job", mapa("id", dados.path("id").asLong(), "status", dados.path("status").asText(),
                        "tentativa", dados.path("tentativa").asInt(), "totalPrevisto", dados.path("totalPrevisto").asLong(),
                        "totalProcessado", dados.path("totalProcessado").asLong(), "totalInserido", dados.path("totalInserido").asLong(),
                        "totalAtualizado", dados.path("totalAtualizado").asLong(), "totalPreservado", dados.path("totalPreservado").asLong(),
                        "percentual", dados.path("percentual").asInt()));
            }
            Files.writeString(evidencias.resolve("http.jsonl"), json(registro) + "\n", StandardOpenOption.APPEND);
            assertEquals(esperado, resposta.statusCode(), metodo + " " + caminho);
            return dados;
        }
        @Override public void close() { cliente.close(); }
    }

    private void conferirClasses() throws Exception {
        Path raiz = Path.of("target/classes");
        try (ZipFile zip = new ZipFile(jar.toFile()); var arquivos = Files.walk(raiz)) {
            for (Path arquivo : arquivos.filter(p -> p.toString().endsWith(".class")).toList()) {
                String nome = raiz.relativize(arquivo).toString().replace('\\', '/');
                var entrada = zip.getEntry("BOOT-INF/classes/" + nome); assertNotNull(entrada, nome);
                try (var bytes = zip.getInputStream(entrada)) {
                    assertArrayEquals(Files.readAllBytes(arquivo), bytes.readAllBytes(), "Reempacotar antes do IT: " + nome);
                }
            }
        }
    }

    private void finalizar(Massa massa, long id, long inicio) throws Exception {
        try {
            if (id > 0) gravar("ultimo-snapshot-confirmado.json", fotografar(id, massa));
        } finally {
            boolean interrompido = Thread.interrupted();
            try {
                for (Filho filho : filhos) {
                    try { filho.close(); }
                    catch (InterruptedException falha) {
                        interrompido = true;
                        if (filho.processo().isAlive()) filho.processo().destroyForcibly();
                    }
                }
                if (evidencias != null) gravar("encerramento.json", mapa("duracaoTotalMs", inicio == 0 ? null : decorridoMs(inicio),
                        "jarSha256", hashJar, "filhos", filhos.stream().map(f -> mapa("pid", f.processo().pid(),
                                "vivo", f.processo().isAlive(), "exitCode", f.processo().isAlive() ? null : f.processo().exitValue())).toList()));
            } finally { if (interrompido) Thread.currentThread().interrupt(); }
        }
    }

    private void gravar(String nome, Object valor) throws Exception {
        byte[] bytes = (mapper.writerWithDefaultPrettyPrinter().writeValueAsString(valor) + "\n").getBytes(StandardCharsets.UTF_8);
        Path temporario = Files.createTempFile(evidencias, nome + "-", ".tmp", permissaoArquivo());
        try (FileChannel canal = FileChannel.open(temporario, StandardOpenOption.WRITE)) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            while (buffer.hasRemaining()) canal.write(buffer);
            canal.force(true);
        }
        Files.move(temporario, evidencias.resolve(nome), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        try (FileChannel diretorio = FileChannel.open(evidencias, StandardOpenOption.READ)) { diretorio.force(true); }
    }

    private static Path diretorioPrivado(String prefixo) {
        try {
            Path raiz = Path.of("target/fechamento-jar-recuperacao").toAbsolutePath(); Files.createDirectories(raiz);
            return Files.createTempDirectory(raiz, prefixo,
                    PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
        } catch (java.io.IOException falha) { throw new IllegalStateException("Armazenamento de teste exclusivo indisponível", falha); }
    }

    private static java.nio.file.attribute.FileAttribute<Set<java.nio.file.attribute.PosixFilePermission>> permissaoArquivo() {
        return PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------"));
    }
    private static long decorridoMs(long inicio) { return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicio); }
    private static String hash(byte[] bytes) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }

    private static String hashArquivo(Path arquivo) throws Exception {
        MessageDigest resumo = MessageDigest.getInstance("SHA-256");
        try (var entrada = Files.newInputStream(arquivo)) {
            byte[] bloco = new byte[8192];
            int lidos;
            while ((lidos = entrada.read(bloco)) != -1) resumo.update(bloco, 0, lidos);
        }
        return HexFormat.of().formatHex(resumo.digest());
    }
}
