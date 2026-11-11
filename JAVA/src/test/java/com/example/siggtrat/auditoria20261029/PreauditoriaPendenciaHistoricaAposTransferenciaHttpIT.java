package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.PendenciaAdministrativa;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** AUD-006/PER-010: tratar a tarefa da frequência requer o escopo do seu dia. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PreauditoriaPendenciaHistoricaAposTransferenciaHttpIT extends ContratosHttpBase {
    @Test
    void responsavelPeloDiaHistoricoPodeIniciarPendenciaAposTransferenciaPosterior() throws Exception {
        Cenario c = criarCenarioPersistido();
        long frequencia = enviar(c.localA, "/api/frequencias", mapa(
                "vinculoContratualId", c.vinculoA.getId(), "dataReferencia", DATA, "status", "FALTA",
                "justificativaRetificacao", "Fonte documental de frequência no período da Gestão A",
                "chaveIdempotencia", "preaud-pendencia-" + UUID.randomUUID()), 200,
                "Frequência histórica real em A").path("id").asLong();
        ciclo(c, "inativar", DATA.minusDays(1));
        ciclo(c, "reativar", DATA);
        long pendencia = emTransacao(() -> em.createQuery(
                        "select p.id from PendenciaAdministrativa p where p.frequenciaId=:id order by p.id", Long.class)
                .setParameter("id", frequencia).setMaxResults(1).getSingleResult());
        conceder(c, c.contratoA.getId(), "TRANSFERENCIA_GESTAO_LOCAL");
        enviar(c.localA, base(c.contratoA.getId()) + "/transferir-gestao", mapa(/* Conferência SINTÉTICA explícita desta fixture; não é padrão do produto. */
                "conferenciaDocumentalConfirmada", true, "versaoConferenciaDocumental", "transferencia-gestao-conferencia-v1", 
                "dataEfetiva", DATA.plusDays(1), "dataAto", RelogioSistema.hoje(),
                "gestaoLocalDestinoId", c.gestaoB.getId(), "tipoContratacaoDestinoId", c.tipoB.getId(),
                "motivo", "Transferência documentada posterior ao dia da frequência pendente",
                "referenciaSei", "SEI-PREAUDITORIA-TRANSFERENCIA-POSTERIOR", "autorizacaoAdministrativa", "ATO-HTTP",
                "mapeamentos", List.of(mapa("composicaoOrigemId", c.composicaoA.getId(),
                        "lotacaoDestinoId", c.lotacaoB.getId(), "jornadaDestinoId", c.jornadaB.getId(),
                        "cargoDestinoId", c.cargoB.getId()))), 200,
                "CON-023: transferência altera somente a responsabilidade a partir do dia seguinte");

        emTransacao(() -> {
            var p = em.find(PendenciaAdministrativa.class, pendencia);
            assertEquals(c.gestaoA.getId(), p.getGestaoLocalId());
            assertEquals(DATA, p.getDataEfetiva());
            assertThat(p.getEstado().name()).isEqualTo("ABERTA");
            return null;
        });
        JsonNode prova = consultar(c.localA, "/api/frequencias/" + frequencia + "/eventos",
                "PER-010: A conserva hoje autorização sobre a prova do seu dia histórico");
        assertThat(prova).isNotEmpty();
        JsonNode centralA = dados(executar(c.localA, get("/api/pendencias")
                        .param("contratoIds", c.contratoA.getId().toString()), 200,
                "AUD-006: a tarefa continua atribuída a A por sua responsabilidade no dia"));
        assertThat(contem(centralA, pendencia)).isTrue();
        JsonNode centralB = dados(executar(c.localB, get("/api/pendencias")
                        .param("contratoIds", c.contratoA.getId().toString()), 200,
                "Controle: B não adquire responsabilidade por um dia anterior à transferência"));
        assertThat(contem(centralB, pendencia)).isFalse();
        executar(c.localB, post("/api/pendencias/{id}/iniciar-tratamento", pendencia), 404,
                "Controle: responsável atual do contrato não recebe tarefa fora de seu período");
        emTransacao(() -> {
            em.clear();
            var p = em.find(PendenciaAdministrativa.class, pendencia);
            assertThat(p.getEstado().name()).isEqualTo("ABERTA");
            assertThat(p.getIniciadaEm()).isNull();
            assertThat(p.getIniciadaPorId()).isNull();
            return null;
        });
        String observacao = "Conferência da prova que continua sob responsabilidade histórica de A";
        enviar(c.localA, "/api/pendencias/" + pendencia + "/iniciar-tratamento",
                mapa("observacao", observacao), 200,
                "AUD-006/PER-010: não exigir simultaneamente gestão histórica A e gestão corrente B");
        emTransacao(() -> {
            em.clear();
            var p = em.find(PendenciaAdministrativa.class, pendencia);
            assertThat(p.getEstado().name()).isEqualTo("EM_TRATAMENTO");
            assertEquals(c.gestaoA.getId(), p.getGestaoLocalId());
            assertEquals(DATA, p.getDataEfetiva());
            assertEquals(c.localA.getId(), p.getIniciadaPorId());
            assertEquals(c.localA.getUsername(), p.getIniciadaPor());
            assertEquals(observacao, p.getObservacaoTratamento());
            assertThat(p.getIniciadaEm()).isNotNull();
            assertThat(p.getResolvidaEm()).isNull();
            return null;
        });
        assertEquals(prova, consultar(c.localA, "/api/frequencias/" + frequencia + "/eventos",
                "Iniciar tratamento preserva integralmente os eventos da prova histórica"));
    }

    private void ciclo(Cenario c, String comando, LocalDate data) throws Exception {
        Map<String, Object> req = mapa("dataEfetiva", data, "dataAto", RelogioSistema.hoje(),
                "motivo", "Conferência administrativa documentada da cobertura da frequência",
                "referenciaSei", "SEI-PREAUDITORIA-PENDENCIA-" + comando);
        JsonNode previa = enviar(c.localA, base(c.contratoA.getId()) + "/previas/" + comando,
                req, 200, "Prévia obrigatória para o ato administrativo retroativo");
        req.put("tokenPrevia", previa.path("tokenPrevia").asText());
        req.put("confirmarImpacto", true);
        enviar(c.localA, base(c.contratoA.getId()) + "/" + comando, req, 200,
                "Ato administrativo conserva a frequência e cria revisão humana pendente");
    }

    private boolean contem(JsonNode pagina, long id) {
        for (JsonNode item : pagina.path("registros")) if (item.path("id").asLong() == id) return true;
        return false;
    }
}
