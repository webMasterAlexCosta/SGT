package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Controles HTTP de ALO-006/FRE-004/015: início do turno e trilha operacional real. */
class FrequenciaTemporalBordasHttpIT extends AuditoriaHttpSuporte {
    @Test
    void jornada22As06PermaneceNaAlocacaoDoInicioMesmoComMudancaNoDiaSeguinte() throws Exception {
        Cenario c = criarCenario();
        c.jornadaA.getDias().forEach(d -> {
            d.setDiaTrabalho(true); d.setEntrada1("22:00"); d.setSaida1("06:00");
            d.setEntrada2(null); d.setSaida2(null);
        });
        // A nova alocação captura o turno noturno no momento de sua publicação.
        c.pessoaA = pessoa("Pessoa do plantão " + UUID.randomUUID(), cpf(820000000 + c.pessoaA.getId().intValue()));
        c.vinculoA = vinculo(c.pessoaA, c.contratoA);
        c.alocacaoA = alocacao(c.vinculoA, c.composicaoA);
        c.empregadoA = projecao.projetar(c.vinculoA, c.alocacaoA);
        Jornada diurna = new Jornada();
        diurna.setDescricao("Turno diurno após plantão " + UUID.randomUUID());
        diurna.setGestaoLocal(c.gestaoA);
        for (java.time.DayOfWeek dia : java.time.DayOfWeek.values()) {
            JornadaDiaSemana d = new JornadaDiaSemana(); d.setJornada(diurna); d.setDiaSemana(dia);
            d.setDiaTrabalho(dia.getValue() <= 5);
            if (dia.getValue() <= 5) { d.setEntrada1("08:00"); d.setSaida1("12:00"); d.setEntrada2("13:00"); d.setSaida2("17:00"); }
            diurna.getDias().add(d);
        }
        em.persist(diurna);
        Lotacao destino = new Lotacao("Destino após meia-noite " + UUID.randomUUID());
        destino.setGestaoLocal(c.gestaoA); destino.setMunicipio(c.lotacaoA.getMunicipio());
        destino.getTurnos().add(diurna); em.persist(destino);
        em.flush();
        // O cenário comprova a disponibilidade das duas novas entidades desde
        // o dia da movimentação. A flag atual, sozinha, não prova o passado.
        for (var catalogo : java.util.List.of(
                java.util.Map.entry(com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal.JORNADA, diurna.getId()),
                java.util.Map.entry(com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal.LOTACAO, destino.getId()))) {
            EventoDisponibilidadeCatalogo origem = new EventoDisponibilidadeCatalogo();
            origem.setTipoCatalogo(catalogo.getKey()); origem.setCatalogoId(catalogo.getValue());
            origem.setDataEfetiva(DATA.plusDays(1)); origem.setAtivo(true);
            origem.setMotivo("Fonte sintética explícita do destino diurno a partir da movimentação");
            em.persist(origem);
        }
        ComposicaoContratual composicao = composicao(c.contratoA, destino, diurna, c.cargoA, 5);
        em.flush();
        Map<String, Object> mudanca = new LinkedHashMap<>(Map.of(
                "dataEfetiva", DATA.plusDays(1).toString(), "composicaoId", composicao.getId(),
                "justificativa", "Mudança após o dia de início do plantão", "evidencia", "Ato de movimentação",
                "autorizacao", "Despacho do Gestor Local"));
        String rota = "/api/vinculos-contratuais/" + c.vinculoA.getId();
        JsonNode previa = dados(executar(c.localA, post(rota + "/previas/alocacoes")
                .contentType(APPLICATION_JSON).content(json(mudanca)), 200, "Prévia da mudança no dia seguinte"));
        mudanca.put("confirmarImpacto", true); mudanca.put("tokenPrevia", previa.path("token").asText());
        JsonNode nova = dados(executar(c.localA, post(rota + "/alocacoes")
                .contentType(APPLICATION_JSON).content(json(mudanca)), 200, "Movimentação temporal confirmada"));
        assertNotEquals(c.alocacaoA.getId().longValue(), nova.path("id").asLong());

        Map<String, Object> lancamento = new LinkedHashMap<>(Map.of(
                "vinculoContratualId", c.vinculoA.getId(), "dataReferencia", DATA.toString(),
                "status", "PRESENCA", "entrada1", "22:00", "saida1", "06:00",
                "justificativaRetificacao", "Plantão comprovado com início no dia anterior à movimentação",
                "chaveIdempotencia", "plantao-fronteira-" + UUID.randomUUID()));
        JsonNode frequencia = dados(executar(c.localA, post("/api/frequencias")
                .contentType(APPLICATION_JSON).content(json(lancamento)), 200, "Plantão atravessa a meia-noite"));
        assertEquals(DATA.toString(), frequencia.path("dataReferencia").asText());
        assertEquals(c.alocacaoA.getId().longValue(), frequencia.path("alocacaoVinculoId").asLong());
        assertEquals(c.lotacaoA.getId().longValue(), frequencia.path("lotacaoId").asLong());
        assertEquals(c.jornadaA.getId().longValue(), frequencia.path("jornadaId").asLong());
        em.flush(); em.clear();
        Frequencia gravada = em.find(Frequencia.class, frequencia.path("id").asLong());
        assertEquals(480, gravada.getMinutosFisicos());
        JsonNode relatorio = dados(executar(c.localA, get("/api/relatorios/frequencia-consolidada")
                .param("inicio", DATA.toString()).param("fim", DATA.plusDays(1).toString())
                .param("contratoIds", c.contratoA.getId().toString()), 200, "Apuração diária das duas alocações"));
        JsonNode antiga = null, posterior = null;
        for (JsonNode linha : relatorio.path("registros")) {
            if (linha.path("empregadoId").asLong() != c.empregadoA.getId()) continue;
            if (linha.path("lotacaoId").asLong() == c.lotacaoA.getId()) antiga = linha;
            if (linha.path("lotacaoId").asLong() == destino.getId()) posterior = linha;
        }
        assertNotNull(antiga); assertNotNull(posterior);
        assertEquals(480, antiga.path("diario").path(DATA.toString()).path("minutosFisicos").asInt());
        assertTrue(antiga.path("diario").path(DATA.toString()).path("lancado").asBoolean());
        assertFalse(posterior.path("diario").path(DATA.plusDays(1).toString()).path("lancado").asBoolean());
        assertEquals(0, posterior.path("totais").path("minutosFisicos").asInt(),
                "A alocação do dia de saída não recebe parcela indevida do plantão iniciado antes.");
    }

