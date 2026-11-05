package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.entidade.MotivoDesligamento;
import com.example.siggtrat.dominio.vinculo.CategoriaTerminoVinculo;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Casos ausentes do baseline: fotografias completas, precedência e habilitação temporal. */
class ContratosLacunasFuncionaisHttpIT extends ContratosHttpBase {
    @org.springframework.beans.factory.annotation.Autowired
    private com.example.siggtrat.servico.GestaoContratoTemporalServico gestaoTemporal;

    @Test
    void publicacaoFotografaMatriculaEDiaReduzidoSemDependerDoCadastroAtual() throws Exception {
        var c = criarCenario();
        c.tipoA.setMatriculaVinculoObrigatoria(true); em.flush();
        var n = publicarComConfiguracao(c, mapa("matriculaVinculoObrigatoria", false,
                "diaSemanaReduzido", "FRIDAY", "horasDiaReduzido", 6));

        var foto = consultar(c.localA, base(n.id()) + "/versoes", "CON008/HIS006 foto integral da publicação").get(0);

        assertThat(foto.path("matriculaVinculoObrigatoria").isBoolean()).isTrue();
        assertThat(foto.path("matriculaVinculoObrigatoria").asBoolean()).isFalse();
        assertThat(foto.path("matriculaVinculoObrigatoriaEfetiva").isBoolean()).isTrue();
        assertThat(foto.path("matriculaVinculoObrigatoriaEfetiva").asBoolean()).isFalse();
        assertThat(foto.path("diaSemanaReduzido").asText()).isEqualTo("FRIDAY");
        assertThat(foto.path("horasDiaReduzido").asInt()).isEqualTo(6);
    }

