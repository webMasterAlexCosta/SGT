package com.example.siggtrat.servico;

import com.example.siggtrat.constante.ConstantesDominio;
import com.example.siggtrat.dto.RelatorioAbsenteismoDTO;
import com.example.siggtrat.dto.RelatorioFrequenciaConsolidadoDTO;
import com.example.siggtrat.dto.RelatorioTerceirizadosDTO;
import com.example.siggtrat.dto.AvisoRevisaoRelatorioDTO;
import com.example.siggtrat.utilitario.XlsxStreamingWriter;
import com.example.siggtrat.utilitario.XlsxStreamingWriter.Cell;
import com.example.siggtrat.repositorio.JornadaRepositorio;
import com.example.siggtrat.repositorio.LotacaoRepositorio;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongConsumer;

/**
 * Exportação de relatórios nacionais em lotes e com persistência temporária no servidor.
 *
 * <p>Cada exportação é montada primeiro em disco, usando paginação por chave de
 * 5.000 colaboradores. Apenas o lote corrente permanece na heap. Quando o XLSX/PDF
 * está íntegro e finalizado, o arquivo é enviado ao cliente e removido do temporário.</p>
 */
@Service
public class RelatorioExportacaoServico {

    private static final int LOTE = RelatorioFrequenciaConsolidadoServico.TAMANHO_LOTE_EXPORTACAO;
    private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_HORA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATA_HORA_ARQUIVO = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final ZoneId ZONA_BR = ZoneId.of(ConstantesDominio.TIMEZONE_PADRAO);

    private static final Color AZUL = new Color(0, 65, 107);
    private static final Color AZUL_ESCURO = new Color(0, 59, 92);
    private static final Color AZUL_FIM_SEMANA = new Color(36, 76, 145);
    private static final Color AMARELO = new Color(255, 212, 0);
    private static final Color CINZA = new Color(241, 245, 249);
    private static final Color CINZA_ZEBRA = new Color(248, 250, 252);
    private static final Color BORDA = new Color(220, 229, 234);
    private static final Color TEXTO = new Color(51, 65, 85);
    private static final Color TEXTO_SUAVE = new Color(100, 116, 139);
    private static final Color BRANCO = Color.WHITE;

    private static final Color VERDE_CLARO = new Color(209, 250, 229);
    private static final Color VERDE_TEXTO = new Color(20, 83, 45);
    private static final Color LARANJA_CLARO = new Color(255, 237, 213);
    private static final Color LARANJA_TEXTO = new Color(120, 53, 15);
    private static final Color VERMELHO_CLARO = new Color(254, 226, 226);
    private static final Color VERMELHO_TEXTO = new Color(185, 28, 28);
    private static final Color AZUL_CLARO = new Color(219, 234, 254);
    private static final Color AZUL_TEXTO = new Color(30, 58, 138);
    private static final Color ROXO_CLARO = new Color(233, 213, 255);
    private static final Color ROXO_TEXTO = new Color(88, 28, 135);
    private static final Color LARANJA_FORTE = new Color(249, 115, 22);
    private static final Color TEAL_CLARO = new Color(204, 251, 241);
    private static final Color TEAL_TEXTO = new Color(15, 118, 110);
    private static final Color DESCANSO_CLARO = new Color(243, 244, 246);
    private static final Color DESCANSO_TEXTO = new Color(107, 114, 128);
    private static final Color ROSA_CLARO = new Color(255, 228, 230);
    private static final Color ROSA_TEXTO = new Color(190, 18, 60);
    private static final Color AMBAR_CLARO = new Color(254, 243, 199);
    private static final Color AMBAR_TEXTO = new Color(180, 83, 9);

    private static final List<StatusVisual> LEGENDA_FREQUENCIA = List.of(
            new StatusVisual("P", "Presença Integral", VERDE_CLARO, VERDE_TEXTO, new Color(74, 222, 128), XlsxStreamingWriter.STYLE_STATUS_P),
            new StatusVisual("PP", "Presença Parcial", LARANJA_CLARO, LARANJA_TEXTO, new Color(251, 146, 60), XlsxStreamingWriter.STYLE_STATUS_PP),
            new StatusVisual("F", "Falta Injustificada", VERMELHO_CLARO, VERMELHO_TEXTO, new Color(248, 113, 113), XlsxStreamingWriter.STYLE_STATUS_F),
            new StatusVisual("AT", "Atestado Médico", AZUL_CLARO, AZUL_TEXTO, new Color(96, 165, 250), XlsxStreamingWriter.STYLE_STATUS_AT),
            new StatusVisual("FER", "Feriado", ROXO_CLARO, ROXO_TEXTO, new Color(192, 132, 252), XlsxStreamingWriter.STYLE_STATUS_FER),
            new StatusVisual("DP", "DSR Perdido", LARANJA_FORTE, BRANCO, new Color(234, 88, 12), XlsxStreamingWriter.STYLE_STATUS_DP),
            new StatusVisual("RT", "Repouso Trabalhado", TEAL_CLARO, TEAL_TEXTO, new Color(45, 212, 191), XlsxStreamingWriter.STYLE_STATUS_RT),
            new StatusVisual("D", "Descanso (Folga/DSR)", DESCANSO_CLARO, DESCANSO_TEXTO, new Color(209, 213, 219), XlsxStreamingWriter.STYLE_STATUS_D),
            new StatusVisual("SUSP", "Suspensão", ROSA_CLARO, ROSA_TEXTO, new Color(254, 205, 211), XlsxStreamingWriter.STYLE_STATUS_SUSP),
            new StatusVisual("ADV", "Advertência", AMBAR_CLARO, AMBAR_TEXTO, new Color(253, 230, 138), XlsxStreamingWriter.STYLE_STATUS_ADV),
            new StatusVisual("FR", "Folga Remunerada", TEAL_CLARO, TEAL_TEXTO, new Color(153, 246, 228), XlsxStreamingWriter.STYLE_STATUS_FR)
    );

    private static final LongConsumer SEM_PROGRESSO = ignorado -> { };

    private final RelatorioFrequenciaConsolidadoServico frequenciaServico;
    private final RelatorioTerceirizadosServico terceirizadosServico;
    private final ArmazenamentoTemporarioRelatorioServico armazenamentoTemporario;
    private final LotacaoRepositorio lotacaoRepositorio;
    private final JornadaRepositorio jornadaRepositorio;
    private final PendenciaAdministrativaServico pendencias;

    public RelatorioExportacaoServico(
            RelatorioFrequenciaConsolidadoServico frequenciaServico,
            RelatorioTerceirizadosServico terceirizadosServico,
            ArmazenamentoTemporarioRelatorioServico armazenamentoTemporario,
            LotacaoRepositorio lotacaoRepositorio,
            JornadaRepositorio jornadaRepositorio,
            PendenciaAdministrativaServico pendencias
    ) {
        this.frequenciaServico = frequenciaServico;
        this.terceirizadosServico = terceirizadosServico;
        this.armazenamentoTemporario = armazenamentoTemporario;
        this.lotacaoRepositorio = lotacaoRepositorio;
        this.jornadaRepositorio = jornadaRepositorio;
        this.pendencias = pendencias;
    }

    public void exportarTerceirizadosXlsx(
            OutputStream output, String busca, String status, Long contratoId, Boolean semContrato, Long lotacaoId
    ) throws IOException {
        armazenamentoTemporario.transferirEExcluir(
                prepararTerceirizadosXlsx(busca, status, contratoId, semContrato, lotacaoId, SEM_PROGRESSO), output);
    }

    public void exportarTerceirizadosPdf(
            OutputStream output, String busca, String status, Long contratoId, Boolean semContrato, Long lotacaoId
    ) throws IOException {
        armazenamentoTemporario.transferirEExcluir(
                prepararTerceirizadosPdf(busca, status, contratoId, semContrato, lotacaoId, SEM_PROGRESSO), output);
    }

    public void exportarFrequenciaXlsx(OutputStream output, LocalDate inicio, LocalDate fim,
                                       Collection<Long> contratoIds, Long lotacaoId, Long jornadaId) throws IOException {
        ContextoFrequencia contexto = criarContextoFrequencia(contratoIds, lotacaoId, jornadaId);
        armazenamentoTemporario.transferirEExcluir(
                prepararFrequenciaXlsx(inicio, fim, contratoIds, lotacaoId, jornadaId, contexto, SEM_PROGRESSO), output);
    }

    public void exportarFrequenciaPdf(OutputStream output, LocalDate inicio, LocalDate fim,
                                      Collection<Long> contratoIds, Long lotacaoId, Long jornadaId) throws IOException {
        ContextoFrequencia contexto = criarContextoFrequencia(contratoIds, lotacaoId, jornadaId);
        armazenamentoTemporario.transferirEExcluir(
                prepararFrequenciaPdf(inicio, fim, contratoIds, lotacaoId, jornadaId, contexto, SEM_PROGRESSO), output);
    }

    public ContextoFrequencia criarContextoFrequencia(
            Collection<Long> contratoIds, Long lotacaoId, Long jornadaId
    ) {
        String lotacao = lotacaoId == null
                ? "Todas as lotações"
                : lotacaoRepositorio.findById(lotacaoId)
                        .map(item -> texto(item.getNome()))
                        .orElse("Lotação " + lotacaoId);
        String turno = jornadaId == null
                ? "Todos os turnos"
                : jornadaRepositorio.findById(jornadaId)
                        .map(item -> texto(item.getDescricao()))
                        .orElse("Turno " + jornadaId);
        long quantidadeContratos = contratoIds == null ? 0L : contratoIds.stream().filter(java.util.Objects::nonNull).distinct().count();
        String contratos = quantidadeContratos == 0
                ? "Todos os contratos"
                : quantidadeContratos == 1 ? "1 contrato selecionado" : quantidadeContratos + " contratos selecionados";
        return new ContextoFrequencia(lotacao, turno, contratos, LocalDateTime.now(ZONA_BR));
    }

    public String nomeArquivoFrequencia(ContextoFrequencia contexto, String formato) {
        ContextoFrequencia seguro = contexto == null
                ? new ContextoFrequencia("Todas as lotações", "Todos os turnos", "Todos os contratos", LocalDateTime.now(ZONA_BR))
                : contexto;
        String extensao = "PDF".equalsIgnoreCase(formato) ? "pdf" : "xlsx";
        return "Relatorio_Frequencia_"
                + segmentoNomeArquivo(seguro.lotacao()) + "_"
                + segmentoNomeArquivo(seguro.turno()) + "_"
                + DATA_HORA_ARQUIVO.format(seguro.geradoEm()) + "." + extensao;
    }

    public ContextoAbsenteismo criarContextoAbsenteismo(
            Collection<Long> contratoIds, Long lotacaoId, Long jornadaId
    ) {
        String lotacao = lotacaoId == null
                ? "Todas as lotações"
                : lotacaoRepositorio.findById(lotacaoId)
                        .map(item -> texto(item.getNome()))
                        .orElse("Lotação " + lotacaoId);
        String turno = jornadaId == null
                ? "Todos os turnos"
                : jornadaRepositorio.findById(jornadaId)
                        .map(item -> texto(item.getDescricao()))
                        .orElse("Turno " + jornadaId);
        long quantidadeContratos = contratoIds == null ? 0L
                : contratoIds.stream().filter(java.util.Objects::nonNull).distinct().count();
        String contratos = quantidadeContratos == 0
                ? "Todos os contratos"
                : quantidadeContratos == 1 ? "1 contrato selecionado" : quantidadeContratos + " contratos selecionados";
        return new ContextoAbsenteismo(lotacao, turno, contratos, LocalDateTime.now(ZONA_BR));
    }

