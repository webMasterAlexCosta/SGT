package com.example.siggtrat.auditoria20261029;

import com.sun.tools.attach.VirtualMachine;
import org.springframework.jdbc.core.JdbcTemplate;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/** Observador opt-in do filho próprio: JMX local e PostgreSQL, sem endpoint ou código no JAR. */
final class RecursosJarObservador implements AutoCloseable {
    private static final Pattern DURACAO = Pattern.compile("\\[(\\d+)]\\s+.*?duration: ([0-9.]+) ms\\s+(?:execute [^:]+|statement):\\s*(.*)");
    private final JdbcTemplate jdbc;
    private final String applicationName;
    private final Path logPostgres;
    private final JMXConnector conector;
    private final MBeanServerConnection servidor;
    private final List<ObjectName> coletores, pools, tomcats;
    private final Set<Integer> pids = new HashSet<>();
    private final ScheduledExecutorService amostrador = Executors.newSingleThreadScheduledExecutor();
    private final List<Map<String, Object>> amostras = new CopyOnWriteArrayList<>();
    private final AtomicReference<Throwable> falha = new AtomicReference<>();
    private long inicioLog;
    private Map<String, Object> antes;

    RecursosJarObservador(Process filho, JdbcTemplate jdbc, String applicationName) throws Exception {
        assertTrue(filho.isAlive());
        this.jdbc = jdbc; this.applicationName = applicationName;
        assertTrue(applicationName.startsWith("sgt-http-it-"));
        String caminho = System.getenv("SGT_INTEGRATION_POSTGRES_LOG");
        assertNotNull(caminho, "Habilitar perfil HTTP no runner do laboratório exclusivo");
        logPostgres = Path.of(caminho).toRealPath(); assertTrue(Files.isRegularFile(logPostgres));
        assertEquals("0", jdbc.queryForObject("select current_setting('log_min_duration_statement')", String.class));
        VirtualMachine vm = VirtualMachine.attach(Long.toString(filho.pid()));
        String endereco;
        try { endereco = vm.startLocalManagementAgent(); } finally { vm.detach(); }
        conector = JMXConnectorFactory.connect(new JMXServiceURL(endereco));
        try {
            servidor = conector.getMBeanServerConnection();
            coletores = nomes("java.lang:type=GarbageCollector,*");
            pools = nomes("com.zaxxer.hikari:type=Pool (*)");
            tomcats = nomes("Tomcat:type=ThreadPool,*");
            assertFalse(coletores.isEmpty()); assertEquals(1, pools.size()); assertEquals(1, tomcats.size());
        } catch (Exception | AssertionError erro) { conector.close(); amostrador.shutdownNow(); throw erro; }
    }

    private List<ObjectName> nomes(String padrao) throws Exception {
        return servidor.queryNames(new ObjectName(padrao), null).stream()
                .sorted(java.util.Comparator.comparing(ObjectName::getCanonicalName)).toList();
    }

    void iniciarJanela() throws Exception {
        assertTrue(amostras.isEmpty()); assertNull(falha.get());
        antes = fotografar(); inicioLog = Files.size(logPostgres);
        tarefa = amostrador.scheduleAtFixedRate(() -> {
            try { amostras.add(fotografar()); }
            catch (Exception | AssertionError erro) { falha.compareAndSet(null, erro); }
        }, 0, 50, TimeUnit.MILLISECONDS);
    }

    private java.util.concurrent.ScheduledFuture<?> tarefa;

    Map<String, Object> terminarJanela() throws Exception {
        tarefa.cancel(false);
        // Uma tarefa pode já estar em voo: a barreira evita misturar amostras de requisições distintas.
        amostrador.submit(() -> {}).get(3, TimeUnit.SECONDS);
        if (falha.get() != null) throw new AssertionError("Falha no observador de recursos; não conceder PASS", falha.get());
        Map<String, Object> depois = fotografar();
        long fimLog = Files.size(logPostgres);
        assertTrue(fimLog >= inicioLog && fimLog - inicioLog <= 4 * 1024 * 1024);
        Map<String, Object> consultas = contarConsultas(inicioLog, fimLog);
        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("antes", antes); resultado.put("depois", depois);
        resultado.put("duranteAmostras50ms", List.copyOf(amostras));
        resultado.put("consultasJarNaJanela", consultas);
        resultado.put("limites", "Métricas do JAR na janela, inclusive tarefas próprias concorrentes; filas/esperas amostradas, sem prova de pico contínuo ou ausência universal de contenção.");
        amostras.clear(); return Map.copyOf(resultado);
    }

