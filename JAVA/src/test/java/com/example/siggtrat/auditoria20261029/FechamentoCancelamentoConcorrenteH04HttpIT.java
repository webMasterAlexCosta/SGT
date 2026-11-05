package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.*;
import com.example.siggtrat.servico.FrequenciaServico;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * DEF-H04-CLOSURE-CANCEL: MockMvc/security/JPA e MySQL real; não é ensaio TCP.
 * O spy identifica exclusivamente o job do cenário e executa o método real.
 * O inspector não modifica SQL nem resultados: suspende somente o segundo
 * SELECT de confirmação do progresso, depois da leitura inicial e da escrita
 * real do bloco. O cancelamento deve confirmar em outra transação antes da
 * liberação. No controle, a mesma barreira é liberada sem cancelar.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@TestPropertySource(properties = "app.homologacao.mysql8045=true")
class FechamentoCancelamentoConcorrenteH04HttpIT extends AuditoriaHttpSuporte {
    @Autowired JdbcTemplate jdbc;
    @MockitoSpyBean FrequenciaServico frequencias;

    @DynamicPropertySource
    static void propriedades(DynamicPropertyRegistry p) {
        p.add("frequencia.fechamento.chunk-size", () -> "1");
        p.add("frequencia.fechamento.executor-core-pool-size", () -> "1");
        p.add("frequencia.fechamento.executor-max-pool-size", () -> "1");
        p.add("spring.jpa.properties.hibernate.session_factory.statement_inspector",
                BarreiraProgresso.class::getName);
    }

    private static final class Coordenacao {
        final Set<Long> empregados;
        final LocalDate data;
        final AtomicReference<Long> job = new AtomicReference<>();
        final AtomicInteger confirmacoes = new AtomicInteger();
        final CompletableFuture<Instant> segundoBlocoPronto = new CompletableFuture<>();
        final CountDownLatch liberar = new CountDownLatch(1);

        Coordenacao(Set<Long> empregados, LocalDate data) {
            this.empregados = empregados;
            this.data = data;
        }
    }

    public static final class BarreiraProgresso implements StatementInspector {
        private static final AtomicReference<Coordenacao> ativa = new AtomicReference<>();
        private static final ThreadLocal<Long> jobNoBloco = new ThreadLocal<>();

        @Override
        public String inspect(String sql) {
            Coordenacao c = ativa.get();
            if (c == null || c.job.get() == null || !Objects.equals(c.job.get(), jobNoBloco.get())
                    || !Thread.currentThread().getName().startsWith("freq-fechamento-")) return sql;
            String normal = sql.toLowerCase(Locale.ROOT).replace("`", "").replaceAll("\\s+", " ");
            boolean confirmandoBloco = Arrays.stream(Thread.currentThread().getStackTrace()).anyMatch(frame ->
                    frame.getClassName().equals(FrequenciaServico.class.getName())
                            && frame.getMethodName().equals("atualizarProgressoDoFechamentoNaMesmaTransacao"));
            if (confirmandoBloco && normal.startsWith("select ")
                    && normal.contains(" from fechamento_frequencia ") && normal.contains(" for update")
                    && c.confirmacoes.incrementAndGet() == 2) {
                assertTrue(TransactionSynchronizationManager.isActualTransactionActive(),
                        "A barreira deve estar na transação real do segundo bloco");
                c.segundoBlocoPronto.complete(Instant.now());
                try {
                    if (!c.liberar.await(45, TimeUnit.SECONDS))
                        throw new AssertionError("O cenário não liberou o SELECT real de confirmação do segundo bloco.");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Barreira de teste interrompida", e);
                }
            }
            return sql;
        }
    }

    private record Massa(Cenario cenario, Set<Long> empregados, Set<Long> vinculos) {}
    private record Fotografia(Map<String, String> identidade, Map<String, String> job,
                              List<Map<String, String>> frequencias, List<Map<String, String>> eventos) {}

