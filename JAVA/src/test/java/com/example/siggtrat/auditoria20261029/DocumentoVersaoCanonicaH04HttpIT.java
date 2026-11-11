package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.DocumentoComprobatorio;
import com.fasterxml.jackson.databind.JsonNode;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * Regressão DOC-VERSION em PostgreSQL real. Não há transação externa de rollback:
 * cada resposta só é comparada depois do commit da transação do serviço.
 * MockMvc cobre MVC/segurança/JPA; não é prova de TCP, Chrome ou restart.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class DocumentoVersaoCanonicaH04HttpIT extends ContratosHttpBase {
    @org.springframework.beans.factory.annotation.Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    @org.springframework.beans.factory.annotation.Autowired
    com.example.siggtrat.configuracao.propriedades.PropriedadesArmazenamentoArquivo armazenamento;
    @ParameterizedTest(name = "{0}; anexo real não vazio={1}")
    @CsvSource({"CRIAR,false", "CRIAR,true", "RETIFICAR,false", "RETIFICAR,true",
            "SUBSTITUIR,false", "SUBSTITUIR,true", "INVALIDAR,false", "INVALIDAR,true"})
    void primeiraRespostaTemVersaoCanonicaAposCommit(String operacao, boolean comArquivo) throws Exception {
        var c = criarCenarioPersistido();
        var n = emTransacao(() -> {
            try { return publicado(c, 10, null, true); }
            catch (Exception erro) { throw new IllegalStateException(erro); }
        });
        long fato = consultar(c.localA, base(n.id()) + "/eventos", "Publicação sintética confirmada").get(0).path("id").asLong();
        String raiz = base(n.id()) + "/documentos";
        String chave = "H04-DOC-" + UUID.randomUUID();
        byte[] original = pdf("Fonte sintética original " + chave);
        byte[] substituto = pdf("Fonte sintética substituta " + chave);
        assertThat(original.length).isGreaterThan(100);
        assertThat(substituto).isNotEqualTo(original);
        Map<String, Object> cadastro = mapa("tipoFato", "CONTRATO_ORIGINAL", "fatoId", fato,
                "titulo", "Fonte sintética para regressão de versão", "referenciaSei", "SEI-ORIGEM-" + chave,
                "classificacaoAcesso", "NEGOCIAL", "dataEfetiva", n.inicio(), "chaveIdempotencia", chave);
        JsonNode criado = dados(executar(c.localA, documentoMultipart(raiz, cadastro, comArquivo ? original : null),
                201, "DOC-VERSION preparar fonte legítima via API, com commit real"));
        String origem = raiz + "/" + criado.path("id").asLong();
        JsonNode primeira;
        Map<String, Object> intencao;
        String destino;
        switch (operacao) {
            case "CRIAR" -> { primeira = criado; intencao = cadastro; destino = raiz; }
            case "RETIFICAR" -> {
                intencao = mapa("novaReferenciaSei", "SEI-RETIFICADO-" + chave, "motivo", "Correção material documentada",
                        "chaveIdempotencia", chave + "-ret");
                destino = origem + "/retificar-sei";
                primeira = enviar(c.localA, destino, intencao, 200, "DOC-VERSION primeira retificação com commit");
            }
            case "SUBSTITUIR" -> {
                intencao = mapa("titulo", "Fonte substituta preservando histórico", "referenciaSei", "SEI-SUBSTITUTO-" + chave,
                        "motivo", "Nova fonte comprovante", "chaveIdempotencia", chave + "-sub");
                destino = origem + "/substituir";
                primeira = dados(executar(c.localA, documentoMultipart(destino, intencao, comArquivo ? substituto : null),
                        200, "DOC-VERSION primeira substituição com commit"));
            }
            case "INVALIDAR" -> {
                intencao = mapa("motivo", "Fonte invalidada com preservação documental");
                destino = origem + "/invalidar";
                primeira = enviar(c.localA, destino, intencao, 200, "DOC-VERSION primeira invalidação com commit");
            }
            default -> throw new AssertionError("Operação desconhecida: " + operacao);
        }
        long id = primeira.path("id").asLong();
        String atual = raiz + "/" + id;
        JsonNode getAposCommit = consultar(c.localA, atual, "DOC-VERSION leitura nova após commit");
        long versionJpa = emTransacao(() -> {
            em.clear();
            return em.find(DocumentoComprobatorio.class, id).getVersion();
        });
        // Registrar o resultado antes do oráculo para preservar o RED discriminante.
        Path prova = Path.of("target/auditoria-http/h04-doc-version", operacao + "-" + comArquivo + ".json");
        Files.createDirectories(prova.getParent());
        Files.writeString(prova, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapa(
                "operacao", operacao, "comArquivo", comArquivo, "id", id,
                "versionPrimeiraResposta", primeira.path("version").asLong(),
                "versionGetAposCommit", getAposCommit.path("version").asLong(), "versionJpa", versionJpa,
                "versaoDocumento", primeira.path("versaoDocumento").asInt(), "camada", "MockMvc + JPA + PostgreSQL real")));
        assertThat(primeira.path("version").asLong()).as("primeira resposta deve trazer a versão persistida, não o valor anterior ao flush")
                .isEqualTo(versionJpa).isEqualTo(getAposCommit.path("version").asLong());
        for (String campo : new String[]{"id", "contratoId", "grupoLogicoId", "versaoDocumento", "documentoAnteriorId",
                "estado", "referenciaSei", "sha256Arquivo", "tamanhoBytes", "downloadDisponivel"}) {
            assertThat(primeira.path(campo)).as("Campo canônico " + campo).isEqualTo(getAposCommit.path(campo));
        }
        assertThat(primeira).as("DTO inteiro, incluindo precisão temporal, autoria e classificação")
                .isEqualTo(getAposCommit);
        byte[] esperado = operacao.equals("SUBSTITUIR") ? substituto : original;
        if (comArquivo) {
            assertThat(primeira.path("sha256Arquivo").asText()).isEqualTo(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(esperado)));
            assertThat(executar(c.localA, get(atual + "/arquivo"), 200, "Bytes reais preservados após commit")
                    .getResponse().getContentAsByteArray()).isEqualTo(esperado);
        } else {
            executar(c.localA, get(atual + "/arquivo"), 404, "Fonte SEI não inventa anexo");
        }
        if (!operacao.equals("CRIAR") && !operacao.equals("INVALIDAR")) {
            assertThat(primeira.path("versaoDocumento").asInt()).isEqualTo(2);
            assertThat(primeira.path("documentoAnteriorId").asLong()).isEqualTo(criado.path("id").asLong());
            assertThat(consultar(c.localA, origem, "Versão original permanece consultável").path("estado").asText()).isEqualTo("SUBSTITUIDO");
            if (comArquivo) assertThat(executar(c.localA, get(origem + "/arquivo"), 200,
                    "Arquivo da versão anterior é imutável").getResponse().getContentAsByteArray()).isEqualTo(original);
        }
        JsonNode fotografiaAntesRepeticao = fotografia(n.id());
        JsonNode replay;
        if (operacao.equals("INVALIDAR")) {
            enviar(c.localA, destino, intencao, 409, "Repetição de invalidação é recusada sem alterar documento");
            replay = consultar(c.localA, atual, "Estado canônico após recusa");
        } else if (operacao.equals("RETIFICAR")) {
            replay = enviar(c.localA, destino, intencao, 200, "Replay idempotente da mesma intenção");
            var conflito = mapa("novaReferenciaSei", "SEI-CONFLITANTE", "motivo", "Correção material documentada",
                    "chaveIdempotencia", chave + "-ret");
            enviar(c.localA, destino, conflito, 409, "Chave reutilizada com outra intenção não reescreve histórico");
        } else {
            replay = dados(executar(c.localA, documentoMultipart(destino, intencao,
                    comArquivo ? esperado : null), operacao.equals("CRIAR") ? 201 : 200,
                    "Replay multipart da mesma intenção e dos mesmos bytes"));
        }
        if (operacao.equals("CRIAR") || operacao.equals("SUBSTITUIR")) {
            var conflito = new java.util.LinkedHashMap<>(intencao);
            conflito.put("titulo", "Intenção conflitante não autorizada pela chave anterior");
            executar(c.localA, documentoMultipart(destino, conflito, comArquivo ? esperado : null),
                    409, "Chave multipart conflitante é recusada sem novos bytes nem metadados");
        }
        assertThat(replay.path("id")).isEqualTo(primeira.path("id"));
        assertThat(replay.path("version").asLong()).isEqualTo(versionJpa);
        assertThat(replay).as("Replay conserva o DTO inteiro, sem apagar campos temporais ou de autoria")
                .isEqualTo(primeira);
        JsonNode fotografiaDepoisRepeticao = fotografia(n.id());
        assertThat(fotografiaDepoisRepeticao).as("Replay e recusa 409 não alteram versões, eventos, âncoras, cardinalidades ou arquivos")
                .isEqualTo(fotografiaAntesRepeticao);
        assertThat(consultar(c.localA, atual, "Recusa/replay preserva versão canônica").path("version").asLong()).isEqualTo(versionJpa);
        JsonNode integridade = consultar(c.localA, atual + "/evidencias-integridade", "Cadeia documental permanece íntegra");
        assertThat(integridade.path("cadeiaIntegra").asBoolean()).as(integridade.toPrettyString()).isTrue();
        executar(c.localB, get(atual), 404, "Correção de versão não amplia escopo entre gestões");
        executar(c.operacionalA, get(atual), 403, "Correção de versão mantém restrição operacional");
        Files.writeString(prova, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapa(
                "operacao", operacao, "comArquivo", comArquivo, "id", id,
                "primeiraResposta", primeira, "getAposCommit", getAposCommit, "versionJpa", versionJpa,
                "replay", replay, "antesReplayRecusa", fotografiaAntesRepeticao,
                "depoisReplayRecusa", fotografiaDepoisRepeticao, "cadeiaIntegra", true,
                "resultado", "PASS", "camada", "MockMvc + JPA + PostgreSQL real; sem prova de navegador ou TCP")));
    }

    /** Somente SELECT. Nenhuma tabela ou linha é criada/corrigida por SQL. */
    private JsonNode fotografia(long contratoId) throws Exception {
        var leitura = new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        leitura.setReadOnly(true);
        var tabelas = leitura.execute(status -> mapa(
                "versoes", linhas("select * from documento_comprobatorio where id_contrato=? order by id_documento", contratoId),
                "eventos", linhas("select e.* from documento_comprobatorio_evento e join documento_comprobatorio d "
                        + "on d.id_documento=e.id_documento where d.id_contrato=? order by e.id_documento_evento", contratoId)));
        var versoes = (java.util.List<?>) tabelas.get("versoes");
        var eventos = (java.util.List<?>) tabelas.get("eventos");
        var arquivos = new java.util.TreeMap<String, Object>();
        Path raiz = armazenamento.uploadDir().toAbsolutePath().normalize();
        if (Files.exists(raiz)) try (var caminhos = Files.walk(raiz)) {
            for (Path arquivo : caminhos.filter(Files::isRegularFile).sorted().toList()) {
                arquivos.put(raiz.relativize(arquivo).toString(), mapa("bytes", Files.size(arquivo),
                        "sha256", HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(arquivo)))));
            }
        }
        return mapper.valueToTree(mapa("versoes", versoes, "eventos", eventos,
                "quantidadeVersoes", versoes.size(), "quantidadeEventos", eventos.size(), "arquivos", arquivos));
    }

    private java.util.List<java.util.Map<String, String>> linhas(String sql, long contratoId) {
        return jdbc.query(sql, (rs, numeroLinha) -> {
            var linha = new java.util.LinkedHashMap<String, String>();
            var meta = rs.getMetaData();
            for (int coluna = 1; coluna <= meta.getColumnCount(); coluna++)
                linha.put(meta.getColumnLabel(coluna), rs.getString(coluna));
            return linha;
        }, contratoId);
    }

    private MockMultipartHttpServletRequestBuilder documentoMultipart(String path, Object metadados, byte[] bytes) throws Exception {
        var request = multipart(path).file(metadados(metadados));
        if (bytes != null) request.file(new MockMultipartFile("arquivo", "fonte-sintetica.pdf", "application/pdf", bytes));
        return request;
    }

    private byte[] pdf(String texto) throws Exception {
        var saida = new ByteArrayOutputStream();
        var documento = new Document();
        PdfWriter.getInstance(documento, saida);
        documento.open(); documento.add(new Paragraph(texto)); documento.close();
        return saida.toByteArray();
    }
}
