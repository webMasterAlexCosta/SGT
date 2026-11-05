package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.example.siggtrat.dominio.vinculo.CategoriaTerminoVinculo;
import com.example.siggtrat.dominio.vinculo.TipoEventoVinculoContratual;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.servico.FilaRegistroAuditoria;
import com.example.siggtrat.servico.GestaoContratoTemporalServico;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Controles 6_E: CAT-007:004/007/008 e controles de CAT-005.
 *
 * Dispatcher MVC, filtros, autorização, serviços, JPA e MySQL reais. A base
 * suprime somente TaskScheduler; por isso a fila real de auditoria é descarregada
 * explicitamente antes das leituras. Não é prova de transporte TCP/JAR/reinício.
 *
 * Fontes, pessoas e referências SEI são sintéticas e declaradas. A fixture não
 * recupera acervo histórico nem sustenta reclassificação documental de CAT-007.
 * A criação JPA sem eventos é intencional: criar pelo endpoint de cadastro
 * geraria origem prospectiva e impediria exercitar as PRIMEIRAS transições.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Execution(ExecutionMode.SAME_THREAD)
@TestPropertySource(properties = "spring.sql.init.mode=never")
class CatalogosTransicoesControles6EHttpIT extends ContratosHttpBase {
    private static final String ACAO = "CATALOGO_TRANSICAO_DOCUMENTADA";
    @Autowired private DataSource dataSource;
    @Autowired private Environment ambiente;
    @Autowired private FilaRegistroAuditoria fila;
    @Autowired private GestaoContratoTemporalServico gestaoDaFonte;

