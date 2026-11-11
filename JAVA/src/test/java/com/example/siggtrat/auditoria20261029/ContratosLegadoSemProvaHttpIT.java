package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.example.siggtrat.entidade.*;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Legado sintético: remover somente fotografias das linhas criadas no próprio teste, sem DDL. */
class ContratosLegadoSemProvaHttpIT extends ContratosHttpBase {
    @Autowired DisponibilidadeCatalogoTemporalServico disponibilidade;

    @Test
    void transferenciaLegadaNaoSubstituiFotografiasAusentesPorNomesAtuais() throws Exception {
        var c = criarCenario(); var t = transferir(c);
        c.gestaoA.setNome("Gestão origem renomeada " + t.id());
        c.gestaoB.setNome("Gestão destino renomeada " + t.id());
        renomearRecortes(c, t.id()); em.flush();
        assertThat(em.createQuery("update TransferenciaGestaoContrato set gestaoOrigemNomeFotografia=null, "
                        + "gestaoDestinoNomeFotografia=null where id=:id")
                .setParameter("id", t.id()).executeUpdate()).isEqualTo(1);
        // Destino era NOT NULL no legado; vazio representa fotografia ausente sem alterar o esquema.
        assertThat(em.createQuery("update MapeamentoTransferenciaGestao set lotacaoOrigemNomeFotografia=null, "
                        + "jornadaOrigemDescricaoFotografia=null, cargoOrigemNomeFotografia=null, "
                        + "lotacaoDestinoNomeFotografia='', jornadaDestinoDescricaoFotografia=' ', "
                        + "cargoDestinoNomeFotografia='' where transferencia.id=:id")
                .setParameter("id", t.id()).executeUpdate()).isEqualTo(1);
        em.clear();

        var historico = consultar(c.nacional, base(t.contrato().id()) + "/transferencias-gestao",
                "CAT004 ausência de fotografia não autoriza usar nome mestre atual").get(0);
        assertThat(historico.path("gestaoOrigemId").asLong()).isEqualTo(c.gestaoA.getId());
        assertThat(historico.path("gestaoDestinoId").asLong()).isEqualTo(c.gestaoB.getId());
        assertThat(historico.path("gestaoOrigemNome").isNull()).isTrue();
        assertThat(historico.path("gestaoDestinoNome").isNull()).isTrue();
        var mapa = historico.path("mapeamentos").get(0);
        for (String campo : List.of("lotacaoOrigemNome", "jornadaOrigemDescricao", "cargoOrigemNome",
                "lotacaoDestinoNome", "jornadaDestinoDescricao", "cargoDestinoNome")) {
            assertThat(mapa.path(campo).isNull()).as(campo + " sem prova histórica").isTrue();
        }
        assertThat(mapa.path("lotacaoOrigemId").asLong()).isEqualTo(c.lotacaoA.getId());
        assertThat(mapa.path("lotacaoDestinoId").asLong()).isEqualTo(c.lotacaoB.getId());
        assertThat(mapa.path("jornadaDestinoId").asLong()).isEqualTo(c.jornadaB.getId());
        assertThat(mapa.path("cargoDestinoId").asLong()).isEqualTo(c.cargoB.getId());
    }

