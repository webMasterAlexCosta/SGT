package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** REL-006 vale também para a previsão calculada quando ainda não há lançamento. */
class EmissaoSemFrequenciaInvalidacaoHttpIT extends ContratosHttpBase {
    @ParameterizedTest
    @ValueSource(strings = {"CONTRATO", "ALOCACAO"})
    void fonteTemporalSemFrequenciaSuperaEmissaoAfetadaEPreservaPeriodoAnterior(String fonte) throws Exception {
        Cenario c = criarCenario();
        NovoContrato contrato = publicado(c, 100, 50, false);
        LocalDate referencia = RelogioSistema.hoje().with(TemporalAdjusters.previous(DayOfWeek.WEDNESDAY));
        long vinculoId = enviar(c.localA, "/api/vinculos-contratuais", mapa(
                "pessoa", mapa("nome", "Pessoa sem lançamentos", "cpf", cpf(Math.toIntExact(960000000L + contrato.id()))),
                "contratoId", contrato.id(), "composicaoId", contrato.composicao1(),
                "dataInicio", referencia.minusDays(2)),
                201, "Contrato publicado e vínculo canônico reais, sem frequência artificial").path("id").asLong();
        assertSemFrequencia(vinculoId);
        Map<String, Object> filtro = filtro(contrato.id(), referencia);
        JsonNode primeira = enviar(c.localA, "/api/relatorios/administrativos/emissoes", filtro,
                201, "Emissão original inclui previsão de SEM_LANCAMENTO");
        long id = primeira.path("metadados").path("id").asLong();
        assertEquals(1, primeira.path("conteudo").path("registros").size());
        assertEquals(1, primeira.path("conteudo").path("totais").path("semLancamento").asInt());
        assertEquals(480, primeira.path("conteudo").path("totais").path("minutosPrevistos").asInt());
        JsonNode controle = enviar(c.localA, "/api/relatorios/administrativos/emissoes",
                filtro(contrato.id(), referencia.minusDays(1)), 201,
                "Controle independente: emissão do dia anterior à alteração efetiva");
        long controleId = controle.path("metadados").path("id").asLong();

        Long cargoNovo = null;
        if (fonte.equals("CONTRATO")) {
            Map<String, Object> acao = mapa("dataEfetiva", referencia,
                    "motivo", "Inativação documentada altera a previsão sem lançamentos");
            JsonNode previa = enviar(c.localA, base(contrato.id()) + "/previas/inativar", acao,
                    200, "Prévia real antes de retirar cobertura contratual");
            assertFalse(previa.path("tokenPrevia").asText().isBlank());
            acao.put("tokenPrevia", previa.path("tokenPrevia").asText());
            acao.put("confirmarImpacto", true);
            enviar(c.localA, base(contrato.id()) + "/inativar", acao, 200,
                    "Inativação real confirma a alteração temporal da fonte");
        } else {
            Map<String, Object> acao = mapa("dataEfetiva", referencia, "composicaoId", contrato.composicao2(),
                    "justificativa", "Mudança documentada de cargo altera a alocação e o relatório",
                    "evidencia", "SEI-ALOCACAO-SEM-FREQUENCIA", "autorizacao", "DESPACHO-ALOCACAO");
            String baseVinculo = "/api/vinculos-contratuais/" + vinculoId;
            JsonNode previa = enviar(c.localA, baseVinculo + "/previas/alocacoes", acao,
                    200, "Prévia real de mudança temporal para outra composição publicada");
            assertEquals(0, previa.path("incompatibilidades").size());
            assertFalse(previa.path("token").asText().isBlank());
            acao.put("tokenPrevia", previa.path("token").asText());
            acao.put("confirmarImpacto", true);
            JsonNode alocacao = enviar(c.localA, baseVinculo + "/alocacoes", acao, 200,
                    "Alteração de alocação real confirma a nova fotografia do cargo");
            assertEquals(contrato.composicao2().longValue(), alocacao.path("composicaoBaseId").asLong());
            assertEquals(referencia.toString(), alocacao.path("dataInicio").asText());
            cargoNovo = alocacao.path("cargoId").asLong();
            assertNotEquals(c.cargoA.getId(), cargoNovo);
        }
        em.flush(); em.clear();
        assertSemFrequencia(vinculoId);
        JsonNode atual = enviar(c.localA, "/api/relatorios/administrativos/previa", filtro,
                200, "Controle: a mudança temporal já afeta efetivamente o cálculo oficial");
        if (fonte.equals("CONTRATO")) {
            assertEquals(0, atual.path("conteudo").path("registros").size());
            assertEquals(0, atual.path("conteudo").path("totais").path("minutosPrevistos").asInt());
        } else {
            assertEquals(1, atual.path("conteudo").path("registros").size());
            assertEquals(cargoNovo.longValue(), atual.path("conteudo").path("registros").get(0).path("cargoId").asLong());
        }
        assertNotEquals(primeira.path("conteudo"), atual.path("conteudo"));
        JsonNode historica = consultar(c.localA, "/api/relatorios/administrativos/emissoes/" + id,
                "Emissão anterior permanece consultável após a mudança da fonte");
        JsonNode controleDepois = consultar(c.localA, "/api/relatorios/administrativos/emissoes/" + controleId,
                "Mudança a partir da data efetiva não supera emissão de período anterior");
        assertEquals(primeira.path("conteudo"), historica.path("conteudo"));
        assertEquals(primeira.path("metadados").path("hashConteudo"), historica.path("metadados").path("hashConteudo"));
        assertEquals("VIGENTE", controleDepois.path("metadados").path("estado").asText());
        assertEquals(controle.path("conteudo"), controleDepois.path("conteudo"));
        filtro.put("chaveIdempotencia", "emissao-fonte-alterada-" + UUID.randomUUID());
        JsonNode segunda = enviar(c.localA, "/api/relatorios/administrativos/emissoes", filtro,
                201, "Nova emissão identifica que a verdade temporal foi retificada");
        assertEquals(2, segunda.path("metadados").path("versao").asInt());
        assertEquals(id, segunda.path("metadados").path("emissaoAnteriorId").asLong());
        assertAll("A ausência de frequência materializada não evita invalidação da emissão",
                () -> assertEquals("SUPERADA", historica.path("metadados").path("estado").asText()),
                () -> assertEquals("RETIFICACAO_FONTE", historica.path("metadados").path("causaSuperacao").asText()),
                () -> assertEquals("RETIFICADA", segunda.path("metadados").path("natureza").asText()));
    }

    private void assertSemFrequencia(long vinculoId) {
        assertEquals(0L, em.createQuery("select count(f) from Frequencia f where f.vinculoContratual.id = :id", Long.class)
                .setParameter("id", vinculoId).getSingleResult());
    }

    private Map<String, Object> filtro(long contratoId, LocalDate data) {
        return mapa("tipo", "FREQUENCIA_CONSOLIDADA", "formato", "VISUALIZACAO",
                "inicio", data, "fim", data, "contratoIds", List.of(contratoId),
                "chaveIdempotencia", "emissao-sem-frequencia-" + UUID.randomUUID());
    }
}
