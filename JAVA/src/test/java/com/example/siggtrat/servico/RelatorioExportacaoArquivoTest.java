package com.example.siggtrat.servico;

import com.example.siggtrat.dominio.acesso.NivelExposicaoDadosPessoais;
import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.LongConsumer;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Arquivos reais em diretório temporário; geração e autorização isoladas. */
class RelatorioExportacaoArquivoTest {
    @TempDir Path diretorio;
    private Path arquivo;
    private RelatorioExportacaoAssincronaServico tarefas;
    private UUID id;
    private static final byte[] CONTEUDO = "Relatório sintético completo".getBytes(java.nio.charset.StandardCharsets.UTF_8);

    @BeforeEach void preparar() throws Exception {
        var contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(new UsernamePasswordAuthenticationToken("owner", "", List.of()));
        SecurityContextHolder.setContext(contexto);
        criarExportacao(CONTEUDO);
    }

    private void criarExportacao(byte[] conteudo) throws Exception {
        arquivo = Files.write(diretorio.resolve("relatorio.xlsx"), conteudo);
        var gerador = mock(RelatorioExportacaoServico.class);
        when(gerador.prepararTerceirizadosXlsx(any(), any(), any(), any(), any(), any(LongConsumer.class)))
                .thenReturn(arquivo);
        var autorizacao = mock(AutorizacaoExportacaoRelatorioServico.class);
        var permissao = new AutorizacaoExportacaoRelatorioServico.Contexto(1L, "owner", PerfilAcesso.GESTOR_LOCAL,
                NivelExposicaoDadosPessoais.COMPLETO, null, null, null, List.of(), true, true, true, false, true);
        when(autorizacao.capturar("owner")).thenReturn(
                new AutorizacaoExportacaoRelatorioServico.Fotografia(permissao, Map.of("usuario:1", 0L)));
        tarefas = new RelatorioExportacaoAssincronaServico(gerador,
                new ArmazenamentoTemporarioRelatorioServico(diretorio.toString(), 3_600_000),
                autorizacao, Runnable::run, 3_600_000);
        id = tarefas.solicitarTerceirizados(null, null, null, false, null, "XLSX", 1L).id();
        assertThat(tarefas.consultar(id).status()).isEqualTo("CONCLUIDO");
    }

    @AfterEach void limpar() { SecurityContextHolder.clearContext(); }

    @ParameterizedTest
    @ValueSource(strings = {"ausente", "vazio", "truncado"})
    void arquivoPerdidoOuIncompletoNaoGeraHandleDeDownload(String falha) throws Exception {
        if (falha.equals("ausente")) Files.delete(arquivo);
        else Files.write(arquivo, falha.equals("vazio") ? new byte[0] : new byte[]{1, 2, 3});
        assertThatThrownBy(() -> tarefas.prepararTransferencia(id))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        erro -> assertThat(erro.getStatusCode().value()).isEqualTo(410));
        assertThat(tarefas.consultar(id).status()).isEqualTo("FALHA");
        assertThat(tarefas.consultar(id).podeBaixar()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ausente", "vazio", "truncado"})
    void perdaDepoisDeAutorizarNaoTransfereArquivoIncompleto(String falha) throws Exception {
        var transferencia = tarefas.prepararTransferencia(id);
        if (falha.equals("ausente")) Files.delete(arquivo);
        else Files.write(arquivo, falha.equals("vazio") ? new byte[0] : new byte[]{1, 2, 3});
        var destino = new ByteArrayOutputStream();
        assertThatThrownBy(() -> tarefas.transferirArquivo(transferencia, destino))
                .isInstanceOf(IOException.class);
        assertThat(destino.size()).isZero();
        assertThat(tarefas.consultar(id).status()).isEqualTo("FALHA");
        assertThat(tarefas.consultar(id).podeBaixar()).isFalse();
    }

    @Test void truncamentoDuranteTransferenciaNaoConfirmaDownloadCompleto() throws Exception {
        byte[] grande = new byte[2 * 1024 * 1024];
        java.util.Arrays.fill(grande, (byte) 7);
        criarExportacao(grande);
        var transferencia = tarefas.prepararTransferencia(id);
        var recebidos = new java.util.concurrent.atomic.AtomicLong();
        OutputStream destino = new OutputStream() {
            private boolean truncou;
            @Override public void write(int valor) throws IOException { write(new byte[]{(byte) valor}, 0, 1); }
            @Override public void write(byte[] bytes, int inicio, int tamanho) throws IOException {
                recebidos.addAndGet(tamanho);
                if (!truncou) {
                    Files.write(arquivo, new byte[0]);
                    truncou = true;
                }
            }
        };
        assertThatThrownBy(() -> tarefas.transferirArquivo(transferencia, destino)).isInstanceOf(IOException.class);
        assertThat(recebidos.get()).isBetween(1L, (long) grande.length - 1);
        assertThat(tarefas.consultar(id).status()).isEqualTo("FALHA");
        assertThat(tarefas.consultar(id).podeBaixar()).isFalse();
    }

    @Test void falhaDaRedePreservaArquivoIntegroParaRepetirDownload() throws Exception {
        var transferencia = tarefas.prepararTransferencia(id);
        OutputStream redeInterrompida = new OutputStream() {
            @Override public void write(int valor) throws IOException { throw new IOException("Rede sintética interrompida"); }
        };
        assertThatThrownBy(() -> tarefas.transferirArquivo(transferencia, redeInterrompida))
                .isInstanceOf(IOException.class);
        assertThat(Files.readAllBytes(arquivo)).isEqualTo(CONTEUDO);
        assertThat(tarefas.consultar(id).status()).isEqualTo("CONCLUIDO");
        assertThat(tarefas.consultar(id).podeBaixar()).isTrue();
        var destino = new ByteArrayOutputStream();
        tarefas.transferirArquivo(tarefas.prepararTransferencia(id), destino);
        assertThat(destino.toByteArray()).isEqualTo(CONTEUDO);
        assertThat(tarefas.consultar(id).status()).isEqualTo("BAIXADO");
        assertThat(Files.exists(arquivo)).isFalse();
    }
}
