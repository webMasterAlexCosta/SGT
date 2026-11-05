package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dto.CorrecaoIdentidadePessoaRequisicao;
import com.example.siggtrat.entidade.Empregado;
import com.example.siggtrat.entidade.Pessoa;
import com.example.siggtrat.servico.PessoaServico;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * PER-007:002 / COL-005 / COL-006 / COL-007: correção excepcional usa HTTP real,
 * JPA e MySQL. O spy chama PessoaServico real e apenas retém o retorno até que
 * outra transação esteja bloqueada na identidade persistida ainda sem commit.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AuditoriaFinalCorrecaoCpfControlesHttpIT extends ContratosHttpBase {
    @MockitoSpyBean PessoaServico pessoaServico;

    @Test
    void cpfNaoUniformeComDigitoAdulteradoRecusaCorrecaoEPreservaIdentidadeETrilha() throws Exception {
        Cenario c = criarCenarioPersistido();
        NovoContrato n = tx(() -> publicado(c, 5, null, false));
        long pessoaId = cadastrar(c, n, 1);
        String valido = cpf(Math.toIntExact(631000000L + n.id() * 10L + 3L));
        assertThat(contarCpf("Pessoa", valido)).isZero();
        enviar(c.localA, rota(pessoaId), correcao("CPF", valido, "CONTROLE-VALIDO"), 200,
                "Pré-condição: correção válida e documentada confirma no mesmo fluxo excepcional");
        Estado antes = estado(pessoaId);
        assertThat(antes.cpf()).isEqualTo(valido);
        assertThat(antes.cpfEmpregado()).isEqualTo(valido);
        assertThat(antes.correcoes()).hasSize(1);
        JsonNode trilhaAntes = consultar(c.localA, rota(pessoaId), "Trilha real preexistente à tentativa inválida");

        String invalido = valido.substring(0, 10) + ((valido.charAt(10) - '0' + 1) % 10);
        assertThat(invalido).matches("\\d{11}");
        assertThat(invalido.chars().distinct().count()).as("CPF inválido não é uma sequência uniforme").isGreaterThan(1L);
        assertThat(invalido.substring(0, 10)).isEqualTo(valido.substring(0, 10));
        assertThat(invalido.charAt(10)).isNotEqualTo(valido.charAt(10));
        JsonNode recusada = enviar(c.localA, rota(pessoaId), correcao("CPF", invalido, "DIGITO-ADULTERADO"), 400,
                "COL-006/PER-007: validação aritmética também incide sobre correção excepcional");
        assertThat(recusada.toString()).contains("CPF inválido");
        assertThat(estado(pessoaId)).as("Nova transação: Pessoa, projeção, vínculos, faixas, eventos e correções intactos")
                .isEqualTo(antes);
        assertThat(consultar(c.localA, rota(pessoaId), "A tentativa inválida não produz uma correção aparente"))
                .isEqualTo(trilhaAntes);
        assertThat(contarCpf("Pessoa", invalido)).isZero();
        assertThat(contarCpf("Empregado", invalido)).isZero();
    }

    @Test
    @Timeout(value = 90, unit = TimeUnit.SECONDS)
    void correcoesDePessoasDistintasDisputamMesmoCpfComEsperaRealERollbackDaPerdedora() throws Exception {
        Cenario c = criarCenarioPersistido();
        NovoContrato n = tx(() -> publicado(c, 5, null, false));
        long pessoaA = cadastrar(c, n, 4);
        long pessoaB = cadastrar(c, n, 5);
        assertThat(pessoaA).isNotEqualTo(pessoaB);
        // Uma trilha legítima em cada identidade torna a preservação verificável
        // também quando a tentativa concorrente perde antes de criar sua correção.
        enviar(c.localA, rota(pessoaA), correcao("NOME", "Nome documental A " + n.id(), "ORIGEM-A"), 200,
                "Pré-condição: correção anterior legítima da Pessoa A");
        enviar(c.localA, rota(pessoaB), correcao("NOME", "Nome documental B " + n.id(), "ORIGEM-B"), 200,
                "Pré-condição: correção anterior legítima da Pessoa B");
        Estado antesA = estado(pessoaA);
        Estado antesB = estado(pessoaB);
        assertThat(antesA.empregadoId()).isNotEqualTo(antesB.empregadoId());
        assertThat(antesA.correcoes()).hasSize(1);
        assertThat(antesB.correcoes()).hasSize(1);
        JsonNode trilhaAntesA = consultar(c.localA, rota(pessoaA), "Trilha anterior da Pessoa A");
        JsonNode trilhaAntesB = consultar(c.localA, rota(pessoaB), "Trilha anterior da Pessoa B");
        String destino = cpf(Math.toIntExact(631000000L + n.id() * 10L + 6L));
        assertThat(contarCpf("Pessoa", destino)).isZero();
        assertThat(contarCpf("Empregado", destino)).isZero();
        var reqA = correcao("CPF", destino, "CONCORRENTE-A");
        var reqB = correcao("CPF", destino, "CONCORRENTE-B");
        // Sessões prontas no chamador: nenhum cache de autenticação da fixture
        // é inicializado simultaneamente pelos workers.
        var httpA = autenticar(post(rota(pessoaA)).contentType(MediaType.APPLICATION_JSON).content(json(reqA)), c.localA);
        var httpB = autenticar(post(rota(pessoaB)).contentType(MediaType.APPLICATION_JSON).content(json(reqB)), c.localA);
        var conexaoBloqueadora = new CompletableFuture<Long>();
        var conexaoSegunda = new CompletableFuture<Long>();
        var etapaA = new AtomicReference<>("ANTES_HTTP");
        var etapaB = new AtomicReference<>("ANTES_HTTP");
        var threadA = new AtomicReference<Thread>();
        var threadB = new AtomicReference<Thread>();
        var liberarCommit = new CountDownLatch(1);
        doAnswer(invocacao -> {
            long pessoa = invocacao.getArgument(0, Long.class);
            if (pessoa != pessoaA && pessoa != pessoaB) return invocacao.callRealMethod();
            var etapa = pessoa == pessoaA ? etapaA : etapaB;
            try {
                assertThat(TransactionSynchronizationManager.isActualTransactionActive())
                        .as("O spy está dentro da transação real do HTTP, antes do commit").isTrue();
                long conexaoEntrada = ((Number) em.createNativeQuery("select connection_id()")
                        .getSingleResult()).longValue();
                etapa.set("ENTROU_SERVICO_REAL");
                if (pessoa == pessoaB) conexaoSegunda.complete(conexaoEntrada);
                Object resposta = invocacao.callRealMethod();
                etapa.set("SERVICO_REAL_RETORNOU");
                if (pessoa == pessoaB) return resposta;
                em.flush();
                assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
                long conexaoAposFlush = ((Number) em.createNativeQuery("select connection_id()")
                        .getSingleResult()).longValue();
                assertThat(conexaoAposFlush).as("A permanece na mesma transação/conexão até o flush")
                        .isEqualTo(conexaoEntrada);
                etapa.set("FLUSH_CONFIRMADO_AGUARDA_LIBERACAO");
                conexaoBloqueadora.complete(conexaoAposFlush);
                assertThat(liberarCommit.await(25, TimeUnit.SECONDS))
                        .as("Commit só é liberado depois da espera real da segunda correção").isTrue();
                etapa.set("LIBERADA_PARA_RETORNAR_AO_CONTROLLER");
                return resposta;
            } catch (Throwable falha) {
                etapa.set("FALHA_" + falha.getClass().getSimpleName());
                (pessoa == pessoaA ? conexaoBloqueadora : conexaoSegunda).completeExceptionally(falha);
                throw falha;
            }
        }).when(pessoaServico).corrigirIdentidade(anyLong(), any(CorrecaoIdentidadePessoaRequisicao.class));

        var executor = Executors.newFixedThreadPool(2);
        Instant inicio = Instant.now().minusSeconds(1);
        MvcResult respostaA;
        MvcResult respostaB;
        Future<MvcResult> a = null;
        Future<MvcResult> b = null;
        boolean gateConfirmado = false;
        try {
            a = executor.submit(() -> {
                threadA.set(Thread.currentThread()); etapaA.set("HTTP_INICIADO");
                return mvc.perform(httpA).andReturn();
            });
            long bloqueador = conexaoBloqueadora.get(15, TimeUnit.SECONDS);
            b = executor.submit(() -> {
                threadB.set(Thread.currentThread()); etapaB.set("HTTP_INICIADO");
                return mvc.perform(httpB).andReturn();
            });
            try {
                long solicitante = conexaoSegunda.get(10, TimeUnit.SECONDS);
                assertThat(solicitante).as("B entrou no serviço real usando uma conexão independente")
                        .isNotEqualTo(bloqueador);
                aguardarDisputaDeIdentidade(bloqueador, solicitante, a, b);
                gateConfirmado = true;
            } catch (Throwable falhaGate) {
                diagnosticarEspera(bloqueador, conexaoSegunda, etapaA.get(), etapaB.get(), threadA.get(), threadB.get());
                throw falhaGate;
            }
            liberarCommit.countDown();
            respostaA = a.get(20, TimeUnit.SECONDS);
            respostaB = b.get(20, TimeUnit.SECONDS);
        } finally {
            liberarCommit.countDown();
            // Falha do gate não deve interromper A e produzir um rollback artificial
            // que seja confundido com o resultado funcional de unicidade.
            executor.shutdown();
            boolean encerrado = executor.awaitTermination(10, TimeUnit.SECONDS);
            if (!gateConfirmado) System.out.println("CPF_CONCORRENCIA_RESULTADOS_APOS_LIBERACAO " + json(mapa(
                    "A", resultadoDiagnostico(a), "B", resultadoDiagnostico(b),
                    "etapaA", etapaA.get(), "etapaB", etapaB.get(), "workersEncerrados", encerrado)));
            if (!encerrado) executor.shutdownNow();
            assertThat(encerrado).as("Workers encerrados depois de concluir suas próprias transações").isTrue();
        }
        Instant fim = Instant.now().plusSeconds(1);
        assertAll(
                () -> assertThat(respostaA.getResponse().getStatus())
                        .as("Primeira transação: %s", respostaA.getResponse().getContentAsString()).isEqualTo(200),
                () -> assertThat(respostaB.getResponse().getStatus())
                        .as("Transação que aguardou unicidade: %s", respostaB.getResponse().getContentAsString()).isEqualTo(409));
        JsonNode confirmada = dados(respostaA);
        assertThat(confirmada.path("id").asLong()).isEqualTo(pessoaA);
        assertThat(confirmada.path("cpf").asText()).isEqualTo(destino);
        assertThat(contarCpf("Pessoa", destino)).isEqualTo(1L);
        assertThat(contarCpf("Empregado", destino)).isEqualTo(1L);
        Estado depoisA = estado(pessoaA);
        assertThat(depoisA.pessoaId()).isEqualTo(antesA.pessoaId());
        assertThat(depoisA.empregadoId()).isEqualTo(antesA.empregadoId());
        assertThat(depoisA.pessoaCanonicaId()).isEqualTo(pessoaA);
        assertThat(depoisA.cpf()).isEqualTo(destino);
        assertThat(depoisA.cpfEmpregado()).isEqualTo(destino);
        assertThat(depoisA.nome()).isEqualTo(antesA.nome());
        assertThat(depoisA.vinculos()).isEqualTo(antesA.vinculos());
        assertThat(depoisA.alocacoes()).isEqualTo(antesA.alocacoes());
        assertThat(depoisA.eventos()).as("Correção excepcional não reescreve fotografias/hashes da história")
                .isEqualTo(antesA.eventos());
        assertThat(depoisA.correcoes()).hasSize(2).containsAll(antesA.correcoes());
        assertThat(estado(pessoaB)).as("Rollback da perdedora preserva inclusive versões, instantes e trilha anterior")
                .isEqualTo(antesB);
        assertThat(consultar(c.localA, rota(pessoaB), "Trilha HTTP da identidade perdedora preservada"))
                .isEqualTo(trilhaAntesB);
        JsonNode trilhaDepoisA = consultar(c.localA, rota(pessoaA), "Só a vencedora ganha correção de CPF");
        assertThat(trilhaDepoisA).hasSize(2);
        assertThat(trilhaDepoisA.get(0)).isEqualTo(trilhaAntesA.get(0));
        JsonNode aplicada = trilhaDepoisA.get(1);
        assertThat(aplicada.path("tipo").asText()).isEqualTo("CPF");
        assertThat(aplicada.path("valorAnterior").asText()).isEqualTo(antesA.cpf());
        assertThat(aplicada.path("valorNovo").asText()).isEqualTo(destino);
        assertThat(aplicada.path("responsavelId").asLong()).isEqualTo(c.localA.getId());
        assertThat(aplicada.path("responsavelUsername").asText()).isEqualTo(c.localA.getUsername());
        assertThat(Instant.parse(aplicada.path("registradoEm").asText())).isBetween(inicio, fim);
        for (String campo : List.of("motivo", "evidencia", "autorizacao", "impactosEncontrados"))
            assertThat(aplicada.path(campo).asText()).isEqualTo(reqA.get(campo));
        tx(() -> {
            assertThat(em.createQuery("select count(p) from Pessoa p where p.id in :ids", Long.class)
                    .setParameter("ids", List.of(pessoaA, pessoaB)).getSingleResult()).isEqualTo(2L);
            assertThat(em.createQuery("select e.pessoaCanonica.id from Empregado e where e.pessoaCanonica.id in :ids", Long.class)
                    .setParameter("ids", List.of(pessoaA, pessoaB)).getResultList()).containsExactlyInAnyOrder(pessoaA, pessoaB);
            return null;
        });
    }

    private void aguardarDisputaDeIdentidade(long bloqueador, long solicitante,
                                             Future<?> a, Future<?> b) throws Exception {
        // O THREAD_ID de um lock RECORD implícito materializado pode pertencer
        // a outro executor (MySQL bug 104431). Correlacionamos os donos pelo
        // ENGINE_TRANSACTION_ID e pelos locks TABLE/IX explícitos que cada
        // conexão adquiriu antes de alterar Pessoa/Empregado. Assim não precisamos
        // de INNODB_TRX/PROCESS e não confundimos criador da estrutura com dono.
        // https://dev.mysql.com/doc/refman/8.0/en/innodb-locking.html
        // https://dev.mysql.com/doc/refman/8.0/en/performance-schema-data-locks-table.html
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        List<Object[]> esperas = List.of();
        do {
            esperas = tx(() -> em.createNativeQuery("""
                    select distinct retentor.PROCESSLIST_ID as conexao_bloqueadora,
                           requisitante.PROCESSLIST_ID as conexao_solicitante,
                           cast(w.BLOCKING_ENGINE_TRANSACTION_ID as char) as transacao_bloqueadora,
                           cast(w.REQUESTING_ENGINE_TRANSACTION_ID as char) as transacao_solicitante,
                           l.OBJECT_NAME as tabela_solicitada, l.INDEX_NAME as indice_solicitado,
                           l.LOCK_TYPE as tipo_lock_solicitado, l.LOCK_STATUS as estado_lock_solicitado,
                           ancora_retida.LOCK_MODE as intencao_conexao_bloqueadora,
                           ancora_solicitada.LOCK_MODE as intencao_conexao_solicitante,
                           detido.LOCK_STATUS as estado_lock_bloqueador
                    from performance_schema.data_lock_waits w
                    join performance_schema.data_locks ancora_retida
                        on ancora_retida.ENGINE_TRANSACTION_ID=w.BLOCKING_ENGINE_TRANSACTION_ID
                        and ancora_retida.ENGINE=w.ENGINE and ancora_retida.LOCK_TYPE='TABLE'
                        and ancora_retida.LOCK_STATUS='GRANTED' and ancora_retida.LOCK_MODE='IX'
                    join performance_schema.threads retentor on retentor.THREAD_ID=ancora_retida.THREAD_ID
                    join performance_schema.data_locks ancora_solicitada
                        on ancora_solicitada.ENGINE_TRANSACTION_ID=w.REQUESTING_ENGINE_TRANSACTION_ID
                        and ancora_solicitada.ENGINE=w.ENGINE and ancora_solicitada.LOCK_TYPE='TABLE'
                        and ancora_solicitada.LOCK_STATUS='GRANTED' and ancora_solicitada.LOCK_MODE='IX'
                    join performance_schema.threads requisitante on requisitante.THREAD_ID=ancora_solicitada.THREAD_ID
                    join performance_schema.data_locks l
                        on l.ENGINE_LOCK_ID=w.REQUESTING_ENGINE_LOCK_ID and l.ENGINE=w.ENGINE
                        and l.ENGINE_TRANSACTION_ID=w.REQUESTING_ENGINE_TRANSACTION_ID
                    join performance_schema.data_locks detido
                        on detido.ENGINE_LOCK_ID=w.BLOCKING_ENGINE_LOCK_ID and detido.ENGINE=w.ENGINE
                        and detido.ENGINE_TRANSACTION_ID=w.BLOCKING_ENGINE_TRANSACTION_ID
                    where retentor.PROCESSLIST_ID=:bloqueador
                        and requisitante.PROCESSLIST_ID=:solicitante
                        and w.BLOCKING_ENGINE_TRANSACTION_ID<>w.REQUESTING_ENGINE_TRANSACTION_ID
                        and l.OBJECT_SCHEMA=database()
                        and l.OBJECT_NAME in ('pessoa','empregado')
                        and l.LOCK_TYPE='RECORD' and l.LOCK_STATUS='WAITING'
                        and ancora_retida.OBJECT_SCHEMA=l.OBJECT_SCHEMA
                        and ancora_retida.OBJECT_NAME=l.OBJECT_NAME
                        and ancora_solicitada.OBJECT_SCHEMA=l.OBJECT_SCHEMA
                        and ancora_solicitada.OBJECT_NAME=l.OBJECT_NAME
                        and detido.LOCK_TYPE='RECORD' and detido.LOCK_STATUS='GRANTED'
                        and detido.OBJECT_SCHEMA=l.OBJECT_SCHEMA
                        and detido.OBJECT_NAME=l.OBJECT_NAME and detido.INDEX_NAME=l.INDEX_NAME
                        and exists (
                            select 1 from information_schema.statistics indice
                            where indice.TABLE_SCHEMA=l.OBJECT_SCHEMA and indice.TABLE_NAME=l.OBJECT_NAME
                                and indice.INDEX_NAME=l.INDEX_NAME and indice.NON_UNIQUE=0
                            group by indice.INDEX_NAME
                            having count(*)=1 and max(indice.COLUMN_NAME)='cpf'
                        )
                    """, Object[].class).setParameter("bloqueador", bloqueador)
                    .setParameter("solicitante", solicitante).getResultList());
            assertThat(a.isDone()).as("Vencedora continua aguardando liberação, sem commit antecipado").isFalse();
            assertThat(b.isDone()).as("Perdedora está em curso, aguardando a identidade ainda não confirmada").isFalse();
            if (!esperas.isEmpty()) break;
            // A aprovação depende da aresta e dos locks observados, não do tempo decorrido.
            TimeUnit.MILLISECONDS.sleep(50);
        } while (System.nanoTime() < limite);
        assertThat(esperas).as("MySQL deve comprovar que a transação de B espera a transação de A no CPF único")
                .isNotEmpty();
        for (Object[] espera : esperas) {
            assertThat(((Number) espera[0]).longValue()).as("Dono do lock retido é a conexão A").isEqualTo(bloqueador);
            assertThat(((Number) espera[1]).longValue()).as("Dono do lock solicitado é a conexão B").isEqualTo(solicitante);
            assertThat(espera[2].toString()).matches("[0-9]+").isNotEqualTo(espera[3].toString());
            assertThat(espera[3].toString()).matches("[0-9]+");
            assertThat(espera[4].toString()).isIn("pessoa", "empregado");
            assertThat(espera[5]).as("O índice persistido é único e composto somente por CPF").isNotNull();
            assertThat(espera[6]).isEqualTo("RECORD");
            assertThat(espera[7]).isEqualTo("WAITING");
            assertThat(espera[8]).as("Lock de intenção explícito liga a transação à conexão A").isEqualTo("IX");
            assertThat(espera[9]).as("Lock de intenção explícito liga a transação à conexão B").isEqualTo("IX");
            assertThat(espera[10]).isEqualTo("GRANTED");
        }
        // Evidência sanitizada do gate, sem CPF, lock_data, SQL de negócio ou credenciais.
        System.out.println("CPF_CONCORRENCIA_PROVA_DONOS " + json(mapa(
                "conexaoA", bloqueador, "conexaoB", solicitante,
                "ambosFuturesPendentes", !a.isDone() && !b.isDone(),
                "colunas", List.of("conexaoDonoBloqueador", "conexaoDonoSolicitante", "transacaoBloqueadora",
                        "transacaoSolicitante", "tabela", "indiceCpfUnico", "tipoLockSolicitado",
                        "estadoLockSolicitado", "intencaoConexaoBloqueadora", "intencaoConexaoSolicitante",
                        "estadoLockBloqueador"),
                "arestas", esperas)));
    }

    private void diagnosticarEspera(long bloqueador, CompletableFuture<Long> segunda,
                                   String etapaA, String etapaB, Thread a, Thread b) throws Exception {
        // Não coletar SQL, lock_data, argumentos, headers, cookies ou credenciais.
        // As arestas completas ajudam a distinguir um lock anterior do filtro
        // restrito às identidades sem transformar esse outro lock em aprovação.
        List<Object[]> arestas = tx(() -> em.createNativeQuery("""
                select distinct retentor.PROCESSLIST_ID as conexao_dono_bloqueador,
                       requisitante.PROCESSLIST_ID as conexao_dono_solicitante,
                       cast(w.BLOCKING_ENGINE_TRANSACTION_ID as char) as transacao_bloqueadora,
                       cast(w.REQUESTING_ENGINE_TRANSACTION_ID as char) as transacao_solicitante,
                       w.BLOCKING_THREAD_ID as thread_criador_estrutura_bloqueadora,
                       w.REQUESTING_THREAD_ID as thread_criador_estrutura_solicitante,
                       l.OBJECT_NAME as tabela_solicitada, l.INDEX_NAME as indice_solicitado,
                       l.LOCK_TYPE as tipo_lock_solicitado, l.LOCK_STATUS as estado_lock_solicitado,
                       detido.LOCK_STATUS as estado_lock_bloqueador,
                       case when l.OBJECT_SCHEMA=database() then 1 else 0 end as mesmo_banco
                from performance_schema.data_lock_waits w
                left join performance_schema.data_locks l on l.ENGINE_LOCK_ID=w.REQUESTING_ENGINE_LOCK_ID
                    and l.ENGINE=w.ENGINE
                left join performance_schema.data_locks detido on detido.ENGINE_LOCK_ID=w.BLOCKING_ENGINE_LOCK_ID
                    and detido.ENGINE=w.ENGINE
                left join performance_schema.data_locks ancora_retida
                    on ancora_retida.ENGINE_TRANSACTION_ID=w.BLOCKING_ENGINE_TRANSACTION_ID
                    and ancora_retida.ENGINE=w.ENGINE and ancora_retida.LOCK_TYPE='TABLE'
                    and ancora_retida.LOCK_STATUS='GRANTED' and ancora_retida.LOCK_MODE in ('IS','IX')
                    and ancora_retida.OBJECT_SCHEMA=l.OBJECT_SCHEMA and ancora_retida.OBJECT_NAME=l.OBJECT_NAME
                left join performance_schema.threads retentor on retentor.THREAD_ID=ancora_retida.THREAD_ID
                left join performance_schema.data_locks ancora_solicitada
                    on ancora_solicitada.ENGINE_TRANSACTION_ID=w.REQUESTING_ENGINE_TRANSACTION_ID
                    and ancora_solicitada.ENGINE=w.ENGINE and ancora_solicitada.LOCK_TYPE='TABLE'
                    and ancora_solicitada.LOCK_STATUS='GRANTED' and ancora_solicitada.LOCK_MODE in ('IS','IX')
                    and ancora_solicitada.OBJECT_SCHEMA=l.OBJECT_SCHEMA and ancora_solicitada.OBJECT_NAME=l.OBJECT_NAME
                left join performance_schema.threads requisitante on requisitante.THREAD_ID=ancora_solicitada.THREAD_ID
                order by transacao_bloqueadora, transacao_solicitante
                """, Object[].class).getResultList());
        Object conexaoB = segunda.isDone() && !segunda.isCompletedExceptionally() ? segunda.getNow(null) : null;
        Map<String, Object> diagnostico = new LinkedHashMap<>();
        diagnostico.put("conexaoA", bloqueador); diagnostico.put("conexaoB", conexaoB);
        diagnostico.put("etapaA", etapaA); diagnostico.put("etapaB", etapaB);
        diagnostico.put("colunasArestas", List.of("conexaoDonoBloqueador", "conexaoDonoSolicitante",
                "transacaoBloqueadora", "transacaoSolicitante", "threadCriadorEstruturaBloqueadora",
                "threadCriadorEstruturaSolicitante", "tabela", "indice", "tipoLockSolicitado",
                "estadoLockSolicitado", "estadoLockBloqueador", "mesmoBancoDoObservador"));
        diagnostico.put("arestasSemDadosDeNegocio", arestas);
        diagnostico.put("workerA", estadoThread(a)); diagnostico.put("workerB", estadoThread(b));
        System.out.println("CPF_CONCORRENCIA_DIAGNOSTICO " + json(diagnostico));
    }

    private Map<String, Object> estadoThread(Thread thread) {
        if (thread == null) return mapa("iniciado", false);
        List<String> pilha = Arrays.stream(thread.getStackTrace()).map(StackTraceElement::toString).toList();
        return mapa("idJava", thread.getId(), "estado", thread.getState().name(), "pilhaSemArgumentos", pilha);
    }

    private Map<String, Object> resultadoDiagnostico(Future<MvcResult> resultado) {
        if (resultado == null) return mapa("iniciado", false);
        if (!resultado.isDone()) return mapa("concluido", false);
        try { return mapa("concluido", true, "statusHttp", resultado.get().getResponse().getStatus()); }
        catch (Exception e) {
            return mapa("concluido", true, "classeFalha", e.getClass().getSimpleName(),
                    "classeCausa", e.getCause() == null ? null : e.getCause().getClass().getSimpleName());
        }
    }

    private long cadastrar(Cenario c, NovoContrato n, int indice) throws Exception {
        return enviar(c.localA, "/api/vinculos-contratuais", mapa("contratoId", n.id(),
                "composicaoId", n.composicao1(), "dataInicio", RelogioSistema.hoje(),
                "pessoa", mapa("nome", "Pessoa CPF controle " + n.id() + "-" + indice,
                        "cpf", cpf(Math.toIntExact(631000000L + n.id() * 10L + indice)))), 201,
                "Pessoa e identidade canônica nascem por HTTP, com vínculo e fonte histórica próprios")
                .path("pessoaId").asLong();
    }

    private Map<String, Object> correcao(String tipo, String novoValor, String documento) {
        return mapa("tipo", tipo, "novoValor", novoValor,
                "motivo", "Erro material comprovado " + documento,
                "evidencia", "SEI-CPF-" + documento, "autorizacao", "ATO-CPF-" + documento,
                "impactosEncontrados", "Identidade, projeção canônica e trilha existente " + documento);
    }

    private String rota(long pessoaId) { return "/api/pessoas/" + pessoaId + "/correcoes-identidade"; }

    private long contarCpf(String entidade, String cpf) {
        return tx(() -> em.createQuery("select count(e) from " + entidade + " e where e.cpf=:cpf", Long.class)
                .setParameter("cpf", cpf).getSingleResult());
    }

    private Estado estado(long pessoaId) {
        return tx(() -> {
            Pessoa p = em.find(Pessoa.class, pessoaId);
            Empregado e = em.createQuery("select e from Empregado e where e.pessoaCanonica.id=:p", Empregado.class)
                    .setParameter("p", pessoaId).getSingleResult();
            List<String> vinculos = linhas("select v.id, v.contrato.id, v.dataInicio, v.dataFim, v.estadoNegocial, v.matricula "
                    + "from VinculoContratual v where v.pessoa.id=:p order by v.id", pessoaId);
            List<String> alocacoes = linhas("select a.id, a.vinculo.id, a.dataInicio, a.dataFim, a.estadoNegocial, "
                    + "a.lotacao.id, a.jornada.id, a.cargo.id from AlocacaoVinculo a where a.vinculo.pessoa.id=:p order by a.id", pessoaId);
            List<String> eventos = new ArrayList<>(linhas("select e.id, e.vinculo.id, e.tipoEvento, e.dataEfetiva, e.registradoEm, "
                    + "e.pessoaNomeFotografia, e.pessoaCpfFotografia, e.hashAnterior, e.hashEvento "
                    + "from EventoVinculoContratual e where e.vinculo.pessoa.id=:p order by e.id", pessoaId));
            eventos.addAll(linhas("select e.id, e.vinculo.id, e.tipoEvento, e.dataEfetiva, e.registradoEm, e.hashAnterior, e.hashEvento "
                    + "from EventoAlocacaoVinculo e where e.vinculo.pessoa.id=:p order by e.id", pessoaId));
            List<String> correcoes = linhas("select c.id, c.tipoCorrecao, c.valorAnterior, c.valorNovo, c.motivo, c.evidencia, "
                    + "c.autorizacao, c.impactosEncontrados, c.responsavelId, c.responsavelUsername, c.registradoEm "
                    + "from CorrecaoPessoa c where c.pessoa.id=:p order by c.id", pessoaId);
            return new Estado(p.getId(), p.getNome(), p.getCpf(), p.getVersion(), p.getAtualizadoEm(),
                    e.getId(), e.getPessoaCanonica().getId(), e.getCpf(), e.getNome(), e.getVersion(),
                    vinculos, alocacoes, eventos, correcoes);
        });
    }

    private List<String> linhas(String jpql, long pessoaId) {
        return em.createQuery(jpql, Object[].class).setParameter("p", pessoaId)
                .getResultList().stream().map(Arrays::deepToString).toList();
    }

    private <T> T tx(Callable<T> acao) {
        return emTransacao(() -> {
            try { return acao.call(); }
            catch (Exception e) { throw new IllegalStateException(e); }
        });
    }

    private record Estado(long pessoaId, String nome, String cpf, long versaoPessoa, Instant atualizadaEm,
            long empregadoId, long pessoaCanonicaId, String cpfEmpregado, String nomeEmpregado, long versaoEmpregado,
            List<String> vinculos, List<String> alocacoes, List<String> eventos, List<String> correcoes) {}
}
