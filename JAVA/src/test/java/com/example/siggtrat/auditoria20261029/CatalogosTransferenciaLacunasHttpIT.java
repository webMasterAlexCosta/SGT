package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.example.siggtrat.servico.DisponibilidadeCatalogoTemporalServico;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** CAT004/005/007 e CON023 exercitados pela mutação real dos catálogos e transferência. */
class CatalogosTransferenciaLacunasHttpIT extends ContratosHttpBase {
    @Autowired DisponibilidadeCatalogoTemporalServico disponibilidade;

    @ParameterizedTest
    @ValueSource(strings = {"cargos", "lotacoes", "jornadas", "tipos-contratacao"})
    void primeiraInativacaoSemFonteNaoInventaDisponibilidadeAnterior(String recurso) throws Exception {
        var c = criarCenario(); encerrarUsos(c, false);
        long id = id(c, recurso, false);
        // Retira somente a prova sintética fornecida pelo cenário para exercitar
        // um legado sem origem. CAT-007 exige validade comprovada na data efetiva.
        em.createQuery("delete from EventoDisponibilidadeCatalogo e where e.tipoCatalogo=:tipo and e.catalogoId=:id")
                .setParameter("tipo", tipo(recurso)).setParameter("id", id).executeUpdate();
        assertThat(em.createQuery("select count(e) from EventoDisponibilidadeCatalogo e where e.tipoCatalogo=:tipo and e.catalogoId=:id", Long.class)
                .setParameter("tipo", tipo(recurso)).setParameter("id", id).getSingleResult()).isZero();
        assertThatThrownBy(() -> disponibilidade.ativoEm(tipo(recurso), id, DATA, true))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class).hasMessageContaining("409");

        if (recurso.equals("tipos-contratacao")) {
            executar(c.localA, put("/api/tipos-contratacao/{id}", id).contentType(MediaType.APPLICATION_JSON)
                    .content(json(mapa("descricao", c.tipoA.getDescricao(), "ativo", false))), 200,
                    "CAT007 primeira transição real do tipo legado");
        } else {
            executar(c.localA, delete("/api/" + recurso + "/{id}", id), 204,
                    "CAT007 primeira transição real de cadastro sem eventos anteriores");
        }

