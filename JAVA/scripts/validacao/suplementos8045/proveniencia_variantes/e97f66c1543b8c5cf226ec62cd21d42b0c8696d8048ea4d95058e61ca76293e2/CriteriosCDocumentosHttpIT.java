package com.example.siggtrat.auditoria20261029;

import com.fasterxml.jackson.databind.JsonNode;
import com.example.siggtrat.entidade.DocumentoComprobatorio;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Prepared only: real MVC/JPA/files. Criteria credit requires the future XML + request/state receipts. */
class CriteriosCDocumentosHttpIT extends CriteriosCHttpBase {
    private byte[] pdf(String marca) {
        return ("%PDF-1.4\n% " + marca + "\n1 0 obj\n<< /Type /Catalog >>\nendobj\n%%EOF\n").getBytes(StandardCharsets.UTF_8);
    }

    private long original(Cenario c, long contrato) throws Exception {
        JsonNode docs = consultar(c.localA, base(contrato) + "/documentos", "Fonte original existente");
        assertEquals(1, docs.size()); return docs.get(0).path("id").asLong();
    }

    private JsonNode substituir(Cenario c, long contrato, long doc, String key, byte[] conteudo) throws Exception {
        return dados(executar(c.localA, multipart(base(contrato) + "/documentos/" + doc + "/substituir")
                .file(metadados(mapa("motivo", "Fonte sintética substituta documentada", "chaveIdempotencia", key)))
                .file(new MockMultipartFile("arquivo", "fonte-c.pdf", "application/pdf", conteudo)),
                200, "DOC-007 controle: substituição real com bytes verificáveis"));
    }

    @Test
    void postFinalSemFonteRecusaAntesDaGravacaoEControleComFonteConfirma() throws Exception {
        Cenario c = criarCenarioPersistido(); NovoContrato n = contratoConfirmado(c, true);
        var req = aditamento("C-FONTE-" + n.id(), n.inicio().plusDays(1));
        req.put("novaDescricao", "Alteração válida com fonte explícita C " + n.id());
        JsonNode previa = enviar(c.localA, base(n.id()) + "/aditamentos/previa", req, 200,
                "DOC-002 controle: dados e prévia válidos antes de retirar somente a fonte");
        req.put("tokenPrevia", previa.path("tokenPrevia").asText());
        assertFalse(req.get("tokenPrevia").toString().isBlank());
        JsonNode antes = estadoDocumental(n.id()); var files = arquivos();
        var semFonte = new LinkedHashMap<>(req); semFonte.remove("referenciaSei");
        var recusado = executar(c.localA, post(base(n.id()) + "/aditamentos")
                .contentType(MediaType.APPLICATION_JSON).content(json(semFonte)), 400,
                "DOC-002:001/003 POST final sem fonte; não é somente validação de prévia");
        String erro = recusado.getResponse().getContentAsString().toLowerCase(Locale.ROOT);
        assertTrue(erro.contains("fonte") && (erro.contains("sei") || erro.contains("comprobat")),
                "A recusa deve identificar a fonte, não token, autorização ou campo não relacionado: " + erro);
        JsonNode depoisRecusa = estadoDocumental(n.id());
        assertEquals(antes, depoisRecusa); assertEquals(files, arquivos());
        // The original valid request remains unchanged; a fresh preview avoids a time-based token ambiguity.
        req.put("tokenPrevia", enviar(c.localA, base(n.id()) + "/aditamentos/previa", req, 200,
                "Controle positivo após recusa sem fonte").path("tokenPrevia").asText());
        JsonNode aceito = enviar(c.localA, base(n.id()) + "/aditamentos", req, 201,
                "DOC-002 restaurar somente fonte válida permite confirmação do instrumento");
        assertTrue(aceito.path("id").asLong() > 0);
        assertEquals(1, estadoDocumental(n.id()).path("aditamentos").size());
        registrar("doc002-post-final-" + n.id(), mapa("contrato", n.id(), "statusSemFonte", 400,
                "estadoAntes", antes, "estadoDepoisRecusa", depoisRecusa, "statusComFonte", 201,
                "instrumentoId", aceito.path("id"), "shaArquivosAntes", files));
    }

