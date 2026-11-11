package com.example.siggtrat.auditoria20261029;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.InputStream;
import java.net.*;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/** Executável opt-in de teste: não inicia Spring, JUnit ou Maven no processo observador. */
public final class RecuperacaoJarSegundaFase {
    static final String IDENTIDADE_SQL = """
        WITH RECURSIVE associadas(oid) AS (
          SELECT oid FROM pg_roles WHERE rolname=current_user
          UNION SELECT m.roleid FROM pg_auth_members m JOIN associadas a ON m.member=a.oid
        )
        SELECT jsonb_build_object(
          'versionNum',current_setting('server_version_num')::integer,
          'database',current_database(),'role',current_user,'sessionRole',session_user,
          'address',inet_server_addr()::text,'port',inet_server_port(),
          'schema',current_schema(),'timezone',current_setting('TimeZone'),
          'databaseOwnedByRole',d.datdba=r.oid,
          'schemaOwnedByRole',n.nspowner=r.oid OR
             (pg_get_userbyid(n.nspowner)='pg_database_owner' AND d.datdba=r.oid),
          'schemaUsage',has_schema_privilege(current_user,'public','USAGE'),
          'schemaCreate',has_schema_privilege(current_user,'public','CREATE'),
          'roleLimited',NOT(r.rolsuper OR r.rolcreatedb OR r.rolcreaterole OR r.rolreplication OR r.rolbypassrls),
          'noMemberships',NOT EXISTS(SELECT 1 FROM pg_auth_members m WHERE m.member=r.oid),
          'noAdministrativeMembership',NOT EXISTS(
             SELECT 1 FROM associadas a JOIN pg_roles x ON x.oid=a.oid
             WHERE x.rolsuper OR x.rolcreatedb OR x.rolcreaterole OR x.rolreplication OR x.rolbypassrls
               OR x.rolname IN ('pg_checkpoint','pg_create_subscription','pg_maintain','pg_monitor',
                  'pg_read_all_settings','pg_read_all_stats','pg_stat_scan_tables','pg_read_all_data',
                  'pg_write_all_data','pg_read_server_files','pg_write_server_files','pg_execute_server_program',
                  'pg_signal_autovacuum_worker','pg_signal_backend','pg_use_reserved_connections'))
        )::text FROM pg_roles r JOIN pg_database d ON d.datname=current_database()
          JOIN pg_namespace n ON n.nspname='public' WHERE r.rolname=current_user
        """;

    // A mesma expressão PostgreSQL é usada pelo preparador e pelo observador JDBC, sem conversão de timestamps.
    static final String SNAPSHOT_SQL = """
        WITH p AS (SELECT ?::bigint AS job,?::date AS data,?::bigint AS lotacao,
                          ?::bigint AS jornada,?::bigint AS contrato,?::bigint AS criador),
        fontes AS (SELECT f.* FROM frequencia f,p WHERE f.data_referencia=p.data
          AND f.lotacao_snapshot_id=p.lotacao AND f.jornada_snapshot_id=p.jornada AND f.contrato_snapshot_id=p.contrato)
        SELECT jsonb_build_object(
          'job',(SELECT to_jsonb(j) FROM fechamento_frequencia j,p WHERE j.id=p.job),
          'frequencias',COALESCE((SELECT jsonb_agg(to_jsonb(f) ORDER BY f.id) FROM fontes f),'[]'::jsonb),
          'eventos',COALESCE((SELECT jsonb_agg(to_jsonb(e) ORDER BY e.id_evento_frequencia)
             FROM evento_frequencia e JOIN fontes f ON f.id=e.id_frequencia),'[]'::jsonb),
          'historicos',COALESCE((SELECT jsonb_agg(to_jsonb(h) ORDER BY h.id_historico)
             FROM historico_empregado h,p WHERE h.id_empregado IN
              (SELECT e.id_empregado FROM empregado e JOIN vinculo_contratual v
                 ON v.id_pessoa=e.id_pessoa_canonica WHERE v.id_contrato=p.contrato)),'[]'::jsonb),
          'auditoriaFrequenciaSave',COALESCE((SELECT jsonb_agg(to_jsonb(a) ORDER BY a.id)
             FROM registro_auditoria a,p WHERE a.user_id=p.criador
               AND a.action='FREQUENCIA_SAVE' AND a.entity_type='Frequencia'),'[]'::jsonb)
        )::text
        """;
    private static final String API = "/api/frequencias/fechamentos";
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");
    private final JsonNode handoff;
    private final Path saida;
    private final Config config;
    private final List<Filho> filhos = new ArrayList<>();
    private final Map<String, Boolean> checks = new LinkedHashMap<>();
    private JsonNode ultimoSnapshot;
    private long duracaoRetomadaMs;
    private String handoffHash;

