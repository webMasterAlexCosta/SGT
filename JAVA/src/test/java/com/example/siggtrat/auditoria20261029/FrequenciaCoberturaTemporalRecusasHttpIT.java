package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.AlocacaoVinculo;
import com.example.siggtrat.entidade.Frequencia;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** ROP027/028: cobertura na data; cancelamento não é sinônimo de ausência. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@TestPropertySource(properties = "frequencia.janela-operacional-dias=30")
class FrequenciaCoberturaTemporalRecusasHttpIT extends MestreConcorrenciaSuporte {
    private static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");

    @Test
    void fimNaturalIncluiUltimoDiaERecusaDiaPosteriorPassadoSemApagarFrequenciaCoberta() throws Exception {
        Cenario c = criarCenarioPersistido();
        LocalDate fim = RelogioSistema.hoje().minusDays(3).with(TemporalAdjusters.previousOrSame(DayOfWeek.THURSDAY));
        var contrato = publicarComDatas(c, fim.minusDays(10), fim);
        long vinculo = vinculo(c, contrato, 1);
        var controle = postar(c.localA, "/api/frequencias", frequencia(vinculo, fim), 200, "ultimo-dia-contratual-incluido");
        var posicaoAntes = consultar(c.localA, base(contrato.id()) + "/posicao-temporal?data=" + fim, "Vigente no último dia");
        var posicaoDepois = consultar(c.localA, base(contrato.id()) + "/posicao-temporal?data=" + fim.plusDays(1), "Encerrado no dia seguinte");
        var pedido = autenticar(post("/api/frequencias").contentType(APPLICATION_JSON).content(json(frequencia(vinculo, fim.plusDays(1)))), c.localA);
        var antes = fotografia();
        var negativa = mvc.perform(pedido).andReturn();
        var depois = fotografia();
        registrar("frequencia-fim-natural", mapa("fim", fim, "dataRecusada", fim.plusDays(1),
                "controle", SnapshotQuantitativos.http(mapper, controle), "recusa", SnapshotQuantitativos.http(mapper, negativa),
                "posicaoUltimoDiaSha", hJson(posicaoAntes), "posicaoSeguinteSha", hJson(posicaoDepois), "antes", antes, "depois", depois));
        assertAll(() -> assertThat(fim.plusDays(1)).isBefore(RelogioSistema.hoje()),
                () -> assertThat(posicaoAntes.path("situacaoTemporal").asText()).isEqualTo("VIGENTE"),
                () -> assertThat(posicaoDepois.path("situacaoTemporal").asText()).isEqualTo("ENCERRADO"),
                () -> assertThat(negativa.getResponse().getStatus()).isEqualTo(409),
                () -> assertThat(dados(negativa).path("code").asText()).isEqualTo("HTTP_409"),
                () -> assertThat(dados(negativa).path("message").asText()).containsIgnoringCase("operacional"),
                () -> assertThat(depois).isEqualTo(antes));
    }

