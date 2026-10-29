import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const ler = (caminho) => readFile(new URL(`../${caminho}`, import.meta.url), "utf8");

test("painel oferece exportações Excel e PDF separadas com ícones compatíveis", async () => {
  const [pagina, executivo] = await Promise.all([
    ler("src/modules/painel/pages/index.jsx"),
    ler("src/modules/painel/components/GestaoExecutiva/index.jsx"),
  ]);
  assert.match(pagina, /exportarPainelGerencial, exportarPainelPdf/);
  assert.match(pagina, /aoExportarXlsx=/);
  assert.match(pagina, /aoExportarPdf=/);
  assert.match(executivo, /FaFileExcel/);
  assert.match(executivo, /BiLogoAdobe/);
  assert.match(executivo, />Excel</);
  assert.match(executivo, />PDF</);
});

test("XLSX do painel preserva semântica de ausência de base e inclui análise cadastral", async () => {
  const exportacao = await ler("src/modules/painel/exportacao.js");
  assert.match(exportacao, /criarConfiguracaoPainelXlsx/);
  assert.match(exportacao, /ANÁLISE CADASTRAL REAL/);
  assert.match(exportacao, /ADMISSÕES MENSAIS/);
  assert.match(exportacao, /Não aplicável/);
  assert.match(exportacao, /Sem lançamentos/);
  assert.match(exportacao, /analiseCadastro/);
  assert.match(exportacao, /admissoesMensais/);
});

test("PDF do painel usa a mesma base real e possui páginas executivas", async () => {
  const exportacao = await ler("src/modules/painel/exportacao.js");
  assert.match(exportacao, /export const exportarPainelPdf = async/);
  assert.match(exportacao, /import\("jspdf"\)/);
  assert.match(exportacao, /import\("jspdf-autotable"\)/);
  assert.match(exportacao, /Painel Gerencial - Resumo Executivo/);
  assert.match(exportacao, /Painel Gerencial - Operação e Contratos/);
  assert.match(exportacao, /Painel Gerencial - Unidades e Riscos/);
  assert.match(exportacao, /Análise cadastral real/);
});

test("acabamento visual do painel é responsivo e não depende de marcadores de versão", async () => {
  const [executivoCss, paginaCss, ferramentasCss, indicadorCss] = await Promise.all([
    ler("src/modules/painel/components/GestaoExecutiva/index.module.css"),
    ler("src/modules/painel/pages/index.module.css"),
    ler("src/modules/painel/components/FerramentasGestao/index.module.css"),
    ler("src/modules/painel/components/IndicadorEfetivo/index.module.css"),
  ]);
  assert.match(executivoCss, /\.botaoExcel/);
  assert.match(executivoCss, /\.botaoPdf/);
  assert.match(executivoCss, /@media \(max-width: 720px\)/);
  assert.match(paginaCss, /@media \(max-width: 820px\)/);
  assert.match(ferramentasCss, /@media/);
  assert.match(indicadorCss, /\.metricas/);
  assert.match(indicadorCss, /\.statusOperacional/);
  assert.match(indicadorCss, /\.chamadaLeitura/);
  assert.match(indicadorCss, /\.modalLeituraOperacional/);
  assert.match(indicadorCss, /@media \(max-width: 760px\)/);
});

test("CTA Ver análise completa possui acabamento próprio nos dois tipos de card", async () => {
  const [executivoCss, ferramentasCss] = await Promise.all([
    ler("src/modules/painel/components/GestaoExecutiva/index.module.css"),
    ler("src/modules/painel/components/FerramentasGestao/index.module.css"),
  ]);
  for (const css of [executivoCss, ferramentasCss]) {
    assert.match(css, /\.abrirAnalise\s*\{[\s\S]*display:\s*inline-flex/);
    assert.match(css, /\.abrirAnalise\s*\{[\s\S]*border-top:\s*1px solid var\(--cor-correios-azul-100\)/);
    assert.match(css, /\.abrirAnalise:hover[\s\S]*--cor-correios-amarelo-100/);
    assert.match(css, /\.abrirAnalise:focus-visible/);
  }
});