    private RecuperacaoJarSegundaFase(Path handoffPath, Path saida) throws Exception {
        this.saida = saida;
        Files.createDirectories(saida, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
        exigir(!Files.exists(saida.resolve("resultado-segunda-fase.json")), "Saída anterior deve ser preservada");
        byte[] bytes = Files.readAllBytes(handoffPath);
        handoffHash = sha(bytes);
        JsonNode selo = JSON.readTree(Files.readAllBytes(handoffPath.resolveSibling("handoff-recuperacao-fase2.sha256.json")));
        exigir(handoffHash.equals(selo.path("sha256").asText()), "Hash do handoff divergiu");
        handoff = JSON.readTree(bytes);
        exigir(handoff.path("schemaVersion").asInt() == 1
                && handoff.path("status").asText().equals("PREPARATION_ONLY_NOT_ACCEPTANCE"), "Handoff não é uma preparação explícita");
        config = Config.ambiente();
    }

    public static void main(String[] argumentos) throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone(ZONA));
        exigir(argumentos.length == 2 && Path.of(argumentos[0]).isAbsolute() && Path.of(argumentos[1]).isAbsolute(),
                "Informe caminhos absolutos de handoff e saída");
        RecuperacaoJarSegundaFase fase = new RecuperacaoJarSegundaFase(Path.of(argumentos[0]), Path.of(argumentos[1]));
        fase.executar();
    }

    private void executar() throws Exception {
        Throwable falha = null;
        boolean concluido = false;
        try (Connection observador = config.conectar()) {
            conferirHandoffEIdentidade(observador);
            String inicial = snapshot(observador);
            exigir(sha(inicial.getBytes(StandardCharsets.UTF_8)).equals(handoff.path("snapshotAposMorteSha256").asText()),
                    "Estado PostgreSQL mudou entre as fases");
            exigir(JSON.readTree(inicial).equals(handoff.path("snapshotAposMorte")), "Snapshot canônico do handoff divergiu");
            checks.put("handoffEEstadoInicial", true);
            gravar("estado-inicial-canonico.json", JSON.readTree(inicial));
            Long outros = escalar(observador, "SELECT count(*) FROM pg_stat_activity WHERE datname=current_database() "
                    + "AND usename=current_user AND backend_type='client backend' AND pid<>pg_backend_pid()");
            exigir(outros == 0, "Há outra conexão da aplicação antes da fase dois; verificar preparador e servidores restantes");
            checks.put("semConexoesDoPreparadorAntesDosFilhos", true);
            Filho b;
            Filho c;
            try (Connection barreira = config.conectar()) {
                barreira.setAutoCommit(false);
                try (PreparedStatement lock = barreira.prepareStatement(
                        "SELECT id,status,tentativa FROM fechamento_frequencia WHERE id=? FOR UPDATE")) {
                    lock.setLong(1, jobId());
                    try (ResultSet r = lock.executeQuery()) {
                        exigir(r.next() && r.getLong(1) == jobId() && r.getString(2).equals("PROCESSANDO") && r.getInt(3) == 2,
                                "Barreira precisa reter o job e a tentativa preparados");
                    }
                }
                exigir(escalar(barreira, "SELECT count(*) FROM fechamento_frequencia WHERE status IN ('PENDENTE','PROCESSANDO')") == 1,
                        "O banco deve conter somente o job ativo conhecido");
                long pidBarreira = escalar(barreira, "SELECT pg_backend_pid()");
                b = iniciar("recuperador-b");
                c = iniciar("recuperador-c");
                saudeDosDois();
                List<Map<String, Object>> disputa = esperarDisputa(observador, pidBarreira, Set.of(b.aplicacao(), c.aplicacao()));
                gravar("disputa-fisica-job-conhecido.json", mapa("jobId", jobId(), "tentativa", 2,
                        "pidBloqueador", pidBarreira, "executores", disputa));
                checks.put("doisExecutoresEmDisputaFisica", true);
                barreira.rollback();
            }
            long inicio = System.nanoTime();
            long limite = inicio + Duration.ofSeconds(120).toNanos();
            JsonNode terminal;
            // Novo login invalida o token anterior do usuário. As duas origens
            // observam a mesma sessão real, como um cliente atrás de dois nós.
            try (Sessao sb = login(b); Sessao sc = new Sessao(c.origem(), sb.cookies)) {
                long anterior = -1;
                do {
                    saudeDosDois();
                    terminal = sb.pedido("GET", API + "/" + jobId(), null, 200);
                    exigir(terminal.path("id").asLong() == jobId() && terminal.path("tentativa").asInt() == 2,
                            "Recuperação não pode mudar o ID ou tentativa");
                    long atual = terminal.path("totalProcessado").asLong();
                    exigir(atual >= anterior, "Progresso regrediu dentro da tentativa"); anterior = atual;
                    exigir(System.nanoTime() <= limite, "Recuperação excedeu o prazo de 120 segundos");
                    String estado = terminal.path("status").asText();
                    exigir(!estado.equals("ERRO") && !estado.equals("CANCELADO"), "Recuperação atingiu terminal incompatível");
                    if (estado.equals("CONCLUIDO")) break;
                    Thread.sleep(150);
                } while (true);
                duracaoRetomadaMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicio);
                saudeDosDois();
                JsonNode vistaC = sc.pedido("GET", API + "/" + jobId(), null, 200);
                exigir(vistaC.path("status").asText().equals("CONCLUIDO") && vistaC.path("tentativa").asInt() == 2,
                        "O segundo servidor não confirmou o mesmo terminal");
                String finalTexto = snapshot(observador);
                JsonNode finalConfirmado = JSON.readTree(finalTexto);
                conferirTerminal(finalConfirmado);
                checks.put("retomadaAutomaticaSemPost", true);
                checks.put("mesmoIdTentativaEPrefixo", true);
                checks.put("duzentasFrequenciasEEventosUnicos", true);
                checks.put("contadoresPorTentativa", true);
                gravar("terminal-canonico.json", finalConfirmado);
                long observarAte = System.nanoTime() + Duration.ofSeconds(3).toNanos();
                do {
                    saudeDosDois();
                    JsonNode visto = sc.pedido("GET", API + "/" + jobId(), null, 200);
                    exigir(visto.path("status").asText().equals("CONCLUIDO") && visto.path("tentativa").asInt() == 2,
                            "Terminal mudou durante a observação adicional");
                    Thread.sleep(150);
                } while (System.nanoTime() < observarAte);
                saudeDosDois();
                String estavel = snapshot(observador);
                exigir(finalTexto.equals(estavel), "Dois schedulers regravaram o terminal ou suas provas");
                checks.put("doisServidoresSaudaveisAteTerminalMaisTresSegundos", true);
                checks.put("terminalCanonicoEstavel", true);
                gravar("terminal-estavel-canonico.json", JSON.readTree(estavel));
            }
            exigir(hashArquivo(Path.of(handoff.path("jar").asText())).equals(handoff.path("jarSha256").asText()), "JAR mudou durante a fase dois");
            checks.put("mesmosBytesJar", true);
            concluido = true;
        } catch (Throwable erro) {
            falha = erro;
        } finally {
            boolean interrompido = Thread.interrupted();
            for (Filho filho : filhos) {
                try { filho.close(); }
                catch (Throwable erro) {
                    if (falha == null) falha = erro;
                    if (erro instanceof InterruptedException) interrompido = true;
                    if (filho.processo().isAlive()) {
                        filho.processo().destroyForcibly();
                        try { filho.processo().waitFor(8, TimeUnit.SECONDS); }
                        catch (InterruptedException novamente) { interrompido = true; }
                    }
                }
            }
            boolean encerrados = filhos.stream().noneMatch(f -> f.processo().isAlive());
            checks.put("filhosPropriosEncerrados", encerrados);
            boolean aprovado = concluido && falha == null && encerrados && checks.values().stream().allMatch(Boolean::booleanValue);
            gravar("resultado-segunda-fase.json", mapa("status", aprovado ? "PASS_IN_PHASES" : "FAILED",
                    "allAssertionsPassed", aprovado, "acceptanceEquivalentAcrossPhases", aprovado,
                    "fullJUnitExecuted", false, "preparationIsAcceptance", false,
                    "checks", checks, "handoffSha256", handoffHash, "jarSha256", handoff.path("jarSha256").asText(),
                    "jobId", jobId(), "tentativa", 2, "population", 200,
                    "retomadaAteTerminalMs", duracaoRetomadaMs, "failureType", falha == null ? null : falha.getClass().getName(),
                    "ultimoSnapshotCanonico", ultimoSnapshot,
                    "children", filhos.stream().map(f -> mapa("pid", f.processo().pid(), "alive", f.processo().isAlive(),
                            "exitCode", f.processo().isAlive() ? null : f.processo().exitValue())).toList()));
            if (interrompido) Thread.currentThread().interrupt();
            if (!aprovado) throw new IllegalStateException("Segunda fase falhou; consulte o recibo privado resultado-segunda-fase.json", falha);
        }
        System.out.println("{\"status\":\"PASS_IN_PHASES\",\"receipt\":\"resultado-segunda-fase.json\"}");
    }

    private void conferirHandoffEIdentidade(Connection conexao) throws Exception {
        String bruto = texto(conexao, IDENTIDADE_SQL);
        JsonNode identidadeCompleta = JSON.readTree(bruto);
        JsonNode identidade = identidadeSemCredenciais(bruto);
        exigir(identidade.equals(handoff.path("databaseIdentity")), "Identidade do PostgreSQL divergiu do handoff");
        exigir(conexao.getMetaData().getDatabaseProductName().equals("PostgreSQL")
                && conexao.getMetaData().getDatabaseProductVersion().matches("18\\.6(?: .*)?"), "Servidor JDBC não é PostgreSQL 18.6");
        exigir(identidade.path("versionNum").asInt() == 180006 && identidade.path("database").asText().startsWith("sgt_20261109_"),
                "Banco não pertence ao PostgreSQL 18.6 isolado desta campanha");
        exigir(identidadeCompleta.path("role").asText().equals(config.usuario())
                && identidadeCompleta.path("sessionRole").asText().equals(config.usuario())
                && conexao.getMetaData().getUserName().equals(config.usuario()), "Identidade da role divergiu");
        for (String flag : List.of("databaseOwnedByRole", "schemaOwnedByRole", "schemaUsage", "schemaCreate", "roleLimited", "noMemberships", "noAdministrativeMembership"))
            exigir(identidade.path(flag).asBoolean(), "Role limitada ou ownership não comprovados: " + flag);
        exigir(identidade.path("schema").asText().equals("public"), "Schema inesperado");
        JsonNode essencial = handoff.path("config");
        exigir(essencial.path("chunk").asInt() == 1 && essencial.path("staleAfterSeconds").asInt() == 15
                && essencial.path("recoveryDelayMs").asInt() == 1000 && essencial.path("heapMiB").asInt() == 192
                && essencial.path("metaspaceMiB").asInt() == 192 && essencial.path("poolMax").asInt() == 2,
                "Configuração essencial não corresponde ao cenário aprovado");
        for (String chave : List.of("REDIS_HOST", "REDIS_PORT", "REDIS_DATABASE"))
            exigir(config.redis().get(chave).equals(essencial.path(chave).asText()), "Redis divergiu entre as fases: " + chave);
        exigir(handoff.path("population").asInt() == 200 && handoff.path("tentativa").asInt() == 2, "Massa ou tentativa incompatível");
        exigir(ids(handoff.path("empregados"), null).size() == 200 && ids(handoff.path("vinculos"), null).size() == 200,
                "Handoff não contém os 200 IDs únicos");
        exigir(handoff.path("prefixoCancelado").asInt() > 0 && handoff.path("prefixoCancelado").asInt() < 200,
                "Cancelamento não comprovou um prefixo parcial positivo");
        exigir(hashArquivo(Path.of(handoff.path("jar").asText())).equals(handoff.path("jarSha256").asText()), "JAR não corresponde ao handoff");
        exigir(hashArquivo(Path.of(essencial.path("envTemplate").asText())).equals(essencial.path("envTemplateSha256").asText()),
                "Template fictício mudou entre as fases");
        for (Iterator<Map.Entry<String, JsonNode>> i = handoff.path("classHashes").fields(); i.hasNext();) {
            var item = i.next();
            exigir(item.getKey().matches("RecuperacaoJarSegundaFase(?:\\$[A-Za-z0-9]+)?\\.class"), "Nome de classe inválido no handoff");
            try (InputStream entrada = RecuperacaoJarSegundaFase.class.getResourceAsStream(item.getKey())) {
                exigir(entrada != null && sha(entrada.readAllBytes()).equals(item.getValue().asText()), "Bytecode da fase dois divergiu");
            }
        }
        exigir(handoff.path("classHashes").size() > 0, "Hashes do observador ausentes");
        checks.put("identidadeRoleOwnershipPg180006", true);
        checks.put("hashesJarTemplateEClasses", true);
        checks.put("configuracaoEquivalente", true);
        gravar("identidade-e-configuracao-sem-credenciais.json", mapa("databaseIdentity", identidade, "config", essencial));
    }

    private String snapshot(Connection c) throws Exception {
        String valor = texto(c, SNAPSHOT_SQL, jobId(), handoff.path("data").asText(),
                handoff.path("lotacaoId").asLong(), handoff.path("jornadaId").asLong(),
                handoff.path("contratoId").asLong(), handoff.path("autor").path("id").asLong());
        ultimoSnapshot = JSON.readTree(valor);
        return valor;
    }

    private void conferirTerminal(JsonNode fim) {
        JsonNode job = fim.path("job");
        exigir(job.path("id").asLong() == jobId() && job.path("tentativa").asInt() == 2
                && job.path("status").asText().equals("CONCLUIDO"), "Terminal não corresponde ao mesmo job e tentativa");
        exigir(job.path("detalhe_erro").isNull(), "Terminal possui erro persistido");
        JsonNode prefixo = handoff.path("snapshotAposMorte");
        for (String conjunto : List.of("frequencias", "eventos", "historicos", "auditoriaFrequenciaSave"))
            for (JsonNode linha : prefixo.path(conjunto)) exigir(contem(fim.path(conjunto), linha), "Prefixo canônico foi alterado: " + conjunto);
        for (String campo : List.of("id", "chave_escopo", "data_referencia", "id_lotacao", "id_jornada", "contrato_ids",
                "id_usuario_criador", "criado_em", "justificativa_retificacao", "chave_idempotencia"))
            exigir(prefixo.path("job").path(campo).equals(job.path(campo)), "Identidade ou intenção do job mudou: " + campo);
        JsonNode fontes = fim.path("frequencias"), eventos = fim.path("eventos");
        exigir(fontes.size() == 200 && eventos.size() == 200, "Universo final não tem 200 frequências e eventos");
        exigir(ids(fontes, "id_empregado").equals(ids(handoff.path("empregados"), null))
                && ids(fontes, "id_vinculo_contratual").equals(ids(handoff.path("vinculos"), null))
                && ids(fontes, "id_pessoa").size() == 200, "Universo final perdeu ou duplicou identidades");
        exigir(ids(fontes, "id").equals(ids(eventos, "id_frequencia")), "Eventos não correspondem às mesmas 200 frequências");
        Set<String> hashes = new HashSet<>(), chaves = new HashSet<>();
        for (JsonNode f : fontes) exigir(f.path("status").asText().equals("PRESENCA") && f.path("versao_negocial").asInt() == 1
                && f.path("autor_original_id").asLong() == handoff.path("autor").path("id").asLong(), "Frequência ou autoria final incompatível");
        for (JsonNode e : eventos) {
            exigir(e.path("numero_versao").asInt() == 1 && e.path("usuario_id").asLong() == handoff.path("autor").path("id").asLong()
                    && e.path("usuario_username").asText().equals(handoff.path("autor").path("username").asText())
                    && e.path("motivo").asText().equals(handoff.path("justificativa").asText()), "Evento final não preserva autoria e intenção");
            exigir(!e.path("hash_evento").asText().isBlank() && hashes.add(e.path("hash_evento").asText())
                    && !e.path("chave_idempotencia").asText().isBlank() && chaves.add(e.path("chave_idempotencia").asText()), "Evento ou chave duplicados");
        }
        long restantes = 200L - handoff.path("prefixoCancelado").asLong();
        exigir(job.path("total_previsto").asLong() == restantes && job.path("total_processado").asLong() == restantes
                && job.path("total_inserido").asLong() == restantes && job.path("total_atualizado").asLong() == 0
                && job.path("total_preservado").asLong() == 0, "Contadores da tentativa não reconciliam prefixo e faltantes");
        exigir(job.path("ultimo_empregado_id_processado").asLong() == ids(fontes, "id_empregado").stream().mapToLong(Long::longValue).max().orElseThrow(),
                "Cursor final divergiu das frequências confirmadas");
    }

    private List<Map<String, Object>> esperarDisputa(Connection c, long bloqueador, Set<String> programas) throws Exception {
        long limite = System.nanoTime() + Duration.ofSeconds(20).toNanos();
        do {
            saudeDosDois();
            List<Map<String, Object>> prova = new ArrayList<>();
            try (PreparedStatement s = c.prepareStatement("""
                WITH RECURSIVE fila(pid) AS (
                  SELECT pid FROM pg_stat_activity WHERE datname=current_database() AND ?=ANY(pg_blocking_pids(pid))
                  UNION SELECT a.pid FROM pg_stat_activity a JOIN fila b ON b.pid=ANY(pg_blocking_pids(a.pid))
                    WHERE a.datname=current_database()
                ) SELECT a.pid,a.application_name,a.wait_event_type,a.query,pg_blocking_pids(a.pid)::text
                  FROM pg_stat_activity a JOIN fila f ON f.pid=a.pid
                """)) {
                s.setLong(1, bloqueador);
                try (ResultSet r = s.executeQuery()) { while (r.next()) {
                    if (!programas.contains(r.getString(2)) || !"Lock".equals(r.getString(3))) continue;
                    String sql = r.getString(4).toLowerCase(Locale.ROOT).replace('"', ' ');
                    boolean alvo = sql.matches("(?s).*\\b(?:from|update)\\s+(?:[a-z0-9_]+\\.)?fechamento_frequencia\\b.*")
                            && (sql.contains("for update") || sql.contains("for no key update") || sql.stripLeading().startsWith("update "));
                    if (alvo) prova.add(mapa("pid", r.getLong(1), "aplicacao", r.getString(2), "tabelaAlvo", "fechamento_frequencia",
                            "bloqueadores", r.getString(5), "querySha256", sha(sql.getBytes(StandardCharsets.UTF_8))));
                } }
            }
            if (prova.stream().map(l -> (String) l.get("aplicacao")).collect(Collectors.toSet()).containsAll(programas)) return prova;
            Thread.sleep(75);
        } while (System.nanoTime() < limite);
        throw new IllegalStateException("Dois executores não disputaram fisicamente a PK conhecida no prazo");
    }

    private Filho iniciar(String nome) throws Exception {
        exigir(hashArquivo(Path.of(handoff.path("jar").asText())).equals(handoff.path("jarSha256").asText()), "JAR mudou antes do filho");
        int porta;
        try (ServerSocket reserva = new ServerSocket(0, 0, InetAddress.getByName("127.0.0.1"))) { porta = reserva.getLocalPort(); }
        String aplicacao = "closure-" + nome + "-" + UUID.randomUUID().toString().substring(0, 8);
        Path log = Files.createFile(saida.resolve(nome + ".log"), permissao());
        JsonNode cfg = handoff.path("config");
        List<String> comando = List.of(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-Xmx192m", "-XX:MaxMetaspaceSize=192m", "-XX:ActiveProcessorCount=2", "-XX:+UseSerialGC", "-Duser.timezone=America/Sao_Paulo",
                "-jar", handoff.path("jar").asText(), "--env-file=" + cfg.path("envTemplate").asText(),
                "--server.address=127.0.0.1", "--server.port=" + porta, "--server.ssl.enabled=false", "--spring.main.banner-mode=off",
                "--spring.jpa.hibernate.ddl-auto=update", "--spring.sql.init.mode=never", "--spring.jpa.open-in-view=false",
                "--spring.jpa.show-sql=false", "--spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true",
                "--spring.datasource.hikari.maximum-pool-size=2", "--spring.datasource.hikari.minimum-idle=1",
                "--logging.level.root=WARN", "--logging.level.com.example.siggtrat=WARN", "--logging.level.org.hibernate.SQL=OFF",
                "--logging.level.org.hibernate.orm.jdbc.bind=OFF", "--app.email.enabled=false", "--app.email.gmail.enabled=false",
                "--spring.cache.type=redis", "--app.seed.unidades-operacionais-enabled=false", "--app.seed.jornadas-operacionais-enabled=false",
                "--app.seed.usuarios-globais-enabled=false", "--app.seed.gestores-se-gestao-local-enabled=false",
                "--file.upload-dir=" + cfg.path("uploadDir").asText(), "--audit.fallback-log=" + saida.resolve(nome + "-audit-fallback.jsonl"),
                "--app.relatorios.temp.diretorio=" + cfg.path("relatorioDir").asText(), "--frequencia.fechamento.chunk-size=1",
                "--frequencia.fechamento.executor-core-pool-size=1", "--frequencia.fechamento.executor-max-pool-size=1",
                "--frequencia.fechamento.recovery-delay-ms=1000", "--frequencia.fechamento.stale-after-seconds=15");
        ProcessBuilder builder = new ProcessBuilder(comando).directory(Path.of(handoff.path("javaDirectory").asText()).toFile())
                .redirectErrorStream(true).redirectOutput(log.toFile());
        Map<String, String> env = builder.environment(); env.clear(); env.put("TZ", "America/Sao_Paulo");
        String url = config.url() + (config.url().contains("?") ? "&" : "?") + "ApplicationName=" + aplicacao;
        env.put("DB_URL", url); env.put("DB_USER", config.usuario()); env.put("DB_PASS", config.senha());
        env.put("SPRING_DATASOURCE_URL", url); env.put("SPRING_DATASOURCE_USERNAME", config.usuario()); env.put("SPRING_DATASOURCE_PASSWORD", config.senha());
        env.putAll(config.redis()); env.put("REDIS_HTTP_GET_ENABLED", "true");
        Filho filho = new Filho(builder.start(), URI.create("http://127.0.0.1:" + porta), aplicacao);
        filhos.add(filho);
        gravar("processos.json", filhos.stream().map(f -> mapa("pid", f.processo().pid(), "aplicacao", f.aplicacao(),
                "origem", f.origem().toString(), "jarSha256", handoff.path("jarSha256").asText(), "chunk", 1,
                "staleAfterSeconds", 15, "recoveryDelayMs", 1000, "heapMiB", 192, "metaspaceMiB", 192, "poolMax", 2)).toList());
        try (HttpClient cliente = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build()) {
            long limite = System.nanoTime() + Duration.ofSeconds(55).toNanos();
            do {
                exigir(filho.processo().isAlive(), "JAR filho encerrou durante o boot");
                try {
                    var r = cliente.send(HttpRequest.newBuilder(filho.origem().resolve("/api/autenticacao/csrf"))
                            .timeout(Duration.ofSeconds(1)).GET().build(), HttpResponse.BodyHandlers.discarding());
                    if (r.statusCode() == 200) return filho;
                } catch (java.io.IOException iniciando) { /* Somente a indisponibilidade transitória do socket durante o boot. */ }
                Thread.sleep(100);
            } while (System.nanoTime() < limite);
        }
        throw new IllegalStateException("JAR filho não ficou pronto em 55 segundos");
    }

    private Sessao login(Filho filho) throws Exception {
        Sessao s = new Sessao(filho.origem());
        try {
            s.csrf();
            JsonNode resposta = s.pedido("POST", "/api/autenticacao/autenticar",
                    mapa("matricula", handoff.path("autor").path("matricula").asText(), "password", AuditoriaHttpSuporte.SENHA), 200);
            exigir(resposta.path("username").asText().equals(handoff.path("autor").path("username").asText())
                    && resposta.path("perfil").asText().equals("GESTOR_LOCAL"), "Autenticação não corresponde ao ator sintético");
            exigir(s.cookies.getCookieStore().getCookies().stream().anyMatch(c -> c.getName().equals("autenticacao"))
                    && s.cookies.getCookieStore().getCookies().stream().anyMatch(c -> c.getName().equals("id_dispositivo")), "Cookies reais ausentes");
            s.csrf(); return s;
        } catch (Exception | AssertionError falha) { s.close(); throw falha; }
    }

    private final class Sessao implements AutoCloseable {
        private final URI origem;
        private final CookieManager cookies;
        private final HttpClient cliente;
        private String csrf;
        private Sessao(URI origem) { this(origem, new CookieManager(null, CookiePolicy.ACCEPT_ORIGINAL_SERVER)); }
        private Sessao(URI origem, CookieManager cookies) {
            this.origem = origem;
            this.cookies = cookies;
            cliente = HttpClient.newBuilder().cookieHandler(cookies).connectTimeout(Duration.ofSeconds(3))
                    .followRedirects(HttpClient.Redirect.NEVER).build();
        }
        private void csrf() throws Exception {
            JsonNode valor = pedido("GET", "/api/autenticacao/csrf", null, 200);
            exigir(valor.path("headerName").asText().equals("X-XSRF-TOKEN") && !valor.path("token").asText().isBlank(), "CSRF real inválido");
            csrf = valor.path("token").asText();
        }
        private JsonNode pedido(String metodo, String caminho, Object corpo, int esperado) throws Exception {
            exigir(metodo.equals("GET") || caminho.equals("/api/autenticacao/autenticar"), "A segunda fase não emite POST de retomada");
            long inicio = System.nanoTime();
            var req = HttpRequest.newBuilder(origem.resolve(caminho)).timeout(Duration.ofSeconds(15))
                    .header("Accept", "application/json").header("Cache-Control", "no-cache").header("User-Agent", "SGT-RecuperacaoJarSegundaFase");
            if (!metodo.equals("GET")) { exigir(csrf != null, "Bootstrap CSRF obrigatório"); req.header("X-XSRF-TOKEN", csrf); }
            if (corpo != null) req.header("Content-Type", "application/json");
            HttpResponse<byte[]> resposta = cliente.send(req.method(metodo, corpo == null ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofByteArray(JSON.writeValueAsBytes(corpo))).build(), HttpResponse.BodyHandlers.ofByteArray());
            Map<String, Object> registro = mapa("instante", Instant.now().toString(), "metodo", metodo, "caminho", caminho,
                    "status", resposta.statusCode(), "esperado", esperado, "duracaoMs", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicio));
            JsonNode raiz = JSON.readTree(resposta.body()); JsonNode valor = raiz.hasNonNull("data") ? raiz.get("data") : raiz;
            if (caminho.startsWith(API)) registro.put("job", mapa("id", valor.path("id").asLong(), "status", valor.path("status").asText(),
                    "tentativa", valor.path("tentativa").asInt(), "totalPrevisto", valor.path("totalPrevisto").asLong(),
                    "totalProcessado", valor.path("totalProcessado").asLong(), "percentual", valor.path("percentual").asInt()));
            Path arquivo = saida.resolve("http-status.jsonl");
            if (!Files.exists(arquivo)) Files.createFile(arquivo, permissao());
            Files.writeString(arquivo, JSON.writeValueAsString(registro) + "\n", StandardOpenOption.APPEND);
            exigir(resposta.statusCode() == esperado, "Status HTTP divergiu em " + metodo + " " + caminho);
            return valor;
        }
        @Override public void close() { cliente.close(); }
    }

    private record Filho(Process processo, URI origem, String aplicacao) implements AutoCloseable {
        @Override public void close() throws InterruptedException {
            if (!processo.isAlive()) return;
            processo.destroy();
            if (!processo.waitFor(10, TimeUnit.SECONDS)) { processo.destroyForcibly(); exigir(processo.waitFor(8, TimeUnit.SECONDS), "Filho próprio não encerrou"); }
        }
    }

    private record Config(String url, String usuario, String senha, Map<String, String> redis) {
        private static Config ambiente() {
            String url = env("INTEGRATION_POSTGRES_URL"), usuario = env("INTEGRATION_POSTGRES_USER"), senha = env("INTEGRATION_POSTGRES_PASSWORD");
            exigir(url.startsWith("jdbc:postgresql://"), "URL PostgreSQL explícita obrigatória");
            URI uri = URI.create(url.substring(5));
            exigir(Set.of("127.0.0.1", "localhost", "[::1]", "::1").contains(uri.getHost()) && uri.getUserInfo() == null
                    && uri.getRawQuery() == null && uri.getFragment() == null && uri.getPath().matches("/sgt_20261109_[A-Za-z0-9_]+"), "Endpoint não pertence ao laboratório loopback autorizado");
            Map<String, String> redis = new HashMap<>();
            for (String chave : List.of("REDIS_HOST", "REDIS_PORT", "REDIS_PASSWORD", "REDIS_DATABASE")) redis.put(chave, env(chave));
            exigir(Set.of("127.0.0.1", "localhost", "::1").contains(redis.get("REDIS_HOST")), "Redis deve ser loopback explícito");
            int porta = Integer.parseInt(redis.get("REDIS_PORT")), banco = Integer.parseInt(redis.get("REDIS_DATABASE"));
            exigir(porta > 0 && porta < 65536 && banco >= 0 && banco <= 15, "Redis isolado inválido");
            return new Config(url, usuario, senha, Map.copyOf(redis));
        }
        private Connection conectar() throws SQLException { return DriverManager.getConnection(url, usuario, senha); }
    }

    private long jobId() { return handoff.path("jobId").asLong(); }
    static JsonNode identidadeSemCredenciais(String bruto) throws Exception {
        ObjectNode valor = (ObjectNode) JSON.readTree(bruto);
        String role = valor.path("role").asText(), sessao = valor.path("sessionRole").asText();
        valor.remove(List.of("role", "sessionRole"));
        valor.put("roleSha256", sha(role.getBytes(StandardCharsets.UTF_8)));
        valor.put("sameSessionRole", role.equals(sessao));
        return valor;
    }
    private void saudeDosDois() { exigir(filhos.size() == 2 && filhos.stream().allMatch(f -> f.processo().isAlive()), "Ambos os servidores precisam permanecer saudáveis"); }
    private static Set<Long> ids(JsonNode linhas, String campo) {
        Set<Long> ids = new HashSet<>();
        for (JsonNode linha : linhas) { JsonNode valor = campo == null ? linha : linha.path(campo); exigir(valor.isIntegralNumber() && valor.asLong() > 0 && ids.add(valor.asLong()), "Identidade ausente ou duplicada"); }
        return ids;
    }
    private static boolean contem(JsonNode conjunto, JsonNode linha) { for (JsonNode atual : conjunto) if (atual.equals(linha)) return true; return false; }
    private static String env(String chave) { String valor = System.getenv(chave); exigir(valor != null && !valor.isBlank(), "Variável explícita ausente: " + chave); return valor; }
    private static void exigir(boolean condicao, String mensagem) { if (!condicao) throw new IllegalStateException(mensagem); }
    private static long escalar(Connection c, String sql) throws SQLException { try (var s = c.createStatement(); var r = s.executeQuery(sql)) { exigir(r.next(), "SELECT escalar não retornou valor"); return r.getLong(1); } }
    private static String texto(Connection c, String sql, Object... parametros) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) s.setObject(i + 1, parametros[i]);
            try (ResultSet r = s.executeQuery()) { exigir(r.next(), "SELECT não retornou snapshot"); return r.getString(1); }
        }
    }
    private static Map<String, Object> mapa(Object... pares) { Map<String, Object> mapa = new LinkedHashMap<>(); for (int i = 0; i < pares.length; i += 2) mapa.put((String) pares[i], pares[i + 1]); return mapa; }
    private static String sha(byte[] bytes) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
    private static String hashArquivo(Path path) throws Exception {
        MessageDigest d = MessageDigest.getInstance("SHA-256"); try (InputStream i = Files.newInputStream(path)) { byte[] b = new byte[65536]; for (int n; (n = i.read(b)) >= 0;) if (n > 0) d.update(b, 0, n); } return HexFormat.of().formatHex(d.digest());
    }
    private static java.nio.file.attribute.FileAttribute<Set<java.nio.file.attribute.PosixFilePermission>> permissao() { return PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")); }
    private void gravar(String nome, Object conteudo) throws Exception {
        Path temporario = Files.createTempFile(saida, nome + "-", ".tmp", permissao());
        try (FileChannel c = FileChannel.open(temporario, StandardOpenOption.WRITE)) {
            ByteBuffer b = ByteBuffer.wrap(JSON.writeValueAsBytes(conteudo)); while (b.hasRemaining()) c.write(b); c.force(true);
        }
        Files.move(temporario, saida.resolve(nome), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        try (FileChannel c = FileChannel.open(saida, StandardOpenOption.READ)) { c.force(true); }
    }
}