    @BeforeEach
    void exigirMysqlETransacoesReais() throws Exception {
        assertThat(TransactionSynchronizationManager.isActualTransactionActive())
                .as("Cada comando HTTP deve encerrar sua própria transação").isFalse();
        try (var conexao = dataSource.getConnection()) {
            assertThat(conexao.getMetaData().getDatabaseProductName()).isEqualTo("MySQL");
            assertThat(conexao.getMetaData().getDatabaseProductVersion()).isEqualTo("8.0.45");
        }
        assertThat(ambiente.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("update");
        assertThat(ambiente.getProperty("spring.sql.init.mode")).isEqualTo("never");
    }

    static Stream<Arguments> primeirasTransicoes() {
        return Arrays.stream(TipoCatalogoTemporal.values())
                .flatMap(tipo -> Stream.of(Arguments.of(tipo, false), Arguments.of(tipo, true)));
    }

    @ParameterizedTest(name = "{0}, mesmaIntencao={1}")
    @MethodSource("primeirasTransicoes")
    void primeirasTransicoesHttpConcorrentesConservamUmaOrigemSemParciais(
            TipoCatalogoTemporal tipo, boolean mesmaIntencao) throws Exception {
        Cenario c = criarCenarioPersistido();
        long id = itemSemFonte(c, tipo);
        LocalDate data = RelogioSistema.hoje().minusDays(4);
        EstadoCatalogo antes = estado(tipo, id);
        assertThat(antes.ativo()).isTrue();
        assertThat(antes.eventos()).isEmpty();
        assertThat(antes.auditorias()).isEmpty();

        Map<String, Object> a = pedido(tipo, id, data, false, "primeira-fonte-A");
        confirmarPrevia(c, tipo, id, a);
        Map<String, Object> b;
        if (mesmaIntencao) {
            // Mesma pessoa, mesma chave e todos os campos da intenção idênticos.
            b = new LinkedHashMap<>(a);
        } else {
            b = pedido(tipo, id, data, false, "primeira-fonte-B");
            confirmarPrevia(c, tipo, id, b);
        }
        assertThat(estado(tipo, id)).as("As prévias não criam fontes ou consomem versão").isEqualTo(antes);

        // Preparar autenticação na thread principal: a base mantém mapa de sessões.
        var pedidoA = autenticar(post(rota(tipo, id)).contentType(MediaType.APPLICATION_JSON)
                .content(json(a)), c.localA);
        var pedidoB = autenticar(post(rota(tipo, id)).contentType(MediaType.APPLICATION_JSON)
                .content(json(b)), c.localA);
        var pool = Executors.newFixedThreadPool(3);
        CountDownLatch iniciar = new CountDownLatch(1);
        CountDownLatch liberar = new CountDownLatch(1);
        CompletableFuture<Long> conexaoBloqueadora = new CompletableFuture<>();
        MvcResult respostaA;
        MvcResult respostaB;
        List<Map<String, Object>> esperas;
        try {
            Future<?> bloqueador = pool.submit(() -> {
                try {
                    emTransacao(() -> {
                        em.find(classe(tipo), id, LockModeType.PESSIMISTIC_WRITE);
                        conexaoBloqueadora.complete(((Number) em.createNativeQuery("select connection_id()")
                                .getSingleResult()).longValue());
                        aguardar(liberar, 25, "A fixture não liberou o catálogo");
                        return null;
                    });
                } catch (RuntimeException | Error ex) {
                    conexaoBloqueadora.completeExceptionally(ex);
                    throw ex;
                }
            });
            long bloqueadora = conexaoBloqueadora.get(10, TimeUnit.SECONDS);
            Future<MvcResult> tentativaA = pool.submit(() -> executarConcorrente(pedidoA, iniciar));
            Future<MvcResult> tentativaB = pool.submit(() -> executarConcorrente(pedidoB, iniciar));
            iniciar.countDown();
            esperas = aguardarDuasEsperasReais(tipo, id, bloqueadora, tentativaA, tentativaB);
            assertThat(tentativaA.isDone()).isFalse();
            assertThat(tentativaB.isDone()).isFalse();
            liberar.countDown();
            bloqueador.get(10, TimeUnit.SECONDS);
            respostaA = tentativaA.get(15, TimeUnit.SECONDS);
            respostaB = tentativaB.get(15, TimeUnit.SECONDS);
        } finally {
            iniciar.countDown();
            liberar.countDown();
            pool.shutdownNow();
            assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }

        EstadoCatalogo depois = estado(tipo, id);
        registrarEvidencia("primeira-" + tipo + "-mesma-" + mesmaIntencao,
                mapa("tipo", tipo, "catalogoId", id, "mesmaIntencao", mesmaIntencao,
                        "esperasMysql", esperas, "estadoAntes", antes, "estadoDepois", depois,
                        "statusA", respostaA.getResponse().getStatus(), "respostaA", dados(respostaA),
                        "statusB", respostaB.getResponse().getStatus(), "respostaB", dados(respostaB)));
        List<Integer> status = List.of(respostaA.getResponse().getStatus(), respostaB.getResponse().getStatus());
        if (mesmaIntencao) assertThat(status).containsExactlyInAnyOrder(200, 200);
        else assertThat(status).containsExactlyInAnyOrder(200, 409);

        // Não fixar qual thread vence, ordem de chegada nem incremento JPA exato.
        MvcResult vencedora = respostaA.getResponse().getStatus() == 200 ? respostaA : respostaB;
        Map<String, Object> intencaoVencedora = vencedora == respostaA ? a : b;
        JsonNode recibo = dados(vencedora);
        assertThat(depois.eventos()).hasSize(1);
        assertThat(depois.auditorias()).hasSize(1);
        EventoFoto evento = depois.eventos().get(0);
        assertThat(evento.id()).isEqualTo(recibo.path("eventoId").asLong());
        assertThat(evento.chaveOrigem()).isEqualTo(tipo.name() + ":" + id);
        assertThat(evento.data()).isEqualTo(data);
        assertThat(evento.ativoAnterior()).as("Nenhuma fonte prova o período anterior").isNull();
        assertThat(evento.ativo()).isFalse();
        assertThat(evento.chaveIdempotencia()).isEqualTo(intencaoVencedora.get("chaveIdempotencia"));
        assertThat(evento.motivo()).isEqualTo(intencaoVencedora.get("motivo"));
        assertThat(evento.evidencia()).isEqualTo(intencaoVencedora.get("evidencia"));
        assertThat(evento.autorizacao()).isEqualTo(intencaoVencedora.get("autorizacao"));
        assertThat(evento.responsavelId()).isEqualTo(c.localA.getId());
        assertThat(evento.responsavelUsername()).isEqualTo(c.localA.getUsername());
        assertThat(evento.hashIntencao()).hasSize(64);
        assertThat(depois.ativo()).isFalse();
        assertThat(depois.versao()).isGreaterThan(antes.versao());
        assertThat(depois.versao()).as("Nenhuma versão parcial além da anunciada pelo vencedor")
                .isEqualTo(recibo.path("versaoAtual").asLong());
        assertThat(evento.versaoApos()).isEqualTo(depois.versao());
        assertThat(evento.ativoAtualApos()).isEqualTo(depois.ativo());
        assertThat(Instant.parse(recibo.path("registradoEm").asText())).isEqualTo(evento.registradoEm());
        AuditoriaFoto auditoria = depois.auditorias().get(0);
        assertThat(auditoria.usuarioId()).isEqualTo(c.localA.getId());
        assertThat(auditoria.path()).isEqualTo(rota(tipo, id));
        JsonNode detalhes = mapper.readTree(auditoria.detalhes());
        assertThat(detalhes.path("eventoId").asLong()).isEqualTo(evento.id());
        assertThat(detalhes.path("version").asLong()).isEqualTo(depois.versao());
        assertThat(detalhes.path("evidencia").asText()).isEqualTo(evento.evidencia());
        assertThat(detalhes.path("autorizacao").asText()).isEqualTo(evento.autorizacao());

        if (mesmaIntencao) {
            JsonNode ra = dados(respostaA), rb = dados(respostaB);
            assertThat(ra.path("eventoId").asLong()).isEqualTo(rb.path("eventoId").asLong());
            assertThat(ra.path("versaoAtual").asLong()).isEqualTo(rb.path("versaoAtual").asLong());
            assertThat(ra.path("registradoEm").asText()).isEqualTo(rb.path("registradoEm").asText());
            assertThat(List.of(ra.path("reenvio").asBoolean(), rb.path("reenvio").asBoolean()))
                    .containsExactlyInAnyOrder(false, true);
        } else {
            String chavePerdedora = String.valueOf((vencedora == respostaA ? b : a).get("chaveIdempotencia"));
            assertThat(depois.eventos()).noneMatch(e -> chavePerdedora.equals(e.chaveIdempotencia()));
        }
        JsonNode antesDaOrigem = disponibilidade(c, tipo, id, data.minusDays(1));
        assertThat(antesDaOrigem.path("ativo").isNull()).isTrue();
        assertThat(antesDaOrigem.path("disponibilidadeComprovada").asBoolean()).isFalse();
        assertThat(disponibilidade(c, tipo, id, data).path("eventoFonteId").asLong()).isEqualTo(evento.id());
        JsonNode reenvio = enviar(c.localA, rota(tipo, id), intencaoVencedora, 200,
                "Reenvio pós-corrida mantém evento e versão, inclusive com prévia já consumida");
        assertThat(reenvio.path("eventoId").asLong()).isEqualTo(evento.id());
        assertThat(reenvio.path("reenvio").asBoolean()).isTrue();
        assertThat(estado(tipo, id)).as("Reenvio não duplica origem, evento, auditoria ou versão").isEqualTo(depois);
    }

    @Test
    void tipoContratacaoRecusaAtoRetroativoQueInvalidariaPublicacaoPosterior() throws Exception {
        Cenario c = criarCenarioPersistido();
        TipoCatalogoTemporal tipo = TipoCatalogoTemporal.TIPO_CONTRATACAO;
        long id = itemSemFonte(c, tipo);
        LocalDate hoje = RelogioSistema.hoje();
        LocalDate origem = hoje.minusDays(40), desde = hoje.minusDays(30), inicio = hoje.minusDays(20);
        registrarFonte(c, tipo, id, origem);
        var req = pedido(tipo, id, desde, false, "retirada-retroativa-tipo");
        confirmarPrevia(c, tipo, id, req); // Ainda não existe dependência deste novo Tipo.

        var rascunho = rascunho(c, "CAT007-6E-" + UUID.randomUUID(), 1, inicio, hoje.plusDays(30));
        rascunho.put("tipoContratacaoId", id);
        long contratoId = enviar(c.localA, "/api/contratos", rascunho, 201,
                "Fixture: contrato com o Tipo correto, vigente após a inativação pretendida").path("id").asLong();
        enviar(c.localA, base(contratoId) + "/composicoes", composicao(c, c.cargoA.getId(), 1), 201,
                "Fixture: composição integral para publicação canônica");
        var publicacao = mapa("dataPublicacao", hoje, "referenciaSei", "SEI-SINTETICO-CAT007-6E-PUBLICACAO",
                "justificativa", "Publicação sintética documentada para controle de dependência do Tipo");
        JsonNode previaPublicacao = enviar(c.localA, base(contratoId) + "/previas/publicar", publicacao, 200,
                "Fixture: confirmação da publicação retroativa com referência SEI legítima no contrato");
        publicacao.put("tokenPrevia", previaPublicacao.path("tokenPrevia").asText());
        publicacao.put("confirmarImpacto", true);
        enviar(c.localA, base(contratoId) + "/publicar", publicacao, 200,
                "Fixture: fato dependente confirmado pela rota canônica depois da prévia do catálogo");

        emNovaTransacao(() -> {
            Contrato contrato = em.find(Contrato.class, contratoId);
            assertThat(contrato.getEstadoNegocial()).isEqualTo(EstadoNegocialContrato.PUBLICADO);
            assertThat(contrato.getTipoContratacao().getId()).isEqualTo(id);
            assertThat(contrato.getDataInicio()).isEqualTo(inicio);
            assertThat(em.createQuery("select count(v) from VersaoContratoNegocial v "
                            + "where v.contrato.id=:contrato and v.tipoContratacaoId=:tipo", Long.class)
                    .setParameter("contrato", contratoId).setParameter("tipo", id).getSingleResult()).isPositive();
            return null;
        });
        EstadoCatalogo antes = estado(tipo, id);
        FotografiaDependente dependente = contrato(contratoId);
        assertThat(antes.versao()).isEqualTo(((Number) req.get("versaoEsperada")).longValue());
        recusarSemParcial(c, tipo, id, req, "invalidaria Tipo de Contratação",
                antes, dependente, () -> contrato(contratoId));
        assertThat(disponibilidade(c, tipo, id, inicio).path("ativo").asBoolean()).isTrue();
        inativacaoAtualPermitida(c, tipo, id, hoje, inicio, dependente, () -> contrato(contratoId));
    }

    @Test
    void motivoDesligamentoRecusaAtoRetroativoQueInvalidariaCausaJaDocumentada() throws Exception {
        Cenario c = criarCenarioPersistido();
        TipoCatalogoTemporal tipo = TipoCatalogoTemporal.MOTIVO_DESLIGAMENTO;
        long id = itemSemFonte(c, tipo);
        LocalDate hoje = RelogioSistema.hoje();
        LocalDate origem = hoje.minusDays(40), desde = hoje.minusDays(30), fim = hoje.minusDays(10);
        assertThat(fim).as("Fixture fixa do vínculo deve abranger a data escolhida").isBetween(INICIO, FIM);
        declararFonteNominalSintetica(c, "CAT007-6E-MOTIVO-" + id);
        // Disponibilidade e descrição/categoria históricas são fontes distintas.
        // Declarar o contexto apenas no dia usado pelo término, sem copiar o mestre.
        emTransacao(() -> FonteMotivoSintetica.declarar(em, gestaoDaFonte,
                c.contratoA.getId(), id, c.gestaoA.getId(), c.localA,
                "Causa individual sintética comprovada na data do término",
                CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL,
                fim, fim, Instant.now(), "SEI-SINTETICO-CAT007-6E-CONTEXTO-MOTIVO-" + id));
        registrarFonte(c, tipo, id, origem);
        var req = pedido(tipo, id, desde, false, "retirada-retroativa-motivo");
        confirmarPrevia(c, tipo, id, req); // Ainda não existe término com este Motivo.
        enviar(c.localA, "/api/vinculos-contratuais/" + c.vinculoA.getId() + "/desligar",
                mapa("dataFim", fim, "motivoDesligamentoId", id,
                        "justificativa", "Término sintético com causa administrativa expressamente escolhida",
                        "evidencia", "SEI-SINTETICO-CAT007-6E-TERMINO", "autorizacao", "ATO-SINTETICO-CAT007-6E"),
                200, "Fixture: término documentado pelo comando canônico após a prévia do catálogo");
        emNovaTransacao(() -> {
            VinculoContratual vinculo = em.find(VinculoContratual.class, c.vinculoA.getId());
            assertThat(vinculo.getDataFim()).isEqualTo(fim);
            assertThat(vinculo.getMotivoDesligamento().getId()).isEqualTo(id);
            assertThat(em.createQuery("select count(e) from EventoVinculoContratual e "
                            + "where e.vinculo.id=:v and e.motivoDesligamentoIdFotografia=:id "
                            + "and e.tipoEvento=:tipo and e.dataEfetiva=:fim", Long.class)
                    .setParameter("v", c.vinculoA.getId()).setParameter("id", id)
                    .setParameter("tipo", TipoEventoVinculoContratual.DESLIGAMENTO)
                    .setParameter("fim", fim).getSingleResult()).isEqualTo(1L);
            return null;
        });
        EstadoCatalogo antes = estado(tipo, id);
        FotografiaDependente dependente = vinculo(c.vinculoA.getId());
        assertThat(antes.versao()).isEqualTo(((Number) req.get("versaoEsperada")).longValue());
        recusarSemParcial(c, tipo, id, req, "invalidaria causa de término",
                antes, dependente, () -> vinculo(c.vinculoA.getId()));
        assertThat(disponibilidade(c, tipo, id, fim).path("ativo").asBoolean()).isTrue();
        inativacaoAtualPermitida(c, tipo, id, hoje, fim, dependente, () -> vinculo(c.vinculoA.getId()));
    }

    private void recusarSemParcial(Cenario c, TipoCatalogoTemporal tipo, long id, Map<String, Object> req,
            String motivoEsperado, EstadoCatalogo antes, FotografiaDependente dependente,
            Supplier<FotografiaDependente> relerDependente) throws Exception {
        for (String sufixo : List.of("/previa", "")) {
            MvcResult negativa = executar(c.localA,
                    post(rota(tipo, id) + sufixo).contentType(MediaType.APPLICATION_JSON).content(json(req)),
                    409, "CAT007 recusa específica do ramo " + tipo + "; prévia antiga não autoriza o ato incompatível");
            // Evita aceitar 409 acidental por autenticação, versão ou ausência de token.
            assertThat(negativa.getResponse().getContentAsString(StandardCharsets.UTF_8)).contains(motivoEsperado);
            EstadoCatalogo depois = estado(tipo, id);
            FotografiaDependente fonteDepois = relerDependente.get();
            registrarEvidencia("negativa-" + tipo + (sufixo.isEmpty() ? "-registro" : "-previa"),
                    mapa("catalogoId", id, "status", negativa.getResponse().getStatus(), "resposta", dados(negativa),
                            "estadoAntes", antes, "estadoDepois", depois,
                            "dependenteAntes", dependente, "dependenteDepois", fonteDepois));
            assertThat(depois).as("Recusa conserva flag, versão, fontes e auditoria de sucesso").isEqualTo(antes);
            assertThat(fonteDepois).as("Fato dependente e seus documentos não sofrem alteração parcial").isEqualTo(dependente);
        }
    }

    private void inativacaoAtualPermitida(Cenario c, TipoCatalogoTemporal tipo, long id,
            LocalDate hoje, LocalDate fatoPassado, FotografiaDependente fonte,
            Supplier<FotografiaDependente> relerDependente) throws Exception {
        EstadoCatalogo antes = estado(tipo, id);
        JsonNode recibo = enviar(c.localA, rota(tipo, id), pedido(tipo, id, hoje, false, "inativacao-atual"), 200,
                "Controle CAT005: bloqueio de programação é de Cargo/Lotação/Turno; fatos passados ficam íntegros");
        EstadoCatalogo depois = estado(tipo, id);
        assertThat(depois.ativo()).isFalse();
        assertThat(depois.versao()).isEqualTo(recibo.path("versaoAtual").asLong()).isGreaterThan(antes.versao());
        assertThat(depois.eventos()).hasSize(antes.eventos().size() + 1);
        assertThat(depois.eventos()).containsAll(antes.eventos());
        assertThat(depois.auditorias()).hasSize(antes.auditorias().size() + 1);
        assertThat(depois.auditorias()).containsAll(antes.auditorias());
        assertThat(relerDependente.get()).isEqualTo(fonte);
        JsonNode disponibilidadePassada = disponibilidade(c, tipo, id, fatoPassado);
        assertThat(disponibilidadePassada.path("disponibilidadeComprovada").asBoolean()).isTrue();
        assertThat(disponibilidadePassada.path("ativo").asBoolean()).isTrue();
    }

    private void registrarFonte(Cenario c, TipoCatalogoTemporal tipo, long id, LocalDate data) throws Exception {
        var req = pedido(tipo, id, data, true, "origem-sintetica-declarada");
        confirmarPrevia(c, tipo, id, req);
        enviar(c.localA, rota(tipo, id), req, 200, "Fixture: fonte sintética datada e atribuída ao ID correto");
        assertThat(estado(tipo, id).eventos()).hasSize(1);
    }

    private Map<String, Object> pedido(TipoCatalogoTemporal tipo, long id, LocalDate data, boolean ativo, String ato) {
        return mapa("dataEfetiva", data, "ativo", ativo, "versaoEsperada", estado(tipo, id).versao(),
                "motivo", "Fonte sintética 6_E " + ato, "evidencia", "SEI-SINTETICO-6E-" + tipo + "-" + id + "-" + ato,
                "autorizacao", "ATO-SINTETICO-6E-" + tipo + "-" + id,
                "chaveIdempotencia", UUID.randomUUID().toString());
    }

    private void confirmarPrevia(Cenario c, TipoCatalogoTemporal tipo, long id, Map<String, Object> req) throws Exception {
        JsonNode previa = enviar(c.localA, rota(tipo, id) + "/previa", req, 200, "Prévia canônica antes da confirmação");
        assertThat(previa.path("tokenPrevia").asText()).isNotBlank();
        req.put("tokenPrevia", previa.path("tokenPrevia").asText());
        req.put("confirmarImpacto", true);
    }

    private JsonNode disponibilidade(Cenario c, TipoCatalogoTemporal tipo, long id, LocalDate data) throws Exception {
        return consultar(c.localA, "/api/catalogos-temporais/" + tipo + "/" + id + "/disponibilidade?data=" + data,
                "Leitura temporal do catálogo pela rota canônica após commit");
    }

    private String rota(TipoCatalogoTemporal tipo, long id) {
        return "/api/catalogos-temporais/" + tipo + "/" + id + "/transicoes";
    }

    private MvcResult executarConcorrente(MockHttpServletRequestBuilder req, CountDownLatch iniciar) throws Exception {
        aguardar(iniciar, 10, "Barreira inicial não liberada");
        try {
            return mvc.perform(req).andReturn();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private List<Map<String, Object>> aguardarDuasEsperasReais(
            TipoCatalogoTemporal tipo, long id, long bloqueadora, Future<?> a, Future<?> b)
            throws InterruptedException {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        String tabela = classe(tipo).getAnnotation(jakarta.persistence.Table.class).name();
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < limite) {
            List<Map<String, Object>> esperas = jdbc.queryForList(
                    "select distinct r.PROCESSLIST_ID as solicitante, b.PROCESSLIST_ID as bloqueadora, "
                            + "l.OBJECT_NAME as tabela, l.LOCK_DATA as chave "
                            + "from performance_schema.data_lock_waits w "
                            + "join performance_schema.threads b on b.THREAD_ID=w.BLOCKING_THREAD_ID "
                            + "join performance_schema.threads r on r.THREAD_ID=w.REQUESTING_THREAD_ID "
                            + "join performance_schema.data_locks l on l.ENGINE=w.ENGINE "
                            + "and l.ENGINE_LOCK_ID=w.REQUESTING_ENGINE_LOCK_ID "
                            + "where b.PROCESSLIST_ID=? and l.OBJECT_SCHEMA=database() "
                            + "and l.OBJECT_NAME=? and l.INDEX_NAME='PRIMARY' and l.LOCK_DATA=?",
                    bloqueadora, tabela, Long.toString(id));
            if (esperas.size() >= 2) return esperas;
            if (a.isDone() || b.isDone())
                throw new AssertionError("A corrida não foi exercitada: comando terminou antes de duas esperas MySQL");
            Thread.sleep(25); // Só intervalo de sondagem; a prova é data_lock_waits.
        }
        throw new AssertionError("Fixture não observou duas conexões HTTP bloqueadas no mesmo catálogo; "
                + "conferir privilégios de performance_schema e pool (mínimo quatro conexões)");
    }

    private static void aguardar(CountDownLatch latch, int segundos, String mensagem) {
        try {
            if (!latch.await(segundos, TimeUnit.SECONDS)) throw new AssertionError(mensagem);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex);
        }
    }

    private <T> T emNovaTransacao(Supplier<T> consulta) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tx.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        tx.setReadOnly(true);
        return tx.execute(status -> {
            em.clear();
            return consulta.get();
        });
    }

