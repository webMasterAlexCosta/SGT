package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.entidade.EventoVinculoContratual;
import com.example.siggtrat.entidade.PendenciaAdministrativa;
import com.example.siggtrat.entidade.VinculoContratual;
import com.example.siggtrat.utilitario.RelogioSistema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * Proposta independente VIN-008/009/HIS-005. Toda a linha negocial nasce por HTTP;
 * só o relógio é controlado. Os oráculos JPA abrem novas transações após os GETs.
 * A transferência fica entre o fim antigo e o fim prorrogado, para distinguir
 * uma tarefa legítima no destino de uma tarefa prematura atribuída à origem.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AuditoriaFinalPendenciaFimNaturalProrrogacaoHttpIT extends ContratosHttpBase {
    @Test
    void consultaNaoCriaPendenciaNoFimAntigoQuandoProrrogacaoJaVigora() throws Exception {
        var f = prepararProrrogacaoComTransferencia();
        var eventosAntes = eventosPersistidos(f.vinculoId());
        try (var clock = Mockito.mockStatic(RelogioSistema.class, Mockito.CALLS_REAL_METHODS)) {
            var data = f.n().fim().plusDays(1);
            clock.when(RelogioSistema::hoje).thenReturn(data);
            var posicao = consultar(f.c().localA, base(f.n().id()) + "/posicao-temporal?data=" + data,
                    "Pré-condição: prorrogação efetiva, transferência ainda futura");
            assertThat(posicao.path("situacaoTemporal").asText()).isEqualTo("VIGENTE");
            assertThat(posicao.path("vigenciaFim").asText()).isEqualTo(f.novoFim().toString());

            var origem = consultar(f.c().localA, rota(f), "Listagem dispara sincronização do fim natural");
            var destino = consultar(f.c().localB, rota(f), "Segunda listagem não deve criar tarefa prematura");
            var persistidas = pendenciasPersistidas(f.vinculoId());
            assertAll(
                    () -> assertThat(origem.path("registros")).as("Contrato ainda vigente não gera tarefa na origem")
                            .isEmpty(),
                    () -> assertThat(destino.path("registros")).as("Contrato ainda vigente não gera tarefa no destino")
                            .isEmpty(),
                    () -> assertThat(persistidas).as("Releitura em nova transação: nenhuma pendência natural prematura")
                            .isEmpty(),
                    () -> assertThat(tx(() -> em.find(VinculoContratual.class, f.vinculoId()).getDataFim()))
                            .as("GET não encerra o vínculo").isNull(),
                    () -> assertThat(eventosPersistidos(f.vinculoId())).as("GET preserva eventos e fotografias originais")
                            .isEqualTo(eventosAntes));
        }
    }

    @Test
    void consultaDepoisDoNovoFimGeraUmaPendenciaNaGestaoEfetivaDoNovoFim() throws Exception {
        var f = prepararProrrogacaoComTransferencia();
        var eventosAntes = eventosPersistidos(f.vinculoId());
        try (var clock = Mockito.mockStatic(RelogioSistema.class, Mockito.CALLS_REAL_METHODS)) {
            var data = f.novoFim().plusDays(1);
            clock.when(RelogioSistema::hoje).thenReturn(data);
            var posicao = consultar(f.c().localB, base(f.n().id()) + "/posicao-temporal?data=" + data,
                    "Pré-condição: a gestão destino consulta o contrato já prorrogado e transferido");
            assertThat(posicao.path("vigenciaFim").asText()).isEqualTo(f.novoFim().toString());

            var destino = consultar(f.c().localB, rota(f), "Tarefa pertence à Gestão responsável no término real");
            var origem = consultar(f.c().localA, rota(f), "A Gestão anterior não recebe tarefa do término prorrogado");
            var antesDoReenvio = pendenciasPersistidas(f.vinculoId());
            var repetido = consultar(f.c().localB, rota(f), "Repetição preserva a tarefa original sem duplicação");
            var depoisDoReenvio = pendenciasPersistidas(f.vinculoId());
            assertAll(
                    () -> assertThat(destino.path("registros")).as("Exatamente uma tarefa visível no destino")
                            .hasSize(1),
                    () -> assertThat(destino.path("registros").path(0).path("dataEfetiva").asText())
                            .isEqualTo(f.novoFim().toString()),
                    () -> assertThat(origem.path("registros")).as("Nenhuma tarefa indevida na origem").isEmpty(),
                    () -> assertThat(antesDoReenvio).as("Uma tarefa persistida para este vínculo").hasSize(1),
                    () -> assertThat(antesDoReenvio).allSatisfy(p -> {
                        assertThat(p.dataEfetiva()).isEqualTo(f.novoFim());
                        assertThat(p.chave()).isEqualTo("FIM_VIGENCIA_VINCULO:" + f.vinculoId() + ":" + f.novoFim());
                        assertThat(p.gestaoId()).isEqualTo(f.c().gestaoB.getId());
                        assertThat(p.superintendenciaId()).isEqualTo(f.c().seB.getId());
                        assertThat(p.estado()).isEqualTo("ABERTA");
                        assertThat(p.surgidaEm()).isNotNull();
                    }),
                    () -> assertThat(repetido).as("O mesmo registro HTTP é devolvido no reenvio").isEqualTo(destino),
                    () -> assertThat(depoisDoReenvio).as("ID, instante, data, escopo e chave permanecem idênticos")
                            .isEqualTo(antesDoReenvio),
                    () -> assertThat(tx(() -> em.find(VinculoContratual.class, f.vinculoId()).getDataFim()))
                            .as("A pendência requer tratamento explícito; gerar tarefa não encerra vínculo").isNull(),
                    () -> assertThat(eventosPersistidos(f.vinculoId())).as("Nenhum evento de término criado pelo GET")
                            .isEqualTo(eventosAntes));
        }
    }

    @Test
    void fimReduzidoProgramadoGeraPendenciaAntesDoFimMaterializadoAntigo() throws Exception {
        var c = criarCenarioPersistido();
        var hoje = RelogioSistema.hoje();
        var n = tx(() -> publicado(c, 5, null, false));
        var novoFim = n.fim().minusDays(30);
        var reducao = aditamento("ADI-PENDENCIA-REDUCAO-" + n.id(), hoje.plusDays(5));
        reducao.put("novaDataFim", novoFim);
        tx(() -> registrarAditamento(c, n.id(), reducao));
        var criado = enviar(c.localA, "/api/vinculos-contratuais", mapa("contratoId", n.id(),
                "composicaoId", n.composicao1(), "dataInicio", hoje,
                "pessoa", mapa("nome", "Pessoa do término reduzido " + n.id(),
                        "cpf", cpf(Math.toIntExact(754000000L + n.id())))), 201,
                "Vínculo nasce depois da redução documentada e conhece seu limite futuro");
        long vinculoId = criado.path("id").asLong();
        var f = new Preparado(c, n, vinculoId, novoFim);
        var faixas = consultar(c.localA, "/api/vinculos-contratuais/" + vinculoId + "/alocacoes",
                "Pré-condição: alocação inicial cobre somente a vigência conhecida, já reduzida");
        assertThat(faixas).hasSize(1);
        assertThat(faixas.path(0).path("dataFim").asText()).isEqualTo(novoFim.toString());
        assertThat(tx(() -> em.find(Contrato.class, n.id()).getDataFim())).isEqualTo(n.fim());
        assertThat(pendenciasPersistidas(vinculoId)).isEmpty();
        var eventosAntes = eventosPersistidos(vinculoId);
        try (var clock = Mockito.mockStatic(RelogioSistema.class, Mockito.CALLS_REAL_METHODS)) {
            var data = novoFim.plusDays(1);
            assertThat(data).isBefore(n.fim());
            clock.when(RelogioSistema::hoje).thenReturn(data);
            var posicao = consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + data,
                    "Pré-condição: fim canônico já passou; fim materializado antigo ainda é futuro");
            assertThat(posicao.path("vigenciaFim").asText()).isEqualTo(novoFim.toString());
            var primeira = consultar(c.localA, rota(f), "Seleção deve encontrar também contrato com master ainda futuro");
            var persistidas = pendenciasPersistidas(vinculoId);
            var repetida = consultar(c.localA, rota(f), "A seleção repetida não duplica a pendência legítima");
            assertAll(
                    () -> assertThat(primeira.path("registros")).as("Tarefa gerada no término canônico reduzido")
                            .hasSize(1),
                    () -> assertThat(primeira.path("registros").path(0).path("dataEfetiva").asText())
                            .isEqualTo(novoFim.toString()),
                    () -> assertThat(persistidas).as("Consulta em nova transação encontra exatamente uma tarefa")
                            .hasSize(1),
                    () -> assertThat(persistidas).allSatisfy(p -> {
                        assertThat(p.dataEfetiva()).isEqualTo(novoFim);
                        assertThat(p.chave()).isEqualTo("FIM_VIGENCIA_VINCULO:" + vinculoId + ":" + novoFim);
                        assertThat(p.gestaoId()).isEqualTo(c.gestaoA.getId());
                        assertThat(p.superintendenciaId()).isEqualTo(c.seA.getId());
                        assertThat(p.estado()).isEqualTo("ABERTA");
                    }),
                    () -> assertThat(repetida).isEqualTo(primeira),
                    () -> assertThat(pendenciasPersistidas(vinculoId)).isEqualTo(persistidas),
                    () -> assertThat(tx(() -> em.find(VinculoContratual.class, vinculoId).getDataFim())).isNull(),
                    () -> assertThat(eventosPersistidos(vinculoId)).isEqualTo(eventosAntes));
        }
    }

    private Preparado prepararProrrogacaoComTransferencia() throws Exception {
        var c = criarCenarioPersistido();
        var hoje = RelogioSistema.hoje();
        var n = tx(() -> publicado(c, 5, null, false));
        var criado = enviar(c.localA, "/api/vinculos-contratuais", mapa("contratoId", n.id(),
                "composicaoId", n.composicao1(), "dataInicio", hoje,
                "pessoa", mapa("nome", "Pessoa da pendência prorrogada " + n.id(),
                        "cpf", cpf(Math.toIntExact(753000000L + n.id())))), 201,
                "Criação canônica com origem nominal, vínculo e alocação documentados");
        long vinculoId = criado.path("id").asLong();
        var novoFim = n.fim().plusDays(30);
        var prorrogacao = aditamento("ADI-PENDENCIA-FUTURA-" + n.id(), hoje.plusDays(5));
        prorrogacao.put("novaDataFim", novoFim);
        tx(() -> registrarAditamento(c, n.id(), prorrogacao));
        conceder(c, n.id(), "TRANSFERENCIA_GESTAO_LOCAL");
        enviar(c.localA, base(n.id()) + "/transferir-gestao", mapa(
                "dataEfetiva", n.fim().plusDays(5), "dataAto", hoje,
                "gestaoLocalDestinoId", c.gestaoB.getId(), "tipoContratacaoDestinoId", c.tipoB.getId(),
                "motivo", "Gestão destino responsável no término prorrogado", "referenciaSei", "SEI-PENDENCIA-" + n.id(),
                "autorizacaoAdministrativa", "ATO-HTTP", "mapeamentos", List.of(mapa(
                        "composicaoOrigemId", n.composicao1(), "lotacaoDestinoId", c.lotacaoB.getId(),
                        "jornadaDestinoId", c.jornadaB.getId(), "cargoDestinoId", c.cargoB.getId()))),
                200, "Transferência programada entre fim antigo e novo, sob vigência documental prorrogada");
        assertThat(tx(() -> em.find(Contrato.class, n.id()).getDataFim()))
                .as("Pré-condição: nenhum job atualizou a projeção materializada do término").isEqualTo(n.fim());
        assertThat(pendenciasPersistidas(vinculoId)).isEmpty();
        return new Preparado(c, n, vinculoId, novoFim);
    }

    private String rota(Preparado f) {
        return "/api/pendencias?contratoIds=" + f.n().id() + "&tipo=FIM_NATURAL_VIGENCIA";
    }

    private List<PendenciaLida> pendenciasPersistidas(long vinculoId) {
        return tx(() -> em.createQuery("select p from PendenciaAdministrativa p where p.vinculoId=:id "
                        + "and p.operacaoOrigem='FIM_NATURAL_CONTRATO' order by p.id", PendenciaAdministrativa.class)
                .setParameter("id", vinculoId).getResultList().stream()
                .map(p -> new PendenciaLida(p.getId(), p.getDataEfetiva(), p.getChaveOrigem(), p.getGestaoLocalId(),
                        p.getSuperintendenciaId(), p.getEstado().name(), p.getSurgidaEm())).toList());
    }

    private List<EventoLido> eventosPersistidos(long vinculoId) {
        return tx(() -> em.createQuery("select e from EventoVinculoContratual e where e.vinculo.id=:id order by e.id",
                        EventoVinculoContratual.class).setParameter("id", vinculoId).getResultList().stream()
                .map(e -> new EventoLido(e.getId(), e.getTipoEvento().name(), e.getDataEfetiva(), e.getRegistradoEm(),
                        e.getResponsavelId(), e.getPessoaNomeFotografia(), e.getPessoaCpfFotografia(),
                        e.getHashAnterior(), e.getHashEvento(), e.getFimNovo())).toList());
    }

    private record Preparado(Cenario c, NovoContrato n, long vinculoId, LocalDate novoFim) {}
    private record PendenciaLida(Long id, LocalDate dataEfetiva, String chave, Long gestaoId,
                                Long superintendenciaId, String estado, Instant surgidaEm) {}
    private record EventoLido(Long id, String tipo, LocalDate dataEfetiva, Instant registradoEm, Long responsavelId,
                             String nome, String cpf, String hashAnterior, String hashEvento, LocalDate fimNovo) {}

    private <T> T tx(Callable<T> tarefa) {
        return emTransacao(() -> {
            try { return tarefa.call(); }
            catch (Exception erro) { throw new IllegalStateException(erro); }
        });
    }
}
