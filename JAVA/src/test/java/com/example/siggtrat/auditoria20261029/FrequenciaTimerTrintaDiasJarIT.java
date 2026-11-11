package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.configuracao.VerificadorAmbientePostgres186;
import com.example.siggtrat.dto.FrequenciaDTO;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.infra.ConfiguracaoPostgresIntegracao;
import com.example.siggtrat.servico.FrequenciaServico;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.AopTestUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipFile;
import java.util.zip.CRC32;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

/**
 * TMP004/ROP034: um filho JAR não instrumentado, relógio físico e @Scheduled
 * produtivo. Pai N45/TaskScheduler herdado mock apenas prepara via API; nunca
 * chama fecharVencidas. Filho usa padrão N30 e cron configurável existente2s.
 * Após estabilidade do alvo, outra data útil do mesmo vínculo comprova nova
 * passagem física pelo fechamento; não se inventa contador de invocações.
 */
@TestPropertySource(properties = "frequencia.janela-operacional-dias=45")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class FrequenciaTimerTrintaDiasJarIT extends MaterializacaoTrintaSuporte {
    @MockitoSpyBean private FrequenciaServico frequenciaPreparador;
    private static final String JAR_SHA = "dcb3a38443c0f941d70f95f10a3a9f80bb44ee1a6aa57f1c29006fc3bf726f7d";
    private static final String CRON = "*/2 * * * * *";
    private final String aplicacao = "n30-timer-" + UUID.randomUUID();
    private URI origem;
    private long prazo;
    private final List<Map<String, Object>> observacoes = new ArrayList<>();

    @Test
    @Timeout(value = 240, unit = TimeUnit.SECONDS)
    void schedulerFisicoDoJarFechaUmaVezEProcessaTestemunhaCriadaAposEstabilidade() throws Exception {
        prazo = System.nanoTime() + Duration.ofMinutes(4).toNanos();
        assertThat(Runtime.getRuntime().maxMemory()).as("Runner deve limitar também o pai a192MiB").isLessThanOrEqualTo(192L * 1024 * 1024);
        assertThat(org.mockito.Mockito.mockingDetails(taskScheduler).isMock()).isTrue();
        Path jar = Path.of("target/sig-gtrat-backend-0.0.4-operacional-20261101.jar").toAbsolutePath();
        Map<String, Object> identidadeJar = conferirJar(jar);
        Map<String, Object> identidadeBanco = conferirBanco();
        Map<String, Object> backup = conferirReciboBackup();
        Cenario c = criarCenarioPersistido();
        declararFonteNominalSintetica(c, "N30-TIMER-FISICO-SINTETICO");
        LocalDate hoje = RelogioSistema.hoje();
        LocalDate d = hoje.minusDays(31).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate testemunhaData = d.minusDays(7);
        LocalDate recente = hoje.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        assertThat(testemunhaData).isAfterOrEqualTo(INICIO);
        assertThat(hoje).isBeforeOrEqualTo(FIM);
        assertThat(testemunhaData.plusDays(45)).isAfter(hoje);
        long alvo = seed(c, d), controle = seed(c, recente);
        Foto antes = foto(alvo), controleAntes = foto(controle);
        registrar("jar-seed", mapa("jar", identidadeJar, "banco", identidadeBanco, "backup", backup,
                "janelaPai", 45, "janelaFilho", "padrão30 sem override", "cronLaboratorio", CRON,
                "antes", antes, "controle", controleAntes, "testemunhaData", testemunhaData));
        conferirOriginal(antes, c.operacionalA.getId());
        conferirOriginal(controleAntes, c.operacionalA.getId());
        Process filho = null;
        Long testemunha = null;
        Foto fechada = null, testemunhaAntes = null, testemunhaDepois = null;
        boolean integraAlvo = false, integraTestemunha = false;
        try {
            filho = iniciarJar(jar);
            fechada = aguardarFechada(filho, alvo);
            registrar("jar-primeiro-fechamento", mapa("pidFilho", filho.pid(), "antes", antes, "depois", fechada));
            conferirFechada(antes, fechada);
            observarEstavel(filho, alvo, fechada, Duration.ofSeconds(4));
            assertThat(RelogioSistema.hoje()).as("Fixture não atravessa uma virada física durante a preparação").isEqualTo(hoje);
            AtomicReference<Foto> fonteTestemunhaCapturada = new AtomicReference<>();
            // O filho pode fechar a testemunha logo após o commit. Capturar a
            // fonte real já flushada antes desse commit elimina essa corrida,
            // sem simular DTO, decisão, autoridade, SQL ou persistência.
            doAnswer(i -> {
                Object resposta = i.callRealMethod();
                FrequenciaDTO pedido = i.getArgument(0);
                if (testemunhaData.equals(pedido.getDataReferencia()) && c.vinculoA.getId().equals(pedido.getVinculoContratualId())) {
                    em.flush();
                    fonteTestemunhaCapturada.set(foto(((FrequenciaDTO) resposta).getId()));
                }
                return resposta;
            }).when(AopTestUtils.<FrequenciaServico>getUltimateTargetObject(frequenciaPreparador)).salvarOuAtualizar(any(FrequenciaDTO.class));
            testemunha = seed(c, testemunhaData);
            testemunhaAntes = foto(testemunha);
            Foto fonteTestemunha = fonteTestemunhaCapturada.get();
            registrar("jar-testemunha-fonte", mapa("fonteFlushadaAntesDoCommit", fonteTestemunha, "primeiraLeituraDepoisCommit", testemunhaAntes));
            assertThat(fonteTestemunha).isNotNull(); conferirOriginal(fonteTestemunha, c.operacionalA.getId());
            testemunhaDepois = aguardarFechada(filho, testemunha);
            observarEstavel(filho, alvo, fechada, Duration.ofSeconds(4));
            observarEstavel(filho, testemunha, testemunhaDepois, Duration.ofSeconds(4));
            try (Sessao sessao = login(filho, c.localA)) {
                var pagina = sessao.enviar("GET", "/api/frequencias?data=" + d + "&contratoId=" + c.contratoA.getId(), null, 200);
                JsonNode alvoApi = null;
                for (var item : pagina.path("content")) if (item.path("id").asLong() == alvo) alvoApi = item;
                assertThat(alvoApi).isNotNull();
                LocalDateTime prazoApi = LocalDateTime.parse(alvoApi.path("prazoEdicao").asText());
                registrar("jar-janela-real", mapa("dataReferencia", d, "prazoEdicaoLidoNoTcp", prazoApi));
                assertThat(prazoApi).as("API do filho confirma N30, não só idade suficiente para fechar").isEqualTo(d.plusDays(30).atStartOfDay());
                var integridadeA = sessao.enviar("GET", "/api/frequencias/" + alvo + "/evidencias-integridade", null, 200);
                var integridadeT = sessao.enviar("GET", "/api/frequencias/" + testemunha + "/evidencias-integridade", null, 200);
                integraAlvo = integridadeA.path("cadeiaIntegra").asBoolean();
                integraTestemunha = integridadeT.path("cadeiaIntegra").asBoolean();
            }
            Foto estavel = foto(alvo), testemunhaEstavel = foto(testemunha), controleDepois = foto(controle);
            registrar("jar-final", mapa("jar", identidadeJar, "banco", identidadeBanco, "backup", backup,
                    "pidFilho", filho.pid(), "aplicacao", aplicacao, "cronLaboratorio", CRON,
                    "alvoAntes", antes, "alvoFechado", fechada, "alvoEstavel", estavel,
                    "testemunhaPrimeiraLeitura", testemunhaAntes, "testemunhaFechada", testemunhaDepois,
                    "testemunhaEstavel", testemunhaEstavel, "controleAntes", controleAntes, "controleDepois", controleDepois,
                    "integridadeAlvo", integraAlvo, "integridadeTestemunha", integraTestemunha, "observacoes", observacoes,
                    "limite", "Timer físico2s de laboratório; não cadência default1min nem meia-noite física. Sem contador de ticks. "
                            + "Segunda data é criada após estabilidade e fechada pelo mesmo filho. Hash SHA-256/formato real, não HMAC."));
            Foto alvoConfirmado = fechada, testemunhaConfirmada = testemunhaDepois;
            boolean integridadeAlvo = integraAlvo, integridadeTestemunha = integraTestemunha;
            assertAll(() -> conferirFechada(antes, alvoConfirmado),
                    () -> conferirFechada(fonteTestemunha, testemunhaConfirmada),
                    () -> assertThat(estavel).isEqualTo(alvoConfirmado),
                    () -> assertThat(testemunhaEstavel).isEqualTo(testemunhaConfirmada),
                    () -> assertThat(controleDepois).isEqualTo(controleAntes),
                    () -> assertThat(integridadeAlvo).isTrue(), () -> assertThat(integridadeTestemunha).isTrue(),
                    () -> assertThat(hashArquivo(jar)).isEqualTo(JAR_SHA));
        } finally {
            encerrar(filho);
            registrar("jar-encerramento", mapa("pidFilho", filho == null ? null : filho.pid(),
                    "filhoEncerrado", filho == null || !filho.isAlive(), "alvo", alvo, "testemunha", testemunha,
                    "ultimaFotoAlvo", foto(alvo), "controle", foto(controle), "observacoes", observacoes));
        }
    }

    private Process iniciarJar(Path jar) throws Exception {
        int porta;
        try (ServerSocket s = new ServerSocket(0, 0, InetAddress.getByName("127.0.0.1"))) { porta = s.getLocalPort(); }
        origem = URI.create("http://127.0.0.1:" + porta);
        Path diretorio = Files.createTempDirectory(Path.of("target").toAbsolutePath(), "n30-timer-",
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
        List<String> comando = List.of(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-Xmx192m", "-XX:MaxMetaspaceSize=192m", "-XX:ActiveProcessorCount=2", "-XX:+UseSerialGC", "-Duser.timezone=America/Sao_Paulo",
                "-jar", jar.toString(), "--env-file=./src/test/resources/auditoria-endpoints.env", "--server.address=127.0.0.1", "--server.port=" + porta,
                "--server.ssl.enabled=false", "--spring.main.banner-mode=off", "--spring.jpa.hibernate.ddl-auto=update", "--spring.sql.init.mode=never",
                "--spring.jpa.open-in-view=false", "--spring.jpa.show-sql=false", "--spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true",
                "--spring.datasource.hikari.maximum-pool-size=8", "--spring.datasource.hikari.minimum-idle=1", "--logging.level.root=WARN",
                "--logging.level.org.hibernate.SQL=OFF", "--logging.level.org.hibernate.orm.jdbc.bind=OFF", "--app.email.enabled=false",
                "--app.email.gmail.enabled=false", "--spring.cache.type=redis", "--app.seed.unidades-operacionais-enabled=false",
                "--app.seed.jornadas-operacionais-enabled=false", "--app.seed.usuarios-globais-enabled=false", "--app.seed.gestores-se-gestao-local-enabled=false",
                "--file.upload-dir=" + diretorio.resolve("uploads"), "--audit.fallback-log=" + diretorio.resolve("audit-fallback.jsonl"),
                "--app.relatorios.temp.diretorio=" + diretorio.resolve("relatorios"), "--frequencia.fechamento-negocial.cron=" + CRON,
                "--frequencia.fechamento-negocial.tamanho-lote=1");
        // Não herda overridesN45/JAVA_TOOL_OPTIONS/credenciais de outro destino.
        Path log = Files.createFile(diretorio.resolve("jar.log"),
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
        ProcessBuilder pb = new ProcessBuilder(comando).redirectErrorStream(true).redirectOutput(log.toFile());
        var env = pb.environment(); env.clear(); env.put("TZ", "America/Sao_Paulo");
        var banco = ConfiguracaoPostgresIntegracao.carregar();
        String url = banco.url() + (banco.url().contains("?") ? "&" : "?") + "ApplicationName=" + aplicacao;
        env.put("DB_URL", url); env.put("DB_USER", banco.usuario()); env.put("DB_PASS", banco.senha());
        env.put("SPRING_DATASOURCE_URL", url); env.put("SPRING_DATASOURCE_USERNAME", banco.usuario()); env.put("SPRING_DATASOURCE_PASSWORD", banco.senha());
        for (String nome : List.of("REDIS_HOST", "REDIS_PORT", "REDIS_PASSWORD", "REDIS_DATABASE")) {
            String valor = System.getenv(nome); assertThat(valor).as("Referência de Redis isolado obrigatória: %s", nome).isNotNull();
            env.put(nome, valor);
        }
        env.put("REDIS_HTTP_GET_ENABLED", "true");
        Process processo = pb.start();
        try (HttpClient cliente = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build()) {
            registrar("jar-processo", mapa("pidFilho", processo.pid(), "portaLoopback", porta, "aplicacao", aplicacao,
                    "jarSha256", JAR_SHA, "heapMiB", 192, "metaspaceMiB", 192, "cpu", 2, "pool", 8, "cron", CRON,
                    "stdout", "arquivo privado600; SQL/binds OFF; não publicar log bruto", "logFilho", log.toString()));
            long fim = Math.min(prazo, System.nanoTime() + Duration.ofSeconds(55).toNanos());
            while (System.nanoTime() < fim) {
                assertThat(processo.isAlive()).as("JAR deve continuar vivo até prontidão").isTrue();
                try {
                    var r = cliente.send(HttpRequest.newBuilder(origem.resolve("/api/autenticacao/csrf")).timeout(Duration.ofSeconds(1)).GET().build(), HttpResponse.BodyHandlers.discarding());
                    if (r.statusCode() == 200) return processo;
                } catch (java.io.IOException inicializando) { /* Socket ainda não disponível; não é aprovação. */ }
                Thread.sleep(100);
            }
            throw new AssertionError("JAR não ficou pronto em55s");
        } catch (Exception | AssertionError falha) { encerrar(processo); throw falha; }
    }

    private Foto aguardarFechada(Process filho, long id) throws Exception {
        long fim = Math.min(prazo, System.nanoTime() + Duration.ofSeconds(45).toNanos());
        Foto ultima;
        do {
            assertThat(filho.isAlive()).isTrue(); ultima = foto(id);
            if (ultima.estado().equals("FECHADA")) return ultima;
            Thread.sleep(100);
        } while (System.nanoTime() < fim);
        registrar("jar-fechamento-nao-observado", mapa("id", id, "ultimaFoto", ultima, "pidFilho", filho.pid()));
        throw new AssertionError("Materialização física não observada no prazo");
    }

    private void observarEstavel(Process filho, long id, Foto esperado, Duration duracao) throws Exception {
        Instant inicio = Instant.now(); long fim = Math.min(prazo, System.nanoTime() + duracao.toNanos());
        List<Foto> amostras = new ArrayList<>();
        do { assertThat(filho.isAlive()).isTrue(); amostras.add(foto(id)); Thread.sleep(200); } while (System.nanoTime() < fim);
        var pids = new JdbcTemplate(fonte).queryForList("select pid from pg_stat_activity where application_name=? and datname=current_database()", Long.class, aplicacao);
        observacoes.add(mapa("inicio", inicio, "fim", Instant.now(), "id", id, "amostras", amostras.size(), "backendsFilho", pids));
        registrar("jar-estabilidade", mapa("esperado", esperado, "amostras", amostras, "observacao", observacoes.get(observacoes.size()-1)));
        assertThat(amostras).allSatisfy(x -> assertThat(x).isEqualTo(esperado));
        assertThat(Duration.between(inicio, Instant.now())).isGreaterThanOrEqualTo(duracao);
        assertThat(pids).isNotEmpty();
    }

    private Map<String, Object> conferirJar(Path jar) throws Exception {
        assertThat(Files.isRegularFile(jar)).isTrue(); assertThat(hashArquivo(jar)).isEqualTo(JAR_SHA);
        int comparadas = 0; long noJar;
        Path classes = Path.of("target/classes");
        try (ZipFile zip = new ZipFile(jar.toFile()); var caminhos = Files.walk(classes)) {
            noJar = zip.stream().filter(e -> e.getName().startsWith("BOOT-INF/classes/") && e.getName().endsWith(".class")).count();
            for (Path p : caminhos.filter(x -> x.toString().endsWith(".class")).toList()) {
                String nome = classes.relativize(p).toString().replace('\\', '/'); var entrada = zip.getEntry("BOOT-INF/classes/" + nome);
                assertThat(entrada).isNotNull();
                try (var in = zip.getInputStream(entrada)) {
                    byte[] bytes = in.readAllBytes(); CRC32 crc = new CRC32(); crc.update(bytes);
                    assertThat(crc.getValue()).as("CRC da entrada empacotada").isEqualTo(entrada.getCrc());
                    assertThat(bytes).isEqualTo(Files.readAllBytes(p));
                }
                comparadas++;
            }
        }
        assertThat(comparadas).isEqualTo(823); assertThat(noJar).isEqualTo(823);
        return mapa("sha256", JAR_SHA, "classesComparadasByteAByteECrc", comparadas, "classesNoJar", noJar);
    }

    private Map<String, Object> conferirBanco() throws Exception {
        try (var c = fonte.getConnection()) {
            VerificadorAmbientePostgres186.verificar(c, "n30-timer-pai");
            var jdbc = new JdbcTemplate(fonte);
            long semOwner = jdbc.queryForObject("select count(*) from pg_class c join pg_namespace n on n.oid=c.relnamespace "
                    + "where n.nspname=current_schema() and c.relkind in ('r','p','S') and not pg_has_role(c.relowner,'USAGE')", Long.class);
            assertThat(semOwner).as("Ownership das relações JPA deve estar comprovado para a role limitada").isZero();
            String identidade = jdbc.queryForObject("select jsonb_build_array(current_database(),current_user,current_schema(),inet_server_addr(),inet_server_port())::text", String.class);
            return mapa("identidadeSha256", h(identidade), "serverVersionNum", 180006, "relacoesSemOwnership", semOwner,
                    "driver", c.getMetaData().getDriverVersion());
        }
    }

    private Map<String, Object> conferirReciboBackup() throws Exception {
        String nome = System.getenv("SGT_N30_BACKUP_RECEIPT");
        String esperado = System.getenv("SGT_N30_BACKUP_SHA256");
        assertThat(nome).as("Root deve fornecer recibo do backup/isolamento já conferido").isNotBlank();
        assertThat(esperado).matches("[a-f0-9]{64}");
        Path campanha = Path.of("..", ".homologacao", "20261109-sgt-final").toAbsolutePath().normalize();
        Path p = Path.of(nome).toAbsolutePath().normalize();
        assertThat(p.startsWith(campanha)).as("Recibo precisa estar dentro da campanha C").isTrue();
        assertThat(Files.isDirectory(campanha, LinkOption.NOFOLLOW_LINKS)).isTrue();
        assertThat(campanha.toRealPath()).isEqualTo(campanha);
        Path trecho = campanha;
        for (Path componente : campanha.relativize(p)) {
            trecho = trecho.resolve(componente); assertThat(Files.isSymbolicLink(trecho)).as("Sem symlink no caminho do recibo").isFalse();
        }
        assertThat(Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS)).isTrue();
        String hash = hashArquivo(p); assertThat(hash).isEqualTo(esperado);
        return mapa("reciboSha256", hash, "bytes", Files.size(p), "limite", "Vínculo ao recibo fornecido; não é ensaio de restauração do backup");
    }

    private String hashArquivo(Path p) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (var in = Files.newInputStream(p)) { byte[] b = new byte[65536]; int n; while ((n = in.read(b)) >= 0) digest.update(b, 0, n); }
        return HexFormat.of().formatHex(digest.digest());
    }

    private void encerrar(Process filho) throws InterruptedException {
        if (filho == null || !filho.isAlive()) return;
        boolean interrompido = Thread.interrupted();
        try {
            filho.destroy();
            try {
                if (!filho.waitFor(12, TimeUnit.SECONDS)) filho.destroyForcibly();
            } catch (InterruptedException limiteDoTeste) {
                interrompido = true; filho.destroyForcibly();
            }
            if (filho.isAlive()) {
                try { assertThat(filho.waitFor(5, TimeUnit.SECONDS)).as("Somente a JVM filha deste teste é encerrada").isTrue(); }
                catch (InterruptedException novaInterrupcao) { interrompido = true; filho.destroyForcibly(); }
            }
            assertThat(filho.isAlive()).as("Nenhuma JVM filha pode ficar órfã").isFalse();
        } finally { if (interrompido) Thread.currentThread().interrupt(); }
    }

    private Sessao login(Process filho, UsuarioAplicacao usuario) throws Exception {
        assertThat(filho.isAlive()).isTrue(); Sessao s = new Sessao();
        try {
            s.csrf(); var r = s.enviar("POST", "/api/autenticacao/autenticar", mapa("matricula", usuario.getMatricula(), "password", SENHA), 200);
            assertThat(r.path("username").asText()).isEqualTo(usuario.getUsername()); s.csrf(); return s;
        } catch (Exception | AssertionError falha) { s.close(); throw falha; }
    }

    private final class Sessao implements AutoCloseable {
        private final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ORIGINAL_SERVER);
        private final HttpClient cliente = HttpClient.newBuilder().cookieHandler(cookies).connectTimeout(Duration.ofSeconds(3)).build();
        private String csrf;
        private void csrf() throws Exception { csrf = enviar("GET", "/api/autenticacao/csrf", null, 200).path("token").asText(); assertThat(csrf).isNotBlank(); }
        private JsonNode enviar(String metodo, String caminho, Object corpo, int esperado) throws Exception {
            var b = HttpRequest.newBuilder(origem.resolve(caminho)).timeout(Duration.ofSeconds(15)).header("Accept", "application/json");
            if (!metodo.equals("GET")) b.header("X-XSRF-TOKEN", csrf).header("X-User-Password", SENHA);
            if (corpo != null) b.header("Content-Type", "application/json");
            var r = cliente.send(b.method(metodo, corpo == null ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(json(corpo), StandardCharsets.UTF_8)).build(), HttpResponse.BodyHandlers.ofByteArray());
            registrar("jar-http", mapa("metodo", metodo, "caminho", caminho, "status", r.statusCode(), "pidObservador", ProcessHandle.current().pid()));
            assertThat(r.statusCode()).isEqualTo(esperado); JsonNode j = mapper.readTree(r.body()); return j.hasNonNull("data") ? j.get("data") : j;
        }
        @Override public void close() { cliente.close(); }
    }
}
