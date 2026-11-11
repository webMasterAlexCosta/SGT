export type SpreadsheetValue = string | number | boolean | Date | null | undefined;
export interface StyledSpreadsheetCell { value: SpreadsheetValue; style?: SpreadsheetStyle; __styleIndex?: number }
export type SpreadsheetCell = SpreadsheetValue | StyledSpreadsheetCell;
export interface SpreadsheetRow { cells: SpreadsheetCell[]; style?: SpreadsheetStyle; height?: number }
export interface SpreadsheetSheet {
  name?: string; rows?: Array<SpreadsheetRow | SpreadsheetCell[]>;
  columns?: Array<{ width?: number | string }>;
  merges?: string[]; autoFilter?: string; freezeRows?: number; freezeColumns?: number;
  showGridLines?: boolean; defaultRowHeight?: number;
  pageSetup?: { paperSize?: string | number; orientation?: string; fitToWidth?: number; fitToHeight?: number; scale?: number };
  printOptions?: { horizontalCentered?: boolean; verticalCentered?: boolean; gridLines?: boolean };
  pageMargins?: Partial<Record<'left' | 'right' | 'top' | 'bottom' | 'header' | 'footer', number>>;
}
export interface SpreadsheetWorkbook { creator?: string; created?: Date | string | number; sheets: SpreadsheetSheet[] }
interface NormalizedStyle {
  font: { bold: boolean; italic: boolean; size: number | null; color: string };
  fillColor: string | null;
  alignment: { horizontal: string | null; vertical: string | null; wrapText: boolean };
  border: { enabled: boolean; color: string };
  numberFormat: string | null;
}
type StyleRegistry = (style?: SpreadsheetStyle) => number;
const styledCell = (cell: SpreadsheetCell): cell is StyledSpreadsheetCell => cell !== null && typeof cell === 'object' && Object.hasOwn(cell, 'value');

export interface SpreadsheetStyle {
  font?: { bold?: boolean; italic?: boolean; size?: number; color?: string };
  alignment?: { horizontal?: string; vertical?: string; wrapText?: boolean };
  border?: boolean | { enabled?: boolean; color?: string };
  fillColor?: string;
  numberFormat?: string;
}

const XLSX_MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
const XMLNS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main";

const encoder = new TextEncoder();

