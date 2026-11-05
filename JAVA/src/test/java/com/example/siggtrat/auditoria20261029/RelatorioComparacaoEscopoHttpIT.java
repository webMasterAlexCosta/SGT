package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.frequencia.EstadoAuditoriaFrequencia;
import com.example.siggtrat.dominio.frequencia.ValidadeOperacionalFrequencia;
import com.example.siggtrat.entidade.EmissaoRelatorioAdministrativo;
import com.example.siggtrat.entidade.Frequencia;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.servico.PendenciaAdministrativaServico;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** REL-001/004/006 e FRE-011: a comparação conserva o recorte publicado e seu aviso. */
class RelatorioComparacaoEscopoHttpIT extends AuditoriaHttpSuporte {
    @Autowired PendenciaAdministrativaServico pendencias;

    @ParameterizedTest
    @CsvSource({"FREQUENCIA_CONSOLIDADA,NACIONAL", "FREQUENCIA_CONSOLIDADA,PRESIDENTE",
            "ABSENTEISMO,NACIONAL", "ABSENTEISMO,PRESIDENTE"})
    void leitorNacionalNaoAmpliaEmissaoLocalSemFiltroDeContratos(String tipo, String perfil) throws Exception {
        Cenario c = criarCenario();
        registrar(c, false, "FALTA");
        registrar(c, true, "FALTA");
        JsonNode original = emitir(c.localA, filtro(tipo));
        long id = original.path("metadados").path("id").asLong();
        String hash = original.path("metadados").path("hashConteudo").asText();
        assertEquals(1, linhas(original.path("conteudo")).size(), "A emissão cobre somente a Gestão A.");
        em.flush(); em.clear();
        byte[] bytes = em.find(EmissaoRelatorioAdministrativo.class, id).getConteudoSnapshotGzip().clone();
        UsuarioAplicacao leitor = perfil.equals("PRESIDENTE") ? c.presidente : c.nacional;

        JsonNode semMudanca = comparar(leitor, id);
        assertAll(
                () -> assertEquals(1, linhas(semMudanca.path("conteudoVigente")).size(),
                        "O leitor nacional deve reconstruir o escopo local gravado, sem acrescentar a Gestão B."),
                () -> assertFalse(semMudanca.path("alterado").asBoolean(),
                        "Mudar o perfil leitor não representa alteração da fonte publicada."),
                () -> assertEquals(c.empregadoA.getId().longValue(), linhas(semMudanca.path("conteudoVigente"))
                        .get(0).path("empregadoId").asLong()),
                () -> assertEquals(hash, semMudanca.path("hashHistorico").asText()));

        registrar(c, true, "PRESENCA");
        JsonNode somenteOutraGestao = comparar(leitor, id);
        assertFalse(somenteOutraGestao.path("alterado").asBoolean(),
                "Retificação na Gestão B não altera o universo publicado pela Gestão A.");
        registrar(c, false, "PRESENCA");
        JsonNode alterada = comparar(leitor, id);
        assertTrue(alterada.path("alterado").asBoolean(), "A mudança real da Gestão A deve ser detectada.");
        assertEquals(1, linhas(alterada.path("conteudoVigente")).size());
        em.flush(); em.clear();
        EmissaoRelatorioAdministrativo preservada = em.find(EmissaoRelatorioAdministrativo.class, id);
        assertEquals(hash, preservada.getHashConteudo());
        assertArrayEquals(bytes, preservada.getConteudoSnapshotGzip(), "Comparar jamais reescreve a prova original.");
        executar(c.localB, get("/api/relatorios/administrativos/emissoes/{id}/comparacao-vigente", id),
                404, "O escopo fixo não concede acesso a outra Gestão Local");
    }

