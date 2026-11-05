package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.dominio.pendencia.EstadoPendencia;
import com.example.siggtrat.dominio.pendencia.EscopoBloqueioPendencia;
import com.example.siggtrat.dominio.pendencia.TipoPendencia;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.servico.FilaRegistroAuditoria;
import com.example.siggtrat.servico.GestaoContratoTemporalServico;
import com.example.siggtrat.servico.MotorTemporalContratoServico;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.test.web.servlet.MvcResult;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Controles independentes para o reparo de resolução de pendência.
 * Nenhum cancelamento/retificação é simulado por DML.
 * O primeiro caso cria um legado incompleto declarado desde o início.
 * O segundo insere apenas a tarefa antiga incorreta e usa HTTP em todos os
 * fatos posteriores, inclusive desligamento, retificação de ADI e restauração.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AuditoriaFinalPendenciaFimControlesHttpIT extends ContratosHttpBase {
    @Autowired FilaRegistroAuditoria fila;
    @Autowired MotorTemporalContratoServico motor;
    @Autowired GestaoContratoTemporalServico gestaoTemporal;

    @Test
    void ausenciaDeFimCanonicoNaoEquivaleACausaResolvidaMesmoComGestaoComprovada() throws Exception {
        var c = criarCenarioPersistido();
        var f = tx(() -> {
            // Inserção inicial de legado: não remover nem alterar uma prova
            // anteriormente publicada. A versão2 prova somente a Gestão.
            var contrato = new Contrato();
            contrato.setGestaoLocal(em.find(GestaoLocal.class, c.gestaoA.getId()));
            contrato.setTipoContratacao(em.find(TipoContratacao.class, c.tipoA.getId()));
            contrato.setTipo(c.tipoA.getDescricao());
            contrato.setNumeroContrato("LEG-SEM-FIM-" + c.gestaoA.getId());
            contrato.setAnoContrato(RelogioSistema.hoje().getYear());
            contrato.setDescricao("Legado declarado com responsabilidade, sem prova do término");
            contrato.setDataInicio(INICIO);
            contrato.setDataFim(RelogioSistema.hoje().minusDays(1));
            contrato.setEstadoNegocial(EstadoNegocialContrato.PUBLICADO);
            contrato.setPublicadoEm(Instant.parse("2026-01-01T03:00:00Z"));
            em.persist(contrato);
            em.flush();
            var fonteParcial = new VersaoContratoNegocial();
            fonteParcial.setContrato(contrato);
            fonteParcial.setNumeroVersao(2);
            fonteParcial.setDataEfetiva(INICIO);
            fonteParcial.setNumeroContrato(contrato.getNumeroContrato());
            fonteParcial.setAnoContrato(contrato.getAnoContrato());
            fonteParcial.setGestaoLocalId(c.gestaoA.getId());
            fonteParcial.setGestaoLocalNome(c.gestaoA.getNome());
            fonteParcial.setDataInicio(INICIO);
            // dataFim não é inventada a partir da coluna materializada.
            em.persist(fonteParcial);
            var pessoa = new Pessoa();
            pessoa.setNome("Pessoa do legado sem término " + contrato.getId());
            pessoa.setCpf(cpf(Math.toIntExact(756000000L + contrato.getId())));
            em.persist(pessoa);
            var vinculo = new VinculoContratual();
            vinculo.setPessoa(pessoa); vinculo.setContrato(contrato); vinculo.setDataInicio(INICIO);
            em.persist(vinculo); em.flush();
            long tarefa = inserirTarefaLegada(vinculo, contrato.getDataFim(), c);
            return new Legado(contrato.getId(), vinculo.getId(), tarefa);
        });
        tx(() -> {
            var contrato = em.find(Contrato.class, f.contratoId());
            var posicao = motor.reconstruirPosicao(contrato, RelogioSistema.hoje(), Instant.now());
            assertThat(posicao.estadoNegocial()).isEqualTo(EstadoNegocialContrato.PUBLICADO);
            assertThat(posicao.vigenciaFim()).as("Pré-condição: fim não comprovado pela fonte").isNull();
            assertThat(motor.fotografiaOriginalConhecida(contrato, Instant.now())).isEmpty();
            assertThat(gestaoTemporal.gestaoEm(contrato, contrato.getDataFim(), Instant.now()).getId())
                    .as("Pré-condição: autorização não esconde a validação de causa").isEqualTo(c.gestaoA.getId());
            return null;
        });
        enviar(c.localA, "/api/pendencias/" + f.pendenciaId() + "/iniciar-tratamento",
                mapa("observacao", "Conferir prova de vigência ausente no legado"), 200,
                "Responsável comprovado pode analisar a pendência");
        aguardarAuditoria(f.pendenciaId(), "PENDENCIA_INICIAR_TRATAMENTO");
        var antes = tarefa(f.pendenciaId());
        var resposta = mvc.perform(autenticar(post("/api/pendencias/" + f.pendenciaId() + "/confirmar-resolucao")
                .contentType(APPLICATION_JSON).content(json(mapa("observacao", "Não há documento que comprove novo fim"))),
                c.localA)).andReturn();
        fila.descarregar();
        assertAll(
                () -> assertThat(resposta.getResponse().getStatus())
                        .as("Ausência de prova não autoriza afirmar causa eliminada").isEqualTo(409),
                () -> assertThat(tarefa(f.pendenciaId())).as("Recusa preserva a tarefa em tratamento").isEqualTo(antes),
                () -> assertThat(auditorias(f.pendenciaId()).stream().filter(a -> a.acao().equals("PENDENCIA_RESOLVIDA")))
                        .isEmpty(),
                () -> assertThat(tx(() -> em.find(VinculoContratual.class, f.vinculoId()).getDataFim())).isNull(),
                () -> assertThat(tx(() -> em.createQuery("select count(e) from EventoCicloVidaContrato e where e.contrato.id=:id", Long.class)
                        .setParameter("id", f.contratoId()).getSingleResult())).isZero(),
                () -> assertThat(tx(() -> em.createQuery("select count(v) from VersaoContratoNegocial v where v.contrato.id=:id", Long.class)
                        .setParameter("id", f.contratoId()).getSingleResult())).isEqualTo(1));
    }

    @Test
    @Timeout(value = 90, unit = TimeUnit.SECONDS)
    void causaNaturalQueVoltaPorAtosValidosGeraNovaOcorrenciaSemReabrirResolucaoAnterior() throws Exception {
        var c = criarCenarioPersistido();
        var hojeInicial = RelogioSistema.hoje();
        var n = tx(() -> publicado(c, 5, null, false));
        long vinculoId = enviar(c.localA, "/api/vinculos-contratuais", mapa(
                "contratoId", n.id(), "composicaoId", n.composicao1(), "dataInicio", hojeInicial,
                "pessoa", mapa("nome", "Pessoa da recorrência natural " + n.id(),
                        "cpf", cpf(Math.toIntExact(757000000L + n.id())))), 201,
                "Origem do vínculo para sequência negocial explícita").path("id").asLong();
        long motivoIndividual = enviar(c.localA, "/api/motivos-desligamento", mapa(
                "descricao", "Motivo individual da recorrência " + n.id(),
                "categoria", "DESLIGAMENTO_INDIVIDUAL", "ativo", true), 201,
                "Catálogo individual nasce por HTTP, antes do dia em que será usado").path("id").asLong();
        var dataEfeitoAdi = hojeInicial.plusDays(5);
        var novoFim = n.fim().plusDays(30);
        var extensao = aditamento("ADI-REC-FIM-" + n.id(), dataEfeitoAdi);
        extensao.put("novaDataFim", novoFim);
        JsonNode adi = tx(() -> registrarAditamento(c, n.id(), extensao));
        long antiga = tx(() -> inserirTarefaLegada(em.find(VinculoContratual.class, vinculoId), n.fim(), c));

        try (var clock = Mockito.mockStatic(RelogioSistema.class, Mockito.CALLS_REAL_METHODS)) {
            // A tarefa inicial é fixture incorreta declarada, não reprodução da
            // geração PES-004. No próprio F0, desligamento programado ainda
            // pode ser cancelado legitimamente; o relógio só avança depois.
            clock.when(RelogioSistema::hoje).thenReturn(n.fim());
            var prorrogado = consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + n.fim(),
                    "Pré-condição: prorrogação vigente torna incorreta a tarefa antiga");
            assertThat(prorrogado.path("vigenciaFim").asText()).isEqualTo(novoFim.toString());
            enviar(c.localA, "/api/pendencias/" + antiga + "/iniciar-tratamento",
                    mapa("observacao", "Conferência da tarefa legada incorreta"), 200, "Tratamento humano da tarefa antiga");
            enviar(c.localA, "/api/pendencias/" + antiga + "/confirmar-resolucao",
                    mapa("observacao", "Fim antigo superado pela prorrogação comprovada"), 200,
                    "Pré-condição: resolução humana do erro anterior");
            var resolvidaAntes = tarefa(antiga);
            assertThat(resolvidaAntes.estado()).isEqualTo(EstadoPendencia.RESOLVIDA);
            var auditoriaAntes = aguardarAuditoria(antiga, "PENDENCIA_RESOLVIDA");

            // Não reduzir vigência de vínculo aberto. Os dependentes são
            // tratados por atos explícitos antes da retificação contratual.
            enviar(c.localA, "/api/vinculos-contratuais/" + vinculoId + "/desligar", mapa(
                    "dataFim", n.fim(), "motivoDesligamentoId", motivoIndividual,
                    "justificativa", "Decisão individual documentada com último dia F0",
                    "evidencia", "SEI-REC-DESLIGAMENTO", "autorizacao", "ATO-REC-DESLIGAMENTO"), 200,
                    "Dependente encerrado em F0 permite a retificação sem perda de cobertura");
            var retificacao = aditamento("ADI-REC-FIM-" + n.id(), dataEfeitoAdi);
            retificacao.put("dataDocumento", n.fim());
            retificacao.put("novaDataFim", n.fim());
            retificacao.put("eventoPrecedenteId", adi.path("eventoId").asLong());
            retificacao.put("relacaoPrecedencia", "RETIFICA");
            JsonNode retificado = tx(() -> {
                previaAditamento(c, n.id(), retificacao);
                return enviar(c.localA, base(n.id()) + "/aditamentos/" + adi.path("id").asLong() + "/retificar",
                        retificacao, 200, "Retificação válida retira a extensão, sem DML de fatos negociais");
            });
            assertThat(retificado.path("id").asLong()).isNotEqualTo(adi.path("id").asLong());
            enviar(c.localA, "/api/vinculos-contratuais/" + vinculoId + "/cancelar-desligamento-programado", mapa(
                    "justificativa", "Decisão individual cancelada ainda no último dia programado",
                    "evidencia", "SEI-REC-RESTAURACAO", "autorizacao", "ATO-REC-RESTAURACAO"), 200,
                    "Restauração pública ainda permitida em F0; contrato continua cobrindo o dia");
            assertThat(tx(() -> em.find(VinculoContratual.class, vinculoId).getDataFim())).isNull();
            var provasAntesDaSincronizacao = eventosVinculo(vinculoId);
            clock.when(RelogioSistema::hoje).thenReturn(n.fim().plusDays(1));
            var encerrado = consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + n.fim().plusDays(1),
                    "Pré-condição: término natural real voltou à data da tarefa antiga");
            assertThat(encerrado.path("vigenciaFim").asText()).isEqualTo(n.fim().toString());
            assertThat(encerrado.path("situacaoTemporal").asText()).isEqualTo("ENCERRADO");
            String rota = "/api/pendencias?contratoIds=" + n.id() + "&tipo=FIM_NATURAL_VIGENCIA&estado=ABERTA";
            // Primeira materialização da recorrência: dois HTTPs e transações
            // reais disputam o contrato. Cada worker possui seu próprio relógio.
            var listasConcorrentes = consultarRecorrenciaConcorrente(c, n.id(), rota, n.fim().plusDays(1));
            var lista = listasConcorrentes.get(0);
            assertThat(listasConcorrentes.get(1))
                    .as("Ambos os clientes observam a mesma e única ocorrência após seus commits")
                    .isEqualTo(lista);
            var tarefasAntesDoReenvio = tarefas(vinculoId);
            var listaRepetida = consultar(c.localA, rota, "Repetição não cria terceira ocorrência");
            var tarefasDepoisDoReenvio = tarefas(vinculoId);
            var ativas = tarefasAntesDoReenvio.stream().filter(p -> p.estado() != EstadoPendencia.RESOLVIDA).toList();
            assertAll(
                    () -> assertThat(lista.path("registros")).as("Uma tarefa ativa representa a recorrência").hasSize(1),
                    () -> assertThat(tarefasAntesDoReenvio).as("Preserva a antiga e acrescenta uma ocorrência").hasSize(2),
                    () -> assertThat(tarefa(antiga)).as("Resolução, autoria, instante, chave e versão anteriores intactos")
                            .isEqualTo(resolvidaAntes),
                    () -> assertThat(ativas).hasSize(1),
                    () -> assertThat(ativas).allSatisfy(p -> {
                        assertThat(p.id()).isNotEqualTo(antiga);
                        assertThat(p.data()).isEqualTo(n.fim());
                        assertThat(p.origemId()).isEqualTo(vinculoId);
                        assertThat(p.estado()).isEqualTo(EstadoPendencia.ABERTA);
                        assertThat(p.chave()).isNotEqualTo(resolvidaAntes.chave());
                        assertThat(p.resolvidaEm()).isNull();
                    }),
                    () -> assertThat(listaRepetida).isEqualTo(lista),
                    () -> assertThat(tarefasDepoisDoReenvio).isEqualTo(tarefasAntesDoReenvio),
                    () -> assertThat(eventosVinculo(vinculoId)).as("Sincronização não cria novo fato negocial")
                            .isEqualTo(provasAntesDaSincronizacao),
                    () -> assertThat(auditorias(antiga)).containsAll(auditoriaAntes));
        }
    }

    private List<JsonNode> consultarRecorrenciaConcorrente(Cenario c, long contratoId,
                                                          String rota, LocalDate hoje) throws Exception {
        // Preparar a sessão no thread chamador evita disputa no cache da fixture.
        var primeira = autenticar(get(rota), c.localA);
        var segunda = autenticar(get(rota), c.localA);
        var executor = Executors.newFixedThreadPool(2);
        var prontas = new CountDownLatch(2);
        var iniciar = new CountDownLatch(1);
        try {
            List<Future<MvcResult>> respostas = tx(() -> {
                em.find(Contrato.class, contratoId, LockModeType.PESSIMISTIC_WRITE);
                long conexao = ((Number) em.createNativeQuery("select connection_id()")
                        .getSingleResult()).longValue();
                var a = executor.submit(() -> {
                    try (var relogio = Mockito.mockStatic(RelogioSistema.class, Mockito.CALLS_REAL_METHODS)) {
                        relogio.when(RelogioSistema::hoje).thenReturn(hoje);
                        prontas.countDown();
                        assertThat(iniciar.await(10, TimeUnit.SECONDS)).as("Barreira do primeiro HTTP").isTrue();
                        return mvc.perform(primeira).andReturn();
                    }
                });
                var b = executor.submit(() -> {
                    try (var relogio = Mockito.mockStatic(RelogioSistema.class, Mockito.CALLS_REAL_METHODS)) {
                        relogio.when(RelogioSistema::hoje).thenReturn(hoje);
                        prontas.countDown();
                        assertThat(iniciar.await(10, TimeUnit.SECONDS)).as("Barreira do segundo HTTP").isTrue();
                        return mvc.perform(segunda).andReturn();
                    }
                });
                assertThat(prontas.await(10, TimeUnit.SECONDS)).as("Ambos os workers prontos").isTrue();
                iniciar.countDown();
                // A sincronização percorre todos os candidatos em ordem. Um
                // worker pode aguardar o outro num contrato anterior ao alvo.
                // A cadeia de espera enraizada neste lock comprova ambos os
                // pedidos bloqueados, sem exigir duas esperas diretas no alvo.
                long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
                long esperas = 0;
                do {
                    esperas = ((Number) em.createNativeQuery("""
                            with recursive esperas (thread_id) as (
                                select e.REQUESTING_THREAD_ID
                                from performance_schema.data_lock_waits e
                                join performance_schema.threads b on b.THREAD_ID=e.BLOCKING_THREAD_ID
                                where b.PROCESSLIST_ID=:conexao
                                union distinct
                                select e.REQUESTING_THREAD_ID
                                from performance_schema.data_lock_waits e
                                join esperas anterior on e.BLOCKING_THREAD_ID=anterior.thread_id
                            )
                            select count(distinct thread_id) from esperas
                            """).setParameter("conexao", conexao).getSingleResult()).longValue();
                    assertThat(a.isDone()).as("Primeiro HTTP não ultrapassa o lock antes da liberação").isFalse();
                    assertThat(b.isDone()).as("Segundo HTTP não ultrapassa o lock antes da liberação").isFalse();
                    if (esperas >= 2) break;
                    TimeUnit.MILLISECONDS.sleep(20);
                } while (System.nanoTime() < limite);
                assertThat(esperas).as("Duas conexões aguardam na cadeia do contrato bloqueado no MySQL")
                        .isGreaterThanOrEqualTo(2);
                return List.of(a, b);
            }); // O commit solta o lock; as duas rotas confirmam transações próprias.
            var primeiraResposta = respostas.get(0).get(20, TimeUnit.SECONDS);
            var segundaResposta = respostas.get(1).get(20, TimeUnit.SECONDS);
            assertAll(
                    () -> assertThat(primeiraResposta.getResponse().getStatus())
                            .as("Primeiro HTTP: %s", primeiraResposta.getResponse().getContentAsString()).isEqualTo(200),
                    () -> assertThat(segundaResposta.getResponse().getStatus())
                            .as("Segundo HTTP: %s", segundaResposta.getResponse().getContentAsString()).isEqualTo(200));
            return List.of(dados(primeiraResposta), dados(segundaResposta));
        } finally {
            iniciar.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).as("Workers encerrados").isTrue();
        }
    }

    private long inserirTarefaLegada(VinculoContratual v, LocalDate fim, Cenario c) {
        var p = new PendenciaAdministrativa();
        p.setChaveOrigem("FIM_VIGENCIA_VINCULO:" + v.getId() + ":" + fim);
        p.setTipo(TipoPendencia.FIM_NATURAL_VIGENCIA); p.setEstado(EstadoPendencia.ABERTA);
        p.setMotivo("Vínculo permaneceu aberto após o fim natural da vigência contratual.");
        p.setDataEfetiva(fim); p.setOperacaoOrigem("FIM_NATURAL_CONTRATO");
        p.setPerfilResponsavel(PerfilAcesso.GESTOR_LOCAL); p.setEscopoBloqueio(EscopoBloqueioPendencia.OPERACAO_VINCULO);
        p.setContratoId(v.getContrato().getId()); p.setContratoFotografia(v.getContrato().getNumeroContrato());
        p.setPessoaId(v.getPessoa().getId()); p.setPessoaFotografia(v.getPessoa().getNome()); p.setVinculoId(v.getId());
        p.setGestaoLocalId(c.gestaoA.getId()); p.setSuperintendenciaId(c.seA.getId());
        p.setOrigemEntidade("VinculoContratual"); p.setOrigemId(v.getId());
        em.persist(p); em.flush(); return p.getId();
    }

    private Tarefa tarefa(long id) {
        return tx(() -> fotografia(em.find(PendenciaAdministrativa.class, id)));
    }

    private List<Tarefa> tarefas(long vinculo) {
        return tx(() -> em.createQuery("select p from PendenciaAdministrativa p where p.vinculoId=:id "
                        + "and p.tipo=:tipo order by p.id", PendenciaAdministrativa.class)
                .setParameter("id", vinculo).setParameter("tipo", TipoPendencia.FIM_NATURAL_VIGENCIA)
                .getResultList().stream().map(this::fotografia).toList());
    }

    private Tarefa fotografia(PendenciaAdministrativa p) {
        return new Tarefa(p.getId(), p.getChaveOrigem(), p.getEstado(), p.getMotivo(), p.getSurgidaEm(),
                p.getDataEfetiva(), p.getOrigemId(), p.getGestaoLocalId(), p.getIniciadaEm(), p.getIniciadaPorId(),
                p.getResolvidaEm(), p.getResolvidaPorId(), p.getObservacaoTratamento(), p.getVersion());
    }

    private List<Auditoria> auditorias(long id) {
        return tx(() -> em.createQuery("select a from RegistroAuditoria a where a.entityType='PendenciaAdministrativa' "
                        + "and a.entityId=:id order by a.id", RegistroAuditoria.class).setParameter("id", id).getResultList()
                .stream().map(a -> new Auditoria(a.getId(), a.getAction(), a.getCreatedAt(), a.getUserId(), a.getDetailsJson())).toList());
    }

    private List<Auditoria> aguardarAuditoria(long id, String acao) throws Exception {
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        List<Auditoria> registros;
        do {
            fila.descarregar(); registros = auditorias(id);
            if (registros.stream().anyMatch(a -> a.acao().equals(acao))) return registros;
            TimeUnit.MILLISECONDS.sleep(20);
        } while (System.nanoTime() < limite);
        assertThat(registros).anySatisfy(a -> assertThat(a.acao()).isEqualTo(acao));
        return registros;
    }

    private List<Evento> eventosVinculo(long id) {
        return tx(() -> em.createQuery("select e from EventoVinculoContratual e where e.vinculo.id=:id order by e.id",
                        EventoVinculoContratual.class).setParameter("id", id).getResultList().stream()
                .map(e -> new Evento(e.getId(), e.getTipoEvento().name(), e.getDataEfetiva(), e.getRegistradoEm(),
                        e.getResponsavelId(), e.getHashAnterior(), e.getHashEvento())).toList());
    }

    private <T> T tx(Callable<T> tarefa) {
        return emTransacao(() -> {
            try { return tarefa.call(); }
            catch (Exception erro) { throw new IllegalStateException(erro); }
        });
    }

    private record Legado(long contratoId, long vinculoId, long pendenciaId) {}
    private record Tarefa(Long id, String chave, EstadoPendencia estado, String motivo, Instant surgidaEm,
                          LocalDate data, Long origemId, Long gestaoId, Instant iniciadaEm, Long iniciadaPor,
                          Instant resolvidaEm, Long resolvidaPor, String observacao, long versao) {}
    private record Auditoria(Long id, String acao, Instant instante, Long usuario, String detalhes) {}
    private record Evento(Long id, String tipo, LocalDate data, Instant registro, Long autor, String hashAnterior, String hash) {}
}
