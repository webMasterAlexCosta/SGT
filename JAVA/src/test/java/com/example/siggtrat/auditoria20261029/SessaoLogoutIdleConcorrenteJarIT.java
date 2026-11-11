package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.TokenAtivo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/** SEC011: duas entradas TCP reais disputam a revogação do mesmo token já ocioso. */
class SessaoLogoutIdleConcorrenteJarIT extends GapsBackend15JarSuporte {
    @Test
    @Timeout(value = 200, unit = TimeUnit.SECONDS)
    void logoutEIdleSobrepostosRevogamSoAlvoSemConflitoEPreservamSessaoDeOutroUsuario() throws Exception {
        preparar("sec011");
        // Propriedade produtiva positiva em minutos: 1 é o menor valor válido.
        // Espera física de 61 s; não representa espera da duração default nem usa clock de teste.
        opcoes = Map.of("app.security.jwt.max-inactivity-minutes", "1");
        ExecutorService pedidos = Executors.newFixedThreadPool(2);
        List<Future<HttpResponse<byte[]>>> respostas = new ArrayList<>();
        try {
            iniciar("unico");
            try (Sessao alvo = login(c.localA); Sessao controle = login(c.localB);
                 Sessao logout = alvo.copiar(); Sessao idle = alvo.copiar()) {
                long tokenId = jdbc.queryForObject("SELECT id FROM token_ativo WHERE username=?", Long.class, c.localA.getUsername());
                assertEquals(Boolean.TRUE, jdbc.queryForObject("SELECT expire_at > localtimestamp + interval '2 minutes' FROM token_ativo WHERE id=?", Boolean.class, tokenId),
                        "O alvo deve chegar ao idle antes da expiração absoluta do JWT");
                String alvoAntes = fotoToken(c.localA.getUsername());
                long inicio = System.nanoTime(), ultimaTestemunha = inicio;
                while (System.nanoTime() - inicio < Duration.ofSeconds(61).toNanos()) {
                    if (System.nanoTime() - ultimaTestemunha >= Duration.ofSeconds(10).toNanos()) {
                        assertTrue(controle.get("/api/autenticacao/sessao?touch=true", 200).path("authenticated").asBoolean());
                        ultimaTestemunha = System.nanoTime();
                    }
                    Thread.sleep(100);
                }
                assertEquals(alvoAntes, fotoToken(c.localA.getUsername()), "Alvo não foi tocado na espera física");
                controle.get("/api/autenticacao/sessao?touch=true", 200);
                String controleAntes = fotoToken(c.localB.getUsername());
                long esperaMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicio);
                assertTrue(esperaMs >= 61_000);
                List<Map<String, Object>> disputa;
                try (Trava barreira = new Trava(TokenAtivo.class, tokenId)) {
                    CountDownLatch partida = new CountDownLatch(1);
                    respostas.add(pedidos.submit(() -> { partida.await(); return logout.pedido("POST", "/api/autenticacao/encerrar-sessao", null, null, -1); }));
                    respostas.add(pedidos.submit(() -> { partida.await(); return idle.pedido("GET", "/api/autenticacao/sessao?touch=false", null, null, -1); }));
                    partida.countDown();
                    disputa = aguardarDoisDeletes(barreira.pid, respostas);
                    assertFalse(respostas.get(0).isDone()); assertFalse(respostas.get(1).isDone());
                    assertEquals(alvoAntes, fotoToken(c.localA.getUsername()));
                    assertEquals(controleAntes, fotoToken(c.localB.getUsername()));
                    prova("sec011-sobreposicao", mapa("barreiraPid", barreira.pid, "tokenIdSintetico", tokenId,
                            "backends", disputa, "esperaFisicaMs", esperaMs, "idleLaboratorioMinutos", 1,
                            "alvoAntesSha256", alvoAntes, "controleAntesSha256", controleAntes,
                            "pedidosAindaPendentes", true, "limite", "Sobreposição física no DELETE produtivo; sem alterar timestamps ou relógios."));
                }
                var saiu = respostas.get(0).get(12, TimeUnit.SECONDS);
                var expirou = respostas.get(1).get(12, TimeUnit.SECONDS);
                assertEquals(200, saiu.statusCode(), "Logout é idempotente e público, inclusive após idle");
                assertEquals(401, expirou.statusCode());
                assertEquals(0L, jdbc.queryForObject("SELECT count(*) FROM token_ativo WHERE username=?", Long.class, c.localA.getUsername()));
                assertEquals(controleAntes, fotoToken(c.localB.getUsername()), "Revogação do alvo não altera a linha da testemunha");
                assertTrue(controle.get("/api/autenticacao/sessao?touch=false", 200).path("authenticated").asBoolean());
                assertEquals(controleAntes, fotoToken(c.localB.getUsername()));
                // Original conserva a autenticação prévia; não se usa o cliente logout, cujos cookies foram apagados.
                alvo.get("/api/autenticacao/sessao?touch=false", 401);
                semErrosOtimistas();
                prova("sec011-final", mapa("statusLogout", saiu.statusCode(), "statusIdle", expirou.statusCode(),
                        "alvoRevogado", true, "controleOutroUsuarioPreservado", true, "controleSha256", controleAntes,
                        "jarSha256", hashArquivo(jar), "pidFilho", filho.pid(), "filhoSaudavel", filho.isAlive()));
                assertTrue(filho.isAlive());
            }
        } finally {
            // Qualquer barreira já foi liberada antes de aguardar as threads e encerrar o próprio filho.
            pedidos.shutdown();
            if (!pedidos.awaitTermination(25, TimeUnit.SECONDS)) {
                for (Future<?> f : respostas) f.cancel(true);
                pedidos.shutdownNow(); assertTrue(pedidos.awaitTermination(5, TimeUnit.SECONDS));
            }
            encerrar();
        }
    }

    private String fotoToken(String username) throws Exception {
        List<String> r = jdbc.query("SELECT to_jsonb(t)::text FROM token_ativo t WHERE username=? ORDER BY id",
                (rs, n) -> rs.getString(1), username);
        assertEquals(1, r.size());
        return SnapshotQuantitativos.hash(r.get(0)); // Nunca exporta token_hash ou atributos da sessão.
    }

    private List<Map<String, Object>> aguardarDoisDeletes(long barreira, List<? extends Future<?>> pedidos) throws Exception {
        long fim = System.nanoTime() + TimeUnit.SECONDS.toNanos(12);
        List<Map<String, Object>> ultima = List.of();
        do {
            List<Map<String, Object>> candidatos = jdbc.queryForList("SELECT pid,query FROM pg_stat_activity "
                    + "WHERE application_name=? AND state='active' AND wait_event_type='Lock' "
                    + "AND lower(query) LIKE '%delete%token_ativo%' AND lower(query) LIKE '%token_hash%'", aplicacao);
            List<Map<String, Object>> confirmados = new ArrayList<>();
            for (var candidato : candidatos) {
                long pid = ((Number) candidato.get("pid")).longValue();
                List<Map<String, Object>> arestas = new ArrayList<>();
                if (alcanca(pid, barreira, new HashSet<>(), arestas)) {
                    List<Map<String, Object>> locks = jdbc.queryForList("SELECT locktype,mode,granted,relation::regclass::text AS relacao,"
                            + "transactionid::text AS transacao FROM pg_locks WHERE pid=? ORDER BY locktype,mode,granted", pid);
                    confirmados.add(mapa("pid", pid, "consultaSha256", SnapshotQuantitativos.hash((String) candidato.get("query")),
                            "caminhoAteBarreira", arestas, "locks", locks));
                }
            }
            ultima = confirmados;
            if (confirmados.size() == 2) return List.copyOf(confirmados);
            for (Future<?> pedido : pedidos) assertFalse(pedido.isDone(), "Pedido terminou antes da prova física de sobreposição");
            Thread.sleep(25);
        } while (System.nanoTime() < fim);
        prova("sec011-disputa-ausente", mapa("barreiraPid", barreira, "ultimaObservacao", ultima));
        return fail("Dois DELETEs do JAR não alcançaram a barreira conhecida em 12 s");
    }
    private boolean alcanca(long pid, long barreira, Set<Long> vistos, List<Map<String, Object>> arestas) {
        if (!vistos.add(pid)) return false;
        List<Long> bloqueadores = jdbc.query("SELECT unnest(pg_blocking_pids(?::integer))::bigint", (r, n) -> r.getLong(1), pid);
        for (long bloqueador : bloqueadores) {
            arestas.add(mapa("esperaPid", pid, "bloqueadorPid", bloqueador));
            if (bloqueador == barreira || alcanca(bloqueador, barreira, vistos, arestas)) return true;
        }
        return false;
    }
}