    @Test
    void aditamentoLegadoNaoInventaDescricaoDoTipoNemNomeDoRecorte() throws Exception {
        var c = criarCenario(); var n = publicado(c, 5, null, false);
        var evento = new EventoCicloVidaContrato(); evento.setContrato(em.find(Contrato.class, n.id()));
        evento.setTipoEvento(TipoEventoCicloVidaContrato.ADITAMENTO); evento.setDataEfetiva(n.inicio().plusDays(2));
        evento.setDataAto(n.inicio().plusDays(2)); evento.setChaveNegocial("ADITAMENTO:LEGADO-" + UUID.randomUUID());
        evento.setMotivo("Aditamento legado documentado sem fotografia de nomes"); evento.setReferenciaSei("SEI-LEGADO");
        em.persist(evento);
        var adi = new AditamentoContratual(); adi.setContrato(evento.getContrato()); adi.setEvento(evento);
        adi.setIdentificacao("ADI-LEGADO-" + UUID.randomUUID()); adi.setDataDocumento(evento.getDataAto());
        adi.setDataEfetiva(evento.getDataEfetiva()); adi.setJustificativa(evento.getMotivo()); adi.setReferenciaSei("SEI-LEGADO");
        adi.setUsuarioResponsavelId(c.localA.getId()); adi.setUsuarioResponsavelUsername(c.localA.getUsername());
        adi.setNovoTipoContratacao(c.tipoA); adi.setNovoEfetivoTotal(6);
        var item = new AditamentoComposicao(); item.setComposicaoBase(em.find(ComposicaoContratual.class, n.composicao1()));
        item.setLotacao(c.lotacaoA); item.setJornada(c.jornadaA); item.setCargo(c.cargoA);
        item.setQuantidadeAnterior(5); item.setVariacao(1); item.setQuantidadeResultante(6); adi.adicionarAlteracao(item);
        em.persist(adi); em.flush();
        c.tipoA.setDescricao("Tipo renomeado após o ato " + adi.getId());
        renomearRecortes(c, adi.getId()); em.flush();
        assertThat(em.createQuery("update AditamentoComposicao set lotacaoNomeFotografia='', "
                        + "jornadaDescricaoFotografia=' ', cargoNomeFotografia='' where aditamento.id=:id")
                .setParameter("id", adi.getId()).executeUpdate()).isEqualTo(1);
        em.clear();

        var historico = consultar(c.localA, base(n.id()) + "/aditamentos", "CAT004 ADI legado conserva IDs e desconhecido").get(0);
        assertThat(historico.path("novoTipoContratacaoId").asLong()).isEqualTo(c.tipoA.getId());
        assertThat(historico.path("novoTipoContratacaoDescricao").isNull()).isTrue();
        var alteracao = historico.path("alteracoesComposicao").get(0);
        assertThat(alteracao.path("lotacaoId").asLong()).isEqualTo(c.lotacaoA.getId());
        assertThat(alteracao.path("lotacaoNome").isNull()).isTrue();
        assertThat(alteracao.path("jornadaDescricao").isNull()).isTrue();
        assertThat(alteracao.path("cargoNome").isNull()).isTrue();
        assertThat(alteracao.path("quantidadeResultante").asInt()).isEqualTo(6);
        var posicao = consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + evento.getDataEfetiva(),
                "CAT004 replay de ADI sem foto não toma descrição atual como histórica").path("versaoContratual");
        assertThat(posicao.path("tipoContratacaoId").asLong()).isEqualTo(c.tipoA.getId());
        assertThat(posicao.path("tipoContratacaoDescricao").isNull()).isTrue();
    }

    @Test
    void transferenciaLegadaSemTipoDestinoNaoDeclaraTipoDaGestaoAnterior() throws Exception {
        var c = criarCenario(); var t = transferir(c); simularTransferenciaSemTipo(c, t);

        var antes = consultar(c.nacional, base(t.contrato().id()) + "/posicao-temporal?data=" + t.data().minusDays(1),
                "CON023 Tipo conhecido no período anterior permanece preservado").path("versaoContratual");
        assertThat(antes.path("tipoContratacaoId").asLong()).isEqualTo(c.tipoA.getId());
        var depois = consultar(c.localB, base(t.contrato().id()) + "/posicao-temporal?data=" + t.data(),
                "CON023 Tipo de destino não comprovado deve permanecer desconhecido").path("versaoContratual");
        assertThat(depois.path("gestaoLocalId").asLong()).isEqualTo(c.gestaoB.getId());
        assertThat(depois.path("tipoContratacaoId").isNull()).isTrue();
        assertThat(depois.path("tipoContratacaoDescricao").isNull()).isTrue();
        assertThat(depois.path("matriculaTipoContratacaoObrigatoria").isNull()).isTrue();
        assertThat(depois.has("tipoContratacaoComprovado")).isTrue();
        assertThat(depois.path("tipoContratacaoComprovado").asBoolean()).isFalse();
        var detalhe = consultar(c.localB, base(t.contrato().id()), "CON023 detalhe não declara Tipo inválido como contexto válido");
        assertThat(detalhe.path("tipoContratacaoId").isNull()).isTrue();
        assertThat(detalhe.path("tipoContratacaoDescricao").isNull()).isTrue();
    }

    @Test
    void aditamentoQueDependeDoTipoLegadoExigeRegularizacaoExplicitaDoDestino() throws Exception {
        var c = criarCenario(); var t = transferir(c); simularTransferenciaSemTipo(c, t);
        var req = aditamento("ADI-POLITICA-TIPO-SEM-PROVA", RelogioSistema.hoje());
        req.put("restaurarPoliticaMatriculaDoTipo", true);
        var negativa = executar(c.localB, post(base(t.contrato().id()) + "/aditamentos/previa")
                        .contentType(MediaType.APPLICATION_JSON).content(json(req)), 409,
                "CON023 não restaurar política de um Tipo destino que o legado não comprovou");
        assertThat(mapper.readTree(negativa.getResponse().getContentAsString()).path("message").asText())
                .containsIgnoringCase("Tipo de Contratação de destino não comprovado");
        assertThat(em.createQuery("select count(a) from AditamentoContratual a where a.contrato.id=:id", Long.class)
                .setParameter("id", t.contrato().id()).getSingleResult()).isZero();

        req.put("novoTipoContratacaoId", c.tipoB.getId());
        var previa = enviar(c.localB, base(t.contrato().id()) + "/aditamentos/previa", req, 200,
                "CON023 novo ato pode estabelecer Tipo destino explícito, válido e documentado");
        req.put("tokenPrevia", previa.path("tokenPrevia").asText());
        enviar(c.localB, base(t.contrato().id()) + "/aditamentos", req, 201,
                "CON023 regularização explícita não inventa o contexto dos dias anteriores");
        var antes = consultar(c.localB, base(t.contrato().id()) + "/posicao-temporal?data=" + t.data(),
                "CON023 período sem prova continua desconhecido").path("versaoContratual");
        assertThat(antes.path("tipoContratacaoId").isNull()).isTrue();
        var depois = consultar(c.localB, base(t.contrato().id()) + "/posicao-temporal?data=" + RelogioSistema.hoje(),
                "CON023 novo ato fornece a prova do Tipo a partir de sua data").path("versaoContratual");
        assertThat(depois.path("tipoContratacaoId").asLong()).isEqualTo(c.tipoB.getId());
        assertThat(depois.path("tipoContratacaoComprovado").asBoolean()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void aditamentoAntesDoPrimeiroFatoSemEstadoAnteriorRetornaConflitoDeConhecimento(boolean estadoAtual) throws Exception {
        var c = criarCenario(); var n = publicado(c, 5, null, false);
        removerFonteSinteticaDoCargo(c.cargoA.getId());
        var evento = disponibilidade(c.cargoA.getId(), RelogioSistema.hoje().minusDays(5), false, null);
        c.cargoA.setAtivo(estadoAtual); em.persist(evento); em.flush();
        var req = aditamento("ADI-ANTES-DA-PROVA", n.inicio().plusDays(2));
        req.put("alteracoesComposicao", List.of(mapa("composicaoId", n.composicao1(), "quantidadeResultante", 6)));
        var resposta = executar(c.localA, post(base(n.id()) + "/aditamentos/previa").contentType(MediaType.APPLICATION_JSON)
                        .content(json(req)), 409, "CAT007 estado atual não comprova disponibilidade anterior à primeira transição");
        assertThat(mapper.readTree(resposta.getResponse().getContentAsString()).path("message").asText())
                .containsIgnoringCase("disponibilidade histórica não comprovada");
        assertThat(em.createQuery("select count(a) from AditamentoContratual a where a.contrato.id=:id", Long.class)
                .setParameter("id", n.id()).getSingleResult()).isZero();
    }

    @Test
    void disponibilidadeSemEventoPermaneceDesconhecidaAtePrimeiroFatoDocumentado() {
        var c = criarCenario(); long id = c.cargoA.getId();
        removerFonteSinteticaDoCargo(id);
        assertThatThrownBy(() -> disponibilidade.ativoEm(TipoCatalogoTemporal.CARGO, id, DATA, true))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class).hasMessageContaining("409");
        assertThatThrownBy(() -> disponibilidade.ativoEm(TipoCatalogoTemporal.CARGO, id, DATA, false))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class).hasMessageContaining("409");
        LocalDate data = RelogioSistema.hoje().minusDays(5);
        em.persist(disponibilidade(id, data, false, true)); em.flush();
        assertThatThrownBy(() -> disponibilidade.ativoEm(TipoCatalogoTemporal.CARGO, id, data.minusDays(1), false))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class).hasMessageContaining("409");
        assertThat(disponibilidade.ativoEm(TipoCatalogoTemporal.CARGO, id, data, true)).isFalse();
        assertThat(disponibilidade.ativoEm(TipoCatalogoTemporal.CARGO, id, data.plusDays(1), true)).isFalse();
    }

    @Test
    void transferenciaNovaContinuaExibindoFotografiasETipoComprovados() throws Exception {
        var c = criarCenario(); String origem = c.gestaoA.getNome(), destino = c.gestaoB.getNome();
        String cargoOrigem = c.cargoA.getNome(), cargoDestino = c.cargoB.getNome(), tipo = c.tipoB.getDescricao();
        var t = transferir(c); renomearRecortes(c, t.id()); c.tipoB.setDescricao(tipo + " atual");
        c.gestaoA.setNome(origem + " atual"); c.gestaoB.setNome(destino + " atual"); em.flush(); em.clear();
        var resposta = consultar(c.nacional, base(t.contrato().id()) + "/transferencias-gestao", "CAT004 fotografia conhecida preservada").get(0);
        assertThat(resposta.path("gestaoOrigemNome").asText()).isEqualTo(origem);
        assertThat(resposta.path("gestaoDestinoNome").asText()).isEqualTo(destino);
        assertThat(resposta.path("tipoContratacaoDestinoId").asLong()).isEqualTo(c.tipoB.getId());
        assertThat(resposta.path("tipoContratacaoDestinoDescricao").asText()).isEqualTo(tipo);
        assertThat(resposta.path("mapeamentos").get(0).path("cargoOrigemNome").asText()).isEqualTo(cargoOrigem);
        assertThat(resposta.path("mapeamentos").get(0).path("cargoDestinoNome").asText()).isEqualTo(cargoDestino);
        var posicao = consultar(c.localB, base(t.contrato().id()) + "/posicao-temporal?data=" + t.data(),
                "CON023 transferência nova tem Tipo comprovado e política herdada conhecida").path("versaoContratual");
        assertThat(posicao.path("tipoContratacaoComprovado").asBoolean()).isTrue();
        assertThat(posicao.path("tipoContratacaoDescricao").asText()).isEqualTo(tipo);
        assertThat(posicao.path("matriculaVinculoObrigatoriaEfetiva").isBoolean()).isTrue();
        assertThat(posicao.path("matriculaVinculoObrigatoriaEfetiva").asBoolean()).isFalse();
    }

    private EventoDisponibilidadeCatalogo disponibilidade(Long id, LocalDate data, boolean ativo, Boolean antes) {
        var e = new EventoDisponibilidadeCatalogo(); e.setTipoCatalogo(TipoCatalogoTemporal.CARGO); e.setCatalogoId(id);
        e.setDataEfetiva(data); e.setAtivo(ativo); e.setAtivoAnterior(antes); e.setMotivo("Transição sintética para prova do legado");
        return e;
    }

    private void removerFonteSinteticaDoCargo(Long id) {
        // Somente as linhas criadas pelo próprio teste; ausência de fonte é a
        // condição deliberada deste cenário, preservada após fortalecer a fixture comum.
        em.createQuery("delete from EventoDisponibilidadeCatalogo e where e.tipoCatalogo=:tipo and e.catalogoId=:id")
                .setParameter("tipo", TipoCatalogoTemporal.CARGO).setParameter("id", id).executeUpdate();
    }

    private void renomearRecortes(Cenario c, long id) {
        c.lotacaoA.setNome("Lotação origem atual " + id); c.lotacaoB.setNome("Lotação destino atual " + id);
        c.jornadaA.setDescricao("Turno origem atual " + id); c.jornadaB.setDescricao("Turno destino atual " + id);
        c.cargoA.setNome("Cargo origem atual " + id); c.cargoB.setNome("Cargo destino atual " + id);
    }

    private void simularTransferenciaSemTipo(Cenario c, TransferenciaSintetica t) {
        em.flush();
        assertThat(em.createQuery("update TransferenciaGestaoContrato set tipoContratacaoDestino=null, "
                        + "tipoDestinoDescricaoFotografia=null where id=:id")
                .setParameter("id", t.id()).executeUpdate()).isEqualTo(1);
        // A implementação anterior copiava para a foto posterior o Tipo da origem.
        assertThat(em.createQuery("update VersaoContratoNegocial set tipoContratacaoId=:tipo, "
                        + "tipoContratacaoDescricao=:descricao, matriculaTipoContratacaoObrigatoria=null "
                        + "where eventoOrigem.id=:evento")
                .setParameter("tipo", c.tipoA.getId()).setParameter("descricao", c.tipoA.getDescricao())
                .setParameter("evento", t.evento()).executeUpdate()).isEqualTo(1);
        em.clear();
    }

    private TransferenciaSintetica transferir(Cenario c) throws Exception {
        var n = publicado(c, 5, null, false); conceder(c, n.id(), "TRANSFERENCIA_GESTAO_LOCAL");
        LocalDate data = RelogioSistema.hoje().minusDays(2);
        Map<String, Object> req = mapa(/* Conferência SINTÉTICA explícita desta fixture; não é padrão do produto. */
                "conferenciaDocumentalConfirmada", true, "versaoConferenciaDocumental", "transferencia-gestao-conferencia-v1", "dataEfetiva", data, "dataAto", RelogioSistema.hoje(),
                "gestaoLocalDestinoId", c.gestaoB.getId(), "tipoContratacaoDestinoId", c.tipoB.getId(),
                "motivo", "Transferência sintética documentada", "referenciaSei", "SEI-LEGADO",
                "autorizacaoAdministrativa", "ATO-HTTP", "mapeamentos", List.of(mapa("composicaoOrigemId", n.composicao1(),
                        "lotacaoDestinoId", c.lotacaoB.getId(), "jornadaDestinoId", c.jornadaB.getId(), "cargoDestinoId", c.cargoB.getId())));
        enviar(c.localA, base(n.id()) + "/transferir-gestao", req, 200, "CON023 preparação de transferência real pela API");
        var t = em.createQuery("select t from TransferenciaGestaoContrato t where t.contrato.id=:id", TransferenciaGestaoContrato.class)
                .setParameter("id", n.id()).getSingleResult();
        return new TransferenciaSintetica(n, t.getId(), t.getEvento().getId(), data);
    }

    private record TransferenciaSintetica(NovoContrato contrato, long id, long evento, LocalDate data) {}
}
