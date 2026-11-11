package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.configuracao.FiltroIdentificadorRequisicao;
import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.example.siggtrat.dto.CargoDTO;
import com.example.siggtrat.entidade.Cargo;
import com.example.siggtrat.entidade.EventoDisponibilidadeCatalogo;
import com.example.siggtrat.entidade.GestaoLocal;
import com.example.siggtrat.entidade.RegistroAuditoria;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.infra.EsperaJdbcPostgres;
import com.example.siggtrat.infra.ObservadorAtorPostgres.Ator;
import com.example.siggtrat.servico.CargoServico;
import com.example.siggtrat.servico.FilaRegistroAuditoria;
import com.example.siggtrat.utilitario.UtilitarioHashSeguranca;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
 * CAT-002:002. Contraprova PostgreSQL/HTTP da identidade nominal por Gestão.
 * Remover a exclusão mútua antes de validar deve produzir 200+200 e duas
 * identidades IgnoreCase; a evidência é persistida antes das asserções finais.
 * O spy executa o serviço completo: somente A é retida após o flush e antes
 * do advice transacional commitar. Não se exige que B ultrapasse a validação.
 * Os controles de autorização exigem espera no namespace (fase GREEN).
 * Draft: compilação, RED e GREEN são responsabilidade da execução central.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CargoIdentidadeConcorrenteHttpIT extends ContratosHttpBase {
    @Autowired private DataSource dataSource;
    @Autowired private FilaRegistroAuditoria filaAuditoria;
    @MockitoSpyBean private CargoServico cargoServico;
    private final String requestIdA = UUID.randomUUID().toString();
    private final String requestIdB = UUID.randomUUID().toString();

    @ParameterizedTest(name = "{0}/{1}: identidade nominal na mesma Gestão")
    @CsvSource({"POST, POST", "POST, PUT", "PUT, PUT"})
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void mesmaGestaoConfirmaUmaIdentidadeEPreservaIntegralmenteAPerdedora(String metodoA, String metodoB)
            throws Exception {
        conferirPostgres();
        Cenario c = criarCenarioPersistido();
        String sufixo = UUID.randomUUID().toString();
        String vencedor = "Analista Senior " + sufixo;
        String nomeA = "  Analista   Senior  " + sufixo + "  ";
        String nomeB = "  ANALISTA    SENIOR   " + sufixo.toUpperCase(Locale.ROOT) + "  ";
        Long idA = metodoA.equals("PUT") ? criarCargo(c.localA, "Origem A " + sufixo) : null;
        Long idB = metodoB.equals("PUT") ? criarCargo(c.localA, "Origem B " + sufixo) : null;
        Estado antes = estado(c.gestaoA.getId());
        var pedidoA = autenticar(pedido(metodoA, idA, nomeA, antes)
                .header(FiltroIdentificadorRequisicao.REQUEST_ID_HEADER, requestIdA), c.localA);
        var pedidoB = autenticar(pedido(metodoB, idB, nomeB, antes)
                .header(FiltroIdentificadorRequisicao.REQUEST_ID_HEADER, requestIdB), c.localA);
        Controle controle = new Controle(c.gestaoA.getId(), nomeA, nomeB);
        instalarBarreira(controle);
        Execucao execucao = disputar(controle, () -> mvc.perform(pedidoA).andReturn(), pedidoB, null);
        Estado depois = estado(c.gestaoA.getId());
        registrar("identidade-" + metodoA + "-" + metodoB, mapa("antes", antes, "depois", depois,
                "nomeNormalizadoEsperado", vencedor, "execucao", resumo(execucao, controle),
                "identidadesIgnoreCase", identidades(depois, vencedor)));

        long vencedorId = idResposta(execucao.a());
        assertAll("Uma escrita vence, a outra recebe conflito nominal sem efeito parcial",
                () -> conferirExecucao(execucao),
                () -> assertThat(status(execucao.a())).isEqualTo(200),
                () -> assertThat(status(execucao.b())).isEqualTo(409),
                () -> assertThat(corpo(execucao.b())).contains("Já existe um Cargo com este nome"),
                () -> assertThat(identidades(depois, vencedor)).hasSize(1),
                () -> assertThat(depois.gestao()).isEqualTo(antes.gestao()),
                () -> assertThat(semCargo(depois, vencedorId))
                        .as("Inclui Cargo/versão, eventos e auditorias de negócio da perdedora e dos demais itens")
                        .isEqualTo(semCargo(antes, vencedorId)),
                () -> conferirVencedora(antes, depois, vencedorId, vencedor, metodoA, c.localA.getId()),
                () -> assertThat(execucao.esperas()).as("GREEN comprova a espera na PK da Gestão, com binding do ID")
                        .isNotEmpty());
    }

    @Test
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void gestoesDistintasConfirmamMesmoNomeAntesDeLiberarAPrimeiraSemLockGlobal() throws Exception {
        conferirPostgres();
        Cenario c = criarCenarioPersistido();
        String sufixo = UUID.randomUUID().toString();
        String nomeA = "  Analista   Senior " + sufixo + "  ";
        String nomeB = "ANALISTA SENIOR " + sufixo.toUpperCase(Locale.ROOT);
        Estado antesA = estado(c.gestaoA.getId(), requestIdA);
        Estado antesB = estado(c.gestaoB.getId(), requestIdB);
        var pedidoA = autenticar(pedido("POST", null, nomeA, antesA)
                .header(FiltroIdentificadorRequisicao.REQUEST_ID_HEADER, requestIdA), c.localA);
        var pedidoB = autenticar(pedido("POST", null, nomeB, antesB)
                .header(FiltroIdentificadorRequisicao.REQUEST_ID_HEADER, requestIdB), c.localB);
        Controle controle = new Controle(c.gestaoB.getId(), nomeA, nomeB);
        instalarBarreira(controle);
        Execucao execucao = disputar(controle, () -> mvc.perform(pedidoA).andReturn(), pedidoB, null);
        Estado depoisA = estado(c.gestaoA.getId(), requestIdA);
        Estado depoisB = estado(c.gestaoB.getId(), requestIdB);
        registrar("gestoes-distintas", mapa("antesA", antesA, "antesB", antesB, "depoisA", depoisA,
                "depoisB", depoisB, "execucao", resumo(execucao, controle)));
        assertAll("Mesmo nome pertence a namespaces independentes",
                () -> conferirExecucao(execucao),
                () -> assertThat(status(execucao.a())).isEqualTo(200),
                () -> assertThat(status(execucao.b())).isEqualTo(200),
                () -> assertThat(execucao.bTerminouAntesDeLiberar()).as("B commitou com A ainda retida").isTrue(),
                () -> assertThat(execucao.esperas()).isEmpty(),
                () -> conferirVencedora(antesA, depoisA, idResposta(execucao.a()),
                        "Analista Senior " + sufixo, "POST", c.localA.getId()),
                () -> conferirVencedora(antesB, depoisB, idResposta(execucao.b()), nomeB, "POST", c.localB.getId()),
                () -> assertThat(identidades(depoisA, nomeB)).hasSize(1),
                () -> assertThat(identidades(depoisB, nomeB)).hasSize(1),
                () -> assertThat(semCargo(depoisA, idResposta(execucao.a()))).isEqualTo(antesA),
                () -> assertThat(semCargo(depoisB, idResposta(execucao.b()))).isEqualTo(antesB));
    }

    @Test
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void gestaoInativadaDuranteEsperaNoNamespaceRecusaSemCargoEventoOuAuditoriaDeNegocio() throws Exception {
        conferirPostgres();
        Cenario c = criarCenarioPersistido();
        String nomeB = "Gestao inativa " + UUID.randomUUID();
        Estado antes = estado(c.gestaoA.getId());
        var pedidoB = autenticar(pedido("POST", null, nomeB, antes)
                .header(FiltroIdentificadorRequisicao.REQUEST_ID_HEADER, requestIdB), c.localA);
        Controle controle = new Controle(c.gestaoA.getId(), null, nomeB);
        instalarBarreira(controle);
        // Fixture JPA declarada: B enxerga ativo=true ainda commitado e espera
        // a transação que publica ativo=false. Não contorna o lock de A real.
        Execucao execucao = disputar(controle, () -> emTransacao(() -> {
            try {
                GestaoLocal gestao = em.find(GestaoLocal.class, c.gestaoA.getId(), LockModeType.PESSIMISTIC_WRITE);
                gestao.setAtivo(false);
                em.flush();
                controle.pidA.complete(pid());
                controle.etapaA.set("GESTAO_INATIVA_FLUSH_SEM_COMMIT");
                aguardarLiberacao(controle);
                return null;
            } catch (Throwable falha) {
                controle.pidA.completeExceptionally(falha);
                throw falha;
            }
        }), pedidoB, null);
        Estado depois = estado(c.gestaoA.getId());
        registrar("gestao-inativada", mapa("antes", antes, "depois", depois,
                "primeiraOperacao", "FIXTURE_JPA_INATIVACAO_GESTAO_SEM_HTTP",
                "execucao", resumo(execucao, controle)));
        assertAll("Condição ativa deve ser relida após adquirir o namespace",
                () -> conferirExecucao(execucao),
                () -> assertThat(execucao.esperas()).isNotEmpty(),
                () -> assertThat(execucao.bTerminouAntesDeLiberar()).isFalse(),
                () -> assertThat(status(execucao.b())).isEqualTo(403),
                () -> assertThat(depois.cargos()).isEqualTo(antes.cargos()),
                () -> assertThat(depois.eventos()).isEqualTo(antes.eventos()),
                () -> assertThat(depois.auditorias()).isEqualTo(antes.auditorias()),
                () -> assertThat(depois.gestao()).isEqualTo(new GestaoEstado(antes.gestao().id(),
                        antes.gestao().nome(), antes.gestao().codigo(), antes.gestao().superintendenciaId(),
                        false, antes.gestao().versao() + 1)),
                () -> assertThat(identidades(depois, nomeB)).isEmpty());
    }

    @Test
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void segundoGestorRevogadoPorHttpDuranteEsperaNoNamespaceNaoConfirmaEscrita() throws Exception {
        conferirPostgres();
        Cenario c = criarCenarioPersistido();
        UsuarioAplicacao segundoGestor = emTransacao(() -> {
            UsuarioAplicacao novo = usuario(PerfilAcesso.GESTOR_LOCAL, em.find(GestaoLocal.class, c.gestaoA.getId()));
            em.flush();
            return novo;
        });
        assertThat(segundoGestor.getId()).isNotEqualTo(c.localA.getId());
        String sufixo = UUID.randomUUID().toString();
        String nomeA = "Vencedora revogacao " + sufixo;
        String nomeB = "Perdedora revogacao " + sufixo;
        Estado antes = estado(c.gestaoA.getId());
        var pedidoA = autenticar(pedido("POST", null, nomeA, antes)
                .header(FiltroIdentificadorRequisicao.REQUEST_ID_HEADER, requestIdA), c.localA);
        var pedidoB = autenticar(pedido("POST", null, nomeB, antes)
                .header(FiltroIdentificadorRequisicao.REQUEST_ID_HEADER, requestIdB), segundoGestor);
        var revogar = autenticar(put("/api/usuarios/" + segundoGestor.getId()).contentType(APPLICATION_JSON)
                .content(json(mapa("active", false))), c.admin);
        Controle controle = new Controle(c.gestaoA.getId(), nomeA, nomeB);
        instalarBarreira(controle);
        Execucao execucao = disputar(controle, () -> mvc.perform(pedidoA).andReturn(), pedidoB, () -> {
            MvcResult resposta = mvc.perform(revogar).andReturn();
            // HTTP terminou: o commit é comprovado por uma nova transação.
            return emTransacao(() -> {
                UsuarioAplicacao atual = em.find(UsuarioAplicacao.class, segundoGestor.getId());
                return mapa("statusHttp", resposta.getResponse().getStatus(), "id", atual.getId(),
                        "ativoRelido", atual.getActive(), "perfilRelido", atual.getPerfil().name(),
                        "gestaoLocalId", atual.getGestaoLocal() == null ? null : atual.getGestaoLocal().getId(),
                        "versaoRelida", atual.getVersion(), "conferidoAntesDaLiberacao", true);
            });
        });
        Estado depois = estado(c.gestaoA.getId());
        registrar("executor-revogado", mapa("antes", antes, "depois", depois,
                "executorOriginalId", c.localA.getId(), "executorRevogadoId", segundoGestor.getId(),
                "execucao", resumo(execucao, controle)));
        assertAll("Segundo executor perde autorização enquanto a primeira transação preserva seu executor próprio",
                () -> conferirExecucao(execucao),
                () -> assertThat(execucao.esperas()).isNotEmpty(),
                () -> assertThat(execucao.alteracaoDuranteEspera()).containsEntry("statusHttp", 200)
                        .containsEntry("ativoRelido", false).containsEntry("conferidoAntesDaLiberacao", true),
                () -> assertThat(execucao.bTerminouAntesDeLiberar()).isFalse(),
                () -> assertThat(status(execucao.a())).isEqualTo(200),
                () -> assertThat(status(execucao.b())).isIn(401, 403),
                () -> assertThat(identidades(depois, nomeB)).isEmpty(),
                () -> assertThat(semCargo(depois, idResposta(execucao.a()))).isEqualTo(antes),
                () -> conferirVencedora(antes, depois, idResposta(execucao.a()), nomeA, "POST", c.localA.getId()));
    }

    private void instalarBarreira(Controle controle) throws Exception {
        CargoServico spy = AopTestUtils.getUltimateTargetObject(cargoServico);
        assertThat(org.mockito.Mockito.mockingDetails(spy).isSpy()).isTrue();
        doAnswer(i -> interceptar(i, i.getArgument(0, CargoDTO.class), controle))
                .when(spy).criar(any(CargoDTO.class));
        doAnswer(i -> interceptar(i, i.getArgument(1, CargoDTO.class), controle))
                .when(spy).atualizar(anyLong(), any(CargoDTO.class));
    }

    private Object interceptar(InvocationOnMock invocacao, CargoDTO dto, Controle controle) throws Throwable {
        boolean primeira = dto != null && controle.nomeA != null && controle.nomeA.equals(dto.nome());
        boolean segunda = dto != null && controle.nomeB.equals(dto.nome());
        if (!primeira && !segunda) return invocacao.callRealMethod();
        var etapa = primeira ? controle.etapaA : controle.etapaB;
        try {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
            long entrada = pid();
            etapa.set("ENTROU_SERVICO_REAL");
            if (segunda) controle.pidB.complete(entrada);
            Object resposta = invocacao.callRealMethod();
            etapa.set("SERVICO_REAL_RETORNOU");
            if (primeira) {
                em.flush();
                assertThat(pid()).as("Mesmo PID antes/depois do serviço e flush").isEqualTo(entrada);
                assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
                etapa.set("FLUSH_REAL_ANTES_DO_COMMIT");
                controle.pidA.complete(entrada);
                aguardarLiberacao(controle);
                etapa.set("LIBERADA_PARA_COMMIT");
            }
            return resposta;
        } catch (Throwable falha) {
            etapa.set("FALHA_" + falha.getClass().getSimpleName());
            (primeira ? controle.pidA : controle.pidB).completeExceptionally(falha);
            throw falha;
        }
    }

    private void aguardarLiberacao(Controle controle) {
        try {
            if (!controle.liberar.await(30, TimeUnit.SECONDS))
                throw new AssertionError("Limite da fixture: primeira transação não foi liberada");
        } catch (InterruptedException falha) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Fixture interrompida antes da liberação", falha);
        }
    }

    /** Nunca cancela A por falha de gate; recolhe os resultados após liberar a própria transação. */
    private Execucao disputar(Controle controle, Callable<MvcResult> primeira,
            AbstractMockHttpServletRequestBuilder<?> pedidoB, Callable<Map<String, Object>> duranteEspera)
            throws Exception {
        var pool = Executors.newFixedThreadPool(2);
        Future<MvcResult> a = null;
        Future<MvcResult> b = null;
        List<Map<String, Object>> esperas = List.of();
        Map<String, Object> alteracao = Map.of();
        String falhaGate = null;
        boolean bTerminouAntes = false;
        boolean aPendenteAntes = false;
        Resultado resultadoA;
        Resultado resultadoB;
        boolean encerrado;
        try {
            a = pool.submit(() -> controle.atorA.executar(primeira));
            long raiz = controle.pidA.get(15, TimeUnit.SECONDS);
            b = pool.submit(() -> controle.atorB.executar(() -> mvc.perform(pedidoB).andReturn()));
            long solicitante = controle.pidB.get(10, TimeUnit.SECONDS);
            if (solicitante == raiz) throw new AssertionError("A e B precisam de conexões/transações independentes");
            long fim = System.nanoTime() + TimeUnit.SECONDS.toNanos(12);
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            while (System.nanoTime() < fim) {
                esperas = EsperaJdbcPostgres.observar(jdbc, raiz, controle.atorB,
                        sql -> sql.selecaoChave("gestao_local", "id_gestao_local", controle.gestaoAlvo),
                        "namespace de Cargo: gestao_local.id_gestao_local=" + controle.gestaoAlvo);
                if (!esperas.isEmpty() || b.isDone()) break;
                Thread.sleep(20);
            }
            if (esperas.isEmpty() && !b.isDone())
                falhaGate = "B não terminou nem comprovou espera na PK da Gestão em 12s";
            if (!esperas.isEmpty() && duranteEspera != null) alteracao = duranteEspera.call();
            bTerminouAntes = b.isDone();
            aPendenteAntes = !a.isDone();
        } catch (Throwable falha) {
            falhaGate = falha.getClass().getSimpleName();
        } finally {
            controle.liberar.countDown();
            resultadoA = recolher(a);
            resultadoB = recolher(b);
            pool.shutdown();
            encerrado = pool.awaitTermination(10, TimeUnit.SECONDS);
            if (!encerrado) pool.shutdownNow();
        }
        return new Execucao(resultadoA, resultadoB, esperas, alteracao, falhaGate,
                bTerminouAntes, aPendenteAntes, encerrado);
    }

    private Resultado recolher(Future<MvcResult> futuro) {
        if (futuro == null) return new Resultado(null, "NAO_INICIADO");
        try { return new Resultado(futuro.get(15, TimeUnit.SECONDS), null); }
        catch (Exception falha) {
            return new Resultado(null, falha.getClass().getSimpleName() + "/"
                    + (falha.getCause() == null ? "SEM_CAUSA" : falha.getCause().getClass().getSimpleName()));
        }
    }

    private void conferirExecucao(Execucao execucao) {
        assertAll(() -> assertThat(execucao.falhaGate()).as("Gate precisa terminar ou provar o alvo exato").isNull(),
                () -> assertThat(execucao.a().falha()).isNull(),
                () -> assertThat(execucao.b().falha()).isNull(),
                () -> assertThat(execucao.aPendenteAntesDeLiberar()).isTrue(),
                () -> assertThat(execucao.encerrado()).as("Workers concluíram suas próprias transações").isTrue());
    }

    private AbstractMockHttpServletRequestBuilder<?> pedido(String metodo, Long id, String nome, Estado estado)
            throws Exception {
        Map<String, Object> body = mapa("nome", nome, "cbo", "1234-5", "descricao", "Descrição sintética concorrente",
                "ativo", true, "gestaoLocalId", estado.gestao().id());
        if (id != null) body.put("version", cargo(estado, id).versao());
        return (metodo.equals("POST") ? post("/api/cargos") : put("/api/cargos/" + id))
                .contentType(APPLICATION_JSON).content(json(body));
    }

    private long criarCargo(UsuarioAplicacao usuario, String nome) throws Exception {
        return dados(executar(usuario, post("/api/cargos").contentType(APPLICATION_JSON)
                .content(json(mapa("nome", nome, "ativo", true))), 200,
                "Pré-condição: Cargo contemporâneo criado pelo serviço real/HTTP")).path("id").asLong();
    }

    private Estado estado(long gestaoId) { return estado(gestaoId, requestIdA, requestIdB); }

    private Estado estado(long gestaoId, String... requisicoes) {
        // O scheduler da base é suprimido. Publicação após commit continua real,
        // e a fila real é descarregada antes do readback em outra transação.
        filaAuditoria.descarregar();
        return emTransacao(() -> {
            GestaoLocal g = em.find(GestaoLocal.class, gestaoId);
            List<CargoEstado> cargos = em.createQuery("select c from Cargo c where c.gestaoLocal.id=:g order by c.id", Cargo.class)
                    .setParameter("g", gestaoId).getResultList().stream()
                    .map(c -> new CargoEstado(c.getId(), c.getNome(), c.getCbo(), c.getDescricao(), c.getAtivo(),
                            c.getGestaoLocal().getId(), c.getVersion())).toList();
            List<EventoEstado> eventos = em.createQuery("select e from EventoDisponibilidadeCatalogo e where "
                    + "e.tipoCatalogo=:tipo and (e.catalogoId in (select c.id from Cargo c where c.gestaoLocal.id=:g) "
                    + "or e.responsavelId in (select u.id from UsuarioAplicacao u where u.gestaoLocal.id=:g)) order by e.id",
                    EventoDisponibilidadeCatalogo.class).setParameter("tipo", TipoCatalogoTemporal.CARGO)
                    .setParameter("g", gestaoId).getResultList().stream().map(e -> new EventoEstado(e.getId(),
                            e.getCatalogoId(), e.getDataEfetiva(), e.getAtivoAnterior(), e.getAtivo(), e.getRegistradoEm(),
                            e.getMotivo(), e.getEvidencia(), e.getAutorizacao(), e.getChaveOrigem(), e.getChaveIdempotencia(),
                            e.getHashIntencao(), e.getResponsavelId(), e.getResponsavelUsername(),
                            e.getAtivoAtualAposRegistro(), e.getVersaoCatalogoApos())).toList();
            // IDs de requisição cobrem inclusive uma auditoria indevida cujo
            // INSERT de Cargo tenha sido revertido (não existe ID para o JOIN).
            List<AuditoriaEstado> auditorias = em.createQuery("select a from RegistroAuditoria a where "
                    + "a.entityType='Cargo' and a.action like 'CARGO_%' and (a.entityId in "
                    + "(select c.id from Cargo c where c.gestaoLocal.id=:g) or a.requestId in :requisicoes) order by a.id",
                    RegistroAuditoria.class).setParameter("g", gestaoId).setParameter("requisicoes", List.of(requisicoes))
                    .getResultList().stream().map(a -> new AuditoriaEstado(a.getId(),
                            a.getEntityId(), a.getAction(), a.getUserId(), a.getCreatedAt(), a.getRequestId(),
                            a.getDetailsJson() == null ? null : UtilitarioHashSeguranca.sha256Hexadecimal(a.getDetailsJson()))).toList();
            return new Estado(new GestaoEstado(g.getId(), g.getNome(), g.getCodigo(), g.getSuperintendencia().getId(),
                    g.getAtivo(), g.getVersion()), cargos, eventos, auditorias);
        });
    }

    private void conferirVencedora(Estado antes, Estado depois, long id, String nome, String metodo, long executor) {
        CargoEstado atual = cargo(depois, id);
        List<AuditoriaEstado> novas = depois.auditorias().stream().filter(a -> a.cargoId() == id)
                .filter(a -> !antes.auditorias().contains(a)).toList();
        assertAll(() -> assertThat(atual.nome()).as("Normaliza espaços preservando a caixa do vencedor").isEqualTo(nome),
                () -> assertThat(atual.ativo()).isTrue(),
                () -> assertThat(atual.versao()).isEqualTo(metodo.equals("POST") ? 0L : cargo(antes, id).versao() + 1),
                () -> assertThat(novas).hasSize(1),
                () -> assertThat(novas).extracting(AuditoriaEstado::acao)
                        .containsExactly(metodo.equals("POST") ? "CARGO_CRIADO" : "CARGO_ATUALIZADO"),
                () -> assertThat(novas).extracting(AuditoriaEstado::executorId).containsExactly(executor),
                () -> assertThat(depois.eventos().stream().filter(e -> e.cargoId() == id).toList())
                        .hasSize(metodo.equals("POST") ? 1 : (int) antes.eventos().stream().filter(e -> e.cargoId() == id).count()));
    }

    private CargoEstado cargo(Estado estado, long id) {
        return estado.cargos().stream().filter(c -> c.id() == id).findFirst()
                .orElseThrow(() -> new AssertionError("Cargo esperado ausente no readback: " + id));
    }

    private List<CargoEstado> identidades(Estado estado, String nome) {
        return estado.cargos().stream().filter(c -> c.nome().equalsIgnoreCase(nome)).toList();
    }

    private Estado semCargo(Estado estado, long id) {
        return new Estado(estado.gestao(), estado.cargos().stream().filter(c -> c.id() != id).toList(),
                estado.eventos().stream().filter(e -> e.cargoId() != id).toList(),
                estado.auditorias().stream().filter(a -> a.cargoId() != id).toList());
    }

    private long pid() { return ((Number) em.createNativeQuery("select pg_backend_pid()").getSingleResult()).longValue(); }
    private Integer status(Resultado resultado) { return resultado.http() == null ? null : resultado.http().getResponse().getStatus(); }
    private long idResposta(Resultado resultado) throws Exception { return resultado.http() == null ? -1 : dados(resultado.http()).path("id").asLong(-1); }
    private String corpo(Resultado resultado) throws Exception { return resultado.http() == null ? "" : resultado.http().getResponse().getContentAsString(StandardCharsets.UTF_8); }

    private void conferirPostgres() throws Exception {
        try (var conexao = dataSource.getConnection()) {
            assertThat(conexao.getMetaData().getDatabaseProductName()).isEqualTo("PostgreSQL");
        }
        assertThat(pkGestao()).as("Catálogo físico confirma a PK literal usada na prova JDBC")
                .containsExactly("id_gestao_local");
    }

    private List<String> pkGestao() {
        return new JdbcTemplate(dataSource).query("""
                select a.attname from pg_index i join pg_class t on t.oid=i.indrelid
                join pg_namespace n on n.oid=t.relnamespace
                join pg_attribute a on a.attrelid=t.oid and a.attnum=any(i.indkey)
                where n.nspname=current_schema() and t.relname='gestao_local' and i.indisprimary
                order by a.attnum
                """, (r, n) -> r.getString(1));
    }

    private Map<String, Object> resumo(Execucao execucao, Controle controle) throws Exception {
        List<Map<String, Object>> esperasSemSql = execucao.esperas().stream().map(linha -> {
            Map<String, Object> prova = new LinkedHashMap<>(linha);
            Object sql = prova.remove("sql_preparado");
            prova.put("sql_preparado_sha256", sql == null ? null : UtilitarioHashSeguranca.sha256Hexadecimal(sql.toString()));
            prova.put("tabelaAlvo", "gestao_local"); prova.put("colunaPkAlvo", "id_gestao_local");
            prova.put("idGestaoLocalAlvo", controle.gestaoAlvo);
            return prova;
        }).toList();
        return mapa("statusA", status(execucao.a()), "statusB", status(execucao.b()),
                "requestIdA", requestIdA, "requestIdB", requestIdB,
                "falhaA", execucao.a().falha(), "falhaB", execucao.b().falha(), "falhaGate", execucao.falhaGate(),
                "pidA", pidRegistrado(controle.pidA), "pidB", pidRegistrado(controle.pidB),
                "etapaA", controle.etapaA.get(), "etapaB", controle.etapaB.get(),
                "bTerminouAntesDaLiberacao", execucao.bTerminouAntesDeLiberar(),
                "aPendenteAntesDaLiberacao", execucao.aPendenteAntesDeLiberar(), "workersEncerrados", execucao.encerrado(),
                "pkGestaoVerificada", pkGestao(), "esperasFisicas", esperasSemSql,
                "alteracaoDuranteEspera", execucao.alteracaoDuranteEspera());
    }

    private Long pidRegistrado(CompletableFuture<Long> futuro) {
        return futuro.isDone() && !futuro.isCompletedExceptionally() ? futuro.getNow(null) : null;
    }

    private void registrar(String caso, Object evidencia) throws Exception {
        Path pasta = Path.of("target/cargo-identidade-concorrente").toAbsolutePath();
        var diretorioPrivado = PosixFilePermissions.fromString("rwx------");
        Files.createDirectories(pasta, PosixFilePermissions.asFileAttribute(diretorioPrivado));
        Files.setPosixFilePermissions(pasta, diretorioPrivado);
        Path arquivo = Files.createFile(pasta.resolve(caso + "-" + UUID.randomUUID() + ".json"),
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
        byte[] bytes = (mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapa("criterio", "CAT-002:002",
                "caso", caso, "registradoEm", Instant.now(), "auditRound", System.getenv("SGT_AUDITORIA_RODADA"),
                "limites", "Fixture contemporânea; MockMvc + serviços/JPA/PostgreSQL reais; sem navegador. "
                        + "Auditoria HTTP negativa pode existir; apenas ações de negócio CARGO_* entram no snapshot. "
                        + "SQL bruto, payload de auditoria, senha, token e cookie não são coletados.",
                "evidencia", evidencia)) + "\n").getBytes(StandardCharsets.UTF_8);
        try (FileChannel canal = FileChannel.open(arquivo, StandardOpenOption.WRITE)) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            while (buffer.hasRemaining()) canal.write(buffer);
            canal.force(true);
        }
        try (FileChannel diretorio = FileChannel.open(pasta, StandardOpenOption.READ)) { diretorio.force(true); }
    }

    private static final class Controle {
        final long gestaoAlvo;
        final String nomeA;
        final String nomeB;
        final CompletableFuture<Long> pidA = new CompletableFuture<>();
        final CompletableFuture<Long> pidB = new CompletableFuture<>();
        final CountDownLatch liberar = new CountDownLatch(1);
        final AtomicReference<String> etapaA = new AtomicReference<>("ANTES_HTTP");
        final AtomicReference<String> etapaB = new AtomicReference<>("ANTES_HTTP");
        final Ator atorA = new Ator("cargo-identidade-A");
        final Ator atorB = new Ator("cargo-identidade-B");
        Controle(long gestaoAlvo, String nomeA, String nomeB) {
            this.gestaoAlvo = gestaoAlvo; this.nomeA = nomeA; this.nomeB = nomeB;
        }
    }

    private record Resultado(MvcResult http, String falha) { }
    private record Execucao(Resultado a, Resultado b, List<Map<String, Object>> esperas,
                            Map<String, Object> alteracaoDuranteEspera, String falhaGate,
                            boolean bTerminouAntesDeLiberar, boolean aPendenteAntesDeLiberar, boolean encerrado) { }
    private record Estado(GestaoEstado gestao, List<CargoEstado> cargos, List<EventoEstado> eventos,
                          List<AuditoriaEstado> auditorias) { }
    private record GestaoEstado(long id, String nome, String codigo, long superintendenciaId, Boolean ativo, long versao) { }
    private record CargoEstado(long id, String nome, String cbo, String descricao, Boolean ativo, long gestaoId, long versao) { }
    private record AuditoriaEstado(long id, long cargoId, String acao, Long executorId, Instant criadoEm,
                                  String requestId, String detalhesSha256) { }
    private record EventoEstado(long id, long cargoId, LocalDate dataEfetiva, Boolean ativoAnterior, Boolean ativo,
                                Instant registradoEm, String motivo, String evidencia, String autorizacao,
                                String chaveOrigem, String chaveIdempotencia, String hashIntencao, Long responsavelId,
                                String responsavelUsername, Boolean ativoAtualAposRegistro, Long versaoCatalogoApos) { }
}