    @ParameterizedTest(name = "cancelar={0}, gestorLocal={1}")
    @CsvSource({"false,false", "true,false", "false,true", "true,true"})
    @Timeout(value = 110, unit = TimeUnit.SECONDS)
    void cancelamentoConfirmadoDuranteBlocoPreservaPrefixoEPermiteReprocessar(boolean cancelar,
                                                                           boolean gestorLocal) throws Exception {
        Massa massa = criarMassa();
        Cenario c = massa.cenario();
        UsuarioAplicacao ator = gestorLocal ? c.localA : c.operacionalA;
        LocalDate data = escolherDiaUtilSemEventoDeCalendario(massa);
        Coordenacao coordenacao = new Coordenacao(massa.empregados(), data);
        assertTrue(BarreiraProgresso.ativa.compareAndSet(null, coordenacao));
        doAnswer(invocacao -> {
            List<Long> ids = invocacao.getArgument(1);
            LocalDate referencia = invocacao.getArgument(2);
            if (!coordenacao.data.equals(referencia) || ids.isEmpty()
                    || !coordenacao.empregados.containsAll(ids)) return invocacao.callRealMethod();
            Long id = invocacao.getArgument(0);
            coordenacao.job.compareAndSet(null, id);
            assertEquals(coordenacao.job.get(), id, "Um único job pertence à barreira deste cenário");
            BarreiraProgresso.jobNoBloco.set(id);
            try { return invocacao.callRealMethod(); }
            finally { BarreiraProgresso.jobNoBloco.remove(); }
        }).when(frequencias).processarBlocoFechamento(anyLong(), anyList(), any(LocalDate.class), anyMap(), anyLong());

        ExecutorService requisicao = Executors.newSingleThreadExecutor();
        Map<String, Object> prova = new LinkedHashMap<>();
        prova.put("layer", "MockMvc/security/JPA/MySQL real; no TCP claim");
        prova.put("cancelar", cancelar);
        prova.put("gestorLocal", gestorLocal);
        prova.put("massa", 3);
        prova.put("chunk", 1);
        prova.put("dataReferencia", data.toString());
        prova.put("started", Instant.now().toString());
        try {
            String justificativa = "Conferência sintética do fechamento concorrente " + UUID.randomUUID();
            JsonNode criado = dados(executar(ator, post("/api/frequencias/fechamentos")
                    .contentType(APPLICATION_JSON).content(json(Map.of(
                            "dataReferencia", data, "lotacaoId", c.lotacaoA.getId(),
                            "jornadaId", c.jornadaA.getId(), "contratoIds", List.of(c.contratoA.getId()),
                            "alteracoes", List.of(), "justificativaRetificacao", justificativa,
                            "chaveIdempotencia", "cancelamento-concorrente-" + UUID.randomUUID()))),
                    202, "Job real aceito antes da concorrência controlada"));
            long id = criado.path("id").asLong();
            assertTrue(id > 0);
            prova.put("jobId", id);
            prova.put("barrierReached", coordenacao.segundoBlocoPronto.get(25, TimeUnit.SECONDS).toString());
            assertEquals(id, coordenacao.job.get());
            Fotografia prefixo = fotografar(id, data, c);
            prova.put("prefixoAntesDoCancelamento", prefixo);
            assertEquals("PROCESSANDO", prefixo.job().get("status"));
            assertEquals(1, prefixo.frequencias().size(), "O primeiro bloco já confirmou antes da barreira do segundo");
            assertEquals(1, prefixo.eventos().size());
            assertEquals("PRESENCA", prefixo.frequencias().get(0).get("status"),
                    "Controle da jornada/calendário antes do POST de cancelamento");
            verificarContadores(prefixo, 1, 3);
            assertEquals("0", prefixo.job().get("cancelamento_solicitado"));
            long versaoAnterior = Long.parseLong(prefixo.job().get("version"));

            Fotografia cancelamentoConfirmado = null;
            if (cancelar) {
                var resposta = requisicao.submit(() -> executar(ator,
                        post("/api/frequencias/fechamentos/{id}/cancelamento", id), 202,
                        "Cancelamento HTTP confirma enquanto o segundo bloco permanece suspenso"));
                JsonNode pedido = dados(resposta.get(15, TimeUnit.SECONDS));
                assertTrue(pedido.path("cancelamentoSolicitado").asBoolean());
                assertEquals(1, pedido.path("tentativa").asInt());
                cancelamentoConfirmado = fotografar(id, data, c);
                prova.put("cancelamentoConfirmadoAntesDeLiberarWorker", cancelamentoConfirmado);
                assertEquals("1", cancelamentoConfirmado.job().get("cancelamento_solicitado"));
                assertEquals("PROCESSANDO", cancelamentoConfirmado.job().get("status"));
                assertEquals(versaoAnterior + 1, Long.parseLong(cancelamentoConfirmado.job().get("version")),
                        "O POST confirmou uma versão nova do job em outra transação");
                assertEquals(prefixo.frequencias(), cancelamentoConfirmado.frequencias(),
                        "A escrita ainda não confirmada do segundo bloco não aparece no snapshot");
                assertEquals(prefixo.eventos(), cancelamentoConfirmado.eventos());
                verificarContadores(cancelamentoConfirmado, 1, 3);
            }

            coordenacao.liberar.countDown();
            JsonNode fim = aguardar(ator, id);
            Fotografia terminado = fotografar(id, data, c);
            prova.put("terminalHttp", fim);
            prova.put("terminalSql", terminado);
            assertEquals(cancelar ? "CANCELADO" : "CONCLUIDO", fim.path("status").asText(),
                    "Cancelamento cooperativo não pode converter uma troca legítima de versão em ERRO: " + fim);
            assertEquals(fim.path("status").asText(), terminado.job().get("status"));
            assertEquals("1", terminado.job().get("tentativa"));
            assertNull(terminado.job().get("detalhe_erro"));
            preservar(prefixo, terminado);
            int confirmados = terminado.frequencias().size();
            if (cancelar) assertTrue(confirmados >= 1 && confirmados < 3,
                    "Pode confirmar o bloco em curso; deve parar antes de percorrer o restante");
            else assertEquals(3, confirmados, "Controle sem cancelamento conclui os três vínculos reais");
            verificarContadores(terminado, confirmados, 3);
            assertEquals(confirmados, terminado.eventos().size());
            assertEquals(cancelar ? "1" : "0", terminado.job().get("cancelamento_solicitado"));
            assertTrue(Long.parseLong(terminado.job().get("version")) > versaoAnterior);
            if (cancelar) {
                assertTrue(Long.parseLong(terminado.job().get("version"))
                        > Long.parseLong(cancelamentoConfirmado.job().get("version")));
                JsonNode reinicio = dados(executar(ator,
                        post("/api/frequencias/fechamentos/{id}/reprocessamento", id), 202,
                        "Mesmo job cancelado é reprocessado sem apagar o prefixo"));
                assertEquals(id, reinicio.path("id").asLong());
                assertEquals(2, reinicio.path("tentativa").asInt());
                JsonNode concluido = aguardar(ator, id);
                Fotografia reprocessado = fotografar(id, data, c);
                prova.put("reprocessamentoHttp", concluido);
                prova.put("reprocessamentoSql", reprocessado);
                assertEquals("CONCLUIDO", concluido.path("status").asText(), concluido.toString());
                assertEquals("2", reprocessado.job().get("tentativa"));
                assertEquals("0", reprocessado.job().get("cancelamento_solicitado"));
                assertEquals(3, reprocessado.frequencias().size());
                assertEquals(3, reprocessado.eventos().size());
                verificarContadores(reprocessado, 3 - confirmados, 3 - confirmados);
                preservar(terminado, reprocessado);
                terminado = reprocessado;
            }
            assertEquals(massa.vinculos(), terminado.frequencias().stream()
                    .map(row -> Long.valueOf(row.get("id_vinculo_contratual"))).collect(java.util.stream.Collectors.toSet()));
            assertEquals(3, terminado.frequencias().stream().map(row -> row.get("id_pessoa")).distinct().count());
            assertTrue(terminado.frequencias().stream().allMatch(row -> "PRESENCA".equals(row.get("status"))));
            if (gestorLocal) {
                assertTrue(terminado.eventos().stream().allMatch(row -> justificativa.equals(row.get("motivo"))),
                        "A leitura inicial mantém a justificativa gerencial em todos os eventos");
                assertTrue(terminado.eventos().stream().allMatch(row -> row.get("chave_idempotencia") != null
                        && row.get("chave_idempotencia").startsWith("fechamento:" + id + ":")));
                assertEquals(3, terminado.eventos().stream().map(row -> row.get("chave_idempotencia")).distinct().count());
            }
            prova.put("status", "PASS");
        } catch (Exception | AssertionError erro) {
            prova.put("status", "FAIL");
            prova.put("failureType", erro.getClass().getName());
            throw erro;
        } finally {
            coordenacao.liberar.countDown();
            BarreiraProgresso.ativa.compareAndSet(coordenacao, null);
            requisicao.shutdownNow();
            requisicao.awaitTermination(5, TimeUnit.SECONDS);
            prova.put("ended", Instant.now().toString());
            Path destino = Path.of("target/auditoria-http/fechamento-cancelamento-concorrente-"
                    + cancelar + "-" + gestorLocal + "-" + coordenacao.job.get() + ".json");
            Files.createDirectories(destino.getParent());
            Path temporario = destino.resolveSibling(destino.getFileName() + ".complete.tmp");
            Files.writeString(temporario, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(prova));
            Files.move(temporario, destino, StandardCopyOption.ATOMIC_MOVE);
        }
    }

