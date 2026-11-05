package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.dominio.pendencia.EstadoPendencia;
import com.example.siggtrat.dominio.pendencia.EscopoBloqueioPendencia;
import com.example.siggtrat.dominio.pendencia.TipoPendencia;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.servico.FilaRegistroAuditoria;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Proposta independente AUD-001/AUD-006; CON-012 impede resolução/reativação automática.
 * Contrato, publicação, ADI, Pessoa e vínculo nascem pelas rotas de domínio.
 * SOMENTE a pendência antiga incorreta é inserida por JPA como fixture explícita
 * representativa do defeito PES-004 já reproduzido. Não é novo fato negocial.
 * O teste não apaga nem corrige a fixture: solicita tratamento humano por HTTP.
 * Todas as verificações de persistência abrem transações independentes.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AuditoriaFinalResolucaoPendenciaFimSuperadoHttpIT extends ContratosHttpBase {
    @Autowired FilaRegistroAuditoria fila;

    @Test
    void tarefaLegadaSemCausaAtualPodeSerResolvidaSemEncerrarVinculoVigente() throws Exception {
        verificarResolucao(false);
    }

    @Test
    void resolverTarefaAntigaPreservaTarefaDoNovoFimAindaNaoTratado() throws Exception {
        verificarResolucao(true);
    }

    private void verificarResolucao(boolean depoisDoNovoFim) throws Exception {
        var f = prepararFixtureExplicitaDePendenciaAntiga();
        var origemInicial = origemPendencia(f.pendenciaAntigaId());
        var fatosAntes = fatosNegociais(f);
        try (var clock = Mockito.mockStatic(RelogioSistema.class, Mockito.CALLS_REAL_METHODS)) {
            LocalDate data = (depoisDoNovoFim ? f.novoFim() : f.n().fim()).plusDays(1);
            clock.when(RelogioSistema::hoje).thenReturn(data);
            var posicao = consultar(f.c().localA, base(f.n().id()) + "/posicao-temporal?data=" + data,
                    "Pré-condição: posição temporal reconstruída pelo contrato prorrogado");
            assertThat(posicao.path("vigenciaFim").asText()).isEqualTo(f.novoFim().toString());
            assertThat(posicao.path("situacaoTemporal").asText())
                    .isEqualTo(depoisDoNovoFim ? "ENCERRADO" : "VIGENTE");
            assertThat(tx(() -> em.find(Contrato.class, f.n().id()).getDataFim()))
                    .as("Master permanece antigo; origem da fixture é explícita").isEqualTo(f.n().fim());

            var consulta = consultar(f.c().localA, rotaPendencias(f),
                    "Listagem revalida geração, sem resolver silenciosamente tarefa legada");
            var pendenciasAntes = pendenciasPersistidas(f.vinculoId());
            assertThat(pendenciasAntes).hasSize(depoisDoNovoFim ? 2 : 1);
            assertThat(consulta.path("registros")).hasSize(depoisDoNovoFim ? 2 : 1);
            assertThat(tx(() -> em.find(PendenciaAdministrativa.class, f.pendenciaAntigaId()).getEstado()))
                    .as("GET não substitui a confirmação humana").isEqualTo(EstadoPendencia.ABERTA);

            Long pendenciaAtualId = pendenciasAntes.stream()
                    .filter(p -> f.novoFim().equals(p.dataEfetiva())).map(PendenciaLida::id).findFirst().orElse(null);
            if (depoisDoNovoFim) {
                assertThat(pendenciaAtualId).as("Pré-condição: tarefa canônica legítima do novo fim").isNotNull();
                assertThat(pendenciasAntes.stream().filter(p -> p.id().equals(pendenciaAtualId)).findFirst().orElseThrow().chave())
                        .isEqualTo("FIM_VIGENCIA_VINCULO:" + f.vinculoId() + ":" + f.novoFim());
                enviar(f.c().localA, "/api/pendencias/" + pendenciaAtualId + "/confirmar-resolucao",
                        mapa("observacao", "Controle negativo: vínculo ainda aberto no término real"), 409,
                        "A causa da tarefa do novo fim permanece e não pode ser dispensada pela correção da antiga");
            }

            enviar(f.c().localA, "/api/pendencias/" + f.pendenciaAntigaId() + "/iniciar-tratamento",
                    mapa("observacao", "Conferência humana da tarefa antiga diante da prorrogação comprovada"), 200,
                    "Gestor responsável inicia o tratamento da projeção legada");
            var auditoriaAntes = auditoriaAposDescarga(f.pendenciaAntigaId(), "PENDENCIA_INICIAR_TRATAMENTO");
            assertThat(auditoriaAntes).anySatisfy(a -> {
                assertThat(a.acao()).isEqualTo("PENDENCIA_INICIAR_TRATAMENTO");
                assertThat(a.usuarioId()).isEqualTo(f.c().localA.getId());
            });
            var inicioAntes = tx(() -> {
                var p = em.find(PendenciaAdministrativa.class, f.pendenciaAntigaId());
                return new InicioTratamento(p.getIniciadaEm(), p.getIniciadaPorId(), p.getIniciadaPor());
            });
            String motivoResolucao = "Conferência documental: o término antigo foi superado; "
                    + "a tarefa legada não exige desligamento artificial do vínculo.";
            // Resposta bruta para que todos os controles de fonte sejam executados
            // mesmo quando o status incorreto for 409 na reprodução vermelha.
            var resposta = mvc.perform(autenticar(post("/api/pendencias/" + f.pendenciaAntigaId() + "/confirmar-resolucao")
                    .contentType(APPLICATION_JSON).content(json(mapa("observacao", motivoResolucao))),
                    f.c().localA)).andReturn();
            var estadoDepois = tx(() -> {
                var p = em.find(PendenciaAdministrativa.class, f.pendenciaAntigaId());
                return new ResolucaoLida(p.getEstado(), p.getResolvidaEm(), p.getResolvidaPorId(),
                        p.getResolvidaPor(), p.getObservacaoTratamento(),
                        new InicioTratamento(p.getIniciadaEm(), p.getIniciadaPorId(), p.getIniciadaPor()));
            });
            var auditoriaDepois = auditoriaAposDescarga(f.pendenciaAntigaId(),
                    resposta.getResponse().getStatus() == 200 ? "PENDENCIA_RESOLVIDA" : "PENDENCIA_INICIAR_TRATAMENTO");
            var resolucoes = auditoriaDepois.stream().filter(a -> a.acao().equals("PENDENCIA_RESOLVIDA")).toList();
            var pendenciasDepois = pendenciasPersistidas(f.vinculoId());
            assertAll(
                    () -> assertThat(resposta.getResponse().getStatus())
                            .as("Resolução explícita da causa superada: " + resposta.getResponse().getContentAsString())
                            .isEqualTo(200),
                    () -> assertThat(estadoDepois.estado()).isEqualTo(EstadoPendencia.RESOLVIDA),
                    () -> assertThat(estadoDepois.resolvidaEm()).isNotNull(),
                    () -> assertThat(estadoDepois.resolvidaPorId()).isEqualTo(f.c().localA.getId()),
                    () -> assertThat(estadoDepois.resolvidaPor()).isEqualTo(f.c().localA.getUsername()),
                    () -> assertThat(estadoDepois.observacao()).isEqualTo(motivoResolucao),
                    () -> assertThat(estadoDepois.inicio()).as("Autoria/instante do início são preservados").isEqualTo(inicioAntes),
                    () -> assertThat(origemPendencia(f.pendenciaAntigaId()))
                            .as("ID, chave, motivo, surgimento, data e fotografias legadas não são reescritos").isEqualTo(origemInicial),
                    () -> assertThat(fatosNegociais(f))
                            .as("Nenhum fim de vínculo, evento ou fotografia negocial é alterado para resolver tarefa sem causa")
                            .isEqualTo(fatosAntes),
                    () -> assertThat(auditoriaDepois).as("Trilha anterior não é apagada nem reescrita").containsAll(auditoriaAntes),
                    () -> assertThat(resolucoes).as("Uma nova ação de resolução auditada").hasSize(1),
                    () -> assertThat(resolucoes).allSatisfy(a -> {
                        assertThat(a.usuarioId()).isEqualTo(f.c().localA.getId());
                        assertThat(a.instante()).isNotNull();
                        assertThat(a.detalhes()).contains("RESOLVIDA");
                    }),
                    () -> assertThat(pendenciasDepois).as("Nenhuma tarefa antiga é excluída").hasSize(pendenciasAntes.size()),
                    () -> {
                        if (depoisDoNovoFim) {
                            var atualAntes = pendenciasAntes.stream().filter(p -> p.id().equals(pendenciaAtualId)).findFirst().orElseThrow();
                            var atualDepois = pendenciasDepois.stream().filter(p -> p.id().equals(pendenciaAtualId)).findFirst().orElseThrow();
                            assertThat(atualDepois).as("A tarefa do fim real permanece aberta, com identidade e trilha próprias")
                                    .isEqualTo(atualAntes);
                            assertThat(atualDepois.estado()).isEqualTo(EstadoPendencia.ABERTA);
                        }
                    });
        }
    }

    private Preparado prepararFixtureExplicitaDePendenciaAntiga() throws Exception {
        var c = criarCenarioPersistido();
        var hoje = RelogioSistema.hoje();
        var n = tx(() -> publicado(c, 5, null, false));
        long vinculoId = enviar(c.localA, "/api/vinculos-contratuais", mapa(
                "contratoId", n.id(), "composicaoId", n.composicao1(), "dataInicio", hoje,
                "pessoa", mapa("nome", "Pessoa da resolução administrativa " + n.id(),
                        "cpf", cpf(Math.toIntExact(755000000L + n.id())))), 201,
                "Vínculo canônico aberto; nenhuma pendência deve exigir desligamento fictício").path("id").asLong();
        var novoFim = n.fim().plusDays(30);
        var adi = aditamento("ADI-RESOLUCAO-FIM-" + n.id(), hoje.plusDays(5));
        adi.put("novaDataFim", novoFim);
        tx(() -> registrarAditamento(c, n.id(), adi));
        assertThat(pendenciasPersistidas(vinculoId)).isEmpty();
        long antiga = tx(() -> {
            // Fixture legada proposital. Não chamar a geração já corrigida para
            // simular um bug antigo; a reprodução PES-004 comprova sua origem.
            var v = em.find(VinculoContratual.class, vinculoId);
            var p = new PendenciaAdministrativa();
            p.setChaveOrigem("FIM_VIGENCIA_VINCULO:" + vinculoId + ":" + n.fim());
            p.setTipo(TipoPendencia.FIM_NATURAL_VIGENCIA);
            p.setEstado(EstadoPendencia.ABERTA);
            p.setMotivo("Vínculo permaneceu aberto após o fim natural da vigência contratual.");
            p.setDataEfetiva(n.fim());
            p.setOperacaoOrigem("FIM_NATURAL_CONTRATO");
            p.setPerfilResponsavel(PerfilAcesso.GESTOR_LOCAL);
            p.setEscopoBloqueio(EscopoBloqueioPendencia.OPERACAO_VINCULO);
            p.setContratoId(n.id());
            p.setContratoFotografia(v.getContrato().getNumeroContrato() + "/" + v.getContrato().getAnoContrato());
            p.setPessoaId(v.getPessoa().getId());
            p.setPessoaFotografia(v.getPessoa().getNome());
            p.setVinculoId(vinculoId);
            p.setGestaoLocalId(c.gestaoA.getId());
            p.setSuperintendenciaId(c.seA.getId());
            p.setOrigemEntidade("VinculoContratual");
            p.setOrigemId(vinculoId);
            em.persist(p);
            em.flush();
            return p.getId();
        });
        return new Preparado(c, n, vinculoId, novoFim, antiga);
    }

    private String rotaPendencias(Preparado f) {
        return "/api/pendencias?contratoIds=" + f.n().id() + "&tipo=FIM_NATURAL_VIGENCIA";
    }

    private OrigemPendencia origemPendencia(long id) {
        return tx(() -> {
            var p = em.find(PendenciaAdministrativa.class, id);
            return new OrigemPendencia(p.getId(), p.getChaveOrigem(), p.getMotivo(), p.getSurgidaEm(),
                    p.getDataEfetiva(), p.getOperacaoOrigem(), p.getOrigemEntidade(), p.getOrigemId(),
                    p.getContratoId(), p.getContratoFotografia(), p.getPessoaId(), p.getPessoaFotografia(),
                    p.getVinculoId(), p.getGestaoLocalId(), p.getSuperintendenciaId());
        });
    }

    private List<PendenciaLida> pendenciasPersistidas(long vinculoId) {
        return tx(() -> em.createQuery("select p from PendenciaAdministrativa p where p.vinculoId=:id "
                        + "and p.operacaoOrigem='FIM_NATURAL_CONTRATO' order by p.id", PendenciaAdministrativa.class)
                .setParameter("id", vinculoId).getResultList().stream()
                .map(p -> new PendenciaLida(p.getId(), p.getDataEfetiva(), p.getChaveOrigem(), p.getEstado(),
                        p.getSurgidaEm(), p.getGestaoLocalId(), p.getSuperintendenciaId(),
                        p.getIniciadaEm(), p.getResolvidaEm())).toList());
    }

    private List<AuditoriaLida> auditoriaPersistida(long pendenciaId) {
        return tx(() -> em.createQuery("select a from RegistroAuditoria a where a.entityType='PendenciaAdministrativa' "
                        + "and a.entityId=:id order by a.id", RegistroAuditoria.class).setParameter("id", pendenciaId)
                .getResultList().stream().map(a -> new AuditoriaLida(a.getId(), a.getAction(), a.getCreatedAt(),
                        a.getUserId(), a.getUsername(), a.getDetailsJson(), a.getPayloadJson())).toList());
    }

    private List<AuditoriaLida> auditoriaAposDescarga(long pendenciaId, String acaoEsperada) throws Exception {
        // O comando HTTP já confirmou sua transação. A publicação ocorre após
        // commit, mas a gravação é em lote: usar o bean real de descarga e reler
        // em nova transação até observar a ação. Não simular a fila nem criar logs.
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        List<AuditoriaLida> registros;
        do {
            fila.descarregar();
            registros = auditoriaPersistida(pendenciaId);
            if (registros.stream().anyMatch(a -> a.acao().equals(acaoEsperada))) return registros;
            TimeUnit.MILLISECONDS.sleep(20);
        } while (System.nanoTime() < limite);
        assertThat(registros).as("Persistência real da ação após descarga da fila: " + acaoEsperada)
                .anySatisfy(a -> assertThat(a.acao()).isEqualTo(acaoEsperada));
        return registros;
    }

    private FatosNegociais fatosNegociais(Preparado f) {
        return tx(() -> {
            var v = em.find(VinculoContratual.class, f.vinculoId());
            var eventosVinculo = em.createQuery("select e from EventoVinculoContratual e where e.vinculo.id=:id order by e.id",
                            EventoVinculoContratual.class).setParameter("id", f.vinculoId()).getResultList().stream()
                    .map(e -> new EventoVinculoLido(e.getId(), e.getTipoEvento().name(), e.getDataEfetiva(),
                            e.getRegistradoEm(), e.getResponsavelId(), e.getPessoaNomeFotografia(),
                            e.getPessoaCpfFotografia(), e.getHashAnterior(), e.getHashEvento(), e.getFimNovo())).toList();
            var eventosContrato = em.createQuery("select e from EventoCicloVidaContrato e where e.contrato.id=:id order by e.id",
                            EventoCicloVidaContrato.class).setParameter("id", f.n().id()).getResultList().stream()
                    .map(e -> new EventoContratoLido(e.getId(), e.getTipoEvento().name(), e.getDataEfetiva(),
                            e.getRegistradoEm(), e.getValorAnterior(), e.getValorNovo(), e.getMotivo())).toList();
            return new FatosNegociais(v.getId(), v.getPessoa().getId(), v.getDataInicio(), v.getDataFim(),
                    v.getEstadoNegocial().name(), v.getContrato().getDataFim(), eventosVinculo, eventosContrato);
        });
    }

    private <T> T tx(Callable<T> tarefa) {
        return emTransacao(() -> {
            try { return tarefa.call(); }
            catch (Exception erro) { throw new IllegalStateException(erro); }
        });
    }

    private record Preparado(Cenario c, NovoContrato n, long vinculoId, LocalDate novoFim, long pendenciaAntigaId) {}
    private record OrigemPendencia(Long id, String chave, String motivo, Instant surgidaEm, LocalDate data,
                                  String operacao, String entidade, Long origemId, Long contratoId,
                                  String contrato, Long pessoaId, String pessoa, Long vinculoId, Long gestaoId, Long seId) {}
    private record PendenciaLida(Long id, LocalDate dataEfetiva, String chave, EstadoPendencia estado,
                                Instant surgidaEm, Long gestaoId, Long seId, Instant iniciadaEm, Instant resolvidaEm) {}
    private record InicioTratamento(Instant instante, Long usuarioId, String usuario) {}
    private record ResolucaoLida(EstadoPendencia estado, Instant resolvidaEm, Long resolvidaPorId,
                                String resolvidaPor, String observacao, InicioTratamento inicio) {}
    private record AuditoriaLida(Long id, String acao, Instant instante, Long usuarioId, String usuario,
                                String detalhes, String payload) {}
    private record EventoVinculoLido(Long id, String tipo, LocalDate data, Instant registro, Long autor,
                                    String nome, String cpf, String hashAnterior, String hash, LocalDate fim) {}
    private record EventoContratoLido(Long id, String tipo, LocalDate data, Instant registro,
                                     String anterior, String novo, String motivo) {}
    private record FatosNegociais(Long vinculoId, Long pessoaId, LocalDate inicio, LocalDate fim, String estado,
                                 LocalDate masterFim, List<EventoVinculoLido> vinculoEventos,
                                 List<EventoContratoLido> contratoEventos) {}
}
