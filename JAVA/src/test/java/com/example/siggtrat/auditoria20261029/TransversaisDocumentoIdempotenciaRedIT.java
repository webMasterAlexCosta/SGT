package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

/** Proposta RED AUD-005: DTO permite omitir chave; reenvio não deve criar duas fontes iguais. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TransversaisDocumentoIdempotenciaRedIT extends ContratosHttpBase {
    private static final byte[] PDF = "%PDF-1.4\n1 0 obj\n<< /Type /Catalog >>\nendobj\n%%EOF"
            .getBytes(StandardCharsets.UTF_8);

    @Test
    void reenvioSemChaveNaoDuplicaRaizDocumental() throws Exception {
        Cenario c = criarCenarioPersistido();
        var inicio = RelogioSistema.hoje().plusDays(5);
        long contrato = enviar(c.localA, "/api/contratos", rascunho(c,
                "DOC-REENVIO-" + c.contratoA.getId(), 10, inicio, inicio.plusMonths(3)), 201,
                "Rascunho próprio para duas tentativas da mesma intenção documental").path("id").asLong();
        String rota = base(contrato) + "/documentos";
        String titulo = "Fonte sem chave " + UUID.randomUUID();
        Map<String, Object> corpo = mapa("tipoFato", "CONTRATO_ORIGINAL", "titulo", titulo,
                "classificacaoAcesso", "NEGOCIAL", "dataEfetiva", inicio);
        Map<String, Object> controle = mapa("tipoFato", "CONTRATO_ORIGINAL", "titulo", "Controle com chave",
                "classificacaoAcesso", "NEGOCIAL", "dataEfetiva", inicio,
                "chaveIdempotencia", "controle-documento-" + UUID.randomUUID());
        executar(c.localA, multipart(rota).file(metadados(controle))
                        .file(new MockMultipartFile("arquivo", "contrato.pdf", "application/pdf", PDF)),
                201, "Controle comprova que a fonte e o escopo são válidos");
        long antes = contar(contrato);

        var primeira = mvc.perform(autenticar(multipart(rota).file(metadados(corpo))
                .file(new MockMultipartFile("arquivo", "contrato.pdf", "application/pdf", PDF)), c.localA)).andReturn();
        if (primeira.getResponse().getStatus() == 400) {
            assertEquals(antes, contar(contrato), "Exigir chave antes da gravação é alternativa segura.");
            return;
        }
        assertEquals(201, primeira.getResponse().getStatus());
        JsonNode original = dados(primeira);
        long confirmado = contar(contrato);
        assertEquals(antes + 1, confirmado);

        var segunda = mvc.perform(autenticar(multipart(rota).file(metadados(corpo))
                .file(new MockMultipartFile("arquivo", "contrato.pdf", "application/pdf", PDF)), c.localA)).andReturn();
        int status = segunda.getResponse().getStatus();
        assertTrue(status == 200 || status == 201 || status == 409,
                "O reenvio deve ser reutilizado ou rejeitado como duplicidade sem efeitos novos.");
        assertEquals(confirmado, contar(contrato), "Mesmo ator, metadados e bytes não podem gerar nova raiz por reenvio.");
        if (status != 409) assertEquals(original.path("id"), dados(segunda).path("id"));
        assertEquals(original.path("sha256Arquivo"),
                consultar(c.localA, rota + "/" + original.path("id").asLong(), "Original intacto após reenvio")
                        .path("sha256Arquivo"));
    }

    private long contar(long contrato) {
        return emTransacao(() -> em.createQuery(
                        "select count(d) from DocumentoComprobatorio d where d.contrato.id=:id", Long.class)
                .setParameter("id", contrato).getSingleResult());
    }
}
