package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.documento.TipoAcessoDocumento;
import com.example.siggtrat.entidade.DocumentoComprobatorio;
import com.example.siggtrat.entidade.PermissaoDocumentoRestrito;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

/** DOC-010: expiração temporal de uma concessão antes válida, com bytes e MySQL reais. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RevisaoDownloadExpiracaoHttpIT extends ContratosHttpBase {
    @Test
    void concessaoValidaDeixaDeAutorizarBytesDepoisDoPrazoSemRevogacaoOuAlteracaoDaProva() throws Exception {
        var c = criarCenarioPersistido();
        NovoContrato n = emTransacao(() -> {
            try { return publicado(c, 5, null, false); }
            catch (Exception erro) { throw new IllegalStateException(erro); }
        });
        long publicacao = consultar(c.localA, base(n.id()) + "/eventos", "Fonte documental publicada")
                .get(0).path("id").asLong();
        ByteArrayOutputStream pdf = new ByteArrayOutputStream();
        Document documento = new Document(); PdfWriter.getInstance(documento, pdf);
        documento.open(); documento.add(new Paragraph("Prova pessoal para expiração " + n.id())); documento.close();
        byte[] bytes = pdf.toByteArray(); String hash = sha256(bytes);
        var criado = dados(executar(c.localA, multipart(base(n.id()) + "/documentos")
                .file(metadados(mapa("tipoFato", "CONTRATO_ORIGINAL", "fatoId", publicacao,
                        "fatoChave", "ORIGINAL", "titulo", "Prova de concessão temporal",
                        "classificacaoAcesso", "PESSOAL_RESTRITO", "dataEfetiva", n.inicio(),
                        "chaveIdempotencia", "expiracao-documental-" + n.id())))
                .file(new MockMultipartFile("arquivo", "expiracao.pdf", "application/pdf", bytes)),
                201, "PDF pessoal real armazenado com sua fonte"));
        long documentoId = criado.path("id").asLong();
        String rota = base(n.id()) + "/documentos/" + documentoId;
        Instant expira = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS).plusSeconds(300);
        var concessao = enviar(c.admin, rota + "/permissoes", mapa("usuarioId", c.nacional.getId(),
                "finalidade", "auditoria temporal", "justificativa", "Concessão válida até o prazo declarado",
                "autorizacaoAdministrativa", "ATO-EXPIRACAO", "validoAte", expira), 200,
                "A concessão é criada ainda válida e confirmada no MySQL");
        long permissaoId = concessao.path("id").asLong();
        byte[] permitido = executar(c.nacional, get(rota + "/arquivo").param("finalidade", "auditoria temporal"),
                200, "Controle positivo anterior à expiração").getResponse().getContentAsByteArray();
        assertThat(permitido).isEqualTo(bytes); assertThat(sha256(permitido)).isEqualTo(hash);

        Instant depois = expira.plusSeconds(1);
        // Somente o relógio muda. A concessão não é revogada nem editada, e a
        // mesma sessão volta a percorrer filtros, serviço e consulta JPA reais.
        try (var relogio = Mockito.mockStatic(Instant.class, Mockito.CALLS_REAL_METHODS)) {
            relogio.when(Instant::now).thenReturn(depois);
            var recusado = executar(c.nacional, get(rota + "/arquivo").param("finalidade", "auditoria temporal"),
                    403, "DOC010 prazo vencido retira acesso aos bytes sem depender de revogação ou job");
            assertThat(recusado.getResponse().getContentType()).isNotEqualTo("application/pdf");
            assertThat(recusado.getResponse().getContentAsByteArray()).isNotEqualTo(bytes);
            executar(c.nacional, get(rota).param("finalidade", "auditoria temporal"), 403,
                    "A expiração também impede reutilizar os metadados pessoais na mesma sessão");
            byte[] controle = executar(c.localA, get(rota + "/arquivo").param("finalidade", "gestão documental"),
                    200, "Expiração da concessão individual conserva o acesso do responsável legítimo")
                    .getResponse().getContentAsByteArray();
            assertThat(sha256(controle)).isEqualTo(hash);
        }
        emTransacao(() -> {
            PermissaoDocumentoRestrito permissao = em.find(PermissaoDocumentoRestrito.class, permissaoId);
            assertThat(permissao.getRevogadoEm()).isNull();
            assertThat(permissao.getValidoAte()).isEqualTo(expira.truncatedTo(java.time.temporal.ChronoUnit.MICROS));
            assertThat(em.find(DocumentoComprobatorio.class, documentoId).getSha256Arquivo()).isEqualTo(hash);
            assertThat(em.createQuery("select count(a) from AcessoDocumentoRestrito a where a.documento.id=:d "
                            + "and a.usuarioId=:u and a.tipoAcesso=:t", Long.class)
                    .setParameter("d", documentoId).setParameter("u", c.nacional.getId())
                    .setParameter("t", TipoAcessoDocumento.DOWNLOAD).getSingleResult()).isEqualTo(1L);
            return null;
        });
    }

    private String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
}
