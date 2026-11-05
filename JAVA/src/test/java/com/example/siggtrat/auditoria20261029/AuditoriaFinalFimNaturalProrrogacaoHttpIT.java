package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.vinculo.CategoriaTerminoVinculo;
import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.entidade.GestaoLocal;
import com.example.siggtrat.entidade.MotivoDesligamento;
import com.example.siggtrat.entidade.VinculoContratual;
import com.example.siggtrat.entidade.EventoDisponibilidadeCatalogo;
import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.example.siggtrat.utilitario.RelogioSistema;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Proposta independente: fonte sintética explícita; nenhuma alteração direta de fatos publicados. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AuditoriaFinalFimNaturalProrrogacaoHttpIT extends ContratosHttpBase {
    @org.springframework.beans.factory.annotation.Autowired
    com.example.siggtrat.servico.GestaoContratoTemporalServico gestaoTemporal;
    @Test
    void fimNaturalNaoPodeUsarFimMaterializadoQuandoProrrogacaoProgramadaJaProduzEfeito() throws Exception {
        var f = prepararProrrogacao();
        var c = f.c(); var n = f.n(); long vinculoId = f.vinculoId(); var novoFim = f.novoFim();
        long eventosAntes = tx(() -> em.createQuery(
                "select count(e) from EventoVinculoContratual e where e.vinculo.id=:id", Long.class)
                .setParameter("id", vinculoId).getSingleResult());

        try (var clock = Mockito.mockStatic(RelogioSistema.class, Mockito.CALLS_REAL_METHODS)) {
            clock.when(RelogioSistema::hoje).thenReturn(n.fim().plusDays(1));
            var posicao = consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + n.fim().plusDays(1),
                    "Pré-condição: a prorrogação já vale antes de qualquer rotina materializadora");
            assertThat(posicao.path("situacaoTemporal").asText()).isEqualTo("VIGENTE");
            assertThat(posicao.path("vigenciaFim").asText()).isEqualTo(novoFim.toString());
            var resposta = mvc.perform(autenticar(post("/api/vinculos-contratuais/contratos/" + n.id()
                    + "/tratar-fim-natural").contentType(MediaType.APPLICATION_JSON).content("{}"), c.localA)).andReturn();
            assertAll(
                    () -> assertThat(resposta.getResponse().getStatus())
                            .as("VIN008/009: contrato ainda vigente não pode sofrer tratamento do fim natural antigo: %s",
                                    resposta.getResponse().getContentAsString()).isEqualTo(409),
                    () -> assertThat(mapper.readTree(resposta.getResponse().getContentAsString()).path("message").asText())
                            .as("409 deve resultar da vigência ainda aberta, sem mascarar ausência de causa documental")
                            .contains("ainda não alcançou o fim natural"),
                    () -> assertThat(tx(() -> em.find(VinculoContratual.class, vinculoId).getDataFim()))
                            .as("Recusa deve preservar vínculo aberto em nova transação").isNull(),
                    () -> assertThat(tx(() -> em.createQuery(
                            "select count(e) from EventoVinculoContratual e where e.vinculo.id=:id", Long.class)
                            .setParameter("id", vinculoId).getSingleResult())).isEqualTo(eventosAntes));
        }
    }

    @Test
    void fimNaturalDepoisDaProrrogacaoUsaNovoFimESuaRepeticaoNaoDuplicaEventos() throws Exception {
        var f = prepararProrrogacao();
        tx(() -> {
            long motivoId = em.createQuery("select m.id from MotivoDesligamento m where m.gestaoLocal.id=:gestao and m.categoria=:categoria", Long.class)
                    .setParameter("gestao", f.c().gestaoA.getId())
                    .setParameter("categoria", CategoriaTerminoVinculo.FIM_VIGENCIA_CONTRATO).getSingleResult();
            FonteMotivoSintetica.declarar(em, gestaoTemporal, f.n().id(), motivoId,
                    f.c().gestaoA.getId(), f.c().localA,
                    "Fonte explícita: fim natural do contrato de auditoria " + f.n().id(),
                    CategoriaTerminoVinculo.FIM_VIGENCIA_CONTRATO, f.novoFim(), f.novoFim(),
                    java.time.Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS), "SEI-SINTETICO-FIM-NATURAL-PRORROGADO");
            return null;
        });
        try (var clock = Mockito.mockStatic(RelogioSistema.class, Mockito.CALLS_REAL_METHODS)) {
            clock.when(RelogioSistema::hoje).thenReturn(f.novoFim().plusDays(1));
            var rota = "/api/vinculos-contratuais/contratos/" + f.n().id() + "/tratar-fim-natural";
            var tratados = enviar(f.c().localA, rota, mapa(), 200,
                    "VIN009/010: término legítimo usa vigência prorrogada, mesmo com coluna antiga");
            assertThat(tratados).hasSize(1);
            assertThat(tratados.get(0).path("dataFim").asText()).isEqualTo(f.novoFim().toString());
            var eventos = consultar(f.c().localA, "/api/vinculos-contratuais/" + f.vinculoId() + "/eventos",
                    "Origem e término natural mantêm datas e causa próprias");
            assertThat(eventos).hasSize(2);
            assertThat(eventos.get(1).path("tipo").asText()).isEqualTo("FIM_NATURAL_CONTRATO");
            assertThat(eventos.get(1).path("dataEfetiva").asText()).isEqualTo(f.novoFim().toString());
            assertThat(eventos.get(1).path("motivoDesligamentoCategoriaFotografia").asText())
                    .isEqualTo("FIM_VIGENCIA_CONTRATO");
            assertThat(tx(() -> em.find(VinculoContratual.class, f.vinculoId()).getDataFim()))
                    .isEqualTo(f.novoFim());
            var faixas = consultar(f.c().localA, "/api/vinculos-contratuais/" + f.vinculoId() + "/alocacoes",
                    "Faixa operacional termina no fim efetivo documentado");
            assertThat(faixas).hasSize(1);
            assertThat(faixas.get(0).path("dataFim").asText()).isEqualTo(f.novoFim().toString());
            enviar(f.c().localA, rota, mapa(), 200, "Reenvio preserva o tratamento já efetivado");
            assertThat(consultar(f.c().localA, "/api/vinculos-contratuais/" + f.vinculoId() + "/eventos",
                    "Reenvio não reescreve nem duplica a prova original")).isEqualTo(eventos);
        }
    }

    private Preparado prepararProrrogacao() throws Exception {
        var c = criarCenarioPersistido();
        var hoje = RelogioSistema.hoje();
        var n = tx(() -> publicado(c, 5, null, false));
        tx(() -> {
            var motivo = new MotivoDesligamento();
            motivo.setGestaoLocal(em.getReference(GestaoLocal.class, c.gestaoA.getId()));
            motivo.setDescricao("Fonte explícita: fim natural do contrato de auditoria " + n.id());
            motivo.setCategoria(CategoriaTerminoVinculo.FIM_VIGENCIA_CONTRATO);
            motivo.setCriadoPorId(c.localA.getId());
            motivo.setCriadoPorUsername(c.localA.getUsername());
            em.persist(motivo);
            em.flush();
            var origem = new EventoDisponibilidadeCatalogo();
            origem.setTipoCatalogo(TipoCatalogoTemporal.MOTIVO_DESLIGAMENTO);
            origem.setCatalogoId(motivo.getId());
            origem.setDataEfetiva(hoje);
            origem.setAtivo(true);
            origem.setMotivo("Fonte sintética explícita da causa desde o registro; válida também no fim antigo");
            em.persist(origem);
            return null;
        });
        var criado = enviar(c.localA, "/api/vinculos-contratuais", mapa("contratoId", n.id(),
                "composicaoId", n.composicao1(), "dataInicio", hoje,
                "pessoa", mapa("nome", "Pessoa da prorrogação tempestiva " + n.id(),
                        "cpf", cpf(Math.toIntExact(752000000L + n.id())))), 201,
                "Vínculo documentado permanece aberto; futura prorrogação não exige nova contratação");
        long vinculoId = criado.path("id").asLong();
        var prorrogacao = aditamento("ADI-PRORROGACAO-FUTURA-" + n.id(), hoje.plusDays(5));
        var novoFim = n.fim().plusDays(30);
        prorrogacao.put("novaDataFim", novoFim);
        tx(() -> registrarAditamento(c, n.id(), prorrogacao));
        assertThat(tx(() -> em.find(Contrato.class, n.id()).getDataFim())).isEqualTo(n.fim());
        return new Preparado(c, n, vinculoId, novoFim);
    }

    private record Preparado(Cenario c, NovoContrato n, long vinculoId, java.time.LocalDate novoFim) {}

    private <T> T tx(Callable<T> tarefa) {
        return emTransacao(() -> {
            try { return tarefa.call(); }
            catch (Exception erro) { throw new IllegalStateException(erro); }
        });
    }
}
