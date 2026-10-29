import { mkdir, writeFile } from "node:fs/promises";
import path from "node:path";

const PAGE_WIDTH = 595.28;
const PAGE_HEIGHT = 841.89;
const MARGIN_X = 42;
const TOP = 800;
const BOTTOM = 42;
const LINE_HEIGHT = 13;
const MAX_CHARS = 92;

function ascii(texto) {
  return String(texto ?? "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/[–—]/g, "-")
    .replace(/[“”]/g, '"')
    .replace(/[‘’]/g, "'")
    .replace(/[^\x20-\x7E]/g, "?");
}

function escapar(texto) {
  return ascii(texto).replace(/([\\()])/g, "\\$1");
}

function quebrarLinha(texto, limite = MAX_CHARS) {
  const bruto = ascii(texto);
  if (!bruto) return [""];
  const palavras = bruto.split(/\s+/);
  const linhas = [];
  let atual = "";
  for (const palavra of palavras) {
    if (!atual) {
      atual = palavra;
    } else if (`${atual} ${palavra}`.length <= limite) {
      atual += ` ${palavra}`;
    } else {
      linhas.push(atual);
      atual = palavra;
    }
    while (atual.length > limite) {
      linhas.push(atual.slice(0, limite));
      atual = atual.slice(limite);
    }
  }
  if (atual) linhas.push(atual);
  return linhas;
}

function prepararPaginas(secoes) {
  const paginas = [];
  let pagina = [];
  let y = TOP;

  const novaPagina = () => {
    if (pagina.length) paginas.push(pagina);
    pagina = [];
    y = TOP;
  };

  const adicionar = (texto, opcoes = {}) => {
    const tamanho = opcoes.tamanho || 10;
    const negrito = opcoes.negrito === true;
    const recuo = opcoes.recuo || 0;
    const espacoAntes = opcoes.espacoAntes || 0;
    const espacoDepois = opcoes.espacoDepois || 0;
    const alturaLinha = opcoes.alturaLinha || Math.max(LINE_HEIGHT, tamanho + 3);
    y -= espacoAntes;
    const limite = opcoes.limite || Math.max(30, MAX_CHARS - Math.round(recuo / 5));
    for (const linha of quebrarLinha(texto, limite)) {
      if (y < BOTTOM + LINE_HEIGHT) novaPagina();
      pagina.push({ texto: linha, x: MARGIN_X + recuo, y, tamanho, negrito });
      y -= alturaLinha;
    }
    y -= espacoDepois;
  };

  for (const secao of secoes) {
    if (secao.novaPagina) novaPagina();
    adicionar(secao.texto, secao);
  }
  novaPagina();
  return paginas.length ? paginas : [[]];
}

export async function gerarPdfTexto({ destino, secoes, titulo = "Relatorio" }) {
  await mkdir(path.dirname(destino), { recursive: true });
  const paginas = prepararPaginas(secoes);
  const objetos = [];
  const adicionarObjeto = (conteudo) => {
    objetos.push(conteudo);
    return objetos.length;
  };

  const catalogId = adicionarObjeto("");
  const pagesId = adicionarObjeto("");
  const fontRegularId = adicionarObjeto("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>");
  const fontBoldId = adicionarObjeto("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>");
  const pageIds = [];

  for (const pagina of paginas) {
    const comandos = ["BT"];
    for (const linha of pagina) {
      comandos.push(`/${linha.negrito ? "F2" : "F1"} ${linha.tamanho} Tf`);
      comandos.push(`1 0 0 1 ${linha.x.toFixed(2)} ${linha.y.toFixed(2)} Tm`);
      comandos.push(`(${escapar(linha.texto)}) Tj`);
    }
    comandos.push("ET");
    const stream = comandos.join("\n");
    const contentId = adicionarObjeto(`<< /Length ${Buffer.byteLength(stream, "ascii")} >>\nstream\n${stream}\nendstream`);
    const pageId = adicionarObjeto(
      `<< /Type /Page /Parent ${pagesId} 0 R /MediaBox [0 0 ${PAGE_WIDTH} ${PAGE_HEIGHT}] ` +
      `/Resources << /Font << /F1 ${fontRegularId} 0 R /F2 ${fontBoldId} 0 R >> >> ` +
      `/Contents ${contentId} 0 R >>`,
    );
    pageIds.push(pageId);
  }

  objetos[catalogId - 1] = `<< /Type /Catalog /Pages ${pagesId} 0 R >>`;
  objetos[pagesId - 1] = `<< /Type /Pages /Count ${pageIds.length} /Kids [${pageIds.map((id) => `${id} 0 R`).join(" ")}] >>`;

  let pdf = "%PDF-1.4\n%SIG-GTRAT\n";
  const offsets = [0];
  objetos.forEach((objeto, indice) => {
    offsets.push(Buffer.byteLength(pdf, "ascii"));
    pdf += `${indice + 1} 0 obj\n${objeto}\nendobj\n`;
  });
  const xref = Buffer.byteLength(pdf, "ascii");
  pdf += `xref\n0 ${objetos.length + 1}\n`;
  pdf += "0000000000 65535 f \n";
  for (let i = 1; i < offsets.length; i += 1) {
    pdf += `${String(offsets[i]).padStart(10, "0")} 00000 n \n`;
  }
  pdf += `trailer\n<< /Size ${objetos.length + 1} /Root ${catalogId} 0 R /Info << /Title (${escapar(titulo)}) >> >>\n`;
  pdf += `startxref\n${xref}\n%%EOF\n`;
  await writeFile(destino, pdf, "ascii");
}
