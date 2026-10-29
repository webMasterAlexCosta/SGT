import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { pathToFileURL } from "node:url";
import { resolve } from "node:path";

const ler = (caminho) => readFile(new URL(`../${caminho}`, import.meta.url), "utf8");
const backendRoot = process.env.SGT_BACKEND_DIR
  ? pathToFileURL(`${resolve(process.env.SGT_BACKEND_DIR)}/`)
  : new URL("../../backend/", import.meta.url);
const lerBackend = (caminho) => readFile(new URL(caminho, backendRoot), "utf8");

test("visão consolidada usa ícones de Excel e Adobe no job oficial do Java", async () => {
  const cabecalho = await ler("src/modules/rh/pages/PaginaRelatoriosTerceirizados/components/CabecalhoRelatorios/index.jsx");
  assert.match(cabecalho, /FaFileExcel/);
  assert.match(cabecalho, /BiLogoAdobe/);
  assert.match(cabecalho, /Resultado filtrado completo/);
  assert.match(cabecalho, /aoExportarExcel/);
  assert.match(cabecalho, /aoExportarPdf/);
  assert.doesNotMatch(cabecalho, /Página atual|Todas as páginas/);
  assert.doesNotMatch(cabecalho, /mostrarMenuExportacao|menuExportacao/);
});

test("geração local antiga de terceiros foi removida do fluxo produtivo", async () => {
  const pagina = await ler("src/modules/rh/pages/PaginaRelatoriosTerceirizados/index.jsx");
  assert.doesNotMatch(pagina, /from "\.\/exportacao"/);
  assert.doesNotMatch(pagina, /exportarRelatorioExcel|exportarRelatorioPdf|jsPDF|gerarXlsx/);
  assert.match(pagina, /executarExportacaoOficial/);
});

test("absenteísmo usa botões diretos de Excel e PDF sem menu intermediário", async () => {
  const pagina = await ler("src/modules/rh/pages/PaginaRelatorioAbsenteismo/index.jsx");
  assert.match(pagina, /FaFileExcel/);
  assert.match(pagina, /BiLogoAdobe/);
  assert.match(pagina, /botaoExcelDireto/);
  assert.match(pagina, /botaoPdfDireto/);
  assert.doesNotMatch(pagina, /mostrarMenuExportacao|referenciaMenuExportacao/);
});

test("absenteísmo exporta a base completa no servidor em XLSX e PDF por lotes", async () => {
  const [pagina, servico, java, writer] = await Promise.all([
    ler("src/modules/rh/pages/PaginaRelatorioAbsenteismo/index.jsx"),
    ler("src/services/rh/ServicoAbsenteismo.js"),
    lerBackend("src/main/java/com/example/siggtrat/servico/RelatorioExportacaoServico.java"),
    lerBackend("src/main/java/com/example/siggtrat/utilitario/XlsxStreamingWriter.java"),
  ]);
  assert.match(pagina, /useExportacaoRelatorio/);
  assert.match(pagina, /tipo:\s*"absenteismo"/);
  assert.match(pagina, /totalEstimado:\s*paginacao\.totalColaboradores/);
  assert.match(servico, /obterUrlExportacao/);
  assert.match(java, /exportarAbsenteismoXlsx/);
  assert.match(java, /Cell\.number\(item\.taxaIndividual\(\), estiloTaxaAbsenteismoXlsx/);
  assert.match(java, /final int porTabela = 100/);
  assert.match(java, /EventoPaginaAbsenteismoPdf/);
  assert.match(java, /Legenda e critérios/);
  assert.match(java, /ÍNDICE DISRUPÇÃO/);
  assert.doesNotMatch(java, /DISRUPÇÃO \(\%\)/);
  assert.match(java, /EPISÓDIOS/);
  assert.match(java, /nomeArquivoAbsenteismo/);
  assert.match(java, /segmentoNomeArquivo\(seguro\.lotacao\(\)\)/);
  assert.match(java, /segmentoNomeArquivo\(seguro\.turno\(\)\)/);
  assert.match(java, /DENTRO DA META|ACIMA DA META/);
  assert.match(writer, /STYLE_ABS_PERCENT_OK/);
  assert.match(writer, /STYLE_ABS_DISRUPCAO_CRITICA/);
  assert.match(writer, /showGridLines=\\"0\\"/);
});