    public String nomeArquivoAbsenteismo(ContextoAbsenteismo contexto, String formato) {
        ContextoAbsenteismo seguro = contexto == null
                ? new ContextoAbsenteismo("Todas as lotações", "Todos os turnos", "Todos os contratos", LocalDateTime.now(ZONA_BR))
                : contexto;
        String extensao = "PDF".equalsIgnoreCase(formato) ? "pdf" : "xlsx";
        return "Relatorio_Absenteismo_"
                + segmentoNomeArquivo(seguro.lotacao()) + "_"
                + segmentoNomeArquivo(seguro.turno()) + "_"
                + DATA_HORA_ARQUIVO.format(seguro.geradoEm()) + "." + extensao;
    }

    public void exportarAbsenteismoXlsx(OutputStream output, LocalDate inicio, LocalDate fim,
                                        Collection<Long> contratoIds, Long lotacaoId, Long jornadaId,
                                        double meta, double limiteDisrupcao) throws IOException {
        exportarAbsenteismoXlsx(output, inicio, fim, contratoIds, lotacaoId, jornadaId, meta, limiteDisrupcao,
                criarContextoAbsenteismo(contratoIds, lotacaoId, jornadaId));
    }

    public void exportarAbsenteismoXlsx(OutputStream output, LocalDate inicio, LocalDate fim,
                                        Collection<Long> contratoIds, Long lotacaoId, Long jornadaId,
                                        double meta, double limiteDisrupcao, ContextoAbsenteismo contexto) throws IOException {
        armazenamentoTemporario.transferirEExcluir(
                prepararAbsenteismoXlsx(inicio, fim, contratoIds, lotacaoId, jornadaId,
                        meta, limiteDisrupcao, contexto, SEM_PROGRESSO), output);
    }

    public void exportarAbsenteismoPdf(OutputStream output, LocalDate inicio, LocalDate fim,
                                       Collection<Long> contratoIds, Long lotacaoId, Long jornadaId,
                                       double meta, double limiteDisrupcao) throws IOException {
        exportarAbsenteismoPdf(output, inicio, fim, contratoIds, lotacaoId, jornadaId, meta, limiteDisrupcao,
                criarContextoAbsenteismo(contratoIds, lotacaoId, jornadaId));
    }

    public void exportarAbsenteismoPdf(OutputStream output, LocalDate inicio, LocalDate fim,
                                       Collection<Long> contratoIds, Long lotacaoId, Long jornadaId,
                                       double meta, double limiteDisrupcao, ContextoAbsenteismo contexto) throws IOException {
        armazenamentoTemporario.transferirEExcluir(
                prepararAbsenteismoPdf(inicio, fim, contratoIds, lotacaoId, jornadaId,
                        meta, limiteDisrupcao, contexto, SEM_PROGRESSO), output);
    }

    public Path prepararTerceirizadosXlsx(
            String busca, String status, Long contratoId, Boolean semContrato, Long lotacaoId, LongConsumer progresso
    ) throws IOException {
        return armazenamentoTemporario.gerarArquivoConcluido(
                "visao-consolidada", ".xlsx",
                arquivo -> gerarTerceirizadosXlsxNoArquivo(
                        arquivo, busca, status, contratoId, semContrato, lotacaoId, progressoSeguro(progresso)));
    }

    public Path prepararTerceirizadosPdf(
            String busca, String status, Long contratoId, Boolean semContrato, Long lotacaoId, LongConsumer progresso
    ) throws IOException {
        return armazenamentoTemporario.gerarArquivoConcluido(
                "visao-consolidada", ".pdf",
                arquivo -> gerarTerceirizadosPdfNoArquivo(
                        arquivo, busca, status, contratoId, semContrato, lotacaoId, progressoSeguro(progresso)));
    }

    public Path prepararFrequenciaXlsx(
            LocalDate inicio, LocalDate fim, Collection<Long> contratoIds, Long lotacaoId, Long jornadaId,
            ContextoFrequencia contexto, LongConsumer progresso
    ) throws IOException {
        ContextoFrequencia seguro = contexto == null ? criarContextoFrequencia(contratoIds, lotacaoId, jornadaId) : contexto;
        seguro = seguro.comAviso(pendencias.avisoRelatorio(inicio, fim, contratoIds, lotacaoId, jornadaId));
        ContextoFrequencia contextoFinal = seguro;
        return armazenamentoTemporario.gerarArquivoConcluido(
                "frequencia-" + inicio + "-" + fim, ".xlsx",
                arquivo -> gerarFrequenciaXlsxNoArquivo(
                        arquivo, inicio, fim, contratoIds, lotacaoId, jornadaId, contextoFinal, progressoSeguro(progresso)));
    }

    public Path prepararFrequenciaPdf(
            LocalDate inicio, LocalDate fim, Collection<Long> contratoIds, Long lotacaoId, Long jornadaId,
            ContextoFrequencia contexto, LongConsumer progresso
    ) throws IOException {
        ContextoFrequencia seguro = contexto == null ? criarContextoFrequencia(contratoIds, lotacaoId, jornadaId) : contexto;
        seguro = seguro.comAviso(pendencias.avisoRelatorio(inicio, fim, contratoIds, lotacaoId, jornadaId));
        ContextoFrequencia contextoFinal = seguro;
        return armazenamentoTemporario.gerarArquivoConcluido(
                "frequencia-" + inicio + "-" + fim, ".pdf",
                arquivo -> gerarFrequenciaPdfNoArquivo(
                        arquivo, inicio, fim, contratoIds, lotacaoId, jornadaId, contextoFinal, progressoSeguro(progresso)));
    }

    public Path prepararAbsenteismoXlsx(
            LocalDate inicio, LocalDate fim, Collection<Long> contratoIds, Long lotacaoId, Long jornadaId,
            double meta, double limiteDisrupcao, ContextoAbsenteismo contexto, LongConsumer progresso
    ) throws IOException {
        ContextoAbsenteismo seguro = contexto == null
                ? criarContextoAbsenteismo(contratoIds, lotacaoId, jornadaId) : contexto;
        seguro = seguro.comAviso(pendencias.avisoRelatorio(inicio, fim, contratoIds, lotacaoId, jornadaId));
        ContextoAbsenteismo contextoFinal = seguro;
        return armazenamentoTemporario.gerarArquivoConcluido(
                "absenteismo-" + inicio + "-" + fim, ".xlsx",
                arquivo -> gerarAbsenteismoXlsxNoArquivo(
                        arquivo, inicio, fim, contratoIds, lotacaoId, jornadaId,
                        meta, limiteDisrupcao, contextoFinal, progressoSeguro(progresso)));
    }

    public Path prepararAbsenteismoPdf(
            LocalDate inicio, LocalDate fim, Collection<Long> contratoIds, Long lotacaoId, Long jornadaId,
            double meta, double limiteDisrupcao, ContextoAbsenteismo contexto, LongConsumer progresso
    ) throws IOException {
        ContextoAbsenteismo seguro = contexto == null
                ? criarContextoAbsenteismo(contratoIds, lotacaoId, jornadaId) : contexto;
        seguro = seguro.comAviso(pendencias.avisoRelatorio(inicio, fim, contratoIds, lotacaoId, jornadaId));
        ContextoAbsenteismo contextoFinal = seguro;
        return armazenamentoTemporario.gerarArquivoConcluido(
                "absenteismo-" + inicio + "-" + fim, ".pdf",
                arquivo -> gerarAbsenteismoPdfNoArquivo(
                        arquivo, inicio, fim, contratoIds, lotacaoId, jornadaId,
                        meta, limiteDisrupcao, contextoFinal, progressoSeguro(progresso)));
    }

    private void gerarTerceirizadosXlsxNoArquivo(
            OutputStream output, String busca, String status, Long contratoId, Boolean semContrato, Long lotacaoId,
            LongConsumer progresso
    ) throws IOException {
        List<Double> larguras = List.of(34d, 18d, 20d, 16d, 24d, 28d, 22d, 16d, 34d);
        AtomicLong linhas = new AtomicLong();
        try (XlsxStreamingWriter xlsx = new XlsxStreamingWriter(
                output, "Visao consolidada", 3, 1, larguras)) {
            xlsx.writeRow(List.of(Cell.text("Visão Consolidada de Terceirizados — base completa",
                    XlsxStreamingWriter.STYLE_TITLE)));
            xlsx.writeRow(List.of(Cell.text(
                    "Arquivo montado no disco temporário do servidor em lotes de 5.000. Todos os colaboradores que atendem aos filtros são exportados.",
                    XlsxStreamingWriter.STYLE_SUBTITLE)));
            xlsx.writeRow(List.of(
                    Cell.text("COLABORADOR", XlsxStreamingWriter.STYLE_HEADER),
                    Cell.text("MATRÍCULA", XlsxStreamingWriter.STYLE_HEADER),
                    Cell.text("CPF", XlsxStreamingWriter.STYLE_HEADER),
                    Cell.text("SITUAÇÃO", XlsxStreamingWriter.STYLE_HEADER),
                    Cell.text("CARGO", XlsxStreamingWriter.STYLE_HEADER),
                    Cell.text("LOTAÇÃO", XlsxStreamingWriter.STYLE_HEADER),
                    Cell.text("TURNO", XlsxStreamingWriter.STYLE_HEADER),
                    Cell.text("ADMISSÃO", XlsxStreamingWriter.STYLE_HEADER),
                    Cell.text("CONTRATO(S)", XlsxStreamingWriter.STYLE_HEADER)
            ));

            try {
                terceirizadosServico.processarEmpregadosEmLotes(
                        busca, status, contratoId, semContrato, lotacaoId,
                        RelatorioTerceirizadosServico.TAMANHO_LOTE_EXPORTACAO, lote -> {
                            for (RelatorioTerceirizadosDTO.EmpregadoResumo item : lote) {
                                try {
                                    xlsx.writeRow(List.of(
                                            Cell.text(texto(item.nome())),
                                            Cell.text(texto(item.matricula())),
                                            Cell.text(texto(item.cpf())),
                                            Cell.text(texto(item.statusDescricao())),
                                            Cell.text(texto(item.cargoNome())),
                                            Cell.text(texto(item.lotacaoNome())),
                                            Cell.text(texto(item.jornadaDescricao())),
                                            Cell.text(item.dataAdmissao() == null ? "-" : DATA_BR.format(item.dataAdmissao())),
                                            Cell.text(contratos(item.contratos()))
                                    ));
                                    linhas.incrementAndGet();
                                } catch (IOException e) {
                                    throw new UncheckedIOException(e);
                                }
                            }
                            persistirLoteXlsx(xlsx);
                            progresso.accept(lote.size());
                        });
            } catch (UncheckedIOException e) {
                throw e.getCause();
            }
            xlsx.writeRow(List.of(Cell.text("TOTAL EXPORTADO: " + linhas.get() + " colaborador(es)",
                    XlsxStreamingWriter.STYLE_TOTAL)));
            xlsx.finish("A3:I" + xlsx.currentRow());
        }
    }

