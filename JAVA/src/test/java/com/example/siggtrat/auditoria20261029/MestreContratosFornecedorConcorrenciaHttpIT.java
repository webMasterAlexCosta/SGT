package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.dto.AditamentoContratualRequisicao;
import com.example.siggtrat.dto.FornecedorEntradaDTO;
import com.example.siggtrat.dto.PublicacaoContratoDTO;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.infra.ComandoJdbcAtor;
import com.example.siggtrat.infra.EsperaJdbcPostgres;
import com.example.siggtrat.infra.ObservadorAtorPostgres.Ator;
import com.example.siggtrat.servico.AditamentoContratualServico;
import com.example.siggtrat.servico.ContratoServico;
import com.example.siggtrat.servico.FilaRegistroAuditoria;
import com.example.siggtrat.servico.FornecedorServico;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * MASTER SEC007/CON001/CON003: duas chamadas legítimas, dois workers e somente
 * observação JDBC na thread do teste. A é retida após o método real completo e
 * flush, ainda dentro do advice transacional. Não se simula decisão/autoridade.
 * Fonte sintética contemporânea; draft sem alegação de compilação/execução.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class MestreContratosFornecedorConcorrenciaHttpIT extends MestreConcorrenciaSuporte {
    @MockitoSpyBean private AditamentoContratualServico aditamentos;
    @MockitoSpyBean private FornecedorServico fornecedores;
    @MockitoSpyBean private ContratoServico contratos;

    @Test
    @Timeout(value = 100, unit = TimeUnit.SECONDS)
    void sec007DoisGestoresDaMesmaGestaoDisputamPreviasDoMesmoEstado() throws Exception {
        Cenario c = criarCenarioPersistido();
        UsuarioAplicacao segundo = segundoGestor(c);
        BaseContrato n = rascunhoPublicado(c, List.of(50, 50), RelogioSistema.hoje().plusDays(10), null, true);
        LocalDate efeito = n.inicio().plusDays(1);
        var reqA = aditamento("PRORROGACAO-MESTRE-A", efeito); reqA.put("novaDataFim", n.fim().plusDays(10));
        var reqB = aditamento("PRORROGACAO-MESTRE-B", efeito); reqB.put("novaDataFim", n.fim().plusDays(20));
        var previaA = dados(postar(c.localA, base(n.id()) + "/aditamentos/previa", reqA, 200, "sec007-previa-A"));
        var previaB = dados(postar(segundo, base(n.id()) + "/aditamentos/previa", reqB, 200, "sec007-previa-B"));
        reqA.put("tokenPrevia", previaA.path("tokenPrevia").asText());
        reqB.put("tokenPrevia", previaB.path("tokenPrevia").asText());
        String ra = UUID.randomUUID().toString(), rb = UUID.randomUUID().toString();
        var a = autenticar(post(base(n.id()) + "/aditamentos").header("X-Request-Id", ra).contentType(APPLICATION_JSON).content(json(reqA)), c.localA);
        var b = autenticar(post(base(n.id()) + "/aditamentos").header("X-Request-Id", rb).contentType(APPLICATION_JSON).content(json(reqB)), segundo);
        var antes = SnapshotQuantitativos.capturar(dataSource, n.id(), null);
        Barreira gate = new Barreira();
        doAnswer(i -> coordenar(i, gate)).when(AopTestUtils.<AditamentoContratualServico>getUltimateTargetObject(aditamentos))
                .registrar(eq(n.id()), any(AditamentoContratualRequisicao.class));
        Corrida r = disputar(gate, a, b, sql -> sql.selecaoChave("contrato", "id_contrato", n.id()), "SEC007 Contrato da prorrogação");
        var depois = SnapshotQuantitativos.capturar(dataSource, n.id(), null);
        var instrumentos = emTransacao(() -> em.createQuery("select a from AditamentoContratual a where a.contrato.id=:id order by a.id", AditamentoContratual.class)
                .setParameter("id", n.id()).getResultList().stream().map(x -> mapa("id", x.getId(), "eventoId", x.getEvento().getId(),
                        "identificacaoSha", h(x.getIdentificacao()), "fim", x.getNovaDataFim(), "autor", x.getUsuarioResponsavelId())).toList());
        var eventos = eventosContrato(n.id());
        var posicao = consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + efeito, "Prorrogação vencedora após commit");
        registrar("sec007", mapa("atorA", c.localA.getId(), "atorB", segundo.getId(), "gestao", c.gestaoA.getId(),
                "previaASha", hJson(previaA), "previaBSha", hJson(previaB), "antes", antes, "depois", depois,
                "corrida", resumo(r), "instrumentos", instrumentos, "eventos", eventos, "posicaoSha", hJson(posicao)));
        assertAll(() -> conferirCorrida(r, 201, 409),
                () -> assertThat(segundo.getId()).isNotEqualTo(c.localA.getId()),
                () -> assertThat(instrumentos).hasSize(1),
                () -> assertThat(instrumentos.get(0)).containsEntry("autor", c.localA.getId())
                        .containsEntry("identificacaoSha", h("PRORROGACAO-MESTRE-A")).containsEntry("fim", n.fim().plusDays(10)),
                () -> assertThat(eventos).hasSize(2),
                () -> assertThat(eventos.stream().filter(e -> e.tipo().equals("ADITAMENTO")).toList())
                        .singleElement().satisfies(e -> assertThat(e.autor()).isEqualTo(c.localA.getId())),
                () -> assertThat(posicao.path("vigenciaFim").asText()).isEqualTo(n.fim().plusDays(10).toString()),
                () -> assertThat(posicao.path("versaoContratual").path("efetivoPrevisto").asInt()).isEqualTo(100),
                () -> assertThat(depois.tabelas().get("aditamento_contratual").linhas()).hasSize(1),
                () -> assertThat(depois.tabelas().get("composicao_contratual")).isEqualTo(antes.tabelas().get("composicao_contratual")));
    }

    @Test
    @Timeout(value = 100, unit = TimeUnit.SECONDS)
    void con001DoisPostsLegitimosComMesmoCnpjConfirmamUmaEmpresaEUmaAutoria() throws Exception {
        Cenario c = criarCenarioPersistido(); UsuarioAplicacao segundo = segundoGestor(c);
        String cnpj = cnpjSintetico(), razaoA = "Fornecedor vencedor " + UUID.randomUUID(), razaoB = "Fornecedor perdedor " + UUID.randomUUID();
        String ra = UUID.randomUUID().toString(), rb = UUID.randomUUID().toString();
        assertThat(emTransacao(() -> em.createQuery("select count(f) from Fornecedor f where f.cnpj=:cnpj", Long.class)
                .setParameter("cnpj", cnpj).getSingleResult())).isZero();
        List<String> indices = EsperaJdbcPostgres.indicesUnicosDaColuna(new JdbcTemplate(dataSource), "fornecedor", "cnpj");
        assertThat(indices).isNotEmpty();
        var a = autenticar(post("/api/fornecedores").header("X-Request-Id", ra).contentType(APPLICATION_JSON)
                .content(json(mapa("razaoSocial", razaoA, "cnpj", cnpj, "email", "a@example.test"))), c.localA);
        var b = autenticar(post("/api/fornecedores").header("X-Request-Id", rb).contentType(APPLICATION_JSON)
                .content(json(mapa("razaoSocial", razaoB, "cnpj", formatado(cnpj), "email", "b@example.test"))), segundo);
        Barreira gate = new Barreira();
        doAnswer(i -> coordenar(i, gate)).when(AopTestUtils.<FornecedorServico>getUltimateTargetObject(fornecedores))
                .criarFornecedor(any(FornecedorEntradaDTO.class));
        Corrida r = disputar(gate, a, b, sql -> sql.insercao("fornecedor", "cnpj", cnpj), "CON001 identidade única normalizada CNPJ");
        var persistidas = emTransacao(() -> em.createQuery("select f from Fornecedor f where f.cnpj=:cnpj order by f.id", Fornecedor.class)
                .setParameter("cnpj", cnpj).getResultList().stream().map(f -> mapa("id", f.getId(), "cnpjSha", h(f.getCnpj()),
                        "razaoSha", h(f.getRazaoSocial()), "emailSha", h(f.getEmail()), "ativo", f.getAtivo())).toList());
        var auditadas = auditorias("FORNECEDOR_CRIADO", List.of(ra, rb), 1);
        registrar("con001", mapa("cnpjSha", h(cnpj), "indicesUnicos", indices, "corrida", resumo(r),
                "empresas", persistidas, "auditorias", auditadas, "atorA", c.localA.getId(), "atorB", segundo.getId()));
        long id = id(r.a());
        assertAll(() -> conferirCorrida(r, 200, 409),
                () -> assertThat(dados(r.b().http()).path("code").asText()).isEqualTo("CNPJ_DUPLICADO"),
                () -> assertThat(persistidas).hasSize(1),
                () -> assertThat(persistidas.get(0)).containsEntry("id", id).containsEntry("cnpjSha", h(cnpj))
                        .containsEntry("razaoSha", h(razaoA)).containsEntry("emailSha", h("a@example.test")),
                () -> assertThat(auditadas).singleElement().satisfies(x -> {
                    assertThat(x.autor()).isEqualTo(c.localA.getId()); assertThat(x.requestId()).isEqualTo(ra);
                    assertThat(x.entidadeId()).isEqualTo(id); assertThat(x.tipo()).isEqualTo("Fornecedor");
                }),
                () -> assertThat(auditadas.stream().filter(x -> x.requestId().equals(rb)).toList()).isEmpty());
    }

    @ParameterizedTest(name = "CON003 publicação primeiro={0}")
    @ValueSource(booleans = {true, false})
    @Timeout(value = 100, unit = TimeUnit.SECONDS)
    void con003PublicacaoEAssociacaoRespeitamAsDuasOrdensLegitimas(boolean publicacaoPrimeiro) throws Exception {
        Cenario c = criarCenarioPersistido(); UsuarioAplicacao segundo = segundoGestor(c);
        String cnpj = cnpjSintetico();
        long fornecedor = dados(postar(c.localA, "/api/fornecedores", mapa("razaoSocial", "Empresa vinculável sintética",
                "cnpj", formatado(cnpj)), 200, "con003-empresa")).path("id").asLong();
        BaseContrato n = rascunhoPublicado(c, List.of(5), RelogioSistema.hoje().plusDays(10), cnpj, false);
        IdentidadeContrato antes = identidade(n.id());
        assertThat(antes.fornecedorId()).isNull();
        assertThat(antes.estado()).isEqualTo("RASCUNHO");
        String ra = UUID.randomUUID().toString(), rb = UUID.randomUUID().toString();
        var publicar = post(base(n.id()) + "/publicar").contentType(APPLICATION_JSON).content(json(publicacao()));
        var vincular = put(base(n.id()) + "/fornecedor/" + fornecedor);
        var a = autenticar((publicacaoPrimeiro ? publicar : vincular).header("X-Request-Id", ra), c.localA);
        var b = autenticar((publicacaoPrimeiro ? vincular : publicar).header("X-Request-Id", rb), segundo);
        Barreira gate = new Barreira();
        doAnswer(i -> coordenar(i, gate)).when(AopTestUtils.<ContratoServico>getUltimateTargetObject(contratos))
                .publicar(eq(n.id()), any(PublicacaoContratoDTO.class));
        doAnswer(i -> coordenar(i, gate)).when(AopTestUtils.<FornecedorServico>getUltimateTargetObject(fornecedores))
                .vincularFornecedor(eq(n.id()), eq(fornecedor));
        Corrida r = disputar(gate, a, b, sql -> sql.selecaoChave("contrato", "id_contrato", n.id()), "CON003 rascunho futuro comum");
        IdentidadeContrato depois = identidade(n.id());
        var fotos = emTransacao(() -> em.createQuery("select v from VersaoContratoNegocial v where v.contrato.id=:id order by v.id", VersaoContratoNegocial.class)
                .setParameter("id", n.id()).getResultList().stream().map(v -> mapa("id", v.getId(), "fornecedorId", v.getFornecedorId(),
                        "cnpjSha", h(v.getCnpjEmpresa()))).toList());
        var eventos = eventosContrato(n.id());
        var auditoria = auditorias("CONTRATO_FORNECEDOR_VINCULADO", List.of(ra, rb), publicacaoPrimeiro ? 0 : 1);
        registrar("con003-" + (publicacaoPrimeiro ? "publicacao-primeiro" : "associacao-primeiro"), mapa("antes", antes,
                "depois", depois, "corrida", resumo(r), "fotografias", fotos, "eventos", eventos, "auditoriaAssociacao", auditoria,
                "fornecedorId", fornecedor, "cnpjDocumentalSha", h(cnpj), "atorA", c.localA.getId(), "atorB", segundo.getId()));
        Long esperadoFk = publicacaoPrimeiro ? null : fornecedor;
        Long autorPublicacao = publicacaoPrimeiro ? c.localA.getId() : segundo.getId();
        assertAll(() -> conferirCorrida(r, 200, publicacaoPrimeiro ? 409 : 200),
                () -> assertThat(depois.estado()).isEqualTo("PUBLICADO"),
                () -> assertThat(depois.fornecedorId()).isEqualTo(esperadoFk),
                () -> assertThat(depois.cnpjSha()).isEqualTo(h(cnpj)),
                () -> assertThat(depois.publicadoPor()).isEqualTo(autorPublicacao),
                () -> assertThat(fotos).singleElement().satisfies(f -> assertThat(f).containsEntry("fornecedorId", esperadoFk).containsEntry("cnpjSha", h(cnpj))),
                () -> assertThat(eventos).singleElement().satisfies(e -> {
                    assertThat(e.tipo()).isEqualTo("PUBLICACAO"); assertThat(e.autor()).isEqualTo(autorPublicacao);
                }),
                () -> assertThat(auditoria).hasSize(publicacaoPrimeiro ? 0 : 1),
                () -> { if (!publicacaoPrimeiro) assertThat(auditoria.get(0).autor()).isEqualTo(c.localA.getId()); });
    }

    private IdentidadeContrato identidade(long id) {
        return emTransacao(() -> {
            Contrato c = em.find(Contrato.class, id);
            return new IdentidadeContrato(c.getId(), c.getEstadoNegocial().name(), c.getVersion(),
                    c.getFornecedor() == null ? null : c.getFornecedor().getId(), h(c.getCnpjEmpresa()),
                    h(c.getRazaoSocialEmpresa()), h(c.getNomeFantasiaEmpresa()), c.getPublicadoPorId());
        });
    }

    private static String cnpjSintetico() {
        return String.format(Locale.ROOT, "%014d", Math.floorMod(UUID.randomUUID().getMostSignificantBits(), 100_000_000_000_000L));
    }
    private static String formatado(String c) { return c.substring(0, 2) + "." + c.substring(2, 5) + "." + c.substring(5, 8) + "/" + c.substring(8, 12) + "-" + c.substring(12); }
    private record IdentidadeContrato(long id, String estado, long versao, Long fornecedorId, String cnpjSha,
                                      String razaoSha, String fantasiaSha, Long publicadoPor) { }
}