    private EstadoCatalogo estado(TipoCatalogoTemporal tipo, long id) {
        // Scheduler é suprimido pela base; este bean real persiste o lote em MySQL.
        fila.descarregar();
        return emNovaTransacao(() -> {
            Object item = em.find(classe(tipo), id);
            assertThat(item).isNotNull();
            List<EventoFoto> eventos = em.createQuery("select e from EventoDisponibilidadeCatalogo e "
                            + "where e.tipoCatalogo=:tipo and e.catalogoId=:id order by e.id", EventoDisponibilidadeCatalogo.class)
                    .setParameter("tipo", tipo).setParameter("id", id).getResultList().stream()
                    .map(e -> new EventoFoto(e.getId(), e.getDataEfetiva(), e.getAtivo(), e.getAtivoAnterior(),
                            e.getChaveOrigem(), e.getMotivo(), e.getEvidencia(), e.getAutorizacao(),
                            e.getChaveIdempotencia(), e.getHashIntencao(), e.getAtivoAtualAposRegistro(),
                            e.getVersaoCatalogoApos(), e.getResponsavelId(), e.getResponsavelUsername(),
                            e.getRegistradoEm())).toList();
            List<AuditoriaFoto> auditorias = em.createQuery("select a from RegistroAuditoria a "
                            + "where a.action=:acao and a.entityType=:tipo and a.entityId=:id order by a.id", RegistroAuditoria.class)
                    .setParameter("acao", ACAO).setParameter("tipo", tipo.name()).setParameter("id", id)
                    .getResultList().stream().map(a -> new AuditoriaFoto(a.getId(), a.getCreatedAt(),
                            a.getUserId(), a.getUsername(), a.getRequestId(), a.getPath(), a.getDetailsJson())).toList();
            return new EstadoCatalogo(ativo(item), versao(item), eventos, auditorias);
        });
    }