    private void gerarTerceirizadosPdfNoArquivo(
            OutputStream output, String busca, String status, Long contratoId, Boolean semContrato, Long lotacaoId,
            LongConsumer progresso
    ) throws IOException {
        Document documento = new Document(PageSize.A4.rotate(), 18, 18, 22, 22);
        try {
            PdfWriter writer = PdfWriter.getInstance(documento, output);
            writer.setCloseStream(false);
            documento.open();
            adicionarTitulo(documento, "Visão Consolidada de Terceirizados",
                    "Base completa conforme os filtros selecionados. Geração temporária em disco, em lotes de 5.000.");
            AtomicLong sequencia = new AtomicLong();
            try {
                terceirizadosServico.processarEmpregadosEmLotes(
                        busca, status, contratoId, semContrato, lotacaoId,
                        RelatorioTerceirizadosServico.TAMANHO_LOTE_EXPORTACAO, lote -> {
                            escreverTerceirizadosPdf(documento, lote, sequencia);
                            persistirLotePdf(output);
                            progresso.accept(lote.size());
                        });
            } catch (UncheckedIOException e) {
                throw e.getCause();
            }
            documento.add(new Paragraph("Total exportado: " + sequencia.get() + " colaborador(es).",
                    fonte(8, Font.BOLD, AZUL)));
        } catch (DocumentException e) {
            throw new IllegalStateException("Falha ao gerar PDF da visão consolidada.", e);
        } finally {
            if (documento.isOpen()) documento.close();
        }
    }

    private void gerarFrequenciaXlsxNoArquivo(OutputStream output, LocalDate inicio, LocalDate fim,
                                       Collection<Long> contratoIds, Long lotacaoId, Long jornadaId,
                                       ContextoFrequencia contexto, LongConsumer progresso) throws IOException {
        List<LocalDate> dias = inicio.datesUntil(fim.plusDays(1)).toList();
        List<Double> larguras = new ArrayList<>();
        larguras.add(34d); larguras.add(24d); larguras.add(24d); larguras.add(20d);
        dias.forEach(ignorado -> larguras.add(6d));
        for (int i = 0; i < 10; i++) larguras.add(i >= 6 ? 13d : 8d);

        int totalColunas = 4 + dias.size() + 10;
        String ultimaColuna = XlsxStreamingWriter.columnName(totalColunas);
        AtomicLong linhas = new AtomicLong();
        TotaisExportacao totais = new TotaisExportacao();
        try (XlsxStreamingWriter xlsx = new XlsxStreamingWriter(
                output, "Frequencia " + inicio + " a " + fim, 5, 5, larguras)) {
            // Não mesclamos o cabeçalho: cada célula recebe o estilo da faixa. Isso
            // preserva a aparência em Excel e visualizadores compatíveis e evita
            // diferenças de renderização de células mescladas em arquivos enormes.
            xlsx.writeRow(linhaFaixaXlsx(totalColunas,
                    "Relatório Consolidado de Frequência - " + DATA_BR.format(inicio) + " a " + DATA_BR.format(fim),
                    XlsxStreamingWriter.STYLE_FREQ_TITLE), 28d);
            xlsx.writeRow(linhaFaixaXlsx(totalColunas,
                    "Lotação: " + contexto.lotacao() + " | Turno: " + contexto.turno()
                            + " | Contratos: " + contexto.contratos()
                            + " | Gerado em " + DATA_HORA_BR.format(contexto.geradoEm()),
                    XlsxStreamingWriter.STYLE_FREQ_FILTER), 22d);
            xlsx.writeRow(linhaFaixaXlsx(totalColunas,
                    "Regra noturna urbana: 22h-05h; 52min30s = 1 hora noturna. H.REAL representa tempo físico; H.NOT e H.PAG consideram a redução noturna.",
                    XlsxStreamingWriter.STYLE_FREQ_NOTE), 23d);
            xlsx.writeRow(linhaFaixaXlsx(totalColunas, contexto.aviso().mensagem(),
                    XlsxStreamingWriter.STYLE_FREQ_NOTE), 23d);

            List<Cell> cabecalho = new ArrayList<>();
            cabecalho.add(Cell.text("COLABORADOR", XlsxStreamingWriter.STYLE_FREQ_HEADER_LEFT));
            cabecalho.add(Cell.text("CARGO", XlsxStreamingWriter.STYLE_FREQ_HEADER_LEFT));
            cabecalho.add(Cell.text("LOTAÇÃO", XlsxStreamingWriter.STYLE_FREQ_HEADER_LEFT));
            cabecalho.add(Cell.text("TURNO", XlsxStreamingWriter.STYLE_FREQ_HEADER_LEFT));
            dias.forEach(data -> cabecalho.add(Cell.text(
                    data.format(DateTimeFormatter.ofPattern("dd/MM")),
                    ehFimDeSemana(data) ? XlsxStreamingWriter.STYLE_FREQ_HEADER_WEEKEND : XlsxStreamingWriter.STYLE_HEADER)));
            for (String titulo : List.of("P", "PP", "F", "AT", "D", "RT", "H.REAL", "H.NOT", "H.PAG", "H.PAG DEC")) {
                cabecalho.add(Cell.text(titulo, XlsxStreamingWriter.STYLE_HEADER));
            }
            xlsx.writeRow(cabecalho, 26d);

            try {
                frequenciaServico.processarFrequenciaEmLotes(
                        inicio, fim, contratoIds, lotacaoId, jornadaId, LOTE, lote -> {
                            for (RelatorioFrequenciaConsolidadoDTO.Linha linha : lote) {
                                try {
                                    boolean zebra = (linhas.get() & 1L) == 1L;
                                    xlsx.writeRow(linhaFrequenciaXlsx(linha, dias, zebra), 20d);
                                    linhas.incrementAndGet();
                                    totais.somar(linha);
                                } catch (IOException e) {
                                    throw new UncheckedIOException(e);
                                }
                            }
                            persistirLoteXlsx(xlsx);
                            progresso.accept(lote.size());
                        });
            } catch (UncheckedIOException e) {
                throw e.getCause();
            }

            int ultimaLinhaDados = xlsx.currentRow();
            List<Cell> total = new ArrayList<>();
            total.add(Cell.text("TOTAL GERAL - " + linhas.get() + " alocação(ões)", XlsxStreamingWriter.STYLE_TOTAL));
            total.add(Cell.text("", XlsxStreamingWriter.STYLE_TOTAL));
            total.add(Cell.text("", XlsxStreamingWriter.STYLE_TOTAL));
            total.add(Cell.text("", XlsxStreamingWriter.STYLE_TOTAL));
            dias.forEach(ignorado -> total.add(Cell.text("", XlsxStreamingWriter.STYLE_TOTAL)));
            total.add(Cell.number(totais.presencas, XlsxStreamingWriter.STYLE_STATUS_P));
            total.add(Cell.number(totais.parciais, XlsxStreamingWriter.STYLE_STATUS_PP));
            total.add(Cell.number(totais.faltas, XlsxStreamingWriter.STYLE_STATUS_F));
            total.add(Cell.number(totais.atestados, XlsxStreamingWriter.STYLE_STATUS_AT));
            total.add(Cell.number(totais.descansos, XlsxStreamingWriter.STYLE_STATUS_D));
            total.add(Cell.number(totais.rt, XlsxStreamingWriter.STYLE_STATUS_RT));
            total.add(Cell.text(minutosParaDuracao(totais.minutosFisicos), XlsxStreamingWriter.STYLE_RESUMO_HREAL));
            total.add(Cell.text(minutosParaDuracao(totais.minutosNoturnos), XlsxStreamingWriter.STYLE_RESUMO_HNOT));
            total.add(Cell.text(minutosParaDuracao(totais.minutosPagaveis), XlsxStreamingWriter.STYLE_RESUMO_HPAG));
            total.add(Cell.number(totais.minutosPagaveis / 60.0, XlsxStreamingWriter.STYLE_RESUMO_HDEC));
            xlsx.writeRow(total, 22d);

            adicionarLegendaFrequenciaXlsx(xlsx, totalColunas);
            xlsx.finish("A5:" + ultimaColuna + Math.max(5, ultimaLinhaDados));
        }
    }

    private void gerarAbsenteismoXlsxNoArquivo(
            OutputStream output, LocalDate inicio, LocalDate fim,
            Collection<Long> contratoIds, Long lotacaoId, Long jornadaId,
            double meta, double limiteDisrupcao, ContextoAbsenteismo contexto,
            LongConsumer progresso
    ) throws IOException {
        final int totalColunas = 12;
        List<Double> larguras = List.of(34d, 24d, 26d, 22d, 14d, 11d, 12d, 13d, 12d, 13d, 16d, 24d);
        AtomicLong linhas = new AtomicLong();
        TotaisAbsenteismo totais = new TotaisAbsenteismo();

        try (XlsxStreamingWriter xlsx = new XlsxStreamingWriter(
                output, "Absenteismo " + inicio.getYear() + "-" + String.format(Locale.ROOT, "%02d", inicio.getMonthValue()),
                5, 5, larguras)) {
            xlsx.writeRow(linhaFaixaXlsx(totalColunas,
                    "Relatório de Absenteísmo - " + DATA_BR.format(inicio) + " a " + DATA_BR.format(fim),
                    XlsxStreamingWriter.STYLE_FREQ_TITLE), 28d);
            xlsx.writeRow(linhaFaixaXlsx(totalColunas,
                    "Lotação: " + contexto.lotacao() + " | Turno: " + contexto.turno()
                            + " | Contratos: " + contexto.contratos()
                            + " | Gerado em " + DATA_HORA_BR.format(contexto.geradoEm()),
                    XlsxStreamingWriter.STYLE_FREQ_FILTER), 22d);
            xlsx.writeRow(linhaFaixaXlsx(totalColunas,
                    String.format(Locale.forLanguageTag("pt-BR"),
                            "Meta de absenteísmo: %.2f%% | Limite do índice de disrupção: %.0f | Taxa = (faltas + atestados) / dias previstos de trabalho.",
                            meta, limiteDisrupcao),
                    XlsxStreamingWriter.STYLE_FREQ_NOTE), 23d);
            xlsx.writeRow(linhaFaixaXlsx(totalColunas, contexto.aviso().mensagem(),
                    XlsxStreamingWriter.STYLE_FREQ_NOTE), 23d);

            xlsx.writeRow(List.of(
                    Cell.text("COLABORADOR", XlsxStreamingWriter.STYLE_FREQ_HEADER_LEFT),
                    Cell.text("CARGO", XlsxStreamingWriter.STYLE_FREQ_HEADER_LEFT),
                    Cell.text("LOTAÇÃO", XlsxStreamingWriter.STYLE_FREQ_HEADER_LEFT),
                    Cell.text("TURNO", XlsxStreamingWriter.STYLE_FREQ_HEADER_LEFT),
                    Cell.text("DIAS PREVISTOS", XlsxStreamingWriter.STYLE_HEADER),
                    Cell.text("FALTAS", XlsxStreamingWriter.STYLE_HEADER),
                    Cell.text("ATESTADOS", XlsxStreamingWriter.STYLE_HEADER),
                    Cell.text("DSR PERDIDO", XlsxStreamingWriter.STYLE_HEADER),
                    Cell.text("EPISÓDIOS", XlsxStreamingWriter.STYLE_HEADER),
                    Cell.text("TAXA (%)", XlsxStreamingWriter.STYLE_HEADER),
                    Cell.text("ÍNDICE DISRUPÇÃO", XlsxStreamingWriter.STYLE_HEADER),
                    Cell.text("CLASSIFICAÇÃO", XlsxStreamingWriter.STYLE_HEADER)
            ), 27d);

            try {
                frequenciaServico.processarAbsenteismoEmLotes(
                        inicio, fim, contratoIds, lotacaoId, jornadaId, LOTE, lote -> {
                            for (RelatorioAbsenteismoDTO item : lote) {
                                try {
                                    boolean zebra = (linhas.get() & 1L) == 1L;
                                    xlsx.writeRow(linhaAbsenteismoXlsx(item, meta, limiteDisrupcao, zebra), 21d);
                                    linhas.incrementAndGet();
                                    totais.somar(item);
                                } catch (IOException e) {
                                    throw new UncheckedIOException(e);
                                }
                            }
                            persistirLoteXlsx(xlsx);
                            progresso.accept(lote.size());
                        });
            } catch (UncheckedIOException e) {
                throw e.getCause();
            }

            double taxaGeral = totais.taxaGeral();
            xlsx.writeRow(List.of(
                    Cell.text("TOTAL GERAL - " + linhas.get() + " alocação(ões)", XlsxStreamingWriter.STYLE_TOTAL),
                    Cell.text("", XlsxStreamingWriter.STYLE_TOTAL),
                    Cell.text("", XlsxStreamingWriter.STYLE_TOTAL),
                    Cell.text("", XlsxStreamingWriter.STYLE_TOTAL),
                    Cell.number(totais.diasPrevistos, XlsxStreamingWriter.STYLE_TOTAL),
                    Cell.number(totais.faltas, XlsxStreamingWriter.STYLE_STATUS_F),
                    Cell.number(totais.atestados, XlsxStreamingWriter.STYLE_STATUS_AT),
                    Cell.number(totais.dsrPerdido, XlsxStreamingWriter.STYLE_STATUS_DP),
                    Cell.number(totais.episodios, XlsxStreamingWriter.STYLE_TOTAL),
                    Cell.number(taxaGeral, estiloTaxaAbsenteismoXlsx(taxaGeral, meta)),
                    Cell.text("-", XlsxStreamingWriter.STYLE_TOTAL),
                    Cell.text(classificar(taxaGeral, meta, 0, limiteDisrupcao),
                            estiloClassificacaoAbsenteismoXlsx(taxaGeral, meta, 0, limiteDisrupcao))
            ), 23d);

            adicionarLegendaAbsenteismoXlsx(xlsx, totalColunas, meta, limiteDisrupcao);
            xlsx.finish("A5:L" + Math.max(5, xlsx.currentRow()));
        }
    }

