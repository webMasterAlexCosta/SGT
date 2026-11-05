package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.VinculoContratual;

import com.example.siggtrat.entidade.EventoVinculoContratual;

import com.example.siggtrat.dominio.vinculo.CategoriaTerminoVinculo;
import com.example.siggtrat.entidade.MotivoDesligamento;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** FRE-015/018/019: prova preservada nao restaura escopo superado por transferencia retroativa. */
class FrequenciaComplementosEscopoAposPerdaCoberturaHttpIT extends ContratosHttpBase {
    @org.springframework.beans.factory.annotation.Autowired
    private com.example.siggtrat.servico.GestaoContratoTemporalServico gestaoDaFonte;

    @ParameterizedTest
    @ValueSource(strings = {"excecoes", "adicionais"})
    void anulacaoNaoRestauraGestaoDaFotografiaSuperadaPorTransferenciaRetroativa(String recurso) throws Exception {
        Cenario c = criarCenario();
        // Este cenário conhece a admissão original; a fixture JPA genérica não
        // fornece fonte nominal e deve continuar servindo aos controles de ausência.
        String nomeDocumentado = c.pessoaA.getNome();
        declararFonteNominalSintetica(c, "COMPLEMENTOS-ANULACAO-ESCOPO");
        emTransacao(() -> {
            em.flush(); em.clear();
            VinculoContratual vinculo = em.find(VinculoContratual.class, c.vinculoA.getId());
            assertThat(vinculo.getDataInicio()).isEqualTo(INICIO);
            assertThat(vinculo.getDataFim()).isNull();
            assertThat(DATA).isAfter(INICIO).isBefore(com.example.siggtrat.utilitario.RelogioSistema.hoje());
            var fontes = em.createQuery("select e from EventoVinculoContratual e where e.vinculo.id=:v "
                            + "and e.dataEfetiva<=:dia order by e.registradoEm,e.id", EventoVinculoContratual.class)
                    .setParameter("v", vinculo.getId()).setParameter("dia", DATA.minusDays(1)).getResultList();
            assertThat(fontes).hasSize(1);
            EventoVinculoContratual fonte = fontes.get(0);
            assertThat(fonte.getTipoEvento()).isEqualTo(com.example.siggtrat.dominio.vinculo.TipoEventoVinculoContratual.CRIACAO);
            assertThat(fonte.getDataEfetiva()).isEqualTo(INICIO);
            assertThat(fonte.getRegistradoEm()).isEqualTo(INICIO.atStartOfDay(java.time.ZoneOffset.UTC).toInstant());
            assertThat(fonte.getPessoaNomeFotografia()).isEqualTo(nomeDocumentado);
            assertThat(fonte.getPessoaCpfFotografia()).isEqualTo(vinculo.getPessoa().getCpf());
            assertThat(fonte.getInicioNovo()).isEqualTo(vinculo.getDataInicio());
            assertThat(fonte.getGestaoLocalIdFotografia()).isEqualTo(c.gestaoA.getId());
            assertThat(fonte.getResponsavelId()).isEqualTo(c.localA.getId());
            assertThat(fonte.getEvidencia()).isEqualTo("SEI-FIXTURE-COMPLEMENTOS-ANULACAO-ESCOPO");
            assertThat(fonte.getAutorizacao()).isEqualTo("ATO-FIXTURE-COMPLEMENTOS-ANULACAO-ESCOPO");
            assertThat(fonte.getHashEvento()).isNotBlank();
            return null;
        });
        String rota = "/api/frequencias-" + recurso;
        Map<String, Object> corpo = recurso.equals("excecoes")
                ? mapa("empregadoId", c.empregadoA.getId(), "dataInicio", DATA, "dataFim", DATA,
                        "tipo", "FOLGA_REMUNERADA", "observacao", "Fonte anterior ao conhecimento da transferencia")
                : mapa("empregadoId", c.empregadoA.getId(), "dataReferencia", DATA, "tipo", "RT", "minutos", 60,
                        "quantidade", 1, "justificativa", "Fonte anterior ao conhecimento da transferencia");
        long id = enviar(c.localA, rota, corpo, 201, "Fonte publicada com o contexto A conhecido originalmente")
                .path("id").asLong();
        JsonNode provaOriginal = consultar(c.localA, rota + "/" + id + "/eventos", "Fotografia original comprovada em A");
        assertEquals(c.gestaoA.getId().longValue(), provaOriginal.get(0).path("contextos").get(0).path("gestaoLocalId").asLong());

        conceder(c, c.contratoA.getId(), "TRANSFERENCIA_GESTAO_LOCAL");
        enviar(c.localA, base(c.contratoA.getId()) + "/transferir-gestao", mapa(
                "dataEfetiva", DATA.minusDays(1), "dataAto", RelogioSistema.hoje(),
                "gestaoLocalDestinoId", c.gestaoB.getId(), "tipoContratacaoDestinoId", c.tipoB.getId(),
                "motivo", "Transferencia retroativa corrige a responsabilidade do dia da fonte",
                "referenciaSei", "SEI-COMPLEMENTO-ESCOPO-CORRIGIDO", "autorizacaoAdministrativa", "ATO-HTTP",
                "mapeamentos", List.of(mapa("composicaoOrigemId", c.composicaoA.getId(),
                        "lotacaoDestinoId", c.lotacaoB.getId(), "jornadaDestinoId", c.jornadaB.getId(),
                        "cargoDestinoId", c.cargoB.getId()))),
                200, "Transferencia real faz B responsavel efetiva por DATA, sem reescrever a fonte anterior");
        assertEquals(0, listar(c, rota, false).path("content").size(),
                "Pre-condicao: a responsabilidade atual reconstruida ja retirou o dia da Gestao A");
        assertEquals(id, listar(c, rota, true).path("content").get(0).path("id").asLong(),
                "Pre-condicao: B consulta o mesmo fato pelo contexto temporal corrigido");

        MotivoDesligamento motivo = new MotivoDesligamento();
        motivo.setGestaoLocal(c.gestaoB); motivo.setDescricao("Anulacao do periodo transferido com prova preservada");
        motivo.setCategoria(CategoriaTerminoVinculo.ANULACAO_CONTRATO); em.persist(motivo); em.flush();
        var origemCatalogo = new com.example.siggtrat.entidade.EventoDisponibilidadeCatalogo();
        origemCatalogo.setTipoCatalogo(com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal.MOTIVO_DESLIGAMENTO);
        origemCatalogo.setCatalogoId(motivo.getId()); origemCatalogo.setDataEfetiva(INICIO); origemCatalogo.setAtivo(true);
        origemCatalogo.setMotivo("Fonte sintética explícita da causa de anulação na Gestão de destino");
        em.persist(origemCatalogo); em.flush();
        // A transferência já documentou B em DATA; a fonte não atravessa a Gestão de origem.
        FonteMotivoSintetica.declarar(em, gestaoDaFonte, c.contratoA.getId(), motivo.getId(),
                c.gestaoB.getId(), c.localB,
                "Anulacao do periodo transferido com prova preservada",
                CategoriaTerminoVinculo.ANULACAO_CONTRATO, DATA, DATA,
                com.example.siggtrat.utilitario.PrecisaoTemporal.agoraMicros(),
                "SEI-SINTETICO-COMPLEMENTOS-ANULACAO-ESCOPO-B");
        Map<String, Object> anulacao = mapa("dataEfetiva", DATA, "dataAto", RelogioSistema.hoje(),
                "motivo", "Anulacao retira cobertura operacional sem desfazer a responsabilidade historica",
                "referenciaSei", "SEI-COMPLEMENTO-ANULACAO");
        JsonNode previa = enviar(c.localB, base(c.contratoA.getId()) + "/previas/anular", anulacao, 200,
                "Previa real antes da retirada da cobertura na data da fonte");
        anulacao.put("tokenPrevia", previa.path("tokenPrevia").asText()); anulacao.put("confirmarImpacto", true);
        enviar(c.localB, base(c.contratoA.getId()) + "/anular", anulacao, 200,
                "Anulacao preserva fontes e transferencia ja documentadas");
        em.flush(); em.clear();

        JsonNode origem = listar(c, rota, false);
        JsonNode destino = listar(c, rota, true);
        assertAll(
                () -> assertEquals(0, origem.path("content").size(),
                        "Fallback de fotografia antiga nao pode devolver a A a responsabilidade ja corrigida para B"),
                () -> assertEquals(1, destino.path("content").size(),
                        "Perda de validade operacional conserva leitura da prova pela Gestao B responsavel"));
        assertEquals(id, destino.path("content").get(0).path("id").asLong());
        executar(c.localA, get(rota + "/{id}/eventos", id), 404,
                "Historico tambem aplica a responsabilidade corrigida, sem conceder acesso pela foto antiga");
        JsonNode historico = consultar(c.localB, rota + "/" + id + "/eventos",
                "Gestao B consulta a prova preservada mesmo apos a anulacao operacional");
        assertEquals(provaOriginal, historico,
                "Autorizar pela responsabilidade efetiva nao altera foto, contexto original, autoria ou hash da fonte");
    }

    private JsonNode listar(Cenario c, String rota, boolean destino) throws Exception {
        return dados(executar(destino ? c.localB : c.localA, get(rota).param("data", DATA.toString())
                        .param("contratoId", c.contratoA.getId().toString()), 200,
                "Consulta temporal da fonte preservada no contrato transferido"));
    }
}
