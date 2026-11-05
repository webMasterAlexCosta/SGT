package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.relatorio.EstadoEmissaoRelatorio;
import com.example.siggtrat.entidade.EmissaoRelatorioAdministrativo;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** REL-003: a consulta por conhecimento não revela a superação descoberta depois do corte. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ConclusaoRelatoriosConhecimentoHttpIT extends ContratosHttpBase {
    private static final String EMISSOES = "/api/relatorios/administrativos/emissoes";

    @Test
    void corteAnteriorNaoExpoeSuperacaoConhecidaSomenteDepois() throws Exception {
        Cenario c = criarCenarioPersistido();
        registrar(c, "FALTA"); Map<String, Object> filtro = filtro(c);
        JsonNode original = enviar(c.localA, EMISSOES, filtro, 201, "Emissão formal conhecida no primeiro instante");
        long id = original.path("metadados").path("id").asLong(); Prova prova = prova(id);
        Instant corte = Instant.now();
        registrar(c, "PRESENCA");
        JsonNode atual = consultar(c.localA, EMISSOES + "/" + id, "Consulta atual conserva a superação posterior");
        assertEquals("SUPERADA", atual.path("metadados").path("estado").asText());
        Instant superadaEm = Instant.parse(atual.path("metadados").path("superadaEm").asText());
        assertTrue(superadaEm.isAfter(corte), "O cenário precisa distinguir os dois instantes de conhecimento.");

        JsonNode anterior = dados(executar(c.localA, post(EMISSOES + "/conhecimento")
                .param("conhecidoAte", corte.toString()).contentType(APPLICATION_JSON).content(json(filtro)), 200,
                "Consulta anterior não pode importar metadados de uma retificação ainda desconhecida"));
        assertEquals(id, anterior.path("metadados").path("id").asLong());
        assertEquals(original.path("conteudo"), anterior.path("conteudo"));
        assertEquals("VIGENTE", anterior.path("metadados").path("estado").asText());
        assertEquals("ORIGINAL", anterior.path("metadados").path("situacao").asText());
        assertTrue(anterior.path("metadados").path("superadaEm").isNull());
        assertTrue(anterior.path("metadados").path("causaSuperacao").isNull());
        assertTrue(anterior.path("metadados").path("motivoSuperacao").isNull());
        assertPersistenciaAtualPreservada(id, prova, superadaEm);
    }

    @Test
    void cortePosteriorMostraSuperacaoJaConhecidaSemReescreverPersistencia() throws Exception {
        Cenario c = criarCenarioPersistido(); registrar(c, "FALTA"); Map<String, Object> filtro = filtro(c);
        JsonNode original = enviar(c.localA, EMISSOES, filtro, 201, "Emissão original para controle do corte posterior");
        long id = original.path("metadados").path("id").asLong(); Prova prova = prova(id);
        registrar(c, "PRESENCA");
        JsonNode atual = consultar(c.localA, EMISSOES + "/" + id, "Superação atual comprovada depois da retificação");
        Instant superadaEm = Instant.parse(atual.path("metadados").path("superadaEm").asText());

        JsonNode posterior = dados(executar(c.localA, post(EMISSOES + "/conhecimento")
                .param("conhecidoAte", Instant.now().toString()).contentType(APPLICATION_JSON).content(json(filtro)), 200,
                "Corte posterior deve mostrar a superação que já era conhecida"));
        assertEquals("SUPERADA", posterior.path("metadados").path("estado").asText());
        assertEquals("SUPERADA", posterior.path("metadados").path("situacao").asText());
        assertEquals(atual.path("metadados").path("causaSuperacao"), posterior.path("metadados").path("causaSuperacao"));
        assertEquals(atual.path("metadados").path("superadaEm"), posterior.path("metadados").path("superadaEm"));
        assertEquals(original.path("conteudo"), posterior.path("conteudo"));
        assertPersistenciaAtualPreservada(id, prova, superadaEm);
    }

    private void registrar(Cenario c, String status) throws Exception {
        enviar(c.localA, "/api/frequencias", mapa("vinculoContratualId", c.vinculoA.getId(),
                "dataReferencia", DATA, "status", status,
                "justificativaRetificacao", "Conferência documental em instante distinto",
                "chaveIdempotencia", "conhecimento-frequencia-" + UUID.randomUUID()), 200,
                "Frequência oficial confirmada antes da consulta do conhecimento");
    }

    private Map<String, Object> filtro(Cenario c) {
        return mapa("tipo", "FREQUENCIA_CONSOLIDADA", "formato", "VISUALIZACAO", "inicio", DATA, "fim", DATA,
                "contratoIds", List.of(c.contratoA.getId()), "chaveIdempotencia", "conhecimento-emissao-" + UUID.randomUUID());
    }

    private Prova prova(long id) {
        return emTransacao(() -> {
            EmissaoRelatorioAdministrativo e = em.find(EmissaoRelatorioAdministrativo.class, id);
            return new Prova(e.getConteudoSnapshotGzip().clone(), e.getHashConteudo());
        });
    }

    private void assertPersistenciaAtualPreservada(long id, Prova original, Instant superadaEm) {
        emTransacao(() -> {
            EmissaoRelatorioAdministrativo e = em.find(EmissaoRelatorioAdministrativo.class, id);
            assertEquals(EstadoEmissaoRelatorio.SUPERADA, e.getEstado(), "A projeção da consulta passada não regrava a entidade atual.");
            assertEquals(superadaEm, e.getSuperadaEm());
            assertArrayEquals(original.gzip(), e.getConteudoSnapshotGzip());
            assertEquals(original.hash(), e.getHashConteudo());
            return null;
        });
    }

    private record Prova(byte[] gzip, String hash) { }
}