    private void gerarFrequenciaPdfNoArquivo(OutputStream output, LocalDate inicio, LocalDate fim,
                                      Collection<Long> contratoIds, Long lotacaoId, Long jornadaId,
                                      ContextoFrequencia contexto, LongConsumer progresso) throws IOException {
        List<LocalDate> dias = inicio.datesUntil(fim.plusDays(1)).toList();
        Rectangle pagina = dias.size() > 18 ? PageSize.A3.rotate() : PageSize.A4.rotate();
        Document documento = new Document(pagina, 18, 18, 86, 40);
        try {
            PdfWriter writer = PdfWriter.getInstance(documento, output);
            writer.setCloseStream(false);
            writer.setPageEvent(new EventoPaginaFrequenciaPdf(inicio, fim, contexto));
            documento.addTitle("Relatório Consolidado de Frequência");
            documento.addSubject("Frequência consolidada por colaborador, lotação e turno");
            documento.addAuthor("SGT-GTRAT");
            documento.addCreator("SGT-GTRAT");
            documento.open();
            adicionarAvisoRevisaoPdf(documento, contexto.aviso());

            AtomicLong sequencia = new AtomicLong();
            try {
                frequenciaServico.processarFrequenciaEmLotes(
                        inicio, fim, contratoIds, lotacaoId, jornadaId, LOTE, lote -> {
                            escreverFrequenciaPdf(documento, lote, dias, sequencia);
                            persistirLotePdf(output);
                            progresso.accept(lote.size());
                        });
            } catch (UncheckedIOException e) {
                throw e.getCause();
            }

            Paragraph total = new Paragraph("Total exportado: " + sequencia.get() + " alocação(ões).",
                    fonte(8.5f, Font.BOLD, AZUL));
            total.setSpacingBefore(7f);
            total.setSpacingAfter(8f);
            documento.add(total);
            adicionarLegendaFrequenciaPdf(documento);
        } catch (DocumentException e) {
            throw new IllegalStateException("Falha ao gerar PDF de frequência.", e);
        } finally {
            if (documento.isOpen()) documento.close();
        }
    }

    private void gerarAbsenteismoPdfNoArquivo(
            OutputStream output, LocalDate inicio, LocalDate fim,
            Collection<Long> contratoIds, Long lotacaoId, Long jornadaId,
            double meta, double limiteDisrupcao, ContextoAbsenteismo contexto,
            LongConsumer progresso
    ) throws IOException {
        Document documento = new Document(PageSize.A4.rotate(), 18, 18, 86, 40);
        try {
            PdfWriter writer = PdfWriter.getInstance(documento, output);
            writer.setCloseStream(false);
            writer.setPageEvent(new EventoPaginaAbsenteismoPdf(inicio, fim, contexto, meta, limiteDisrupcao));
            documento.addTitle("Relatório de Absenteísmo");
            documento.addSubject("Absenteísmo por colaborador, lotação e turno");
            documento.addAuthor("SGT-GTRAT");
            documento.addCreator("SGT-GTRAT");
            documento.open();
            adicionarAvisoRevisaoPdf(documento, contexto.aviso());

            AtomicLong sequencia = new AtomicLong();
            TotaisAbsenteismo totais = new TotaisAbsenteismo();
            try {
                frequenciaServico.processarAbsenteismoEmLotes(
                        inicio, fim, contratoIds, lotacaoId, jornadaId, LOTE, lote -> {
                            escreverAbsenteismoPdf(documento, lote, meta, limiteDisrupcao, sequencia, totais);
                            persistirLotePdf(output);
                            progresso.accept(lote.size());
                        });
            } catch (UncheckedIOException e) {
                throw e.getCause();
            }

            adicionarResumoAbsenteismoPdf(documento, sequencia.get(), totais, meta);
            adicionarLegendaAbsenteismoPdf(documento, meta, limiteDisrupcao);
        } catch (DocumentException e) {
            throw new IllegalStateException("Falha ao gerar PDF de absenteísmo.", e);
        } finally {
            if (documento.isOpen()) documento.close();
        }
    }

    private static LongConsumer progressoSeguro(LongConsumer progresso) {
        return progresso == null ? SEM_PROGRESSO : progresso;
    }

