import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const ler = (caminho) => readFile(new URL(`../${caminho}`, import.meta.url), "utf8");

test("painel usa cálculo assíncrono persistido em vez do endpoint monolítico", async () => {
  const hook = await ler("src/modules/painel/pages/index.js");
  const service = await ler("src/services/painel/ServicoPainel.js");
  assert.match(hook, /Painel\.solicitarCalculo/);
  assert.match(hook, /Painel\.consultarCalculo/);
  assert.doesNotMatch(hook, /Promise\.all\(\[/);
  assert.match(service, /\/painel\/calculos/);
});

test("painel preserva os dados exibidos durante atualização e mostra progresso", async () => {
  const hook = await ler("src/modules/painel/pages/index.js");
  const page = await ler("src/modules/painel/pages/index.jsx");
  assert.match(hook, /desatualizado/);
  assert.match(page, /barraProcessamento/);
  assert.match(page, /Atualizando painel em segundo plano/);
});

test("ranking pesado é carregado somente sob demanda", async () => {
  const hook = await ler("src/modules/painel/pages/index.js");
  const component = await ler("src/modules/painel/components/FerramentasGestao/index.jsx");
  assert.match(hook, /carregarRanking/);
  assert.match(component, /Carregar ranking/);
  assert.match(component, /Consulta detalhada sob demanda/);
});


test("painel se recupera uma vez de cálculo concluído sem dados", async () => {
  const hook = await ler("src/modules/painel/pages/index.js");
  assert.match(hook, /tentativa < 2/);
  assert.match(hook, /forcar \|\| tentativa > 0/);
  assert.match(hook, /Reconstruindo o painel uma vez/);
  assert.match(hook, /mesmo após a reconstrução automática/);
});
