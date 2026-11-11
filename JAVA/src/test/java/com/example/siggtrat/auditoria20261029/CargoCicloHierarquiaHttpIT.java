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
import com.example.siggtrat.servico.CargoServico;
import com.example.siggtrat.servico.DisponibilidadeCatalogoTemporalServico;
import com.example.siggtrat.servico.FilaRegistroAuditoria;
import com.example.siggtrat.servico.UsuarioAplicacaoServico;
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
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Regressão da ordem Gestão(namespace Cargo) -> executor contra a alteração
 * administrativa executor -> Gestão da mesma superintendência. HTTP409 não
 * basta: o handler também pode converter deadlock em conflito. SQLSTATE40P01,
 * contador PostgreSQL e as duas arestas físicas permanecem na evidência RED.
 * Todos os spies chamam os métodos reais completos; somente a conclusão da
 * transição CARGO é retida antes de CargoServico revalidar o executor.
 * Draft não executado: incorporação/compilação/RED/fix/GREEN são centrais.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CargoCicloHierarquiaHttpIT extends ContratosHttpBase {
    @Autowired private DataSource dataSource;
    @Autowired private FilaRegistroAuditoria filaAuditoria;
    @MockitoSpyBean private DisponibilidadeCatalogoTemporalServico disponibilidade;
    @MockitoSpyBean private CargoServico cargoServico;
    @MockitoSpyBean private UsuarioAplicacaoServico usuarioServico;

    @Test
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void cargoEInativacaoAdministrativaDaHierarquiaResolvemConflitoSemDeadlockNemEscritaParcial() throws Exception {
        Map<String, List<String>> chavesPrimarias = conferirPostgres();
        ControleSequencial sequencial = controlarComandoAdministrativoSemDisputa();
        Cenario c = criarCenarioPersistido();
        String requestCargo = UUID.randomUUID().toString();
        String requestAdmin = UUID.randomUUID().toString();
        String nome = "Cargo ciclo hierarquia " + UUID.randomUUID();
        // Sessões e requests são preparados no sequencial; não há computeIfAbsent
        // concorrente no cache de autenticação da fixture.
        var pedidoA = autenticar(post("/api/cargos").contentType(APPLICATION_JSON)
                .header(FiltroIdentificadorRequisicao.REQUEST_ID_HEADER, requestCargo)
                .content(json(mapa("nome", nome, "ativo", true))), c.localA);
        var pedidoB = autenticar(comandoAdministrativo(c)
                .header(FiltroIdentificadorRequisicao.REQUEST_ID_HEADER, requestAdmin), c.admin);
        Estado antes = estado(c, requestCargo, null);
        assertThat(antes.administracao().sessoesExecutor()).as("Sessão real do executor existe antes da revogação").isPositive();
        Controle controle = new Controle(c.localA.getId(), c.gestaoA.getId(), nome);
        instalarSpies(controle);
        Contador antesDeadlocks = contadorDeadlocks();
        Execucao execucao = disputar(controle, pedidoA, pedidoB);
        List<Contador> amostrasDepois = observarContadorDepoisDasTransacoes(antesDeadlocks);
        Contador depoisDeadlocks = amostrasDepois.get(amostrasDepois.size() - 1);
        Estado depois = estado(c, requestCargo, controle.cargoTentativa.get());
        List<FalhaSql> estadosSql = List.copyOf(controle.falhasSql);
        boolean stateDeadlock = estadosSql.stream().anyMatch(f -> "40P01".equals(f.sqlState()));
        boolean contadorAumentou = depoisDeadlocks.quantidade() > antesDeadlocks.quantidade();
        String classificacao = stateDeadlock ? "RED_SQLSTATE_40P01"
                : contadorAumentou && !execucao.cicloDuplo().isEmpty() ? "RED_CONTADOR_E_DUAS_ARESTAS"
                : contadorAumentou ? "CONTADOR_AUMENTOU_SEM_ATRIBUICAO_COMPLETA"
                : "SEM_DEADLOCK_OBSERVADO";
        registrar("concorrente", mapa("controleSequencial", sequencial, "antes", antes, "depois", depois,
                "chavesPrimariasVerificadas", chavesPrimarias,
                "requestCargo", requestCargo, "requestAdmin", requestAdmin,
                "cargoTentativaId", controle.cargoTentativa.get(), "resultadoA", resumo(execucao.a()),
                "resultadoB", resumo(execucao.b()), "pidA", pidRegistrado(controle.pidA),
                "pidB", pidRegistrado(controle.pidB), "etapaA", controle.etapaA.get(), "etapaB", controle.etapaB.get(),
                "esperaBNoUpdateDaGestao", semSql(execucao.esperaB()), "cicloDuplo", semSql(execucao.cicloDuplo()),
                "aPendenteAntesDeLiberar", execucao.aPendenteAntesDeLiberar(),
                "bPendenteAntesDeLiberar", execucao.bPendenteAntesDeLiberar(), "falhaGate", execucao.falhaGate(),
                "workersEncerrados", execucao.encerrado(), "sqlStates", estadosSql,
                "cadeiasThrowableSemMensagens", List.copyOf(controle.cadeiasThrowable),
                "deadlocksAntes", antesDeadlocks, "deadlocksDepois", amostrasDepois,
                "classificacaoObservada", classificacao));

        assertAll("Conflito Cargo/hierarquia não pode depender de uma vítima de deadlock",
                () -> assertThat(execucao.falhaGate()).isNull(),
                () -> assertThat(execucao.a().falha()).isNull(),
                () -> assertThat(execucao.b().falha()).isNull(),
                () -> assertThat(execucao.aPendenteAntesDeLiberar()).isTrue(),
                () -> assertThat(execucao.bPendenteAntesDeLiberar()).isTrue(),
                () -> assertThat(execucao.esperaB()).as("B aguarda UPDATE da PK exata da Gestão já retida por A").isNotEmpty(),
                () -> assertThat(execucao.encerrado()).isTrue(),
                () -> assertThat(status(execucao.a())).isEqualTo(409),
                () -> assertThat(execucao.a().http().getResponse().getContentAsString(StandardCharsets.UTF_8))
                        .contains("CONCURRENT_OPERATION"),
                () -> assertThat(status(execucao.b())).isEqualTo(200),
                // 55P03/NOWAIT é um conflito esperado; não exigir lista SQL vazia.
                () -> assertThat(estadosSql).extracting(FalhaSql::sqlState).doesNotContain("40P01"),
                () -> assertThat(amostrasDepois).allSatisfy(a -> assertThat(a.quantidade()).isEqualTo(antesDeadlocks.quantidade())),
                () -> assertThat(depois.negocio()).as("Cargo, versões, eventos e CARGO_* inclusive órfãos permanecem iguais")
                        .isEqualTo(antes.negocio()),
                () -> conferirAdministracaoCommitada(antes.administracao(), depois.administracao(), c));
    }

    /** A mesma combinação de perfil/inativação precisa ser legítima sem disputa. */
    private ControleSequencial controlarComandoAdministrativoSemDisputa() throws Exception {
        Cenario c = criarCenarioPersistido();
        String requestId = UUID.randomUUID().toString();
        var sessao = autenticar(get("/api/usuarios/meu-perfil"), c.localA);
        MvcResult sessaoAntes = mvc.perform(sessao).andReturn();
        var pedido = autenticar(comandoAdministrativo(c), c.admin);
        Estado antes = estado(c, requestId, null);
        MvcResult resposta = mvc.perform(pedido).andReturn();
        Estado depois = estado(c, requestId, null);
        ControleSequencial resultado = new ControleSequencial(sessaoAntes.getResponse().getStatus(),
                resposta.getResponse().getStatus(), antes, depois);
        registrar("controle-sequencial", resultado);
        assertAll("Pré-condição real do PUT administrativo combinado",
                () -> assertThat(resultado.statusSessaoAntes()).isEqualTo(200),
                () -> assertThat(resultado.statusAdmin()).isEqualTo(200),
                () -> assertThat(antes.administracao().sessoesExecutor()).isPositive(),
                () -> assertThat(depois.negocio()).isEqualTo(antes.negocio()),
                () -> conferirAdministracaoCommitada(antes.administracao(), depois.administracao(), c));
        return resultado;
    }

    private AbstractMockHttpServletRequestBuilder<?> comandoAdministrativo(Cenario c) throws Exception {
        return put("/api/usuarios/" + c.localA.getId()).contentType(APPLICATION_JSON)
                .content(json(mapa("perfil", "GESTOR_ESTADUAL", "active", false,
                        "superintendenciaId", c.seA.getId())));
    }

    private void instalarSpies(Controle controle) throws Exception {
        DisponibilidadeCatalogoTemporalServico disponibilidadeSpy = AopTestUtils.getUltimateTargetObject(disponibilidade);
        CargoServico cargoSpy = AopTestUtils.getUltimateTargetObject(cargoServico);
        UsuarioAplicacaoServico usuarioSpy = AopTestUtils.getUltimateTargetObject(usuarioServico);
        assertThat(org.mockito.Mockito.mockingDetails(disponibilidadeSpy).isSpy()).isTrue();
        assertThat(org.mockito.Mockito.mockingDetails(cargoSpy).isSpy()).isTrue();
        assertThat(org.mockito.Mockito.mockingDetails(usuarioSpy).isSpy()).isTrue();
        doAnswer(i -> {
            TipoCatalogoTemporal tipo = i.getArgument(0);
            UsuarioAplicacao responsavel = i.getArgument(5);
            if (tipo != TipoCatalogoTemporal.CARGO || responsavel == null
                    || !Long.valueOf(controle.executorId).equals(responsavel.getId())) return i.callRealMethod();
            try {
                assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
                long entrada = pid();
                Object resultado = i.callRealMethod();
                // O método real inteiro já gravou a transição. Ainda não houve
                // retorno a CargoServico nem sua revalidação do executor.
                em.flush();
                assertThat(pid()).isEqualTo(entrada);
                controle.cargoTentativa.set(i.getArgument(1, Long.class));
                controle.etapaA.set("TRANSICAO_CARGO_REAL_FLUSH_ANTES_REVALIDAR_EXECUTOR");
                controle.pidA.complete(entrada);
                if (!controle.liberarA.await(30, TimeUnit.SECONDS))
                    throw new AssertionError("Fixture não liberou A após a transição real");
                controle.etapaA.set("RETORNOU_TRANSICAO_SEGUE_REVALIDACAO_REAL");
                return resultado;
            } catch (Throwable falha) {
                capturarSql("Disponibilidade.registrarTransicao", falha, controle);
                controle.pidA.completeExceptionally(falha);
                throw falha;
            }
        }).when(disponibilidadeSpy).registrarTransicao(any(TipoCatalogoTemporal.class), anyLong(),
                nullable(Boolean.class), anyBoolean(), anyString(), any(UsuarioAplicacao.class));
        doAnswer(i -> {
            CargoDTO dto = i.getArgument(0);
            if (dto == null || !controle.nomeCargo.equals(dto.nome())) return i.callRealMethod();
            try { return i.callRealMethod(); }
            catch (Throwable falha) {
                capturarSql("CargoServico.criar", falha, controle);
                controle.pidA.completeExceptionally(falha);
                throw falha;
            }
        }).when(cargoSpy).criar(any(CargoDTO.class));
        doAnswer(i -> {
            Long id = i.getArgument(0);
            if (!Long.valueOf(controle.executorId).equals(id)) return i.callRealMethod();
            try {
                assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
                controle.pidB.complete(pid());
                controle.etapaB.set("ENTROU_ATUALIZACAO_ADMIN_REAL");
                Object resultado = i.callRealMethod();
                controle.etapaB.set("ATUALIZACAO_ADMIN_REAL_RETORNOU");
                return resultado;
            } catch (Throwable falha) {
                capturarSql("UsuarioAplicacaoServico.atualizar", falha, controle);
                controle.pidB.completeExceptionally(falha);
                throw falha;
            }
        }).when(usuarioSpy).atualizar(anyLong(), any(AtualizacaoUsuarioAplicacaoDTO.class));
    }

    private Execucao disputar(Controle controle, AbstractMockHttpServletRequestBuilder<?> pedidoA,
                              AbstractMockHttpServletRequestBuilder<?> pedidoB) throws Exception {
        var pool = Executors.newFixedThreadPool(2);
        Future<MvcResult> a = null;
        Future<MvcResult> b = null;
        List<Map<String, Object>> esperaB = List.of();
        List<Map<String, Object>> ciclo = List.of();
        String falhaGate = null;
        boolean aPendente = false;
        boolean bPendente = false;
        Resultado resultadoA;
        Resultado resultadoB;
        boolean encerrado;
        try {
            a = pool.submit(() -> controle.atorA.executar(() -> executarCapturando(pedidoA, "HTTP_A", controle)));
            long pidA = controle.pidA.get(15, TimeUnit.SECONDS);
            b = pool.submit(() -> controle.atorB.executar(() -> executarCapturando(pedidoB, "HTTP_B", controle)));
            long pidB = controle.pidB.get(10, TimeUnit.SECONDS);
            if (pidA == pidB) throw new AssertionError("A e B precisam de transações/conexões independentes");
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            esperaB = EsperaJdbcPostgres.aguardar(jdbc, pidA, controle.atorB,
                    sql -> sql.alteracaoNaChave("gestao_local", "id_gestao_local", controle.gestaoId),
                    "UPDATE da Gestão original durante PUT administrativo do executor", b);
            aPendente = !a.isDone(); bPendente = !b.isDone();
            controle.liberarA.countDown();
            // No RED, a segunda aresta fecha o ciclo físico antes da vítima.
            // No GREEN, NOWAIT pode retornar55P03 antes de qualquer amostra A.
            long fim = System.nanoTime() + TimeUnit.SECONDS.toNanos(12);
            while (System.nanoTime() < fim && !a.isDone() && !b.isDone()) {
                var comandoA = controle.atorA.comandoAtual();
                var comandoB = controle.atorB.comandoAtual();
                var aEsperaUser = EsperaJdbcPostgres.observar(jdbc, pidB, controle.atorA,
                        sql -> sql.selecaoChave("usuario_aplicacao", "id", controle.executorId),
                        "revalidação do executor original pelo Cargo");
                var bEsperaGestao = EsperaJdbcPostgres.observar(jdbc, pidA, controle.atorB,
                        sql -> sql.alteracaoNaChave("gestao_local", "id_gestao_local", controle.gestaoId),
                        "UPDATE da Gestão original pelo administrador");
                if (!aEsperaUser.isEmpty() && !bEsperaGestao.isEmpty()
                        && controle.atorA.comandoAtual() == comandoA && controle.atorB.comandoAtual() == comandoB) {
                    List<Map<String, Object>> duas = new ArrayList<>(aEsperaUser);
                    duas.addAll(bEsperaGestao);
                    ciclo = List.copyOf(duas);
                    break;
                }
                Thread.sleep(10);
            }
        } catch (Throwable falha) {
            capturarSql("gate", falha, controle);
            falhaGate = falha.getClass().getSimpleName();
        } finally {
            // Não interromper A antes de liberar: isso fabricaria rollback.
            controle.liberarA.countDown();
            resultadoA = recolher(a, "future_A", controle);
            resultadoB = recolher(b, "future_B", controle);
            pool.shutdown();
            encerrado = pool.awaitTermination(10, TimeUnit.SECONDS);
            if (!encerrado) pool.shutdownNow();
        }
        return new Execucao(resultadoA, resultadoB, esperaB, ciclo, falhaGate, aPendente, bPendente, encerrado);
    }

    private MvcResult executarCapturando(AbstractMockHttpServletRequestBuilder<?> pedido, String origem, Controle controle)
            throws Exception {
        try {
            MvcResult resultado = mvc.perform(pedido).andReturn();
            if (resultado.getResolvedException() != null) capturarSql(origem + ".resolvedException", resultado.getResolvedException(), controle);
            return resultado;
        } catch (Exception | Error falha) {
            capturarSql(origem, falha, controle);
            throw falha;
        }
    }

    /** Inclui SQLException.nextException; nunca coleta mensagens, argumentos ou SQL. */
    private void capturarSql(String origem, Throwable primeira, Controle controle) {
        Set<Throwable> visitadas = Collections.newSetFromMap(new IdentityHashMap<>());
        var fila = new ArrayDeque<Throwable>();
        fila.add(primeira);
        while (!fila.isEmpty()) {
            Throwable atual = fila.removeFirst();
            if (!visitadas.add(atual)) continue;
            controle.cadeiasThrowable.addIfAbsent(new NoFalha(origem, atual.getClass().getName(),
                    atual instanceof SQLException sql ? sql.getSQLState() : null,
                    atual instanceof SQLException sql ? sql.getErrorCode() : null));
            if (atual instanceof SQLException sql) {
                controle.falhasSql.addIfAbsent(new FalhaSql(origem, sql.getClass().getName(), sql.getSQLState(), sql.getErrorCode()));
                if (sql.getNextException() != null) fila.addLast(sql.getNextException());
            }
            if (atual.getCause() != null) fila.addLast(atual.getCause());
            Collections.addAll(fila, atual.getSuppressed());
        }
    }

    private Resultado recolher(Future<MvcResult> futuro, String origem, Controle controle) {
        if (futuro == null) return new Resultado(null, "NAO_INICIADO");
        try { return new Resultado(futuro.get(15, TimeUnit.SECONDS), null); }
        catch (Exception falha) {
            capturarSql(origem, falha, controle);
            if (falha instanceof InterruptedException) Thread.currentThread().interrupt();
            return new Resultado(null, falha.getClass().getName() + "/"
                    + (falha.getCause() == null ? "SEM_CAUSA" : falha.getCause().getClass().getName()));
        }
    }

    private Estado estado(Cenario c, String requestCargo, Long cargoTentativa) {
        filaAuditoria.descarregar();
        return emTransacao(() -> {
            long tentativa = cargoTentativa == null ? -1L : cargoTentativa;
            long gestaoId = c.gestaoA.getId();
            long executorId = c.localA.getId();
            List<CargoEstado> cargos = em.createQuery("select c from Cargo c where c.gestaoLocal.id=:g order by c.id", Cargo.class)
                    .setParameter("g", gestaoId).getResultList().stream()
                    .map(e -> new CargoEstado(e.getId(), e.getNome(), e.getCbo(), e.getDescricao(), e.getAtivo(), e.getVersion())).toList();
            List<EventoEstado> eventos = em.createQuery("select e from EventoDisponibilidadeCatalogo e where "
                    + "e.tipoCatalogo=:tipo and (e.catalogoId in (select c.id from Cargo c where c.gestaoLocal.id=:g) "
                    + "or e.catalogoId=:tentativa or e.responsavelId=:executor) order by e.id", EventoDisponibilidadeCatalogo.class)
                    .setParameter("tipo", TipoCatalogoTemporal.CARGO).setParameter("g", gestaoId)
                    .setParameter("tentativa", tentativa).setParameter("executor", executorId)
                    .getResultList().stream().map(e -> new EventoEstado(e.getId(), e.getCatalogoId(), e.getDataEfetiva(),
                            e.getAtivoAnterior(), e.getAtivo(), e.getRegistradoEm(), e.getMotivo(), e.getEvidencia(), e.getAutorizacao(),
                            e.getChaveOrigem(), e.getChaveIdempotencia(), e.getHashIntencao(), e.getResponsavelId(),
                            e.getResponsavelUsername(), e.getAtivoAtualAposRegistro(), e.getVersaoCatalogoApos())).toList();
            List<AuditoriaEstado> auditorias = em.createQuery("select a from RegistroAuditoria a where a.entityType='Cargo' "
                    + "and a.action like 'CARGO_%' and (a.entityId in (select c.id from Cargo c where c.gestaoLocal.id=:g) "
                    + "or a.entityId=:tentativa or a.requestId=:requestId) order by a.id", RegistroAuditoria.class)
                    .setParameter("g", gestaoId).setParameter("tentativa", tentativa).setParameter("requestId", requestCargo)
                    .getResultList().stream().map(a -> new AuditoriaEstado(a.getId(), a.getEntityId(), a.getAction(), a.getUserId(),
                            a.getCreatedAt(), a.getRequestId(), a.getDetailsJson() == null ? null
                            : UtilitarioHashSeguranca.sha256Hexadecimal(a.getDetailsJson()))).toList();
            UsuarioAplicacao u = em.find(UsuarioAplicacao.class, executorId);
            GestaoLocal g = em.find(GestaoLocal.class, gestaoId);
            SuperintendenciaEstadual se = em.find(SuperintendenciaEstadual.class, c.seA.getId());
            long sessoes = em.createQuery("select count(t) from TokenAtivo t where t.username=:username", Long.class)
                    .setParameter("username", c.localA.getUsername()).getSingleResult();
            Administracao administracao = new Administracao(u.getId(), u.getPerfil(), u.getActive(),
                    u.getSuperintendencia() == null ? null : u.getSuperintendencia().getId(),
                    u.getGestaoLocal() == null ? null : u.getGestaoLocal().getId(), u.getVersion(), u.getForceChangePassword(),
                    g.getId(), g.getAtivo(), g.getVersion(), se.getId(), se.getAtivo(), se.getVersion(), sessoes);
            return new Estado(new Negocio(cargos, eventos, auditorias), administracao);
        });
    }

    private void conferirAdministracaoCommitada(Administracao antes, Administracao depois, Cenario c) {
        assertAll(() -> assertThat(depois.executorId()).isEqualTo(c.localA.getId()),
                () -> assertThat(depois.perfil()).isEqualTo(PerfilAcesso.GESTOR_ESTADUAL),
                () -> assertThat(depois.ativo()).isFalse(),
                () -> assertThat(depois.superintendenciaExecutor()).isEqualTo(c.seA.getId()),
                () -> assertThat(depois.gestaoExecutor()).isNull(),
                () -> assertThat(depois.versaoExecutor()).isGreaterThan(antes.versaoExecutor()),
                () -> assertThat(depois.forcarTrocaSenha()).isTrue(),
                () -> assertThat(depois.gestaoId()).isEqualTo(c.gestaoA.getId()),
                () -> assertThat(depois.gestaoAtiva()).isFalse(),
                () -> assertThat(depois.versaoGestao()).isGreaterThan(antes.versaoGestao()),
                () -> assertThat(depois.superintendenciaId()).isEqualTo(c.seA.getId()),
                () -> assertThat(depois.superintendenciaAtiva()).isFalse(),
                () -> assertThat(depois.versaoSuperintendencia()).isGreaterThan(antes.versaoSuperintendencia()),
                () -> assertThat(depois.sessoesExecutor()).as("Sessões persistidas revogadas pela inativação real").isZero());
    }

    private Contador contadorDeadlocks() {
        Long valor = new JdbcTemplate(dataSource).queryForObject(
                "select deadlocks from pg_stat_database where datname=current_database()", Long.class);
        if (valor == null) throw new AssertionError("Contador PostgreSQL da base corrente indisponível");
        return new Contador(Instant.now(), valor);
    }

    private List<Contador> observarContadorDepoisDasTransacoes(Contador antes) throws InterruptedException {
        // Estatísticas PostgreSQL são publicadas de forma assíncrona. A janela
        // serve ao contador complementar; a prova de lock vem do JDBC/pg_locks.
        List<Contador> amostras = new ArrayList<>();
        long fim = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        do {
            Contador atual = contadorDeadlocks();
            amostras.add(atual);
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
        List<String> gestao = pk(jdbc, "gestao_local");
        List<String> usuario = pk(jdbc, "usuario_aplicacao");
        assertThat(gestao).containsExactly("id_gestao_local");
        assertThat(usuario).containsExactly("id");
        return Map.of("gestao_local", gestao, "usuario_aplicacao", usuario);
    }

    private List<String> pk(JdbcTemplate jdbc, String tabela) {
        return jdbc.query("""
                select a.attname from pg_index i join pg_class t on t.oid=i.indrelid
                join pg_namespace n on n.oid=t.relnamespace
                join pg_attribute a on a.attrelid=t.oid and a.attnum=any(i.indkey)
                where n.nspname=current_schema() and t.relname=? and i.indisprimary order by a.attnum
                """, (r, n) -> r.getString(1), tabela);
    }

    private long pid() { return ((Number) em.createNativeQuery("select pg_backend_pid()").getSingleResult()).longValue(); }
    private Long pidRegistrado(CompletableFuture<Long> pid) { return pid.isDone() && !pid.isCompletedExceptionally() ? pid.getNow(null) : null; }
    private Integer status(Resultado resultado) { return resultado.http() == null ? null : resultado.http().getResponse().getStatus(); }
    private Map<String, Object> resumo(Resultado resultado) { return mapa("statusHttp", status(resultado), "falhaClasse", resultado.falha()); }

    private List<Map<String, Object>> semSql(List<Map<String, Object>> linhas) {
        return linhas.stream().map(linha -> {
            Map<String, Object> copia = new LinkedHashMap<>(linha);
            Object sql = copia.remove("sql_preparado");
            copia.put("sql_preparado_sha256", sql == null ? null : UtilitarioHashSeguranca.sha256Hexadecimal(sql.toString()));
            return copia;
        }).toList();
    }

    private void registrar(String caso, Object evidencia) throws Exception {
        Path pasta = Path.of("target/cargo-ciclo-hierarquia").toAbsolutePath();
        var permissaoPasta = PosixFilePermissions.fromString("rwx------");
        Files.createDirectories(pasta, PosixFilePermissions.asFileAttribute(permissaoPasta));
        Files.setPosixFilePermissions(pasta, permissaoPasta);
        Path arquivo = Files.createFile(pasta.resolve(caso + "-" + UUID.randomUUID() + ".json"),
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
        byte[] bytes = (mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapa("caso", caso,
                "registradoEm", Instant.now(), "auditRound", System.getenv("SGT_AUDITORIA_RODADA"),
                "limites", "MockMvc/serviços/JPA/PostgreSQL reais, fixture contemporânea e base isolada. "
                        + "Contador deadlocks abrange toda a base e exige exclusividade da execução central; "
                        + "a publicação das estatísticas é assíncrona.40P01 é RED mesmo com HTTP409.55P03 é conflito esperado. "
                        + "Snapshots não contêm senha, token, cookie, SQL bruto nem payload de auditoria. "
                        + "Revogação comprovada nas sessões persistidas; não é jornada em navegador. "
                        + "Auditorias HTTP negativas podem existir fora de CARGO_*.", "evidencia", evidencia)) + "\n")
                .getBytes(StandardCharsets.UTF_8);
        try (FileChannel canal = FileChannel.open(arquivo, StandardOpenOption.WRITE)) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            while (buffer.hasRemaining()) canal.write(buffer);
            canal.force(true);
        }
        try (FileChannel diretorio = FileChannel.open(pasta, StandardOpenOption.READ)) { diretorio.force(true); }
    }

    private static final class Controle {
        final long executorId;
        final long gestaoId;
        final String nomeCargo;
        final CountDownLatch liberarA = new CountDownLatch(1);
        final CompletableFuture<Long> pidA = new CompletableFuture<>();
        final CompletableFuture<Long> pidB = new CompletableFuture<>();
        final AtomicReference<Long> cargoTentativa = new AtomicReference<>();
        final AtomicReference<String> etapaA = new AtomicReference<>("ANTES_HTTP");
        final AtomicReference<String> etapaB = new AtomicReference<>("ANTES_HTTP");
        final CopyOnWriteArrayList<FalhaSql> falhasSql = new CopyOnWriteArrayList<>();
        final CopyOnWriteArrayList<NoFalha> cadeiasThrowable = new CopyOnWriteArrayList<>();
        final Ator atorA = new Ator("cargo-hierarquia-A");
        final Ator atorB = new Ator("cargo-hierarquia-admin-B");
        Controle(long executorId, long gestaoId, String nomeCargo) {
            this.executorId = executorId; this.gestaoId = gestaoId; this.nomeCargo = nomeCargo;
        }
    }

    private record Resultado(MvcResult http, String falha) { }
    private record Execucao(Resultado a, Resultado b, List<Map<String, Object>> esperaB,
                            List<Map<String, Object>> cicloDuplo, String falhaGate,
                            boolean aPendenteAntesDeLiberar, boolean bPendenteAntesDeLiberar, boolean encerrado) { }
    private record FalhaSql(String origem, String classe, String sqlState, int codigo) { }
    private record NoFalha(String origem, String classe, String sqlState, Integer codigo) { }
    private record Contador(Instant observadoEm, long quantidade) { }
    private record ControleSequencial(int statusSessaoAntes, int statusAdmin, Estado antes, Estado depois) { }
    private record Estado(Negocio negocio, Administracao administracao) { }
    private record Negocio(List<CargoEstado> cargos, List<EventoEstado> eventos, List<AuditoriaEstado> auditorias) { }
    private record Administracao(long executorId, PerfilAcesso perfil, Boolean ativo, Long superintendenciaExecutor,
                                 Long gestaoExecutor, long versaoExecutor, Boolean forcarTrocaSenha, long gestaoId,
                                 Boolean gestaoAtiva, long versaoGestao, long superintendenciaId, Boolean superintendenciaAtiva,
                                 long versaoSuperintendencia, long sessoesExecutor) { }
    private record CargoEstado(long id, String nome, String cbo, String descricao, Boolean ativo, long versao) { }
    private record AuditoriaEstado(long id, Long cargoId, String acao, Long executorId, Instant criadoEm,
                                  String requestId, String detalhesSha256) { }
    private record EventoEstado(long id, Long cargoId, LocalDate dataEfetiva, Boolean ativoAnterior, Boolean ativo,
                                Instant registradoEm, String motivo, String evidencia, String autorizacao,
                                String chaveOrigem, String chaveIdempotencia, String hashIntencao, Long responsavelId,
                                String responsavelUsername, Boolean ativoAtualAposRegistro, Long versaoCatalogoApos) { }
}
