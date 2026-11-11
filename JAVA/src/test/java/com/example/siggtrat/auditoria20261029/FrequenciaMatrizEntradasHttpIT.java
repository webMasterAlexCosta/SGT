package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.VinculoContratual;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** ROP021/026: negativos distintos, commits reais e contraprovas nos limites. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@TestPropertySource(properties = "frequencia.janela-operacional-dias=30")
class FrequenciaMatrizEntradasHttpIT extends MestreConcorrenciaSuporte {
    @Test
    void justificativaEChaveInvalidasNaoCriamVersaoOuEventoELimitesValidosConfirmam() throws Exception {
        Cenario c = criarCenarioPersistido();
        LocalDate dia = DataJornadaSintetica.ultimoDiaDeTrabalhoAnterior(c.jornadaA);
        long id = postarFrequencia(c, entrada(c, dia, "FALTA"), "Controle original").path("id").asLong();
        var casos = List.of("MOTIVO_AUSENTE", "MOTIVO_QUATRO", "CHAVE_AUSENTE", "CHAVE_SETE", "CHAVE_121");
        for (String caso : casos) {
            Map<String, Object> body = entrada(c, dia, "PRESENCA");
            switch (caso) {
                case "MOTIVO_AUSENTE" -> body.remove("justificativaRetificacao");
                case "MOTIVO_QUATRO" -> body.put("justificativaRetificacao", "   abcd   ");
                case "CHAVE_AUSENTE" -> body.remove("chaveIdempotencia");
                case "CHAVE_SETE" -> body.put("chaveIdempotencia", "   1234567   ");
                case "CHAVE_121" -> body.put("chaveIdempotencia", "k".repeat(121));
                default -> throw new AssertionError(caso);
            }
            var pedido = autenticar(post("/api/frequencias").contentType(APPLICATION_JSON).content(json(body)), c.localA);
            var antes = fotografia();
            var resposta = mvc.perform(pedido).andReturn();
            var depois = fotografia();
            JsonNode erro = dados(resposta);
            registrar("frequencia-entrada-" + caso.toLowerCase(java.util.Locale.ROOT).replace('_', '-'), mapa("antes", antes, "depois", depois,
                    "resposta", SnapshotQuantitativos.http(mapper, resposta)));
            assertAll(() -> assertThat(resposta.getResponse().getStatus()).isEqualTo(400),
                    () -> assertThat(erro.path("code").asText()).isEqualTo("HTTP_400"),
                    () -> assertThat(erro.path("message").asText()).containsIgnoringCase(caso.startsWith("MOTIVO") ? "justificativa" : "chave"),
                    () -> assertThat(depois).as(caso + " preserva todas as linhas/colunas e auditoria negocial").isEqualTo(antes));
        }
        var minimo = entrada(c, dia, "PRESENCA");
        minimo.put("justificativaRetificacao", "  abcde  "); minimo.put("chaveIdempotencia", "  12345678  ");
        JsonNode v2 = postarFrequencia(c, minimo, "Cinco caracteres e chave de oito após trim");
        var maximo = entrada(c, dia, "FALTA");
        maximo.put("justificativaRetificacao", "abcde"); maximo.put("chaveIdempotencia", "k".repeat(120));
        JsonNode v3 = postarFrequencia(c, maximo, "Chave de120 e alteração efetiva");
        var eventos = consultar(c.localA, "/api/frequencias/" + id + "/eventos", "Três intenções válidas confirmadas");
        var integridade = consultar(c.localA, "/api/frequencias/" + id + "/evidencias-integridade", "Cadeia preservada após negativas e positivos");
        registrar("frequencia-limites-validos", mapa("v2Sha", hJson(v2), "v3Sha", hJson(v3), "eventosSha", hJson(eventos), "integridade", integridade));
        assertAll(() -> assertThat(v2.path("id").asLong()).isEqualTo(id),
                () -> assertThat(v2.path("versaoNegocial").asInt()).isEqualTo(2),
                () -> assertThat(v3.path("versaoNegocial").asInt()).isEqualTo(3),
                () -> assertThat(v3.path("status").asText()).isEqualTo("FALTA"),
                () -> assertThat(eventos).hasSize(3),
                () -> assertThat(integridade.path("cadeiaIntegra").asBoolean()).isTrue());
    }

