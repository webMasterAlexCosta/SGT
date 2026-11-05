package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.frequencia.EstadoAuditoriaFrequencia;
import com.example.siggtrat.dominio.frequencia.ValidadeOperacionalFrequencia;
import com.example.siggtrat.entidade.Frequencia;
import com.example.siggtrat.servico.PendenciaAdministrativaServico;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** As dez rotas de emissão/pendências são exercitadas com conteúdo e transições reais. */
class RelatoriosAdministrativosPendenciasHttpIT extends AuditoriaHttpSuporte {
    @Autowired PendenciaAdministrativaServico pendencias;

    @Test void emissaoPreservaSnapshotConhecimentoAutoriaERetificacaoOficial() throws Exception {
        Cenario c = criarCenario();
        registrarFrequencia(c, "FALTA");
        Map<String,Object> filtro = filtro(c);
        JsonNode previa = dados(executar(c.localA, post("/api/relatorios/administrativos/previa")
                .contentType(APPLICATION_JSON).content(json(filtro)), 200, "REL-001 prévia não formalizada"));
        assertEquals("PREVIA", previa.path("metadados").path("situacao").asText());
        assertEquals(1, previa.path("conteudo").path("totais").path("faltas").intValue());
        JsonNode primeira = dados(executar(c.localA, post("/api/relatorios/administrativos/emissoes")
                .contentType(APPLICATION_JSON).content(json(filtro)), 201, "REL-005 emissão formal original"));
        JsonNode meta = primeira.path("metadados");
        long id = meta.path("id").longValue();
        assertEquals(1, meta.path("versao").intValue());
        assertEquals("ORIGINAL", meta.path("natureza").asText());
        assertEquals(c.localA.getId().longValue(), meta.path("usuarioId").longValue());
        assertEquals(DATA.toString(), meta.path("inicio").asText());
        assertEquals(64, meta.path("hashConteudo").asText().length());
        assertTrue(meta.path("tamanhoConteudoBytes").longValue() > 100);
        assertNotNull(Instant.parse(meta.path("geradoEm").asText()));
        Instant conhecidaAte = Instant.now();
        JsonNode repetida = dados(executar(c.localA, post("/api/relatorios/administrativos/emissoes")
                .contentType(APPLICATION_JSON).content(json(filtro)), 201, "Emissão idempotente retorna mesma prova"));
        assertEquals(id, repetida.path("metadados").path("id").longValue());

        registrarFrequencia(c, "PRESENCA");
        JsonNode historica = dados(executar(c.localA, get("/api/relatorios/administrativos/emissoes/{id}", id),
                200, "REL-006 emissão anterior fica SUPERADA sem perder conteúdo"));
        assertEquals("SUPERADA", historica.path("metadados").path("estado").asText());
        assertEquals(primeira.path("conteudo"), historica.path("conteudo"));
        JsonNode comparacao = dados(executar(c.localA,
                get("/api/relatorios/administrativos/emissoes/{id}/comparacao-vigente", id),
                200, "REL-003 comparação separa prova histórica e verdade vigente"));
        assertTrue(comparacao.path("alterado").asBoolean());
        assertEquals(1, comparacao.path("conteudoHistorico").path("totais").path("faltas").intValue());
        assertEquals(1, comparacao.path("conteudoVigente").path("totais").path("presencas").intValue());
        filtro.put("chaveIdempotencia", "emissao-retificada-" + UUID.randomUUID());
        JsonNode segunda = dados(executar(c.localA, post("/api/relatorios/administrativos/emissoes")
                .contentType(APPLICATION_JSON).content(json(filtro)), 201, "REL-002 emissão retificada após correção"));
        assertEquals(2, segunda.path("metadados").path("versao").intValue());
        assertEquals("RETIFICADA", segunda.path("metadados").path("natureza").asText());
        assertEquals(id, segunda.path("metadados").path("emissaoAnteriorId").longValue());
        JsonNode conhecida = dados(executar(c.localA, post("/api/relatorios/administrativos/emissoes/conhecimento")
                .param("conhecidoAte", conhecidaAte.toString()).contentType(APPLICATION_JSON).content(json(filtro)),
                200, "REL-003 consulta do que era conhecido antes da retificação"));
        assertEquals(id, conhecida.path("metadados").path("id").longValue());
        assertEquals(primeira.path("conteudo"), conhecida.path("conteudo"));
        JsonNode lista = dados(executar(c.localA, get("/api/relatorios/administrativos/emissoes")
                .param("inicio", DATA.toString()).param("fim", DATA.toString()).param("tipo", "FREQUENCIA_CONSOLIDADA"),
                200, "Consulta paginada das duas emissões administrativas"));
        assertEquals(2, lista.path("totalElements").intValue());
        executar(c.localB, get("/api/relatorios/administrativos/emissoes/{id}", id), 404, "Emissão fora da Gestão");
        executar(c.localB, get("/api/relatorios/administrativos/emissoes/{id}/comparacao-vigente", id),
                404, "Comparação não expõe emissão alheia");
        assertEquals(0, dados(executar(c.localB, get("/api/relatorios/administrativos/emissoes"),
                200, "Listagem de outra Gestão não contém emissões alheias")).path("content").size());
    }