/** Suporte privado dos dois drafts; não adiciona capacidade de produção. */
abstract class MestreConcorrenciaSuporte extends ContratosHttpBase {
    @Autowired protected DataSource dataSource;
    @Autowired private FilaRegistroAuditoria fila;

    protected Object coordenar(InvocationOnMock i, Barreira gate) throws Throwable {
        boolean a = Thread.currentThread() == gate.threadA.get();
        boolean b = Thread.currentThread() == gate.threadB.get();
        if (!a && !b) return i.callRealMethod();
        try {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
            long pid = ((Number) em.createNativeQuery("select pg_backend_pid()").getSingleResult()).longValue();
            if (b) gate.pidB.complete(pid);
            Object resposta = i.callRealMethod();
            if (a) {
                em.flush();
                assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
                gate.pidA.complete(pid);
                if (!gate.liberar.await(30, TimeUnit.SECONDS)) throw new AssertionError("Barreira A não liberada");
            }
            return resposta;
        } catch (Throwable falha) {
            (a ? gate.pidA : gate.pidB).completeExceptionally(falha);
            throw falha;
        }
    }

    protected Corrida disputar(Barreira gate, AbstractMockHttpServletRequestBuilder<?> a,
                               AbstractMockHttpServletRequestBuilder<?> b, Predicate<ComandoJdbcAtor> alvo, String descricao)
            throws Exception {
        var pool = Executors.newFixedThreadPool(2);
        Future<MvcResult> fa = null, fb = null;
        List<Map<String, Object>> esperas = List.of(); String falhaGate = null;
        boolean aPendente = false, bPendente = false, encerrado;
        Resultado ra, rb;
        try {
            fa = pool.submit(() -> { gate.threadA.set(Thread.currentThread()); return gate.atorA.executar(() -> mvc.perform(a).andReturn()); });
            long raiz = gate.pidA.get(15, TimeUnit.SECONDS);
            fb = pool.submit(() -> { gate.threadB.set(Thread.currentThread()); return gate.atorB.executar(() -> mvc.perform(b).andReturn()); });
            long segunda = gate.pidB.get(10, TimeUnit.SECONDS);
            if (raiz == segunda) throw new AssertionError("A e B exigem conexões/transações independentes");
            esperas = EsperaJdbcPostgres.aguardar(new JdbcTemplate(dataSource), raiz, gate.atorB, alvo, descricao, fb);
            aPendente = !fa.isDone(); bPendente = !fb.isDone();
        } catch (Throwable falha) { falhaGate = falha.getClass().getName(); }
        finally {
            gate.liberar.countDown();
            ra = recolher(fa); rb = recolher(fb);
            pool.shutdown(); encerrado = pool.awaitTermination(10, TimeUnit.SECONDS);
            if (!encerrado) pool.shutdownNow();
        }
        Corrida corrida = new Corrida(ra, rb, esperas, falhaGate, aPendente, bPendente, encerrado);
        registrar("corrida", mapa("alvoObservado", descricao, "resultado", resumo(corrida)));
        return corrida;
    }

