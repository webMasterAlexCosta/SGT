package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.*;
import com.example.siggtrat.evento.EventoFechamentoFrequenciaSolicitado;
import com.example.siggtrat.servico.FechamentoFrequenciaServico;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
class FechamentosTransacoesHttpIT extends AuditoriaHttpSuporte {
    @Autowired ApplicationEventPublisher eventos;

    @Test
    void fechamentoExplicitoPreservaJustificativaEIdempotenciaAoPersistirAFila() throws Exception {
        Cenario c = criarCenarioPersistido();
        String chave = "fechamento-item-" + UUID.randomUUID();
        Map<String,Object> alteracao = new LinkedHashMap<>(alteracao(c, DATA, "FALTA", chave));
        alteracao.put("empregado", Map.of("id", c.empregadoA.getId()));
        JsonNode criado = dados(executar(c.localA, post("/api/frequencias/fechamentos").contentType(APPLICATION_JSON)
                .content(json(comando(c, DATA, List.of(alteracao)))), 202,
                "Fechamento explícito aceito para processamento real"));
        long id = criado.path("id").asLong();
        JsonNode finalizado = aguardar(c.localA, id);
        assertEquals("CONCLUIDO", finalizado.path("status").asText(), finalizado.toString());
        assertEquals(1, finalizado.path("totalInserido").asLong());
        assertEquals(1, finalizado.path("totalProcessado").asLong());
        long frequenciaId = emTransacao(() -> {
            Frequencia f = em.createQuery("select f from Frequencia f where f.vinculoContratual.id=:v and f.dataReferencia=:d", Frequencia.class)
                    .setParameter("v", c.vinculoA.getId()).setParameter("d", DATA).getSingleResult();
            assertEquals("FALTA", f.getStatus()); assertTrue(f.getBloqueadaEdicaoOperacional());
            return f.getId();
        });
        JsonNode cadeia = dados(executar(c.localA, get("/api/frequencias/{id}/eventos", frequenciaId), 200,
                "Worker deve produzir a mesma trilha formal do lançamento direto"));
        assertEquals(1, cadeia.size());
        assertEquals("Justificativa formal do item de fechamento", cadeia.get(0).path("motivo").asText());
        emTransacao(() -> {
            assertEquals(1L, em.createQuery("select count(e) from EventoFrequencia e where e.chaveIdempotencia=:c", Long.class)
                    .setParameter("c", chave).getSingleResult()); return null;
        });
        JsonNode atual = dados(executar(c.localA, get("/api/frequencias/fechamentos/atual")
                .param("data", DATA.toString()).param("lotacaoId", c.lotacaoA.getId().toString())
                .param("jornadaId", c.jornadaA.getId().toString()).param("contratoIds", c.contratoA.getId().toString()), 200,
                "Consulta atual recupera o fechamento pelo mesmo escopo canônico"));
        assertEquals(id, atual.path("id").asLong());
        executar(c.localA, post("/api/frequencias/fechamentos/{id}/cancelamento", id), 409,
                "Fechamento concluído não pode ser cancelado");
        executar(c.localA, post("/api/frequencias/fechamentos/{id}/reprocessamento", id), 409,
                "Fechamento concluído não é reprocessável");
    }

    @Test
    void fechamentoImplicitoGerencialExigeEPreservaJustificativaFormalDoComando() throws Exception {
        Cenario c = criarCenarioPersistido();
        Map<String,Object> corpo = new LinkedHashMap<>(comando(c, DATA, List.of()));
        corpo.put("justificativaRetificacao", "Conferência documental coletiva do dia de referência");
        corpo.put("chaveIdempotencia", "fechamento-coletivo-" + UUID.randomUUID());
        JsonNode criado = dados(executar(c.localA, post("/api/frequencias/fechamentos").contentType(APPLICATION_JSON)
                .content(json(corpo)), 202, "Fechamento gerencial contém intenção e motivo coletivo explícitos"));
        JsonNode fim = aguardar(c.localA, criado.path("id").asLong());
        assertEquals("CONCLUIDO", fim.path("status").asText(), fim.toString());
        assertEquals(1, fim.path("totalInserido").asLong());
        emTransacao(() -> {
            EventoFrequencia evento = em.createQuery("select e from EventoFrequencia e where e.frequencia.vinculoContratual.id=:v", EventoFrequencia.class)
                    .setParameter("v", c.vinculoA.getId()).getSingleResult();
            assertEquals("Conferência documental coletiva do dia de referência", evento.getMotivo());
            assertNotNull(evento.getChaveIdempotencia());
            assertEquals(c.localA.getId(), evento.getUsuarioId()); return null;
        });
    }