const escaparXml = (valor: unknown) =>
  String(valor ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&apos;");

const normalizarCor = (cor: string | undefined, padrao = "FF000000") => {
  if (!cor) return padrao;
  const limpa = String(cor).replace(/^#/, "").toUpperCase();
  if (/^[0-9A-F]{8}$/.test(limpa)) return limpa;
  if (/^[0-9A-F]{6}$/.test(limpa)) return `FF${limpa}`;
  return padrao;
};

const colunaParaNome = (indiceBaseUm: (number)) => {
  let indice = indiceBaseUm;
  let resultado = "";
  while (indice > 0) {
    const resto = (indice - 1) % 26;
    resultado = String.fromCharCode(65 + resto) + resultado;
    indice = Math.floor((indice - 1) / 26);
  }
  return resultado;
};

const referenciaCelula = (linha: (number), coluna: (number)) => `${colunaParaNome(coluna)}${linha}`;

const normalizarNomePlanilha = (nome: string | undefined, indice: (number)) => {
  const limpo = String(nome || `Planilha ${indice + 1}`)
    .replace(/[\\/*?:\[\]]/g, "-")
    .slice(0, 31)
    .trim();
  return limpo || `Planilha ${indice + 1}`;
};

const serializarEstilo = (estilo: SpreadsheetStyle = {}) => {
  const font = estilo.font || {};
  const alignment = estilo.alignment || {};
  const border = estilo.border || {};
  return JSON.stringify({
    font: {
      bold: Boolean(font.bold),
      italic: Boolean(font.italic),
      size: Number(font.size || 11),
      color: normalizarCor(font.color, "FF000000"),
    },
    fillColor: estilo.fillColor ? normalizarCor(estilo.fillColor, "FFFFFFFF") : null,
    alignment: {
      horizontal: alignment.horizontal || null,
      vertical: alignment.vertical || null,
      wrapText: Boolean(alignment.wrapText),
    },
    border: border === true
      ? { enabled: true, color: "FFD1D5DB" }
      : {
          enabled: Boolean(border.enabled),
          color: normalizarCor(border.color, "FFD1D5DB"),
        },
    numberFormat: estilo.numberFormat ? String(estilo.numberFormat) : null,
  });
};

const criarRegistroEstilos = (planilhas: SpreadsheetSheet[]) => {
  const estilos: NormalizedStyle[] = [JSON.parse(serializarEstilo()) as NormalizedStyle];
  const indices = new Map([[serializarEstilo(), 0]]);

  const registrar = (estilo?: SpreadsheetStyle) => {
    const chave = serializarEstilo(estilo);
    if (!indices.has(chave)) {
      indices.set(chave, estilos.length);
      estilos.push(JSON.parse(chave) as NormalizedStyle);
    }
    return indices.get(chave)!;
  };

  planilhas.forEach((planilha) => {
    (planilha.rows || []).forEach((linha) => {
      const estiloLinha = Array.isArray(linha) ? null : linha.style || null;
      (Array.isArray(linha) ? linha : linha.cells || []).forEach((celula) => {
        if (styledCell(celula)) {
          celula.__styleIndex = registrar(celula.style || estiloLinha || {});
        } else if (estiloLinha) {
          registrar(estiloLinha);
        }
      });
    });
  });

  return { estilos, registrar };
};

const criarXmlEstilos = (estilos: NormalizedStyle[]) => {
  const fonts: NormalizedStyle["font"][] = [];
  const fills = [null, "gray125"];
  const borders: Array<NormalizedStyle["border"] | null> = [null];
  const fontMap = new Map<string, number>();
  const fillMap = new Map([["", 0]]);
  const borderMap = new Map([["", 0]]);
  const formatosNumero: Array<{ id: number; formato: string }> = [];
  const formatoNumeroMap = new Map<string, number>();

  const obterFontId = (font: NormalizedStyle["font"]) => {
    const chave = JSON.stringify(font);
    if (!fontMap.has(chave)) {
      fontMap.set(chave, fonts.length);
      fonts.push(font);
    }
    return fontMap.get(chave)!;
  };

  const obterFillId = (cor: string | null) => {
    if (!cor) return 0;
    if (!fillMap.has(cor)) {
      fillMap.set(cor, fills.length);
      fills.push(cor);
    }
    return fillMap.get(cor)!;
  };

  const obterBorderId = (border: NormalizedStyle["border"]) => {
    if (!border?.enabled) return 0;
    const chave = border.color;
    if (!borderMap.has(chave)) {
      borderMap.set(chave, borders.length);
      borders.push(border);
    }
    return borderMap.get(chave)!;
  };

  const obterNumFmtId = (formato: string | null) => {
    if (!formato) return 0;
    if (!formatoNumeroMap.has(formato)) {
      const id = 164 + formatosNumero.length;
      formatoNumeroMap.set(formato, id);
      formatosNumero.push({ id, formato });
    }
    return formatoNumeroMap.get(formato)!;
  };

  const xfs = estilos.map((estilo) => ({
    fontId: obterFontId(estilo.font),
    fillId: obterFillId(estilo.fillColor),
    borderId: obterBorderId(estilo.border),
    numFmtId: obterNumFmtId(estilo.numberFormat),
    alignment: estilo.alignment,
  }));

  const fontsXml = fonts
    .map((font) => `<font>${font.bold ? "<b/>" : ""}${font.italic ? "<i/>" : ""}<sz val="${font.size}"/><color rgb="${font.color}"/><name val="Calibri"/><family val="2"/><scheme val="minor"/></font>`)
    .join("");

  const fillsXml = fills
    .map((fill, indice) => {
      if (indice === 0) return '<fill><patternFill patternType="none"/></fill>';
      if (fill === "gray125") return '<fill><patternFill patternType="gray125"/></fill>';
      return `<fill><patternFill patternType="solid"><fgColor rgb="${fill}"/><bgColor indexed="64"/></patternFill></fill>`;
    })
    .join("");

  const bordersXml = borders
    .map((border) => {
      if (!border) return "<border><left/><right/><top/><bottom/><diagonal/></border>";
      const lado = `<color rgb="${border.color}"/>`;
      return `<border><left style="thin">${lado}</left><right style="thin">${lado}</right><top style="thin">${lado}</top><bottom style="thin">${lado}</bottom><diagonal/></border>`;
    })
    .join("");

  const numFmtsXml = formatosNumero.length
    ? `<numFmts count="${formatosNumero.length}">${formatosNumero.map(({ id, formato }) => `<numFmt numFmtId="${id}" formatCode="${escaparXml(formato)}"/>`).join("")}</numFmts>`
    : '<numFmts count="0"/>';

  const xfsXml = xfs
    .map((xf) => {
      const alinhamento = xf.alignment || {};
      const temAlinhamento = alinhamento.horizontal || alinhamento.vertical || alinhamento.wrapText;
      const attrs = [
        `numFmtId="${xf.numFmtId}"`,
        `fontId="${xf.fontId}"`,
        `fillId="${xf.fillId}"`,
        `borderId="${xf.borderId}"`,
        'xfId="0"',
        xf.numFmtId ? 'applyNumberFormat="1"' : "",
        xf.fontId ? 'applyFont="1"' : "",
        xf.fillId ? 'applyFill="1"' : "",
        xf.borderId ? 'applyBorder="1"' : "",
        temAlinhamento ? 'applyAlignment="1"' : "",
      ].filter(Boolean).join(" ");
      if (!temAlinhamento) return `<xf ${attrs}/>`;
      const alignmentAttrs = [
        alinhamento.horizontal ? `horizontal="${escaparXml(alinhamento.horizontal)}"` : "",
        alinhamento.vertical ? `vertical="${escaparXml(alinhamento.vertical)}"` : "",
        alinhamento.wrapText ? 'wrapText="1"' : "",
      ].filter(Boolean).join(" ");
      return `<xf ${attrs}><alignment ${alignmentAttrs}/></xf>`;
    })
    .join("");

  return `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="${XMLNS}">
  ${numFmtsXml}
  <fonts count="${fonts.length}">${fontsXml}</fonts>
  <fills count="${fills.length}">${fillsXml}</fills>
  <borders count="${borders.length}">${bordersXml}</borders>
  <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
  <cellXfs count="${xfs.length}">${xfsXml}</cellXfs>
  <cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>
  <dxfs count="0"/>
  <tableStyles count="0" defaultTableStyle="TableStyleMedium2" defaultPivotStyle="PivotStyleLight16"/>
</styleSheet>`;
};

const valorCelula = (celula: SpreadsheetCell) => {
  if (styledCell(celula)) {
    return celula.value;
  }
  return celula;
};

const estiloCelula = (celula: SpreadsheetCell, estiloLinha: SpreadsheetStyle | undefined, registrar: StyleRegistry) => {
  if (styledCell(celula)) {
    return celula.__styleIndex ?? registrar(celula.style || estiloLinha || {});
  }
  return registrar(estiloLinha || {});
};

const criarXmlPlanilha = (planilha: SpreadsheetSheet, registrar: StyleRegistry) => {
  const linhas = planilha.rows || [];
  const colunas = planilha.columns || [];
  let ultimaColuna = Math.max(1, colunas.length);
  let ultimaLinha = Math.max(1, linhas.length);

  const rowsXml = linhas.map((linhaOriginal, indiceLinha) => {
    const linha = Array.isArray(linhaOriginal) ? { cells: linhaOriginal } : (linhaOriginal || { cells: [] });
    const cells = linha.cells || [];
    ultimaColuna = Math.max(ultimaColuna, cells.length);
    const rowNumber = indiceLinha + 1;
    const attrsLinha = [`r="${rowNumber}"`];
    if (linha.height) attrsLinha.push(`ht="${Number(linha.height)}" customHeight="1"`);

    const cellsXml = cells.map((celula, indiceColuna) => {
      const valor = valorCelula(celula);
      const styleIndex = estiloCelula(celula, linha.style, registrar);
      const ref = referenciaCelula(rowNumber, indiceColuna + 1);
      const styleAttr = styleIndex ? ` s="${styleIndex}"` : "";

      if (valor === null || valor === undefined || valor === "") {
        return styleIndex ? `<c r="${ref}"${styleAttr} t="inlineStr"><is><t></t></is></c>` : "";
      }
      if (typeof valor === "number" && Number.isFinite(valor)) {
        return `<c r="${ref}"${styleAttr} t="n"><v>${valor}</v></c>`;
      }
      if (typeof valor === "boolean") {
        return `<c r="${ref}"${styleAttr} t="b"><v>${valor ? 1 : 0}</v></c>`;
      }
      return `<c r="${ref}"${styleAttr} t="inlineStr"><is><t xml:space="preserve">${escaparXml(valor)}</t></is></c>`;
    }).join("");

    return `<row ${attrsLinha.join(" ")}>${cellsXml}</row>`;
  }).join("");

  const colunasXml = colunas.length
    ? `<cols>${colunas.map((coluna, indice) => `<col min="${indice + 1}" max="${indice + 1}" width="${Math.max(2, Number(coluna.width || 12))}" customWidth="1"/>`).join("")}</cols>`
    : "";

  const freezeRows = Math.max(0, Number(planilha.freezeRows || 0));
  const freezeColumns = Math.max(0, Number(planilha.freezeColumns || 0));
  const mostrarGrade = planilha.showGridLines !== false;
  const atributosVisao = [`workbookViewId="0"`, mostrarGrade ? "" : `showGridLines="0"`].filter(Boolean).join(" ");
  let sheetViews: string;
  if (freezeRows || freezeColumns) {
    const topLeftCell = `${colunaParaNome(freezeColumns + 1)}${freezeRows + 1}`;
    const activePane = freezeRows && freezeColumns ? "bottomRight" : freezeRows ? "bottomLeft" : "topRight";
    const paneAttrs = [
      freezeColumns ? `xSplit="${freezeColumns}"` : "",
      freezeRows ? `ySplit="${freezeRows}"` : "",
      `topLeftCell="${topLeftCell}"`,
      `activePane="${activePane}"`,
      `state="frozen"`,
    ].filter(Boolean).join(" ");
    sheetViews = `<sheetViews><sheetView ${atributosVisao}><pane ${paneAttrs}/><selection pane="${activePane}" activeCell="${topLeftCell}" sqref="${topLeftCell}"/></sheetView></sheetViews>`;
  } else {
    sheetViews = `<sheetViews><sheetView ${atributosVisao}/></sheetViews>`;
  }

  const merges = (planilha.merges || []).filter(Boolean);
  const mergesXml = merges.length
    ? `<mergeCells count="${merges.length}">${merges.map((ref) => `<mergeCell ref="${escaparXml(ref)}"/>`).join("")}</mergeCells>`
    : "";

  const filtro = planilha.autoFilter ? `<autoFilter ref="${escaparXml(planilha.autoFilter)}"/>` : "";
  const dimension = `A1:${referenciaCelula(ultimaLinha, ultimaColuna)}`;

  const pageSetup = planilha.pageSetup || {};
  const usarFitToPage = pageSetup.fitToWidth !== undefined || pageSetup.fitToHeight !== undefined;
  const sheetPr = usarFitToPage ? '<sheetPr><pageSetUpPr fitToPage="1"/></sheetPr>' : "";
  const printOptions = planilha.printOptions
    ? `<printOptions${planilha.printOptions.horizontalCentered ? ' horizontalCentered="1"' : ''}${planilha.printOptions.verticalCentered ? ' verticalCentered="1"' : ''}${planilha.printOptions.gridLines ? ' gridLines="1"' : ''}/>`
    : "";
  const margens = {
    left: 0.35,
    right: 0.35,
    top: 0.55,
    bottom: 0.55,
    header: 0.2,
    footer: 0.2,
    ...(planilha.pageMargins || {}),
  };
  const pageMargins = `<pageMargins left="${Number(margens.left)}" right="${Number(margens.right)}" top="${Number(margens.top)}" bottom="${Number(margens.bottom)}" header="${Number(margens.header)}" footer="${Number(margens.footer)}"/>`;
  const pageSetupXml = Object.keys(pageSetup).length
    ? `<pageSetup${pageSetup.paperSize ? ` paperSize="${escaparXml(pageSetup.paperSize)}"` : ""}${pageSetup.orientation ? ` orientation="${escaparXml(pageSetup.orientation)}"` : ""}${pageSetup.fitToWidth !== undefined ? ` fitToWidth="${Number(pageSetup.fitToWidth)}"` : ""}${pageSetup.fitToHeight !== undefined ? ` fitToHeight="${Number(pageSetup.fitToHeight)}"` : ""}${pageSetup.scale ? ` scale="${Number(pageSetup.scale)}"` : ""}/>`
    : "";

  return `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="${XMLNS}" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  ${sheetPr}
  <dimension ref="${dimension}"/>
  ${sheetViews}
  <sheetFormatPr defaultRowHeight="${Number(planilha.defaultRowHeight || 15)}"/>
  ${colunasXml}
  <sheetData>${rowsXml}</sheetData>
  ${filtro}
  ${mergesXml}
  ${printOptions}
  ${pageMargins}
  ${pageSetupXml}
</worksheet>`;
};

const criarXmlWorkbook = (planilhas: SpreadsheetSheet[]) => `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="${XMLNS}" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <bookViews><workbookView xWindow="0" yWindow="0" windowWidth="24000" windowHeight="12000"/></bookViews>
  <sheets>${planilhas.map((planilha, indice) => `<sheet name="${escaparXml(planilha.name)}" sheetId="${indice + 1}" r:id="rId${indice + 1}"/>`).join("")}</sheets>
  <calcPr calcId="191029" fullCalcOnLoad="1"/>
</workbook>`;

const criarXmlRelsWorkbook = (planilhas: SpreadsheetSheet[]) => `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  ${planilhas.map((_, indice) => `<Relationship Id="rId${indice + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet${indice + 1}.xml"/>`).join("")}
  <Relationship Id="rId${planilhas.length + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>`;

const criarContentTypes = (planilhas: SpreadsheetSheet[]) => `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
  ${planilhas.map((_, indice) => `<Override PartName="/xl/worksheets/sheet${indice + 1}.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>`).join("")}
  <Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/>
  <Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/>
</Types>`;

const crcTable = (() => {
  const tabela = new Uint32Array(256);
  for (let n = 0; n < 256; n += 1) {
    let c = n;
    for (let k = 0; k < 8; k += 1) c = (c & 1) ? (0xedb88320 ^ (c >>> 1)) : (c >>> 1);
    tabela[n] = c >>> 0;
  }
  return tabela;
})();

const crc32 = (bytes: (Uint8Array<ArrayBufferLike>)) => {
  let crc = 0xffffffff;
  for (let i = 0; i < bytes.length; i += 1) crc = crcTable[(crc ^ bytes[i]) & 0xff] ^ (crc >>> 8);
  return (crc ^ 0xffffffff) >>> 0;
};

const dataDos = (data = new Date()) => {
  const ano = Math.max(1980, data.getFullYear());
  const hora = ((data.getHours() & 0x1f) << 11) | ((data.getMinutes() & 0x3f) << 5) | ((Math.floor(data.getSeconds() / 2)) & 0x1f);
  const dia = (((ano - 1980) & 0x7f) << 9) | (((data.getMonth() + 1) & 0x0f) << 5) | (data.getDate() & 0x1f);
  return { hora, dia };
};

const escreverUint16 = (view: (DataView<ArrayBuffer>), offset: number, valor: (number)) => view.setUint16(offset, valor, true);
const escreverUint32 = (view: (DataView<ArrayBuffer>), offset: number, valor: (number)) => view.setUint32(offset, valor >>> 0, true);

const concatenar = (partes: Uint8Array[]) => {
  const total = partes.reduce((soma, parte) => soma + parte.length, 0);
  const saida = new Uint8Array(total);
  let offset = 0;
  partes.forEach((parte) => {
    saida.set(parte, offset);
    offset += parte.length;
  });
  return saida;
};

const criarZipSemCompressao = (arquivos: Array<{ nome: string; conteudo: string | Uint8Array }>) => {
  const locais: Uint8Array[] = [];
  const centrais: Uint8Array[] = [];
  let offset = 0;
  const { hora, dia } = dataDos();

  arquivos.forEach(({ nome, conteudo }) => {
    const nomeBytes = encoder.encode(nome);
    const dados = conteudo instanceof Uint8Array ? conteudo : encoder.encode(conteudo);
    const crc = crc32(dados);

    const local = new Uint8Array(30 + nomeBytes.length);
    const localView = new DataView(local.buffer);
    escreverUint32(localView, 0, 0x04034b50);
    escreverUint16(localView, 4, 20);
    escreverUint16(localView, 6, 0x0800);
    escreverUint16(localView, 8, 0);
    escreverUint16(localView, 10, hora);
    escreverUint16(localView, 12, dia);
    escreverUint32(localView, 14, crc);
    escreverUint32(localView, 18, dados.length);
    escreverUint32(localView, 22, dados.length);
    escreverUint16(localView, 26, nomeBytes.length);
    escreverUint16(localView, 28, 0);
    local.set(nomeBytes, 30);
    locais.push(local, dados);

    const central = new Uint8Array(46 + nomeBytes.length);
    const centralView = new DataView(central.buffer);
    escreverUint32(centralView, 0, 0x02014b50);
    escreverUint16(centralView, 4, 20);
    escreverUint16(centralView, 6, 20);
    escreverUint16(centralView, 8, 0x0800);
    escreverUint16(centralView, 10, 0);
    escreverUint16(centralView, 12, hora);
    escreverUint16(centralView, 14, dia);
    escreverUint32(centralView, 16, crc);
    escreverUint32(centralView, 20, dados.length);
    escreverUint32(centralView, 24, dados.length);
    escreverUint16(centralView, 28, nomeBytes.length);
    escreverUint16(centralView, 30, 0);
    escreverUint16(centralView, 32, 0);
    escreverUint16(centralView, 34, 0);
    escreverUint16(centralView, 36, 0);
    escreverUint32(centralView, 38, 0);
    escreverUint32(centralView, 42, offset);
    central.set(nomeBytes, 46);
    centrais.push(central);

    offset += local.length + dados.length;
  });

  const tamanhoCentral = centrais.reduce((soma, item) => soma + item.length, 0);
  const fim = new Uint8Array(22);
  const fimView = new DataView(fim.buffer);
  escreverUint32(fimView, 0, 0x06054b50);
  escreverUint16(fimView, 4, 0);
  escreverUint16(fimView, 6, 0);
  escreverUint16(fimView, 8, arquivos.length);
  escreverUint16(fimView, 10, arquivos.length);
  escreverUint32(fimView, 12, tamanhoCentral);
  escreverUint32(fimView, 16, offset);
  escreverUint16(fimView, 20, 0);

  return concatenar([...locais, ...centrais, fim]);
};

export const colunaXlsx = colunaParaNome;
export const celulaXlsx = (value: SpreadsheetValue, style: SpreadsheetStyle = {}) => ({ value, style });

export const criarArquivoXlsx = ({ creator = "SGT-GTRAT", created = new Date(), sheets = [] }: SpreadsheetWorkbook) => {
  if (!sheets.length) throw new Error("Ao menos uma planilha é obrigatória.");

  const planilhas = sheets.map((planilha, indice) => ({
    ...planilha,
    name: normalizarNomePlanilha(planilha.name, indice),
  }));
  const { estilos, registrar } = criarRegistroEstilos(planilhas);
  const dataCriacao = created instanceof Date ? created : new Date(created);
  const iso = Number.isNaN(dataCriacao.getTime()) ? new Date().toISOString() : dataCriacao.toISOString();

  const arquivos = [
    { nome: "[Content_Types].xml", conteudo: criarContentTypes(planilhas) },
    { nome: "_rels/.rels", conteudo: `<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/><Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/><Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" Target="docProps/app.xml"/></Relationships>` },
    { nome: "docProps/core.xml", conteudo: `<?xml version="1.0" encoding="UTF-8" standalone="yes"?><cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dcterms="http://purl.org/dc/terms/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"><dc:creator>${escaparXml(creator)}</dc:creator><cp:lastModifiedBy>${escaparXml(creator)}</cp:lastModifiedBy><dcterms:created xsi:type="dcterms:W3CDTF">${iso}</dcterms:created><dcterms:modified xsi:type="dcterms:W3CDTF">${iso}</dcterms:modified></cp:coreProperties>` },
    { nome: "docProps/app.xml", conteudo: `<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties" xmlns:vt="http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes"><Application>SGT-GTRAT</Application><AppVersion>1.0</AppVersion><TitlesOfParts><vt:vector size="${planilhas.length}" baseType="lpstr">${planilhas.map((p) => `<vt:lpstr>${escaparXml(p.name)}</vt:lpstr>`).join("")}</vt:vector></TitlesOfParts></Properties>` },
    { nome: "xl/workbook.xml", conteudo: criarXmlWorkbook(planilhas) },
    { nome: "xl/_rels/workbook.xml.rels", conteudo: criarXmlRelsWorkbook(planilhas) },
    { nome: "xl/styles.xml", conteudo: criarXmlEstilos(estilos) },
    ...planilhas.map((planilha, indice) => ({ nome: `xl/worksheets/sheet${indice + 1}.xml`, conteudo: criarXmlPlanilha(planilha, registrar) })),
  ];

  return criarZipSemCompressao(arquivos);
};

export const baixarArquivoXlsx = (configuracao: SpreadsheetWorkbook, nomeArquivo: (string)) => {
  const bytes = criarArquivoXlsx(configuracao);
  const blob = new Blob([bytes], { type: XLSX_MIME });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = nomeArquivo.endsWith(".xlsx") ? nomeArquivo : `${nomeArquivo}.xlsx`;
  link.style.display = "none";
  document.body.appendChild(link);
  link.click();
  link.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
};
