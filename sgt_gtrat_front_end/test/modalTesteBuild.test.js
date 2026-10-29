import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

test("modal de contas de teste é controlado por ambiente e o candidato mantém credenciais desabilitadas", async () => {
  const config = await readFile(new URL("../src/core/config.js", import.meta.url), "utf8");
  const envExemplo = await readFile(new URL("../.env.example", import.meta.url), "utf8");

  assert.doesNotMatch(config, /SHOW_TEST_CREDENTIALS:\s*import\.meta\.env\.DEV/);
  assert.match(config, /VITE_SHOW_TEST_CREDENTIALS/);
  assert.match(envExemplo, /^VITE_SHOW_TEST_CREDENTIALS=false$/m);
  assert.match(envExemplo, /^VITE_TEST_GLOBAL_PASSWORD=\s*$/m);
  assert.match(envExemplo, /^VITE_TEST_GESTOR_PASSWORD=\s*$/m);
});

test("modal exibe matrículas e usa os novos nomes das contas do RJ", async () => {
  const modal = await readFile(
    new URL("../src/modules/auth/pages/Login/components/ModalTeste.jsx", import.meta.url),
    "utf8",
  );

  assert.match(modal, /"gestorEstadual"/);
  assert.match(modal, /`gestorLocal\$\{siglaGestaoLocal/);
  assert.match(modal, /"GTRAT"/);
  assert.match(modal, /"GEDIS"/);
  assert.match(modal, /"GETRA"/);
  assert.match(modal, /Matrícula \{formatarMatricula\(matricula\)\}/);
  assert.match(modal, /aoPreencherUsuario\(\{ matricula, senha \}\)/);
  assert.doesNotMatch(modal, /`gestorse\$\{/);
  assert.doesNotMatch(modal, /`gestorgtrat\$\{/i);
});
