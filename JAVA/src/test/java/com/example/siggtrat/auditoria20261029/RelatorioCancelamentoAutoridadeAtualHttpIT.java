package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.servico.RelatorioExportacaoAssincronaServico;
import com.example.siggtrat.servico.RelatorioExportacaoServico;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongConsumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** JAVA-API: owner continua ativo; decisão usa sua autoridade atual, com geração real retida. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RelatorioCancelamentoAutoridadeAtualHttpIT extends MestreConcorrenciaSuporte {
    private static final Path PRIVADO = GapsBackend15JarSuporte.privado("cancelamento-");
    @MockitoSpyBean private RelatorioExportacaoServico gerador;
    @Autowired private RelatorioExportacaoAssincronaServico assincrono;
    private String tarefaControle;

    @DynamicPropertySource
    static void arquivosProprios(DynamicPropertyRegistry r) {
        r.add("file.upload-dir", () -> PRIVADO.resolve("uploads").toString());
        r.add("audit.fallback-log", () -> PRIVADO.resolve("audit.jsonl").toString());
        r.add("app.relatorios.temp.diretorio", () -> PRIVADO.resolve("relatorios").toString());
    }

    @Test
    @Timeout(value = 100, unit = TimeUnit.SECONDS)
    void ownerRebaixadoNaoCancelaGeradorRetidoEAutoridadeRestauradaCancelaComLimpeza() throws Exception {
        assertTrue(Runtime.getRuntime().maxMemory() <= 192L * 1024 * 1024, "Pai deve ter heap máximo de 192 MiB");
        Cenario c = criarCenarioPersistido();
        var liberar = new CountDownLatch(1);
        var pronto = new CompletableFuture<Path>();
        var umaVez = new AtomicBoolean();
        doAnswer(i -> {
            Path arquivo = (Path) i.callRealMethod(); // Arquivo e decisão reais; spy só coordena.
            if (umaVez.compareAndSet(false, true)) {
                pronto.complete(arquivo);
                if (!liberar.await(45, TimeUnit.SECONDS)) throw new AssertionError("Barreira real do gerador expirou");
            }
            return arquivo;
        }).when(AopTestUtils.<RelatorioExportacaoServico>getUltimateTargetObject(gerador))
                .prepararTerceirizadosXlsx(any(), any(), any(), any(), any(), any(LongConsumer.class));
        Login owner = entrar(c.localA);
        String id = dados(owner.enviar(post(solicitacao(c)), 202)).path("id").asText();
        String rota = "/api/relatorios/exportacoes/" + id;
        Path arquivo = null;
        boolean restaurado = false;
        try {
            arquivo = pronto.get(20, TimeUnit.SECONDS);
            assertTrue(arquivo.toAbsolutePath().normalize().startsWith(PRIVADO.resolve("relatorios")));
            assertFalse(Files.isSymbolicLink(arquivo));
            String hash = GapsBackend15JarSuporte.hashArquivo(arquivo);
            assertTrue(Files.size(arquivo) > 100);
            mudar(c, "OPERACIONAL"); // API real revoga os tokens anteriores.
            Login rebaixado = entrar(c.localA); // Mesmo username/owner; login real com perfil atual.
            assertEquals("OPERACIONAL", dados(rebaixado.enviar(get("/api/autenticacao/sessao?touch=false"), 200))
                    .path("user").path("perfil").asText());
            var fatosAntes = SnapshotQuantitativos.capturar(dataSource, c.contratoA.getId(), c.pessoaA.getId());
            var tarefaAntes = tarefa(id);
            assertEquals("PROCESSANDO", tarefaAntes.get("status"));
            MvcResult negada = rebaixado.enviar(post(rota + "/cancelamento"), 403);
            var tarefaDepois = tarefa(id);
            var fatosDepois = SnapshotQuantitativos.capturar(dataSource, c.contratoA.getId(), c.pessoaA.getId());
            SnapshotQuantitativos.registrar(mapper, "gaps-backend15", "cancelamento-negado", mapa(
                    "camadaExecutada", "MOCKMVC-FILTROS-SPRING-JPA-POSTGRESQL", "id", id, "owner", c.localA.getId(),
                    "autoridadeAtual", "OPERACIONAL", "http", SnapshotQuantitativos.http(mapper, negada),
                    "tarefaAntes", tarefaAntes, "tarefaDepois", tarefaDepois, "fatosAntes", fatosAntes, "fatosDepois", fatosDepois,
                    "arquivoSha256", hash, "geradorContinuaRetido", liberar.getCount() == 1));
            assertEquals(tarefaAntes, tarefaDepois, "Recusa não altera status, progresso, datas ou arquivo da tarefa");
            assertEquals(fatosAntes, fatosDepois);
            assertEquals(hash, GapsBackend15JarSuporte.hashArquivo(arquivo));
            assertEquals(1L, liberar.getCount());
            mudar(c, "GESTOR_LOCAL"); restaurado = true;
            Login autorizado = entrar(c.localA);
            autorizado.enviar(post(rota + "/cancelamento"), 200);
            assertEquals("CANCELADO", tarefa(id).get("status"));
            assertEquals(1L, liberar.getCount(), "Controle cancela enquanto o executor continua retido");
            liberar.countDown();
            esperarRemocao(arquivo);
            assertEquals("CANCELADO", tarefa(id).get("status"));
            autorizado.enviar(get(rota + "/arquivo"), 409);
            String controle = dados(autorizado.enviar(post(solicitacao(c)), 202)).path("id").asText();
            tarefaControle = controle;
            esperarEstado(autorizado, controle, "CONCLUIDO");
            MvcResult baixa = autorizado.enviar(get("/api/relatorios/exportacoes/" + controle + "/arquivo"), 200);
            assertTrue(baixa.getResponse().getContentAsByteArray().length > 100);
            assertEquals("BAIXADO", tarefa(controle).get("status"));
            SnapshotQuantitativos.registrar(mapper, "gaps-backend15", "cancelamento-controles", mapa(
                    "id", id, "terminal", tarefa(id), "arquivoTardioRemovido", !Files.exists(arquivo),
                    "controleId", controle, "controle", tarefa(controle), "baixa", mapa("status", baixa.getResponse().getStatus(),
                            "bytes", baixa.getResponse().getContentAsByteArray().length,
                            "sha256", GapsBackend15JarSuporte.hash(baixa.getResponse().getContentAsByteArray())),
                    "limite", "Uma troca de perfil ativa; não cobre desativação nem TCP/Chrome."));
        } finally {
            liberar.countDown();
            if (!restaurado) mudar(c, "GESTOR_LOCAL");
            // Aguarda o próprio worker; nunca apaga arquivo para esconder falha de limpeza produtiva.
            if (arquivo != null) esperarRemocao(arquivo);
            else if (pronto.isDone() && !pronto.isCompletedExceptionally()) esperarRemocao(pronto.join());
            limparControleProprio();
        }
    }

    private String solicitacao(Cenario c) {
        return "/api/relatorios/terceirizados/exportacao/solicitacao?contratoId=" + c.contratoA.getId() + "&formato=XLSX";
    }
    private void mudar(Cenario c, String perfil) throws Exception {
        Map<String, Object> req = mapa("perfil", perfil, "gestaoLocalId", c.gestaoA.getId());
        if (perfil.equals("OPERACIONAL")) { req.put("lotacaoId", c.lotacaoA.getId()); req.put("jornadaId", c.jornadaA.getId()); }
        executar(c.admin, put("/api/usuarios/" + c.localA.getId()).contentType(MediaType.APPLICATION_JSON).content(json(req)),
                200, "Administrador altera autoridade atual pela API canônica");
        emTransacao(() -> {
            var atual = em.find(UsuarioAplicacao.class, c.localA.getId());
            assertEquals(perfil, atual.getPerfil().name()); assertTrue(atual.getActive()); return null;
        });
    }
    private Map<String, Object> tarefa(String id) throws Exception {
        Object tarefa = tarefaReal(id);
        Map<String, Object> r = new LinkedHashMap<>();
        for (String campo : List.of("id", "usuario", "status", "etapa", "mensagem", "criadoEm", "iniciadoEm", "atualizadoEm", "arquivo", "tamanhoBytes")) {
            Object v = ReflectionTestUtils.getField(tarefa, campo); r.put(campo, v == null ? null : v.toString());
        }
        for (String campo : List.of("processados", "totalEstimado"))
            r.put(campo, ((AtomicLong) Objects.requireNonNull(ReflectionTestUtils.getField(tarefa, campo))).get());
        return r;
    }
    private Object tarefaReal(String id) throws Exception {
        Object servico = AopTestUtils.getUltimateTargetObject(assincrono);
        Object tarefa = ((Map<?, ?>) Objects.requireNonNull(ReflectionTestUtils.getField(servico, "tarefas"))).get(UUID.fromString(id));
        assertNotNull(tarefa); return tarefa;
    }
    private void limparControleProprio() throws Exception {
        if (tarefaControle == null) return;
        long fim = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
        while (Set.of("NA_FILA", "PROCESSANDO", "BAIXANDO").contains(tarefa(tarefaControle).get("status")) && System.nanoTime() < fim) Thread.sleep(20);
        assertFalse(Set.of("NA_FILA", "PROCESSANDO", "BAIXANDO").contains(tarefa(tarefaControle).get("status")), "Nenhum worker próprio pode ficar pendente");
        Path p = (Path) ReflectionTestUtils.getField(tarefaReal(tarefaControle), "arquivo");
        if (p != null && Files.exists(p)) {
            assertTrue(p.toAbsolutePath().normalize().startsWith(PRIVADO.resolve("relatorios")));
            SnapshotQuantitativos.registrar(mapper, "gaps-backend15", "cancelamento-teardown-controle", mapa(
                    "tarefaId", tarefaControle, "estado", tarefa(tarefaControle), "sha256", GapsBackend15JarSuporte.hashArquivo(p),
                    "limpezaDoTeste", "Somente temporário do controle próprio após falha; não aprova transferência nem limpeza produtiva"));
            Files.delete(p);
        }
    }
    private void esperarRemocao(Path arquivo) throws Exception {
        long fim = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (Files.exists(arquivo) && System.nanoTime() < fim) Thread.sleep(20);
        assertFalse(Files.exists(arquivo), "Worker próprio deve remover o arquivo sem ajuda do teste");
    }
    private void esperarEstado(Login s, String id, String estado) throws Exception {
        long fim = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        do {
            String atual = dados(s.enviar(get("/api/relatorios/exportacoes/" + id), 200)).path("status").asText();
            if (atual.equals(estado)) return;
            assertFalse(atual.equals("FALHA")); Thread.sleep(20);
        } while (System.nanoTime() < fim);
        fail("Exportação real não alcançou " + estado + " em 20 s");
    }
    private Login entrar(UsuarioAplicacao u) throws Exception {
        Login s = new Login(); s.csrf();
        JsonNode login = dados(s.enviar(post("/api/autenticacao/autenticar").contentType(MediaType.APPLICATION_JSON)
                .content(json(mapa("matricula", u.getMatricula(), "password", SENHA))), 200));
        assertEquals(u.getUsername(), login.path("username").asText());
        assertTrue(s.cookies.containsKey("autenticacao")); s.csrf(); return s;
    }
    private final class Login {
        final Map<String, Cookie> cookies = new LinkedHashMap<>();
        String csrf;
        void csrf() throws Exception { csrf = dados(enviar(get("/api/autenticacao/csrf"), 200)).path("token").asText(); assertFalse(csrf.isBlank()); }
        MvcResult enviar(MockHttpServletRequestBuilder pedido, int esperado) throws Exception {
            if (!cookies.isEmpty()) pedido.cookie(cookies.values().toArray(Cookie[]::new));
            if (csrf != null) pedido.header("X-XSRF-TOKEN", csrf).header("X-User-Password", SENHA);
            MvcResult r = mvc.perform(pedido).andReturn();
            for (Cookie k : r.getResponse().getCookies()) { if (k.getMaxAge() == 0) cookies.remove(k.getName()); else cookies.put(k.getName(), k); }
            if (r.getRequest().isAsyncStarted()) r = mvc.perform(asyncDispatch(r)).andReturn();
            assertEquals(esperado, r.getResponse().getStatus()); return r;
        }
    }
}