    @Test
    void alteracaoOperacionalNaJanelaPreservaAntesDepoisEAutorOriginal() throws Exception {
        Cenario c = criarCenario();
        LocalDate data = DataJornadaSintetica.ultimoDiaDeTrabalhoAnterior(c.jornadaA);
        Map<String, Object> req = new LinkedHashMap<>(Map.of("vinculoContratualId", c.vinculoA.getId(),
                "dataReferencia", data.toString(), "status", "FALTA"));
        JsonNode original = dados(executar(c.operacionalA, post("/api/frequencias")
                .contentType(APPLICATION_JSON).content(json(req)), 200, "Primeiro registro operacional"));
        req.put("status", "PRESENCA");
        JsonNode alterada = dados(executar(c.operacionalA, post("/api/frequencias")
                .contentType(APPLICATION_JSON).content(json(req)), 200, "Alteração operacional dentro dos sete dias"));
        assertEquals("ABERTA", alterada.path("estadoNegocial").asText());
        assertEquals("ALTERADA_OPERACIONAL", alterada.path("estadoAuditoria").asText());
        JsonNode eventos = dados(executar(c.localA, get("/api/frequencias/{id}/eventos", original.path("id").asLong()),
                200, "Auditoria preserva a alteração do Operacional"));
        assertEquals(2, eventos.size());
        assertTrue(eventos.get(1).path("snapshotAnterior").asText().contains("status=FALTA"));
        assertTrue(eventos.get(1).path("snapshotNovo").asText().contains("status=PRESENCA"));
        assertEquals(c.operacionalA.getId().longValue(), eventos.get(1).path("usuarioId").asLong());
        assertEquals(data.toString(), eventos.get(1).path("dataReferencia").asText());
        assertFalse(eventos.get(1).path("registradoEm").asText().isBlank());
        assertEquals(eventos.get(0).path("hashEvento").asText(), eventos.get(1).path("hashAnterior").asText());
        em.flush(); em.clear();
        assertEquals(c.operacionalA.getId(), em.find(Frequencia.class, original.path("id").asLong()).getAutorOriginalId());
    }
}