    private void persistirLoteXlsx(XlsxStreamingWriter xlsx) {
        try {
            xlsx.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void persistirLotePdf(OutputStream output) {
        try {
            output.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private List<Cell> linhaFrequenciaXlsx(
            RelatorioFrequenciaConsolidadoDTO.Linha linha, List<LocalDate> dias, boolean zebra
    ) {
        List<Cell> cells = new ArrayList<>();
        cells.add(Cell.text(texto(linha.nome()), zebra
                ? XlsxStreamingWriter.STYLE_FREQ_BODY_BOLD_ZEBRA : XlsxStreamingWriter.STYLE_FREQ_BODY_BOLD));
        cells.add(Cell.text(texto(linha.cargo()), zebra
                ? XlsxStreamingWriter.STYLE_FREQ_BODY_ZEBRA : XlsxStreamingWriter.STYLE_FREQ_BODY));
        cells.add(Cell.text(texto(linha.lotacao()), zebra
                ? XlsxStreamingWriter.STYLE_FREQ_BODY_ZEBRA : XlsxStreamingWriter.STYLE_FREQ_BODY));
        cells.add(Cell.text(texto(linha.turno()), zebra
                ? XlsxStreamingWriter.STYLE_FREQ_TURNO_ZEBRA : XlsxStreamingWriter.STYLE_FREQ_TURNO));
        for (LocalDate data : dias) {
            RelatorioFrequenciaConsolidadoDTO.Dia dia = linha.diario().get(data.toString());
            String codigo = sigla(dia);
            cells.add(Cell.text(codigo, estiloStatusXlsx(codigo, zebra)));
        }
        RelatorioFrequenciaConsolidadoDTO.Totais t = linha.totais();
        cells.add(Cell.number(t.presencas(), XlsxStreamingWriter.STYLE_STATUS_P));
        cells.add(Cell.number(t.presencasParciais(), XlsxStreamingWriter.STYLE_STATUS_PP));
        cells.add(Cell.number(t.faltas(), XlsxStreamingWriter.STYLE_STATUS_F));
        cells.add(Cell.number(t.atestados(), XlsxStreamingWriter.STYLE_STATUS_AT));
        cells.add(Cell.number(t.descansos(), XlsxStreamingWriter.STYLE_STATUS_D));
        cells.add(Cell.number(contarStatus(linha, "RT"), XlsxStreamingWriter.STYLE_STATUS_RT));
        cells.add(Cell.text(minutosParaDuracao(t.minutosFisicos()), XlsxStreamingWriter.STYLE_RESUMO_HREAL));
        cells.add(Cell.text(minutosParaDuracao(t.minutosNoturnosReduzidos()), XlsxStreamingWriter.STYLE_RESUMO_HNOT));
        cells.add(Cell.text(minutosParaDuracao(t.minutosPagaveis()), XlsxStreamingWriter.STYLE_RESUMO_HPAG));
        cells.add(Cell.number(t.minutosPagaveis() / 60.0, XlsxStreamingWriter.STYLE_RESUMO_HDEC));
        return cells;
    }

    private int estiloStatusXlsx(String codigo, boolean zebra) {
        StatusVisual status = statusVisual(codigo);
        if (status != null) return status.estiloXlsx();
        return zebra ? XlsxStreamingWriter.STYLE_FREQ_CENTER_ZEBRA : XlsxStreamingWriter.STYLE_FREQ_CENTER;
    }

    private List<Cell> linhaFaixaXlsx(int totalColunas, String texto, int estilo) {
        List<Cell> linha = new ArrayList<>(totalColunas);
        for (int coluna = 0; coluna < totalColunas; coluna++) {
            linha.add(Cell.text(coluna == 0 ? texto : "", estilo));
        }
        return linha;
    }

    private void adicionarAvisoRevisaoPdf(Document documento, AvisoRevisaoRelatorioDTO aviso)
            throws DocumentException {
        AvisoRevisaoRelatorioDTO seguro = aviso == null ? AvisoRevisaoRelatorioDTO.definitivo() : aviso;
        Color cor = seguro.sujeitoARevisao() ? VERMELHO_TEXTO : VERDE_TEXTO;
        Paragraph paragrafo = new Paragraph(seguro.mensagem(), fonte(9f, Font.BOLD, cor));
        paragrafo.setSpacingAfter(8f);
        documento.add(paragrafo);
    }

    private void adicionarLegendaFrequenciaXlsx(
            XlsxStreamingWriter xlsx, int totalColunas
    ) throws IOException {
        xlsx.writeRow(List.of());
        xlsx.writeRow(linhaFaixaXlsx(totalColunas, "Legenda dos lançamentos",
                XlsxStreamingWriter.STYLE_LEGEND_TITLE), 22d);

        int larguraBloco = Math.max(1, totalColunas / 3);
        for (int i = 0; i < LEGENDA_FREQUENCIA.size(); i += 3) {
            List<Cell> linha = new ArrayList<>(totalColunas);
            for (int c = 0; c < totalColunas; c++) linha.add(Cell.empty());
            for (int coluna = 0; coluna < 3; coluna++) {
                int indice = i + coluna;
                if (indice >= LEGENDA_FREQUENCIA.size()) break;
                StatusVisual item = LEGENDA_FREQUENCIA.get(indice);
                int inicio = coluna * larguraBloco;
                int fim = coluna == 2 ? totalColunas - 1 : Math.min(totalColunas - 1, inicio + larguraBloco - 1);
                for (int c = inicio; c <= fim; c++) {
                    linha.set(c, Cell.text(c == inicio ? item.sigla() + " - " + item.descricao() : "", item.estiloXlsx()));
                }
            }
            xlsx.writeRow(linha, 20d);
        }

        xlsx.writeRow(linhaFaixaXlsx(totalColunas,
                "H.REAL = tempo físico | H.NOT = horas noturnas reduzidas | H.PAG = total pagável | H.PAG DEC = total pagável em horas decimais",
                XlsxStreamingWriter.STYLE_LEGEND_NOTE), 21d);
    }

    private List<Cell> linhaAbsenteismoXlsx(
            RelatorioAbsenteismoDTO item, double meta, double limiteDisrupcao, boolean zebra
    ) {
        int corpo = zebra ? XlsxStreamingWriter.STYLE_FREQ_BODY_ZEBRA : XlsxStreamingWriter.STYLE_FREQ_BODY;
        int corpoNegrito = zebra ? XlsxStreamingWriter.STYLE_FREQ_BODY_BOLD_ZEBRA : XlsxStreamingWriter.STYLE_FREQ_BODY_BOLD;
        int centro = zebra ? XlsxStreamingWriter.STYLE_FREQ_CENTER_ZEBRA : XlsxStreamingWriter.STYLE_FREQ_CENTER;
        String classificacao = classificar(item.taxaIndividual(), meta, item.disrupcao(), limiteDisrupcao);

        return List.of(
                Cell.text(texto(item.nome()), corpoNegrito),
                Cell.text(texto(item.cargo()), corpo),
                Cell.text(texto(item.lotacao()), corpo),
                Cell.text(texto(item.turno()), zebra ? XlsxStreamingWriter.STYLE_FREQ_TURNO_ZEBRA : XlsxStreamingWriter.STYLE_FREQ_TURNO),
                Cell.number(item.diasTrabalhaveis(), centro),
                Cell.number(item.faltas(), item.faltas() > 0 ? XlsxStreamingWriter.STYLE_STATUS_F : centro),
                Cell.number(item.atestados(), item.atestados() > 0 ? XlsxStreamingWriter.STYLE_STATUS_AT : centro),
                Cell.number(item.dsrPerdido(), item.dsrPerdido() > 0 ? XlsxStreamingWriter.STYLE_STATUS_DP : centro),
                Cell.number(item.spells(), centro),
                Cell.number(item.taxaIndividual(), estiloTaxaAbsenteismoXlsx(item.taxaIndividual(), meta)),
                Cell.number(item.disrupcao(), estiloDisrupcaoAbsenteismoXlsx(item.disrupcao(), limiteDisrupcao)),
                Cell.text(classificacao,
                        estiloClassificacaoAbsenteismoXlsx(item.taxaIndividual(), meta, item.disrupcao(), limiteDisrupcao))
        );
    }

    private int estiloTaxaAbsenteismoXlsx(double taxa, double meta) {
        if (taxa <= meta) return XlsxStreamingWriter.STYLE_ABS_PERCENT_OK;
        if (taxa <= meta * 2.0) return XlsxStreamingWriter.STYLE_ABS_PERCENT_WARN;
        return XlsxStreamingWriter.STYLE_ABS_PERCENT_CRITICAL;
    }

    private int estiloDisrupcaoAbsenteismoXlsx(double indice, double limite) {
        if (indice < limite) return XlsxStreamingWriter.STYLE_ABS_DISRUPCAO_NORMAL;
        if (indice < limite * 2.0) return XlsxStreamingWriter.STYLE_ABS_DISRUPCAO_ALERTA;
        return XlsxStreamingWriter.STYLE_ABS_DISRUPCAO_CRITICA;
    }

    private int estiloClassificacaoAbsenteismoXlsx(
            double taxa, double meta, double disrupcao, double limiteDisrupcao
    ) {
        if (disrupcao >= limiteDisrupcao) return XlsxStreamingWriter.STYLE_STATUS_F;
        if (taxa > meta) return XlsxStreamingWriter.STYLE_STATUS_ADV;
        return XlsxStreamingWriter.STYLE_STATUS_P;
    }

    private void adicionarLegendaAbsenteismoXlsx(
            XlsxStreamingWriter xlsx, int totalColunas, double meta, double limiteDisrupcao
    ) throws IOException {
        xlsx.writeRow(List.of());
        xlsx.writeRow(linhaFaixaXlsx(totalColunas, "Legenda e critérios",
                XlsxStreamingWriter.STYLE_LEGEND_TITLE), 22d);

        xlsx.writeRow(linhaFaixaXlsx(totalColunas,
                "FALTAS - ausências registradas como falta | ATESTADOS - ausências por atestado | DSR PERDIDO - descanso semanal perdido | EPISÓDIOS - sequências independentes de ausência.",
                XlsxStreamingWriter.STYLE_LEGEND_NOTE), 22d);

        List<Cell> classificacoes = new ArrayList<>(totalColunas);
        for (int i = 0; i < totalColunas; i++) classificacoes.add(Cell.empty());
        int bloco = totalColunas / 3;
        preencherFaixaLegenda(classificacoes, 0, bloco - 1,
                "DENTRO DA META - taxa até " + String.format(Locale.forLanguageTag("pt-BR"), "%.2f%%", meta),
                XlsxStreamingWriter.STYLE_STATUS_P);
        preencherFaixaLegenda(classificacoes, bloco, bloco * 2 - 1,
                "ACIMA DA META - taxa superior à meta", XlsxStreamingWriter.STYLE_STATUS_ADV);
        preencherFaixaLegenda(classificacoes, bloco * 2, totalColunas - 1,
                "ALERTA DE DISRUPÇÃO - índice >= " + String.format(Locale.ROOT, "%.0f", limiteDisrupcao),
                XlsxStreamingWriter.STYLE_STATUS_F);
        xlsx.writeRow(classificacoes, 21d);

        xlsx.writeRow(linhaFaixaXlsx(totalColunas,
                "O índice de disrupção é uma pontuação operacional, não um percentual. Valores maiores indicam ausências mais fragmentadas e recorrentes.",
                XlsxStreamingWriter.STYLE_LEGEND_NOTE), 22d);
    }

    private void preencherFaixaLegenda(List<Cell> linha, int inicio, int fim, String texto, int estilo) {
        for (int coluna = Math.max(0, inicio); coluna <= Math.min(linha.size() - 1, fim); coluna++) {
            linha.set(coluna, Cell.text(coluna == inicio ? texto : "", estilo));
        }
    }

    private void escreverFrequenciaPdf(Document documento, List<RelatorioFrequenciaConsolidadoDTO.Linha> lote,
                                       List<LocalDate> dias, AtomicLong sequencia) {
        final int porTabela = 80;
        for (int inicio = 0; inicio < lote.size(); inicio += porTabela) {
            List<RelatorioFrequenciaConsolidadoDTO.Linha> bloco =
                    lote.subList(inicio, Math.min(lote.size(), inicio + porTabela));
            PdfPTable tabela = new PdfPTable(4 + dias.size() + 10);
            tabela.setWidthPercentage(100);
            tabela.setHeaderRows(1);
            tabela.setSpacingAfter(5f);
            try {
                tabela.setWidths(largurasFrequenciaPdf(dias.size()));
            } catch (DocumentException e) {
                throw new IllegalStateException("Não foi possível configurar as larguras do PDF de frequência.", e);
            }

            adicionarCabecalhoFrequencia(tabela, "Colaborador", false);
            adicionarCabecalhoFrequencia(tabela, "Cargo", false);
            adicionarCabecalhoFrequencia(tabela, "Lotação", false);
            adicionarCabecalhoFrequencia(tabela, "Turno", false);
            dias.forEach(data -> adicionarCabecalhoFrequencia(
                    tabela, data.format(DateTimeFormatter.ofPattern("dd/MM")), ehFimDeSemana(data)));
            List.of("P", "PP", "F", "AT", "D", "RT", "H.REAL", "H.NOT", "H.PAG", "H.PAG DEC")
                    .forEach(valor -> adicionarCabecalhoFrequencia(tabela, valor, false));

            float fonteTexto = dias.size() > 18 ? 6.0f : 5.5f;
            float fonteDia = dias.size() > 18 ? 5.5f : 5.0f;
            for (RelatorioFrequenciaConsolidadoDTO.Linha linha : bloco) {
                boolean zebra = (sequencia.getAndIncrement() & 1L) == 1L;
                Color fundoLinha = zebra ? CINZA_ZEBRA : BRANCO;
                adicionarCelulaPdf(tabela, texto(linha.nome()), fonteTexto, false, fundoLinha, new Color(15, 23, 42), true);
                adicionarCelulaPdf(tabela, texto(linha.cargo()), fonteTexto, false, fundoLinha, TEXTO, false);
                adicionarCelulaPdf(tabela, texto(linha.lotacao()), fonteTexto, false, fundoLinha, TEXTO, false);
                adicionarCelulaPdf(tabela, texto(linha.turno()), fonteTexto, true, fundoLinha, AZUL, true);
                dias.forEach(data -> adicionarStatusFrequenciaPdf(
                        tabela, sigla(linha.diario().get(data.toString())), fonteDia, zebra));

                RelatorioFrequenciaConsolidadoDTO.Totais t = linha.totais();
                adicionarResumoFrequenciaPdf(tabela, Integer.toString(t.presencas()), "P", fonteDia);
                adicionarResumoFrequenciaPdf(tabela, Integer.toString(t.presencasParciais()), "PP", fonteDia);
                adicionarResumoFrequenciaPdf(tabela, Integer.toString(t.faltas()), "F", fonteDia);
                adicionarResumoFrequenciaPdf(tabela, Integer.toString(t.atestados()), "AT", fonteDia);
                adicionarResumoFrequenciaPdf(tabela, Integer.toString(t.descansos()), "D", fonteDia);
                adicionarResumoFrequenciaPdf(tabela, Integer.toString(contarStatus(linha, "RT")), "RT", fonteDia);
                adicionarCelulaPdf(tabela, minutosParaDuracao(t.minutosFisicos()), fonteDia, true,
                        new Color(254, 252, 232), new Color(133, 77, 14), true);
                adicionarCelulaPdf(tabela, minutosParaDuracao(t.minutosNoturnosReduzidos()), fonteDia, true,
                        new Color(239, 246, 255), new Color(29, 78, 216), true);
                adicionarCelulaPdf(tabela, minutosParaDuracao(t.minutosPagaveis()), fonteDia, true,
                        new Color(245, 243, 255), new Color(109, 40, 217), true);
                adicionarCelulaPdf(tabela, String.format(Locale.ROOT, "%.2f", t.minutosPagaveis() / 60.0), fonteDia, true,
                        DESCANSO_CLARO, DESCANSO_TEXTO, true);
            }
            try {
                documento.add(tabela);
            } catch (DocumentException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private float[] largurasFrequenciaPdf(int quantidadeDias) {
        float[] larguras = new float[4 + quantidadeDias + 10];
        larguras[0] = 5.8f;
        larguras[1] = 4.4f;
        larguras[2] = 4.8f;
        larguras[3] = 3.5f;
        for (int i = 0; i < quantidadeDias; i++) larguras[4 + i] = 1.25f;
        int baseResumo = 4 + quantidadeDias;
        for (int i = 0; i < 6; i++) larguras[baseResumo + i] = 1.35f;
        for (int i = 6; i < 10; i++) larguras[baseResumo + i] = 1.85f;
        return larguras;
    }

    private void adicionarCabecalhoFrequencia(PdfPTable tabela, String valor, boolean fimDeSemana) {
        Color fundo = fimDeSemana ? AZUL_FIM_SEMANA : AZUL;
        Color texto = fimDeSemana ? new Color(255, 229, 107) : BRANCO;
        PdfPCell cell = new PdfPCell(new Paragraph(valor, fonte(5.4f, Font.BOLD, texto)));
        cell.setBackgroundColor(fundo);
        cell.setBorderColor(AZUL_ESCURO);
        cell.setPadding(2.4f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        tabela.addCell(cell);
    }

    private void adicionarStatusFrequenciaPdf(PdfPTable tabela, String codigo, float tamanho, boolean zebra) {
        StatusVisual status = statusVisual(codigo);
        if (status == null) {
            adicionarCelulaPdf(tabela, codigo, tamanho, true, zebra ? CINZA_ZEBRA : BRANCO, TEXTO_SUAVE, false);
            return;
        }
        adicionarCelulaPdf(tabela, codigo, tamanho, true, status.fundo(), status.texto(), true, status.borda());
    }

    private void adicionarResumoFrequenciaPdf(PdfPTable tabela, String valor, String codigo, float tamanho) {
        StatusVisual status = statusVisual(codigo);
        if (status == null) {
            adicionarCelulaPdf(tabela, valor, tamanho, true, BRANCO, TEXTO, true);
            return;
        }
        adicionarCelulaPdf(tabela, valor, tamanho, true, status.fundo(), status.texto(), true, status.borda());
    }

    private void adicionarLegendaFrequenciaPdf(Document documento) throws DocumentException {
        Paragraph titulo = new Paragraph("Legenda dos lançamentos", fonte(10.5f, Font.BOLD, AZUL));
        titulo.setSpacingBefore(2f);
        titulo.setSpacingAfter(5f);
        documento.add(titulo);

        Paragraph criterio = new Paragraph(
                "H.REAL: tempo físico | H.NOT: horas noturnas reduzidas | H.PAG: total pagável | H.PAG DEC: total em horas decimais",
                fonte(7.5f, Font.NORMAL, TEXTO_SUAVE));
        criterio.setSpacingAfter(7f);
        documento.add(criterio);

        PdfPTable legenda = new PdfPTable(3);
        legenda.setWidthPercentage(100);
        legenda.setSpacingAfter(4f);
        for (StatusVisual item : LEGENDA_FREQUENCIA) {
            PdfPCell celula = new PdfPCell(new Paragraph(
                    item.sigla() + "  " + item.descricao(), fonte(7.2f, Font.BOLD, item.texto())));
            celula.setBackgroundColor(item.fundo());
            celula.setBorderColor(item.borda());
            celula.setPadding(5f);
            celula.setVerticalAlignment(Element.ALIGN_MIDDLE);
            legenda.addCell(celula);
        }
        int restante = LEGENDA_FREQUENCIA.size() % 3;
        if (restante != 0) {
            for (int i = restante; i < 3; i++) {
                PdfPCell vazio = new PdfPCell(new Paragraph(""));
                vazio.setBorder(Rectangle.NO_BORDER);
                legenda.addCell(vazio);
            }
        }
        documento.add(legenda);
    }

    private void adicionarCelulaPdf(
            PdfPTable tabela, String valor, float tamanho, boolean centro,
            Color fundo, Color corTexto, boolean negrito
    ) {
        adicionarCelulaPdf(tabela, valor, tamanho, centro, fundo, corTexto, negrito, BORDA);
    }

    private void adicionarCelulaPdf(
            PdfPTable tabela, String valor, float tamanho, boolean centro,
            Color fundo, Color corTexto, boolean negrito, Color corBorda
    ) {
        PdfPCell cell = new PdfPCell(new Paragraph(
                valor, fonte(tamanho, negrito ? Font.BOLD : Font.NORMAL, corTexto)));
        cell.setBackgroundColor(fundo);
        cell.setBorderColor(corBorda);
        cell.setPadding(2.2f);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        if (centro) cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        tabela.addCell(cell);
    }

    private StatusVisual statusVisual(String codigo) {
        if (codigo == null || codigo.isBlank() || "-".equals(codigo)) return null;
        return LEGENDA_FREQUENCIA.stream()
                .filter(item -> item.sigla().equalsIgnoreCase(codigo))
                .findFirst()
                .orElse(null);
    }

    private boolean ehFimDeSemana(LocalDate data) {
        if (data == null) return false;
        DayOfWeek dia = data.getDayOfWeek();
        return dia == DayOfWeek.SATURDAY || dia == DayOfWeek.SUNDAY;
    }

    private void escreverAbsenteismoPdf(
            Document documento, List<RelatorioAbsenteismoDTO> lote,
            double meta, double limiteDisrupcao, AtomicLong sequencia, TotaisAbsenteismo totais
    ) {
        final int porTabela = 100;
        for (int inicio = 0; inicio < lote.size(); inicio += porTabela) {
            List<RelatorioAbsenteismoDTO> bloco = lote.subList(inicio, Math.min(lote.size(), inicio + porTabela));
            PdfPTable tabela = new PdfPTable(12);
            tabela.setWidthPercentage(100);
            tabela.setHeaderRows(1);
            tabela.setSpacingAfter(5f);
            try {
                tabela.setWidths(new float[] {5.8f, 4.5f, 5.0f, 3.8f, 2.5f, 1.9f, 2.1f, 2.2f, 2.1f, 2.4f, 2.8f, 4.8f});
            } catch (DocumentException e) {
                throw new IllegalStateException("Não foi possível configurar as larguras do PDF de absenteísmo.", e);
            }

            for (String titulo : List.of(
                    "Colaborador", "Cargo", "Lotação", "Turno", "Dias previstos", "Faltas", "Atestados",
                    "DSR perdido", "Episódios", "Taxa %", "Índice disrupção", "Classificação")) {
                adicionarCabecalhoFrequencia(tabela, titulo, false);
            }

            for (RelatorioAbsenteismoDTO item : bloco) {
                boolean zebra = (sequencia.getAndIncrement() & 1L) == 1L;
                Color fundoLinha = zebra ? CINZA_ZEBRA : BRANCO;
                totais.somar(item);

                adicionarCelulaPdf(tabela, texto(item.nome()), 6.2f, false, fundoLinha,
                        new Color(15, 23, 42), true);
                adicionarCelulaPdf(tabela, texto(item.cargo()), 6.0f, false, fundoLinha, TEXTO, false);
                adicionarCelulaPdf(tabela, texto(item.lotacao()), 6.0f, false, fundoLinha, TEXTO, false);
                adicionarCelulaPdf(tabela, texto(item.turno()), 6.0f, true, fundoLinha, AZUL, true);
                adicionarCelulaPdf(tabela, Integer.toString(item.diasTrabalhaveis()), 6.1f, true,
                        fundoLinha, TEXTO, false);

                adicionarCelulaPdf(tabela, item.faltas() == 0 ? "-" : Integer.toString(item.faltas()), 6.2f, true,
                        item.faltas() == 0 ? fundoLinha : VERMELHO_CLARO,
                        item.faltas() == 0 ? TEXTO_SUAVE : VERMELHO_TEXTO,
                        item.faltas() > 0,
                        item.faltas() == 0 ? BORDA : new Color(248, 113, 113));
                adicionarCelulaPdf(tabela, item.atestados() == 0 ? "-" : Integer.toString(item.atestados()), 6.2f, true,
                        item.atestados() == 0 ? fundoLinha : AZUL_CLARO,
                        item.atestados() == 0 ? TEXTO_SUAVE : AZUL_TEXTO,
                        item.atestados() > 0,
                        item.atestados() == 0 ? BORDA : new Color(96, 165, 250));
                adicionarCelulaPdf(tabela, item.dsrPerdido() == 0 ? "-" : Integer.toString(item.dsrPerdido()), 6.2f, true,
                        item.dsrPerdido() == 0 ? fundoLinha : LARANJA_CLARO,
                        item.dsrPerdido() == 0 ? TEXTO_SUAVE : LARANJA_TEXTO,
                        item.dsrPerdido() > 0,
                        item.dsrPerdido() == 0 ? BORDA : new Color(251, 146, 60));
                adicionarCelulaPdf(tabela, item.spells() == 0 ? "-" : Integer.toString(item.spells()), 6.2f, true,
                        fundoLinha, TEXTO, item.spells() > 0);

                adicionarTaxaAbsenteismoPdf(tabela, item.taxaIndividual(), meta);
                adicionarDisrupcaoAbsenteismoPdf(tabela, item.disrupcao(), limiteDisrupcao);
                adicionarClassificacaoAbsenteismoPdf(
                        tabela, item.taxaIndividual(), meta, item.disrupcao(), limiteDisrupcao);
            }
            try {
                documento.add(tabela);
            } catch (DocumentException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private void adicionarTaxaAbsenteismoPdf(PdfPTable tabela, double taxa, double meta) {
        if (taxa <= meta) {
            adicionarCelulaPdf(tabela, String.format(Locale.forLanguageTag("pt-BR"), "%.2f", taxa), 6.2f, true,
                    VERDE_CLARO, VERDE_TEXTO, true, new Color(74, 222, 128));
        } else if (taxa <= meta * 2.0) {
            adicionarCelulaPdf(tabela, String.format(Locale.forLanguageTag("pt-BR"), "%.2f", taxa), 6.2f, true,
                    AMBAR_CLARO, AMBAR_TEXTO, true, new Color(253, 186, 116));
        } else {
            adicionarCelulaPdf(tabela, String.format(Locale.forLanguageTag("pt-BR"), "%.2f", taxa), 6.2f, true,
                    VERMELHO_CLARO, VERMELHO_TEXTO, true, new Color(248, 113, 113));
        }
    }

    private void adicionarDisrupcaoAbsenteismoPdf(PdfPTable tabela, double indice, double limiteDisrupcao) {
        if (indice < limiteDisrupcao) {
            adicionarCelulaPdf(tabela, String.format(Locale.forLanguageTag("pt-BR"), "%.2f", indice), 6.2f, true,
                    AZUL_CLARO, AZUL_TEXTO, true, new Color(96, 165, 250));
        } else if (indice < limiteDisrupcao * 2.0) {
            adicionarCelulaPdf(tabela, String.format(Locale.forLanguageTag("pt-BR"), "%.2f", indice), 6.2f, true,
                    AMBAR_CLARO, AMBAR_TEXTO, true, new Color(251, 146, 60));
        } else {
            adicionarCelulaPdf(tabela, String.format(Locale.forLanguageTag("pt-BR"), "%.2f", indice), 6.2f, true,
                    VERMELHO_CLARO, VERMELHO_TEXTO, true, new Color(248, 113, 113));
        }
    }

    private void adicionarClassificacaoAbsenteismoPdf(
            PdfPTable tabela, double taxa, double meta, double disrupcao, double limiteDisrupcao
    ) {
        String classificacao = classificar(taxa, meta, disrupcao, limiteDisrupcao);
        if (disrupcao >= limiteDisrupcao) {
            adicionarCelulaPdf(tabela, classificacao, 5.8f, true,
                    VERMELHO_CLARO, VERMELHO_TEXTO, true, new Color(248, 113, 113));
        } else if (taxa > meta) {
            adicionarCelulaPdf(tabela, classificacao, 5.8f, true,
                    AMBAR_CLARO, AMBAR_TEXTO, true, new Color(251, 146, 60));
        } else {
            adicionarCelulaPdf(tabela, classificacao, 5.8f, true,
                    VERDE_CLARO, VERDE_TEXTO, true, new Color(74, 222, 128));
        }
    }

    private void adicionarResumoAbsenteismoPdf(
            Document documento, long totalColaboradores, TotaisAbsenteismo totais, double meta
    ) throws DocumentException {
        double taxaGeral = totais.taxaGeral();
        Paragraph titulo = new Paragraph("Resumo do período", fonte(10.5f, Font.BOLD, AZUL));
        titulo.setSpacingBefore(5f);
        titulo.setSpacingAfter(5f);
        documento.add(titulo);

        PdfPTable resumo = new PdfPTable(6);
        resumo.setWidthPercentage(100);
        resumo.setSpacingAfter(8f);
        adicionarResumoAbsenteismoCelula(resumo, "Colaboradores", Long.toString(totalColaboradores), AZUL_CLARO, AZUL_TEXTO);
        adicionarResumoAbsenteismoCelula(resumo, "Dias previstos", Long.toString(totais.diasPrevistos), CINZA_ZEBRA, TEXTO);
        adicionarResumoAbsenteismoCelula(resumo, "Faltas", Long.toString(totais.faltas), VERMELHO_CLARO, VERMELHO_TEXTO);
        adicionarResumoAbsenteismoCelula(resumo, "Atestados", Long.toString(totais.atestados), AZUL_CLARO, AZUL_TEXTO);
        adicionarResumoAbsenteismoCelula(resumo, "DSR perdido", Long.toString(totais.dsrPerdido), LARANJA_CLARO, LARANJA_TEXTO);
        Color fundoTaxa = taxaGeral <= meta ? VERDE_CLARO : taxaGeral <= meta * 2.0 ? AMBAR_CLARO : VERMELHO_CLARO;
        Color textoTaxa = taxaGeral <= meta ? VERDE_TEXTO : taxaGeral <= meta * 2.0 ? AMBAR_TEXTO : VERMELHO_TEXTO;
        adicionarResumoAbsenteismoCelula(resumo, "Taxa geral", String.format(Locale.forLanguageTag("pt-BR"), "%.2f%%", taxaGeral), fundoTaxa, textoTaxa);
        documento.add(resumo);
    }

    private void adicionarResumoAbsenteismoCelula(
            PdfPTable tabela, String rotulo, String valor, Color fundo, Color corTexto
    ) {
        Paragraph p = new Paragraph();
        p.add(new Phrase(rotulo + "\n", fonte(6.8f, Font.NORMAL, TEXTO_SUAVE)));
        p.add(new Phrase(valor, fonte(10f, Font.BOLD, corTexto)));
        PdfPCell cell = new PdfPCell(p);
        cell.setBackgroundColor(fundo);
        cell.setBorderColor(BORDA);
        cell.setPadding(6f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        tabela.addCell(cell);
    }

    private void adicionarLegendaAbsenteismoPdf(
            Document documento, double meta, double limiteDisrupcao
    ) throws DocumentException {
        Paragraph titulo = new Paragraph("Legenda e critérios", fonte(10.5f, Font.BOLD, AZUL));
        titulo.setSpacingAfter(5f);
        documento.add(titulo);

        PdfPTable legenda = new PdfPTable(3);
        legenda.setWidthPercentage(100);
        legenda.setSpacingAfter(6f);
        adicionarLegendaAbsenteismoCelula(
                legenda, "DENTRO DA META",
                "Taxa até " + String.format(Locale.forLanguageTag("pt-BR"), "%.2f%%", meta),
                VERDE_CLARO, VERDE_TEXTO, new Color(74, 222, 128));
        adicionarLegendaAbsenteismoCelula(
                legenda, "ACIMA DA META",
                "Taxa superior à meta definida",
                AMBAR_CLARO, AMBAR_TEXTO, new Color(251, 146, 60));
        adicionarLegendaAbsenteismoCelula(
                legenda, "ALERTA DE DISRUPÇÃO",
                "Índice igual ou superior a " + String.format(Locale.ROOT, "%.0f", limiteDisrupcao),
                VERMELHO_CLARO, VERMELHO_TEXTO, new Color(248, 113, 113));
        documento.add(legenda);

        Paragraph criterios = new Paragraph(
                "Taxa de absenteísmo = (faltas + atestados) / dias previstos de trabalho. "
                        + "O índice de disrupção é uma pontuação operacional, não um percentual; valores maiores indicam ausências mais fragmentadas e recorrentes. "
                        + "DSR perdido é apresentado separadamente e não compõe o numerador da taxa.",
                fonte(7.4f, Font.NORMAL, TEXTO_SUAVE));
        criterios.setSpacingAfter(4f);
        documento.add(criterios);
    }

    private void adicionarLegendaAbsenteismoCelula(
            PdfPTable tabela, String titulo, String descricao, Color fundo, Color texto, Color borda
    ) {
        Paragraph p = new Paragraph();
        p.add(new Phrase(titulo + "\n", fonte(7.2f, Font.BOLD, texto)));
        p.add(new Phrase(descricao, fonte(6.7f, Font.NORMAL, texto)));
        PdfPCell cell = new PdfPCell(p);
        cell.setBackgroundColor(fundo);
        cell.setBorderColor(borda);
        cell.setPadding(5f);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        tabela.addCell(cell);
    }

    private void escreverTerceirizadosPdf(
            Document documento, List<RelatorioTerceirizadosDTO.EmpregadoResumo> lote, AtomicLong sequencia
    ) {
        final int porTabela = 120;
        for (int inicio = 0; inicio < lote.size(); inicio += porTabela) {
            List<RelatorioTerceirizadosDTO.EmpregadoResumo> bloco =
                    lote.subList(inicio, Math.min(lote.size(), inicio + porTabela));
            PdfPTable tabela = new PdfPTable(9);
            tabela.setWidthPercentage(100);
            tabela.setHeaderRows(1);
            cabecalhoPdf(tabela, "Colaborador", "Matrícula", "CPF", "Situação", "Cargo",
                    "Lotação", "Turno", "Admissão", "Contrato(s)");
            for (RelatorioTerceirizadosDTO.EmpregadoResumo item : bloco) {
                sequencia.incrementAndGet();
                adicionarCelula(tabela, texto(item.nome()), 6.0f, false);
                adicionarCelula(tabela, texto(item.matricula()), 5.8f, false);
                adicionarCelula(tabela, texto(item.cpf()), 5.8f, false);
                adicionarCelula(tabela, texto(item.statusDescricao()), 5.8f, true);
                adicionarCelula(tabela, texto(item.cargoNome()), 5.8f, false);
                adicionarCelula(tabela, texto(item.lotacaoNome()), 5.8f, false);
                adicionarCelula(tabela, texto(item.jornadaDescricao()), 5.8f, false);
                adicionarCelula(tabela, item.dataAdmissao() == null ? "-" : DATA_BR.format(item.dataAdmissao()), 5.8f, true);
                adicionarCelula(tabela, contratos(item.contratos()), 5.5f, false);
            }
            try {
                documento.add(tabela);
            } catch (DocumentException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private String contratos(List<RelatorioTerceirizadosDTO.ContratoResumo> contratos) {
        if (contratos == null || contratos.isEmpty()) return "-";
        return contratos.stream()
                .map(item -> {
                    String numero = item.numeroContrato();
                    String descricao = item.descricao();
                    String base = numero != null && !numero.isBlank() ? numero
                            : descricao != null && !descricao.isBlank() ? descricao
                            : "Contrato " + item.id();
                    return item.vigente() ? base : base + " (não vigente)";
                })
                .collect(java.util.stream.Collectors.joining(" | "));
    }

    private void adicionarTitulo(Document documento, String titulo, String subtitulo) throws DocumentException {
        Paragraph p = new Paragraph(titulo, fonte(15, Font.BOLD, AZUL));
        p.setSpacingAfter(4);
        documento.add(p);
        Paragraph s = new Paragraph(subtitulo, fonte(8, Font.NORMAL, Color.DARK_GRAY));
        s.setSpacingAfter(8);
        documento.add(s);
    }

    private void cabecalhoPdf(PdfPTable tabela, String... valores) {
        for (String valor : valores) adicionarCabecalho(tabela, valor);
    }

    private void adicionarCabecalho(PdfPTable tabela, String valor) {
        PdfPCell cell = new PdfPCell(new Paragraph(valor, fonte(5.2f, Font.BOLD, BRANCO)));
        cell.setBackgroundColor(AZUL);
        cell.setBorderColor(AMARELO);
        cell.setPadding(2f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        tabela.addCell(cell);
    }

    private void adicionarCelula(PdfPTable tabela, String valor, float tamanho, boolean centro) {
        PdfPCell cell = new PdfPCell(new Paragraph(valor, fonte(tamanho, Font.NORMAL, Color.DARK_GRAY)));
        cell.setBackgroundColor(CINZA);
        cell.setBorderColor(Color.LIGHT_GRAY);
        cell.setPadding(1.8f);
        if (centro) cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        tabela.addCell(cell);
    }

    private Font fonte(float tamanho, int estilo, Color cor) {
        return FontFactory.getFont(FontFactory.HELVETICA, tamanho, estilo, cor);
    }

    private String sigla(RelatorioFrequenciaConsolidadoDTO.Dia dia) {
        if (dia == null || dia.status() == null || "SEM_LANCAMENTO".equalsIgnoreCase(dia.status())) return "-";
        return switch (dia.status().toUpperCase(Locale.ROOT)) {
            case "PRESENCA", "TR" -> "P";
            case "FALTA" -> "F";
            case "PRESENCA_PARCIAL" -> "PP";
            case "ATESTADO" -> "AT";
            case "FERIADO" -> "FER";
            case "FOLGA", "DESCANSO" -> "D";
            case "DSR_PERDIDO" -> "DP";
            case "RT" -> "RT";
            case "SUSPENSAO" -> "SUSP";
            case "ADVERTENCIA" -> "ADV";
            case "FOLGA_REMUNERADA" -> "FR";
            default -> "?";
        };
    }

    private int contarStatus(RelatorioFrequenciaConsolidadoDTO.Linha linha, String status) {
        return (int) linha.diario().values().stream()
                .filter(dia -> dia != null && status.equalsIgnoreCase(dia.status()))
                .count();
    }

    private String classificar(double taxa, double meta, double disrupcao, double limiteDisrupcao) {
        if (disrupcao >= limiteDisrupcao) return "ALERTA DE DISRUPÇÃO";
        if (taxa > meta) return "ACIMA DA META";
        return "DENTRO DA META";
    }

    private String minutosParaDuracao(long minutos) {
        long seguro = Math.max(0, minutos);
        return String.format(Locale.ROOT, "%02d:%02d", seguro / 60, seguro % 60);
    }

    private String texto(String valor) {
        return valor == null || valor.isBlank() ? "-" : valor;
    }

    private String segmentoNomeArquivo(String valor) {
        String base = valor == null || valor.isBlank() ? "Nao_informado" : valor.trim();
        String semAcentos = Normalizer.normalize(base, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        String seguro = semAcentos
                .replaceAll("[^A-Za-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (seguro.isBlank()) seguro = "Nao_informado";
        return seguro.length() <= 48 ? seguro : seguro.substring(0, 48).replaceAll("_+$", "");
    }

    public record ContextoFrequencia(
            String lotacao,
            String turno,
            String contratos,
            LocalDateTime geradoEm,
            AvisoRevisaoRelatorioDTO aviso
    ) {
        public ContextoFrequencia(String lotacao, String turno, String contratos, LocalDateTime geradoEm) {
            this(lotacao, turno, contratos, geradoEm, AvisoRevisaoRelatorioDTO.definitivo());
        }
        public ContextoFrequencia comAviso(AvisoRevisaoRelatorioDTO novoAviso) {
            return new ContextoFrequencia(lotacao, turno, contratos, geradoEm,
                    novoAviso == null ? AvisoRevisaoRelatorioDTO.definitivo() : novoAviso);
        }
    }

    public record ContextoAbsenteismo(
            String lotacao,
            String turno,
            String contratos,
            LocalDateTime geradoEm,
            AvisoRevisaoRelatorioDTO aviso
    ) {
        public ContextoAbsenteismo(String lotacao, String turno, String contratos, LocalDateTime geradoEm) {
            this(lotacao, turno, contratos, geradoEm, AvisoRevisaoRelatorioDTO.definitivo());
        }
        public ContextoAbsenteismo comAviso(AvisoRevisaoRelatorioDTO novoAviso) {
            return new ContextoAbsenteismo(lotacao, turno, contratos, geradoEm,
                    novoAviso == null ? AvisoRevisaoRelatorioDTO.definitivo() : novoAviso);
        }
    }

    private record StatusVisual(
            String sigla,
            String descricao,
            Color fundo,
            Color texto,
            Color borda,
            int estiloXlsx
    ) { }

    private static final class EventoPaginaAbsenteismoPdf extends PdfPageEventHelper {
        private final String periodo;
        private final String lotacao;
        private final String turno;
        private final String contratos;
        private final String geradoEm;
        private final String meta;
        private final String limiteDisrupcao;

        private EventoPaginaAbsenteismoPdf(
                LocalDate inicio, LocalDate fim, ContextoAbsenteismo contexto,
                double meta, double limiteDisrupcao
        ) {
            ContextoAbsenteismo seguro = contexto == null
                    ? new ContextoAbsenteismo("Todas as lotações", "Todos os turnos", "Todos os contratos", LocalDateTime.now(ZONA_BR))
                    : contexto;
            this.periodo = DATA_BR.format(inicio) + " a " + DATA_BR.format(fim);
            this.lotacao = limitarTexto(seguro.lotacao(), 68);
            this.turno = limitarTexto(seguro.turno(), 46);
            this.contratos = limitarTexto(seguro.contratos(), 35);
            this.geradoEm = DATA_HORA_BR.format(seguro.geradoEm());
            this.meta = String.format(Locale.forLanguageTag("pt-BR"), "%.2f%%", meta);
            this.limiteDisrupcao = String.format(Locale.ROOT, "%.0f", limiteDisrupcao);
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte canvas = writer.getDirectContent();
            Rectangle pagina = document.getPageSize();
            float largura = pagina.getWidth();
            float topo = pagina.getTop();
            float esquerda = 28f;
            float direita = largura - 28f;

            canvas.saveState();
            canvas.setColorFill(AZUL_ESCURO);
            canvas.rectangle(0, topo - 68f, largura, 68f);
            canvas.fill();
            canvas.setColorFill(AMARELO);
            canvas.rectangle(0, topo - 68f, largura, 4f);
            canvas.fill();

            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT,
                    new Phrase("Relatório de Absenteísmo",
                            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15f, BRANCO)),
                    esquerda, topo - 24f, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT,
                    new Phrase("Período: " + periodo + " | Lotação: " + lotacao,
                            FontFactory.getFont(FontFactory.HELVETICA, 8.2f, BRANCO)),
                    esquerda, topo - 42f, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT,
                    new Phrase("Turno: " + turno + " | Contratos: " + contratos
                            + " | Meta: " + meta + " | Limite disrupção: " + limiteDisrupcao,
                            FontFactory.getFont(FontFactory.HELVETICA, 7.6f, new Color(219, 234, 254))),
                    esquerda, topo - 55f, 0);

            float yLinha = pagina.getBottom() + 28f;
            canvas.setColorStroke(BORDA);
            canvas.setLineWidth(0.6f);
            canvas.moveTo(esquerda, yLinha);
            canvas.lineTo(direita, yLinha);
            canvas.stroke();
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT,
                    new Phrase("SGT-GTRAT | Relatório de absenteísmo | Gerado em " + geradoEm,
                            FontFactory.getFont(FontFactory.HELVETICA, 7f, TEXTO_SUAVE)),
                    esquerda, pagina.getBottom() + 13f, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_RIGHT,
                    new Phrase("Página " + writer.getPageNumber(),
                            FontFactory.getFont(FontFactory.HELVETICA, 7f, TEXTO_SUAVE)),
                    direita, pagina.getBottom() + 13f, 0);
            canvas.restoreState();
        }

        private static String limitarTexto(String valor, int limite) {
            String texto = valor == null || valor.isBlank() ? "-" : valor.trim();
            if (texto.length() <= limite) return texto;
            return texto.substring(0, Math.max(1, limite - 3)) + "...";
        }
    }

    private static final class EventoPaginaFrequenciaPdf extends PdfPageEventHelper {
        private final String periodo;
        private final String lotacao;
        private final String turno;
        private final String contratos;
        private final String geradoEm;

        private EventoPaginaFrequenciaPdf(LocalDate inicio, LocalDate fim, ContextoFrequencia contexto) {
            ContextoFrequencia seguro = contexto == null
                    ? new ContextoFrequencia("Todas as lotações", "Todos os turnos", "Todos os contratos", LocalDateTime.now(ZONA_BR))
                    : contexto;
            this.periodo = DATA_BR.format(inicio) + " a " + DATA_BR.format(fim);
            this.lotacao = limitarTexto(seguro.lotacao(), 70);
            this.turno = limitarTexto(seguro.turno(), 55);
            this.contratos = limitarTexto(seguro.contratos(), 45);
            this.geradoEm = DATA_HORA_BR.format(seguro.geradoEm());
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte canvas = writer.getDirectContent();
            Rectangle pagina = document.getPageSize();
            float largura = pagina.getWidth();
            float topo = pagina.getTop();
            float esquerda = 28f;
            float direita = largura - 28f;

            canvas.saveState();
            canvas.setColorFill(AZUL_ESCURO);
            canvas.rectangle(0, topo - 68f, largura, 68f);
            canvas.fill();
            canvas.setColorFill(AMARELO);
            canvas.rectangle(0, topo - 68f, largura, 4f);
            canvas.fill();

            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT,
                    new Phrase("Relatório Consolidado de Frequência",
                            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15f, BRANCO)),
                    esquerda, topo - 24f, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT,
                    new Phrase("Período: " + periodo + " | Lotação: " + lotacao,
                            FontFactory.getFont(FontFactory.HELVETICA, 8.2f, BRANCO)),
                    esquerda, topo - 42f, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT,
                    new Phrase("Turno: " + turno + " | Contratos: " + contratos,
                            FontFactory.getFont(FontFactory.HELVETICA, 7.8f, new Color(219, 234, 254))),
                    esquerda, topo - 55f, 0);

            float yLinha = pagina.getBottom() + 28f;
            canvas.setColorStroke(BORDA);
            canvas.setLineWidth(0.6f);
            canvas.moveTo(esquerda, yLinha);
            canvas.lineTo(direita, yLinha);
            canvas.stroke();
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT,
                    new Phrase("SGT-GTRAT | Relatório de frequência | Gerado em " + geradoEm,
                            FontFactory.getFont(FontFactory.HELVETICA, 7f, TEXTO_SUAVE)),
                    esquerda, pagina.getBottom() + 13f, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_RIGHT,
                    new Phrase("Página " + writer.getPageNumber(),
                            FontFactory.getFont(FontFactory.HELVETICA, 7f, TEXTO_SUAVE)),
                    direita, pagina.getBottom() + 13f, 0);
            canvas.restoreState();
        }

        private static String limitarTexto(String valor, int limite) {
            String texto = valor == null || valor.isBlank() ? "-" : valor.trim();
            if (texto.length() <= limite) return texto;
            return texto.substring(0, Math.max(1, limite - 3)) + "...";
        }
    }

    private static final class TotaisAbsenteismo {
        private long faltas;
        private long atestados;
        private long dsrPerdido;
        private long episodios;
        private long diasPrevistos;

        private void somar(RelatorioAbsenteismoDTO item) {
            if (item == null) return;
            faltas += Math.max(0, item.faltas());
            atestados += Math.max(0, item.atestados());
            dsrPerdido += Math.max(0, item.dsrPerdido());
            episodios += Math.max(0, item.spells());
            diasPrevistos += Math.max(0, item.diasTrabalhaveis());
        }

        private double taxaGeral() {
            if (diasPrevistos <= 0) return 0.0;
            double taxa = ((faltas + atestados) * 100.0) / diasPrevistos;
            return Math.round(taxa * 100.0) / 100.0;
        }
    }

    private static final class TotaisExportacao {
        private long presencas, parciais, faltas, atestados, descansos, rt;
        private long minutosFisicos, minutosNoturnos, minutosPagaveis;

        private void somar(RelatorioFrequenciaConsolidadoDTO.Linha linha) {
            RelatorioFrequenciaConsolidadoDTO.Totais t = linha.totais();
            presencas += t.presencas(); parciais += t.presencasParciais(); faltas += t.faltas();
            atestados += t.atestados(); descansos += t.descansos();
            rt += linha.diario().values().stream().filter(d -> d != null && "RT".equalsIgnoreCase(d.status())).count();
            minutosFisicos += t.minutosFisicos(); minutosNoturnos += t.minutosNoturnosReduzidos();
            minutosPagaveis += t.minutosPagaveis();
        }
    }
}
