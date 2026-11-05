package com.example.siggtrat.utilitario;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Escritor XLSX sequencial de memória constante.
 *
 * <p>Usa strings inline em vez de sharedStrings para que relatórios com
 * centenas de milhares de linhas sejam gravados sequencialmente em arquivo
 * temporário, sem manter a planilha inteira na heap.</p>
 */
public final class XlsxStreamingWriter implements AutoCloseable {

    public record Cell(String value, boolean numeric, int style) {
        public static Cell text(String value) { return new Cell(value, false, 0); }
        public static Cell text(String value, int style) { return new Cell(value, false, style); }
        public static Cell number(Number value) { return new Cell(value == null ? "0" : value.toString(), true, 0); }
        public static Cell number(Number value, int style) { return new Cell(value == null ? "0" : value.toString(), true, style); }
        public static Cell empty() { return new Cell("", false, 0); }
    }

    // Estilos genéricos já utilizados pelos demais relatórios.
    public static final int STYLE_NORMAL = 0;
    public static final int STYLE_TITLE = 1;
    public static final int STYLE_SUBTITLE = 2;
    public static final int STYLE_HEADER = 3;
    public static final int STYLE_NUMBER = 4;
    public static final int STYLE_TOTAL = 5;
    public static final int STYLE_DECIMAL = 6;

    // Estilos específicos do relatório de frequência. Mantidos separados para
    // não alterar visual ou comportamento dos outros relatórios existentes.
    public static final int STYLE_FREQ_TITLE = 7;
    public static final int STYLE_FREQ_FILTER = 8;
    public static final int STYLE_FREQ_NOTE = 9;
    public static final int STYLE_FREQ_HEADER_LEFT = 10;
    public static final int STYLE_FREQ_HEADER_WEEKEND = 11;
    public static final int STYLE_FREQ_BODY = 12;
    public static final int STYLE_FREQ_BODY_ZEBRA = 13;
    public static final int STYLE_FREQ_BODY_BOLD = 14;
    public static final int STYLE_FREQ_BODY_BOLD_ZEBRA = 15;
    public static final int STYLE_FREQ_TURNO = 16;
    public static final int STYLE_FREQ_TURNO_ZEBRA = 17;
    public static final int STYLE_STATUS_P = 18;
    public static final int STYLE_STATUS_PP = 19;
    public static final int STYLE_STATUS_F = 20;
    public static final int STYLE_STATUS_AT = 21;
    public static final int STYLE_STATUS_FER = 22;
    public static final int STYLE_STATUS_DP = 23;
    public static final int STYLE_STATUS_RT = 24;
    public static final int STYLE_STATUS_D = 25;
    public static final int STYLE_STATUS_SUSP = 26;
    public static final int STYLE_STATUS_ADV = 27;
    public static final int STYLE_STATUS_FR = 28;
    public static final int STYLE_RESUMO_HREAL = 29;
    public static final int STYLE_RESUMO_HNOT = 30;
    public static final int STYLE_RESUMO_HPAG = 31;
    public static final int STYLE_RESUMO_HDEC = 32;
    public static final int STYLE_LEGEND_TITLE = 33;
    public static final int STYLE_LEGEND_NOTE = 34;
    public static final int STYLE_FREQ_CENTER = 35;
    public static final int STYLE_FREQ_CENTER_ZEBRA = 36;

    // Estilos numéricos específicos de absenteísmo. O valor já é expresso em
    // pontos percentuais (ex.: 3.25), por isso o formato permanece 0.00 e a
    // coluna identifica a unidade (%).
    public static final int STYLE_ABS_PERCENT_OK = 37;
    public static final int STYLE_ABS_PERCENT_WARN = 38;
    public static final int STYLE_ABS_PERCENT_CRITICAL = 39;
    public static final int STYLE_ABS_DISRUPCAO_NORMAL = 40;
    public static final int STYLE_ABS_DISRUPCAO_ALERTA = 41;
    public static final int STYLE_ABS_DISRUPCAO_CRITICA = 42;

    private final ZipOutputStream zip;
    private final List<String> mergedCells = new ArrayList<>();
    private int rowNumber;
    private boolean finished;