    @Test
    void substituirEInvalidarPreservamBytesFisicosDownloadsEMetadadosAnteriores() throws Exception {
        Cenario c = criarCenarioPersistido(); NovoContrato n = contratoConfirmado(c, true);
        byte[] a = pdf("ARQUIVO A " + n.id()), b = pdf("ARQUIVO B DISTINTO " + n.id());
        long d1 = substituir(c, n.id(), original(c, n.id()), "c-arquivo-a-" + n.id(), a).path("id").asLong();
        var meta1 = metadadosImutaveisDocumento(d1);
        assertArrayEquals(a, arquivoDocumento(d1));
        assertArrayEquals(a, executar(c.presidente, get(base(n.id()) + "/documentos/" + d1 + "/arquivo"),
                200, "Controle dos bytes A antes da substituição").getResponse().getContentAsByteArray());
        long d2 = substituir(c, n.id(), d1, "c-arquivo-b-" + n.id(), b).path("id").asLong();
        assertNotEquals(d1, d2); assertNotEquals(sha(a), sha(b));
        assertEquals("SUBSTITUIDO", consultar(c.localA, base(n.id()) + "/documentos/" + d1,
                "Versão A ainda consultável").path("estado").asText());
        assertEquals(meta1, metadadosImutaveisDocumento(d1)); assertArrayEquals(a, arquivoDocumento(d1));
        assertArrayEquals(a, executar(c.presidente, get(base(n.id()) + "/documentos/" + d1 + "/arquivo"),
                200, "DOC-007 bytes A após substituir A por B").getResponse().getContentAsByteArray());
        var meta2 = metadadosImutaveisDocumento(d2); assertArrayEquals(b, arquivoDocumento(d2));
        assertEquals("INVALIDO", enviar(c.localA, base(n.id()) + "/documentos/" + d2 + "/invalidar",
                mapa("motivo", "Invalidação sintética sem apagar fonte"), 200,
                "DOC-007 invalidação de documento que já possui bytes").path("estado").asText());
        assertEquals(meta2, metadadosImutaveisDocumento(d2)); assertArrayEquals(b, arquivoDocumento(d2));
        assertArrayEquals(b, executar(c.presidente, get(base(n.id()) + "/documentos/" + d2 + "/arquivo"),
                200, "DOC-007 bytes B após invalidação").getResponse().getContentAsByteArray());
        assertArrayEquals(a, arquivoDocumento(d1));
        registrar("doc007-bytes-" + n.id(), mapa("contrato", n.id(), "substituido", d1, "invalidado", d2,
                "shaA", sha(a), "shaB", sha(b), "metadadosA", meta1, "metadadosB", meta2,
                "estadoFinal", estadoDocumental(n.id()), "arquivos", arquivos()));
    }

