package com.example.siggtrat.load;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

public final class LoadTestPdfReport {

    private static final Locale LOCALE = Locale.US;
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            .withZone(ZoneId.of("America/Sao_Paulo"));

    public File write(LoadTestConfig config, LoadTestResult result) {
        File directory = new File("target/relatorios");
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IllegalStateException("Não foi possível criar target/relatorios.");
        }

        File file = new File(directory, "relatorio-teste-carga-http.pdf");
        Document document = new Document(PageSize.A4, 32, 32, 30, 30);
        try (FileOutputStream out = new FileOutputStream(file)) {
            PdfWriter.getInstance(document, out);
            document.open();

            Color blue = new Color(0, 68, 121);
            Color green = new Color(0, 120, 65);
            Color red = new Color(180, 30, 35);
            Color light = new Color(238, 242, 247);

            Paragraph title = new Paragraph(
                    "RELATÓRIO DE TESTE DE CARGA HTTP - SIG GTRAT",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 17, blue)
            );
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(10);
            document.add(title);

            Paragraph verdict = new Paragraph(
                    result.approved() ? "CARGA APROVADA" : "CARGA REPROVADA",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, result.approved() ? green : red)
            );
            verdict.setAlignment(Element.ALIGN_CENTER);
            verdict.setSpacingAfter(12);
            document.add(verdict);

            PdfPTable configTable = new PdfPTable(2);
            configTable.setWidthPercentage(100);
            addRow(configTable, "Alvo", config.uri().toString(), light);
            addRow(configTable, "Método", config.method(), light);
            addRow(configTable, "Início", DATE_TIME.format(result.startedAt()), light);
            addRow(configTable, "Fim", DATE_TIME.format(result.finishedAt()), light);
            addRow(configTable, "Usuários virtuais", Integer.toString(config.virtualUsers()), light);
            addRow(configTable, "Aquecimento", config.warmupSeconds() + " s", light);
            addRow(configTable, "Duração medida", String.format(LOCALE, "%.3f s", result.elapsed().toNanos() / 1_000_000_000.0), light);
            addRow(configTable, "Timeout por requisição", config.requestTimeout().toMillis() + " ms", light);
            addRow(configTable, "Verificação de integridade", config.verifyStableBodyHash() ? "SHA-256 estável da resposta 2xx" : "desativada", light);
            document.add(configTable);

            section(document, "Resultado agregado", blue);
            PdfPTable metrics = new PdfPTable(2);
            metrics.setWidthPercentage(100);
            addRow(metrics, "Requisições totais", Long.toString(result.totalRequests()), light);
            addRow(metrics, "2xx", Long.toString(result.successful2xx()), light);
            addRow(metrics, "429 / rate limit", Long.toString(result.throttled429()), light);
            addRow(metrics, "4xx (exceto 429)", Long.toString(result.clientErrors4xx()), light);
            addRow(metrics, "5xx", Long.toString(result.serverErrors5xx()), light);
            addRow(metrics, "Erros de transporte/timeout", Long.toString(result.transportErrors()), light);
            addRow(metrics, "Divergências de conteúdo", Long.toString(result.integrityMismatches()), light);
            addRow(metrics, "Throughput", String.format(LOCALE, "%.2f req/s", result.requestsPerSecond()), light);
            addRow(metrics, "Dados recebidos", humanBytes(result.bytesReceived()), light);
            document.add(metrics);

            section(document, "Latência", blue);
            PdfPTable latency = new PdfPTable(2);
            latency.setWidthPercentage(100);
            addRow(latency, "P50", ms(result.p50Ms()), light);
            addRow(latency, "P90", ms(result.p90Ms()), light);
            addRow(latency, "P95", ms(result.p95Ms()), light);
            addRow(latency, "P99", ms(result.p99Ms()), light);
            addRow(latency, "Máxima", ms(result.maxMs()), light);
            document.add(latency);

            section(document, "Distribuição HTTP", blue);
            PdfPTable statuses = new PdfPTable(new float[]{1f, 1.5f, 3f});
            statuses.setWidthPercentage(100);
            header(statuses, "Status", blue);
            header(statuses, "Quantidade", blue);
            header(statuses, "% do total", blue);
            if (result.statusCodes().isEmpty()) {
                statuses.addCell(cell("-", false));
                statuses.addCell(cell("0", false));
                statuses.addCell(cell("0.000%", false));
            } else {
                for (Map.Entry<Integer, Long> entry : result.statusCodes().entrySet()) {
                    statuses.addCell(cell(Integer.toString(entry.getKey()), false));
                    statuses.addCell(cell(Long.toString(entry.getValue()), false));
                    double pct = result.totalRequests() == 0 ? 0 : entry.getValue() * 100.0 / result.totalRequests();
                    statuses.addCell(cell(String.format(LOCALE, "%.3f%%", pct), false));
                }
            }
            document.add(statuses);

            section(document, "Critérios de aprovação", blue);
            PdfPTable thresholds = new PdfPTable(2);
            thresholds.setWidthPercentage(100);
            addRow(thresholds, "P95 máximo", ms(config.maxP95Ms()), light);
            addRow(thresholds, "Taxa 5xx máxima", pct(config.maxServerErrorRatePct()), light);
            addRow(thresholds, "Erros de transporte máximos", pct(config.maxTransportErrorRatePct()), light);
            addRow(thresholds, "4xx máximos (exceto 429)", pct(config.maxClientErrorRatePct()), light);
            addRow(thresholds, "429 máximo", pct(config.maxThrottleRatePct()), light);
            addRow(thresholds, "Divergência de conteúdo máxima", pct(config.maxIntegrityErrorRatePct()), light);
            addRow(thresholds, "RPS mínimo", config.minRequestsPerSecond() <= 0 ? "não exigido" : String.format(LOCALE, "%.2f", config.minRequestsPerSecond()), light);
            document.add(thresholds);

            if (!result.thresholdViolations().isEmpty()) {
                section(document, "Violações encontradas", red);
                for (String violation : result.thresholdViolations()) {
                    document.add(new Paragraph("- " + violation, FontFactory.getFont(FontFactory.HELVETICA, 9, red)));
                }
            }

            Paragraph note = new Paragraph(
                    "Observação: 429 é contabilizado separadamente porque pode representar proteção intencional por rate limiting. " +
                            "O teste de integridade compara SHA-256 do corpo das respostas 2xx quando habilitado.",
                    FontFactory.getFont(FontFactory.HELVETICA, 8, Color.DARK_GRAY)
            );
            note.setSpacingBefore(12);
            document.add(note);

            document.close();
            return file;
        } catch (Exception ex) {
            if (document.isOpen()) document.close();
            throw new IllegalStateException("Falha ao gerar relatório PDF do teste de carga.", ex);
        }
    }

    private static void section(Document document, String text, Color color) throws Exception {
        Paragraph section = new Paragraph(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, color));
        section.setSpacingBefore(11);
        section.setSpacingAfter(5);
        document.add(section);
    }

    private static void addRow(PdfPTable table, String key, String value, Color keyBackground) {
        PdfPCell keyCell = cell(key, true);
        keyCell.setBackgroundColor(keyBackground);
        table.addCell(keyCell);
        table.addCell(cell(value, false));
    }

    private static void header(PdfPTable table, String text, Color background) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE)));
        cell.setBackgroundColor(background);
        cell.setPadding(5);
        table.addCell(cell);
    }

    private static PdfPCell cell(String text, boolean bold) {
        Font font = FontFactory.getFont(bold ? FontFactory.HELVETICA_BOLD : FontFactory.HELVETICA, 8);
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, font));
        cell.setPadding(4);
        cell.setVerticalAlignment(Element.ALIGN_TOP);
        return cell;
    }

    private static String ms(double value) {
        return String.format(LOCALE, "%.2f ms", value);
    }

    private static String pct(double value) {
        return String.format(LOCALE, "%.3f%%", value);
    }

    private static String humanBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024L * 1024L) return String.format(LOCALE, "%.2f KiB", bytes / 1024.0);
        return String.format(LOCALE, "%.2f MiB", bytes / (1024.0 * 1024.0));
    }
}
