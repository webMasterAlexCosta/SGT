import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const caminhoLogica = new URL(
  "../src/modules/rh/pages/EmpregadosLista/components/FormularioEmpregado/useLogicaFormulario.js",
  import.meta.url,
);
const caminhoSecoes = new URL(
  "../src/modules/rh/pages/EmpregadosLista/components/FormularioEmpregado/components/SecoesFormulario.jsx",
  import.meta.url,
);

test("matrícula do empregado é opcional e aceita até 30 caracteres", async () => {
  const [logica, secoes] = await Promise.all([
    readFile(caminhoLogica, "utf8"),
    readFile(caminhoSecoes, "utf8"),
  ]);

  assert.match(
    logica,
    /dadosFormulario\.matricula\?\.trim\(\)\.length\s*>\s*30/,
    "O formulário deve rejeitar somente valores acima de 30 caracteres.",
  );
  assert.match(
    logica,
    /matricula:\s*dadosFormulario\.matricula\?\.trim\(\)\s*\|\|\s*null/,
    "Matrícula vazia deve continuar sendo enviada como null.",
  );
  assert.doesNotMatch(
    logica,
    /formatarMatricula|matriculaEstaCompleta/,
    "O cadastro de colaborador não deve aplicar a máscara numérica de oito dígitos usada em outros componentes.",
  );

  const campoMatricula = secoes.match(/<CampoEntrada label="Matrícula \(opcional\)"[^>]*\/>/)?.[0];
  assert.ok(campoMatricula, "O campo de matrícula opcional deve existir.");
  assert.match(campoMatricula, /maxLength=\{30\}/);
  assert.doesNotMatch(campoMatricula, /\srequired(?:\s|=|\/?>)/);
});