    private synchronized Map<String, Object> fotografar() throws Exception {
        List<Map<String, Object>> gc = new ArrayList<>(), hikari = new ArrayList<>(), tomcat = new ArrayList<>();
        for (ObjectName n : coletores) gc.add(atributos(n, "CollectionCount", "CollectionTime"));
        for (ObjectName n : pools) hikari.add(atributos(n, "ActiveConnections", "IdleConnections", "TotalConnections", "ThreadsAwaitingConnection"));
        for (ObjectName n : tomcats) tomcat.add(atributos(n, "currentThreadCount", "currentThreadsBusy", "maxThreads"));
        List<Map<String, Object>> atividades = jdbc.queryForList("""
                select a.pid, a.state, coalesce(a.wait_event_type,'NONE') as wait_event_type,
                       coalesce(a.wait_event,'NONE') as wait_event,
                       (select count(*) from pg_locks l where l.pid=a.pid and l.granted) as locks_granted,
                       (select count(*) from pg_locks l where l.pid=a.pid and not l.granted) as locks_waiting,
                       cardinality(pg_blocking_pids(a.pid)) as blocking_pids_count
                  from pg_stat_activity a where a.datname=current_database() and a.application_name=? order by a.pid
                """, applicationName);
        assertFalse(atividades.isEmpty(), "Conexões do JAR devem ser identificadas no banco atual");
        for (Map<String, Object> a : atividades) pids.add(((Number) a.get("pid")).intValue());
        return Map.of("nanoTime", System.nanoTime(), "gc", gc, "hikari", hikari, "tomcat", tomcat,
                "postgresAtividadesLocksEsperas", atividades);
    }

    private Map<String, Object> atributos(ObjectName nome, String... atributos) throws Exception {
        Map<String, Object> valores = new LinkedHashMap<>(); valores.put("mbean", nome.getCanonicalName());
        for (String atributo : atributos) {
            Object valor = servidor.getAttribute(nome, atributo);
            assertInstanceOf(Number.class, valor, atributo);
            assertTrue(((Number) valor).longValue() >= 0, atributo);
            valores.put(atributo, valor);
        }
        return Map.copyOf(valores);
    }

    private synchronized Map<String, Object> contarConsultas(long inicio, long fim) throws Exception {
        byte[] bytes = new byte[Math.toIntExact(fim - inicio)];
        try (RandomAccessFile arquivo = new RandomAccessFile(logPostgres.toFile(), "r")) {
            arquivo.seek(inicio); arquivo.readFully(bytes);
        }
        long execucoes = 0; double somaMs = 0, maxMs = 0;
        Map<String, Long> operacoes = new LinkedHashMap<>();
        for (String linha : new String(bytes, StandardCharsets.UTF_8).split("\\R")) {
            var m = DURACAO.matcher(linha);
            if (!m.find() || !pids.contains(Integer.parseInt(m.group(1)))) continue;
            execucoes++; double ms = Double.parseDouble(m.group(2)); somaMs += ms; maxMs = Math.max(maxMs, ms);
            String op = m.group(3).stripLeading().split("\\s+", 2)[0].toUpperCase(java.util.Locale.ROOT);
            if (!Set.of("SELECT", "INSERT", "UPDATE", "DELETE", "BEGIN", "COMMIT", "ROLLBACK", "SET", "SHOW").contains(op)) op = "OUTRO";
            operacoes.merge(op, 1L, Long::sum);
        }
        assertTrue(execucoes > 0, "Contador deve reconhecer SQL executado pelas sessões reais do JAR, inclusive autorização em HIT");
        return Map.of("execucoes", execucoes, "duracaoSqlTotalMs", somaMs, "duracaoSqlMaxMs", maxMs,
                "operacoes", operacoes, "pgBackendPidsIdentificados", List.copyOf(pids),
                "fonte", "PostgreSQL log_min_duration_statement=0 do banco exclusivo; somente execute/statement de PIDs do JAR entre offsets. Sem SQL, bind ou parâmetros exportados.");
    }

    @Override public void close() throws Exception {
        if (tarefa != null) tarefa.cancel(false);
        amostrador.shutdownNow(); assertTrue(amostrador.awaitTermination(3, TimeUnit.SECONDS));
        conector.close();
    }
}
