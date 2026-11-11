package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.dominio.documento.TipoAcessoDocumento;
import com.example.siggtrat.entidade.AcessoDocumentoRestrito;
import com.example.siggtrat.entidade.DocumentoComprobatorio;
import com.example.siggtrat.entidade.GestaoLocal;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.servico.RelatorioExportacaoAssincronaServico;
import com.fasterxml.jackson.databind.JsonNode;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * DOC-010, PER-010/011 e REL-004: autorização presente em download real.
 * Segurança MVC, serviços, executor assíncrono, arquivos e PostgreSQL são reais.
 * Fixtures e mudanças de permissão/escopo são confirmadas em transações próprias.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RevisaoDownloadsAutorizacaoAtualHttpIT extends ContratosHttpBase {
    @Autowired RelatorioExportacaoAssincronaServico exportacoes;

    @Test
    void downloadRestritoRevalidaRevogacaoSemAlterarBytesOuAuditoriaAnterior() throws Exception {
        var c = criarCenarioPersistido();
        NovoContrato n = emTransacao(() -> {
            try { return publicado(c, 5, null, false); }
            catch (Exception ex) { throw new IllegalStateException(ex); }
        });
        long publicacao = consultar(c.localA, base(n.id()) + "/eventos", "Fonte publicada do documento restrito")
                .get(0).path("id").asLong();
        byte[] pdf = pdfReal("Documento pessoal de teste " + n.id());
        String hash = sha256(pdf);
        var documento = dados(executar(c.localA, multipart(base(n.id()) + "/documentos")
                .file(metadados(mapa("tipoFato", "CONTRATO_ORIGINAL", "fatoId", publicacao,
                        "fatoChave", "ORIGINAL", "titulo", "Documento pessoal com bytes reais",
                        "classificacaoAcesso", "PESSOAL_RESTRITO", "dataEfetiva", n.inicio(),
                        "chaveIdempotencia", "download-restrito-" + n.id())))
                .file(new MockMultipartFile("arquivo", "restrito.pdf", "application/pdf", pdf)),
                201, "DOC009 arquivo PDF real com classificação própria"));
        long documentoId = documento.path("id").asLong();
        String rota = base(n.id()) + "/documentos/" + documentoId;
        assertThat(documento.path("sha256Arquivo").asText()).isEqualTo(hash);
        executar(c.nacional, get(rota + "/arquivo").param("finalidade", "auditoria"), 403,
                "DOC010 arquivo restrito recusa download sem concessão");

        var permissao = enviar(c.admin, rota + "/permissoes", mapa("usuarioId", c.nacional.getId(),
                "finalidade", "auditoria", "justificativa", "Auditoria autorizada dos bytes",
                "autorizacaoAdministrativa", "ATO-DOWNLOAD", "validoAte", Instant.now().plusSeconds(3600)),
                200, "DOC010 concessão confirmada em transação independente");
        long permissaoId = permissao.path("id").asLong();
        var autorizado = executar(c.nacional, get(rota + "/arquivo").param("finalidade", "auditoria"), 200,
                "DOC010 download restrito concedido entrega bytes e audita uso");
        assertThat(autorizado.getResponse().getContentAsByteArray()).isEqualTo(pdf);
        assertThat(sha256(autorizado.getResponse().getContentAsByteArray())).isEqualTo(hash);
        assertThat(autorizado.getResponse().getContentType()).isEqualTo("application/pdf");
        emTransacao(() -> {
            var acessos = em.createQuery("select a from AcessoDocumentoRestrito a "
                            + "where a.documento.id=:doc and a.usuarioId=:usuario and a.tipoAcesso=:tipo",
                    AcessoDocumentoRestrito.class).setParameter("doc", documentoId)
                    .setParameter("usuario", c.nacional.getId()).setParameter("tipo", TipoAcessoDocumento.DOWNLOAD)
                    .getResultList();
            assertThat(acessos).hasSize(1);
            assertThat(acessos.get(0).getPermissaoId()).isEqualTo(permissaoId);
            assertThat(acessos.get(0).getFinalidade()).isEqualTo("auditoria");
            assertThat(acessos.get(0).getAcessadoEm()).isNotNull();
            return null;
        });
        JsonNode trilhaAutorizada = fotografiaDownloads(documentoId, c.nacional.getId());
        assertThat(trilhaAutorizada.size()).isEqualTo(1);

        enviar(c.admin, rota + "/permissoes/" + permissaoId + "/revogar", mapa(
                "motivo", "Auditoria encerrada; autorização retirada", "autorizacaoAdministrativa", "ATO-REVOGACAO"),
                200, "DOC010 revogação persistida antes da nova requisição de arquivo");
        var negado = executar(c.nacional, get(rota + "/arquivo").param("finalidade", "auditoria"), 403,
                "DOC010 mesma sessão e URL não reutilizam concessão revogada para bytes");
        assertThat(negado.getResponse().getContentType()).isNotEqualTo("application/pdf");
        assertThat(negado.getResponse().getContentAsByteArray()).isNotEqualTo(pdf);
        assertThat(fotografiaDownloads(documentoId, c.nacional.getId())).isEqualTo(trilhaAutorizada);
        emTransacao(() -> {
            assertThat(em.find(DocumentoComprobatorio.class, documentoId).getSha256Arquivo()).isEqualTo(hash);
            assertThat(em.createQuery("select count(a) from AcessoDocumentoRestrito a "
                            + "where a.documento.id=:doc and a.usuarioId=:usuario and a.tipoAcesso=:tipo", Long.class)
                    .setParameter("doc", documentoId).setParameter("usuario", c.nacional.getId())
                    .setParameter("tipo", TipoAcessoDocumento.DOWNLOAD).getSingleResult()).isEqualTo(1L);
            return null;
        });
        var controle = executar(c.localA, get(rota + "/arquivo").param("finalidade", "gestao documental"), 200,
                "DOC010 revogação individual não destrói o arquivo nem impede responsável legítimo");
        assertThat(sha256(controle.getResponse().getContentAsByteArray())).isEqualTo(hash);
    }

    @ParameterizedTest(name = "mudancaAtual={0}")
    @ValueSource(strings = {"GESTAO", "PERFIL_MINIMIZADO"})
    void exportacaoProntaRevalidaAutorizacaoAtualSemConsumirArquivoRecusado(String mudanca) throws Exception {
        var c = criarCenarioPersistido();
        // A exportação de controle exige fonte do dia, antes da revogação que será testada.
        declararFonteNominalSintetica(c, "DOWNLOAD-REVOGACAO-" + mudanca);
        enviar(c.localA, "/api/frequencias", mapa("vinculoContratualId", c.vinculoA.getId(),
                "dataReferencia", DATA, "status", "FALTA",
                "justificativaRetificacao", "Fonte da exportação que terá acesso reavaliado",
                "chaveIdempotencia", "download-exportacao-" + UUID.randomUUID()), 200,
                "Fonte persistida antes do executor assíncrono real");
        var solicitada = dados(executar(c.localA, post("/api/relatorios/frequencia-consolidada/exportacao/solicitacao")
                .param("inicio", DATA.toString()).param("fim", DATA.toString())
                .param("contratoIds", c.contratoA.getId().toString()).param("formato", "XLSX")
                .param("totalEstimado", "1"), 202, "REL004 geração real pelo Gestor Local original"));
        String id = solicitada.path("id").asText();
        JsonNode pronta = aguardar(c, id);
        var transferenciaPreparada = observarTransferenciaPreparada(c.localA, id);
        Path arquivo = transferenciaPreparada.arquivo();
        byte[] original = Files.readAllBytes(arquivo);
        String hash = sha256(original);
        assertThat(pronta.path("tamanhoBytes").asLong()).isEqualTo(original.length);
        assertThat(textoXlsx(original)).contains(c.pessoaA.getNome()).doesNotContain(c.pessoaB.getNome());

        emTransacao(() -> {
            UsuarioAplicacao atual = em.find(UsuarioAplicacao.class, c.localA.getId());
            if (mudanca.equals("GESTAO")) {
                var destino = em.find(GestaoLocal.class, c.gestaoB.getId());
                atual.setGestaoLocal(destino); atual.setSuperintendencia(destino.getSuperintendencia());
            } else {
                // Presidente tem consulta nacional, porém não pode receber o
                // arquivo nominal produzido sob o perfil operacional anterior.
                atual.setPerfil(PerfilAcesso.PRESIDENTE);
                atual.setGestaoLocal(null); atual.setSuperintendencia(null);
            }
            return null;
        });
        emTransacao(() -> {
            UsuarioAplicacao atual = em.find(UsuarioAplicacao.class, c.localA.getId());
            if (mudanca.equals("GESTAO")) assertThat(atual.getGestaoLocal().getId()).isEqualTo(c.gestaoB.getId());
            else assertThat(atual.getPerfil()).isEqualTo(PerfilAcesso.PRESIDENTE);
            return null;
        });
        var recusado = executar(c.localA, get("/api/relatorios/exportacoes/{id}/arquivo", id), 403,
                "PER010/011 proprietário original perdeu o escopo ou a exposição nominal que autorizava o arquivo");
        assertThat(recusado.getResponse().getContentType()).doesNotContain("spreadsheetml");
        executar(c.localA, get("/api/relatorios/exportacoes/{id}", id), 403,
                "Metadados e nome do arquivo pronto também exigem a autorização persistida atual");
        ByteArrayOutputStream fluxoRecusado = new ByteArrayOutputStream();
        var segurancaAnterior = SecurityContextHolder.getContext();
        SecurityContextHolder.clearContext();
        try {
            ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                    () -> exportacoes.transferirArquivo(transferenciaPreparada, fluxoRecusado),
                    "Handle preparado antes da revogação é revalidado antes do primeiro byte na thread de streaming");
            assertThat(erro.getStatusCode().value()).isEqualTo(403);
            assertThat(fluxoRecusado.size()).isZero();
        } finally { SecurityContextHolder.setContext(segurancaAnterior); }
        assertThat(Files.isRegularFile(arquivo)).as("Recusa não consome o arquivo pronto").isTrue();
        assertThat(sha256(Files.readAllBytes(arquivo))).isEqualTo(hash);

        emTransacao(() -> {
            UsuarioAplicacao atual = em.find(UsuarioAplicacao.class, c.localA.getId());
            var origem = em.find(GestaoLocal.class, c.gestaoA.getId());
            atual.setPerfil(PerfilAcesso.GESTOR_LOCAL); atual.setGestaoLocal(origem);
            atual.setSuperintendencia(origem.getSuperintendencia());
            return null;
        });
        var aindaPronta = consultar(c.localA, "/api/relatorios/exportacoes/" + id,
                "Controle: restauração da autorização conserva a mesma tarefa concluída");
        assertThat(aindaPronta.path("status").asText()).isEqualTo("CONCLUIDO");
        assertThat(aindaPronta.path("podeBaixar").asBoolean()).isTrue();
        var permitido = executar(c.localA, get("/api/relatorios/exportacoes/{id}/arquivo", id), 200,
                "Autorização atual restaurada permite baixar exatamente o arquivo original");
        assertThat(sha256(permitido.getResponse().getContentAsByteArray())).isEqualTo(hash);
        assertThat(permitido.getResponse().getContentAsByteArray()).isEqualTo(original);
        assertThat(consultar(c.localA, "/api/relatorios/exportacoes/" + id,
                "Somente o download autorizado consome a tarefa").path("status").asText()).isEqualTo("BAIXADO");
    }

    private JsonNode aguardar(Cenario c, String id) throws Exception {
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        JsonNode estado = null;
        do {
            estado = consultar(c.localA, "/api/relatorios/exportacoes/" + id, "Executor real conclui exportação");
            assertThat(estado.path("falhou").asBoolean()).as(estado.toString()).isFalse();
            if (estado.path("podeBaixar").asBoolean()) return estado;
            Thread.sleep(100);
        } while (System.nanoTime() < limite);
        return fail("Exportação não concluiu em 30s: " + estado);
    }

    private JsonNode fotografiaDownloads(long documento, long usuario) {
        return emTransacao(() -> mapper.valueToTree(em.createQuery(
                        "select a.id,a.documento.id,a.tipoAcesso,a.finalidade,a.usuarioId,a.usuarioUsername,"
                                + "a.perfisFotografia,a.permissaoId,a.acessadoEm from AcessoDocumentoRestrito a "
                                + "where a.documento.id=:doc and a.usuarioId=:usuario and a.tipoAcesso=:tipo order by a.id", Object[].class)
                .setParameter("doc", documento).setParameter("usuario", usuario).setParameter("tipo", TipoAcessoDocumento.DOWNLOAD)
                .getResultList().stream().map(java.util.Arrays::asList).toList()));
    }

    private RelatorioExportacaoAssincronaServico.TransferenciaExportacao observarTransferenciaPreparada(
            UsuarioAplicacao usuario, String id) {
        // Leitura do handle público antes da perda de autorização serve apenas
        // para medir os bytes já gerados; não transfere nem consome a tarefa.
        // Todos os downloads que exercitam a autorização passam por HTTP/JWT.
        var anterior = SecurityContextHolder.getContext();
        var contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(new UsernamePasswordAuthenticationToken(usuario.getUsername(), null,
                List.of(new SimpleGrantedAuthority(usuario.getPerfil().autoridade()))));
        SecurityContextHolder.setContext(contexto);
        try { return exportacoes.prepararTransferencia(UUID.fromString(id)); }
        finally { SecurityContextHolder.setContext(anterior); }
    }

    private byte[] pdfReal(String texto) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        Document documento = new Document();
        PdfWriter.getInstance(documento, buffer);
        documento.open(); documento.add(new Paragraph(texto)); documento.close();
        byte[] bytes = buffer.toByteArray();
        PdfReader leitor = new PdfReader(bytes);
        try { assertThat(leitor.getNumberOfPages()).isEqualTo(1); }
        finally { leitor.close(); }
        return bytes;
    }

    private String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private String textoXlsx(byte[] bytes) throws Exception {
        StringBuilder xml = new StringBuilder();
        boolean workbook = false;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            for (var item = zip.getNextEntry(); item != null; item = zip.getNextEntry()) {
                if (item.getName().equals("xl/workbook.xml")) workbook = true;
                if (item.getName().endsWith(".xml")) xml.append(new String(zip.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        assertThat(workbook).as("Controle estrutural do XLSX gerado").isTrue();
        return xml.toString();
    }
}