    private Massa criarMassa() {
        Cenario c = criarCenarioPersistido();
        return emTransacao(() -> {
            Set<Long> empregados = new LinkedHashSet<>(List.of(c.empregadoA.getId()));
            Set<Long> vinculos = new LinkedHashSet<>(List.of(c.vinculoA.getId()));
            Contrato contrato = em.find(Contrato.class, c.contratoA.getId());
            ComposicaoContratual composicao = em.find(ComposicaoContratual.class, c.composicaoA.getId());
            int semente = 90000000;
            for (int i = 0; i < 2; i++) {
                String documento;
                do { documento = cpf(semente++); }
                while (em.createQuery("select count(p) from Pessoa p where p.cpf=:cpf", Long.class)
                        .setParameter("cpf", documento).getSingleResult() != 0);
                Pessoa pessoa = pessoa("Pessoa cancelamento " + UUID.randomUUID(), documento);
                VinculoContratual vinculo = vinculo(pessoa, contrato);
                AlocacaoVinculo alocacao = alocacao(vinculo, composicao);
                Empregado empregado = projecao.projetar(vinculo, alocacao);
                empregados.add(empregado.getId());
                vinculos.add(vinculo.getId());
            }
            em.flush();
            return new Massa(c, empregados, vinculos);
        });
    }