    @Test void emissoesRejeitamPerfilEscritorIndevidoIntencaoDivergenteEFiltrosInvalidos() throws Exception {
        Cenario c = criarCenario();
        Map<String,Object> filtro = filtro(c);
        executar(c.presidente, post("/api/relatorios/administrativos/emissoes")
                .contentType(APPLICATION_JSON).content(json(filtro)), 403, "Presidente não formaliza emissão");
        executar(c.operacionalA, post("/api/relatorios/administrativos/previa")
                .contentType(APPLICATION_JSON).content(json(filtro)), 403, "Operacional não consulta relatório gerencial");
        executar(c.localA, post("/api/relatorios/administrativos/emissoes")
                .contentType(APPLICATION_JSON).content(json(filtro)), 201, "Emissão para teste de intenção");
        filtro.put("fim", DATA.plusDays(1).toString());
        executar(c.localA, post("/api/relatorios/administrativos/emissoes")
                .contentType(APPLICATION_JSON).content(json(filtro)), 409, "Idempotência não aceita filtros diferentes");
        filtro.put("inicio", DATA.plusDays(3).toString());
        executar(c.localA, post("/api/relatorios/administrativos/previa")
                .contentType(APPLICATION_JSON).content(json(filtro)), 400, "Prévia rejeita intervalo invertido");
        executar(c.localA, get("/api/relatorios/administrativos/emissoes/999999999"), 404, "Emissão inexistente");
        executar(c.localA, get("/api/relatorios/administrativos/emissoes/999999999/comparacao-vigente"),
                404, "Comparação de emissão inexistente");
        executar(c.localA, post("/api/relatorios/administrativos/emissoes/conhecimento")
                .param("conhecidoAte", "2020-01-01T00:00:00Z").contentType(APPLICATION_JSON).content(json(filtro(c))),
                404, "Nenhuma emissão era conhecida antes da criação");
        executar(c.localA, get("/api/relatorios/administrativos/emissoes").param("tipo", "INEXISTENTE"),
                400, "Enum inválido de relatório");
    }