    private FotografiaDependente contrato(long id) {
        return emNovaTransacao(() -> new FotografiaDependente(
                linhas("select c.id, c.version, c.estadoNegocial, c.tipoContratacao.id, c.dataInicio, c.dataFim, "
                        + "c.publicadoEm, c.referenciaSeiPublicacao from Contrato c where c.id=:id", id),
                linhas("select v.id, v.numeroVersao, v.dataEfetiva, v.registradoEm, v.tipoContratacaoId, "
                        + "v.tipoContratacaoDescricao, v.dataInicio, v.dataFim, origem.id "
                        + "from VersaoContratoNegocial v left join v.eventoOrigem origem "
                        + "where v.contrato.id=:id order by v.id", id),
                linhas("select e.id, e.tipoEvento, e.dataEfetiva, e.dataAto, e.registradoEm, e.motivo, "
                        + "e.referenciaSei, e.valorAnterior, e.valorNovo, e.situacaoFato "
                        + "from EventoCicloVidaContrato e where e.contrato.id=:id order by e.id", id)));
    }

    private FotografiaDependente vinculo(long id) {
        return emNovaTransacao(() -> new FotografiaDependente(
                linhas("select v.id, v.version, v.estadoNegocial, v.dataInicio, v.dataFim, m.id "
                        + "from VinculoContratual v left join v.motivoDesligamento m where v.id=:id", id),
                linhas("select e.id, e.tipoEvento, e.dataEfetiva, e.registradoEm, "
                        + "e.motivoDesligamentoIdFotografia, e.motivoDesligamentoDescricaoFotografia, "
                        + "e.motivoDesligamentoCategoriaFotografia, e.justificativa, e.evidencia, e.autorizacao, "
                        + "e.fimAnterior, e.fimNovo, e.hashAnterior, e.hashEvento "
                        + "from EventoVinculoContratual e where e.vinculo.id=:id order by e.id", id),
                linhas("select a.id, a.version, a.dataInicio, a.dataFim, a.cargo.id, a.lotacao.id, "
                        + "a.jornada.id, a.composicao.id from AlocacaoVinculo a where a.vinculo.id=:id order by a.id", id)));
    }

