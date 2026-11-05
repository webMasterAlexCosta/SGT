package com.example.siggtrat.etapa11;

import com.example.siggtrat.dto.AvisoRevisaoRelatorioDTO;
import com.example.siggtrat.repositorio.JornadaRepositorio;
import com.example.siggtrat.repositorio.LotacaoRepositorio;
import com.example.siggtrat.servico.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ExportacaoRelatorioAvisoEtapa11Test {
    @TempDir Path temporarios;

    @Test
    void xlsxExibeAvisoEQuantidadeDePendenciasNoProprioArquivo() throws Exception {
        RelatorioFrequenciaConsolidadoServico frequencia = mock(RelatorioFrequenciaConsolidadoServico.class);
        when(frequencia.processarFrequenciaEmLotes(any(), any(), anyCollection(), isNull(), isNull(),
                anyInt(), any())).thenReturn(0L);
        PendenciaAdministrativaServico pendencias = mock(PendenciaAdministrativaServico.class);
        when(pendencias.avisoRelatorio(any(), any(), anyCollection(), isNull(), isNull()))
                .thenReturn(AvisoRevisaoRelatorioDTO.de(2L));
        RelatorioExportacaoServico exportacao = new RelatorioExportacaoServico(
                frequencia, mock(RelatorioTerceirizadosServico.class),
                new ArmazenamentoTemporarioRelatorioServico(temporarios.toString(), 3_600_000L),
                mock(LotacaoRepositorio.class), mock(JornadaRepositorio.class), pendencias);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        exportacao.exportarFrequenciaXlsx(bytes, LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31), List.of(10L), null, null);

        String xml = conteudoXml(bytes.toByteArray());
        assertThat(xml).contains("SUJEITO A REVISÃO", "2 pendências");
    }

    private String conteudoXml(byte[] xlsx) throws Exception {
        StringBuilder conteudo = new StringBuilder();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(xlsx))) {
            for (ZipEntry entrada = zip.getNextEntry(); entrada != null; entrada = zip.getNextEntry()) {
                if (entrada.getName().endsWith(".xml")) {
                    conteudo.append(new String(zip.readAllBytes(), StandardCharsets.UTF_8));
                }
            }
        }
        return conteudo.toString();
    }
}