    @Test void pendenciaMantemOrigemEscopoAvisoETemResolucaoSomenteDepoisDaRetificacao() throws Exception {
        Cenario c = criarCenario();
        JsonNode frequencia = registrarFrequencia(c, "FALTA");
        Frequencia f = em.find(Frequencia.class, frequencia.path("id").longValue());
        f.setEstadoAuditoria(EstadoAuditoriaFrequencia.SUJEITA_REVISAO);
        f.setValidadeOperacional(ValidadeOperacionalFrequencia.SUJEITA_REVISAO);
        f.setBloqueadaEdicaoOperacional(true);
        var pendencia = pendencias.registrarRevisaoFrequencia(f, "Impacto retroativo sobre alocação");
        em.flush();
        long id = pendencia.getId();
        JsonNode lista = dados(executar(c.localA, get("/api/pendencias").param("estado", "ABERTA")
                .param("contratoIds", c.contratoA.getId().toString()), 200, "AUD-006 pendência com origem e responsável"));
        assertEquals(1, lista.path("totalElementos").intValue());
        assertEquals(id, lista.path("registros").get(0).path("id").longValue());
        assertEquals("RETIFICACAO_FREQUENCIA", lista.path("registros").get(0).path("tipo").asText());
        JsonNode resumo = dados(executar(c.localA, get("/api/pendencias/resumo")
                .param("contratoIds", c.contratoA.getId().toString()), 200, "Resumo de pendência no escopo"));
        assertEquals(1, resumo.path("abertas").longValue());
        assertEquals(1, resumo.path("retificacoesFrequencia").longValue());
        assertEquals(0, dados(executar(c.localB, get("/api/pendencias"), 200,
                "Gestão B não vê pendência de A")).path("registros").size());
        assertEquals(0, dados(executar(c.localB, get("/api/pendencias/resumo"), 200,
                "Resumo de Gestão B não conta pendência de A")).path("abertas").longValue());
        JsonNode previa = dados(executar(c.localA, post("/api/relatorios/administrativos/previa")
                .contentType(APPLICATION_JSON).content(json(filtro(c))), 200, "FRE-011/REL-004 aviso durante revisão"));
        assertTrue(previa.path("metadados").path("sujeitoARevisao").asBoolean());
        assertEquals(1, previa.path("metadados").path("quantidadePendencias").longValue());
        executar(c.localB, post("/api/pendencias/{id}/iniciar-tratamento", id), 404, "Gestão alheia não trata pendência");
        executar(c.estadualA, post("/api/pendencias/{id}/confirmar-resolucao", id), 403,
                "Estadual não executa tratamento negocial");
        JsonNode iniciada = dados(executar(c.localA, post("/api/pendencias/{id}/iniciar-tratamento", id)
                .contentType(APPLICATION_JSON).content(json(Map.of("observacao", "Documentação em conferência"))),
                200, "Transição ABERTA para EM_TRATAMENTO"));
        assertEquals("EM_TRATAMENTO", iniciada.path("estado").asText());
        executar(c.localA, post("/api/pendencias/{id}/confirmar-resolucao", id), 409,
                "Não resolver antes de corrigir a frequência de origem");
        registrarFrequencia(c, "PRESENCA");
        JsonNode resolvida = dados(executar(c.localA, post("/api/pendencias/{id}/confirmar-resolucao", id)
                .contentType(APPLICATION_JSON).content(json(Map.of("observacao", "Retificação conferida"))),
                200, "Resolução confirmada após origem corrigida"));
        assertEquals("RESOLVIDA", resolvida.path("estado").asText());
        executar(c.localA, post("/api/pendencias/{id}/iniciar-tratamento", id), 409,
                "Pendência resolvida não reinicia tratamento");
    }

    @Test void pendenciasRejeitamEnumPeriodoEIdentificadorInvalidos() throws Exception {
        Cenario c = criarCenario();
        executar(c.localA, get("/api/pendencias").param("estado", "IGNORADA"), 400, "Estado inválido de pendência");
        executar(c.localA, get("/api/pendencias/resumo").param("inicio", DATA.plusDays(1).toString())
                .param("fim", DATA.toString()), 400, "Resumo com período invertido");
        executar(c.localA, post("/api/pendencias/999999999/iniciar-tratamento"), 404, "Pendência inexistente para início");
        executar(c.localA, post("/api/pendencias/999999999/confirmar-resolucao"), 404, "Pendência inexistente para resolução");
    }

    private Map<String,Object> filtro(Cenario c) {
        Map<String,Object> r = new LinkedHashMap<>();
        r.put("tipo", "FREQUENCIA_CONSOLIDADA"); r.put("formato", "VISUALIZACAO");
        r.put("inicio", DATA.toString()); r.put("fim", DATA.toString()); r.put("contratoIds", List.of(c.contratoA.getId()));
        r.put("chaveIdempotencia", "relatorio-auditoria-" + UUID.randomUUID()); return r;
    }

    private JsonNode registrarFrequencia(Cenario c, String status) throws Exception {
        return dados(executar(c.localA, post("/api/frequencias").contentType(APPLICATION_JSON).content(json(Map.of(
                "vinculoContratualId", c.vinculoA.getId(), "dataReferencia", DATA.toString(), "status", status,
                "justificativaRetificacao", "Conferência documental de resultado consolidado",
                "chaveIdempotencia", "frequencia-relatorio-" + UUID.randomUUID()))), 200, "Retificação que alimenta emissão"));
    }
}