    @Test
    void publicacaoInativaPermaneceInativaNaPosicaoTemporal() throws Exception {
        var c = criarCenario(); var n = publicarComConfiguracao(c, mapa("ativo", false));
        var posicao = consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + n.inicio(),
                "CON007/022 a publicação não redefine habilitação administrativa");
        assertThat(posicao.path("estadoNegocial").asText()).isEqualTo("PUBLICADO");
        assertThat(posicao.path("habilitacaoAdministrativa").asText()).isEqualTo("INATIVO");
    }

    @Test
    void alterarTipoMestreNaoDispensaMatriculaExigidaNaPublicacao() throws Exception {
        var c = criarCenario(); c.tipoA.setMatriculaVinculoObrigatoria(true); em.flush();
        var n = publicado(c, 5, null, false);
        executar(c.localA, put("/api/tipos-contratacao/{id}", c.tipoA.getId()).contentType(MediaType.APPLICATION_JSON)
                .content(json(mapa("descricao", c.tipoA.getDescricao(), "ativo", true, "matriculaVinculoObrigatoria", false))),
                200, "CAT004 alteração do cadastro mestre depois da publicação");
        var req = mapa("pessoa", mapa("nome", "Pessoa da política publicada", "cpf", cpf(840000000 + c.pessoaA.getId().intValue())),
                "contratoId", n.id(), "composicaoId", n.composicao1(), "dataInicio", n.inicio().plusDays(1));

        enviar(c.localA, "/api/vinculos-contratuais", req, 400,
                "MAT005/CAT004 a exigência publicada continua válida na data do vínculo");
        assertThat(em.createQuery("select count(v) from VinculoContratual v where v.contrato.id=:id", Long.class)
                .setParameter("id", n.id()).getSingleResult()).isZero();
    }

    @Test
    void aditamentoAlteraConfiguracoesNegociaisPreservandoFotoOriginal() throws Exception {
        var c = criarCenario(); var n = publicarComConfiguracao(c, mapa("matriculaVinculoObrigatoria", false,
                "diaSemanaReduzido", "FRIDAY", "horasDiaReduzido", 6));
        var req = aditamento("ADI-CONFIG", RelogioSistema.hoje());
        req.put("novaDescricao", "Configuração documentada alterada");
        req.put("novaMatriculaVinculoObrigatoria", true);
        req.put("novoDiaSemanaReduzido", "THURSDAY"); req.put("novasHorasDiaReduzido", 5);
        registrarAditamento(c, n.id(), req);

        var posicao = consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + RelogioSistema.hoje(),
                "CON005/008 §5.9 demais campos publicados são alterados por ato documentado");
        var atual = posicao.path("versaoContratual");
        assertThat(atual.path("matriculaVinculoObrigatoriaEfetiva").asBoolean()).isTrue();
        assertThat(atual.path("diaSemanaReduzido").asText()).isEqualTo("THURSDAY");
        assertThat(atual.path("horasDiaReduzido").asInt()).isEqualTo(5);
        var original = consultar(c.localA, base(n.id()) + "/versoes", "HIS001 fotografia original preservada").get(0);
        assertThat(original.path("matriculaVinculoObrigatoriaEfetiva").asBoolean()).isFalse();
        assertThat(original.path("diaSemanaReduzido").asText()).isEqualTo("FRIDAY");
        assertThat(original.path("horasDiaReduzido").asInt()).isEqualTo(6);
        var detalhe = consultar(c.localA, base(n.id()), "CON005 detalhe usa os valores negociais atuais");
        assertThat(detalhe.path("diaSemanaReduzido").asText()).isEqualTo("THURSDAY");
        assertThat(detalhe.path("horasDiaReduzido").asInt()).isEqualTo(5);
        assertThat(detalhe.path("matriculaVinculoObrigatoria").asBoolean()).isTrue();
    }

    @Test
    void aditamentoRejeitaConfiguracaoDeDiaReduzidoIncompleta() throws Exception {
        var c = criarCenario(); var n = publicado(c, 5, null, false);
        var req = aditamento("ADI-DIA-INCOMPLETO", RelogioSistema.hoje());
        req.put("novaDescricao", "Não deve ocultar configuração inválida"); req.put("novoDiaSemanaReduzido", "FRIDAY");

        enviar(c.localA, base(n.id()) + "/aditamentos/previa", req, 400,
                "CON008/ADI002 dia reduzido exige quantidade de horas válida");
    }

    @Test
    void correcaoDaAnulacaoNaoReutilizaReativacaoAdministrativaAnterior() throws Exception {
        var c = criarCenario(); var n = publicado(c, 5, null, false);
        var motivo = new MotivoDesligamento(); motivo.setGestaoLocal(c.gestaoA);
        motivo.setDescricao("Anulação contratual documentada"); motivo.setCategoria(CategoriaTerminoVinculo.ANULACAO_CONTRATO);
        em.persist(motivo); em.flush();
        var origem = new com.example.siggtrat.entidade.EventoDisponibilidadeCatalogo();
        origem.setTipoCatalogo(com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal.MOTIVO_DESLIGAMENTO);
        origem.setCatalogoId(motivo.getId()); origem.setDataEfetiva(n.inicio()); origem.setAtivo(true);
        origem.setMotivo("Fonte sintética da causa de anulação desde o início do contrato deste cenário");
        em.persist(origem); em.flush();
        executarAcao(c, n.id(), "inativar", mapa("dataEfetiva", n.inicio().plusDays(1), "motivo", "Suspensão anterior"));
        executarAcao(c, n.id(), "reativar", mapa("dataEfetiva", n.inicio().plusDays(2), "motivo", "Retomada anterior"));
        // Premissa positiva opt-in, limitada ao dia do ato e à Gestão deste contrato.
        // Disponibilidade do cadastro não comprova sua descrição/categoria histórica.
        FonteMotivoSintetica.declarar(em, gestaoTemporal, n.id(), motivo.getId(),
                c.gestaoA.getId(), c.localA, "Anulação contratual documentada",
                CategoriaTerminoVinculo.ANULACAO_CONTRATO,
                n.inicio().plusDays(3), n.inicio().plusDays(3),
                com.example.siggtrat.utilitario.PrecisaoTemporal.agoraMicros(),
                "SEI-SINTETICO-CON020-REATIVACAO-ANTERIOR");
        executarAcao(c, n.id(), "anular", mapa("dataEfetiva", n.inicio().plusDays(3), "dataAto", RelogioSistema.hoje(),
                "motivo", "Anulação indevida", "referenciaSei", "SEI-ANULACAO"));
        var eventos = consultar(c.localA, base(n.id()) + "/eventos", "CON020 localizar ato original preservado");
        long anulacao = 0;
        for (var evento : eventos) if (evento.path("tipoEvento").asText().equals("ANULACAO")) anulacao = evento.path("id").asLong();
        assertThat(anulacao).isPositive();
        executarAcao(c, n.id(), "corrigir-anulacao", mapa("dataEfetiva", n.inicio().plusDays(3),
                "dataAto", RelogioSistema.hoje(), "motivo", "Correção documentada", "referenciaSei", "SEI-CORRECAO",
                "eventoPrecedenteId", anulacao, "relacaoPrecedencia", "CANCELA"));

        var posicao = consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + RelogioSistema.hoje(),
                "CON020 correção não reativa silenciosamente o contrato");
        var detalhe = consultar(c.localA, base(n.id()), "CON022 detalhe e posição compartilham a mesma habilitação");
        assertThat(posicao.path("habilitacaoAdministrativa").asText()).isEqualTo("INATIVO");
        assertThat(detalhe.path("habilitacaoAdministrativa").asText()).isEqualTo("INATIVO");
    }

    @ParameterizedTest
    @ValueSource(strings = {"aditamentos", "remanejamentos"})
    void inativacaoProgramadaImpedeNovoInstrumentoQuandoSuaDataChega(String recurso) throws Exception {
        var c = criarCenario(); var n = publicado(c, 100, 50, false);
        LocalDate bloqueio = RelogioSistema.hoje().plusDays(1);
        executarAcao(c, n.id(), "inativar", mapa("dataEfetiva", bloqueio, "motivo", "Inativação programada"));
        assertThat(em.find(Contrato.class, n.id()).getAtivo()).isTrue();
        try (var clock = Mockito.mockStatic(RelogioSistema.class, Mockito.CALLS_REAL_METHODS)) {
            clock.when(RelogioSistema::hoje).thenReturn(bloqueio);
            var detalhe = consultar(c.localA, base(n.id()), "CON010 inativação é efetiva pela data, sem rotina de materialização");
            assertThat(detalhe.path("habilitacaoAdministrativa").asText()).isEqualTo("INATIVO");
            Map<String, Object> req;
            if (recurso.equals("aditamentos")) {
                req = aditamento("ADI-APOS-BLOQUEIO", bloqueio.plusDays(1)); req.put("novaDescricao", "Alteração posterior ao bloqueio");
                previaAditamento(c, n.id(), req);
            } else {
                req = remanejamento(n, "REM-APOS-BLOQUEIO", bloqueio.plusDays(1), 5); previaRemanejamento(c, n.id(), req);
            }
            enviar(c.localA, base(n.id()) + "/" + recurso, req, 409,
                    "CON010/REM006 fato programado não libera nova operação após início da inativação");
        }
    }

    @Test
    void aditamentoRetroativoRespeitaAssociacaoPublicadaAposRemocaoNoCatalogoAtual() throws Exception {
        var c = criarCenario(); var n = publicado(c, 10, null, false);
        executar(c.localA, put("/api/lotacoes/{id}", c.lotacaoA.getId()).contentType(MediaType.APPLICATION_JSON)
                .content(json(mapa("nome", c.lotacaoA.getNome(), "municipio", c.lotacaoA.getMunicipio(), "ativo", true,
                        "gestaoLocalId", c.gestaoA.getId(), "turnos", List.of()))), 200,
                "CAT004 atualização de associação mestre não apaga combinação publicada");
        var req = aditamento("ADI-ASSOCIACAO-HISTORICA", n.inicio().plusDays(2));
        req.put("alteracoesComposicao", List.of(mapa("composicaoId", n.composicao1(), "quantidadeResultante", 11)));
        var salvo = registrarAditamento(c, n.id(), req);
        assertThat(salvo.path("novoEfetivoTotal").asInt()).isEqualTo(11);
    }

    private NovoContrato publicarComConfiguracao(Cenario c, Map<String, Object> configuracoes) throws Exception {
        LocalDate inicio = RelogioSistema.hoje().minusDays(30), fim = RelogioSistema.hoje().plusDays(120);
        var req = rascunho(c, "LACUNA-" + UUID.randomUUID().toString().substring(0, 8), 5, inicio, fim);
        req.putAll(configuracoes);
        long id = enviar(c.localA, "/api/contratos", req, 201, "CON008 rascunho com configuração explícita").path("id").asLong();
        long composicao = enviar(c.localA, base(id) + "/composicoes", composicao(c, c.cargoA.getId(), 5),
                201, "CMP001 composição da publicação").path("id").asLong();
        var publicar = mapa("dataPublicacao", RelogioSistema.hoje(), "referenciaSei", "SEI-PUBLICACAO-CONFIG",
                "justificativa", "Publicação com configurações negociais");
        executarAcao(c, id, "publicar", publicar); em.flush();
        return new NovoContrato(id, composicao, null, inicio, fim);
    }

    private JsonNode executarAcao(Cenario c, long id, String operacao, Map<String, Object> req) throws Exception {
        var previa = enviar(c.localA, base(id) + "/previas/" + operacao, req, 200, "Prévia " + operacao);
        req.put("tokenPrevia", previa.path("tokenPrevia").asText()); req.put("confirmarImpacto", true);
        return enviar(c.localA, base(id) + "/" + operacao, req, 200, "Ato explícito " + operacao);
    }
}
