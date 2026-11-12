package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.configuracao.propriedades.PropriedadesArmazenamentoArquivo;
import com.example.siggtrat.dto.CriacaoOcorrenciaDTO;
import com.example.siggtrat.entidade.Frequencia;
import com.example.siggtrat.servico.OcorrenciaServico;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Falha física no move de beforeCommit, depois de flush real dos fatos. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@TestPropertySource(properties = "frequencia.janela-operacional-dias=30")
class OcorrenciaFalhaFisicaGravacaoHttpIT extends MestreConcorrenciaSuporte {
    @Autowired private PropriedadesArmazenamentoArquivo armazenamento;
    @MockitoSpyBean private OcorrenciaServico ocorrencias;
    private static final byte[] BYTES = "Anexo sintético íntegro do ensaio de I/O".getBytes(StandardCharsets.UTF_8);

    @Test
    void anonimatoNaoMutaEFalhaNoMoveReverteMetadadosFrequenciaHistoricoEAuditoria() throws Exception {
        Cenario c = criarCenarioPersistido();
        LocalDate dia = DataJornadaSintetica.ultimoDiaDeTrabalhoAnterior(c.jornadaA);
        long frequenciaId = dados(executar(c.localA, post("/api/frequencias").contentType(APPLICATION_JSON)
                .content(json(mapa("vinculoContratualId", c.vinculoA.getId(), "dataReferencia", dia,
                        "status", "PRESENCA", "justificativaRetificacao", "Controle anterior à falha física",
                        "chaveIdempotencia", "IO-PRESENCA-" + UUID.randomUUID()))), 200, "Controle persistido")).path("id").asLong();
        Path root = armazenamento.uploadDir().toAbsolutePath().normalize();
        assertThat(System.getenv("FILE_UPLOAD_DIR")).isNotBlank();
        assertThat(root).isEqualTo(Path.of(System.getenv("FILE_UPLOAD_DIR")).toAbsolutePath().normalize());
        assertThat(Files.isSymbolicLink(root)).isFalse();
        Path staging = root.resolve(".staging");
        assertThat(Files.isDirectory(staging)).isTrue();
        Path preservado = root.resolve("controle-preservado-" + UUID.randomUUID() + ".bin");
        Files.write(preservado, BYTES);
        Files.setPosixFilePermissions(preservado, PosixFilePermissions.fromString("rw-------"));

        AtomicInteger chamadasReais = new AtomicInteger();
        AtomicReference<Map<String, Object>> antesCommit = new AtomicReference<>();
        doAnswer(i -> {
            Object resposta = i.callRealMethod();
            chamadasReais.incrementAndGet();
            em.flush();
            long quantidade = em.createQuery("select count(o) from Ocorrencia o where o.empregado.id=:id", Long.class)
                    .setParameter("id", c.empregadoA.getId()).getSingleResult();
            long anexos = em.createQuery("select count(a) from AnexoOcorrencia a where a.ocorrencia.empregado.id=:id", Long.class)
                    .setParameter("id", c.empregadoA.getId()).getSingleResult();
            try (var arquivos = Files.list(staging)) {
                antesCommit.set(mapa("ocorrenciasFlushed", quantidade, "anexosFlushed", anexos,
                        "frequenciaStatusNaoConfirmado", em.find(Frequencia.class, frequenciaId).getStatus(),
                        "arquivosStaging", arquivos.count()));
            }
            return resposta;
        }).when(AopTestUtils.<OcorrenciaServico>getUltimateTargetObject(ocorrencias))
                .criar(any(CriacaoOcorrenciaDTO.class), ArgumentMatchers.anyList());

        // CSRF anônimo emitido pelo endpoint real, sem autenticação inserida no contexto.
        var csrf = mvc.perform(get("/api/autenticacao/csrf")).andReturn();
        assertThat(csrf.getResponse().getStatus()).isEqualTo(200);
        JsonNode token = dados(csrf);
        var anonimo = pedido(c, dia).cookie(csrf.getResponse().getCookies())
                .header(token.path("headerName").asText(), token.path("token").asText());
        var autenticado = autenticar(pedido(c, dia), c.localA);
        Map<String, Object> antes401 = fotografia(root);
        var recusado = mvc.perform(anonimo).andReturn();
        Map<String, Object> depois401 = fotografia(root);
        assertAll(() -> assertThat(recusado.getResponse().getStatus()).isEqualTo(401),
                () -> assertThat(chamadasReais.get()).isZero(),
                () -> assertThat(depois401).isEqualTo(antes401));

        Set<PosixFilePermission> permissoes = Files.getPosixFilePermissions(root);
        org.springframework.test.web.servlet.MvcResult falha;
        try {
            Files.setPosixFilePermissions(root, PosixFilePermissions.fromString("r-x------"));
            assertThat(Files.isWritable(root)).as("O processo não pode contornar a falha por privilégios").isFalse();
            assertThat(Files.isWritable(staging)).isTrue();
            falha = mvc.perform(autenticado).andReturn();
        } finally {
            Files.setPosixFilePermissions(root, permissoes);
        }
        Map<String, Object> transiente = antesCommit.get();
        Map<String, Object> depoisFalha = fotografia(root);
        registrar("ocorrencia-io-rollback", mapa("antes401", antes401, "depois401", depois401,
                "resposta401", SnapshotQuantitativos.http(mapper, recusado), "estadoFlushedAntesDoMove", transiente,
                "respostaFalha", SnapshotQuantitativos.http(mapper, falha), "depoisRollback", depoisFalha));
        assertAll(() -> assertThat(chamadasReais.get()).isEqualTo(1),
                () -> assertThat(transiente).containsEntry("ocorrenciasFlushed", 1L).containsEntry("anexosFlushed", 1L)
                        .containsEntry("frequenciaStatusNaoConfirmado", "ATESTADO").containsEntry("arquivosStaging", 1L),
                () -> assertThat(falha.getResponse().getStatus()).isEqualTo(500),
                () -> assertThat(dados(falha).path("code").asText()).isEqualTo("INTERNAL_OPERATION_FAILED"),
                () -> assertThat(falha.getResolvedException()).hasRootCauseInstanceOf(AccessDeniedException.class),
                () -> assertThat(falha.getResponse().getContentAsString()).doesNotContain(root.toString(), "AccessDeniedException"),
                () -> assertThat(depoisFalha).isEqualTo(depois401),
                () -> assertThat(Files.readAllBytes(preservado)).isEqualTo(BYTES));
        try (var arquivos = Files.list(staging)) { assertThat(arquivos.count()).isZero(); }

        var positivo = executar(c.localA, pedido(c, dia), 201, "Permissões restauradas permitem commit real");
        JsonNode criada = dados(positivo);
        assertThat(criada.path("anexos")).hasSize(1);
        byte[] baixado = executar(c.localA, get("/api/ocorrencias/anexos/" + criada.path("anexos").get(0).path("id").asLong()),
                200, "Controle positivo do mesmo fluxo após rollback").getResponse().getContentAsByteArray();
        assertThat(baixado).isEqualTo(BYTES);
        assertThat(emTransacao(() -> em.find(Frequencia.class, frequenciaId).getStatus())).isEqualTo("ATESTADO");
        registrar("ocorrencia-io-controle-positivo", mapa("resposta", SnapshotQuantitativos.http(mapper, positivo),
                "bytesSha", h(new String(baixado, StandardCharsets.UTF_8)), "aposCommit", fotografia(root)));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"leitura", "fechamento"})
    void falhaNoFluxoDeUploadNaoDeixaArquivosParciais(String etapa) throws Exception {
        Cenario c = criarCenarioPersistido();
        LocalDate dia = DataJornadaSintetica.ultimoDiaDeTrabalhoAnterior(c.jornadaA);
        Path root = armazenamento.uploadDir().toAbsolutePath().normalize();
        Path preservado = root.resolve("controle-upload-" + UUID.randomUUID() + ".bin");
        Files.write(preservado, BYTES);
        Map<String, Object> antes = fotografia(root);
        Set<Path> temporariosAntes;
        try (var arquivos = Files.list(root.resolve(".staging"))) {
            temporariosAntes = Set.copyOf(arquivos.toList());
        }
        AtomicInteger aberturas = new AtomicInteger();
        var falho = new MockMultipartFile("anexos", "falha-io.txt", "text/plain", BYTES) {
            @Override public java.io.InputStream getInputStream() throws java.io.IOException {
                var original = super.getInputStream();
                // A primeira leitura calcula o hash da intenção; a segunda prepara o arquivo.
                if (aberturas.incrementAndGet() == 1) return original;
                return new java.io.FilterInputStream(original) {
                    private boolean iniciou;
                    @Override public int read(byte[] b, int off, int len) throws java.io.IOException {
                        if (!etapa.equals("leitura")) return super.read(b, off, len);
                        if (iniciou) throw new java.io.IOException("Falha sintética de leitura do upload");
                        iniciou = true;
                        return super.read(b, off, Math.min(len, 5));
                    }
                    @Override public void close() throws java.io.IOException {
                        super.close();
                        if (etapa.equals("fechamento")) throw new java.io.IOException("Falha sintética ao fechar upload");
                    }
                };
            }
        };
        try {
            var resposta = executar(c.localA, pedido(c, dia).file(falho), 500,
                    "Falha de I/O deve abortar o upload e limpar todos os temporários da tentativa");
            assertThat(aberturas.get()).isEqualTo(2);
            assertThat(resposta.getResolvedException()).hasRootCauseInstanceOf(java.io.IOException.class);
            assertThat(resposta.getResponse().getContentAsString()).doesNotContain(root.toString(), "IOException");
            Map<String, Object> depois = fotografia(root);
            assertThat(depois.get("arquivos")).as("Temporários da tentativa abortada").isEqualTo(antes.get("arquivos"));
            assertThat(depois).isEqualTo(antes);
            assertThat(Files.readAllBytes(preservado)).isEqualTo(BYTES);
        } finally {
            // Isola a regressão negativa: remove somente resíduos desta tentativa, após as asserções.
            try (var arquivos = Files.list(root.resolve(".staging"))) {
                for (Path arquivo : arquivos.toList()) {
                    if (!temporariosAntes.contains(arquivo)) Files.deleteIfExists(arquivo);
                }
            }
        }
    }

    private MockMultipartHttpServletRequestBuilder pedido(Cenario c, LocalDate dia) {
        return multipart("/api/ocorrencias").file(new MockMultipartFile("anexos", "prova-io.txt", "text/plain", BYTES))
                .param("empregadoId", c.empregadoA.getId().toString()).param("motivo", "ATESTADO")
                .param("dataOcorrencia", dia.toString()).param("dias", "1").param("numeroSei", "SEI-SINTETICO-IO");
    }

    private Map<String, Object> fotografia(Path root) throws Exception {
        descarregarAuditoria();
        Map<String, Object> x = new LinkedHashMap<>();
        var jdbc = new JdbcTemplate(dataSource);
        for (String tabela : List.of("contrato", "pessoa", "vinculo_contratual", "alocacao_vinculo", "ocorrencia",
                "contexto_ocorrencia", "anexo_ocorrencia", "frequencia", "evento_frequencia", "historico_empregado")) {
            x.put(tabela, jdbc.queryForList("select to_jsonb(t)::text from " + tabela + " t order by 1", String.class)
                    .stream().map(this::h).toList());
        }
        x.put("auditoriaNegocial", jdbc.queryForList("select to_jsonb(t)::text from registro_auditoria t where entity_type in "
                + "('Ocorrencia','AnexoOcorrencia','Frequencia','EventoFrequencia','HistoricoEmpregado') order by 1", String.class)
                .stream().map(this::h).toList());
        Map<String, String> arquivos = new LinkedHashMap<>();
        try (var paths = Files.walk(root)) {
            for (Path p : paths.sorted().toList()) {
                assertThat(Files.isSymbolicLink(p)).isFalse();
                if (Files.isRegularFile(p)) arquivos.put(root.relativize(p).toString(),
                        java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(p))));
            }
        }
        x.put("arquivos", arquivos);
        return Map.copyOf(x);
    }
}