    @ParameterizedTest
    @ValueSource(strings = {"FREQUENCIA_CONSOLIDADA", "ABSENTEISMO"})
    void presidenciaRecebeAvisoDeRevisaoComContagemRealDoRecorte(String tipo) throws Exception {
        Cenario c = criarCenario();
        long pendencia = marcarRevisao(c, false);
        marcarRevisao(c, true);
        Map<String, Object> filtro = filtro(tipo);
        filtro.put("contratoIds", List.of(c.contratoA.getId()));
        JsonNode local = previa(c.localA, filtro);
        JsonNode presidente = previa(c.presidente, filtro);
        assertTrue(local.path("metadados").path("sujeitoARevisao").asBoolean());
        assertEquals(1, local.path("metadados").path("quantidadePendencias").asLong());
        assertAll(
                () -> assertTrue(presidente.path("metadados").path("sujeitoARevisao").asBoolean(),
                        "A Presidência não pode receber um relatório definitivo com retificação pendente."),
                () -> assertEquals(1, presidente.path("metadados").path("quantidadePendencias").asLong(),
                        "O filtro de contrato exclui a pendência de B, mas inclui a pendência real de A."),
                () -> assertFalse(presidente.path("conteudo").toString().contains(c.pessoaA.getNome()),
                        "O aviso não concede exposição nominal à Presidência."));
        executar(c.presidente, post("/api/pendencias/{id}/iniciar-tratamento", pendencia),
                403, "Contar pendências não concede tratamento negocial à Presidência");
    }

    @Test
    void comparacaoDeEmissaoLocalContaSomentePendenciasDoRecortePublicado() throws Exception {
        Cenario c = criarCenario();
        marcarRevisao(c, false);
        marcarRevisao(c, true);
        JsonNode original = emitir(c.localA, filtro("FREQUENCIA_CONSOLIDADA"));
        assertEquals(1, original.path("metadados").path("quantidadePendencias").asLong());
        long id = original.path("metadados").path("id").asLong();
        JsonNode comparacao = comparar(c.nacional, id);
        assertTrue(comparacao.path("revisaoVigente").path("sujeitoARevisao").asBoolean());
        assertEquals(1, comparacao.path("revisaoVigente").path("quantidadePendencias").asLong(),
                "O aviso da comparação usa a Gestão gravada na emissão, não todo o escopo do leitor.");
    }

    private long marcarRevisao(Cenario c, boolean ladoB) throws Exception {
        JsonNode dto = registrar(c, ladoB, "FALTA");
        Frequencia f = em.find(Frequencia.class, dto.path("id").asLong());
        f.setEstadoAuditoria(EstadoAuditoriaFrequencia.SUJEITA_REVISAO);
        f.setValidadeOperacional(ValidadeOperacionalFrequencia.SUJEITA_REVISAO);
        f.setBloqueadaEdicaoOperacional(true);
        long id = pendencias.registrarRevisaoFrequencia(f, "Revisão de fonte documental da alocação histórica").getId();
        em.flush();
        return id;
    }

    private JsonNode registrar(Cenario c, boolean ladoB, String status) throws Exception {
        return dados(executar(ladoB ? c.localB : c.localA, post("/api/frequencias")
                .contentType(APPLICATION_JSON).content(json(Map.of(
                        "vinculoContratualId", (ladoB ? c.vinculoB : c.vinculoA).getId(),
                        "dataReferencia", DATA.toString(), "status", status,
                        "justificativaRetificacao", "Conferência documental da frequência no recorte",
                        "chaveIdempotencia", "escopo-frequencia-" + UUID.randomUUID()))),
                200, "Fonte real do relatório " + (ladoB ? "B" : "A")));
    }

    private Map<String, Object> filtro(String tipo) {
        Map<String, Object> filtro = new LinkedHashMap<>();
        filtro.put("tipo", tipo); filtro.put("formato", "VISUALIZACAO");
        filtro.put("inicio", DATA.toString()); filtro.put("fim", DATA.toString());
        filtro.put("chaveIdempotencia", "escopo-emissao-" + UUID.randomUUID());
        return filtro;
    }
    private JsonNode emitir(UsuarioAplicacao usuario, Map<String, Object> filtro) throws Exception {
        return dados(executar(usuario, post("/api/relatorios/administrativos/emissoes")
                .contentType(APPLICATION_JSON).content(json(filtro)), 201, "Emissão no escopo autorizado"));
    }
    private JsonNode previa(UsuarioAplicacao usuario, Map<String, Object> filtro) throws Exception {
        return dados(executar(usuario, post("/api/relatorios/administrativos/previa")
                .contentType(APPLICATION_JSON).content(json(filtro)), 200, "Prévia com aviso de pendência"));
    }
    private JsonNode comparar(UsuarioAplicacao usuario, long id) throws Exception {
        return dados(executar(usuario, get("/api/relatorios/administrativos/emissoes/{id}/comparacao-vigente", id),
                200, "Comparação respeita o escopo da emissão existente"));
    }
    private JsonNode linhas(JsonNode conteudo) { return conteudo.isArray() ? conteudo : conteudo.path("registros"); }
}
