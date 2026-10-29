import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const ler = (caminho) => readFile(new URL(`../${caminho}`, import.meta.url), "utf8");
const lerOpcional = (caminho) => ler(caminho).catch((erro) => {
  if (erro?.code === "ENOENT") return "";
  throw erro;
});

test("frontend não mantém janelas retroativas/prazo de edição/dias de ocorrência como autoridade local", async () => {
  const [config, frequencia, ocorrencias, env, envDev, envExemplo] = await Promise.all([
    ler("src/core/config.js"),
    ler("src/modules/rh/pages/PaginaFrequencia/useLogicaFrequencia.js"),
    ler("src/modules/rh/pages/PaginaOcorrencias/index.jsx"),
    lerOpcional(".env"),
    lerOpcional(".env.development"),
    ler(".env.example"),
  ]);

  const conjunto = [config, frequencia, ocorrencias, env, envDev, envExemplo].join("\n");
  assert.doesNotMatch(conjunto, /VITE_FREQUENCIA_LIMIT_(?:ADMIN|UNLOCKED|DEFAULT)_DAYS/);
  assert.doesNotMatch(conjunto, /VITE_FREQUENCIA_EDIT_WINDOW_HOURS/);
  assert.doesNotMatch(conjunto, /VITE_OCORRENCIA_MAX_DIAS/);
  assert.doesNotMatch(frequencia, /prazoEdicaoEstimado/);
  assert.match(frequencia, /Nunca inventar prazo de edição no navegador/);
  assert.match(frequencia, /iniciarProcessoDeCargaDaFrequencia\(\)/);
});

test("cadastro de turno mantém validações estruturais, mas não bloqueia por limite regulamentar VITE", async () => {
  const turnos = await ler("src/modules/admin/pages/PaginaTurnos/index.jsx");
  const trechoValidacao = turnos.slice(
    turnos.indexOf("const aplicarRegrasDeValidacaoAoFormulario"),
    turnos.indexOf("const enviarRotinaProcessadaAoBackend"),
  );

  assert.match(trechoValidacao, /semHorario/);
  assert.match(trechoValidacao, /duração efetiva é igual a zero/);
  assert.doesNotMatch(trechoValidacao, /JORNADA_REFERENCIA_HORAS/);
  assert.doesNotMatch(trechoValidacao, /JORNADA_LIMIAR_INFERIOR/);
  assert.match(trechoValidacao, /API é a fonte autoritativa/);
});
