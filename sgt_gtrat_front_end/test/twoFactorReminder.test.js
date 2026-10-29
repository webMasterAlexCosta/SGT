import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import {
  calcularProximoLembrete2FA,
  criarChaveLembrete2FA,
  deveExibirLembrete2FA,
  lerProximoLembrete2FA,
  salvarProximoLembrete2FA,
  limparLembrete2FA,
  TWO_FACTOR_REMINDER_INTERVAL_MS,
} from "../src/core/security/twoFactorReminder.js";

const ler = (caminho) => readFile(new URL(`../${caminho}`, import.meta.url), "utf8");

test("lembrete 2FA usa janela exata de 7 dias", () => {
  const agora = Date.UTC(2026, 7, 17, 12, 0, 0);
  const proximo = calcularProximoLembrete2FA(agora);
  assert.equal(proximo - agora, 7 * 24 * 60 * 60 * 1000);
  assert.equal(proximo - agora, TWO_FACTOR_REMINDER_INTERVAL_MS);
});

test("lembrete só aparece quando o 2FA está desativado e o prazo venceu", () => {
  const agora = 1_000_000;
  assert.equal(deveExibirLembrete2FA({ ativo: true, proximoLembreteEm: 0, agora }), false);
  assert.equal(deveExibirLembrete2FA({ ativo: false, proximoLembreteEm: 0, agora }), true);
  assert.equal(deveExibirLembrete2FA({ ativo: false, proximoLembreteEm: agora + 1, agora }), false);
  assert.equal(deveExibirLembrete2FA({ ativo: false, proximoLembreteEm: agora, agora }), true);
});

test("localStorage do lembrete é isolado por usuário", () => {
  const usuarioA = criarChaveLembrete2FA("SGT-GTRAT", { id: 10, matricula: "111" });
  const usuarioB = criarChaveLembrete2FA("SGT-GTRAT", { id: 11, matricula: "111" });
  assert.notEqual(usuarioA, usuarioB);
  assert.match(usuarioA, /sgt-gtrat_2fa_reminder_10/);
});



test("persistência do lembrete tolera storage indisponível e dados inválidos", () => {
  const dados = new Map();
  const storage = {
    getItem: (chave) => dados.get(chave) ?? null,
    setItem: (chave, valor) => dados.set(chave, valor),
    removeItem: (chave) => dados.delete(chave),
  };

  assert.equal(lerProximoLembrete2FA(storage, "x"), 0);
  assert.equal(salvarProximoLembrete2FA(storage, "x", 12345), true);
  assert.equal(lerProximoLembrete2FA(storage, "x"), 12345);
  limparLembrete2FA(storage, "x");
  assert.equal(lerProximoLembrete2FA(storage, "x"), 0);

  const quebrado = {
    getItem: () => { throw new Error("storage bloqueado"); },
    setItem: () => { throw new Error("storage bloqueado"); },
    removeItem: () => { throw new Error("storage bloqueado"); },
  };
  assert.equal(lerProximoLembrete2FA(quebrado, "x"), 0);
  assert.equal(salvarProximoLembrete2FA(quebrado, "x", 1), false);
  assert.doesNotThrow(() => limparLembrete2FA(quebrado, "x"));
});

test("integração consulta status real, navega direto para segurança e abre o modal", async () => {
  const [componente, app, perfil] = await Promise.all([
    ler("src/core/ui/LembreteSegundoFator/index.jsx"),
    ler("src/Aplicacao.jsx"),
    ler("src/modules/perfil/pages/PaginaPerfil/index.jsx"),
  ]);

  assert.match(componente, /ServicoPerfil\.buscarStatusSegundoFator/);
  assert.match(componente, /localStorage/);
  assert.doesNotMatch(componente, /sessionStorage/);
  assert.match(componente, /Lembrar em \{TWO_FACTOR_REMINDER_DAYS\} dias/);
  assert.match(componente, /\/perfil\?security=2fa/);
  assert.match(componente, /Ative a Proteção Avançada/);
  assert.match(app, /<LembreteSegundoFator user=\{user\}/);
  assert.match(perfil, /parametros\.get\("security"\) === "2fa"/);
  assert.match(perfil, /setModalTwoFactorAberto\(true\)/);
  assert.match(perfil, /<ModalSegurancaSegundoFator/);
  assert.match(perfil, /title="Alterar Senha de Acesso"/);
  assert.match(perfil, /Proteção Avançada \(2FA\)/);
});
