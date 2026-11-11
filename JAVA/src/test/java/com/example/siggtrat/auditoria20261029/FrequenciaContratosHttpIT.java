package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.Frequencia;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Contratos positivos, filtros, escopo e negativas das seis rotas públicas de frequência. */
class FrequenciaContratosHttpIT extends AuditoriaHttpSuporte {
    @Test void consultasDiariaMensalEIntervaloRetornamIdentidadeCanonicaEEstadosIndependentes() throws Exception {
        Cenario c = criarCenario();
        JsonNode criada = dados(executar(c.localA, post("/api/frequencias").contentType(APPLICATION_JSON)
                        .content(json(payload(c.vinculoA.getId(), DATA, "FALTA", "consulta-" + UUID.randomUUID()))),
                200, "FRE-001 criação canônica"));
        long id = criada.path("id").longValue();
        // TMP-001:002[0]: Pessoa/Vínculo/Contrato/Alocação de D continuam A;
        // somente a projeção cadastral atual passa a apontar para B.
        long empregadoId = c.empregadoA.getId();
        c.empregadoA.getContratos().clear(); c.empregadoA.getContratos().add(c.contratoB);
        c.empregadoA.setGestaoLocal(c.gestaoB); c.empregadoA.setLotacao(c.lotacaoB);
        c.empregadoA.setJornada(c.jornadaB); c.empregadoA.setCargo(c.cargoB);
        em.flush(); em.clear(); // leitura JPA após descarte do cache, não prova commit
        var atual = em.find(com.example.siggtrat.entidade.Empregado.class, empregadoId);
        assertEquals(c.pessoaA.getId(), atual.getPessoaCanonica().getId());
        assertEquals(c.contratoB.getId(), atual.getContratos().get(0).getId());
        assertEquals(c.lotacaoB.getId(), atual.getLotacao().getId());
        assertNotEquals(c.contratoA.getId(), atual.getContratos().get(0).getId());
        assertNotEquals(c.lotacaoA.getId(), atual.getLotacao().getId());
        for (String rota : List.of("?data=" + DATA, "/mensal?mes=6&ano=2026",
                "/intervalo?inicio=" + DATA + "&fim=" + DATA + "&status=FALTA")) {
            JsonNode pagina = dados(executar(c.localA, get("/api/frequencias" + rota)
                            .param("contratoId", c.contratoA.getId().toString()).param("size", "10"),
                    200, "FRE-019 contrato de leitura " + rota));
            assertEquals(1, pagina.path("totalElements").intValue());
            JsonNode item = pagina.path("content").get(0);
            assertEquals(id, item.path("id").longValue());
            assertEquals(c.pessoaA.getId().longValue(), item.path("pessoaId").longValue());
            assertEquals(c.contratoA.getId().longValue(), item.path("contratoId").longValue());
            assertEquals(c.vinculoA.getId().longValue(), item.path("vinculoContratualId").longValue());
            assertEquals(c.alocacaoA.getId().longValue(), item.path("alocacaoVinculoId").longValue());
            assertEquals(c.lotacaoA.getId().longValue(), item.path("lotacaoId").longValue());
            assertEquals("FECHADA", item.path("estadoNegocial").asText());
            assertEquals("VALIDA", item.path("validadeOperacional").asText());
            assertEquals("RETIFICADA_GESTOR", item.path("estadoAuditoria").asText());
            assertTrue(item.path("bloqueadaEdicaoOperacional").asBoolean());
            assertTrue(item.path("prazoEdicao").asText().startsWith(DATA.plusDays(7).toString()));
            JsonNode alheia = dados(executar(c.localB, get("/api/frequencias" + rota)
                            .param("contratoId", c.contratoA.getId().toString()),
                    200, "FRE-015 consulta de outra Gestão não expõe registro"));
            assertEquals(0, alheia.path("content").size());
        }
        JsonNode eventos = dados(executar(c.localA, get("/api/frequencias/{id}/eventos", id),
                200, "FRE-004/FRE-005 eventos com autoria, motivo e hash"));
        assertEquals(1, eventos.size());
        assertEquals(c.localA.getId().longValue(), eventos.get(0).path("usuarioId").longValue());
        assertEquals(DATA.toString(), eventos.get(0).path("dataReferencia").asText());
        assertEquals(64, eventos.get(0).path("hashEvento").asText().length());
        assertTrue(eventos.get(0).path("snapshotNovo").asText().contains("status=FALTA"));
        executar(c.localB, get("/api/frequencias/{id}/eventos", id), 404, "Evento fora do escopo");
    }

