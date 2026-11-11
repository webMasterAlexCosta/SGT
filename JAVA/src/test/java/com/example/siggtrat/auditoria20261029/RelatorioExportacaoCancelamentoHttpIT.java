package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.servico.RelatorioExportacaoServico;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongConsumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** ARQ007: cancela trabalho real, inclusive a corrida com o arquivo já gerado. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RelatorioExportacaoCancelamentoHttpIT extends MestreConcorrenciaSuporte {
    @MockitoSpyBean private RelatorioExportacaoServico gerador;

    @Test
    @Timeout(value = 90, unit = TimeUnit.SECONDS)
    void proprietarioCancelaSemConcluirNemPublicarArquivoTardioEControleNormalPermaneceValido() throws Exception {
        Cenario c = criarCenarioPersistido();
        var terceiro = segundoGestor(c);
        var liberar = new CountDownLatch(1);
        var arquivoPronto = new CompletableFuture<Path>();
        var primeiro = new AtomicBoolean(true);
        doAnswer(i -> {
            Path arquivo = (Path) i.callRealMethod();
            if (primeiro.compareAndSet(true, false)) {
                arquivoPronto.complete(arquivo);
                if (!liberar.await(20, TimeUnit.SECONDS)) throw new AssertionError("Geração real não liberada pelo teste");
            }
            return arquivo;
        }).when(AopTestUtils.<RelatorioExportacaoServico>getUltimateTargetObject(gerador))
                .prepararTerceirizadosXlsx(any(), any(), any(), any(), any(), any(LongConsumer.class));
        var antes = SnapshotQuantitativos.capturar(dataSource, c.contratoA.getId(), c.pessoaA.getId());
        String id = solicitar(c).path("id").asText();
        String rota = "/api/relatorios/exportacoes/" + id;
        Path arquivo;
        String hashArquivo;
        MvcResult alheio, cancelamento, repeticao, downloadEnquantoRetido;
        JsonNode imediatamente;
        try {
            arquivo = arquivoPronto.get(20, TimeUnit.SECONDS);
            assertThat(arquivo.toAbsolutePath().normalize()).startsWith(Path.of(System.getenv("RELATORIO_TEMP_DIR")).toAbsolutePath().normalize());
            assertThat(Files.isSymbolicLink(arquivo)).isFalse();
            byte[] bytes = Files.readAllBytes(arquivo);
            assertThat(bytes.length).isGreaterThan(100);
            assertThat(bytes[0]).isEqualTo((byte) 'P'); assertThat(bytes[1]).isEqualTo((byte) 'K');
            hashArquivo = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));
            alheio = mvc.perform(autenticar(post(rota + "/cancelamento"), terceiro)).andReturn();
            cancelamento = mvc.perform(autenticar(post(rota + "/cancelamento"), c.localA)).andReturn();
            repeticao = mvc.perform(autenticar(post(rota + "/cancelamento"), c.localA)).andReturn();
            imediatamente = consultar(c.localA, rota, "Estado enquanto o gerador continua retido");
            downloadEnquantoRetido = mvc.perform(autenticar(get(rota + "/arquivo"), c.localA)).andReturn();
        } finally {
            liberar.countDown();
        }
        JsonNode terminal = aguardarFim(c, id);
        long limiteLimpeza = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (Files.exists(arquivo) && System.nanoTime() < limiteLimpeza) Thread.sleep(20);
        boolean arquivoRemovidoPeloServidor = !Files.exists(arquivo);
        JsonNode depoisGerador = consultar(c.localA, rota, "Conclusão tardia não republica tarefa cancelada");
        MvcResult baixaTardia = mvc.perform(autenticar(get(rota + "/arquivo"), c.localA)).andReturn();
        if (baixaTardia.getRequest().isAsyncStarted()) baixaTardia = mvc.perform(asyncDispatch(baixaTardia)).andReturn();
        final MvcResult baixaFinal = baixaTardia;
        var depois = SnapshotQuantitativos.capturar(dataSource, c.contratoA.getId(), c.pessoaA.getId());
        registrar("exportacao-cancelamento", mapa("id", id, "proprietario", c.localA.getId(), "terceiro", terceiro.getId(),
                "arquivoGeradoSha", hashArquivo, "alheio", SnapshotQuantitativos.http(mapper, alheio),
                "cancelamento", SnapshotQuantitativos.http(mapper, cancelamento), "repeticao", SnapshotQuantitativos.http(mapper, repeticao),
                "estadoImediato", imediatamente, "estadoTerminal", terminal, "estadoAposGerador", depoisGerador,
                "baixaRetida", resumoArquivo(downloadEnquantoRetido),
                "baixaTardia", resumoArquivo(baixaFinal), "servidorRemoveuArquivo", arquivoRemovidoPeloServidor,
                "fatosAntes", antes, "fatosDepois", depois));

        String controleId = solicitar(c).path("id").asText();
        JsonNode controle = aguardarFim(c, controleId);
        MvcResult cancelarConcluida = mvc.perform(autenticar(post("/api/relatorios/exportacoes/" + controleId + "/cancelamento"), c.localA)).andReturn();
        JsonNode controleDepois = consultar(c.localA, "/api/relatorios/exportacoes/" + controleId, "Recusa não invalida arquivo concluído");
        byte[] baixaControle = executar(c.localA, get("/api/relatorios/exportacoes/" + controleId + "/arquivo"),
                200, "Geração sem cancelamento preserva transferência válida").getResponse().getContentAsByteArray();
        registrar("exportacao-cancelamento-controle", mapa("estadoAntes", controle, "estadoDepois", controleDepois,
                "recusa", SnapshotQuantitativos.http(mapper, cancelarConcluida), "bytesBaixados", baixaControle.length));
        assertAll(() -> assertThat(alheio.getResponse().getStatus()).isEqualTo(403),
                () -> assertThat(cancelamento.getResponse().getStatus()).isEqualTo(200),
                () -> assertThat(repeticao.getResponse().getStatus()).isEqualTo(200),
                () -> assertThat(imediatamente.path("status").asText()).isEqualTo("CANCELADO"),
                () -> assertThat(terminal.path("status").asText()).isEqualTo("CANCELADO"),
                () -> assertThat(depoisGerador.path("status").asText()).isEqualTo("CANCELADO"),
                () -> assertThat(depoisGerador.path("concluido").asBoolean()).isFalse(),
                () -> assertThat(depoisGerador.path("falhou").asBoolean()).isFalse(),
                () -> assertThat(depoisGerador.path("podeBaixar").asBoolean()).isFalse(),
                () -> assertThat(downloadEnquantoRetido.getResponse().getStatus()).isEqualTo(409),
                () -> assertThat(baixaFinal.getResponse().getStatus()).isEqualTo(409),
                () -> assertThat(arquivoRemovidoPeloServidor).isTrue(),
                () -> assertThat(depois).isEqualTo(antes),
                () -> assertThat(controle.path("status").asText()).isEqualTo("CONCLUIDO"),
                () -> assertThat(cancelarConcluida.getResponse().getStatus()).isEqualTo(409),
                () -> assertThat(controleDepois.path("status").asText()).isEqualTo("CONCLUIDO"),
                () -> assertThat(baixaControle.length).isGreaterThan(100),
                () -> assertThat(baixaControle[0]).isEqualTo((byte) 'P'),
                () -> assertThat(baixaControle[1]).isEqualTo((byte) 'K'));
    }

    private JsonNode solicitar(Cenario c) throws Exception {
        return dados(executar(c.localA, post("/api/relatorios/terceirizados/exportacao/solicitacao")
                .param("contratoId", c.contratoA.getId().toString()).param("formato", "XLSX"), 202, "Gerador e executor reais"));
    }

    private Map<String, Object> resumoArquivo(MvcResult resposta) throws Exception {
        byte[] bytes = resposta.getResponse().getContentAsByteArray();
        return mapa("status", resposta.getResponse().getStatus(), "tipo", resposta.getResponse().getContentType(),
                "bytes", bytes.length, "sha256", java.util.HexFormat.of().formatHex(
                        java.security.MessageDigest.getInstance("SHA-256").digest(bytes)));
    }

    private JsonNode aguardarFim(Cenario c, String id) throws Exception {
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        JsonNode estado;
        do {
            estado = consultar(c.localA, "/api/relatorios/exportacoes/" + id, "Aguardar estado terminal real");
            if (Set.of("CANCELADO", "CONCLUIDO", "FALHA").contains(estado.path("status").asText())) return estado;
            Thread.sleep(20);
        } while (System.nanoTime() < limite);
        throw new AssertionError("Exportação não atingiu estado terminal em20s");
    }
}
