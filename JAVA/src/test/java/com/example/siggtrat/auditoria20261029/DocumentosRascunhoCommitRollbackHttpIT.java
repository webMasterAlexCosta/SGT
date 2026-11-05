package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.configuracao.propriedades.PropriedadesArmazenamentoArquivo;
import com.example.siggtrat.entidade.DocumentoComprobatorio;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** DOC-008: banco real e arquivo físico concordam após commit e após rollback. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class DocumentosRascunhoCommitRollbackHttpIT extends ContratosHttpBase {
    @Autowired PropriedadesArmazenamentoArquivo armazenamento;
    private static final byte[] PDF = "%PDF-1.4\n1 0 obj\n<< /Type /Catalog >>\nendobj\n%%EOF".getBytes(StandardCharsets.UTF_8);
    private record Fonte(Cenario c, long contrato, long documento, Path arquivo) {}

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void descarteFisicoAcompanhaConfirmacaoDaTransacaoSemApagarArquivoEmRollback(boolean rollback) throws Exception {
        Fonte f = preparar(false);
        assertArrayEquals(PDF, Files.readAllBytes(f.arquivo()));
        new TransactionTemplate(transactionManager).execute(status -> {
            try {
                executar(f.c.localA, delete(base(f.contrato()) + "/documentos/" + f.documento()), 204,
                        "Rascunho exclusivo admite descarte dentro da unidade transacional");
                em.flush();
                assertTrue(Files.exists(f.arquivo()), "Arquivo referenciado permanece até a confirmação efetiva");
                if (rollback) status.setRollbackOnly();
                return null;
            } catch (Exception e) { throw new IllegalStateException(e); }
        });
        assertEquals(rollback, emTransacao(() -> em.find(DocumentoComprobatorio.class, f.documento()) != null));
        assertEquals(rollback, Files.exists(f.arquivo()), "Estado físico deve acompanhar a releitura em nova transação");
        if (rollback) assertArrayEquals(PDF, executar(f.c.localA,
                get(base(f.contrato()) + "/documentos/" + f.documento() + "/arquivo"), 200,
                "Rollback preserva inclusive o download dos bytes originais").getResponse().getContentAsByteArray());
    }

    @Test
    void referenciaExternaNoRascunhoImpedeDescarteEPreservaMetadadosEArquivo() throws Exception {
        Fonte f = preparar(true);
        executar(f.c.localA, delete(base(f.contrato()) + "/documentos/" + f.documento()), 409,
                "Valor documental externo impede apagar a fonte, mesmo antes da publicação do contrato");
        emTransacao(() -> {
            DocumentoComprobatorio d = em.find(DocumentoComprobatorio.class, f.documento());
            assertNotNull(d); assertEquals("SEI-PROVA-EXTERNA", d.getReferenciaSei()); return null;
        });
        assertArrayEquals(PDF, Files.readAllBytes(f.arquivo()));
    }

    private Fonte preparar(boolean referenciaExterna) throws Exception {
        Cenario c = criarCenarioPersistido();
        var inicio = RelogioSistema.hoje().plusDays(5);
        long contrato = enviar(c.localA, "/api/contratos", rascunho(c,
                "DOC-TRANS-" + UUID.randomUUID().toString().substring(0,8), 10, inicio, inicio.plusMonths(1)),
                201, "Rascunho isolado para descarte documental").path("id").asLong();
        var corpo = mapa("tipoFato", "CONTRATO_ORIGINAL", "titulo", "Prova documental transacional",
                "classificacaoAcesso", "NEGOCIAL", "dataEfetiva", inicio);
        if (referenciaExterna) corpo.put("referenciaSei", "SEI-PROVA-EXTERNA");
        long documento = dados(executar(c.localA, multipart(base(contrato) + "/documentos").file(metadados(corpo))
                .file(new MockMultipartFile("arquivo", "prova.pdf", "application/pdf", PDF)), 201,
                "Arquivo e metadados confirmados no MySQL antes do ensaio")).path("id").asLong();
        String nome = emTransacao(() -> em.find(DocumentoComprobatorio.class, documento).getNomeArmazenado());
        return new Fonte(c, contrato, documento, armazenamento.uploadDir().resolve(nome));
    }
}