    @Test void loteCanonicoCriaDuasDatasESerializaTodosOsResultados() throws Exception {
        Cenario c = criarCenario();
        var primeiro = payload(c.vinculoA.getId(), DATA, "FALTA", "lote-a-" + UUID.randomUUID());
        var segundo = payload(c.vinculoA.getId(), DATA.plusDays(1), "PRESENCA", "lote-b-" + UUID.randomUUID());
        JsonNode resposta = dados(executar(c.localA, post("/api/frequencias/lote").contentType(APPLICATION_JSON)
                .content(json(List.of(primeiro, segundo))), 200, "Lote canônico transacional válido"));
        assertEquals(2, resposta.size());
        assertNotEquals(resposta.get(0).path("id").longValue(), resposta.get(1).path("id").longValue());
        assertEquals("FALTA", resposta.get(0).path("status").asText());
        assertEquals("PRESENCA", resposta.get(1).path("status").asText());
        assertEquals(2L, em.createQuery("select count(f) from Frequencia f where f.vinculoContratual.id = :v", Long.class)
                .setParameter("v", c.vinculoA.getId()).getSingleResult());
    }

    @Test void reenvioIdempotenteNaoDuplicaVersaoENovaIntencaoConflita() throws Exception {
        Cenario c = criarCenario();
        Map<String, Object> corpo = payload(c.vinculoA.getId(), DATA, "FALTA", "idempotencia-" + UUID.randomUUID());
        JsonNode primeira = dados(executar(c.localA, post("/api/frequencias").contentType(APPLICATION_JSON)
                .content(json(corpo)), 200, "Retificação inicial"));
        JsonNode repetida = dados(executar(c.localA, post("/api/frequencias").contentType(APPLICATION_JSON)
                .content(json(corpo)), 200, "Mesmo reenvio mantém versão"));
        assertEquals(primeira.path("id"), repetida.path("id"));
        assertEquals(primeira.path("versaoNegocial"), repetida.path("versaoNegocial"));
        corpo.put("status", "ATESTADO");
        executar(c.localA, post("/api/frequencias").contentType(APPLICATION_JSON).content(json(corpo)),
                409, "Mesma chave com conteúdo diferente");
    }

    @Test void perfilOperacionalEditaNaJanelaEGestorBloqueiaSomenteRegistroRetificado() throws Exception {
        Cenario c = criarCenario();
        LocalDate data = DataJornadaSintetica.ultimoDiaDeTrabalhoAnterior(c.jornadaA);
        Map<String,Object> corpo = payload(c.vinculoA.getId(), data, "FALTA", "janela-" + UUID.randomUUID());
        corpo.remove("justificativaRetificacao"); corpo.remove("chaveIdempotencia");
        JsonNode original = dados(executar(c.operacionalA, post("/api/frequencias").contentType(APPLICATION_JSON)
                .content(json(corpo)), 200, "FRE-002/FRE-004 registro operacional na janela"));
        assertEquals("ABERTA", original.path("estadoNegocial").asText());
        assertEquals("ORIGINAL", original.path("estadoAuditoria").asText());
        corpo.put("status", "PRESENCA");
        JsonNode alterada = dados(executar(c.operacionalA, post("/api/frequencias").contentType(APPLICATION_JSON)
                .content(json(corpo)), 200, "Operacional altera dentro da janela"));
        assertEquals("ALTERADA_OPERACIONAL", alterada.path("estadoAuditoria").asText());
        corpo.put("justificativaRetificacao", "Conferência documental do Gestor Local");
        corpo.put("chaveIdempotencia", "janela-gestor-" + UUID.randomUUID());
        JsonNode retificada = dados(executar(c.localA, post("/api/frequencias").contentType(APPLICATION_JSON)
                .content(json(corpo)), 200, "FRE-006 gestor corrige sem fechar antecipadamente"));
        assertEquals("ABERTA", retificada.path("estadoNegocial").asText());
        assertTrue(retificada.path("bloqueadaEdicaoOperacional").asBoolean());
        assertEquals(3, retificada.path("versaoNegocial").intValue());
        executar(c.operacionalA, post("/api/frequencias").contentType(APPLICATION_JSON).content(json(corpo)),
                403, "FRE-014 frequência corrigida não volta ao Operacional");
    }

