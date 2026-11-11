package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.vinculo.CategoriaTerminoVinculo;
import com.example.siggtrat.entidade.MotivoDesligamento;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CicloVidaContratosHttpIT extends ContratosHttpBase {
    @org.springframework.beans.factory.annotation.Autowired
    com.example.siggtrat.servico.GestaoContratoTemporalServico gestaoTemporal;
    @Test
    void previaListaImpactosERejeitaConfirmacaoQuandoNovaFrequenciaAparece() throws Exception {
        var c = criarCenario();
        var req = mapa("dataEfetiva", DATA.minusDays(1), "motivo", "Inativação retroativa documentada");
        var p = previa(c, c.contratoA.getId(), "inativar", req);
        assertThat(p.path("vinculosAfetados").size()).isEqualTo(1);
        assertThat(p.path("vinculosAfetados").get(0).path("vinculoId").asLong()).isEqualTo(c.vinculoA.getId());
        assertThat(p.path("vinculosAfetados").get(0).path("pessoaNome").asText()).isEqualTo(c.pessoaA.getNome());
        assertThat(p.path("quantidadeFrequenciasAfetadas").asLong()).isZero();
        String tokenAnterior = p.path("tokenPrevia").asText();
        frequencia(c, false, DATA, "FALTA"); em.flush();
        enviar(c.localA, base(c.contratoA.getId()) + "/inativar", req, 409, "AUD-004 nova frequência torna a prévia desatualizada");
        p = previa(c, c.contratoA.getId(), "inativar", req);
        assertThat(p.path("tokenPrevia").asText()).isNotEqualTo(tokenAnterior);
        assertThat(p.path("quantidadeFrequenciasAfetadas").asLong()).isEqualTo(1);
        var inativo = enviar(c.localA, base(c.contratoA.getId()) + "/inativar", req, 200, "CON-010 inativação após nova confirmação");
        assertThat(inativo.path("habilitacaoAdministrativa").asText()).isEqualTo("INATIVO");
        assertThat(c.vinculoA.getDataFim()).isNull();
    }

    @Test
    void inativacaoEReativacaoSaoEventosSeparadosSemNovoVinculo() throws Exception {
        var c = criarCenario();
        var inativar = mapa("dataEfetiva", DATA, "motivo", "Suspensão administrativa");
        previa(c, c.contratoA.getId(), "inativar", inativar);
        var inativo=enviar(c.localA, base(c.contratoA.getId()) + "/inativar", inativar, 200, "CON-010 inativação explícita");
        assertThat(inativo.path("id").asLong()).isEqualTo(c.contratoA.getId());
        assertThat(inativo.path("dataInicio").asText()).isEqualTo("2026-01-01");
        assertThat(inativo.path("dataFim").asText()).isEqualTo("2027-12-31");
        var reativar = mapa("dataEfetiva", DATA.plusDays(2), "motivo", "Retomada administrativa documentada");
        // ALO008: injeta somente referência a composição de outro Contrato.
        // É contraprova de integridade por JPA, não fluxo legítimo de cadastro.
        c.alocacaoA.setComposicao(c.composicaoB); em.flush();
        previa(c, c.contratoA.getId(), "reativar", reativar);
        long eventosAntes = em.createQuery("select count(e) from EventoCicloVidaContrato e where e.contrato.id=:c",Long.class)
                .setParameter("c",c.contratoA.getId()).getSingleResult();
        var erro = enviar(c.localA, base(c.contratoA.getId()) + "/reativar", reativar, 400,
                "ALO008 composição alheia impede retomada antes de registrar evento");
        assertThat(erro.path("message").asText()).containsIgnoringCase("composição");
        assertThat(em.createQuery("select count(e) from EventoCicloVidaContrato e where e.contrato.id=:c",Long.class)
                .setParameter("c",c.contratoA.getId()).getSingleResult()).isEqualTo(eventosAntes);
        assertThat(c.vinculoA.getDataFim()).isNull();
        assertThat(c.alocacaoA.getComposicao().getId()).isEqualTo(c.composicaoB.getId());
        assertThat(consultar(c.localA,base(c.contratoA.getId())+"/posicao-temporal?data="+DATA.plusDays(2),
                "ALO008 recusa preserva habilitação inativa").path("habilitacaoAdministrativa").asText()).isEqualTo("INATIVO");
        c.alocacaoA.setComposicao(c.composicaoA); em.flush();
        previa(c, c.contratoA.getId(), "reativar", reativar);
        enviar(c.localA, base(c.contratoA.getId()) + "/reativar", reativar, 200, "CON-010 reativação é novo evento");
        var noBloqueio = consultar(c.localA, base(c.contratoA.getId()) + "/posicao-temporal?data=" + DATA.plusDays(1), "CON-022 habilitação distinta da vigência");
        assertThat(noBloqueio.path("habilitacaoAdministrativa").asText()).isEqualTo("INATIVO");
        assertThat(noBloqueio.path("situacaoTemporal").asText()).isEqualTo("VIGENTE");
        var depois = consultar(c.localA, base(c.contratoA.getId()) + "/posicao-temporal?data=" + DATA.plusDays(2), "CON-010 intervalo preservado");
        assertThat(depois.path("habilitacaoAdministrativa").asText()).isEqualTo("ATIVO");
        assertThat(c.vinculoA.getDataFim()).isNull();
        assertThat(em.createQuery("select count(v) from VinculoContratual v where v.contrato.id=:id", Long.class)
                .setParameter("id", c.contratoA.getId()).getSingleResult()).isEqualTo(1L);
    }

    @Test
    void encerramentoComPreviaTerminaVinculoNaDataEfetivaComCausaPropria() throws Exception {
        var c = criarCenario();
        declararFonteNominalSintetica(c, "CICLO-RESCISAO-COM-CAUSA");
        motivo(c, CategoriaTerminoVinculo.ENCERRAMENTO_ANTECIPADO_CONTRATO);
        long motivoId = em.createQuery("select m.id from MotivoDesligamento m where m.gestaoLocal.id=:gestao and m.categoria=:categoria", Long.class)
                .setParameter("gestao", c.gestaoA.getId())
                .setParameter("categoria", CategoriaTerminoVinculo.ENCERRAMENTO_ANTECIPADO_CONTRATO).getSingleResult();
        FonteMotivoSintetica.declarar(em, gestaoTemporal, c.contratoA.getId(), motivoId,
                c.gestaoA.getId(), c.localA, "Motivo ENCERRAMENTO_ANTECIPADO_CONTRATO",
                CategoriaTerminoVinculo.ENCERRAMENTO_ANTECIPADO_CONTRATO, DATA, DATA,
                java.time.Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS), "SEI-SINTETICO-CICLO-RESCISAO-COM-CAUSA");
        var req = mapa("dataEfetiva", DATA, "dataAto", DATA.minusDays(2), "motivo", "Rescisão formal", "referenciaSei", "SEI-RESCISAO");
        previa(c, c.contratoA.getId(), "encerrar-antecipadamente", req);
        var fim = enviar(c.localA, base(c.contratoA.getId()) + "/encerrar-antecipadamente", req, 200, "CON-016 encerramento com impacto confirmado");
        assertThat(fim.path("dataFim").asText()).isEqualTo(DATA.toString());
        em.flush(); em.refresh(c.vinculoA);
        assertThat(c.vinculoA.getDataFim()).isEqualTo(DATA);
        assertThat(c.vinculoA.getMotivoDesligamento().getCategoria()).isEqualTo(CategoriaTerminoVinculo.ENCERRAMENTO_ANTECIPADO_CONTRATO);
        var pos = consultar(c.localA, base(c.contratoA.getId()) + "/posicao-temporal?data=" + DATA.plusDays(1), "CON-016 dia seguinte ao fim fica encerrado");
        assertThat(pos.path("situacaoTemporal").asText()).isEqualTo("ENCERRADO");
    }

    @Test
    void anulacaoECorrecaoPreservamAlcanceAnteriorSemReativacaoSilenciosa() throws Exception {
        var c = criarCenario();
        declararFonteNominalSintetica(c, "CICLO-ANULACAO-E-CORRECAO");
        motivo(c, CategoriaTerminoVinculo.ANULACAO_CONTRATO);
        long motivoId = em.createQuery("select m.id from MotivoDesligamento m where m.gestaoLocal.id=:gestao and m.categoria=:categoria", Long.class)
                .setParameter("gestao", c.gestaoA.getId())
                .setParameter("categoria", CategoriaTerminoVinculo.ANULACAO_CONTRATO).getSingleResult();
        FonteMotivoSintetica.declarar(em, gestaoTemporal, c.contratoA.getId(), motivoId,
                c.gestaoA.getId(), c.localA, "Motivo ANULACAO_CONTRATO",
                CategoriaTerminoVinculo.ANULACAO_CONTRATO, DATA, DATA,
                java.time.Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS), "SEI-SINTETICO-CICLO-ANULACAO-E-CORRECAO");
        var req = mapa("dataEfetiva", DATA, "dataAto", DATA.plusDays(4), "motivo", "Ato anulatório com alcance específico", "referenciaSei", "SEI-ANULAR");
        previa(c, c.contratoA.getId(), "anular", req);
        var anulacao = enviar(c.localA, base(c.contratoA.getId()) + "/anular", req, 200, "CON-015/018/024 anulação com alcance documental");
        assertThat(anulacao.path("estadoNegocial").asText()).isEqualTo("ANULADO");
        JsonNode anulacaoOriginal=consultar(c.localA,base(c.contratoA.getId())+"/eventos","CON019 fonte original da anulação").get(0).deepCopy();
        long eventoId = anulacaoOriginal.path("id").asLong();
        assertThat(anulacaoOriginal.path("tipoEvento").asText()).isEqualTo("ANULACAO");
        var cor = mapa("dataEfetiva", DATA, "dataAto", DATA.plusDays(5), "motivo", "Anulação registrada indevidamente",
                "referenciaSei", "SEI-COR-ANU", "eventoPrecedenteId", eventoId, "relacaoPrecedencia", "CANCELA");
        previa(c, c.contratoA.getId(), "corrigir-anulacao", cor);
        var corrigido = enviar(c.localA, base(c.contratoA.getId()) + "/corrigir-anulacao", cor, 200, "CON-020 correção por novo evento");
        assertThat(corrigido.path("estadoNegocial").asText()).isEqualTo("PUBLICADO");
        assertThat(corrigido.path("habilitacaoAdministrativa").asText()).isEqualTo("INATIVO");
        var fatos=consultar(c.localA,base(c.contratoA.getId())+"/eventos","CON019 preserva os dois fatos por ID e tipo");
        assertThat(fatos).hasSize(2);
        JsonNode original=null,correcao=null;
        for(JsonNode fato:fatos) {
            if(fato.path("id").asLong()==eventoId) original=fato;
            if(fato.path("tipoEvento").asText().equals("CORRECAO_ANULACAO")) correcao=fato;
        }
        assertThat(original).isNotNull();assertThat(correcao).isNotNull();
        assertThat(original.path("tipoEvento").asText()).isEqualTo("ANULACAO");
        assertThat(correcao.path("id").asLong()).isPositive().isNotEqualTo(eventoId);
        assertThat(correcao.path("tipoEvento").asText()).isEqualTo("CORRECAO_ANULACAO");
        assertThat(correcao.path("dataEfetiva").asText()).isEqualTo(DATA.toString());
        for(String campo:java.util.List.of("id","tipoEvento","dataEfetiva","dataAto","registradoEm","motivo","referenciaSei",
                "usuarioResponsavelId","usuarioResponsavelUsername"))
            assertThat(original.path(campo)).as("CON019 fato original preserva "+campo).isEqualTo(anulacaoOriginal.path(campo));
    }

    @Test
    void cancelarContratoFuturoMantemIdentificadorReservado() throws Exception {
        var c = criarCenario(); var n = publicado(c, 100, null, true);
        var req = mapa("dataEfetiva", RelogioSistema.hoje(), "motivo", "Efeitos futuros oficialmente cancelados", "referenciaSei", "SEI-CANCEL");
        previa(c, n.id(), "cancelar-futuro", req);
        var cancelado = enviar(c.localA, base(n.id()) + "/cancelar-futuro", req, 200, "CON-021 contrato futuro válido assume CANCELADO");
        assertThat(cancelado.path("situacaoTemporal").asText()).isEqualTo("CANCELADO");
        var body = rascunho(c, cancelado.path("numeroContrato").asText(), 100, n.inicio(), n.fim());
        enviar(c.localA, "/api/contratos", body, 409, "CON-025 publicado cancelado mantém identificador reservado");
    }

    private JsonNode previa(Cenario c, long id, String operacao, Map<String, Object> req) throws Exception {
        var p = enviar(c.localA, base(id) + "/previas/" + operacao, req, 200, "AUD-004 prévia concreta " + operacao);
        req.put("tokenPrevia", p.path("tokenPrevia").asText()); req.put("confirmarImpacto", true); return p;
    }

    private void motivo(Cenario c, CategoriaTerminoVinculo categoria) {
        var m = new MotivoDesligamento(); m.setGestaoLocal(c.gestaoA); m.setCategoria(categoria);
        m.setDescricao("Motivo " + categoria.name()); m.setCriadoPorId(c.localA.getId()); m.setCriadoPorUsername(c.localA.getUsername());
        em.persist(m); em.flush();
        var origem = new com.example.siggtrat.entidade.EventoDisponibilidadeCatalogo();
        origem.setTipoCatalogo(com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal.MOTIVO_DESLIGAMENTO);
        origem.setCatalogoId(m.getId()); origem.setDataEfetiva(INICIO); origem.setAtivo(true);
        origem.setMotivo("Fonte sintética explícita da causa contratual desde o início do cenário");
        em.persist(origem); em.flush();
    }
}