    @Test
    void contratoCanceladoRecusaQuandoDataAlcancaInicioEControleEquivalenteConfirma() throws Exception {
        Cenario c = criarCenarioPersistido();
        LocalDate hoje = RelogioSistema.hoje(), inicio = hoje.plusDays(3).with(TemporalAdjusters.nextOrSame(DayOfWeek.WEDNESDAY));
        var cancelado = publicarComDatas(c, inicio, inicio.plusDays(30));
        var controle = publicarComDatas(c, inicio, inicio.plusDays(30));
        long vinculoCancelado = vinculo(c, cancelado, 1), vinculoControle = vinculo(c, controle, 1);
        var acao = mapa("dataEfetiva", hoje, "motivo", "Cancelamento formal da programação sintética", "referenciaSei", "SEI-TMP-CANCELAMENTO");
        confirmarAcao(c, cancelado.id(), "cancelar-futuro", acao);
        var pedidoNegativo = autenticar(post("/api/frequencias").contentType(APPLICATION_JSON).content(json(frequencia(vinculoCancelado, inicio))), c.localA);
        var pedidoControle = autenticar(post("/api/frequencias").contentType(APPLICATION_JSON).content(json(frequencia(vinculoControle, inicio))), c.localA);
        var antes = fotografia();
        org.springframework.test.web.servlet.MvcResult negativa, positiva;
        Map<String, Object> aposNegativa;
        try (MockedStatic<LocalDate> relogio = mockStatic(LocalDate.class, CALLS_REAL_METHODS)) {
            relogio.when(() -> LocalDate.now(ZONA)).thenReturn(inicio);
            negativa = mvc.perform(pedidoNegativo).andReturn();
            aposNegativa = fotografia();
            positiva = mvc.perform(pedidoControle).andReturn();
        }
        registrar("frequencia-contrato-cancelado", mapa("relogio", "Somente LocalDate.now(SaoPaulo) na thread; sem passagem física do dia",
                "dataSemantica", inicio, "negativa", SnapshotQuantitativos.http(mapper, negativa),
                "controle", SnapshotQuantitativos.http(mapper, positiva), "antes", antes, "aposNegativa", aposNegativa));
        assertAll(() -> assertThat(negativa.getResponse().getStatus()).isEqualTo(409),
                () -> assertThat(dados(negativa).path("code").asText()).isEqualTo("HTTP_409"),
                () -> assertThat(aposNegativa).isEqualTo(antes),
                () -> assertThat(positiva.getResponse().getStatus()).isEqualTo(200),
                () -> assertThat(dados(positiva).path("vinculoContratualId").asLong()).isEqualTo(vinculoControle));
    }

    @Test
    void cancelarMovimentoRestauraFaixaValidaEAlocacaoAlheiaNaoPodeSerCanceladaPeloOutroVinculo() throws Exception {
        Cenario c = criarCenarioPersistido();
        LocalDate hoje = RelogioSistema.hoje(), data = hoje.plusDays(3).with(TemporalAdjusters.nextOrSame(DayOfWeek.WEDNESDAY));
        var contrato = rascunhoPublicado(c, List.of(5, 5), hoje, null, true);
        long vinculo = vinculo(c, contrato, 1), outro = vinculo(c, contrato, 2);
        long original = alocacaoInicial(vinculo), alheia = alocacaoInicial(outro);
        String raiz = "/api/vinculos-contratuais/" + vinculo;
        var movimento = mapa("dataEfetiva", data, "composicaoId", contrato.composicoes().get(1),
                "justificativa", "Movimento programado sintético", "evidencia", "SEI-MOVIMENTO-SINTETICO", "autorizacao", "ATO-SINTETICO");
        var previa = dados(postar(c.localA, raiz + "/previas/alocacoes", movimento, 200, "previa-movimento"));
        movimento.put("tokenPrevia", previa.path("token").asText()); movimento.put("confirmarImpacto", true);
        long programada = dados(postar(c.localA, raiz + "/alocacoes", movimento, 200, "movimento-programado")).path("id").asLong();
        var cancelar = mapa("justificativa", "Programação desnecessária; restaurar origem", "evidencia", "SEI-CANCELAMENTO-SINTETICO", "autorizacao", "ATO-SINTETICO");
        postar(c.localA, raiz + "/alocacoes/" + programada + "/cancelar-programacao", cancelar, 200, "cancelamento-restaura-faixa");
        var pedidoAlheio = autenticar(post(raiz + "/alocacoes/" + alheia + "/cancelar-programacao").contentType(APPLICATION_JSON).content(json(cancelar)), c.localA);
        var antes = fotografia();
        var recusa = mvc.perform(pedidoAlheio).andReturn();
        var depois = fotografia();
        var pedidoFrequencia = autenticar(post("/api/frequencias").contentType(APPLICATION_JSON).content(json(frequencia(vinculo, data))), c.localA);
        org.springframework.test.web.servlet.MvcResult positiva;
        try (MockedStatic<LocalDate> relogio = mockStatic(LocalDate.class, CALLS_REAL_METHODS)) {
            relogio.when(() -> LocalDate.now(ZONA)).thenReturn(data);
            positiva = mvc.perform(pedidoFrequencia).andReturn();
        }
        assertThat(positiva.getResponse().getStatus()).isEqualTo(200);
        long frequenciaId = dados(positiva).path("id").asLong();
        long efetiva = emTransacao(() -> em.find(Frequencia.class, frequenciaId).getAlocacaoVinculo().getId());
        String estadoCancelada = emTransacao(() -> em.find(AlocacaoVinculo.class, programada).getEstadoNegocial().name());
        registrar("frequencia-faixa-restaurada", mapa("origemId", original, "programadaCanceladaId", programada,
                "alheiaId", alheia, "alocacaoPersistidaNaFrequencia", efetiva, "estadoProgramada", estadoCancelada,
                "recusaAlheia", SnapshotQuantitativos.http(mapper, recusa), "antesRecusa", antes, "depoisRecusa", depois,
                "frequencia", SnapshotQuantitativos.http(mapper, positiva), "relogioSemantico", data));
        assertAll(() -> assertThat(recusa.getResponse().getStatus()).isEqualTo(400),
                () -> assertThat(depois).isEqualTo(antes),
                () -> assertThat(efetiva).isEqualTo(original),
                () -> assertThat(estadoCancelada).isEqualTo("CANCELADA"));
    }

