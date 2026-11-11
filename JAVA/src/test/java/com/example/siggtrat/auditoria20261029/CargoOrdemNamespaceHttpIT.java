package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.configuracao.FiltroIdentificadorRequisicao;
import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.example.siggtrat.dto.AtualizacaoUsuarioAplicacaoDTO;
import com.example.siggtrat.dto.CargoDTO;
import com.example.siggtrat.entidade.Cargo;
import com.example.siggtrat.entidade.EventoDisponibilidadeCatalogo;
import com.example.siggtrat.entidade.GestaoLocal;
import com.example.siggtrat.entidade.RegistroAuditoria;
import com.example.siggtrat.entidade.SuperintendenciaEstadual;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.infra.EsperaJdbcPostgres;
import com.example.siggtrat.infra.ObservadorAtorPostgres.Ator;
import com.example.siggtrat.servico.AcessoOrganizacionalServico;
import com.example.siggtrat.servico.CargoServico;
import com.example.siggtrat.servico.FilaRegistroAuditoria;
import com.example.siggtrat.servico.UsuarioAplicacaoServico;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.example.siggtrat.utilitario.UtilitarioHashSeguranca;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
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
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Contraprova transitiva: C/transição documentada retém Cargo; A/PUT aguarda
 * Cargo; B/Admin retém o executor e pode aguardar o namespace de A. Liberar C
 * pode fechar C -> executor/B -> Gestão/A -> Cargo/C. SQLSTATE40P01 reprova
 * mesmo quando o handler responde409. O teste aceita B terminada OU esperando
 * a Gestão no gate, mas GREEN exige B commitada antes de liberar C.
 * Spies executam métodos reais completos. Somente a chamada original de
 * revalidação do worker C é pausada, após o lock do Cargo e antes de executar.
 * Draft: nenhuma execução, compilação ou aprovação é herdada de outro teste.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CargoOrdemNamespaceHttpIT extends ContratosHttpBase {
    @Autowired private DataSource dataSource;
    @Autowired private FilaRegistroAuditoria filaAuditoria;
    @MockitoSpyBean private AcessoOrganizacionalServico acesso;
    @MockitoSpyBean private CargoServico cargoServico;
    @MockitoSpyBean private UsuarioAplicacaoServico usuarioServico;

    @Test
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void cargoAguardaItemSemReterNamespaceNemFecharCicloComTransicaoEAdministracao() throws Exception {
        Map<String, List<String>> pks = conferirPostgres();
        ControlePositivo positivo = controlarComandosSequenciais();
        Preparado p = preparar();
        Cenario c = p.cenario();
        String requestA = UUID.randomUUID().toString();
        String requestB = UUID.randomUUID().toString();
        String requestC = UUID.randomUUID().toString();
        List<String> requests = List.of(requestA, requestB, requestC);
        String novoNome = "Relabel disputa " + UUID.randomUUID();
        // A/C usam a versão inicial real: C pausa antes de qualquer mutação.
        // Headers/sessões e todos os requests são preparados sequencialmente.
        var pedidoC = autenticar(transicao(p.cargoId(), p.versao())
                .header(FiltroIdentificadorRequisicao.REQUEST_ID_HEADER, requestC), c.localA);
        var pedidoA = autenticar(relabel(p.cargoId(), p.versao(), novoNome)
                .header(FiltroIdentificadorRequisicao.REQUEST_ID_HEADER, requestA), c.localA);
        var pedidoB = autenticar(comandoAdmin(c)
                .header(FiltroIdentificadorRequisicao.REQUEST_ID_HEADER, requestB), c.admin);
        Estado antes = estado(c, requests);
        assertThat(antes.administracao().sessoesExecutor()).isPositive();
        Controle controle = new Controle(c.localA.getId(), c.gestaoA.getId(), p.cargoId());
        instalarSpies(controle);
        Contador deadlocksAntes = contadorDeadlocks();
        Execucao execucao = disputar(controle, c, pedidoA, pedidoB, pedidoC);
        List<Contador> deadlocksDepois = observarContador(deadlocksAntes);
        Estado depois = estado(c, requests);
        List<FalhaTecnica> falhas = List.copyOf(controle.falhas);
        boolean state40 = falhas.stream().anyMatch(f -> "40P01".equals(f.sqlState()));
        boolean incremento = deadlocksDepois.stream().anyMatch(a -> a.quantidade() > deadlocksAntes.quantidade());
        registrar("tres-http", mapa("controlePositivo", positivo, "chavesPrimarias", pks,
                "cargoDisputadoId", p.cargoId(), "requestA", requestA, "requestB", requestB, "requestC", requestC,
                "antes", antes, "depois", depois, "resultadoA", resumo(execucao.a()),
                "resultadoB", resumo(execucao.b()), "resultadoC", resumo(execucao.c()),
                "pidA", pidRegistrado(controle.pidA), "pidB", pidRegistrado(controle.pidB), "pidC", pidRegistrado(controle.pidC),
                "esperaAPeloCargo", semSql(execucao.esperaA()), "esperaBPelaGestao", semSql(execucao.esperaB()),
                "cicloTresArestas", semSql(execucao.ciclo()), "bTerminouAntesDeLiberarC", execucao.bTerminouAntes(),
                "administracaoRelidaAntesDeLiberarC", execucao.adminAntesLiberar(),
                "aPendenteAntesDeLiberarC", execucao.aPendenteAntes(), "cPendenteAntesDeLiberarC", execucao.cPendenteAntes(),
                "falhaGate", execucao.falhaGate(), "workersEncerrados", execucao.encerrado(),
                "cadeiasThrowableSemMensagens", falhas, "deadlocksAntes", deadlocksAntes, "deadlocksDepois", deadlocksDepois,
                "classificacao", state40 ? "RED_SQLSTATE_40P01" : incremento && !execucao.ciclo().isEmpty()
                        ? "RED_CONTADOR_E_TRES_ARESTAS" : incremento ? "CONTADOR_AUMENTOU_SEM_ATRIBUICAO_COMPLETA"
                        : "SEM_DEADLOCK_OBSERVADO"));

        assertAll("A espera de Cargo não pode conservar um namespace que a administração precisa",
                () -> assertThat(execucao.falhaGate()).isNull(),
                () -> assertThat(execucao.a().falha()).isNull(),
                () -> assertThat(execucao.b().falha()).isNull(),
                () -> assertThat(execucao.c().falha()).isNull(),
                () -> assertThat(execucao.esperaA()).as("A aguarda fisicamente a PK Cargo retida por C").isNotEmpty(),
                () -> assertThat(execucao.aPendenteAntes()).isTrue(),
                () -> assertThat(execucao.cPendenteAntes()).isTrue(),
                () -> assertThat(execucao.bTerminouAntes()).as("B commitou com C ainda retida").isTrue(),
                () -> assertThat(execucao.encerrado()).isTrue(),
                () -> assertThat(status(execucao.b())).isEqualTo(200),
                () -> assertThat(status(execucao.a())).isIn(401, 403),
                () -> assertThat(status(execucao.c())).isIn(401, 403),
                () -> assertThat(falhas).extracting(FalhaTecnica::sqlState).doesNotContain("40P01"),
                () -> assertThat(deadlocksDepois).allSatisfy(a -> assertThat(a.quantidade()).isEqualTo(deadlocksAntes.quantidade())),
                () -> assertThat(depois.negocio()).as("Cargo/versões/eventos/CARGO_* e CATALOGO_TRANSICAO_DOCUMENTADA intactos")
                        .isEqualTo(antes.negocio()),
                () -> conferirAdmin(antes.administracao(), execucao.adminAntesLiberar(), c),
                () -> conferirAdmin(antes.administracao(), depois.administracao(), c));
    }

    private Preparado preparar() throws Exception {
        Cenario c = criarCenarioPersistido();
        var cargo = enviar(c.localA, "/api/cargos", mapa("nome", "Cargo sem usos " + UUID.randomUUID(), "ativo", true),
                200, "Pré-condição: Cargo novo por HTTP, sem vínculos nem programação");
        long id = cargo.path("id").asLong();
        assertThat(id).isPositive();
        assertThat(emTransacao(() -> em.createQuery("select count(c) from ComposicaoContratual c where c.cargo.id=:id", Long.class)
                .setParameter("id", id).getSingleResult())).as("C não retém Contrato: Cargo novo sem composições").isZero();
        return new Preparado(c, id, cargo.path("version").asLong());
    }

    /** Hoje não exige prévia retroativa. A reativa/relabela antes de B inativar a hierarquia. */
    private ControlePositivo controlarComandosSequenciais() throws Exception {
        Preparado p = preparar();
        Cenario c = p.cenario();
        String requestA = UUID.randomUUID().toString();
        String requestB = UUID.randomUUID().toString();
        String requestC = UUID.randomUUID().toString();
        List<String> requests = List.of(requestA, requestB, requestC);
        Estado antes = estado(c, requests);
        MvcResult respostaC = mvc.perform(autenticar(transicao(p.cargoId(), p.versao())
                .header(FiltroIdentificadorRequisicao.REQUEST_ID_HEADER, requestC), c.localA)).andReturn();
        Estado depoisC = estado(c, requests);
        CargoEstado aposC = cargo(depoisC, p.cargoId());
        String nome = "Relabel controle " + UUID.randomUUID();
        MvcResult respostaA = mvc.perform(autenticar(relabel(p.cargoId(), aposC.versao(), nome)
                .header(FiltroIdentificadorRequisicao.REQUEST_ID_HEADER, requestA), c.localA)).andReturn();
        Estado depoisA = estado(c, requests);
        MvcResult respostaB = mvc.perform(autenticar(comandoAdmin(c)
                .header(FiltroIdentificadorRequisicao.REQUEST_ID_HEADER, requestB), c.admin)).andReturn();
        Estado depoisB = estado(c, requests);
        ControlePositivo controle = new ControlePositivo(respostaC.getResponse().getStatus(), respostaA.getResponse().getStatus(),
                respostaB.getResponse().getStatus(), antes, depoisC, depoisA, depoisB);
        registrar("controle-sequencial", controle);
        assertAll("Os três comandos são legítimos na ordem sequencial apropriada",
                () -> assertThat(controle.statusC()).isEqualTo(200),
                () -> assertThat(controle.statusA()).isEqualTo(200),
                () -> assertThat(controle.statusB()).isEqualTo(200),
                () -> assertThat(aposC.ativo()).isFalse(),
                () -> assertThat(aposC.versao()).isGreaterThan(p.versao()),
                () -> assertThat(cargo(depoisA, p.cargoId()).nome()).isEqualTo(nome),
                () -> assertThat(cargo(depoisA, p.cargoId()).ativo()).isTrue(),
                () -> assertThat(cargo(depoisA, p.cargoId()).versao()).isGreaterThan(aposC.versao()),
                () -> assertThat(depoisC.negocio().eventos().stream().filter(e -> Long.valueOf(p.cargoId()).equals(e.cargoId())).toList())
                        .as("Origem HTTP mais ato documentado").hasSize(2),
                () -> assertThat(depoisA.negocio().eventos().stream().filter(e -> Long.valueOf(p.cargoId()).equals(e.cargoId())).toList())
                        .as("Reativação real pelo PUT acrescenta a terceira transição").hasSize(3),
                () -> assertThat(depoisC.negocio().auditorias().stream().filter(a -> a.acao().equals("CATALOGO_TRANSICAO_DOCUMENTADA")
                        && a.tipo().equals("CARGO") && Long.valueOf(p.cargoId()).equals(a.cargoId())).toList()).hasSize(1),
                () -> assertThat(depoisB.negocio()).isEqualTo(depoisA.negocio()),
                () -> conferirAdmin(depoisA.administracao(), depoisB.administracao(), c));
        return controle;
    }

    private AbstractMockHttpServletRequestBuilder<?> transicao(long id, long versao) throws Exception {
        return post("/api/catalogos-temporais/CARGO/" + id + "/transicoes").contentType(APPLICATION_JSON)
                .content(json(mapa("dataEfetiva", RelogioSistema.hoje(), "ativo", false, "versaoEsperada", versao,
                        "motivo", "Ato sintético de controle da ordem de locks", "evidencia", "SEI-SINTETICO-ORDEM-CARGO",
                        "autorizacao", "ATO-SINTETICO-ORDEM-CARGO", "chaveIdempotencia", UUID.randomUUID().toString())));
    }

    private AbstractMockHttpServletRequestBuilder<?> relabel(long id, long versao, String nome) throws Exception {
        return put("/api/cargos/" + id).contentType(APPLICATION_JSON)
                .content(json(mapa("nome", nome, "ativo", true, "version", versao)));
    }

    private AbstractMockHttpServletRequestBuilder<?> comandoAdmin(Cenario c) throws Exception {
        return put("/api/usuarios/" + c.localA.getId()).contentType(APPLICATION_JSON)
                .content(json(mapa("perfil", "GESTOR_ESTADUAL", "active", false, "superintendenciaId", c.seA.getId())));
    }

    private void instalarSpies(Controle controle) throws Exception {
        AcessoOrganizacionalServico acessoSpy = AopTestUtils.getUltimateTargetObject(acesso);
        CargoServico cargoSpy = AopTestUtils.getUltimateTargetObject(cargoServico);
        UsuarioAplicacaoServico usuarioSpy = AopTestUtils.getUltimateTargetObject(usuarioServico);
        assertThat(org.mockito.Mockito.mockingDetails(acessoSpy).isSpy()).isTrue();
        assertThat(org.mockito.Mockito.mockingDetails(cargoSpy).isSpy()).isTrue();
        assertThat(org.mockito.Mockito.mockingDetails(usuarioSpy).isSpy()).isTrue();
        doAnswer(i -> {
            UsuarioAplicacao usuario = i.getArgument(0);
            boolean c = Thread.currentThread() == controle.threadC.get() && usuario != null
                    && Long.valueOf(controle.executorId).equals(usuario.getId());
            if (!c) return i.callRealMethod();
            try {
                if (controle.barreiraC.compareAndSet(false, true)) {
                    assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
                    // A espera física de A comprovará o lock do Cargo que C já adquiriu.
                    controle.pidC.complete(pid());
                    if (!controle.liberarC.await(30, TimeUnit.SECONDS))
                        throw new AssertionError("Fixture não liberou a revalidação original de C");
                }
                return i.callRealMethod();
            } catch (Throwable falha) {
                capturar("C.Acesso.revalidarAposEspera", falha, controle);
                controle.pidC.completeExceptionally(falha);
                throw falha;
            }
        }).when(acessoSpy).revalidarAposEspera(any(UsuarioAplicacao.class));
        doAnswer(i -> {
            Long id = i.getArgument(0);
            if (!Long.valueOf(controle.cargoId).equals(id)) return i.callRealMethod();
            try {
                assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
                controle.pidA.complete(pid());
                return i.callRealMethod();
            } catch (Throwable falha) {
                capturar("A.CargoServico.atualizar", falha, controle);
                controle.pidA.completeExceptionally(falha);
                throw falha;
            }
        }).when(cargoSpy).atualizar(anyLong(), any(CargoDTO.class));
        doAnswer(i -> {
            Long id = i.getArgument(0);
            if (!Long.valueOf(controle.executorId).equals(id)) return i.callRealMethod();
            try {
                assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
                controle.pidB.complete(pid());
                return i.callRealMethod();
            } catch (Throwable falha) {
                capturar("B.UsuarioAplicacaoServico.atualizar", falha, controle);
                controle.pidB.completeExceptionally(falha);
                throw falha;
            }
        }).when(usuarioSpy).atualizar(anyLong(), any(AtualizacaoUsuarioAplicacaoDTO.class));
    }

    private Execucao disputar(Controle controle, Cenario c, AbstractMockHttpServletRequestBuilder<?> pedidoA,
                              AbstractMockHttpServletRequestBuilder<?> pedidoB, AbstractMockHttpServletRequestBuilder<?> pedidoC)
            throws Exception {
        var pool = Executors.newFixedThreadPool(3);
        Future<MvcResult> a = null, b = null, terceiro = null;
        List<Map<String, Object>> esperaA = List.of(), esperaB = List.of(), ciclo = List.of();
        boolean bTerminou = false, aPendente = false, cPendente = false;
        Administracao adminAntes = null;
        String falhaGate = null;
        Resultado resultadoA, resultadoB, resultadoC;
        boolean encerrado;
        try {
            terceiro = pool.submit(() -> {
                controle.threadC.set(Thread.currentThread());
                return controle.atorC.executar(() -> executarCapturando(pedidoC, "HTTP_C", controle));
            });
            long pidC = controle.pidC.get(15, TimeUnit.SECONDS);
            a = pool.submit(() -> controle.atorA.executar(() -> executarCapturando(pedidoA, "HTTP_A", controle)));
            long pidA = controle.pidA.get(10, TimeUnit.SECONDS);
            if (pidA == pidC) throw new AssertionError("A e C precisam de transações independentes");
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            esperaA = EsperaJdbcPostgres.aguardar(jdbc, pidC, controle.atorA,
                    sql -> sql.selecaoChave("cargo", "id_cargo", controle.cargoId), "Cargo individual retido por C", a);
            b = pool.submit(() -> controle.atorB.executar(() -> executarCapturando(pedidoB, "HTTP_B", controle)));
            long pidB = controle.pidB.get(10, TimeUnit.SECONDS);
            if (pidB == pidA || pidB == pidC) throw new AssertionError("B precisa de conexão independente de A e C");
            long fimB = System.nanoTime() + TimeUnit.SECONDS.toNanos(12);
            while (System.nanoTime() < fimB) {
                esperaB = EsperaJdbcPostgres.observar(jdbc, pidA, controle.atorB,
                        sql -> sql.alteracaoNaChave("gestao_local", "id_gestao_local", controle.gestaoId),
                        "UPDATE da Gestão original pelo Admin B");
                if (b.isDone() || !esperaB.isEmpty()) break;
                Thread.sleep(20);
            }
            if (!b.isDone() && esperaB.isEmpty())
                throw new AssertionError("B não concluiu nem esperou no UPDATE da PK exata da Gestão");
            bTerminou = b.isDone();
            if (bTerminou) {
                // O Future só termina depois do advice/commit MVC. A releitura é
                // independente e ocorre com C ainda pausada na barreira.
                b.get();
                adminAntes = emTransacao(() -> administracao(c));
            }
            aPendente = !a.isDone(); cPendente = !terceiro.isDone();
            controle.liberarC.countDown();
            long fimCiclo = System.nanoTime() + TimeUnit.SECONDS.toNanos(12);
            while (!bTerminou && System.nanoTime() < fimCiclo && !a.isDone() && !b.isDone() && !terceiro.isDone()) {
                var comandoA = controle.atorA.comandoAtual();
                var comandoB = controle.atorB.comandoAtual();
                var comandoC = controle.atorC.comandoAtual();
                var ca = EsperaJdbcPostgres.observar(jdbc, pidC, controle.atorA,
                        sql -> sql.selecaoChave("cargo", "id_cargo", controle.cargoId), "A -> Cargo/C");
                var cb = EsperaJdbcPostgres.observar(jdbc, pidA, controle.atorB,
                        sql -> sql.alteracaoNaChave("gestao_local", "id_gestao_local", controle.gestaoId), "B -> Gestão/A");
                var cc = EsperaJdbcPostgres.observar(jdbc, pidB, controle.atorC,
                        sql -> sql.selecaoChave("usuario_aplicacao", "id", controle.executorId), "C -> executor/B");
                if (!ca.isEmpty() && !cb.isEmpty() && !cc.isEmpty()
                        && comandoA == controle.atorA.comandoAtual() && comandoB == controle.atorB.comandoAtual()
                        && comandoC == controle.atorC.comandoAtual()) {
                    List<Map<String, Object>> tres = new ArrayList<>(ca); tres.addAll(cb); tres.addAll(cc);
                    ciclo = List.copyOf(tres);
                    break;
                }
                Thread.sleep(10);
            }
        } catch (Throwable falha) {
            capturar("gate", falha, controle);
            falhaGate = falha.getClass().getName();
        } finally {
            controle.liberarC.countDown();
            resultadoA = recolher(a, "future_A", controle);
            resultadoB = recolher(b, "future_B", controle);
            resultadoC = recolher(terceiro, "future_C", controle);
            pool.shutdown();
            encerrado = pool.awaitTermination(10, TimeUnit.SECONDS);
            if (!encerrado) pool.shutdownNow();
        }
        return new Execucao(resultadoA, resultadoB, resultadoC, esperaA, esperaB, ciclo, falhaGate,
                bTerminou, aPendente, cPendente, adminAntes, encerrado);
    }

    private MvcResult executarCapturando(AbstractMockHttpServletRequestBuilder<?> pedido, String origem, Controle controle)
            throws Exception {
        try {
            MvcResult resultado = mvc.perform(pedido).andReturn();
            if (resultado.getResolvedException() != null) capturar(origem + ".resolvedException", resultado.getResolvedException(), controle);
            return resultado;
        } catch (Exception | Error falha) { capturar(origem, falha, controle); throw falha; }
    }

    private void capturar(String origem, Throwable primeira, Controle controle) {
        Set<Throwable> visitadas = Collections.newSetFromMap(new IdentityHashMap<>());
        var fila = new ArrayDeque<Throwable>(); fila.add(primeira);
        while (!fila.isEmpty()) {
            Throwable atual = fila.removeFirst();
            if (!visitadas.add(atual)) continue;
            controle.falhas.addIfAbsent(new FalhaTecnica(origem, atual.getClass().getName(),
                    atual instanceof SQLException sql ? sql.getSQLState() : null,
                    atual instanceof SQLException sql ? sql.getErrorCode() : null));
            if (atual instanceof SQLException sql && sql.getNextException() != null) fila.addLast(sql.getNextException());
            if (atual.getCause() != null) fila.addLast(atual.getCause());
            Collections.addAll(fila, atual.getSuppressed());
        }
    }

    private Resultado recolher(Future<MvcResult> futuro, String origem, Controle controle) {
        if (futuro == null) return new Resultado(null, "NAO_INICIADO");
        try { return new Resultado(futuro.get(15, TimeUnit.SECONDS), null); }
        catch (Exception falha) {
            capturar(origem, falha, controle);
            if (falha instanceof InterruptedException) Thread.currentThread().interrupt();
            return new Resultado(null, falha.getClass().getName() + "/"
                    + (falha.getCause() == null ? "SEM_CAUSA" : falha.getCause().getClass().getName()));
        }
    }

    private Estado estado(Cenario c, List<String> requests) {
        filaAuditoria.descarregar();
        return emTransacao(() -> {
            long gestaoId = c.gestaoA.getId();
            List<CargoEstado> cargos = em.createQuery("select c from Cargo c where c.gestaoLocal.id=:g order by c.id", Cargo.class)
                    .setParameter("g", gestaoId).getResultList().stream().map(e -> new CargoEstado(e.getId(), e.getNome(),
                            e.getCbo(), e.getDescricao(), e.getAtivo(), e.getVersion())).toList();
            List<EventoEstado> eventos = em.createQuery("select e from EventoDisponibilidadeCatalogo e where e.tipoCatalogo=:tipo "
                    + "and (e.catalogoId in (select c.id from Cargo c where c.gestaoLocal.id=:g) or e.responsavelId=:executor) order by e.id",
                    EventoDisponibilidadeCatalogo.class).setParameter("tipo", TipoCatalogoTemporal.CARGO)
                    .setParameter("g", gestaoId).setParameter("executor", c.localA.getId()).getResultList().stream()
                    .map(e -> new EventoEstado(e.getId(), e.getCatalogoId(), e.getDataEfetiva(), e.getAtivoAnterior(), e.getAtivo(),
                            e.getRegistradoEm(), e.getMotivo(), e.getEvidencia(), e.getAutorizacao(), e.getChaveOrigem(),
                            e.getChaveIdempotencia(), e.getHashIntencao(), e.getResponsavelId(), e.getResponsavelUsername(),
                            e.getAtivoAtualAposRegistro(), e.getVersaoCatalogoApos())).toList();
            List<AuditoriaEstado> auditorias = em.createQuery("select a from RegistroAuditoria a where a.entityType in ('Cargo','CARGO') "
                    + "and (a.action like 'CARGO_%' or a.action='CATALOGO_TRANSICAO_DOCUMENTADA') and (a.entityId in "
                    + "(select c.id from Cargo c where c.gestaoLocal.id=:g) or a.requestId in :requests) order by a.id", RegistroAuditoria.class)
                    .setParameter("g", gestaoId).setParameter("requests", requests).getResultList().stream()
                    .map(a -> new AuditoriaEstado(a.getId(), a.getEntityId(), a.getEntityType(), a.getAction(), a.getUserId(),
                            a.getCreatedAt(), a.getRequestId(), a.getDetailsJson() == null ? null
                            : UtilitarioHashSeguranca.sha256Hexadecimal(a.getDetailsJson()))).toList();
            return new Estado(new Negocio(cargos, eventos, auditorias), administracao(c));
        });
    }

    /** Chamado dentro de transação nova; não lê hash/senha/token/cookie. */
    private Administracao administracao(Cenario c) {
        UsuarioAplicacao u = em.find(UsuarioAplicacao.class, c.localA.getId());
        GestaoLocal g = em.find(GestaoLocal.class, c.gestaoA.getId());
        SuperintendenciaEstadual se = em.find(SuperintendenciaEstadual.class, c.seA.getId());
        long sessoes = em.createQuery("select count(t) from TokenAtivo t where t.username=:username", Long.class)
                .setParameter("username", c.localA.getUsername()).getSingleResult();
        return new Administracao(u.getId(), u.getPerfil(), u.getActive(),
                u.getSuperintendencia() == null ? null : u.getSuperintendencia().getId(),
                u.getGestaoLocal() == null ? null : u.getGestaoLocal().getId(), u.getVersion(), g.getId(), g.getAtivo(),
                g.getVersion(), se.getId(), se.getAtivo(), se.getVersion(), sessoes);
    }

    private void conferirAdmin(Administracao antes, Administracao depois, Cenario c) {
        assertThat(depois).as("Admin precisa commitar antes da liberação de C no GREEN").isNotNull();
        assertAll(() -> assertThat(depois.executorId()).isEqualTo(c.localA.getId()),
                () -> assertThat(depois.perfil()).isEqualTo(PerfilAcesso.GESTOR_ESTADUAL),
                () -> assertThat(depois.ativo()).isFalse(),
                () -> assertThat(depois.superintendenciaExecutor()).isEqualTo(c.seA.getId()),
                () -> assertThat(depois.gestaoExecutor()).isNull(),
                () -> assertThat(depois.versaoExecutor()).isGreaterThan(antes.versaoExecutor()),
                () -> assertThat(depois.gestaoId()).isEqualTo(c.gestaoA.getId()),
                () -> assertThat(depois.gestaoAtiva()).isFalse(),
                () -> assertThat(depois.versaoGestao()).isGreaterThan(antes.versaoGestao()),
                () -> assertThat(depois.superintendenciaId()).isEqualTo(c.seA.getId()),
                () -> assertThat(depois.superintendenciaAtiva()).isFalse(),
                () -> assertThat(depois.versaoSuperintendencia()).isGreaterThan(antes.versaoSuperintendencia()),
                () -> assertThat(depois.sessoesExecutor()).isZero());
    }

    private CargoEstado cargo(Estado estado, long id) {
        return estado.negocio().cargos().stream().filter(c -> c.id() == id).findFirst()
                .orElseThrow(() -> new AssertionError("Cargo novo não encontrado no readback"));
    }

    private Contador contadorDeadlocks() {
        Long n = new JdbcTemplate(dataSource).queryForObject("select deadlocks from pg_stat_database where datname=current_database()", Long.class);
        if (n == null) throw new AssertionError("Contador PostgreSQL da base corrente indisponível");
        return new Contador(Instant.now(), n);
    }

    private List<Contador> observarContador(Contador antes) throws InterruptedException {
        List<Contador> amostras = new ArrayList<>();
        long fim = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        do {
            Contador atual = contadorDeadlocks(); amostras.add(atual);
            if (atual.quantidade() > antes.quantidade()) break;
            Thread.sleep(50);
        } while (System.nanoTime() < fim);
        return List.copyOf(amostras);
    }

    private Map<String, List<String>> conferirPostgres() throws Exception {
        try (var conexao = dataSource.getConnection()) {
            assertThat(conexao.getMetaData().getDatabaseProductName()).isEqualTo("PostgreSQL");
        }
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        List<String> cargo = pk(jdbc, "cargo"), gestao = pk(jdbc, "gestao_local"), usuario = pk(jdbc, "usuario_aplicacao");
        assertThat(cargo).containsExactly("id_cargo");
        assertThat(gestao).containsExactly("id_gestao_local");
        assertThat(usuario).containsExactly("id");
        return Map.of("cargo", cargo, "gestao_local", gestao, "usuario_aplicacao", usuario);
    }

    private List<String> pk(JdbcTemplate jdbc, String tabela) {
        return jdbc.query("""
                select a.attname from pg_index i join pg_class t on t.oid=i.indrelid join pg_namespace n on n.oid=t.relnamespace
                join pg_attribute a on a.attrelid=t.oid and a.attnum=any(i.indkey)
                where n.nspname=current_schema() and t.relname=? and i.indisprimary order by a.attnum
                """, (r, n) -> r.getString(1), tabela);
    }

    private long pid() { return ((Number) em.createNativeQuery("select pg_backend_pid()").getSingleResult()).longValue(); }
    private Long pidRegistrado(CompletableFuture<Long> pid) { return pid.isDone() && !pid.isCompletedExceptionally() ? pid.getNow(null) : null; }
    private Integer status(Resultado r) { return r.http() == null ? null : r.http().getResponse().getStatus(); }
    private Map<String, Object> resumo(Resultado r) { return mapa("statusHttp", status(r), "falhaClasse", r.falha()); }

    private List<Map<String, Object>> semSql(List<Map<String, Object>> linhas) {
        return linhas.stream().map(linha -> {
            Map<String, Object> copia = new LinkedHashMap<>(linha);
            Object sql = copia.remove("sql_preparado");
            copia.put("sql_preparado_sha256", sql == null ? null : UtilitarioHashSeguranca.sha256Hexadecimal(sql.toString()));
            return copia;
        }).toList();
    }

    private void registrar(String caso, Object evidencia) throws Exception {
        Path pasta = Path.of("target/cargo-ordem-namespace").toAbsolutePath();
        var permissoes = PosixFilePermissions.fromString("rwx------");
        Files.createDirectories(pasta, PosixFilePermissions.asFileAttribute(permissoes));
        Files.setPosixFilePermissions(pasta, permissoes);
        Path arquivo = Files.createFile(pasta.resolve(caso + "-" + UUID.randomUUID() + ".json"),
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
        byte[] bytes = (mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapa("caso", caso,
                "registradoEm", Instant.now(), "auditRound", System.getenv("SGT_AUDITORIA_RODADA"),
                "limites", "MockMvc/serviços/JPA/PostgreSQL reais; três workers e pool existente8. "
                        + "Fixture contemporânea, Cargo novo sem composições; ato de hoje sem token de retroatividade. "
                        + "Contador de deadlocks abrange a base inteira e é assíncrono: execução central exclusiva. "
                        + "SQLSTATE40P01 é RED mesmo com HTTP409; não se exige ausência de todo SQLSTATE. "
                        + "Sem SQL bruto, mensagens técnicas, senhas, tokens/cookies ou payload de auditoria. "
                        + "Sessões comprovadas no banco, sem jornada de navegador; auditoria HTTP negativa pode existir.",
                "evidencia", evidencia)) + "\n").getBytes(StandardCharsets.UTF_8);
        try (FileChannel canal = FileChannel.open(arquivo, StandardOpenOption.WRITE)) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            while (buffer.hasRemaining()) canal.write(buffer);
            canal.force(true);
        }
        try (FileChannel diretorio = FileChannel.open(pasta, StandardOpenOption.READ)) { diretorio.force(true); }
    }

    private static final class Controle {
        final long executorId, gestaoId, cargoId;
        final CountDownLatch liberarC = new CountDownLatch(1);
        final AtomicReference<Thread> threadC = new AtomicReference<>();
        final AtomicBoolean barreiraC = new AtomicBoolean();
        final CompletableFuture<Long> pidA = new CompletableFuture<>(), pidB = new CompletableFuture<>(), pidC = new CompletableFuture<>();
        final Ator atorA = new Ator("ordem-A-PUT-Cargo"), atorB = new Ator("ordem-B-Admin"), atorC = new Ator("ordem-C-Transicao");
        final CopyOnWriteArrayList<FalhaTecnica> falhas = new CopyOnWriteArrayList<>();
        Controle(long executorId, long gestaoId, long cargoId) {
            this.executorId = executorId; this.gestaoId = gestaoId; this.cargoId = cargoId;
        }
    }

    private record Preparado(Cenario cenario, long cargoId, long versao) { }
    private record Resultado(MvcResult http, String falha) { }
    private record Execucao(Resultado a, Resultado b, Resultado c, List<Map<String, Object>> esperaA,
                            List<Map<String, Object>> esperaB, List<Map<String, Object>> ciclo, String falhaGate,
                            boolean bTerminouAntes, boolean aPendenteAntes, boolean cPendenteAntes,
                            Administracao adminAntesLiberar, boolean encerrado) { }
    private record FalhaTecnica(String origem, String classe, String sqlState, Integer codigo) { }
    private record Contador(Instant observadoEm, long quantidade) { }
    private record ControlePositivo(int statusC, int statusA, int statusB, Estado antes, Estado depoisC, Estado depoisA, Estado depoisB) { }
    private record Estado(Negocio negocio, Administracao administracao) { }
    private record Negocio(List<CargoEstado> cargos, List<EventoEstado> eventos, List<AuditoriaEstado> auditorias) { }
    private record Administracao(long executorId, PerfilAcesso perfil, Boolean ativo, Long superintendenciaExecutor,
                                 Long gestaoExecutor, long versaoExecutor, long gestaoId, Boolean gestaoAtiva, long versaoGestao,
                                 long superintendenciaId, Boolean superintendenciaAtiva, long versaoSuperintendencia, long sessoesExecutor) { }
    private record CargoEstado(long id, String nome, String cbo, String descricao, Boolean ativo, long versao) { }
    private record AuditoriaEstado(long id, Long cargoId, String tipo, String acao, Long executorId, Instant criadoEm,
                                  String requestId, String detalhesSha256) { }
    private record EventoEstado(long id, Long cargoId, LocalDate dataEfetiva, Boolean ativoAnterior, Boolean ativo,
                                Instant registradoEm, String motivo, String evidencia, String autorizacao,
                                String chaveOrigem, String chaveIdempotencia, String hashIntencao, Long responsavelId,
                                String responsavelUsername, Boolean ativoAtualAposRegistro, Long versaoCatalogoApos) { }
}