    @Test void operacionalNaoCriaRegistroFechadoEGestaoAlheiaNaoEscreve() throws Exception {
        Cenario c = criarCenario();
        String corpo = json(payload(c.vinculoA.getId(), DATA, "FALTA", "escopo-" + UUID.randomUUID()));
        executar(c.operacionalA, post("/api/frequencias").contentType(APPLICATION_JSON).content(corpo),
                403, "FRE-003/FRE-013 Operacional não altera após janela");
        executar(c.localB, post("/api/frequencias").contentType(APPLICATION_JSON).content(corpo),
                403, "FRE-018 Gestão sem autorização sobre alocação histórica");
        executar(c.estadualA, post("/api/frequencias").contentType(APPLICATION_JSON).content(corpo),
                403, "PER-002 Estadual não escreve frequência");
        assertEquals(0L, em.createQuery("select count(f) from Frequencia f where f.vinculoContratual.id = :v", Long.class)
                .setParameter("v", c.vinculoA.getId()).getSingleResult());
    }

    @Test void parametrosInvalidosNaoSaoAceitosComoConsultasOuLancamentosValidos() throws Exception {
        Cenario c = criarCenario();
        executar(c.localA, get("/api/frequencias"), 400, "Data obrigatória na consulta diária");
        executar(c.localA, get("/api/frequencias/mensal").param("mes", "13").param("ano", "2026"),
                400, "Mês inválido");
        executar(c.localA, get("/api/frequencias/intervalo").param("inicio", DATA.plusDays(1).toString())
                .param("fim", DATA.toString()), 400, "Intervalo invertido");
        executar(c.localA, get("/api/frequencias/999999999/eventos"), 404, "Evento inexistente");
        executar(c.localA, post("/api/frequencias").contentType(APPLICATION_JSON).content(json(Map.of(
                "empregado", Map.of("id", c.empregadoA.getId()), "dataReferencia", DATA.toString(), "status", "FALTA"))),
                400, "Contrato público exige vínculo canônico");
        executar(c.localA, post("/api/frequencias/lote").contentType(APPLICATION_JSON).content("[]"),
                400, "Lote vazio rejeitado");
        Map<String,Object> semMotivo = payload(c.vinculoA.getId(), DATA, "PRESENCA", "sem-motivo-" + UUID.randomUUID());
        semMotivo.remove("justificativaRetificacao");
        executar(c.localA, post("/api/frequencias").contentType(APPLICATION_JSON).content(json(semMotivo)),
                400, "Retificação gerencial exige justificativa");
    }

    private Map<String,Object> payload(Long vinculo, LocalDate data, String status, String chave) {
        Map<String,Object> p = new LinkedHashMap<>();
        p.put("vinculoContratualId", vinculo); p.put("dataReferencia", data.toString()); p.put("status", status);
        p.put("justificativaRetificacao", "Conferência documental autorizada pelo Gestor Local");
        p.put("chaveIdempotencia", chave); return p;
    }
}