    private BaseContrato publicarComDatas(Cenario c, LocalDate inicio, LocalDate fim) throws Exception {
        long id = dados(postar(c.localA, "/api/contratos", rascunho(c, "TMP-" + UUID.randomUUID().toString().substring(0, 8), 5, inicio, fim), 201, "contrato-temporal")).path("id").asLong();
        long composicao = dados(postar(c.localA, base(id) + "/composicoes", composicao(c, c.cargoA.getId(), 5), 201, "composicao-temporal")).path("id").asLong();
        Map<String, Object> req = publicacao();
        if (inicio.isBefore(RelogioSistema.hoje())) {
            JsonNode previa = dados(postar(c.localA, base(id) + "/previas/publicar", req, 200, "previa-publicacao-historica"));
            req.put("tokenPrevia", previa.path("tokenPrevia").asText()); req.put("confirmarImpacto", true);
        }
        postar(c.localA, base(id) + "/publicar", req, 200, "publicacao-confirmada");
        return new BaseContrato(id, List.of(composicao), inicio, fim);
    }

    private long vinculo(Cenario c, BaseContrato n, int variante) throws Exception {
        return dados(postar(c.localA, "/api/vinculos-contratuais", mapa("contratoId", n.id(), "composicaoId", n.composicoes().get(0),
                "dataInicio", n.inicio(), "pessoa", mapa("nome", "Pessoa sintética temporal " + variante,
                        "cpf", cpf(Math.toIntExact(830_000_000L + n.id() * 100 + variante)))), 201, "vinculo-temporal")).path("id").asLong();
    }

    private void confirmarAcao(Cenario c, long id, String acao, Map<String, Object> req) throws Exception {
        JsonNode previa = dados(postar(c.localA, base(id) + "/previas/" + acao, req, 200, "previa-" + acao));
        req.put("tokenPrevia", previa.path("tokenPrevia").asText()); req.put("confirmarImpacto", true);
        postar(c.localA, base(id) + "/" + acao, req, 200, "confirmar-" + acao);
    }

    private Map<String, Object> frequencia(long vinculo, LocalDate dia) {
        return mapa("vinculoContratualId", vinculo, "dataReferencia", dia, "status", "FALTA",
                "justificativaRetificacao", "Prova temporal com fonte conhecida", "chaveIdempotencia", "TEMPORAL-" + UUID.randomUUID());
    }

    private long alocacaoInicial(long vinculo) {
        return emTransacao(() -> em.createQuery("select a.id from AlocacaoVinculo a where a.vinculo.id=:id order by a.id", Long.class)
                .setParameter("id", vinculo).getResultList().get(0));
    }

    private Map<String, Object> fotografia() {
        descarregarAuditoria();
        var jdbc = new JdbcTemplate(dataSource);
        Map<String, Object> out = new LinkedHashMap<>();
        for (String tabela : List.of("contrato", "contrato_versao_negocial", "contrato_evento_ciclo_vida", "pessoa", "vinculo_contratual",
                "alocacao_vinculo", "alocacao_vinculo_evento", "frequencia", "evento_frequencia", "historico_empregado")) {
            out.put(tabela, jdbc.queryForList("select to_jsonb(t)::text from " + tabela + " t order by 1", String.class).stream().map(this::h).toList());
        }
        return Map.copyOf(out);
    }
}
