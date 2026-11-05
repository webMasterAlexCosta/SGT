package com.example.siggtrat.auditoria20261029;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Regressões pelos controladores reais, serviços reais e persistência MySQL. */
class ContratosLacunasHttpIT extends AuditoriaHttpSuporte {

    @Test
    void encerramentoComVinculosExigePreviaAntesDeAlterarFim() throws Exception {
        var c = criarCenario();
        var fimOriginal = c.contratoA.getDataFim();
        var fimVinculoOriginal = c.vinculoA.getDataFim();
        executar(c.localA, post("/api/contratos/{id}/encerrar-antecipadamente", c.contratoA.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("dataEfetiva", DATA, "motivo", "Rescisão documentada", "referenciaSei", "SEI-ENC"))),
                409, "CON-016/AUD-004 encerramento sem prévia não altera contrato nem vínculos");
        em.flush();
        em.refresh(c.contratoA);
        assertThat(c.contratoA.getDataFim()).isEqualTo(fimOriginal);
        assertThat(c.vinculoA.getDataFim()).isEqualTo(fimVinculoOriginal);
    }

    @Test
    void inativacaoRetroativaExigePreviaAntesDeBloquearOperacao() throws Exception {
        var c = criarCenario();
        executar(c.localA, post("/api/contratos/{id}/inativar", c.contratoA.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("dataEfetiva", DATA.minusDays(1), "motivo", "Bloqueio retroativo documentado"))),
                409, "CON-010/AUD-004 inativação retroativa exige prévia de impactos");
        em.flush();
        em.refresh(c.contratoA);
        assertThat(c.contratoA.getAtivo()).isTrue();
    }

    @Test
    void anulacaoExigePreviaAntesDeRetirarValidadeDosFatosRelacionados() throws Exception {
        var c = criarCenario();
        executar(c.localA, post("/api/contratos/{id}/anular", c.contratoA.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("dataEfetiva", DATA.minusDays(1), "dataAto", DATA,
                        "motivo", "Ato anulatório", "referenciaSei", "SEI-ANU"))),
                409, "CON-018/CON-024/AUD-004 anulação exige prévia revalidada");
        em.flush();
        em.refresh(c.contratoA);
        assertThat(c.contratoA.getEstadoNegocial().name()).isEqualTo("PUBLICADO");
    }

    @Test
    void documentoNaoPodeSerPublicadoParaFatoInexistente() throws Exception {
        var c = criarCenario();
        var metadados = new MockMultipartFile("metadados", "metadados.json", MediaType.APPLICATION_JSON_VALUE,
                json(Map.of("tipoFato", "ADITAMENTO", "fatoId", Long.MAX_VALUE,
                        "titulo", "Fonte de fato inexistente", "referenciaSei", "SEI-DOC-INVALIDO",
                        "classificacaoAcesso", "NEGOCIAL", "dataEfetiva", DATA)).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        executar(c.localA, multipart("/api/contratos/{id}/documentos", c.contratoA.getId()).file(metadados),
                400, "DOC-002 integridade da associação documental ao fato negocial");
        assertThat(em.createQuery("select count(d) from DocumentoComprobatorio d where d.contrato.id=:id and d.fatoId=:fato", Long.class)
                .setParameter("id", c.contratoA.getId()).setParameter("fato", Long.MAX_VALUE).getSingleResult()).isZero();
    }

    @Test
    void historicoDocumentalMantemHashIntegroDepoisDeRecarregarDoMysql() throws Exception {
        var c = criarCenario();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("tipoFato", "CONTRATO_ORIGINAL");
        body.put("fatoChave", "ORIGINAL");
        body.put("titulo", "Referência preparada");
        body.put("referenciaSei", "SEI-HASH-MYSQL");
        body.put("classificacaoAcesso", "NEGOCIAL");
        body.put("dataEfetiva", DATA);
        var metadados = new MockMultipartFile("metadados", "metadados.json", MediaType.APPLICATION_JSON_VALUE,
                json(body).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        JsonNode doc = dados(executar(c.localA,
                multipart("/api/contratos/{id}/documentos", c.contratoA.getId()).file(metadados),
                201, "DOC-005 criação de documento com trilha hash"));
        Long contratoId = c.contratoA.getId();
        long documentoId = doc.path("id").asLong();
        em.flush();
        em.clear();
        JsonNode eventos = dados(executar(c.localA,
                get("/api/contratos/{id}/documentos/{doc}/eventos", contratoId, documentoId),
                200, "DOC-005 cadeia hash deve sobreviver à precisão temporal do MySQL"));
        assertThat(eventos.isArray()).isTrue();
        assertThat(eventos.size()).isGreaterThan(0);
        assertThat(eventos.get(0).path("hashEvento").asText()).hasSize(64);
    }
}
