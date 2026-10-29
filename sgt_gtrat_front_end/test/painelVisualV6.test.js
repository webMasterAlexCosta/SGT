import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { criarArquivoXlsx, celulaXlsx } from "../src/core/export/xlsx.js";

const ler = (caminho) => readFile(new URL(`../${caminho}`, import.meta.url), "utf8");

test("painel V6 usa azul e amarelo institucionais sem misturar famílias no mesmo degradê", async () => {
  const [paginaCss, executivoCss, ferramentasCss, modalCss] = await Promise.all([
    ler("src/modules/painel/pages/index.module.css"),
    ler("src/modules/painel/components/GestaoExecutiva/index.module.css"),
    ler("src/modules/painel/components/FerramentasGestao/index.module.css"),
    ler("src/modules/painel/components/ModalPainel/index.module.css"),
  ]);
  for (const css of [paginaCss, executivoCss, ferramentasCss, modalCss]) {
    assert.match(css, /--cor-correios-azul-/);
    assert.match(css, /--cor-correios-amarelo-/);
    const gradients = css.match(/linear-gradient\([^;]+?\)/gs) || [];
    for (const gradient of gradients) {
      assert.equal(gradient.includes("azul") && gradient.includes("amarelo"), false);
    }
  }
  assert.match(executivoCss, /linear-gradient\(90deg, var\(--cor-correios-azul-800\), var\(--cor-correios-azul-500\)\)/);
  assert.match(ferramentasCss, /linear-gradient\(90deg, var\(--cor-correios-azul-800\), var\(--cor-correios-azul-500\)\)/);
});

test("painel V6 possui breakpoints para desktop, tablet e celular", async () => {
  const [paginaCss, executivoCss, ferramentasCss] = await Promise.all([
    ler("src/modules/painel/pages/index.module.css"),
    ler("src/modules/painel/components/GestaoExecutiva/index.module.css"),
    ler("src/modules/painel/components/FerramentasGestao/index.module.css"),
  ]);
  assert.match(paginaCss, /@media \(max-width: 1280px\)/);
  assert.match(paginaCss, /@media \(max-width: 820px\)/);
  assert.match(paginaCss, /@media \(max-width: 620px\)/);
  assert.match(executivoCss, /@media \(max-width: 1120px\)/);
  assert.match(executivoCss, /@media \(max-width: 680px\)/);
  assert.match(ferramentasCss, /@media \(max-width: 720px\)/);
});

test("XLSX gerencial V6 contém oito visões executivas", async () => {
  const exportacao = await ler("src/modules/painel/exportacao.js");
  for (const nome of [
    "Dashboard executivo",
    "Efetivo e contratos",
    "Frequência e absenteísmo",
    "Evolução operacional",
    "Unidades e riscos",
    "Riscos e alertas",
    "Integridade e controles",
    "Metodologia",
  ]) assert.match(exportacao, new RegExp(nome));
  assert.match(exportacao, /SEMÁFORO EXECUTIVO/);
  assert.match(exportacao, /Ação sugerida/);
  assert.match(exportacao, /CRITÉRIO|Critérios|critérios/);
});

test("gerador XLSX grava formatos numéricos personalizados no styles.xml", () => {
  const bytes = criarArquivoXlsx({
    sheets: [{
      name: "Teste",
      rows: [{ cells: [celulaXlsx(0.875, { numberFormat: "0.0%" })] }],
    }],
  });
  const texto = new TextDecoder().decode(bytes);
  assert.match(texto, /formatCode="0\.0%"/);
  assert.match(texto, /applyNumberFormat="1"/);
});
