import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const raiz = new URL("../", import.meta.url);
const ler = (caminho) => readFile(new URL(caminho, raiz), "utf8");

test("filtro de contrato envia ids em formato estável para o Spring", async () => {
  const servico = await ler("src/services/rh/ServicoEmpregado.js");

  assert.match(servico, /key === 'contratoIds'/);
  assert.match(servico, /value\.map\(String\)\.filter\(Boolean\)\.join\(','\)/);
});

test("navegação de contratos seleciona somente um contrato por vez", async () => {
  const logica = await ler("src/modules/rh/pages/EmpregadosLista/useEmpregadosLista.js");

  assert.match(logica, /anteriores\.length === 1 && anteriores\[0\] === normalizado/);
  assert.match(logica, /:\s*\[normalizado\]/);
  assert.doesNotMatch(logica, /:\s*\[\.\.\.anteriores, normalizado\]/);
});

test("resultado filtrado pelo servidor não é escondido por um segundo filtro local", async () => {
  const logica = await ler("src/modules/rh/pages/EmpregadosLista/useEmpregadosLista.js");

  assert.match(logica, /const gruposParaRenderizar = gruposBase/);
  assert.doesNotMatch(logica, /gruposBase\.filter\(\(grupo\) => selecionados/);
});
