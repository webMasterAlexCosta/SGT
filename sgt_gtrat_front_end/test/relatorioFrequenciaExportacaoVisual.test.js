import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { pathToFileURL } from "node:url";
import { resolve } from "node:path";
import { criarArquivoXlsx } from "../src/core/export/xlsx.js";

const ler = (caminho) => readFile(new URL(`../${caminho}`, import.meta.url), "utf8");
const backendRoot = process.env.SGT_BACKEND_DIR
  ? pathToFileURL(`${resolve(process.env.SGT_BACKEND_DIR)}/`)
  : new URL("../../backend/", import.meta.url);
const lerBackend = (caminho) => readFile(new URL(caminho, backendRoot), "utf8");

test("relatório de frequência usa ícones semânticos de planilha e PDF", async () => {
  const cabecalho = await ler("src/modules/rh/pages/PaginaRelatorioFrequencia/components/CabecalhoRelatorio/index.jsx");
  assert.match(cabecalho, /FileSpreadsheet/);
  assert.match(cabecalho, /FileText/);
  assert.match(cabecalho, /Opções de Exportação/);
  assert.match(cabecalho, />Planilha</);
  assert.match(cabecalho, />PDF</);
});

test("relatório de frequência melhora leitura visual com lotação fixa e métricas do período", async () => {
  const [tabela, estilos] = await Promise.all([
    ler("src/modules/rh/pages/PaginaRelatorioFrequencia/components/TabelaRelatorio/index.jsx"),
    ler("src/modules/rh/pages/PaginaRelatorioFrequencia/components/TabelaRelatorio/index.module.css"),
  ]);
  assert.match(tabela, /cabLotacao/);
  assert.match(tabela, /diasDoMes\.length/);
  assert.match(estilos, /\.cabLotacao/);
  assert.match(estilos, /position:\s*sticky/);
});

test("exportação XLSX de frequência é streaming, congela linhas/colunas e configura impressão", async () => {
  const [front, java, writer] = await Promise.all([
    ler("src/modules/rh/pages/PaginaRelatorioFrequencia/exportacao.js"),
    lerBackend("src/main/java/com/example/siggtrat/servico/RelatorioExportacaoServico.java"),
    lerBackend("src/main/java/com/example/siggtrat/utilitario/XlsxStreamingWriter.java"),
  ]);
  assert.match(front, /iniciarDownloadNativo/);
  const blocoFrequencia = java.match(/private void gerarFrequenciaXlsxNoArquivo[\s\S]*?private void gerarAbsenteismoXlsxNoArquivo/)?.[0] ?? "";
  assert.match(blocoFrequencia, /new XlsxStreamingWriter\(\s*output,[\s\S]*?5, 5, larguras\)/);
  assert.match(blocoFrequencia, /TOTAL GERAL/);
  assert.match(writer, /<pane/);
  assert.match(writer, /orientation=\\"landscape\\"/);
  assert.match(writer, /fitToWidth=\\"1\\"/);
  assert.match(writer, /showGridLines=\\"0\\"/);
});

test("gerador XLSX suporta congelamento bidimensional e pageSetup", () => {
  const bytes = criarArquivoXlsx({
    sheets: [{
      name: "Teste",
      rows: [{ cells: ["A", "B"] }, { cells: [1, 2] }],
      freezeRows: 1,
      freezeColumns: 1,
      showGridLines: false,
      pageSetup: { paperSize: 9, orientation: "landscape", fitToWidth: 1, fitToHeight: 0 },
    }],
  });
  const texto = new TextDecoder().decode(bytes);
  assert.match(texto, /xSplit="1"/);
  assert.match(texto, /ySplit="1"/);
  assert.match(texto, /topLeftCell="B2"/);
  assert.match(texto, /showGridLines="0"/);
  assert.match(texto, /orientation="landscape"/);
  assert.match(texto, /fitToWidth="1"/);
});

test("PDF de frequência é gerado no servidor em A3 para períodos longos e por blocos", async () => {
  const [front, java] = await Promise.all([
    ler("src/modules/rh/pages/PaginaRelatorioFrequencia/exportacao.js"),
    lerBackend("src/main/java/com/example/siggtrat/servico/RelatorioExportacaoServico.java"),
  ]);
  assert.match(front, /obterUrlExportacaoFrequencia/);
  assert.match(java, /dias\.size\(\) > 18 \? PageSize\.A3\.rotate\(\) : PageSize\.A4\.rotate\(\)/);
  assert.match(java, /final int porTabela = 80/);
  assert.match(java, /setHeaderRows\(1\)/);
  assert.match(java, /Regra noturna urbana/);
  assert.match(java, /adicionarLegendaFrequenciaPdf/);
  assert.match(java, /LEGENDA_FREQUENCIA/);
  assert.match(java, /status\.fundo\(\)/);
  assert.match(java, /setWidths\(largurasFrequenciaPdf/);
  assert.match(java, /EventoPaginaFrequenciaPdf/);
  assert.doesNotMatch(java.match(/private void gerarFrequenciaPdfNoArquivo[\s\S]*?private void gerarAbsenteismoPdfNoArquivo/)?.[0] ?? "", /Persist[eê]ncia tempor[aá]ria|Processamento em lotes|Base completa/);
});

test("nome do arquivo de frequência inclui lotação, turno, data e hora", async () => {
  const java = await lerBackend("src/main/java/com/example/siggtrat/servico/RelatorioExportacaoServico.java");
  assert.match(java, /Relatorio_Frequencia_/);
  assert.match(java, /segmentoNomeArquivo\(seguro\.lotacao\(\)\)/);
  assert.match(java, /segmentoNomeArquivo\(seguro\.turno\(\)\)/);
  assert.match(java, /yyyy-MM-dd_HH-mm-ss/);
});

test("XLSX de frequência usa legenda e cores de status sem expor detalhes internos", async () => {
  const [java, writer] = await Promise.all([
    lerBackend("src/main/java/com/example/siggtrat/servico/RelatorioExportacaoServico.java"),
    lerBackend("src/main/java/com/example/siggtrat/utilitario/XlsxStreamingWriter.java"),
  ]);
  assert.match(java, /adicionarLegendaFrequenciaXlsx/);
  for (const status of ["P", "PP", "F", "AT", "FER", "DP", "RT", "D", "SUSP", "ADV", "FR"]) {
    assert.match(writer, new RegExp(`STYLE_STATUS_${status}`));
  }
  const bloco = java.match(/private void gerarFrequenciaXlsxNoArquivo[\s\S]*?private void gerarAbsenteismoXlsxNoArquivo/)?.[0] ?? "";
  assert.doesNotMatch(bloco, /Persist[eê]ncia tempor[aá]ria|Processamento em lotes|Base completa conforme/);
});
