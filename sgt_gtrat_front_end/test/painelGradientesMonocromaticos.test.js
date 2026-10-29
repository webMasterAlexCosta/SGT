import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const arquivos = [
  "src/modules/painel/pages/index.module.css",
  "src/modules/painel/components/GestaoExecutiva/index.module.css",
  "src/modules/painel/components/FerramentasGestao/index.module.css",
  "src/modules/painel/components/ModalPainel/index.module.css",
];

const ler = (caminho) => readFile(new URL(`../${caminho}`, import.meta.url), "utf8");

test("painel não usa mais degradês decorativos radiais ou mistura azul, amarelo e verde", async () => {
  for (const arquivo of arquivos) {
    const css = await ler(arquivo);
    assert.doesNotMatch(css, /radial-gradient|conic-gradient/);

    const gradients = css.match(/linear-gradient\([^;]+?\)/gs) || [];
    for (const gradient of gradients) {
      assert.equal(
        gradient.includes("azul") && gradient.includes("amarelo"),
        false,
        `${arquivo} contém gradiente misturando azul e amarelo: ${gradient}`,
      );
      assert.doesNotMatch(gradient, /#005baa[^)]*#ffd300|#ffd300[^)]*#005baa/i);
      assert.doesNotMatch(gradient, /#004b8d[^)]*#0d7d40|#0d7d40[^)]*#004b8d/i);
    }
  }
});

test("cabeçalhos do painel usam azul Correios para azul Correios", async () => {
  const [executivo, modal] = await Promise.all([
    ler("src/modules/painel/components/GestaoExecutiva/index.module.css"),
    ler("src/modules/painel/components/ModalPainel/index.module.css"),
  ]);
  const esperado = /linear-gradient\(125deg, var\(--cor-correios-azul-950\), var\(--cor-correios-azul-700\)\)/;
  assert.match(executivo, esperado);
  assert.match(modal, esperado);
});