    @Test
    void elegibilidadeDoFechamentoUsaAlocacaoHistoricaMesmoComProjecaoAtualDiferente() throws Exception {
        Cenario c = criarCenarioPersistido();
        emTransacao(() -> {
            Empregado e = em.find(Empregado.class, c.empregadoA.getId());
            e.getContratos().clear(); e.getContratos().add(em.getReference(Contrato.class, c.contratoB.getId()));
            e.setLotacao(em.getReference(Lotacao.class, c.lotacaoB.getId()));
            e.setJornada(em.getReference(Jornada.class, c.jornadaB.getId()));
            e.setGestaoLocal(em.getReference(GestaoLocal.class, c.gestaoB.getId())); return null;
        });
        Map<String,Object> corpo = new LinkedHashMap<>(comando(c, DATA, List.of()));
        corpo.put("justificativaRetificacao", "Fechamento de período pertencente à Gestão A");
        corpo.put("chaveIdempotencia", "fechamento-historico-" + UUID.randomUUID());
        JsonNode criado = dados(executar(c.localA, post("/api/frequencias/fechamentos").contentType(APPLICATION_JSON)
                .content(json(corpo)), 202, "Elegibilidade histórica não depende da projeção atual do Empregado"));
        JsonNode fim = aguardar(c.localA, criado.path("id").asLong());
        assertEquals("CONCLUIDO", fim.path("status").asText(), fim.toString());
        assertEquals(1, fim.path("totalInserido").asLong());
        emTransacao(() -> {
            Frequencia f = em.createQuery("select f from Frequencia f where f.vinculoContratual.id=:v", Frequencia.class)
                    .setParameter("v", c.vinculoA.getId()).getSingleResult();
            assertEquals(c.lotacaoA.getId(), f.getLotacaoSnapshotId());
            assertEquals(c.contratoA.getId(), f.getContratoSnapshotId()); return null;
        });
    }

    @Test
    void consultaDeFechamentoPermiteLeitoresSemConcederPoderDeEdicao() throws Exception {
        Cenario c = criarCenarioPersistido();
        long id = criarFechamento(c, DATA, c.localA, FechamentoFrequenciaStatus.PENDENTE);
        for (UsuarioAplicacao leitor : List.of(c.localA, c.estadualA, c.nacional, c.presidente)) {
            JsonNode visto = dados(executar(leitor, get("/api/frequencias/fechamentos/{id}", id), 200,
                    "Leitor autorizado acompanha fechamento: " + leitor.getPerfil()));
            assertEquals(c.lotacaoA.getId().longValue(), visto.path("lotacaoId").asLong());
        }
        executar(c.localB, get("/api/frequencias/fechamentos/{id}", id), 404, "Fechamento A não é revelado à Gestão B");
        executar(c.estadualB, get("/api/frequencias/fechamentos/{id}", id), 404, "Fechamento A não é revelado ao Estado B");
        executar(c.estadualA, post("/api/frequencias/fechamentos/{id}/cancelamento", id), 403,
                "Permissão de leitura não concede cancelamento");
    }

    @Test
    void cancelamentoEReprocessamentoMantemEstadoContadoresETransacoesDoWorker() throws Exception {
        Cenario c = criarCenarioPersistido(); LocalDate data = LocalDate.now().minusDays(1);
        long id = criarFechamento(c, data, c.operacionalA, FechamentoFrequenciaStatus.PENDENTE);
        executar(c.localB, post("/api/frequencias/fechamentos/{id}/cancelamento", id), 404,
                "Não cancela trabalho de outra gestão");
        JsonNode cancelamento = dados(executar(c.operacionalA, post("/api/frequencias/fechamentos/{id}/cancelamento", id),
                202, "Cancelamento é solicitado antes do processamento do próximo bloco"));
        assertTrue(cancelamento.path("cancelamentoSolicitado").asBoolean());
        emTransacao(() -> { eventos.publishEvent(new EventoFechamentoFrequenciaSolicitado(id)); return null; });
        assertEquals("CANCELADO", aguardar(c.operacionalA, id).path("status").asText());
        executar(c.localB, post("/api/frequencias/fechamentos/{id}/reprocessamento", id), 404,
                "Não reprocessa fechamento alheio");
        JsonNode reinicio = dados(executar(c.operacionalA, post("/api/frequencias/fechamentos/{id}/reprocessamento", id),
                202, "Reprocessamento de cancelado inicia nova tentativa"));
        assertEquals(2, reinicio.path("tentativa").asInt());
        JsonNode fim = aguardar(c.operacionalA, id);
        assertEquals("CONCLUIDO", fim.path("status").asText(), fim.toString());
        assertEquals(1, fim.path("totalProcessado").asLong());
        assertEquals(1, fim.path("totalInserido").asLong());
        assertEquals(100, fim.path("percentual").asInt());
    }

