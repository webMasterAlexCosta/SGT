package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** REL-006/PER-010: a invalidação acompanha a Gestão efetiva da fonte alterada. */
class EmissaoVinculoTransferidoInvalidacaoHttpIT extends ContratosHttpBase {
    @org.springframework.beans.factory.annotation.Autowired
    private com.example.siggtrat.servico.GestaoContratoTemporalServico gestaoDaFonte;

    @Test
    void retificacaoDoVinculoDepoisDeTransferenciaProgramadaSuperaEmissaoDaGestaoEfetiva() throws Exception {
        Cenario c = criarCenario();
        LocalDate preparacao = DATA.minusDays(10);
        LocalDate transferencia = DATA.minusDays(2);
        try (var relogio = Mockito.mockStatic(RelogioSistema.class, Mockito.CALLS_REAL_METHODS)) {
            relogio.when(RelogioSistema::hoje).thenReturn(preparacao);
            NovoContrato contrato = publicado(c, 100, null, false);
            long motivoB = enviar(c.localB, "/api/motivos-desligamento", mapa(
                    "descricao", "Motivo documentado da Gestão de destino " + contrato.id(),
                    "categoria", "DESLIGAMENTO_INDIVIDUAL", "ativo", true),
                    201, "Catálogo real de B disponível antes das datas dos fatos").path("id").asLong();
            long vinculoId = enviar(c.localA, "/api/vinculos-contratuais", mapa(
                    "pessoa", mapa("nome", "Pessoa da emissão após transferência",
                            "cpf", cpf(Math.toIntExact(980000000L + contrato.id()))),
                    "contratoId", contrato.id(), "composicaoId", contrato.composicao1(),
                    "dataInicio", preparacao),
                    201, "Vínculo canônico criado na Gestão de origem").path("id").asLong();
            conceder(c, contrato.id(), "TRANSFERENCIA_GESTAO_LOCAL");
            JsonNode atualizado = enviar(c.localA, base(contrato.id()) + "/transferir-gestao", mapa(
                    "dataEfetiva", transferencia, "dataAto", preparacao,
                    "gestaoLocalDestinoId", c.gestaoB.getId(), "tipoContratacaoDestinoId", c.tipoB.getId(),
                    "motivo", "Responsabilidade de B programada com mapeamento explícito",
                    "referenciaSei", "SEI-TRANSFERENCIA-EMISSAO", "autorizacaoAdministrativa", "ATO-HTTP",
                    "mapeamentos", List.of(mapa("composicaoOrigemId", contrato.composicao1(),
                            "lotacaoDestinoId", c.lotacaoB.getId(), "jornadaDestinoId", c.jornadaB.getId(),
                            "cargoDestinoId", c.cargoB.getId()))),
                    200, "Transferência programada não depende de atualização posterior da coluna materializada");
            assertEquals(contrato.id(), atualizado.path("id").asLong());
            JsonNode transferencias = consultar(c.nacional, base(contrato.id()) + "/transferencias-gestao",
                    "ContratoDTO do POST é complementado pela prova explícita da transferência");
            assertEquals(1, transferencias.size());
            assertEquals(c.gestaoB.getId().longValue(), transferencias.get(0).path("gestaoDestinoId").asLong());
            assertEquals(transferencia.toString(), transferencias.get(0).path("dataEfetiva").asText());

            relogio.when(RelogioSistema::hoje).thenReturn(DATA);
            em.flush(); em.clear();
            assertEquals(c.gestaoA.getId(), em.find(Contrato.class, contrato.id()).getGestaoLocal().getId(),
                    "A coluna conserva A; o contrato é reconstruído como B na data efetiva");
            assertEquals(c.gestaoB.getId().longValue(), consultar(c.nacional,
                    base(contrato.id()) + "/posicao-temporal?data=" + DATA,
                    "Pré-condição CT007: autorização corrente e leitura temporal já reconhecem B")
                    .path("versaoContratual").path("gestaoLocalId").asLong());
            // A causa de DATA-1 está documentada antes das emissões de controle.
            // A inserção não é o ato sob teste e não pode ser a causa de sua superação.
            FonteMotivoSintetica.declarar(em, gestaoDaFonte, contrato.id(), motivoB, c.gestaoB.getId(), c.localB,
                    "Motivo documentado da Gestão de destino " + contrato.id(),
                    com.example.siggtrat.dominio.vinculo.CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL,
                    DATA.minusDays(1), DATA.minusDays(1), java.time.Instant.now(),
                    "SEI-SINTETICO-REL006-CAUSA-ANTERIOR-A-EMISSAO");
            String baseVinculo = "/api/vinculos-contratuais/" + vinculoId;
            enviar(c.localB, baseVinculo + "/desligar", desligamento(DATA.plusDays(10), motivoB),
                    200, "Desligamento original futuro mantém a cobertura de DATA");
            assertSemFrequencia(vinculoId);
            Map<String, Object> filtro = filtro(contrato.id(), DATA);
            JsonNode original = enviar(c.localB, "/api/relatorios/administrativos/emissoes", filtro,
                    201, "B emite formalmente a previsão da fonte sob sua responsabilidade efetiva");
            long emissaoId = original.path("metadados").path("id").asLong();
            assertEquals(1, original.path("conteudo").path("registros").size());
            assertEquals(1, original.path("conteudo").path("totais").path("semLancamento").asInt());
            assertEquals(480, original.path("conteudo").path("totais").path("minutosPrevistos").asInt());
            JsonNode controle = enviar(c.localB, "/api/relatorios/administrativos/emissoes",
                    filtro(contrato.id(), DATA.minusDays(1)), 201,
                    "O último dia que continuará coberto tem emissão independente");

            Map<String, Object> retificacao = desligamento(DATA.minusDays(1), motivoB);
            JsonNode previa = enviar(c.localB, baseVinculo + "/previas/retificacao-desligamento", retificacao,
                    200, "Prévia real e vinculada ao conteúdo da retificação");
            assertFalse(previa.path("token").asText().isBlank());
            assertEquals(0, previa.path("vinculosConflitantes").size());
            retificacao.put("tokenPrevia", previa.path("token").asText());
            retificacao.put("confirmarImpacto", true);
            JsonNode retificado = enviar(c.localB, baseVinculo + "/retificar-desligamento", retificacao,
                    200, "B retifica o término e aciona a invalidação por vínculo");
            assertEquals(DATA.minusDays(1).toString(), retificado.path("dataFim").asText());
            em.flush(); em.clear();
            assertSemFrequencia(vinculoId);
            JsonNode calculoAtual = enviar(c.localB, "/api/relatorios/administrativos/previa", filtro,
                    200, "Controle de efeito: a fonte alterada já removeu a previsão de DATA");
            assertEquals(0, calculoAtual.path("conteudo").path("registros").size());
            assertEquals(0, calculoAtual.path("conteudo").path("totais").path("minutosPrevistos").asInt());
            JsonNode historica = consultar(c.localB, "/api/relatorios/administrativos/emissoes/" + emissaoId,
                    "A emissão permanece acessível com a prova original preservada");
            assertEquals(original.path("conteudo"), historica.path("conteudo"));
            assertEquals(original.path("metadados").path("hashConteudo"), historica.path("metadados").path("hashConteudo"));
            JsonNode controleDepois = consultar(c.localB, "/api/relatorios/administrativos/emissoes/"
                    + controle.path("metadados").path("id").asLong(), "O último dia válido permanece VIGENTE");
            assertEquals("VIGENTE", controleDepois.path("metadados").path("estado").asText());
            assertEquals(controle.path("conteudo"), controleDepois.path("conteudo"));
            filtro.put("chaveIdempotencia", "emissao-vinculo-retificado-" + UUID.randomUUID());
            JsonNode nova = enviar(c.localB, "/api/relatorios/administrativos/emissoes", filtro,
                    201, "A emissão seguinte representa a fonte retificada");
            assertEquals(2, nova.path("metadados").path("versao").asInt());
            assertEquals(emissaoId, nova.path("metadados").path("emissaoAnteriorId").asLong());
            assertAll("A Gestão materializada A não pode ocultar a invalidação da fonte efetiva de B",
                    () -> assertEquals("SUPERADA", historica.path("metadados").path("estado").asText()),
                    () -> assertEquals("RETIFICACAO_FONTE", historica.path("metadados").path("causaSuperacao").asText()),
                    () -> assertEquals("RETIFICADA", nova.path("metadados").path("natureza").asText()));
        }
    }

    private Map<String, Object> desligamento(LocalDate fim, long motivoId) {
        return mapa("dataFim", fim, "motivoDesligamentoId", motivoId,
                "justificativa", "Término documentado da Gestão de destino",
                "evidencia", "SEI-TERMINO-TRANSFERIDO", "autorizacao", "ATO-TERMINO-TRANSFERIDO");
    }

    private void assertSemFrequencia(long vinculoId) {
        assertEquals(0L, em.createQuery("select count(f) from Frequencia f where f.vinculoContratual.id = :id", Long.class)
                .setParameter("id", vinculoId).getSingleResult());
    }

    private Map<String, Object> filtro(long contratoId, LocalDate data) {
        return mapa("tipo", "FREQUENCIA_CONSOLIDADA", "formato", "VISUALIZACAO",
                "inicio", data, "fim", data, "contratoIds", List.of(contratoId),
                "chaveIdempotencia", "emissao-vinculo-transferido-" + UUID.randomUUID());
    }
}
