package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.configuracao.propriedades.PropriedadesArmazenamentoArquivo;
import com.example.siggtrat.dto.RetificacaoSeiDocumentoRequisicao;
import com.example.siggtrat.entidade.DocumentoComprobatorio;
import com.example.siggtrat.servico.DocumentoComprobatorioServico;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.AopTestUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** CON010: duas retificações SEI reais da mesma versão com anexo não vazio. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class MestreDocumentoRetificacaoConcorrenteHttpIT extends MestreConcorrenciaSuporte {
    @MockitoSpyBean private DocumentoComprobatorioServico documentos;
    @Autowired private PropriedadesArmazenamentoArquivo armazenamento;

    @Test
    @Timeout(value = 100, unit = TimeUnit.SECONDS)
    void duasRetificacoesDaMesmaVersaoConfirmamUmaCadeiaPreservandoTodosOsBytes() throws Exception {
        Cenario c = criarCenarioPersistido();
        var segundo = segundoGestor(c);
        var contrato = rascunhoPublicado(c, List.of(5), RelogioSistema.hoje().plusDays(5), null, true);
        String raiz = base(contrato.id()) + "/documentos";
        long inicial = consultar(c.localA, raiz, "Publicação já confirmada").get(0).path("id").asLong();
        byte[] bytes = ("%PDF-1.4\n% Prova sintética não vazia " + UUID.randomUUID() + "\n%%EOF").getBytes(StandardCharsets.UTF_8);
        JsonNode origem = dados(executar(c.localA, multipart(raiz + "/" + inicial + "/substituir")
                .file(metadados(mapa("motivo", "Fonte sintética anterior à corrida", "chaveIdempotencia", "CON010-FONTE-" + UUID.randomUUID())))
                .file(new MockMultipartFile("arquivo", "prova.pdf", "application/pdf", bytes)), 200, "Anexo confirmado antes da disputa"));
        long id = origem.path("id").asLong();
        String rota = raiz + "/" + id + "/retificar-sei";
        var a = autenticar(post(rota).contentType(APPLICATION_JSON).content(json(mapa("novaReferenciaSei", "SEI-VENCEDOR-A",
                "motivo", "Retificação concorrente legítima A", "chaveIdempotencia", "CON010-A-" + UUID.randomUUID()))), c.localA);
        var b = autenticar(post(rota).contentType(APPLICATION_JSON).content(json(mapa("novaReferenciaSei", "SEI-PERDEDOR-B",
                "motivo", "Retificação concorrente legítima B", "chaveIdempotencia", "CON010-B-" + UUID.randomUUID()))), segundo);
        var eventosAntes = consultar(c.localA, raiz + "/" + id + "/eventos", "Prefixo documental persistido");
        Map<String, String> arquivosAntes = arquivos();
        Barreira gate = new Barreira();
        doAnswer(i -> coordenar(i, gate)).when(AopTestUtils.<DocumentoComprobatorioServico>getUltimateTargetObject(documentos))
                .retificarSei(eq(contrato.id()), eq(id), any(RetificacaoSeiDocumentoRequisicao.class));
        Corrida corrida = disputar(gate, a, b, sql -> sql.selecaoChave("contrato", "id_contrato", contrato.id()),
                "CON010 Contrato da mesma versão documental");
        conferirCorrida(corrida, 200, 409);
        long novoId = id(corrida.a());
        JsonNode vencedor = consultar(c.localA, raiz + "/" + novoId, "Nova versão após commit");
        JsonNode antiga = consultar(segundo, raiz + "/" + id, "A versão anterior continua acessível");
        JsonNode eventosDepois = consultar(c.localA, raiz + "/" + novoId + "/eventos", "Cadeia após ambos os resultados");
        var integridade = consultar(c.localA, raiz + "/" + novoId + "/evidencias-integridade", "Cadeia documental verificada pelo servidor");
        var versoes = emTransacao(() -> em.createQuery("select d from DocumentoComprobatorio d where d.grupoLogicoId=:grupo order by d.versaoDocumento", DocumentoComprobatorio.class)
                .setParameter("grupo", origem.path("grupoLogicoId").asText()).getResultList().stream()
                .map(d -> mapa("id", d.getId(), "versao", d.getVersaoDocumento(), "estado", d.getEstado().name(),
                        "seiSha", h(d.getReferenciaSei()), "arquivoSha", d.getSha256Arquivo())).toList());
        Map<String, String> arquivosDepois = arquivos();
        registrar("documento-retificacao-concorrente", mapa("corrida", resumo(corrida), "origemId", id, "novoId", novoId,
                "atorA", c.localA.getId(), "atorB", segundo.getId(), "versoes", versoes, "eventosAntesSha", hJson(eventosAntes),
                "eventosDepoisSha", hJson(eventosDepois), "arquivosAntes", arquivosAntes, "arquivosDepois", arquivosDepois,
                "integridadeSha", hJson(integridade)));
        assertAll(() -> assertThat(novoId).isNotEqualTo(id),
                () -> assertThat(vencedor.path("documentoAnteriorId").asLong()).isEqualTo(id),
                () -> assertThat(vencedor.path("versaoDocumento").asInt()).isEqualTo(origem.path("versaoDocumento").asInt() + 1),
                () -> assertThat(vencedor.path("referenciaSei").asText()).isEqualTo("SEI-VENCEDOR-A"),
                () -> assertThat(vencedor.path("sha256Arquivo")).isEqualTo(origem.path("sha256Arquivo")),
                () -> assertThat(antiga.path("referenciaSei")).isEqualTo(origem.path("referenciaSei")),
                () -> assertThat(antiga.path("estado").asText()).isEqualTo("SUBSTITUIDO"),
                () -> assertThat(versoes).hasSize(3),
                () -> assertThat(versoes.stream().filter(v -> h("SEI-PERDEDOR-B").equals(v.get("seiSha"))).toList()).isEmpty(),
                () -> assertThat(eventosDepois).hasSize(eventosAntes.size() + 1),
                () -> assertThat(eventosDepois.get(eventosDepois.size() - 1).path("tipoEvento").asText()).isEqualTo("RETIFICACAO_SEI"),
                () -> assertThat(eventosDepois.get(eventosDepois.size() - 1).path("responsavelId").asLong()).isEqualTo(c.localA.getId()),
                () -> assertThat(integridade.path("cadeiaIntegra").asBoolean()).isTrue(),
                () -> assertThat(arquivosDepois).isEqualTo(arquivosAntes));
        for (int i = 0; i < eventosAntes.size(); i++) assertThat(eventosDepois.get(i)).isEqualTo(eventosAntes.get(i));
        for (long documentoId : List.of(id, novoId)) assertThat(executar(c.localA, get(raiz + "/" + documentoId + "/arquivo"),
                200, "Bytes anteriores e atuais preservados").getResponse().getContentAsByteArray()).isEqualTo(bytes);
    }

    private Map<String, String> arquivos() throws Exception {
        Path root = armazenamento.uploadDir().toAbsolutePath().normalize();
        Map<String, String> out = new LinkedHashMap<>();
        try (var paths = Files.walk(root)) {
            for (Path p : paths.sorted().toList()) {
                assertThat(Files.isSymbolicLink(p)).isFalse();
                if (Files.isRegularFile(p)) out.put(root.relativize(p).toString(), java.util.HexFormat.of().formatHex(
                        java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(p))));
            }
        }
        return Map.copyOf(out);
    }
}
