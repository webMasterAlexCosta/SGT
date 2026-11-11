package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.configuracao.VerificadorAmbientePostgres186;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.infra.ConfiguracaoPostgresIntegracao;
import com.fasterxml.jackson.databind.JsonNode;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

/** Draft comum só de testes: nenhum hook, agente ou mock dentro do JAR filho. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
abstract class GapsBackend15JarSuporte extends ContratosHttpBase {
    static final String JAR_SHA = "dcb3a38443c0f941d70f95f10a3a9f80bb44ee1a6aa57f1c29006fc3bf726f7d";
    static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");
    static final Path PREPARADOR = privado("preparador-");
    @Autowired protected DataSource dataSource;
    @Autowired protected JdbcTemplate jdbc;
    protected Path pasta, jar;
    protected URI origem;
    protected String aplicacao;
    protected Process filho;
    protected Cenario c;
    protected Map<String, String> opcoes = Map.of();
    private final List<Path> logs = new ArrayList<>();

    @DynamicPropertySource
    static void isolarPreparador(DynamicPropertyRegistry r) {
        r.add("file.upload-dir", () -> PREPARADOR.resolve("uploads").toString());
        r.add("audit.fallback-log", () -> PREPARADOR.resolve("audit.jsonl").toString());
        r.add("app.relatorios.temp.diretorio", () -> PREPARADOR.resolve("relatorios").toString());
    }

    protected void preparar(String nome) throws Exception {
        assertTrue(Runtime.getRuntime().maxMemory() <= 192L * 1024 * 1024, "Pai deve ter heap máximo de 192 MiB");
        pasta = privado(nome + "-");
        jar = Path.of("target/sig-gtrat-backend-0.0.4-operacional-20261101.jar").toAbsolutePath();
        verificarJar();
        try (Connection banco = dataSource.getConnection()) { VerificadorAmbientePostgres186.verificar(banco, nome); }
        assertEquals(Boolean.TRUE, jdbc.queryForObject("SELECT pg_get_userbyid(datdba)=current_user FROM pg_database WHERE datname=current_database()", Boolean.class));
        assertEquals(0L, jdbc.queryForObject("SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace "
                + "WHERE n.nspname=current_schema() AND c.relkind IN ('r','p','S') AND pg_get_userbyid(c.relowner)<>current_user", Long.class));
        c = criarCenarioPersistido();
        declararFonteNominalSintetica(c, "GAPS15-" + nome);
        aplicacao = "gaps15-" + UUID.randomUUID().toString().substring(0, 12);
        int porta;
        try (ServerSocket reserva = new ServerSocket(0, 0, InetAddress.getByName("127.0.0.1"))) { porta = reserva.getLocalPort(); }
        origem = URI.create("http://127.0.0.1:" + porta);
        prova("preparacao", mapa("jarSha256", JAR_SHA, "classes", 823, "pastaPrivada", pasta.toString(),
                "origem", origem.toString(), "ator", c.localA.getId(), "controle", c.localB.getId(),
                "banco", identidadeBanco(), "redis", identidadeRedis()));
    }

    protected void iniciar(String fase) throws Exception {
        assertTrue(filho == null || !filho.isAlive());
        assertEquals(JAR_SHA, hashArquivo(jar));
        List<String> args = new ArrayList<>(List.of(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-Xmx192m", "-XX:MaxMetaspaceSize=192m", "-XX:ActiveProcessorCount=2", "-XX:+UseSerialGC", "-XX:TieredStopAtLevel=1",
                "-Duser.timezone=America/Sao_Paulo", "-jar", jar.toString(), "--env-file=./src/test/resources/auditoria-endpoints.env",
                "--server.address=127.0.0.1", "--server.port=" + origem.getPort(), "--server.ssl.enabled=false",
                "--spring.main.banner-mode=off", "--spring.jpa.hibernate.ddl-auto=update", "--spring.sql.init.mode=never",
                "--spring.jpa.open-in-view=false", "--spring.jpa.show-sql=false", "--spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true",
                "--spring.datasource.hikari.maximum-pool-size=4", "--spring.datasource.hikari.minimum-idle=1",
                "--logging.level.root=WARN", "--logging.level.org.hibernate.SQL=OFF", "--logging.level.org.hibernate.orm.jdbc.bind=OFF",
                "--app.email.enabled=false", "--app.email.gmail.enabled=false", "--spring.cache.type=redis",
                "--app.seed.unidades-operacionais-enabled=false", "--app.seed.jornadas-operacionais-enabled=false",
                "--app.seed.usuarios-globais-enabled=false", "--app.seed.gestores-se-gestao-local-enabled=false",
                "--file.upload-dir=" + pasta.resolve("uploads"), "--audit.fallback-log=" + pasta.resolve("audit-fallback.jsonl"),
                "--app.relatorios.temp.diretorio=" + pasta.resolve("relatorios"),
                "--auditlog.flush.delay-ms=1000"));
        new TreeMap<>(opcoes).forEach((k, v) -> args.add("--" + k + "=" + v));
        Path log = Files.createFile(pasta.resolve(fase + ".log"), PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
        logs.add(log);
        ProcessBuilder pb = new ProcessBuilder(args).redirectErrorStream(true).redirectOutput(log.toFile());
        Map<String, String> env = pb.environment(); env.clear(); env.put("TZ", "America/Sao_Paulo");
        var banco = ConfiguracaoPostgresIntegracao.carregar();
        String url = banco.url() + (banco.url().contains("?") ? "&" : "?") + "ApplicationName=" + aplicacao;
        env.put("DB_URL", url); env.put("DB_USER", banco.usuario()); env.put("DB_PASS", banco.senha());
        env.put("SPRING_DATASOURCE_URL", url); env.put("SPRING_DATASOURCE_USERNAME", banco.usuario()); env.put("SPRING_DATASOURCE_PASSWORD", banco.senha());
        for (String chave : List.of("REDIS_HOST", "REDIS_PORT", "REDIS_PASSWORD", "REDIS_DATABASE")) {
            String valor = System.getenv(chave); assertNotNull(valor, chave); assertFalse(valor.isBlank()); env.put(chave, valor);
        }
        env.put("REDIS_HTTP_GET_ENABLED", "true");
        filho = pb.start();
        prova("processo-" + fase, mapa("pid", filho.pid(), "jarSha256", JAR_SHA, "origem", origem.toString(),
                "applicationName", aplicacao, "heapMiB", 192, "metaspaceMiB", 192, "cpu", 2, "serialGC", true,
                "pool", 4, "jitTierMaximo", 1, "opcoesLaboratorio", opcoes, "auditFlushMs", 1000, "logPrivado", log.toString()));
        try (HttpClient h = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build()) {
            long fim = System.nanoTime() + TimeUnit.SECONDS.toNanos(55);
            do {
                assertTrue(filho.isAlive(), "JAR encerrou durante boot; consultar log privado");
                try {
                    var r = h.send(HttpRequest.newBuilder(origem.resolve("/api/autenticacao/csrf")).timeout(Duration.ofSeconds(1)).GET().build(),
                            HttpResponse.BodyHandlers.discarding());
                    if (r.statusCode() == 200) return; // Único readiness: CSRF anônimo.
                } catch (IOException iniciando) { /* Somente socket ainda indisponível durante boot. */ }
                Thread.sleep(100);
            } while (System.nanoTime() < fim);
            fail("Tomcat não ficou pronto em 55 s");
        } catch (Exception | AssertionError erro) { encerrar(); throw erro; }
    }

    protected void encerrar() throws Exception {
        if (filho == null || !filho.isAlive()) return;
        boolean interrompido = Thread.interrupted();
        try {
            filho.destroy();
            try {
                if (!filho.waitFor(15, TimeUnit.SECONDS)) {
                    filho.destroyForcibly(); assertTrue(filho.waitFor(8, TimeUnit.SECONDS));
                    fail("Parada graciosa do filho excedeu 15 s; encerramento forçado não aprova reinício gracioso");
                }
            } catch (InterruptedException e) { interrompido = true; filho.destroyForcibly(); filho.waitFor(8, TimeUnit.SECONDS); throw e; }
            assertFalse(filho.isAlive());
        } finally {
            prova("encerramento", mapa("pid", filho.pid(), "vivo", filho.isAlive(), "exitCode", filho.isAlive() ? null : filho.exitValue()));
            if (interrompido) Thread.currentThread().interrupt();
        }
    }

    protected Sessao login(UsuarioAplicacao ator) throws Exception {
        Sessao s = new Sessao();
        try {
            s.csrf();
            JsonNode r = s.json("POST", "/api/autenticacao/autenticar", mapa("matricula", ator.getMatricula(), "password", SENHA), 200);
            assertEquals(ator.getUsername(), r.path("username").asText()); assertEquals(ator.getPerfil().name(), r.path("perfil").asText());
            assertTrue(s.cookies.getCookieStore().getCookies().stream().anyMatch(k -> k.getName().equals("autenticacao")));
            s.csrf(); return s;
        } catch (Exception | AssertionError erro) { s.close(); throw erro; }
    }

    protected final class Sessao implements AutoCloseable {
        final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ORIGINAL_SERVER);
        final HttpClient http = HttpClient.newBuilder().cookieHandler(cookies).connectTimeout(Duration.ofSeconds(3))
                .followRedirects(HttpClient.Redirect.NEVER).version(HttpClient.Version.HTTP_1_1).build();
        String csrf;
        void csrf() throws Exception { JsonNode r = get("/api/autenticacao/csrf", 200); csrf = r.path("token").asText(); assertFalse(csrf.isBlank()); }
        Sessao copiar() {
            Sessao copia = new Sessao(); copia.csrf = csrf;
            for (HttpCookie k : cookies.getCookieStore().getCookies()) copia.cookies.getCookieStore().add(origem, (HttpCookie) k.clone());
            return copia;
        }
        String cookieHeader() {
            return cookies.getCookieStore().getCookies().stream().filter(k -> !k.hasExpired())
                    .map(k -> k.getName() + "=" + k.getValue()).collect(java.util.stream.Collectors.joining("; "));
        }
        JsonNode get(String alvo, int status) throws Exception { return dados(pedido("GET", alvo, null, null, status)); }
        JsonNode json(String metodo, String alvo, Object corpo, int status) throws Exception {
            return dados(pedido(metodo, alvo, corpo == null ? null : GapsBackend15JarSuporte.this.json(corpo).getBytes(StandardCharsets.UTF_8), "application/json", status));
        }
        HttpResponse<byte[]> pedido(String metodo, String alvo, byte[] corpo, String tipo, int esperado) throws Exception {
            var b = HttpRequest.newBuilder(origem.resolve(alvo)).timeout(Duration.ofSeconds(20)).header("Accept", "*/*").header("Cache-Control", "no-cache");
            if (tipo != null) b.header("Content-Type", tipo);
            if (!metodo.equals("GET")) { assertNotNull(csrf); b.header("X-XSRF-TOKEN", csrf).header("X-User-Password", SENHA); }
            var r = http.send(b.method(metodo, corpo == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofByteArray(corpo)).build(),
                    HttpResponse.BodyHandlers.ofByteArray());
            prova("http", mapa("metodo", metodo, "alvo", alvo, "status", r.statusCode(), "esperado", esperado, "bytes", r.body().length,
                    "bodySha256", hash(r.body())));
            if (esperado >= 0) assertEquals(esperado, r.statusCode(), metodo + " " + alvo);
            return r;
        }
        JsonNode multipart(String alvo, Map<String, Object> metadados, String nome, String tipo, byte[] bytes, boolean documento) throws Exception {
            String limite = "sgt-gap15-" + UUID.randomUUID();
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            if (documento) {
                b.write(("--" + limite + "\r\nContent-Disposition: form-data; name=\"metadados\"; filename=\"metadados.json\"\r\nContent-Type: application/json\r\n\r\n"
                        + GapsBackend15JarSuporte.this.json(metadados) + "\r\n").getBytes(StandardCharsets.UTF_8));
            } else for (var e : metadados.entrySet()) {
                b.write(("--" + limite + "\r\nContent-Disposition: form-data; name=\"" + e.getKey() + "\"\r\n\r\n" + e.getValue() + "\r\n").getBytes(StandardCharsets.UTF_8));
            }
            b.write(("--" + limite + "\r\nContent-Disposition: form-data; name=\"" + (documento ? "arquivo" : "anexos")
                    + "\"; filename=\"" + nome + "\"\r\nContent-Type: " + tipo + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            b.write(bytes); b.write(("\r\n--" + limite + "--\r\n").getBytes(StandardCharsets.UTF_8));
            return dados(pedido("POST", alvo, b.toByteArray(), "multipart/form-data; boundary=" + limite, documento ? 200 : 201));
        }
        private JsonNode dados(HttpResponse<byte[]> r) throws Exception {
            if (r.body().length == 0) return mapper.nullNode();
            JsonNode n = mapper.readTree(r.body()); return n.hasNonNull("data") ? n.get("data") : n;
        }
        @Override public void close() { http.close(); }
    }

    protected long publicar(Sessao gestor) throws Exception {
        LocalDate hoje = LocalDate.now(ZONA);
        long id = gestor.json("POST", "/api/contratos", rascunho(c, "GAP15-" + UUID.randomUUID().toString().substring(0, 8),
                3, hoje.minusDays(40), hoje.plusDays(120)), 201).path("id").asLong();
        gestor.json("POST", base(id) + "/composicoes", composicao(c, c.cargoA.getId(), 3), 201);
        var pedido = mapa("dataPublicacao", hoje, "referenciaSei", "SEI-SINTETICO-GAP15", "justificativa", "Publicação sintética do corpus de reinício");
        JsonNode previa = gestor.json("POST", base(id) + "/previas/publicar", pedido, 200);
        pedido.put("tokenPrevia", previa.path("tokenPrevia").asText()); pedido.put("confirmarImpacto", true);
        gestor.json("POST", base(id) + "/publicar", pedido, 200); return id;
    }

    protected byte[] pdf(String texto) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(); Document d = new Document();
        PdfWriter.getInstance(d, bytes); try { d.open(); d.add(new Paragraph(texto)); } finally { d.close(); }
        return bytes.toByteArray();
    }

    /** Todas as linhas/colunas das tabelas públicas do banco exclusivo; nenhum valor pessoal/token em claro. */
    protected record Banco(Map<String, List<String>> linhas, Map<String, Object> catalogo) {}
    protected Banco bancoCompleto() throws Exception {
        try (Connection b = dataSource.getConnection()) {
            b.setReadOnly(true); b.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ); b.setAutoCommit(false);
            try {
                List<String> tabelas = new ArrayList<>();
                try (var s = b.createStatement(); var r = s.executeQuery("SELECT c.relname FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace "
                        + "WHERE n.nspname=current_schema() AND c.relkind IN ('r','p') ORDER BY c.relname")) {
                    while (r.next()) tabelas.add(r.getString(1));
                }
                assertTrue(tabelas.containsAll(List.of("contrato", "token_ativo", "fechamento_frequencia", "documento_comprobatorio")));
                Map<String, List<String>> linhas = new TreeMap<>();
                for (String tabela : tabelas) {
                    List<String> hashes = new ArrayList<>();
                    try (var s = b.createStatement(); var r = s.executeQuery("SELECT to_jsonb(t)::text FROM " + citar(tabela) + " t ORDER BY to_jsonb(t)::text COLLATE \"C\"")) {
                        while (r.next()) hashes.add(SnapshotQuantitativos.hash(r.getString(1)));
                    }
                    linhas.put(tabela, List.copyOf(hashes));
                }
                Map<String, Object> catalogo = new TreeMap<>();
                catalogo.put("schema-e-tabelas", lerHashes(b, """
                    SELECT n.nspname,pg_get_userbyid(n.nspowner),n.nspacl::text,c.relname,c.relkind,c.relpersistence,
                      pg_get_userbyid(c.relowner),c.relrowsecurity,c.relforcerowsecurity,c.reloptions::text
                    FROM pg_namespace n JOIN pg_class c ON c.relnamespace=n.oid
                    WHERE n.nspname=current_schema() AND c.relkind IN ('r','p') ORDER BY c.relname
                    """));
                catalogo.put("colunas", lerHashes(b, """
                    SELECT n.nspname,c.relname,a.attnum,a.attname,format_type(a.atttypid,a.atttypmod),a.attnotnull,a.attidentity,
                      pg_get_expr(d.adbin,d.adrelid) FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
                      JOIN pg_attribute a ON a.attrelid=c.oid LEFT JOIN pg_attrdef d ON d.adrelid=c.oid AND d.adnum=a.attnum
                    WHERE n.nspname=current_schema() AND c.relkind IN ('r','p') AND a.attnum>0 AND NOT a.attisdropped ORDER BY c.relname,a.attnum
                    """));
                catalogo.put("indices", lerHashes(b, """
                    SELECT c.relname,i.relname,x.indisprimary,x.indisunique,x.indisvalid,x.indisready,pg_get_indexdef(i.oid)
                    FROM pg_index x JOIN pg_class c ON c.oid=x.indrelid JOIN pg_class i ON i.oid=x.indexrelid
                      JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname=current_schema() ORDER BY c.relname,i.relname
                    """));
                catalogo.put("constraints-incluindo-fks", lerHashes(b, """
                    SELECT c.relname,x.conname,x.contype,x.condeferrable,x.condeferred,x.convalidated,pg_get_constraintdef(x.oid)
                    FROM pg_constraint x JOIN pg_class c ON c.oid=x.conrelid JOIN pg_namespace n ON n.oid=c.relnamespace
                    WHERE n.nspname=current_schema() ORDER BY c.relname,x.conname
                    """));
                catalogo.put("configuracao-sequencias", lerHashes(b, """
                    SELECT c.relname,format_type(s.seqtypid,NULL),s.seqstart,s.seqincrement,s.seqmax,s.seqmin,s.seqcache,s.seqcycle
                    FROM pg_sequence s JOIN pg_class c ON c.oid=s.seqrelid JOIN pg_namespace n ON n.oid=c.relnamespace
                    WHERE n.nspname=current_schema() ORDER BY c.relname
                    """));
                Map<String, Object> sequencias = new TreeMap<>();
                try (var s = b.createStatement(); var r = s.executeQuery("SELECT c.relname FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace "
                        + "WHERE n.nspname=current_schema() AND c.relkind='S' ORDER BY c.relname")) {
                    while (r.next()) {
                        String nome = r.getString(1); sequencias.put(nome, lerHashes(b, "SELECT last_value,is_called FROM " + citar(nome)));
                    }
                }
                catalogo.put("sequencias", sequencias); b.commit();
                return new Banco(Map.copyOf(linhas), Map.copyOf(catalogo));
            } catch (Exception | Error erro) { b.rollback(); throw erro; }
        }
    }

    private List<String> lerHashes(Connection b, String sql) throws Exception {
        List<String> resultado = new ArrayList<>();
        try (var s = b.createStatement(); var r = s.executeQuery(sql)) {
            while (r.next()) {
                List<String> valores = new ArrayList<>();
                for (int i = 1; i <= r.getMetaData().getColumnCount(); i++) valores.add(r.getString(i));
                resultado.add(SnapshotQuantitativos.hashJson(mapper, valores));
            }
        }
        return List.copyOf(resultado);
    }
    private String citar(String nome) { return "\"" + nome.replace("\"", "\"\"") + "\""; }

    protected Map<String, String> arquivos() throws Exception {
        Map<String, String> arquivos = new TreeMap<>();
        for (String tipo : List.of("uploads", "relatorios")) {
            Path raiz = pasta.resolve(tipo);
            if (Files.exists(raiz)) try (var itens = Files.walk(raiz)) {
                for (Path p : itens.filter(Files::isRegularFile).sorted().toList()) {
                    assertFalse(Files.isSymbolicLink(p)); arquivos.put(tipo + "/" + raiz.relativize(p), hashArquivo(p));
                }
            }
        }
        return Map.copyOf(arquivos);
    }

    protected Banco esperarQuietude() throws Exception {
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(25);
        Banco anterior = bancoCompleto(); long desde = System.nanoTime();
        do {
            Thread.sleep(500);
            Banco atual = bancoCompleto();
            if (!atual.equals(anterior)) { anterior = atual; desde = System.nanoTime(); }
            else if (System.nanoTime() - desde >= TimeUnit.SECONDS.toNanos(4)) return atual;
        } while (System.nanoTime() < limite);
        throw new AssertionError("Corpus não estabilizou por 4 s antes do snapshot integral");
    }

    protected Map<String, Object> identidadeBanco() {
        return jdbc.queryForMap("SELECT current_database() AS banco,inet_server_addr()::text AS endereco,inet_server_port() AS porta,"
                + "pg_postmaster_start_time()::text AS postmaster_inicio,current_setting('server_version_num') AS versao");
    }

    protected Map<String, String> identidadeRedis() throws Exception {
        String host = System.getenv("REDIS_HOST"); int porta = Integer.parseInt(System.getenv("REDIS_PORT"));
        assertTrue(Set.of("127.0.0.1", "localhost", "::1").contains(host));
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, porta), 2000); socket.setSoTimeout(3000);
            OutputStream out = socket.getOutputStream(); InputStream in = socket.getInputStream();
            resp(out, "AUTH", System.getenv("REDIS_PASSWORD")); assertEquals("+OK", linha(in));
            resp(out, "SELECT", System.getenv("REDIS_DATABASE")); assertEquals("+OK", linha(in));
            resp(out, "INFO", "server"); String cabecalho = linha(in); assertTrue(cabecalho.startsWith("$"));
            byte[] bytes = in.readNBytes(Integer.parseInt(cabecalho.substring(1)));
            Map<String, String> campos = new HashMap<>();
            for (String l : new String(bytes, StandardCharsets.UTF_8).split("\\r?\\n")) {
                int p = l.indexOf(':'); if (p > 0) campos.put(l.substring(0, p), l.substring(p + 1));
            }
            Map<String, String> id = new TreeMap<>();
            for (String k : List.of("process_id", "run_id", "redis_version")) { assertNotNull(campos.get(k)); id.put(k, campos.get(k)); }
            id.put("database", System.getenv("REDIS_DATABASE"));
            return Map.copyOf(id);
        }
    }
    private void resp(OutputStream out, String... partes) throws Exception {
        out.write(("*" + partes.length + "\r\n").getBytes(StandardCharsets.US_ASCII));
        for (String p : partes) { byte[] b = p.getBytes(StandardCharsets.UTF_8); out.write(("$" + b.length + "\r\n").getBytes(StandardCharsets.US_ASCII)); out.write(b); out.write("\r\n".getBytes(StandardCharsets.US_ASCII)); }
        out.flush();
    }
    protected static String linha(InputStream in) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        for (int x; (x = in.read()) != -1;) {
            if (x == '\n') return b.toString(StandardCharsets.ISO_8859_1).replaceFirst("\\r$", "");
            if (b.size() > 32768) throw new IOException("Linha HTTP/RESP excedeu limite");
            b.write(x);
        }
        throw new EOFException("Resposta incompleta");
    }

    protected void verificarJar() throws Exception {
        assertEquals(JAR_SHA, hashArquivo(jar));
        int classes = 0, crcs = 0;
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            var entradas = zip.entries();
            while (entradas.hasMoreElements()) {
                var entrada = entradas.nextElement(); if (entrada.isDirectory()) continue;
                CRC32 crc = new CRC32();
                try (InputStream in = zip.getInputStream(entrada)) { byte[] b = new byte[65536]; for (int n; (n = in.read(b)) >= 0;) if (n > 0) crc.update(b, 0, n); }
                assertEquals(entrada.getCrc(), crc.getValue()); crcs++;
                if (entrada.getName().startsWith("BOOT-INF/classes/") && entrada.getName().endsWith(".class")) {
                    Path corrente = Path.of("target/classes").resolve(entrada.getName().substring("BOOT-INF/classes/".length()));
                    assertTrue(Files.isRegularFile(corrente));
                    try (InputStream in = zip.getInputStream(entrada)) { assertArrayEquals(Files.readAllBytes(corrente), in.readAllBytes()); }
                    classes++;
                }
            }
        }
        try (var arquivos = Files.walk(Path.of("target/classes"))) { assertEquals(823, arquivos.filter(p -> p.toString().endsWith(".class")).count()); }
        assertEquals(823, classes); assertTrue(crcs > classes);
    }

    protected void semErrosOtimistas() throws Exception {
        for (Path log : logs) {
            String texto = Files.readString(log);
            for (String nome : List.of("ObjectOptimisticLockingFailureException", "OptimisticLockException", "StaleObjectStateException"))
                assertFalse(texto.contains(nome), "Log privado contém conflito otimista: " + nome);
        }
    }

    protected void prova(String caso, Object dados) throws Exception {
        SnapshotQuantitativos.registrar(mapper, "gaps-backend15", caso, mapa("camadaExecutada", "HTTP-TCP-TOMCAT-JAR",
                "notaEnvelope", "O rótulo genérico MockMvc do utilitário não descreve estas chamadas TCP reais.",
                "dados", dados));
    }
    protected static Path privado(String prefixo) {
        try {
            Path raiz = Path.of("target/gaps-backend15").toAbsolutePath(); Files.createDirectories(raiz);
            return Files.createTempDirectory(raiz, prefixo, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
        } catch (IOException e) { throw new IllegalStateException("Diretório privado do teste indisponível", e); }
    }
    protected static String hash(byte[] bytes) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
    protected static String hashArquivo(Path p) throws Exception {
        MessageDigest h = MessageDigest.getInstance("SHA-256");
        try (InputStream in = Files.newInputStream(p)) { byte[] b = new byte[65536]; for (int n; (n = in.read(b)) >= 0;) if (n > 0) h.update(b, 0, n); }
        return HexFormat.of().formatHex(h.digest());
    }
    protected final class Trava implements AutoCloseable {
        final EntityManager e = em.getEntityManagerFactory().createEntityManager();
        final long pid;
        Trava(Class<?> tipo, long id) {
            try {
                e.getTransaction().begin(); assertNotNull(e.find(tipo, id, LockModeType.PESSIMISTIC_WRITE));
                pid = ((Number) e.createNativeQuery("select pg_backend_pid()").getSingleResult()).longValue();
            } catch (RuntimeException | Error erro) { close(); throw erro; }
        }
        @Override public void close() {
            if (!e.isOpen()) return;
            try { if (e.getTransaction().isActive()) e.getTransaction().rollback(); } finally { e.close(); }
        }
    }
}
