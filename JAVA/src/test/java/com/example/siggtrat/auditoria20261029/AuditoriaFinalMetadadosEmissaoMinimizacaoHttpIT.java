package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.EmissaoRelatorioAdministrativo;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Proposta independente; NÃO executada pelo auditor. Compilar apenas em área de validação separada. */
class AuditoriaFinalMetadadosEmissaoMinimizacaoHttpIT extends AuditoriaHttpSuporte {
    @ParameterizedTest
    @ValueSource(strings = {"PRESIDENTE", "NACIONAL", "ESTADUAL", "ADMINISTRADOR"})
    void motivoPessoalDaRetificacaoNaoVazaNosMetadadosDaEmissao(String perfil) throws Exception {
        Cenario c = criarCenario();
        registrar(c, "FALTA", "Conferência inicial documentada");
        JsonNode original = dados(executar(c.localA,
                post("/api/relatorios/administrativos/emissoes")
                        .contentType(APPLICATION_JSON).content(json(Map.of(
                                "tipo", "FREQUENCIA_CONSOLIDADA", "formato", "VISUALIZACAO",
                                "inicio", DATA.toString(), "fim", DATA.toString(),
                                "contratoIds", List.of(c.contratoA.getId()),
                                "chaveIdempotencia", "audit-emissao-" + UUID.randomUUID()))),
                201, "Pré-condição: emissão formal válida no recorte local"));
        long emissaoId = original.path("metadados").path("id").asLong();
        String canario = "CANARIO_RESTRITO_DIAGNOSTICO_" + UUID.randomUUID();
        registrar(c, "PRESENCA", canario);
        em.flush(); em.clear();
        EmissaoRelatorioAdministrativo persistida = em.find(EmissaoRelatorioAdministrativo.class, emissaoId);
        assertEquals(canario, persistida.getMotivoSuperacao(), "Pré-condição: motivo original preservado no PostgreSQL");
        byte[] prova = persistida.getConteudoSnapshotGzip().clone();
        String hash = persistida.getHashConteudo();
        JsonNode leituraLocal = dados(executar(c.localA,
                get("/api/relatorios/administrativos/emissoes/{id}", emissaoId),
                200, "Responsável local mantém acesso à justificativa restrita do seu recorte"));
        assertEquals(canario, leituraLocal.path("metadados").path("motivoSuperacao").asText());
        UsuarioAplicacao leitor = switch (perfil) {
            case "PRESIDENTE" -> c.presidente;
            case "NACIONAL" -> c.nacional;
            case "ADMINISTRADOR" -> c.admin;
            default -> c.estadualA;
        };
        JsonNode leitura = dados(executar(leitor,
                get("/api/relatorios/administrativos/emissoes/{id}", emissaoId),
                200, "Consulta institucional sem concessão de conteúdo pessoal/restrito"));
        JsonNode comparacao = dados(executar(leitor,
                get("/api/relatorios/administrativos/emissoes/{id}/comparacao-vigente", emissaoId),
                200, "Comparação institucional da mesma emissão"));
        JsonNode lista = dados(executar(leitor,
                get("/api/relatorios/administrativos/emissoes").param("inicio", DATA.toString())
                        .param("fim", DATA.toString()).param("tipo", "FREQUENCIA_CONSOLIDADA"),
                200, "Listagem institucional dos metadados"));
        em.flush(); em.clear();
        EmissaoRelatorioAdministrativo depois = em.find(EmissaoRelatorioAdministrativo.class, emissaoId);
        assertAll(
                () -> assertFalse(leitura.toString().contains(canario), "PER-006/PER-011: motivo pessoal vazou em obter"),
                () -> assertFalse(comparacao.toString().contains(canario), "PER-006/PER-011: motivo pessoal vazou em comparar"),
                () -> assertFalse(lista.toString().contains(canario), "PER-006/PER-011: motivo pessoal vazou na listagem"),
                () -> assertEquals(canario, depois.getMotivoSuperacao(), "A minimização não apaga a prova"),
                () -> assertEquals(hash, depois.getHashConteudo()),
                () -> assertArrayEquals(prova, depois.getConteudoSnapshotGzip()));
    }

    private void registrar(Cenario c, String status, String motivo) throws Exception {
        executar(c.localA, post("/api/frequencias").contentType(APPLICATION_JSON).content(json(Map.of(
                "vinculoContratualId", c.vinculoA.getId(), "dataReferencia", DATA.toString(), "status", status,
                "justificativaRetificacao", motivo, "chaveIdempotencia", "audit-frequencia-" + UUID.randomUUID()))),
                200, "Frequência canônica com justificativa própria do ato");
    }
}