    @Test
    void fechamentoRejeitaEntradaInvalidaEPerfisSemPermissao() throws Exception {
        Cenario c = criarCenarioPersistido();
        executar(c.localA, get("/api/frequencias/fechamentos/atual").param("data", DATA.toString())
                .param("lotacaoId", c.lotacaoA.getId().toString()).param("jornadaId", c.jornadaA.getId().toString()),
                204, "Escopo sem fechamento retorna corpo vazio");
        executar(c.localA, get("/api/frequencias/fechamentos/{id}", Long.MAX_VALUE), 404, "Fechamento inexistente");
        executar(c.localA, post("/api/frequencias/fechamentos/{id}/cancelamento", Long.MAX_VALUE), 404, "Cancelamento inexistente");
        executar(c.localA, post("/api/frequencias/fechamentos/{id}/reprocessamento", Long.MAX_VALUE), 404, "Reprocessamento inexistente");
        executar(c.localA, post("/api/frequencias/fechamentos").contentType(APPLICATION_JSON).content("{}"),
                400, "Campos do comando são obrigatórios");
        executar(c.localA, post("/api/frequencias/fechamentos").contentType(APPLICATION_JSON)
                .content(json(comando(c, LocalDate.now().plusDays(1), List.of()))), 400, "Não fecha data futura");
        executar(c.localB, post("/api/frequencias/fechamentos").contentType(APPLICATION_JSON)
                .content(json(comando(c, DATA, List.of()))), 403, "Não cria fechamento no escopo alheio");
        executar(c.operacionalA, post("/api/frequencias/fechamentos").contentType(APPLICATION_JSON)
                .content(json(comando(c, DATA, List.of()))), 403, "Operacional não fecha retroativamente após o oitavo dia");
        executar(c.estadualA, post("/api/frequencias/fechamentos").contentType(APPLICATION_JSON)
                .content(json(comando(c, DATA, List.of()))), 403, "Gestor Estadual consulta, mas não lança frequência");
    }

    @Test
    void loteComSegundoItemInvalidoNaoConfirmaPrimeiroItemNemAuditoria() throws Exception {
        Cenario c = criarCenarioPersistido();
        Map<String,Object> primeiro = alteracao(c, DATA, "FALTA", "lote-um-" + UUID.randomUUID());
        Map<String,Object> segundo = alteracao(c, DATA.plusDays(1), "STATUS_INEXISTENTE", "lote-dois-" + UUID.randomUUID());
        executar(c.localA, post("/api/frequencias/lote").contentType(APPLICATION_JSON)
                .content(json(List.of(primeiro, segundo))), 400, "Falha do lote deve reverter o conjunto completo");
        emTransacao(() -> {
            assertEquals(0L, em.createQuery("select count(f) from Frequencia f where f.vinculoContratual.id=:v", Long.class)
                    .setParameter("v", c.vinculoA.getId()).getSingleResult());
            assertEquals(0L, em.createQuery("select count(e) from EventoFrequencia e where e.frequencia.vinculoContratual.id=:v", Long.class)
                    .setParameter("v", c.vinculoA.getId()).getSingleResult()); return null;
        });
    }

    private Map<String,Object> comando(Cenario c, LocalDate data, List<?> alteracoes) {
        return Map.of("dataReferencia", data.toString(), "lotacaoId", c.lotacaoA.getId(), "jornadaId", c.jornadaA.getId(),
                "contratoIds", List.of(c.contratoA.getId()), "alteracoes", alteracoes);
    }
    private Map<String,Object> alteracao(Cenario c, LocalDate data, String status, String chave) {
        return Map.of("vinculoContratualId", c.vinculoA.getId(), "dataReferencia", data.toString(), "status", status,
                "justificativaRetificacao", "Justificativa formal do item de fechamento", "chaveIdempotencia", chave);
    }
    private long criarFechamento(Cenario c, LocalDate data, UsuarioAplicacao usuario, FechamentoFrequenciaStatus status) {
        return emTransacao(() -> {
            FechamentoFrequencia f = new FechamentoFrequencia();
            f.setChaveEscopo(FechamentoFrequenciaServico.montarChaveEscopo(data, c.lotacaoA.getId(), c.jornadaA.getId(), List.of(c.contratoA.getId())));
            f.setDataReferencia(data); f.setLotacao(em.getReference(Lotacao.class, c.lotacaoA.getId()));
            f.setJornada(em.getReference(Jornada.class, c.jornadaA.getId()));
            f.setUsuarioCriador(em.getReference(UsuarioAplicacao.class, usuario.getId()));
            f.setContratoIds(c.contratoA.getId().toString()); f.setStatus(status); em.persist(f); em.flush(); return f.getId();
        });
    }
    private JsonNode aguardar(UsuarioAplicacao usuario, long id) throws Exception {
        long limite = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(25);
        JsonNode estado;
        do {
            estado = dados(executar(usuario, get("/api/frequencias/fechamentos/{id}", id), 200,
                    "Consulta do progresso do fechamento"));
            if (Set.of("CONCLUIDO", "ERRO", "CANCELADO").contains(estado.path("status").asText())) return estado;
            Thread.sleep(100);
        } while (System.nanoTime() < limite);
        return fail("Fechamento não concluiu em 25s: " + estado);
    }
}
