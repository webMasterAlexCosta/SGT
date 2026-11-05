package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.Frequencia;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** PER-010: concessão atual é aplicada à organização fotografada na frequência. */
class FrequenciaPermissaoHistoricaHttpIT extends ContratosHttpBase {
    @ParameterizedTest
    @ValueSource(strings = {"eventos", "evidencias-integridade", "intervalo"})
    void concessaoDaGestaoAtualNaoAmpliaLeituraPessoalDaGestaoHistorica(String rota) throws Exception {
        Cenario c = criarCenario();
        NovoContrato contrato = publicado(c, 100, null, false);
        LocalDate dataFonte = DataJornadaSintetica.ultimoDiaDeTrabalhoAnterior(c.jornadaA);
        JsonNode vinculo = enviar(c.localA, "/api/vinculos-contratuais", mapa(
                "pessoa", mapa("nome", "Pessoa da frequência histórica", "cpf", cpf(Math.toIntExact(940000000L + contrato.id()))),
                "contratoId", contrato.id(), "composicaoId", contrato.composicao1(), "dataInicio", dataFonte),
                201, "Cadastro real sob a Gestão A antes da transferência");
        String segredo = "DADO-PESSOAL-DA-GESTAO-A-" + UUID.randomUUID();
        long frequenciaId = enviar(c.localA, "/api/frequencias", mapa(
                "vinculoContratualId", vinculo.path("id").asLong(), "dataReferencia", dataFonte,
                "status", "PRESENCA", "horasTrabalhadas", "04:00", "horarioSaida", "12:00",
                "observacao", segredo, "justificativaRetificacao", "Registro histórico documentado",
                "chaveIdempotencia", "frequencia-historica-" + UUID.randomUUID()),
                200, "Frequência e prova integral reais na Gestão A").path("id").asLong();
        LocalDate transferencia = RelogioSistema.hoje().plusDays(5);
        conceder(c, contrato.id(), "TRANSFERENCIA_GESTAO_LOCAL");
        enviar(c.localA, base(contrato.id()) + "/transferir-gestao", mapa(
                "dataEfetiva", transferencia, "dataAto", RelogioSistema.hoje(), "gestaoLocalDestinoId", c.gestaoB.getId(), "tipoContratacaoDestinoId", c.tipoB.getId(),
                "motivo", "Transferência documentada posterior à frequência", "referenciaSei", "SEI-FREQUENCIA-HISTORICA",
                "autorizacaoAdministrativa", "ATO-HTTP", "mapeamentos", List.of(mapa(
                        "composicaoOrigemId", contrato.composicao1(), "lotacaoDestinoId", c.lotacaoB.getId(),
                        "jornadaDestinoId", c.jornadaB.getId(), "cargoDestinoId", c.cargoB.getId()))),
                200, "Transferência efetiva posterior preserva o registro anterior");
        em.flush(); em.clear();
        assertEquals(c.gestaoA.getId(), em.find(Frequencia.class, frequenciaId).getGestaoLocalSnapshotId());

        try (var relogio = Mockito.mockStatic(RelogioSistema.class, Mockito.CALLS_REAL_METHODS)) {
            relogio.when(RelogioSistema::hoje).thenReturn(transferencia);
            assertEquals(c.gestaoB.getId().longValue(), consultar(c.nacional,
                    base(contrato.id()) + "/posicao-temporal?data=" + transferencia,
                    "O contrato está efetivamente na Gestão B no momento da consulta")
                    .path("versaoContratual").path("gestaoLocalId").asLong());
            long concessaoB = concederGestao(c, c.gestaoB.getId());
            String path = rota.equals("intervalo") ? "/api/frequencias/intervalo?inicio=" + dataFonte
                    + "&fim=" + dataFonte + "&contratoId=" + contrato.id()
                    : "/api/frequencias/" + frequenciaId + "/" + rota;
            JsonNode minimizada = consultar(c.nacional, path,
                    "Concessão atual na Gestão B não alcança o conteúdo pessoal histórico da Gestão A");
            assertFalse(minimizada.toString().contains(segredo));
            assertEquals(0, consultar(c.admin, "/api/permissoes-excepcionais/" + concessaoB + "/usos",
                    "A concessão fora do escopo histórico não é consumida").size());

            long concessaoA = concederGestao(c, c.gestaoA.getId());
            JsonNode completa = consultar(c.nacional, path,
                    "Concessão vigente na Gestão A permite a prova pessoal do respectivo registro histórico");
            assertTrue(completa.toString().contains(segredo));
            assertEquals(1, consultar(c.admin, "/api/permissoes-excepcionais/" + concessaoA + "/usos",
                    "A concessão histórica compatível registra exatamente a consulta protegida").size());
            if (rota.equals("intervalo")) {
                assertEquals(0, consultar(c.estadualB, path,
                        "Autorização atual da Estadual B não inclui registros históricos da Estadual A")
                        .path("content").size());
            } else {
                executar(c.estadualB, get(path), 404,
                        "A concessão não substitui autorização atual combinada com escopo histórico");
            }
        }
    }

    private long concederGestao(Cenario c, Long gestaoId) throws Exception {
        return enviar(c.admin, "/api/permissoes-excepcionais", mapa(
                "tipo", "ACESSO_DADOS_PESSOAIS_RESTRITOS", "usuarioBeneficiarioId", c.nacional.getId(),
                "gestaoLocalId", gestaoId, "finalidade", "Auditoria histórica de frequência",
                "justificativa", "Exame pessoal limitado à Gestão explicitamente autorizada",
                "autorizacaoAdministrativa", "SEI-GESTAO-HISTORICA", "validoAte", Instant.now().plusSeconds(3600)),
                201, "Concessão pessoal vigente por Gestão Local").path("id").asLong();
    }
}
