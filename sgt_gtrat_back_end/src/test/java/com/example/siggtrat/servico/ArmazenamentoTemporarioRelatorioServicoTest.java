package com.example.siggtrat.servico;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ArmazenamentoTemporarioRelatorioServicoTest {

    @TempDir
    Path tempDir;

    @Test
    void deveGerarNoDiscoTransferirEExcluirTemporario() throws Exception {
        ArmazenamentoTemporarioRelatorioServico servico =
                new ArmazenamentoTemporarioRelatorioServico(tempDir.toString(), 60 * 60 * 1000L);
        ByteArrayOutputStream destino = new ByteArrayOutputStream();
        byte[] esperado = "relatorio-completo".getBytes(StandardCharsets.UTF_8);

        servico.gerarTransferirEExcluir("frequencia", ".xlsx", destino, arquivo -> arquivo.write(esperado));

        assertArrayEquals(esperado, destino.toByteArray());
        try (var arquivos = Files.list(tempDir)) {
            assertEquals(0L, arquivos.count(), "temporários devem ser excluídos após o download");
        }
    }

    @Test
    void deveExcluirTemporarioQuandoGeracaoFalhar() throws Exception {
        ArmazenamentoTemporarioRelatorioServico servico =
                new ArmazenamentoTemporarioRelatorioServico(tempDir.toString(), 60 * 60 * 1000L);

        assertThrows(IOException.class, () -> servico.gerarTransferirEExcluir(
                "absenteismo", ".pdf", new ByteArrayOutputStream(), arquivo -> {
                    arquivo.write("parcial".getBytes(StandardCharsets.UTF_8));
                    throw new IOException("falha simulada");
                }));

        try (var arquivos = Files.list(tempDir)) {
            assertEquals(0L, arquivos.count(), "falhas não podem deixar arquivo parcial");
        }
    }

    @Test
    void limpezaDeSegurancaRemoveSomenteArquivoExpirado() throws Exception {
        ArmazenamentoTemporarioRelatorioServico servico =
                new ArmazenamentoTemporarioRelatorioServico(tempDir.toString(), 30 * 60 * 1000L);
        Path antigo = Files.writeString(tempDir.resolve("antigo.xlsx.part"), "x");
        Path recente = Files.writeString(tempDir.resolve("recente.xlsx.part"), "y");
        Files.setLastModifiedTime(antigo, FileTime.from(Instant.now().minusSeconds(2 * 60 * 60)));

        servico.limparExpirados();

        assertEquals(false, Files.exists(antigo));
        assertEquals(true, Files.exists(recente));
    }
}