    private Resultado recolher(Future<MvcResult> f) {
        if (f == null) return new Resultado(null, "NAO_INICIADO");
        try { return new Resultado(f.get(20, TimeUnit.SECONDS), null); }
        catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            return new Resultado(null, e.getClass().getName() + "/" + (e.getCause() == null ? "SEM_CAUSA" : e.getCause().getClass().getName()));
        }
    }

    protected void conferirCorrida(Corrida r, int a, int b) {
        assertAll(() -> assertThat(r.falhaGate()).isNull(), () -> assertThat(r.a().falha()).isNull(), () -> assertThat(r.b().falha()).isNull(),
                () -> assertThat(r.esperas()).isNotEmpty(), () -> assertThat(r.aPendente()).isTrue(), () -> assertThat(r.bPendente()).isTrue(),
                () -> assertThat(r.encerrado()).isTrue(), () -> assertThat(status(r.a())).isEqualTo(a), () -> assertThat(status(r.b())).isEqualTo(b));
    }

    protected Map<String, Object> resumo(Corrida r) throws Exception {
        List<Map<String, Object>> espera = r.esperas().stream().map(l -> {
            Map<String, Object> x = new LinkedHashMap<>(l); Object sql = x.remove("sql_preparado");
            x.put("sqlSha256", sql == null ? null : h(sql.toString())); return x;
        }).toList();
        return mapa("A", r.a().http() == null ? mapa("falha", r.a().falha()) : SnapshotQuantitativos.http(mapper, r.a().http()),
                "B", r.b().http() == null ? mapa("falha", r.b().falha()) : SnapshotQuantitativos.http(mapper, r.b().http()),
                "esperasFisicasSemSQL", espera, "falhaGate", r.falhaGate(), "aPendenteAntesDeLiberar", r.aPendente(),
                "bPendenteAntesDeLiberar", r.bPendente(), "workersEncerrados", r.encerrado());
    }

    protected UsuarioAplicacao segundoGestor(Cenario c) {
        return emTransacao(() -> { UsuarioAplicacao u = usuario(PerfilAcesso.GESTOR_LOCAL, em.find(GestaoLocal.class, c.gestaoA.getId())); em.flush(); return u; });
    }

    protected BaseContrato rascunhoPublicado(Cenario c, List<Integer> quantidades, LocalDate inicio, String cnpj, boolean publicar) throws Exception {
        LocalDate fim = inicio.plusDays(60);
        var body = rascunho(c, "MESTRE-" + UUID.randomUUID().toString().substring(0, 8), quantidades.stream().mapToInt(Integer::intValue).sum(), inicio, fim);
        if (cnpj != null) body.put("cnpjEmpresa", cnpj);
        long id = dados(postar(c.localA, "/api/contratos", body, 201, "contrato")).path("id").asLong();
        List<Long> comps = new ArrayList<>();
        for (int i = 0; i < quantidades.size(); i++) {
            long cargo = i == 0 ? c.cargoA.getId() : dados(postar(c.localA, "/api/cargos",
                    mapa("nome", "Cargo mestre " + UUID.randomUUID(), "ativo", true), 200, "cargo")).path("id").asLong();
            comps.add(dados(postar(c.localA, base(id) + "/composicoes", composicao(c, cargo, quantidades.get(i)), 201, "composicao")).path("id").asLong());
        }
        if (publicar) postar(c.localA, base(id) + "/publicar", publicacao(), 200, "publicacao");
        return new BaseContrato(id, List.copyOf(comps), inicio, fim);
    }

    protected Map<String, Object> publicacao() { return mapa("dataPublicacao", RelogioSistema.hoje(), "referenciaSei", "SEI-MESTRE-ORIGINAL", "justificativa", "Publicação sintética mestre"); }
    protected MvcResult postar(UsuarioAplicacao u, String path, Object body, int esperado, String etapa) throws Exception {
        var r = mvc.perform(autenticar(post(path).contentType(APPLICATION_JSON).content(json(body)), u)).andReturn();
        registrar("setup-" + etapa, SnapshotQuantitativos.http(mapper, r));
        assertThat(r.getResponse().getStatus()).as("Pré-condição %s", etapa).isEqualTo(esperado); return r;
    }

    protected List<Auditoria> auditorias(String acao, List<String> requests, int minimo) throws Exception {
        long fim = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        List<Auditoria> resposta;
        do {
            fila.descarregar();
            resposta = emTransacao(() -> em.createQuery("select a from RegistroAuditoria a where a.action=:acao and a.requestId in :requests order by a.id", RegistroAuditoria.class)
                    .setParameter("acao", acao).setParameter("requests", requests).getResultList().stream().map(a -> new Auditoria(a.getId(),
                            a.getAction(), a.getEntityType(), a.getEntityId(), a.getUserId(), a.getRequestId(), h(a.getDetailsJson()))).toList());
            if (resposta.size() >= minimo) return resposta;
            Thread.sleep(20);
        } while (System.nanoTime() < fim);
        return resposta;
    }

    protected void descarregarAuditoria() { fila.descarregar(); }

    protected List<EventoContrato> eventosContrato(long id) {
        return emTransacao(() -> em.createQuery("select e from EventoCicloVidaContrato e where e.contrato.id=:id order by e.id", EventoCicloVidaContrato.class)
                .setParameter("id", id).getResultList().stream().map(e -> new EventoContrato(e.getId(), e.getTipoEvento().name(), e.getUsuarioResponsavelId())).toList());
    }

    protected List<String> linhas(String sql, String parametro, Object valor) {
        return emTransacao(() -> {
            List<String> hashes = new ArrayList<>();
            for (Object x : em.createNativeQuery(sql).setParameter(parametro, valor).getResultList()) hashes.add(h(x.toString()));
            return List.copyOf(hashes);
        });
    }
    protected void registrar(String caso, Object dados) throws Exception { SnapshotQuantitativos.registrar(mapper, "mestre-concorrencia", caso, dados); }
    protected String h(String valor) { return valor == null ? null : SnapshotQuantitativos.hash(valor); }
    protected String hJson(Object valor) throws Exception { return SnapshotQuantitativos.hashJson(mapper, valor); }
    protected Integer status(Resultado r) { return r.http() == null ? null : r.http().getResponse().getStatus(); }
    protected long id(Resultado r) throws Exception { return r.http() == null ? -1 : dados(r.http()).path("id").asLong(-1); }

    protected static final class Barreira {
        final AtomicReference<Thread> threadA = new AtomicReference<>(), threadB = new AtomicReference<>();
        final CompletableFuture<Long> pidA = new CompletableFuture<>(), pidB = new CompletableFuture<>();
        final CountDownLatch liberar = new CountDownLatch(1);
        final Ator atorA = new Ator("mestre-A"), atorB = new Ator("mestre-B");
    }
    protected record Resultado(MvcResult http, String falha) { }
    protected record Corrida(Resultado a, Resultado b, List<Map<String, Object>> esperas, String falhaGate,
                             boolean aPendente, boolean bPendente, boolean encerrado) { }
    protected record BaseContrato(long id, List<Long> composicoes, LocalDate inicio, LocalDate fim) { }
    protected record Auditoria(long id, String acao, String tipo, Long entidadeId, Long autor, String requestId, String detalhesSha) { }
    protected record EventoContrato(long id, String tipo, Long autor) { }
}
