package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.vinculo.CategoriaTerminoVinculo;
import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.entidade.EventoDisponibilidadeCatalogo;
import com.example.siggtrat.entidade.MotivoDesligamento;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** CT007 nos consumidores Pessoa/Vínculo, com relógio controlado e HTTP/MySQL reais. */
class PessoasTransferenciaGestaoEfetivaHttpIT extends ContratosHttpBase {
    @org.springframework.beans.factory.annotation.Autowired
    private com.example.siggtrat.servico.GestaoContratoTemporalServico gestaoDaFonte;

    private static final String V = "/api/vinculos-contratuais";

    @Test
    void listasEDetalheDePessoaEVinculoAcompanhamGestaoAtualSemMutarProjecaoContratual() throws Exception {
        Preparado f = preparar();
        String pessoas = "/api/pessoas?contratoId=" + f.contrato().id();
        String vinculos = V + "?contratoId=" + f.contrato().id();
        assertThat(ids(consultar(f.c().localA, pessoas, "Escopo antes da transferência"))).contains(f.pessoaId());
        assertThat(ids(consultar(f.c().localB, pessoas, "Destino antes da transferência"))).doesNotContain(f.pessoaId());
        try (var relogio = Mockito.mockStatic(RelogioSistema.class, Mockito.CALLS_REAL_METHODS)) {
            relogio.when(RelogioSistema::hoje).thenReturn(f.data());
            assertThat(ids(consultar(f.c().localB, pessoas, "Pessoa no escopo efetivo do destino"))).contains(f.pessoaId());
            assertThat(ids(consultar(f.c().localA, pessoas, "Origem deixa de listar Pessoa deste contrato"))).doesNotContain(f.pessoaId());
            assertThat(ids(consultar(f.c().localB, vinculos, "Vínculo no escopo efetivo do destino"))).contains(f.vinculoId());
            assertThat(ids(consultar(f.c().localA, vinculos, "Origem deixa de listar vínculo transferido"))).doesNotContain(f.vinculoId());
            JsonNode dto = consultar(f.c().localB, V + "/" + f.vinculoId(), "DTO corrente usa a Gestão efetiva");
            assertThat(dto.path("gestaoLocalId").asLong()).isEqualTo(f.c().gestaoB.getId());
            assertThat(dto.path("gestaoLocalNome").asText()).isEqualTo(f.c().gestaoB.getNome());
            executar(f.c().localA, get(V + "/" + f.vinculoId()), 404, "Origem sem autorização corrente para vínculo transferido");
            assertThat(em.find(Contrato.class, f.contrato().id()).getGestaoLocal().getId()).isEqualTo(f.c().gestaoA.getId());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void destinoAtualizaPessoaEUsaCatalogoEFotografiaDaDataRealDoDesligamento(boolean retroativo) throws Exception {
        Preparado f = preparar();
        if (retroativo) {
            // Fonte contextual sintética opt-in deste cenário; disponibilidade permanece fonte separada.
            FonteMotivoSintetica.declarar(em, gestaoDaFonte, f.contrato().id(), f.motivoA(), f.c().gestaoA.getId(), f.c().localA,
                    "Término individual na origem", CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL,
                    f.data().minusDays(1), f.data().minusDays(1), java.time.Instant.now(), "SEI-SINTETICO-C5-PES-GESTAO-EFETIVA-RETROATIVA");
        }

        try (var relogio = Mockito.mockStatic(RelogioSistema.class, Mockito.CALLS_REAL_METHODS)) {
            relogio.when(RelogioSistema::hoje).thenReturn(f.data());
            executar(f.c().localB, put("/api/pessoas/" + f.pessoaId() + "/dados-opcionais")
                    .contentType(MediaType.APPLICATION_JSON).content(json(mapa("rg", "RG-DESTINO-EFETIVO"))), 200,
                    "PER001 a Gestão atual pode atualizar opcionais da Pessoa vinculada");
            executar(f.c().localA, put("/api/pessoas/" + f.pessoaId() + "/dados-opcionais")
                    .contentType(MediaType.APPLICATION_JSON).content(json(mapa("rg", "RG-ORIGEM-SEM-ACESSO"))), 404,
                    "PER001 a Gestão anterior não conserva escrita por projeção obsoleta");
            LocalDate fim = retroativo ? f.data().minusDays(1) : f.data().plusDays(2);
            long motivo = retroativo ? f.motivoA() : f.motivoB();
            enviar(f.c().localB, V + "/" + f.vinculoId() + "/desligar",
                    mapa("dataFim", fim, "motivoDesligamentoId", motivo,
                            "justificativa", "Término documentado pela Gestão responsável atual",
                            "evidencia", "SEI-TERMINO-POS-TRANSFERENCIA", "autorizacao", "ATO-TERMINO-TRANSFERENCIA"),
                    200, "CAT009 motivo é validado na Gestão da data do fato, com autorização corrente");
            JsonNode eventos = consultar(f.c().localB, V + "/" + f.vinculoId() + "/eventos", "AUD001 fotografia da Gestão histórica do fato");
            JsonNode ultimo = eventos.get(eventos.size() - 1);
            assertThat(ultimo.path("gestaoLocalIdFotografia").asLong()).isEqualTo(retroativo ? f.c().gestaoA.getId() : f.c().gestaoB.getId());
            assertThat(ultimo.path("motivoDesligamentoIdFotografia").asLong()).isEqualTo(motivo);
        }
    }

    private Preparado preparar() throws Exception {
        Cenario c = criarCenario();
        NovoContrato contrato = publicado(c, 100, null, false);
        JsonNode v = enviar(c.localA, V,
                mapa("pessoa", mapa("nome", "Pessoa da Gestão transferida", "cpf", cpf(Math.toIntExact(930000000L + contrato.id()))),
                        "contratoId", contrato.id(), "composicaoId", contrato.composicao1(), "dataInicio", RelogioSistema.hoje()),
                201, "Cadastro integrado antes da transferência");
        MotivoDesligamento a = new MotivoDesligamento(); a.setGestaoLocal(c.gestaoA);
        a.setDescricao("Término individual na origem"); a.setCategoria(CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL); em.persist(a);
        MotivoDesligamento b = new MotivoDesligamento(); b.setGestaoLocal(c.gestaoB);
        b.setDescricao("Término individual no destino"); b.setCategoria(CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL); em.persist(b);
        em.flush();
        for (MotivoDesligamento motivo : List.of(a, b)) {
            EventoDisponibilidadeCatalogo origem = new EventoDisponibilidadeCatalogo();
            origem.setTipoCatalogo(TipoCatalogoTemporal.MOTIVO_DESLIGAMENTO);
            origem.setCatalogoId(motivo.getId()); origem.setDataEfetiva(RelogioSistema.hoje()); origem.setAtivo(true);
            origem.setMotivo("Fonte sintética explícita da causa individual anterior à transferência programada");
            em.persist(origem);
        }
        em.flush();
        LocalDate data = RelogioSistema.hoje().plusDays(5);
        conceder(c, contrato.id(), "TRANSFERENCIA_GESTAO_LOCAL");
        enviar(c.localA, base(contrato.id()) + "/transferir-gestao",
                mapa("dataEfetiva", data, "dataAto", RelogioSistema.hoje(), "gestaoLocalDestinoId", c.gestaoB.getId(), "tipoContratacaoDestinoId", c.tipoB.getId(),
                        "motivo", "Transferência futura com vínculo existente", "referenciaSei", "SEI-TRANSFERENCIA-PESSOA",
                        "autorizacaoAdministrativa", "ATO-HTTP",
                        "mapeamentos", List.of(mapa("composicaoOrigemId", contrato.composicao1(),
                                "lotacaoDestinoId", c.lotacaoB.getId(), "jornadaDestinoId", c.jornadaB.getId(), "cargoDestinoId", c.cargoB.getId()))),
                200, "Transferência futura com mapeamento de alocação");
        em.flush(); em.clear();
        return new Preparado(c, contrato, v.path("pessoaId").asLong(), v.path("id").asLong(), data, a.getId(), b.getId());
    }

    private List<Long> ids(JsonNode pagina) {
        List<Long> ids = new ArrayList<>();
        pagina.path("content").forEach(item -> ids.add(item.path("id").asLong()));
        return ids;
    }

    private record Preparado(Cenario c, NovoContrato contrato, long pessoaId, long vinculoId,
                             LocalDate data, long motivoA, long motivoB) {}
}