    @Test
    void vinculoOmitidoEPositivoInexistenteNaoFazemFallbackPeloEmpregado() throws Exception {
        Cenario c = criarCenarioPersistido();
        LocalDate dia = DataJornadaSintetica.ultimoDiaDeTrabalhoAnterior(c.jornadaA);
        assertThat(emTransacao(() -> em.find(VinculoContratual.class, Long.MAX_VALUE))).isNull();
        for (boolean omitido : List.of(true, false)) {
            var body = entrada(c, dia, "FALTA");
            body.put("empregado", mapa("id", c.empregadoA.getId()));
            if (omitido) body.remove("vinculoContratualId"); else body.put("vinculoContratualId", Long.MAX_VALUE);
            var pedido = autenticar(post("/api/frequencias").contentType(APPLICATION_JSON).content(json(body)), c.localA);
            var antes = fotografia();
            var resposta = mvc.perform(pedido).andReturn();
            var depois = fotografia();
            registrar("frequencia-vinculo-" + (omitido ? "omitido" : "inexistente-positivo"),
                    mapa("idInexistenteComprovado", !omitido, "empregadoLegitimoInformado", true,
                            "antes", antes, "depois", depois, "resposta", SnapshotQuantitativos.http(mapper, resposta)));
            assertAll(() -> assertThat(resposta.getResponse().getStatus()).isEqualTo(400),
                    () -> assertThat(dados(resposta).path("code").asText()).isEqualTo("HTTP_400"),
                    () -> assertThat(depois).isEqualTo(antes));
        }
        var body = entrada(c, dia, "FALTA"); body.put("empregado", mapa("id", c.empregadoA.getId()));
        var controle = postarFrequencia(c, body, "Vínculo canônico válido com o mesmo Empregado");
        var eventos = consultar(c.localA, "/api/frequencias/" + controle.path("id").asLong() + "/eventos", "Só a intenção válida cria evento");
        assertThat(eventos).hasSize(1);
    }

    @Test
    void atestadoSemObservacaoNoFechamentoRecusaAntesDeCriarJobOuFatos() throws Exception {
        Cenario c = criarCenarioPersistido();
        LocalDate dia = DataJornadaSintetica.ultimoDiaDeTrabalhoAnterior(c.jornadaA);
        var body = mapa("dataReferencia", dia, "lotacaoId", c.lotacaoA.getId(), "jornadaId", c.jornadaA.getId(),
                "contratoIds", List.of(c.contratoA.getId()), "justificativaRetificacao", "Justificativa gerencial coletiva válida",
                "chaveIdempotencia", "ATESTADO-NEGATIVO-" + UUID.randomUUID(), "alteracoes", List.of(mapa(
                        "empregado", mapa("id", c.empregadoA.getId()), "vinculoContratualId", c.vinculoA.getId(),
                        "status", "ATESTADO", "observacao", "   ")));
        var pedido = autenticar(post("/api/frequencias/fechamentos").contentType(APPLICATION_JSON).content(json(body)), c.localA);
        var antes = fotografia();
        var resposta = mvc.perform(pedido).andReturn();
        var depois = fotografia();
        registrar("fechamento-atestado-sem-justificativa", mapa("antes", antes, "depois", depois,
                "resposta", SnapshotQuantitativos.http(mapper, resposta)));
        assertAll(() -> assertThat(resposta.getResponse().getStatus()).isEqualTo(400),
                () -> assertThat(dados(resposta).path("code").asText()).isEqualTo("HTTP_400"),
                () -> assertThat(dados(resposta).path("message").asText()).contains("Atestado exige justificativa"),
                () -> assertThat(depois).isEqualTo(antes));
    }

    private Map<String, Object> entrada(Cenario c, LocalDate dia, String status) {
        return mapa("vinculoContratualId", c.vinculoA.getId(), "dataReferencia", dia, "status", status,
                "justificativaRetificacao", "Motivo válido documentado", "chaveIdempotencia", "ENTRADA-" + UUID.randomUUID());
    }

    private JsonNode postarFrequencia(Cenario c, Map<String, Object> body, String etapa) throws Exception {
        return dados(executar(c.localA, post("/api/frequencias").contentType(APPLICATION_JSON).content(json(body)), 200, etapa));
    }

    private Map<String, Object> fotografia() {
        descarregarAuditoria();
        var jdbc = new JdbcTemplate(dataSource);
        Map<String, Object> out = new LinkedHashMap<>();
        for (String tabela : List.of("contrato", "pessoa", "vinculo_contratual", "alocacao_vinculo", "frequencia", "evento_frequencia",
                "historico_empregado", "ocorrencia", "contexto_ocorrencia", "anexo_ocorrencia", "fechamento_frequencia", "permissao_excepcional_uso")) {
            out.put(tabela, jdbc.queryForList("select to_jsonb(t)::text from " + tabela + " t order by 1", String.class).stream().map(this::h).toList());
        }
        out.put("auditoriaNegocial", jdbc.queryForList("select to_jsonb(a)::text from registro_auditoria a where entity_type in "
                + "('Frequencia','EventoFrequencia','HistoricoEmpregado','FechamentoFrequencia') order by 1", String.class).stream().map(this::h).toList());
        return Map.copyOf(out);
    }
}