    public XlsxStreamingWriter(OutputStream output, String sheetName, int freezeRows,
                               int freezeColumns, List<Double> columnWidths) throws IOException {
        this.zip = new ZipOutputStream(output, StandardCharsets.UTF_8);
        writePackageParts(sheetName);
        zip.putNextEntry(new ZipEntry("xl/worksheets/sheet1.xml"));
        write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        write("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">");
        write("<sheetViews><sheetView workbookViewId=\"0\" showGridLines=\"0\">");
        if (freezeRows > 0 || freezeColumns > 0) {
            String topLeft = columnName(freezeColumns + 1) + (freezeRows + 1);
            write("<pane");
            if (freezeColumns > 0) write(" xSplit=\"" + freezeColumns + "\"");
            if (freezeRows > 0) write(" ySplit=\"" + freezeRows + "\"");
            write(" topLeftCell=\"" + topLeft + "\" state=\"frozen\"/>");
        }
        write("</sheetView></sheetViews>");
        if (columnWidths != null && !columnWidths.isEmpty()) {
            write("<cols>");
            for (int i = 0; i < columnWidths.size(); i++) {
                double width = Math.max(3.0, Math.min(80.0, columnWidths.get(i)));
                int index = i + 1;
                write("<col min=\"" + index + "\" max=\"" + index + "\" width=\""
                        + width + "\" customWidth=\"1\"/>");
            }
            write("</cols>");
        }
        write("<sheetData>");
    }

    public void writeRow(List<Cell> cells) throws IOException {
        writeRow(cells, null);
    }

    /** Escreve uma linha permitindo altura visual sem acumular linhas em memória. */
    public void writeRow(List<Cell> cells, Double height) throws IOException {
        if (finished) throw new IllegalStateException("Planilha já finalizada.");
        rowNumber++;
        write("<row r=\"" + rowNumber + "\"");
        if (height != null && height > 0) {
            double segura = Math.max(10.0, Math.min(80.0, height));
            write(" ht=\"" + segura + "\" customHeight=\"1\"");
        }
        write(">");
        for (int i = 0; i < cells.size(); i++) {
            Cell cell = cells.get(i);
            if (cell == null) cell = Cell.empty();
            String ref = columnName(i + 1) + rowNumber;
            int style = Math.max(0, cell.style());
            if (cell.numeric()) {
                write("<c r=\"" + ref + "\" s=\"" + style + "\" t=\"n\"><v>"
                        + sanitizeNumber(cell.value()) + "</v></c>");
            } else {
                write("<c r=\"" + ref + "\" s=\"" + style + "\" t=\"inlineStr\"><is><t xml:space=\"preserve\">"
                        + escapeXml(cell.value()) + "</t></is></c>");
            }
        }
        write("</row>");
        if (rowNumber % 100 == 0) zip.flush();
    }

    /** Registra uma mesclagem pequena de apresentação; apenas a referência fica em memória. */
    public void mergeCells(String ref) {
        if (finished) throw new IllegalStateException("Planilha já finalizada.");
        if (ref == null || !ref.matches("[A-Z]+\\d+:[A-Z]+\\d+")) {
            throw new IllegalArgumentException("Referência de mesclagem inválida: " + ref);
        }
        mergedCells.add(ref);
    }

    public int currentRow() { return rowNumber; }

    /** Força a persistência do lote atual no arquivo temporário do servidor. */
    public void flush() throws IOException {
        zip.flush();
    }

    public void finish(String autoFilterRef) throws IOException {
        if (finished) return;
        write("</sheetData>");
        if (!mergedCells.isEmpty()) {
            write("<mergeCells count=\"" + mergedCells.size() + "\">");
            for (String ref : mergedCells) {
                write("<mergeCell ref=\"" + escapeXmlAttribute(ref) + "\"/>");
            }
            write("</mergeCells>");
        }
        if (autoFilterRef != null && !autoFilterRef.isBlank()) {
            write("<autoFilter ref=\"" + escapeXmlAttribute(autoFilterRef) + "\"/>");
        }
        write("<pageMargins left=\"0.25\" right=\"0.25\" top=\"0.45\" bottom=\"0.45\" header=\"0.2\" footer=\"0.2\"/>");
        write("<pageSetup orientation=\"landscape\" fitToWidth=\"1\" fitToHeight=\"0\"/>");
        write("</worksheet>");
        zip.closeEntry();
        finished = true;
    }

    @Override
    public void close() throws IOException {
        if (!finished) finish(null);
        zip.finish();
        zip.flush();
    }

    private void writePackageParts(String sheetName) throws IOException {
        putEntry("[Content_Types].xml", """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                  <Default Extension="xml" ContentType="application/xml"/>
                  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
                </Types>
                """);
        putEntry("_rels/.rels", """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                </Relationships>
                """);
        putEntry("xl/workbook.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" "
                + "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
                + "<sheets><sheet name=\"" + escapeXmlAttribute(safeSheetName(sheetName))
                + "\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");
        putEntry("xl/_rels/workbook.xml.rels", """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
                  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
                </Relationships>
                """);
        putEntry("xl/styles.xml", stylesXml());
    }

    private String stylesXml() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                  <numFmts count="1"><numFmt numFmtId="164" formatCode="0.00"/></numFmts>
                  <fonts count="20">
                    <font><sz val="10"/><color rgb="FF334155"/><name val="Arial"/></font>
                    <font><b/><sz val="15"/><color rgb="FF00416B"/><name val="Arial"/></font>
                    <font><b/><sz val="10"/><color rgb="FF334155"/><name val="Arial"/></font>
                    <font><b/><sz val="10"/><color rgb="FFFFFFFF"/><name val="Arial"/></font>
                    <font><b/><sz val="15"/><color rgb="FFFFFFFF"/><name val="Arial"/></font>
                    <font><b/><sz val="10"/><color rgb="FF003B5C"/><name val="Arial"/></font>
                    <font><i/><sz val="9"/><color rgb="FF64748B"/><name val="Arial"/></font>
                    <font><sz val="10"/><color rgb="FF334155"/><name val="Arial"/></font>
                    <font><b/><sz val="10"/><color rgb="FF0F172A"/><name val="Arial"/></font>
                    <font><b/><sz val="10"/><color rgb="FF00416B"/><name val="Arial"/></font>
                    <font><b/><sz val="9"/><color rgb="FF14532D"/><name val="Arial"/></font>
                    <font><b/><sz val="9"/><color rgb="FF78350F"/><name val="Arial"/></font>
                    <font><b/><sz val="9"/><color rgb="FFB91C1C"/><name val="Arial"/></font>
                    <font><b/><sz val="9"/><color rgb="FF1E3A8A"/><name val="Arial"/></font>
                    <font><b/><sz val="9"/><color rgb="FF581C87"/><name val="Arial"/></font>
                    <font><b/><sz val="9"/><color rgb="FFFFFFFF"/><name val="Arial"/></font>
                    <font><b/><sz val="9"/><color rgb="FF0F766E"/><name val="Arial"/></font>
                    <font><b/><sz val="9"/><color rgb="FF6B7280"/><name val="Arial"/></font>
                    <font><b/><sz val="9"/><color rgb="FFBE123C"/><name val="Arial"/></font>
                    <font><b/><sz val="9"/><color rgb="FFB45309"/><name val="Arial"/></font>
                  </fonts>
                  <fills count="23">
                    <fill><patternFill patternType="none"/></fill>
                    <fill><patternFill patternType="gray125"/></fill>
                    <fill><patternFill patternType="solid"><fgColor rgb="FFEAF3F8"/><bgColor indexed="64"/></patternFill></fill>
                    <fill><patternFill patternType="solid"><fgColor rgb="FF00416B"/><bgColor indexed="64"/></patternFill></fill>
                    <fill><patternFill patternType="solid"><fgColor rgb="FFFFD400"/><bgColor indexed="64"/></patternFill></fill>
                    <fill><patternFill patternType="solid"><fgColor rgb="FF003B5C"/><bgColor indexed="64"/></patternFill></fill>
                    <fill><patternFill patternType="solid"><fgColor rgb="FFF8FAFC"/><bgColor indexed="64"/></patternFill></fill>
                    <fill><patternFill patternType="solid"><fgColor rgb="FFF8FAFC"/><bgColor indexed="64"/></patternFill></fill>
                    <fill><patternFill patternType="solid"><fgColor rgb="FFD1FAE5"/><bgColor indexed="64"/></patternFill></fill>
                    <fill><patternFill patternType="solid"><fgColor rgb="FFFFEDD5"/><bgColor indexed="64"/></patternFill></fill>
                    <fill><patternFill patternType="solid"><fgColor rgb="FFFEE2E2"/><bgColor indexed="64"/></patternFill></fill>
                    <fill><patternFill patternType="solid"><fgColor rgb="FFDBEAFE"/><bgColor indexed="64"/></patternFill></fill>
                    <fill><patternFill patternType="solid"><fgColor rgb="FFE9D5FF"/><bgColor indexed="64"/></patternFill></fill>
                    <fill><patternFill patternType="solid"><fgColor rgb="FFF97316"/><bgColor indexed="64"/></patternFill></fill>
                    <fill><patternFill patternType="solid"><fgColor rgb="FFCCFBF1"/><bgColor indexed="64"/></patternFill></fill>
                    <fill><patternFill patternType="solid"><fgColor rgb="FFF3F4F6"/><bgColor indexed="64"/></patternFill></fill>
                    <fill><patternFill patternType="solid"><fgColor rgb="FFFFE4E6"/><bgColor indexed="64"/></patternFill></fill>
                    <fill><patternFill patternType="solid"><fgColor rgb="FFFEF3C7"/><bgColor indexed="64"/></patternFill></fill>
                    <fill><patternFill patternType="solid"><fgColor rgb="FFCCFBF1"/><bgColor indexed="64"/></patternFill></fill>
                    <fill><patternFill patternType="solid"><fgColor rgb="FFFEFCE8"/><bgColor indexed="64"/></patternFill></fill>
                    <fill><patternFill patternType="solid"><fgColor rgb="FFEFF6FF"/><bgColor indexed="64"/></patternFill></fill>
                    <fill><patternFill patternType="solid"><fgColor rgb="FFF5F3FF"/><bgColor indexed="64"/></patternFill></fill>
                    <fill><patternFill patternType="solid"><fgColor rgb="FF244C91"/><bgColor indexed="64"/></patternFill></fill>
                  </fills>
                  <borders count="2">
                    <border><left/><right/><top/><bottom/><diagonal/></border>
                    <border><left style="thin"><color rgb="FFD7E1E8"/></left><right style="thin"><color rgb="FFD7E1E8"/></right><top style="thin"><color rgb="FFD7E1E8"/></top><bottom style="thin"><color rgb="FFD7E1E8"/></bottom><diagonal/></border>
                  </borders>
                  <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
                  <cellXfs count="43">
                    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
                    <xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/>
                    <xf numFmtId="0" fontId="2" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1"/>
                    <xf numFmtId="0" fontId="3" fillId="3" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1"><alignment horizontal="center" vertical="center" wrapText="1"/></xf>
                    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1"><alignment horizontal="right" vertical="center"/></xf>
                    <xf numFmtId="0" fontId="3" fillId="3" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1"><alignment horizontal="right" vertical="center"/></xf>
                    <xf numFmtId="164" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1"><alignment horizontal="right" vertical="center"/></xf>

                    <xf numFmtId="0" fontId="4" fillId="5" borderId="0" xfId="0" applyFont="1" applyFill="1"><alignment horizontal="left" vertical="center"/></xf>
                    <xf numFmtId="0" fontId="5" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1"><alignment horizontal="left" vertical="center"/></xf>
                    <xf numFmtId="0" fontId="6" fillId="6" borderId="0" xfId="0" applyFont="1" applyFill="1"><alignment horizontal="left" vertical="center"/></xf>
                    <xf numFmtId="0" fontId="3" fillId="3" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1"><alignment horizontal="left" vertical="center" wrapText="1"/></xf>
                    <xf numFmtId="0" fontId="3" fillId="22" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1"><alignment horizontal="center" vertical="center" wrapText="1"/></xf>
                    <xf numFmtId="0" fontId="7" fillId="0" borderId="1" xfId="0" applyFont="1" applyBorder="1"><alignment horizontal="left" vertical="center" wrapText="1"/></xf>
                    <xf numFmtId="0" fontId="7" fillId="7" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1"><alignment horizontal="left" vertical="center" wrapText="1"/></xf>
                    <xf numFmtId="0" fontId="8" fillId="0" borderId="1" xfId="0" applyFont="1" applyBorder="1"><alignment horizontal="left" vertical="center" wrapText="1"/></xf>
                    <xf numFmtId="0" fontId="8" fillId="7" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1"><alignment horizontal="left" vertical="center" wrapText="1"/></xf>
                    <xf numFmtId="0" fontId="9" fillId="0" borderId="1" xfId="0" applyFont="1" applyBorder="1"><alignment horizontal="center" vertical="center" wrapText="1"/></xf>
                    <xf numFmtId="0" fontId="9" fillId="7" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1"><alignment horizontal="center" vertical="center" wrapText="1"/></xf>

                    <xf numFmtId="0" fontId="10" fillId="8" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1"><alignment horizontal="center" vertical="center"/></xf>
                    <xf numFmtId="0" fontId="11" fillId="9" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1"><alignment horizontal="center" vertical="center"/></xf>
                    <xf numFmtId="0" fontId="12" fillId="10" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1"><alignment horizontal="center" vertical="center"/></xf>
                    <xf numFmtId="0" fontId="13" fillId="11" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1"><alignment horizontal="center" vertical="center"/></xf>
                    <xf numFmtId="0" fontId="14" fillId="12" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1"><alignment horizontal="center" vertical="center"/></xf>
                    <xf numFmtId="0" fontId="15" fillId="13" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1"><alignment horizontal="center" vertical="center"/></xf>
                    <xf numFmtId="0" fontId="16" fillId="14" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1"><alignment horizontal="center" vertical="center"/></xf>
                    <xf numFmtId="0" fontId="17" fillId="15" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1"><alignment horizontal="center" vertical="center"/></xf>
                    <xf numFmtId="0" fontId="18" fillId="16" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1"><alignment horizontal="center" vertical="center"/></xf>
                    <xf numFmtId="0" fontId="19" fillId="17" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1"><alignment horizontal="center" vertical="center"/></xf>
                    <xf numFmtId="0" fontId="16" fillId="18" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1"><alignment horizontal="center" vertical="center"/></xf>

                    <xf numFmtId="0" fontId="19" fillId="19" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1"><alignment horizontal="center" vertical="center"/></xf>
                    <xf numFmtId="0" fontId="13" fillId="20" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1"><alignment horizontal="center" vertical="center"/></xf>
                    <xf numFmtId="0" fontId="14" fillId="21" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1"><alignment horizontal="center" vertical="center"/></xf>
                    <xf numFmtId="164" fontId="17" fillId="15" borderId="1" xfId="0" applyFont="1" applyFill="1" applyNumberFormat="1" applyBorder="1"><alignment horizontal="center" vertical="center"/></xf>
                    <xf numFmtId="0" fontId="5" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1"><alignment horizontal="left" vertical="center"/></xf>
                    <xf numFmtId="0" fontId="6" fillId="6" borderId="0" xfId="0" applyFont="1" applyFill="1"><alignment horizontal="left" vertical="center" wrapText="1"/></xf>
                    <xf numFmtId="0" fontId="7" fillId="0" borderId="1" xfId="0" applyFont="1" applyBorder="1"><alignment horizontal="center" vertical="center"/></xf>
                    <xf numFmtId="0" fontId="7" fillId="7" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1"><alignment horizontal="center" vertical="center"/></xf>

                    <xf numFmtId="164" fontId="10" fillId="8" borderId="1" xfId="0" applyFont="1" applyFill="1" applyNumberFormat="1" applyBorder="1"><alignment horizontal="center" vertical="center"/></xf>
                    <xf numFmtId="164" fontId="11" fillId="9" borderId="1" xfId="0" applyFont="1" applyFill="1" applyNumberFormat="1" applyBorder="1"><alignment horizontal="center" vertical="center"/></xf>
                    <xf numFmtId="164" fontId="12" fillId="10" borderId="1" xfId="0" applyFont="1" applyFill="1" applyNumberFormat="1" applyBorder="1"><alignment horizontal="center" vertical="center"/></xf>
                    <xf numFmtId="164" fontId="13" fillId="11" borderId="1" xfId="0" applyFont="1" applyFill="1" applyNumberFormat="1" applyBorder="1"><alignment horizontal="center" vertical="center"/></xf>
                    <xf numFmtId="164" fontId="11" fillId="9" borderId="1" xfId="0" applyFont="1" applyFill="1" applyNumberFormat="1" applyBorder="1"><alignment horizontal="center" vertical="center"/></xf>
                    <xf numFmtId="164" fontId="12" fillId="10" borderId="1" xfId="0" applyFont="1" applyFill="1" applyNumberFormat="1" applyBorder="1"><alignment horizontal="center" vertical="center"/></xf>
                  </cellXfs>
                  <cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>
                </styleSheet>
                """;
    }

    private void putEntry(String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        write(content);
        zip.closeEntry();
    }

    private void write(String value) throws IOException {
        zip.write(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String sanitizeNumber(String value) {
        if (value == null || !value.matches("-?\\d+(\\.\\d+)?")) return "0";
        return value;
    }

    private static String safeSheetName(String value) {
        String result = value == null || value.isBlank() ? "Relatorio" : value.replaceAll("[\\\\/*?:\\[\\]]", "-");
        return result.length() <= 31 ? result : result.substring(0, 31);
    }

    public static String columnName(int index) {
        StringBuilder sb = new StringBuilder();
        int value = index;
        while (value > 0) {
            value--;
            sb.insert(0, (char) ('A' + (value % 26)));
            value /= 26;
        }
        return sb.toString();
    }

    private static String escapeXml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }

    private static String escapeXmlAttribute(String value) {
        return escapeXml(value);
    }
}
