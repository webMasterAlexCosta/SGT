package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.*;
import com.example.siggtrat.dominio.frequencia.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.http.MediaType.APPLICATION_JSON;

/** RED executável contra main42: somente APIs e entidades já existentes. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class OcorrenciaPrimeiroLancamento43HttpIT extends Ocorrencia43HttpBase {
    @Test
    void diaQueChegouRecebeAtestadoPreserva480MinAtorNormalOrdemEReplay() throws Exception {
        Cenario c = cenario43(); LocalDate hoje = LocalDate.now(ZONA43);
        Fonte43 a = fonteAnteriorComDiaHoje(c, "ATESTADO"); Fonte43 b = fonteAnteriorComDiaHoje(c, " atestado ");
        assertEquals(0L, quantidade43(c.vinculoA.getId(), hoje), "Cauda ainda não produziu frequência na criação anterior");
        var corpo = presenca43(c.vinculoA.getId(), hoje, "f43-normal-" + UUID.randomUUID());
        gravar43(c.operacionalB, corpo, 403);
        JsonNode criada = gravar43(c.operacionalA, corpo, 200); long id = criada.path("id").asLong();
        assertEquals("ATESTADO", criada.path("status").asText()); fisico48043(id);
        assertEquals("ORIGINAL", criada.path("estadoAuditoria").asText());
        assertFalse(criada.path("bloqueadaEdicaoOperacional").asBoolean());
        String obs = criada.path("observacao").asText();
        assertTrue(obs.contains("Ocorrência ID: " + a.id()));
        assertTrue(obs.contains("Ocorrência ID: " + b.id()));
        assertTrue(obs.indexOf("Ocorrência ID: " + b.id()) < obs.indexOf("Ocorrência ID: " + a.id()), "Empate de prioridade/data tem IDdecrescente estável");
        assertFalse(obs.contains(a.observacao())); assertFalse(obs.contains(b.observacao()));
        JsonNode eventos = consultar(c.localA, "/api/frequencias/" + id + "/eventos", "Autor real do primeiro comando");
        assertEquals(1, eventos.size()); assertEquals("REGISTRO", eventos.get(0).path("tipoEvento").asText());
        assertEquals(c.operacionalA.getId().longValue(), eventos.get(0).path("usuarioId").asLong());
        String antes = snapshot43(id); List<String> hashes = hashes43(id);
        assertEquals(id, gravar43(c.operacionalA, corpo, 200).path("id").asLong());
        assertEquals(antes, snapshot43(id)); assertEquals(hashes, hashes43(id));
        executar(c.localA, delete("/api/ocorrencias/" + b.id()), 204, "Retira fonte de mesma prioridade sem perder a restante");
        assertEquals("ATESTADO", status43(id)); fisico48043(id);
        executar(c.localA, delete("/api/ocorrencias/" + a.id()), 204, "Controle ROP023: fonte válida restaura presença cabível");
        assertEquals("PRESENCA", status43(id)); fisico48043(id);
    }

    @Test
    void fechamentoPeloOperacionalUsaMesmoComandoSemAdquirirAutoridadeDeGestor() throws Exception {
        Cenario c = cenario43(); LocalDate hoje = LocalDate.now(ZONA43);
        Fonte43 fonte = fonteAnteriorComDiaHoje(c, "ATESTADO");
        assertEquals(0L, quantidade43(c.vinculoA.getId(), hoje));
        JsonNode aceito = dados(executar(c.operacionalA, post("/api/frequencias/fechamentos").contentType(APPLICATION_JSON)
                .content(json(mapa("dataReferencia", hoje, "lotacaoId", c.lotacaoA.getId(), "jornadaId", c.jornadaA.getId(),
                        "contratoIds", List.of(c.contratoA.getId()), "alteracoes", List.of()))), 202, "Fechamento normal autorizado"));
        JsonNode fim = aguardar43(c.operacionalA, aceito.path("id").asLong());
        assertEquals("CONCLUIDO", fim.path("status").asText(), fim.toString()); assertEquals(1L, fim.path("totalInserido").asLong());
        long id = frequencia43(c.vinculoA.getId(), hoje); assertEquals("ATESTADO", status43(id));
        emTransacao(() -> {
            Frequencia f = em.find(Frequencia.class, id);
            assertEquals(c.operacionalA.getId(), f.getAutorOriginalId()); assertFalse(Boolean.TRUE.equals(f.getBloqueadaEdicaoOperacional()));
            assertEquals(EstadoAuditoriaFrequencia.ORIGINAL, f.getEstadoAuditoria());
            assertNull(em.find(Ocorrencia.class, fonte.id()).getCanceladoEm()); return null;
        });
    }

    @Test
    void correcaoManualReplayECancelamentoPreservamDecisaoMesmoComOutraOcorrenciaAtiva() throws Exception {
        Cenario c = cenario43(); LocalDate hoje = LocalDate.now(ZONA43);
        var inicial = presenca43(c.vinculoA.getId(), hoje, "f43-presenca-" + UUID.randomUUID());
        inicial.put("justificativaRetificacao", "Presença anterior aos atos de ocorrência");
        long id = gravar43(c.localA, inicial, 200).path("id").asLong();
        Fonte43 a = criarFonte43(c, hoje, "ATESTADO", 1); Fonte43 b = criarFonte43(c, hoje, "SUSPENSAO", 1);
        assertEquals("SUSPENSAO", status43(id), "Prioridade vigente preservada");
        var manual = presenca43(c.vinculoA.getId(), hoje, "f43-correcao-" + UUID.randomUUID());
        manual.put("justificativaRetificacao", "Correção manual posterior aos fatos administrativos");
        gravar43(c.localA, manual, 200); assertEquals("PRESENCA", status43(id));
        String antes = snapshot43(id); List<String> hashes = hashes43(id);
        gravar43(c.localA, manual, 200); assertEquals(antes, snapshot43(id)); assertEquals(hashes, hashes43(id));
        assertEquals(a.id(), reenviarFonte43(c, a).path("id").asLong());
        assertEquals(antes, snapshot43(id)); assertEquals(hashes, hashes43(id));
        long historia = historicosFonte43(a.id());
        executar(c.localA, delete("/api/ocorrencias/" + a.id()), 204, "Cancelar fonte não é nova intervenção sobre frequência manual");
        assertAll(
                () -> assertEquals("PRESENCA", status43(id)),
                () -> assertEquals(antes, snapshot43(id)),
                () -> assertEquals(hashes, hashes43(id)),
                () -> assertEquals(historia + 1, historicosFonte43(a.id())));
        emTransacao(() -> {
            Frequencia f = em.find(Frequencia.class, id);
            assertFalse(Boolean.TRUE.equals(f.getOcorrenciaOverrideAtivo())); assertNull(f.getStatusBaseAntesOcorrencia());
            assertNull(em.find(Ocorrencia.class, b.id()).getCanceladoEm(), "Outra fonte ativa não reabre projeção automática"); return null;
        });
        fisico48043(id);
    }

    @Test
    void cancelamentoAutorizadoPreservaFotografiaInvalidaAposRetificacaoDeInicio() throws Exception {
        Cenario c = cenario43();
        var original = presenca43(c.vinculoA.getId(), DATA, "f43-historica-" + UUID.randomUUID());
        original.put("justificativaRetificacao", "Frequência histórica anterior à retificação do vínculo");
        long id = gravar43(c.localA, original, 200).path("id").asLong();
        Fonte43 fonte = criarFonte43(c, DATA, "ATESTADO", 1); fisico48043(id);
        var req = mapa("novoInicio", DATA.plusDays(1), "justificativa", "Retificação sintética que retira cobertura da data anterior",
                "evidencia", "SEI-F43-INICIO", "autorizacao", "ATO-F43-INICIO");
        JsonNode previa = enviar(c.localA, "/api/vinculos-contratuais/" + c.vinculoA.getId() + "/previas/retificacao-inicio", req, 200, "Prévia real de retirada de cobertura");
        req.put("tokenPrevia", previa.path("token").asText()); req.put("confirmarImpacto", true);
        enviar(c.localA, "/api/vinculos-contratuais/" + c.vinculoA.getId() + "/retificar-inicio", req, 200, "Retificação real preserva frequência inválida");
        emTransacao(() -> {
            Frequencia f = em.find(Frequencia.class, id);
            assertTrue(f.getValidadeOperacional() == ValidadeOperacionalFrequencia.INVALIDA_COBERTURA
                    || f.getValidadeOperacional() == ValidadeOperacionalFrequencia.SUJEITA_REVISAO
                    || f.getEstadoAuditoria() == EstadoAuditoriaFrequencia.SUJEITA_REVISAO, "Pré-condição explícita do domínio"); return null;
        });
        String foto = snapshot43(id); List<String> hashes = hashes43(id); List<String> contextos = fotosFonte43(fonte.id());
        long historia = historicosFonte43(fonte.id());
        executar(c.localB, delete("/api/ocorrencias/" + fonte.id()), 403, "A prova não concede autoridade à outra Gestão");
        executar(c.localA, delete("/api/ocorrencias/" + fonte.id()), 204, "Gestor autorizado pode retirar a fonte sem fabricar cobertura");
        assertEquals(foto, snapshot43(id)); assertEquals(hashes, hashes43(id)); fisico48043(id);
        assertEquals(contextos, fotosFonte43(fonte.id())); assertEquals(historia + 1, historicosFonte43(fonte.id()));
        emTransacao(() -> { assertNotNull(em.find(Ocorrencia.class, fonte.id()).getCanceladoEm()); return null; });
    }
}
