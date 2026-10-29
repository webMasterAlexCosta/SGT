import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const ler = (caminho) => readFile(new URL(`../${caminho}`, import.meta.url), "utf8");

test("painel V5 usa grade bento com hierarquia de tamanhos", async () => {
  const [pagina, estilos] = await Promise.all([
    ler("src/modules/painel/pages/index.jsx"),
    ler("src/modules/painel/pages/index.module.css"),
  ]);
  assert.match(pagina, /gradeBento/);
  assert.match(pagina, /tamanho="duplo"/);
  assert.match(pagina, /tamanho="triplo"/);
  assert.match(estilos, /grid-template-columns:\s*repeat\(3/);
  assert.match(estilos, /grid-auto-flow:\s*dense/);
});

test("cards resumem conteúdo e oferecem análise completa em modal", async () => {
  const [executivo, ferramentas] = await Promise.all([
    ler("src/modules/painel/components/GestaoExecutiva/index.jsx"),
    ler("src/modules/painel/components/FerramentasGestao/index.jsx"),
  ]);
  assert.match(executivo, /previewPainel/);
  assert.match(executivo, /Ver análise completa/);
  assert.match(ferramentas, /previewCartao/);
  assert.match(ferramentas, /Ver análise completa/);
});

test("modal V5 possui escape, foco contido e modo tela cheia", async () => {
  const modal = await ler("src/modules/painel/components/ModalPainel/index.jsx");
  assert.match(modal, /evento\.key === "Escape"/);
  assert.match(modal, /evento\.key !== "Tab"/);
  assert.match(modal, /setTelaCheia/);
  assert.match(modal, /focoAnteriorRef/);
});

test("radar executivo reutiliza os dados já consolidados sem nova chamada de serviço", async () => {
  const pagina = await ler("src/modules/painel/pages/index.jsx");
  assert.match(pagina, /RadarExecutivo/);
  assert.match(pagina, /dados\?\.frequencia\?\.pendentes/);
  assert.match(pagina, /dados\?\.contratos\?\.vencendoEm30Dias/);
  assert.doesNotMatch(pagina, /requisitar\(|fetch\(|Painel\./);
});