    private Fotografia fotografar(long id, LocalDate data, Cenario c) {
        TransactionTemplate leitura = new TransactionTemplate(transactionManager);
        leitura.setReadOnly(true);
        leitura.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        return leitura.execute(status -> {
            Map<String, String> identidade = linhas("SELECT @@transaction_read_only AS tx_read_only, "
                    + "@@transaction_isolation AS tx_isolation, CONNECTION_ID() AS connection_id, "
                    + "VERSION() AS server_version, @@server_uuid AS server_uuid").get(0);
            assertEquals("1", identidade.get("tx_read_only"));
            assertEquals("REPEATABLE-READ", identidade.get("tx_isolation"));
            assertEquals("8.0.45", identidade.get("server_version"));
            String uuidEsperado = System.getenv("SGT_MYSQL_SERVER_UUID");
            if (uuidEsperado != null && !uuidEsperado.isBlank()) {
                assertEquals(uuidEsperado, identidade.get("server_uuid"),
                        "A conexão real deve corresponder ao UUID explícito da campanha");
                identidade.put("expected_server_uuid", uuidEsperado);
            }
            Map<String, String> job = linhas("SELECT * FROM fechamento_frequencia WHERE id=?", id).get(0);
            var frequencias = linhas("SELECT * FROM frequencia WHERE data_referencia=? "
                            + "AND lotacao_snapshot_id=? AND jornada_snapshot_id=? AND contrato_snapshot_id=? ORDER BY id",
                    java.sql.Date.valueOf(data), c.lotacaoA.getId(), c.jornadaA.getId(), c.contratoA.getId());
            var eventos = linhas("SELECT e.* FROM evento_frequencia e JOIN frequencia f ON f.id=e.id_frequencia "
                            + "WHERE f.data_referencia=? AND f.lotacao_snapshot_id=? AND f.jornada_snapshot_id=? "
                            + "AND f.contrato_snapshot_id=? ORDER BY e.id_evento_frequencia",
                    java.sql.Date.valueOf(data), c.lotacaoA.getId(), c.jornadaA.getId(), c.contratoA.getId());
            return new Fotografia(identidade, job, frequencias, eventos);
        });
    }

