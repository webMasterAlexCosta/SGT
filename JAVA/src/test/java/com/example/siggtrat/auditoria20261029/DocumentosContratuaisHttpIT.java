package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

class DocumentosContratuaisHttpIT extends ContratosHttpBase {
    private static final byte[] PDF = "%PDF-1.4\n1 0 obj\n<< /Type /Catalog >>\nendobj\n%%EOF".getBytes(StandardCharsets.UTF_8);

    @Test
    void seiPodeSerRetificadoReceberAnexoESerInvalidadoComHistoricoPreservado() throws Exception {
        var c = criarCenario();
        var n = publicado(c, 100, null, true);
        JsonNode lista = consultar(c.localA, base(n.id()) + "/documentos", "DOC-001 publicação cria fonte SEI");
        assertThat(lista.size()).isEqualTo(1);
        long original = lista.get(0).path("id").asLong();
        assertThat(lista.get(0).path("referenciaSei").asText()).isEqualTo("SEI-ORIGINAL");
        for (var leitor : List.of(c.localA, c.estadualA, c.nacional, c.presidente, c.admin)) {
            var d = consultar(leitor, base(n.id()) + "/documentos/" + original, "DOC-009 negocial no escopo " + leitor.getPerfil());
            assertThat(d.path("classificacaoAcesso").asText()).isEqualTo("NEGOCIAL");
        }
        executar(c.localB, get(base(n.id()) + "/documentos/" + original), 404, "DOC-009 documento de outra gestão não exposto");
        executar(c.operacionalA, get(base(n.id()) + "/documentos/" + original), 403, "DOC-010 Operacional não acessa documento");
        executar(c.localA, get(base(n.id()) + "/documentos/" + original + "/arquivo"), 404, "fonte somente SEI não possui arquivo");
        var nova = enviar(c.localA, base(n.id()) + "/documentos/" + original + "/retificar-sei",
                mapa("novaReferenciaSei", "SEI-CORRIGIDO", "motivo", "Número materialmente incorreto", "chaveIdempotencia", "sei-" + n.id()),
                200, "DOC-005 nova referência cria versão sem apagar anterior");
        long novaId = nova.path("id").asLong();
        assertThat(nova.path("documentoAnteriorId").asLong()).isEqualTo(original);
        assertThat(nova.path("versaoDocumento").asInt()).isEqualTo(2);
        var antiga = consultar(c.localA, base(n.id()) + "/documentos/" + original, "DOC-005 referência anterior consultável");
        assertThat(antiga.path("referenciaSei").asText()).isEqualTo("SEI-ORIGINAL");
        assertThat(antiga.path("estado").asText()).isEqualTo("SUBSTITUIDO");
        var anexo = dados(executar(c.localA, multipart(base(n.id()) + "/documentos/" + novaId + "/substituir")
                .file(metadados(mapa("motivo", "Anexo recebido após publicação SEI", "chaveIdempotencia", "anexo-" + n.id())))
                .file(new MockMultipartFile("arquivo", "contrato.pdf", "application/pdf", PDF)),
                200, "DOC-006 anexo posterior mantém SEI"));
        long anexoId = anexo.path("id").asLong();
        assertThat(anexo.path("referenciaSei").asText()).isEqualTo("SEI-CORRIGIDO");
        assertThat(anexo.path("documentoAnteriorId").asLong()).isEqualTo(novaId);
        assertThat(anexo.path("sha256Arquivo").asText()).hasSize(64);
        var baixa = executar(c.presidente, get(base(n.id()) + "/documentos/" + anexoId + "/arquivo"), 200, "DOC-006 download negocial binário");
        assertThat(baixa.getResponse().getContentAsByteArray()).isEqualTo(PDF);
        assertThat(baixa.getResponse().getContentType()).isEqualTo("application/pdf");
        assertThat(baixa.getResponse().getHeader("Content-Disposition")).contains("attachment", "contrato.pdf");
        assertThat(baixa.getResponse().getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
        var invalidado = enviar(c.localA, base(n.id()) + "/documentos/" + anexoId + "/invalidar",
                mapa("motivo", "Arquivo enviado errado"), 200, "DOC-007 invalidação preserva arquivo");
        assertThat(invalidado.path("estado").asText()).isEqualTo("INVALIDO");
        executar(c.localA, delete(base(n.id()) + "/documentos/" + anexoId), 409, "DOC-007 arquivo publicado não é apagado");
        em.flush(); em.clear();
        var eventos = consultar(c.localA, base(n.id()) + "/documentos/" + anexoId + "/eventos", "DOC-005/007 cadeia completa auditável após PostgreSQL");
        assertThat(eventos.size()).isGreaterThanOrEqualTo(4);
        JsonNode eventoSei = null;
        for (JsonNode evento : eventos) {
            if ("RETIFICACAO_SEI".equals(evento.path("tipoEvento").asText())) {
                assertThat(eventoSei).as("Uma única retificação do SEI neste cenário").isNull();
                eventoSei = evento;
            }
        }
        assertThat(eventoSei).as("DOC005 a versão possui evento próprio recuperado do PostgreSQL").isNotNull();
        assertThat(eventoSei.path("documentoId").asLong()).isEqualTo(novaId);
        assertThat(eventoSei.path("responsavelId").asLong()).isEqualTo(c.localA.getId());
        assertThat(eventoSei.path("responsavelUsername").asText()).isEqualTo(c.localA.getUsername());
        assertThat(eventoSei.path("motivo").asText()).isEqualTo("Número materialmente incorreto");
        assertThat(eventoSei.path("dataEfetiva").asText()).isEqualTo(n.inicio().toString());
        assertThat(java.time.Instant.parse(eventoSei.path("registradoEm").asText())).isNotNull();
        assertThat(eventoSei.path("referenciaSeiAnterior").asText()).isEqualTo("SEI-ORIGINAL");
        assertThat(eventoSei.path("referenciaSeiNova").asText()).isEqualTo("SEI-CORRIGIDO");
        assertThat(consultar(c.localA, base(n.id()) + "/documentos", "DOC-007 todas versões preservadas").size()).isEqualTo(3);
    }

    @Test
    void substituicaoEInvalidacaoConservamBytesMetadadosEEventosAnteriores() throws Exception {
        var c = criarCenario();
        var n = publicado(c, 100, null, true);
        long original = consultar(c.localA, base(n.id()) + "/documentos", "Fonte SEI sem impor upload de contrato")
                .get(0).path("id").asLong();
        var primeira = dados(executar(c.localA, multipart(base(n.id()) + "/documentos/" + original + "/substituir")
                .file(metadados(mapa("motivo", "Primeiros bytes sintéticos", "chaveIdempotencia", "bytes-a-" + n.id())))
                .file(new MockMultipartFile("arquivo", "primeiro.pdf", "application/pdf", PDF)),
                200, "DOC007 controle de arquivo antes da substituição"));
        long a = primeira.path("id").asLong();
        JsonNode internosA = conteudoPersistido(a);
        JsonNode eventosAntes = consultar(c.localA, base(n.id()) + "/documentos/" + a + "/eventos", "Cadeia antes de substituir bytes");
        byte[] segundoPdf = (new String(PDF, StandardCharsets.UTF_8) + "\n% segunda prova sintética\n").getBytes(StandardCharsets.UTF_8);
        assertThat(sha256(segundoPdf)).isNotEqualTo(sha256(PDF));
        var segunda = dados(executar(c.localA, multipart(base(n.id()) + "/documentos/" + a + "/substituir")
                .file(metadados(mapa("motivo", "Segundos bytes sintéticos", "chaveIdempotencia", "bytes-b-" + n.id())))
                .file(new MockMultipartFile("arquivo", "segundo.pdf", "application/pdf", segundoPdf)),
                200, "DOC007 substituição gera nova versão sem regravar a anterior"));
        long b = segunda.path("id").asLong();
        JsonNode internosB = conteudoPersistido(b);
        assertThat(segunda.path("documentoAnteriorId").asLong()).isEqualTo(a);
        JsonNode aposSubstituicao = consultar(c.localA, base(n.id()) + "/documentos/" + a, "Versão anterior depois da substituição");
        assertThat(aposSubstituicao.path("estado").asText()).isEqualTo("SUBSTITUIDO");
        assertThat(conteudoMetadados(aposSubstituicao)).isEqualTo(conteudoMetadados(primeira));
        assertThat(conteudoPersistido(a)).isEqualTo(internosA);
        assertArquivo(c, n.id(), a, PDF, primeira.path("sha256Arquivo").asText());
        JsonNode eventosSubstituidos = consultar(c.localA, base(n.id()) + "/documentos/" + b + "/eventos", "Cadeia depois da substituição");
        assertPrefixoPreservado(eventosAntes, eventosSubstituidos);
        enviar(c.localA, base(n.id()) + "/documentos/" + b + "/invalidar", mapa("motivo", "Segunda prova incorreta"),
                200, "DOC007 invalidação não apaga nenhuma versão");
        em.flush(); em.clear();
        JsonNode anteriorRelida = consultar(c.localA, base(n.id()) + "/documentos/" + a, "Metadados anteriores relidos após invalidação");
        JsonNode invalidada = consultar(c.localA, base(n.id()) + "/documentos/" + b, "Metadados invalidados relidos");
        assertThat(invalidada.path("estado").asText()).isEqualTo("INVALIDO");
        assertThat(anteriorRelida).isEqualTo(aposSubstituicao);
        assertThat(conteudoMetadados(invalidada)).isEqualTo(conteudoMetadados(segunda));
        assertThat(conteudoPersistido(a)).isEqualTo(internosA);
        assertThat(conteudoPersistido(b)).isEqualTo(internosB);
        assertArquivo(c, n.id(), a, PDF, primeira.path("sha256Arquivo").asText());
        assertArquivo(c, n.id(), b, segundoPdf, segunda.path("sha256Arquivo").asText());
        assertPrefixoPreservado(eventosSubstituidos,
                consultar(c.localA, base(n.id()) + "/documentos/" + b + "/eventos", "História inteira conserva o prefixo após invalidação"));
    }

    private JsonNode conteudoMetadados(JsonNode documento) {
        com.fasterxml.jackson.databind.node.ObjectNode copia = documento.deepCopy();
        // Somente estes três campos do DTO mudam legitimamente pelo ciclo de vida.
        copia.remove(List.of("estado", "versaoAtual", "version"));
        return copia;
    }

    private JsonNode conteudoPersistido(long documento) {
        em.flush(); em.clear();
        Object[] linha = em.createQuery("select d.fatoChaveNormalizada,d.referenciaSeiNormalizada,d.nomeArmazenado,"
                        + "d.chaveIdempotencia,d.hashIntencao from DocumentoComprobatorio d where d.id=:id", Object[].class)
                .setParameter("id", documento).getSingleResult();
        return mapper.valueToTree(java.util.Arrays.asList(linha));
    }

    private void assertPrefixoPreservado(JsonNode antes, JsonNode depois) {
        assertThat(depois.size()).isGreaterThan(antes.size());
        for (int i = 0; i < antes.size(); i++) assertThat(depois.get(i)).isEqualTo(antes.get(i));
    }

    private void assertArquivo(Cenario c, long contrato, long documento, byte[] esperado, String hash) throws Exception {
        byte[] baixado = executar(c.localA, get(base(contrato) + "/documentos/" + documento + "/arquivo"),
                200, "Bytes da versão sintética seguem recuperáveis pelo ID original").getResponse().getContentAsByteArray();
        assertThat(baixado).isEqualTo(esperado);
        assertThat(hash).isEqualTo(sha256(esperado));
        assertThat(sha256(baixado)).isEqualTo(hash);
    }

    private String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    @Test
    void documentoRestritoExigeFinalidadePermissaoEAuditaAcessoComRevogacao() throws Exception {
        var c = criarCenario();
        var n = publicado(c, 100, null, false);
        long publicacao = consultar(c.localA, base(n.id()) + "/eventos", "fonte original publicada").get(0).path("id").asLong();
        var doc = dados(executar(c.localA, multipart(base(n.id()) + "/documentos")
                .file(metadados(mapa("tipoFato", "CONTRATO_ORIGINAL", "fatoId", publicacao,
                        "fatoChave", "ORIGINAL", "titulo", "Documento restrito", "referenciaSei", "SEI-RESTRITO",
                        "classificacaoAcesso", "PESSOAL_RESTRITO", "dataEfetiva", n.inicio(), "chaveIdempotencia", "restrito-" + n.id()))),
                201, "DOC-009 classificação documental própria"));
        long id = doc.path("id").asLong(); String path = base(n.id()) + "/documentos/" + id;
        assertThat(doc.path("estado").asText()).isEqualTo("PUBLICADO");
        executar(c.localA, get(path), 400, "DOC-010 consulta restrita exige finalidade");
        consultar(c.localA, path + "?finalidade=operacao", "DOC-010 Gestor responsável com necessidade declarada");
        for (var perfil : List.of(c.nacional, c.estadualA, c.presidente, c.admin, c.operacionalA))
            executar(perfil, get(path).param("finalidade", "auditoria"), 403, "DOC-010 restrito sem concessão " + perfil.getPerfil());
        JsonNode listaSemFinalidade = consultar(c.presidente, base(n.id()) + "/documentos", "DOC-010 listagem institucional omite restritos");
        for (JsonNode item : listaSemFinalidade) assertThat(item.path("id").asLong()).isNotEqualTo(id);
        var perm = enviar(c.admin, path + "/permissoes", mapa("usuarioId", c.nacional.getId(),
                "finalidade", "auditoria", "justificativa", "Auditoria documental autorizada",
                "autorizacaoAdministrativa", "ATO-DOC", "validoAte", java.time.Instant.now().plusSeconds(3600)),
                200, "DOC-010 concessão técnica excepcional");
        assertThat(perm.path("ativo").asBoolean()).isTrue();
        consultar(c.nacional, path + "?finalidade=auditoria", "DOC-010 acesso restrito autorizado e auditado");
        assertThat(consultar(c.admin, path + "/permissoes", "DOC-010 consulta de permissões").size()).isEqualTo(1);
        var acessos = consultar(c.localA, path + "/acessos", "DOC-010 trilha de consulta restrita");
        assertThat(acessos.size()).isGreaterThanOrEqualTo(2);
        assertThat(acessos.get(1).path("usuarioId").asLong()).isEqualTo(c.nacional.getId());
        enviar(c.admin, path + "/permissoes/" + perm.path("id").asLong() + "/revogar",
                mapa("motivo", "Auditoria concluída", "autorizacaoAdministrativa", "ATO-REVOGA"), 200,
                "DOC-010 revogação explícita de acesso");
        executar(c.nacional, get(path).param("finalidade", "auditoria"), 403, "DOC-010 concessão revogada não autoriza");
        executar(c.estadualA, get(path + "/acessos"), 403, "DOC-010 auditoria de acessos não exposta a perfil sem permissão");
    }

    @Test
    void documentoRascunhoExclusivoPodeSerExcluidoEPublicacaoAceitaSomenteAnexo() throws Exception {
        var c = criarCenario();
        var inicio = RelogioSistema.hoje().plusDays(5);
        long id = enviar(c.localA, "/api/contratos", rascunho(c, "DOC-DRAFT-" + c.contratoA.getId(), 10,
                inicio, inicio.plusMonths(2)), 201, "DOC-008 preparação de rascunho").path("id").asLong();
        enviar(c.localA, base(id) + "/composicoes", composicao(c, c.cargoA.getId(), 10), 201, "CMP-003 composição integral");
        var body = mapa("tipoFato", "CONTRATO_ORIGINAL", "titulo", "Contrato anexado", "classificacaoAcesso", "NEGOCIAL", "dataEfetiva", inicio);
        var doc = dados(executar(c.localA, multipart(base(id) + "/documentos").file(metadados(body))
                .file(new MockMultipartFile("arquivo", "rascunho.pdf", "application/pdf", PDF)), 201, "DOC-008 anexo exclusivo de rascunho"));
        executar(c.localA, delete(base(id) + "/documentos/" + doc.path("id").asLong()), 204, "DOC-008 descarte permitido sem valor SEI externo");
        assertThat(consultar(c.localA, base(id) + "/documentos", "DOC-008 exclusão transacional").size()).isZero();
        var anexo = dados(executar(c.localA, multipart(base(id) + "/documentos").file(metadados(body))
                .file(new MockMultipartFile("arquivo", "contrato.pdf", "application/pdf", PDF)), 201, "DOC-001 fonte binária antes de publicar"));
        var pub = enviar(c.localA, base(id) + "/publicar", mapa("dataPublicacao", RelogioSistema.hoje(), "justificativa", "Fonte apenas anexo"),
                200, "DOC-001 anexo suficiente sem referência SEI");
        assertThat(pub.path("estadoNegocial").asText()).isEqualTo("PUBLICADO");
        assertThat(consultar(c.localA, base(id) + "/documentos/" + anexo.path("id").asLong(), "DOC-001 fonte acompanha publicação")
                .path("estado").asText()).isEqualTo("PUBLICADO");
    }

    @Test
    void uploadValidaClassificacaoFormatoESigiloPorEscopo() throws Exception {
        var c = criarCenario();
        var n = publicado(c, 100, null, true);
        var semClasse = mapa("tipoFato", "CONTRATO_ORIGINAL", "titulo", "Sem classificação", "referenciaSei", "SEI-MESMO");
        executar(c.localA, multipart(base(n.id()) + "/documentos").file(metadados(semClasse)), 400, "DOC-009 classificação obrigatória");
        var body = mapa("tipoFato", "CONTRATO_ORIGINAL", "titulo", "Formato inválido", "classificacaoAcesso", "NEGOCIAL");
        executar(c.localA, multipart(base(n.id()) + "/documentos").file(metadados(body))
                .file(new MockMultipartFile("arquivo", "arquivo.pdf", "application/pdf", "nao-pdf".getBytes(StandardCharsets.UTF_8))),
                415, "assinatura binária incompatível rejeitada");
        executar(c.localA, multipart(base(n.id()) + "/documentos").file(metadados(body))
                .file(new MockMultipartFile("arquivo", "arquivo.exe", "application/octet-stream", PDF)),
                415, "extensão não autorizada rejeitada");
        executar(c.localB, multipart(base(n.id()) + "/documentos").file(metadados(semClasse)), 404, "DOC-009 escrita em contrato fora do escopo rejeitada");
    }
}