    @Test
    void chaveDivergentePreservaConteudoIntegralDaCadeiaDocumental() throws Exception {
        Cenario c = criarCenarioPersistido(); NovoContrato n = contratoConfirmado(c, true);
        long d0 = original(c, n.id()); String rota = base(n.id()) + "/documentos/" + d0 + "/retificar-sei";
        String key = "c-retificar-" + UUID.randomUUID();
        var req = mapa("novaReferenciaSei", "SEI-C-RETIFICADO", "motivo", "Correção C", "chaveIdempotencia", key);
        JsonNode primeiro = enviar(c.localA, rota, req, 200, "DOC-005 controle: chave confirma nova versão");
        // C02 observed a genuine technical DTO version difference: the first POST returned
        // version=0 while the committed row/replay returned version=1. Preserve that observation;
        // this focal tests canonical committed state, without dropping version or any other field.
        long documentoId = primeiro.path("id").asLong();
        JsonNode canonico = consultar(c.localA, base(n.id()) + "/documentos/" + documentoId,
                "GET canônico após commit da primeira retificação documental");
        long versaoPersistida = emTransacao(() -> em.find(DocumentoComprobatorio.class, documentoId).getVersion());
        assertEquals(documentoId, canonico.path("id").asLong());
        assertEquals(n.id(), canonico.path("contratoId").asLong());
        assertEquals(2, canonico.path("versaoDocumento").asInt());
        assertEquals("SEI-C-RETIFICADO", canonico.path("referenciaSei").asText());
        assertTrue(canonico.path("version").isIntegralNumber());
        assertEquals(versaoPersistida, canonico.path("version").asLong(), "GET deve expor a versão técnica persistida");
        Set<String> campos = new TreeSet<>();
        primeiro.fieldNames().forEachRemaining(campos::add); canonico.fieldNames().forEachRemaining(campos::add);
        List<String> divergentes = campos.stream().filter(campo -> !Objects.equals(primeiro.get(campo), canonico.get(campo))).toList();
        registrar("doc005-primeiro-dto-vs-get-" + n.id(), mapa("contrato", n.id(),
                "primeiroDTO", primeiro, "GETcanonicoPosCommit", canonico, "camposDivergentes", divergentes,
                "respostasIntegralmenteIguais", primeiro.equals(canonico),
                "interpretacao", "Divergência técnica do primeiro DTO permanece registrada separadamente; não é aceite da equivalência de respostas"));
        JsonNode antes = estadoDocumental(n.id()); var files = arquivos();
        var divergente = new LinkedHashMap<>(req); divergente.put("novaReferenciaSei", "SEI-C-OUTRA-INTENCAO");
        enviar(c.localA, rota, divergente, 409, "DOC-005 intenção divergente conserva cadeia integral");
        assertEquals(antes, estadoDocumental(n.id())); assertEquals(files, arquivos());
        JsonNode reenvio = enviar(c.localA, rota, req, 200, "Controle da mesma chave/intenção após conflito");
        assertEquals(canonico, reenvio, "Replay deve devolver o DTO canônico pós-commit integral, inclusive version");
        assertEquals(antes, estadoDocumental(n.id())); assertEquals(files, arquivos());
        assertEquals(canonico, consultar(c.localA, base(n.id()) + "/documentos/" + documentoId,
                "GET canônico final mantém integralmente o mesmo DTO após replay"));
        assertEquals(antes, estadoDocumental(n.id())); assertEquals(files, arquivos());
        registrar("doc005-cadeia-" + n.id(), mapa("contrato", n.id(), "estadoAntesEDepois", antes,
                "conflito", 409, "idReutilizado", reenvio.path("id"), "GETcanonico", canonico,
                "replayDTO", reenvio, "primeiroDTO", primeiro, "camposDivergentesPrimeiroVsCanonico", divergentes,
                "arquivos", files));
    }

    @Test
    void estadualForaDoEstadoNaoLeMetadadosNemBytesDeDocumentoExistente() throws Exception {
        Cenario c = criarCenarioPersistido(); NovoContrato n = contratoConfirmado(c, true);
        byte[] conteudo = pdf("ESCOPO ESTADUAL " + n.id());
        long id = substituir(c, n.id(), original(c, n.id()), "c-estado-" + n.id(), conteudo).path("id").asLong();
        String rota = base(n.id()) + "/documentos/" + id;
        JsonNode correto = consultar(c.estadualA, rota, "PER-009 documento existente no estado correto");
        assertEquals(id, correto.path("id").asLong());
        assertArrayEquals(conteudo, executar(c.estadualA, get(rota + "/arquivo"), 200,
                "Controle positivo estadual de arquivo").getResponse().getContentAsByteArray());
        JsonNode antes = estadoDocumental(n.id()); var files = arquivos();
        executar(c.estadualB, get(rota), 404, "PER-009 metadados negados ao estado distinto");
        var negado = executar(c.estadualB, get(rota + "/arquivo"), 404, "PER-009 bytes negados ao estado distinto");
        assertFalse(Arrays.equals(conteudo, negado.getResponse().getContentAsByteArray()));
        assertEquals(antes, estadoDocumental(n.id())); assertEquals(files, arquivos());
        assertArrayEquals(conteudo, executar(c.estadualA, get(rota + "/arquivo"), 200,
                "Controle positivo posterior prova que recusa não era documento ausente").getResponse().getContentAsByteArray());
        registrar("per009-estado-" + n.id(), mapa("documento", id, "estadoCorreto", c.seA.getId(),
                "estadoRecusado", c.seB.getId(), "statusMetadadosRecusados", 404,
                "statusBytesRecusados", 404, "sha256", sha(conteudo)));
    }
}