    private List<Map<String, String>> linhas(String sql, Object... argumentos) {
        return jdbc.query(sql, (rs, numero) -> {
            Map<String, String> row = new TreeMap<>();
            var metadados = rs.getMetaData();
            for (int coluna = 1; coluna <= metadados.getColumnCount(); coluna++)
                row.put(metadados.getColumnLabel(coluna), rs.getString(coluna));
            return row;
        }, argumentos);
    }

    private LocalDate escolherDiaUtilSemEventoDeCalendario(Massa massa) {
        TransactionTemplate leitura = new TransactionTemplate(transactionManager);
        leitura.setReadOnly(true);
        leitura.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        return leitura.execute(status -> {
            assertEquals(0L, em.createQuery("select count(e) from FrequenciaExcecao e "
                            + "where e.empregado.id in :ids", Long.class)
                    .setParameter("ids", massa.empregados()).getSingleResult(),
                    "A massa nova não contém exceções individuais prévias");
            for (int dias = 1; dias <= 7; dias++) {
                LocalDate data = RelogioSistema.hoje().minusDays(dias);
                if (data.getDayOfWeek() == DayOfWeek.SATURDAY || data.getDayOfWeek() == DayOfWeek.SUNDAY)
                    continue;
                long eventos = em.createQuery("select count(e) from CalendarioEvento e where e.data=:data "
                                + "and e.ativo=true and e.cancelado=false "
                                + "and upper(e.tipo) in ('FERIADO','DIA_DESCANSO')", Long.class)
                        .setParameter("data", data).getSingleResult();
                // Precondição conservadora: nenhum evento ativo em qualquer
                // escopo nesse dia. Não remove nem altera calendário existente.
                if (eventos == 0) return data;
            }
            return fail("Preparo: não há dia útil sem evento de calendário na janela operacional de sete dias.");
        });
    }

    private void verificarContadores(Fotografia foto, long processados, long previstos) {
        assertEquals(processados, Long.parseLong(foto.job().get("total_processado")));
        assertEquals(processados, Long.parseLong(foto.job().get("total_inserido")));
        assertEquals(previstos, Long.parseLong(foto.job().get("total_previsto")));
        assertEquals("0", foto.job().get("total_atualizado"));
        assertEquals("0", foto.job().get("total_preservado"));
        long ultimo = foto.frequencias().stream().mapToLong(row -> Long.parseLong(row.get("id_empregado"))).max().orElseThrow();
        assertEquals(ultimo, Long.parseLong(foto.job().get("ultimo_empregado_id_processado")),
                "Cursor e frequências pertencem ao mesmo snapshot confirmado");
    }

    private void preservar(Fotografia antes, Fotografia depois) {
        for (var row : antes.frequencias())
            assertTrue(depois.frequencias().contains(row), "Todos os campos da frequência confirmada são preservados");
        for (var row : antes.eventos())
            assertTrue(depois.eventos().contains(row), "Todos os campos dos eventos confirmados são preservados");
        for (String campo : List.of("id", "chave_escopo", "data_referencia", "id_lotacao", "id_jornada",
                "contrato_ids", "id_usuario_criador", "criado_em", "justificativa_retificacao", "chave_idempotencia"))
            assertEquals(antes.job().get(campo), depois.job().get(campo), "Identidade e intenção imutáveis: " + campo);
    }

    private JsonNode aguardar(UsuarioAplicacao usuario, long id) throws Exception {
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        JsonNode estado;
        do {
            estado = dados(executar(usuario, get("/api/frequencias/fechamentos/{id}", id), 200,
                    "Consulta autenticada do job sob teste"));
            if (Set.of("CONCLUIDO", "CANCELADO", "ERRO").contains(estado.path("status").asText())) return estado;
            TimeUnit.MILLISECONDS.sleep(75);
        } while (System.nanoTime() < limite);
        return fail("Job não atingiu um estado terminal na janela de 30 segundos: " + estado);
    }
}