    private List<List<Object>> linhas(String jpql, long id) {
        return em.createQuery(jpql, Object[].class).setParameter("id", id).getResultList()
                .stream().map(Arrays::asList).toList();
    }

    private long itemSemFonte(Cenario c, TipoCatalogoTemporal tipo) {
        return emTransacao(() -> {
            GestaoLocal gestao = em.getReference(GestaoLocal.class, c.gestaoA.getId());
            String descricao = "Sem fonte 6_E " + UUID.randomUUID();
            Object item = switch (tipo) {
                case CARGO -> {
                    Cargo x = new Cargo(descricao, "1"); x.setGestaoLocal(gestao); yield x;
                }
                case JORNADA -> {
                    Jornada x = new Jornada(); x.setGestaoLocal(gestao); x.setDescricao(descricao); yield x;
                }
                case LOTACAO -> {
                    Lotacao x = new Lotacao(descricao); x.setGestaoLocal(gestao);
                    x.getTurnos().add(em.getReference(Jornada.class, c.jornadaA.getId())); yield x;
                }
                case TIPO_CONTRATACAO -> {
                    TipoContratacao x = new TipoContratacao(); x.setGestaoLocal(gestao); x.setDescricao(descricao); yield x;
                }
                case MOTIVO_DESLIGAMENTO -> {
                    MotivoDesligamento x = new MotivoDesligamento(); x.setGestaoLocal(gestao); x.setDescricao(descricao);
                    x.setCategoria(CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL); yield x;
                }
            };
            em.persist(item);
            em.flush();
            return ((Number) em.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(item)).longValue();
        });
    }