        assertThatThrownBy(() -> disponibilidade.ativoEm(tipo(recurso), id, DATA, false))
                .as("Estado anterior conhecido não comprova desde quando era válido")
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class).hasMessageContaining("409");
        assertThat(disponibilidade.ativoEm(tipo(recurso), id, RelogioSistema.hoje().plusDays(1), false)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"cargos", "lotacoes", "jornadas"})
    void transferenciaProgramadaSemPessoasImpedeInativarCatalogoDoDestino(String recurso) throws Exception {
        var c = criarCenario(); encerrarUsos(c, true);
        var n = publicado(c, 5, null, false); conceder(c, n.id(), "TRANSFERENCIA_GESTAO_LOCAL");
        var req = transferencia(c, n, RelogioSistema.hoje().plusDays(5));
        enviar(c.localA, base(n.id()) + "/transferir-gestao", req, 200,
                "CON023 transferência futura cria dependência de catálogo sem vínculo alocado");
        assertThat(em.createQuery("select count(v) from VinculoContratual v where v.contrato.id=:id", Long.class)
                .setParameter("id", n.id()).getSingleResult()).isZero();

        executar(c.localB, delete("/api/" + recurso + "/{id}", id(c, recurso, true)), 409,
                "CAT005 programação de transferência deve ser tratada antes da inativação do destino");
    }

    @Test
    void transferenciaExigeMapeamentoExplicitoDoTipoLocalDeDestino() throws Exception {
        var c = criarCenario(); var n = publicado(c, 5, null, false);
        conceder(c, n.id(), "TRANSFERENCIA_GESTAO_LOCAL");
        var req = transferencia(c, n, RelogioSistema.hoje()); req.remove("tipoContratacaoDestinoId");

        enviar(c.localA, base(n.id()) + "/transferir-gestao", req, 400,
                "CON023/CAT001 transferência exige mapear também o Tipo de Contratação local");
    }

    @Test
    void transferenciaRejeitaTipoQuePertenceAGestaoDeOrigem() throws Exception {
        var c = criarCenario(); var n = publicado(c, 5, null, false);
        conceder(c, n.id(), "TRANSFERENCIA_GESTAO_LOCAL");
        var req = transferencia(c, n, RelogioSistema.hoje()); req.put("tipoContratacaoDestinoId", c.tipoA.getId());

        enviar(c.localA, base(n.id()) + "/transferir-gestao", req, 400,
                "CON023 tipo posterior deve pertencer à Gestão responsável pelo período");
    }

    @Test
    void transferenciaUsaTipoMapeadoNoDestinoEPreservaTipoAnterior() throws Exception {
        var c = criarCenario(); var n = publicado(c, 5, null, false);
        conceder(c, n.id(), "TRANSFERENCIA_GESTAO_LOCAL");
        enviar(c.localA, base(n.id()) + "/transferir-gestao", transferencia(c, n, RelogioSistema.hoje()), 200,
                "CON023 transferência com todos os catálogos de destino explícitos");

        var antes = consultar(c.nacional, base(n.id()) + "/posicao-temporal?data=" + RelogioSistema.hoje().minusDays(1),
                "CON023 contexto originário antes do evento").path("versaoContratual");
        var depois = consultar(c.localB, base(n.id()) + "/posicao-temporal?data=" + RelogioSistema.hoje(),
                "CON023 contexto de destino depois do evento").path("versaoContratual");
        assertThat(antes.path("tipoContratacaoId").asLong()).isEqualTo(c.tipoA.getId());
        assertThat(depois.path("tipoContratacaoId").asLong()).isEqualTo(c.tipoB.getId());
        assertThat(depois.path("tipoContratacaoDescricao").asText()).isEqualTo(c.tipoB.getDescricao());
    }

    @Test
    void historicoDaTransferenciaNaoReescreveNomesAposRenomearCatalogosEGestoes() throws Exception {
        var c = criarCenario(); var n = publicado(c, 5, null, false);
        String gestaoOrigem = c.gestaoA.getNome(), gestaoDestino = c.gestaoB.getNome();
        String lotacaoOrigem = c.lotacaoA.getNome(), lotacaoDestino = c.lotacaoB.getNome();
        String jornadaOrigem = c.jornadaA.getDescricao(), jornadaDestino = c.jornadaB.getDescricao();
        String cargoOrigem = c.cargoA.getNome(), cargoDestino = c.cargoB.getNome();
        conceder(c, n.id(), "TRANSFERENCIA_GESTAO_LOCAL");
        enviar(c.localA, base(n.id()) + "/transferir-gestao", transferencia(c, n, RelogioSistema.hoje()), 200,
                "CON023 transferência fotografia o contexto documental");
        c.gestaoA.setNome(gestaoOrigem + " atual"); c.gestaoB.setNome(gestaoDestino + " atual");
        c.lotacaoA.setNome(lotacaoOrigem + " atual"); c.lotacaoB.setNome(lotacaoDestino + " atual");
        c.jornadaA.setDescricao(jornadaOrigem + " atual"); c.jornadaB.setDescricao(jornadaDestino + " atual");
        c.cargoA.setNome(cargoOrigem + " atual"); c.cargoB.setNome(cargoDestino + " atual"); em.flush();

        var historico = consultar(c.nacional, base(n.id()) + "/transferencias-gestao", "CAT004 nomes da fotografia são imutáveis").get(0);
        assertThat(historico.path("gestaoOrigemNome").asText()).isEqualTo(gestaoOrigem);
        assertThat(historico.path("gestaoDestinoNome").asText()).isEqualTo(gestaoDestino);
        var mapa = historico.path("mapeamentos").get(0);
        assertThat(mapa.path("lotacaoOrigemNome").asText()).isEqualTo(lotacaoOrigem);
        assertThat(mapa.path("lotacaoDestinoNome").asText()).isEqualTo(lotacaoDestino);
        assertThat(mapa.path("jornadaOrigemDescricao").asText()).isEqualTo(jornadaOrigem);
        assertThat(mapa.path("jornadaDestinoDescricao").asText()).isEqualTo(jornadaDestino);
        assertThat(mapa.path("cargoOrigemNome").asText()).isEqualTo(cargoOrigem);
        assertThat(mapa.path("cargoDestinoNome").asText()).isEqualTo(cargoDestino);
    }

    private Map<String, Object> transferencia(Cenario c, NovoContrato n, LocalDate data) {
        return mapa("dataEfetiva", data, "dataAto", RelogioSistema.hoje(), "gestaoLocalDestinoId", c.gestaoB.getId(),
                "tipoContratacaoDestinoId", c.tipoB.getId(), "motivo", "Transferência com contexto documental completo",
                "referenciaSei", "SEI-TRANSFERENCIA", "autorizacaoAdministrativa", "ATO-HTTP",
                "mapeamentos", List.of(mapa("composicaoOrigemId", n.composicao1(), "lotacaoDestinoId", c.lotacaoB.getId(),
                        "jornadaDestinoId", c.jornadaB.getId(), "cargoDestinoId", c.cargoB.getId())));
    }

    private void encerrarUsos(Cenario c, boolean ladoB) {
        LocalDate fim = RelogioSistema.hoje().minusDays(1);
        var contrato = ladoB ? c.contratoB : c.contratoA; contrato.setDataFim(fim);
        // O encerramento sintético dos usos deve constar da fonte publicada.
        // Alterar só a projeção não retira a vigência futura que o motor reconstrói.
        em.createQuery("select v from VersaoContratoNegocial v where v.contrato.id=:c",
                        com.example.siggtrat.entidade.VersaoContratoNegocial.class)
                .setParameter("c", contrato.getId()).getResultList().forEach(v -> v.setDataFim(fim));
        var composicao = ladoB ? c.composicaoB : c.composicaoA; composicao.setDataFimVigencia(fim);
        var vinculo = ladoB ? c.vinculoB : c.vinculoA; vinculo.setDataFim(fim);
        var alocacao = ladoB ? c.alocacaoB : c.alocacaoA; alocacao.setDataFim(fim); em.flush();
    }

    private long id(Cenario c, String recurso, boolean ladoB) {
        return switch (recurso) {
            case "cargos" -> (ladoB ? c.cargoB : c.cargoA).getId();
            case "lotacoes" -> (ladoB ? c.lotacaoB : c.lotacaoA).getId();
            case "jornadas" -> (ladoB ? c.jornadaB : c.jornadaA).getId();
            default -> (ladoB ? c.tipoB : c.tipoA).getId();
        };
    }

    private TipoCatalogoTemporal tipo(String recurso) {
        return switch (recurso) {
            case "cargos" -> TipoCatalogoTemporal.CARGO;
            case "lotacoes" -> TipoCatalogoTemporal.LOTACAO;
            case "jornadas" -> TipoCatalogoTemporal.JORNADA;
            default -> TipoCatalogoTemporal.TIPO_CONTRATACAO;
        };
    }
}
