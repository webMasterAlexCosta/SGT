package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.DocumentoComprobatorio;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** DOC-002/003: fonte opcional/alternativa vinculada ao instrumento, com releitura após commit. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class DocumentosInstrumentosPersistenciaHttpIT extends ContratosHttpBase {
    private static final byte[] PDF = "%PDF-1.4\n1 0 obj\n<< /Type /Catalog >>\nendobj\n%%EOF".getBytes(StandardCharsets.UTF_8);

    @Test
    void aditamentoSemSeiPublicaAnexoPreparadoEVinculaFonteAoInstrumentoCorreto() throws Exception {
        Cenario c = criarCenarioPersistido(); NovoContrato n = publicar(c, false);
        String identificacao = "ADI-FONTE-ANEXO";
        JsonNode preparado = dados(executar(c.localA, multipart(base(n.id()) + "/documentos")
                .file(metadados(mapa("tipoFato", "ADITAMENTO", "fatoChave", identificacao,
                        "titulo", "Aditamento com prova binária", "classificacaoAcesso", "NEGOCIAL",
                        "dataEfetiva", n.inicio().plusDays(1))))
                .file(new MockMultipartFile("arquivo", "instrumento.pdf", "application/pdf", PDF)), 201,
                "Preparação da fonte documental do aditamento ainda não registrado"));
        long documento = preparado.path("id").asLong();
        assertEquals("RASCUNHO", preparado.path("estado").asText());
        var req = aditamento(identificacao, n.inicio().plusDays(1)); req.remove("referenciaSei");
        req.put("novaDescricao", "Descrição alterada por instrumento com anexo suficiente");
        JsonNode previa = enviar(c.localA, base(n.id()) + "/aditamentos/previa", req, 200,
                "Anexo correspondente é fonte suficiente, sem exigir SEI adicional");
        req.put("tokenPrevia", previa.path("tokenPrevia").asText());
        long instrumento = enviar(c.localA, base(n.id()) + "/aditamentos", req, 201,
                "Aditamento e publicação da fonte confirmam atomicamente").path("id").asLong();
        emTransacao(() -> {
            DocumentoComprobatorio d = em.find(DocumentoComprobatorio.class, documento);
            assertEquals(instrumento, d.getFatoId()); assertEquals("ADITAMENTO", d.getTipoFato().name());
            assertEquals("PUBLICADO", d.getEstado().name()); assertNull(d.getReferenciaSei());
            assertEquals(preparado.path("sha256Arquivo").asText(), d.getSha256Arquivo()); return null;
        });
        assertArrayEquals(PDF, executar(c.localA, get(base(n.id()) + "/documentos/" + documento + "/arquivo"), 200,
                "Arquivo do instrumento permanece recuperável após confirmação e releitura").getResponse().getContentAsByteArray());
    }

    @Test
    void remanejamentoComSeiOpcionalPreservaReferenciaEFatoSemExigirArquivo() throws Exception {
        Cenario c = criarCenarioPersistido(); NovoContrato n = publicar(c, true);
        var req = remanejamento(n, "REM-COM-SEI", n.inicio().plusDays(1), 5);
        req.put("referenciaSei", "SEI-REM-OPCIONAL");
        JsonNode previa = enviar(c.localA, base(n.id()) + "/remanejamentos/previa", req, 200,
                "Referência opcional é aceita no instrumento com soma zero");
        req.put("tokenPrevia", previa.path("tokenPrevia").asText());
        long instrumento = enviar(c.localA, base(n.id()) + "/remanejamentos", req, 201,
                "Remanejamento conserva a referência fornecida sem tornar arquivo obrigatório").path("id").asLong();
        long documento = emTransacao(() -> {
            DocumentoComprobatorio d = em.createQuery("select d from DocumentoComprobatorio d where d.contrato.id=:c and d.fatoId=:f and d.tipoFato=com.example.siggtrat.dominio.documento.TipoFatoDocumental.REMANEJAMENTO",
                    DocumentoComprobatorio.class).setParameter("c", n.id()).setParameter("f", instrumento).getSingleResult();
            assertEquals("SEI-REM-OPCIONAL", d.getReferenciaSei()); assertEquals("PUBLICADO", d.getEstado().name());
            assertFalse(d.possuiArquivo()); return d.getId();
        });
        JsonNode fonte = consultar(c.localA, base(n.id()) + "/documentos/" + documento,
                "Fonte do remanejamento publicada com vínculo preservado");
        assertEquals(instrumento, fonte.path("fatoId").asLong()); assertEquals("SEI-REM-OPCIONAL", fonte.path("referenciaSei").asText());
    }

    private NovoContrato publicar(Cenario c, boolean duasComposicoes) {
        return emTransacao(() -> {
            try { return publicado(c, 100, duasComposicoes ? 50 : null, true); }
            catch (Exception e) { throw new IllegalStateException(e); }
        });
    }

    @Test
    void postFinalSemFonteRecusaAditamentoSemFatoParcialESeiPermaneceAlternativaLegitima() throws Exception {
        Cenario c = criarCenarioPersistido(); NovoContrato n = publicar(c, false);
        String identificacao = "ADI-SEM-FONTE-" + n.id();
        var req = aditamento(identificacao, n.inicio().plusDays(1)); req.remove("referenciaSei");
        req.put("novaDescricao", "Condição válida cujo único suporte será retirado antes do POST final");
        JsonNode preparado = dados(executar(c.localA, multipart(base(n.id()) + "/documentos")
                .file(metadados(mapa("tipoFato", "ADITAMENTO", "fatoChave", identificacao,
                        "titulo", "Fonte sintética exclusiva ainda em rascunho", "classificacaoAcesso", "NEGOCIAL",
                        "dataEfetiva", n.inicio().plusDays(1))))
                .file(new MockMultipartFile("arquivo", "fonte-temporaria.pdf", "application/pdf", PDF)),
                201, "Controle positivo: fonte existente permite preparar a intenção sem SEI"));
        assertEquals("RASCUNHO", preparado.path("estado").asText());
        JsonNode previa = enviar(c.localA, base(n.id()) + "/aditamentos/previa", req, 200,
                "Demais campos e token provêm da prévia pública válida");
        req.put("tokenPrevia", previa.path("tokenPrevia").asText());
        assertFalse(req.get("tokenPrevia").toString().isBlank());
        executar(c.localA, delete(base(n.id()) + "/documentos/" + preparado.path("id").asLong()), 204,
                "Apenas o rascunho sintético exclusivo sem SEI é retirado antes do comando final");
        JsonNode antes = fotografiaFato(c, n.id());
        var negada = executar(c.localA, post(base(n.id()) + "/aditamentos")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content(json(req)),
                400, "DOC002 POST final revalida ausência de anexo e de SEI");
        assertTrue(mapper.readTree(negada.getResponse().getContentAsString()).path("message").asText()
                .contains("fonte comprobatória"), "Recusa deve identificar a fonte, não token ou sintaxe");
        assertEquals(antes, fotografiaFato(c, n.id()), "Não cria instrumento, evento, versão ou alteração parcial do contrato");
        req.put("referenciaSei", "SEI-ALTERNATIVA-LEGITIMA-" + n.id());
        req.put("tokenPrevia", enviar(c.localA, base(n.id()) + "/aditamentos/previa", req, 200,
                "SEI é fonte suficiente sem tornar upload obrigatório").path("tokenPrevia").asText());
        JsonNode controle = enviar(c.localA, base(n.id()) + "/aditamentos", req, 201,
                "Controle positivo do mesmo ato com referência SEI legítima no modelo");
        assertEquals(identificacao, controle.path("identificacao").asText());
    }

    private JsonNode fotografiaFato(Cenario c, long contrato) throws Exception {
        return mapper.valueToTree(mapa(
                "contrato", consultar(c.localA, base(contrato), "Estado integral do contrato no recorte da fixture"),
                "instrumentos", consultar(c.localA, base(contrato) + "/aditamentos", "Instrumentos antes/depois do POST"),
                "eventos", consultar(c.localA, base(contrato) + "/eventos", "Eventos antes/depois do POST"),
                "versoes", consultar(c.localA, base(contrato) + "/versoes", "Versões antes/depois do POST"),
                "documentos", consultar(c.localA, base(contrato) + "/documentos", "Fontes antes/depois do POST")));
    }
}
