package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.entidade.Empregado;
import com.example.siggtrat.entidade.Frequencia;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** FRE-018: o filtro explícito de Contrato não pode substituir sua responsabilidade histórica. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class FechamentoContratoTransferidoHttpIT extends ContratosHttpBase {

    @Test
    void fechamentoDaGestaoHistoricaAceitaContratoTransferidoEWorkerPreservaContexto() throws Exception {
        Cenario c = criarCenarioPersistido();
        transferir(c);
        assertProjecaoOperacionalNaGestaoAtual(c);
        Map<String,Object> comando = comando(c, false);
        JsonNode criado = dados(executar(c.localA, post("/api/frequencias/fechamentos")
                .contentType(APPLICATION_JSON).content(json(comando)), 202,
                "Contrato explicitamente filtrado pertencia à Gestão A na data do fechamento"));
        JsonNode fim = aguardar(c.localA, criado.path("id").asLong());
        assertEquals("CONCLUIDO", fim.path("status").asText(), fim.toString());
        assertEquals(1, fim.path("totalInserido").asLong());
        emTransacao(() -> {
            Frequencia f = em.createQuery("select f from Frequencia f where f.vinculoContratual.id=:v and f.dataReferencia=:d", Frequencia.class)
                    .setParameter("v", c.vinculoA.getId()).setParameter("d", DATA).getSingleResult();
            assertEquals(c.gestaoA.getId(), f.getGestaoLocalSnapshotId());
            assertEquals(c.lotacaoA.getId(), f.getLotacaoSnapshotId());
            assertEquals(c.jornadaA.getId(), f.getJornadaSnapshotId());
            assertEquals(c.cargoA.getId(), f.getCargoSnapshotId());
            assertEquals(c.contratoA.getId(), f.getContratoSnapshotId());
            assertEquals(c.localA.getId(), f.getAutorOriginalId());
            assertEquals(c.empregadoA.getId(), f.getEmpregado().getId());
            assertEquals(c.pessoaA.getId(), f.getPessoa().getId());
            assertEquals(c.alocacaoA.getId(), f.getAlocacaoVinculo().getId());
            return null;
        });
        assertProjecaoOperacionalNaGestaoAtual(c);
        executar(c.localB, get("/api/frequencias/fechamentos/{id}", criado.path("id").asLong()), 404,
                "A Gestão atual do contrato não assume o fechamento de período da Gestão A");
    }

    @Test
    void comandoDaGestaoAtualNaoAceitaContratoEmDataHistoricamenteAlheia() throws Exception {
        Cenario c = criarCenarioPersistido();
        transferir(c);
        var resposta = mvc.perform(autenticar(post("/api/frequencias/fechamentos").contentType(APPLICATION_JSON)
                .content(json(comando(c, true))), c.localB)).andReturn().getResponse();
        assertTrue(List.of(400, 403).contains(resposta.getStatus()),
                "Controle de negação já existente: não pode aceitar fechamento B em dia de A; resposta="
                        + resposta.getStatus() + " " + resposta.getContentAsString());
        assertEquals(0L, emTransacao(() -> em.createQuery("select count(f) from FechamentoFrequencia f "
                        + "where f.contratoIds=:c and f.dataReferencia=:d", Long.class)
                .setParameter("c", c.contratoA.getId().toString()).setParameter("d", DATA).getSingleResult()));
    }

    private Map<String,Object> comando(Cenario c, boolean ladoB) {
        return mapa("dataReferencia", DATA, "lotacaoId", (ladoB ? c.lotacaoB : c.lotacaoA).getId(),
                "jornadaId", (ladoB ? c.jornadaB : c.jornadaA).getId(), "contratoIds", List.of(c.contratoA.getId()),
                "alteracoes", List.of(), "justificativaRetificacao", "Fechamento histórico documentado após transferência",
                "chaveIdempotencia", "fechamento-contrato-transferido-" + UUID.randomUUID());
    }

    private void assertProjecaoOperacionalNaGestaoAtual(Cenario c) {
        emTransacao(() -> {
            // Releitura em transação própria: o fechamento histórico não pode
            // persistir uma combinação de Gestão atual B com alocação passada A.
            em.clear();
            Empregado empregado = em.find(Empregado.class, c.empregadoA.getId());
            assertNotNull(empregado);
            assertEquals(c.pessoaA.getId(), empregado.getPessoaCanonica().getId());
            assertEquals(c.gestaoB.getId(), empregado.getGestaoLocal().getId());
            assertEquals(c.lotacaoB.getId(), empregado.getLotacao().getId());
            assertEquals(c.jornadaB.getId(), empregado.getJornada().getId());
            assertEquals(c.cargoB.getId(), empregado.getCargo().getId());
            assertEquals("ATIVO", empregado.getStatus().getDescricao());
            assertEquals(List.of(c.contratoA.getId()), empregado.getContratos().stream().map(Contrato::getId).toList());
            return null;
        });
    }

    private void transferir(Cenario c) throws Exception {
        conceder(c, c.contratoA.getId(), "TRANSFERENCIA_GESTAO_LOCAL");
        enviar(c.localA, base(c.contratoA.getId()) + "/transferir-gestao", mapa(/* Conferência SINTÉTICA explícita desta fixture; não é padrão do produto. */
                "conferenciaDocumentalConfirmada", true, "versaoConferenciaDocumental", "transferencia-gestao-conferencia-v1", 
                "dataEfetiva", DATA.plusDays(1), "dataAto", RelogioSistema.hoje(),
                "gestaoLocalDestinoId", c.gestaoB.getId(), "tipoContratacaoDestinoId", c.tipoB.getId(),
                "motivo", "Transferência formal posterior ao dia do fechamento", "referenciaSei", "SEI-FECHAMENTO-TRANSFERIDO",
                "autorizacaoAdministrativa", "ATO-HTTP", "mapeamentos", List.of(mapa(
                        "composicaoOrigemId", c.composicaoA.getId(), "lotacaoDestinoId", c.lotacaoB.getId(),
                        "jornadaDestinoId", c.jornadaB.getId(), "cargoDestinoId", c.cargoB.getId()))),
                200, "Transferência efetiva real atualiza a raiz do Contrato para a Gestão B");
        assertEquals(c.gestaoB.getId(), emTransacao(() -> em.find(Contrato.class, c.contratoA.getId()).getGestaoLocal().getId()));
    }

    private JsonNode aguardar(UsuarioAplicacao usuario, long id) throws Exception {
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(25);
        JsonNode atual;
        do {
            atual = dados(executar(usuario, get("/api/frequencias/fechamentos/{id}", id), 200,
                    "Acompanhamento do worker real no escopo histórico"));
            if (List.of("CONCLUIDO", "ERRO", "CANCELADO").contains(atual.path("status").asText())) return atual;
            Thread.sleep(100);
        } while (System.nanoTime() < limite);
        fail("Fechamento não concluiu: " + atual);
        return atual;
    }
}
