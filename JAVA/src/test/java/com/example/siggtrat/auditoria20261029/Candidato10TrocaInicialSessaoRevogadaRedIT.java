package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.utilitario.UtilitarioHashSeguranca;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Autoria central /root; requer revisão independente, não se apresenta como teste do revisor.
 * Base C10 e58cd276bad1395ce910dd9fc466c50031d1579f12e8a69747ccfadcdfb95b99.
 * Falha procurada: troca inicial emite nova sessão ou modifica credenciais depois
 * de aguardar o commit de uma revogação administrativa da sessão/conta.
 * Login, comando administrativo, cadeia MVC, JPA e locks são reais; sem mocks.
 * A transação externa apenas retém o commit do comando HTTP administrativo para
 * observar a espera física do comando de troca em performance_schema.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class Candidato10TrocaInicialSessaoRevogadaRedIT extends ContratosHttpBase {
    @Autowired private DataSource dataSource;

    @ParameterizedTest(name = "revogacaoInicial={0}")
    @ValueSource(strings = {"INATIVAR", "REDEFINIR_SENHA"})
    @Timeout(value = 75, unit = TimeUnit.SECONDS)
    void trocaInicialRejeitaSessaoRevogadaDuranteEsperaDoUsuario(String fluxo) throws Exception {
        Cenario c = criarCenarioPersistido();
        emTransacao(() -> {
            em.find(UsuarioAplicacao.class, c.operacionalA.getId()).setForceChangePassword(true);
            em.find(UsuarioAplicacao.class, c.operacionalB.getId()).setForceChangePassword(true);
            em.flush();
            return null;
        });

        Map<String, Cookie> controle = loginInicial(c.operacionalB);
        executar(null, comCookies(post("/api/autenticacao/alterar-senha-inicial"), controle)
                .contentType(APPLICATION_JSON).content(json(mapa("newPassword", "ControleInicial@2026"))),
                200, "Controle positivo: sessão atual troca a própria senha inicial");
        emTransacao(() -> {
            em.clear();
            UsuarioAplicacao usuario = em.find(UsuarioAplicacao.class, c.operacionalB.getId());
            assertThat(usuario.getForceChangePassword()).isFalse();
            assertThat(passwordEncoder.matches("ControleInicial@2026", usuario.getPassword())).isTrue();
            return null;
        });

        Map<String, Cookie> cookies = loginInicial(c.operacionalA);
        var tentativaHttp = comCookies(post("/api/autenticacao/alterar-senha-inicial"), cookies)
                .contentType(APPLICATION_JSON).content(json(mapa("newPassword", "TentativaRevogada@2026")));
        var administracaoHttp = fluxo.equals("INATIVAR")
                ? delete("/api/usuarios/" + c.operacionalA.getId())
                : post("/api/usuarios/" + c.operacionalA.getId() + "/redefinir-senha")
                    .contentType(APPLICATION_JSON).content(json(mapa("newPassword", "NovaTemporariaAdmin@2026")));
        autenticar(administracaoHttp, c.admin);

        var pool = Executors.newFixedThreadPool(2);
        CountDownLatch permitirCommit = new CountDownLatch(1);
        CompletableFuture<Barreira> barreira = new CompletableFuture<>();
        try {
            Future<?> administracao = pool.submit(() -> emTransacao(() -> {
                try {
                    MvcResult resultado = mvc.perform(administracaoHttp).andReturn();
                    assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
                    em.flush();
                    long conexao = ((Number) em.createNativeQuery("select connection_id()")
                            .getSingleResult()).longValue();
                    JsonNode esperado = estadoNaTransacao(c.operacionalA.getId());
                    assertThat(esperado.path("sessoes").asLong()).isZero();
                    assertThat(esperado.path("forceChangePassword").asBoolean()).isTrue();
                    assertThat(esperado.path("active").asBoolean()).isEqualTo(!fluxo.equals("INATIVAR"));
                    barreira.complete(new Barreira(conexao, esperado));
                    if (!permitirCommit.await(40, TimeUnit.SECONDS))
                        throw new AssertionError("Fixture: commit administrativo não liberado");
                    return null;
                } catch (Throwable ex) {
                    barreira.completeExceptionally(ex);
                    if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
                    throw new IllegalStateException(ex);
                }
            }));
            Barreira retida = barreira.get(15, TimeUnit.SECONDS);
            Future<MvcResult> tentativa = pool.submit(() -> mvc.perform(tentativaHttp).andReturn());
            aguardarUsuarioNoMySql(retida.conexao(), tentativa);
            assertThat(administracao.isDone()).isFalse();
            assertThat(tentativa.isDone()).isFalse();
            permitirCommit.countDown();
            administracao.get(10, TimeUnit.SECONDS);
            MvcResult resultado = tentativa.get(15, TimeUnit.SECONDS);
            JsonNode depois = emTransacao(() -> { em.clear(); return estadoNaTransacao(c.operacionalA.getId()); });
            boolean emitiuSessao = resultado.getResponse().getHeaders("Set-Cookie").stream()
                    .anyMatch(h -> h.startsWith("autenticacao=") && !h.startsWith("autenticacao=;"));
            Path evidencia = Path.of("target/auditoria-http/candidato10-troca-inicial-" + fluxo + ".json");
            Files.createDirectories(evidencia.getParent());
            Files.writeString(evidencia, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapa(
                    "hipotese", "C10-CENTRAL-TROCA-INICIAL-SESSAO-REVOGADA", "autoria", "/root",
                    "fluxo", fluxo, "esperaMySqlUsuarioObservada", true,
                    "revogacaoAdministrativaHttp", 200, "commitAdministrativoConfirmado", true,
                    "statusEsperado", 401, "status", resultado.getResponse().getStatus(),
                    "emitiuCookieAutenticacao", emitiuSessao, "estadoAdministrativo", retida.estado(),
                    "estadoDepoisTentativa", depois)));
            assertAll("O primeiro acesso não recupera uma sessão revogada durante a espera",
                    () -> assertThat(resultado.getResponse().getStatus()).isEqualTo(401),
                    () -> assertThat(emitiuSessao).isFalse(),
                    () -> assertThat(depois).as("Credenciais, versão, estado e zero sessões da decisão administrativa")
                            .isEqualTo(retida.estado()));
        } finally {
            permitirCommit.countDown();
            pool.shutdownNow();
            pool.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private JsonNode estadoNaTransacao(long id) {
        UsuarioAplicacao usuario = em.find(UsuarioAplicacao.class, id);
        long sessoes = em.createQuery("select count(t) from TokenAtivo t where t.username=:u", Long.class)
                .setParameter("u", usuario.getUsername()).getSingleResult();
        return mapper.valueToTree(mapa("id", id, "active", usuario.getActive(),
                "forceChangePassword", usuario.getForceChangePassword(), "version", usuario.getVersion(),
                "passwordDigest", UtilitarioHashSeguranca.sha256Hexadecimal(usuario.getPassword()),
                "twoFactorEnabled", usuario.getTwoFactorEnabled(), "sessoes", sessoes));
    }

    private Map<String, Cookie> loginInicial(UsuarioAplicacao usuario) throws Exception {
        var login = executar(null, post("/api/autenticacao/autenticar")
                .cookie(new Cookie("XSRF-TOKEN", "csrf-inicial-c10"))
                .header("X-XSRF-TOKEN", "csrf-inicial-c10").contentType(APPLICATION_JSON)
                .content(json(mapa("matricula", usuario.getMatricula(), "password", SENHA))),
                200, "Login real com senha temporária para ensaio concorrente de primeiro acesso");
        assertThat(dados(login).path("forceChangePassword").asBoolean()).isTrue();
        Map<String, Cookie> cookies = new LinkedHashMap<>();
        for (String header : login.getResponse().getHeaders("Set-Cookie")) {
            String[] par = header.split(";", 2)[0].split("=", 2);
            if (par.length == 2 && !par[1].isBlank()) cookies.put(par[0], new Cookie(par[0], par[1]));
        }
        assertThat(cookies).containsKeys("autenticacao", "id_dispositivo");
        cookies.remove("XSRF-TOKEN");
        var csrf = executar(null, comCookies(get("/api/autenticacao/csrf"), cookies), 200,
                "CSRF atual da sessão inicial real");
        cookies.put("XSRF-TOKEN", new Cookie("XSRF-TOKEN", dados(csrf).path("token").asText()));
        return cookies;
    }

    private MockHttpServletRequestBuilder comCookies(MockHttpServletRequestBuilder request,
                                                     Map<String, Cookie> cookies) {
        request.cookie(cookies.values().toArray(Cookie[]::new));
        if (cookies.containsKey("XSRF-TOKEN")) request.header("X-XSRF-TOKEN", cookies.get("XSRF-TOKEN").getValue());
        return request;
    }

    private void aguardarUsuarioNoMySql(long conexao, Future<?> tentativa) throws Exception {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        var observador = Executors.newSingleThreadScheduledExecutor();
        CompletableFuture<Void> observada = new CompletableFuture<>();
        var consulta = observador.scheduleWithFixedDelay(() -> {
            try {
                Long quantidade = jdbc.queryForObject("select count(*) from performance_schema.data_lock_waits w "
                        + "join performance_schema.threads t on t.THREAD_ID=w.BLOCKING_THREAD_ID "
                        + "join performance_schema.data_locks l on l.ENGINE_LOCK_ID=w.REQUESTING_ENGINE_LOCK_ID "
                        + "and l.ENGINE=w.ENGINE where t.PROCESSLIST_ID=? and l.OBJECT_NAME='usuario_aplicacao'",
                        Long.class, conexao);
                if (quantidade != null && quantidade > 0) observada.complete(null);
                else if (tentativa.isDone()) observada.completeExceptionally(
                        new AssertionError("A troca inicial não alcançou espera real no usuário"));
            } catch (Throwable ex) { observada.completeExceptionally(ex); }
        }, 0, 25, TimeUnit.MILLISECONDS);
        try { observada.get(12, TimeUnit.SECONDS); }
        finally {
            consulta.cancel(true); observador.shutdownNow(); observador.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    private record Barreira(long conexao, JsonNode estado) { }
}