    private Class<?> classe(TipoCatalogoTemporal tipo) {
        return switch (tipo) {
            case CARGO -> Cargo.class;
            case LOTACAO -> Lotacao.class;
            case JORNADA -> Jornada.class;
            case TIPO_CONTRATACAO -> TipoContratacao.class;
            case MOTIVO_DESLIGAMENTO -> MotivoDesligamento.class;
        };
    }

    private boolean ativo(Object item) {
        if (item instanceof Cargo e) return Boolean.TRUE.equals(e.getAtivo());
        if (item instanceof Lotacao e) return Boolean.TRUE.equals(e.getAtivo());
        if (item instanceof Jornada e) return Boolean.TRUE.equals(e.getAtivo());
        if (item instanceof TipoContratacao e) return Boolean.TRUE.equals(e.getAtivo());
        return Boolean.TRUE.equals(((MotivoDesligamento) item).getAtivo());
    }

    private long versao(Object item) {
        if (item instanceof Cargo e) return e.getVersion();
        if (item instanceof Lotacao e) return e.getVersion();
        if (item instanceof Jornada e) return e.getVersion();
        if (item instanceof TipoContratacao e) return e.getVersion();
        return ((MotivoDesligamento) item).getVersion();
    }

    private void registrarEvidencia(String nome, Object valor) throws Exception {
        Path path = Path.of("target/auditoria-http/catalogos-controles-6e/" + nome + ".json");
        Files.createDirectories(path.getParent());
        Files.writeString(path, json(mapa("transporte", "MockMvc/dispatcher real; não TCP",
                "fontes", "Sintéticas e declaradas; não constituem recuperação documental", "observacoes", valor)));
    }

    private record EstadoCatalogo(boolean ativo, long versao, List<EventoFoto> eventos, List<AuditoriaFoto> auditorias) {}
    private record EventoFoto(Long id, LocalDate data, Boolean ativo, Boolean ativoAnterior,
            String chaveOrigem, String motivo, String evidencia, String autorizacao, String chaveIdempotencia,
            String hashIntencao, Boolean ativoAtualApos, Long versaoApos, Long responsavelId,
            String responsavelUsername, Instant registradoEm) {}
    private record AuditoriaFoto(Long id, Instant criadoEm, Long usuarioId, String username,
            String requestId, String path, String detalhes) {}
    private record FotografiaDependente(List<List<Object>> principal,
            List<List<Object>> fontes, List<List<Object>> complementos) {}
}
