import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const ler = (caminho) => readFile(new URL(caminho, import.meta.url), "utf8");

test("governança usa confirmação de senha nas ações protegidas", async () => {
  const fonte = await ler("../src/modules/governanca/pages/PaginaGovernancaTerceiros/index.jsx");
  assert.match(fonte, /useConfirmacaoSenha/);
  assert.match(fonte, /<ModalConfirmacao\s*\/>/);
  assert.match(fonte, /senhaConfirmacao/);
  assert.match(fonte, /executarProtegido/);
  for (const acao of [
    "criarFornecedor",
    "vincularFornecedor",
    "criarTabelaTarifaria",
    "criarProcesso",
    "atualizarProcesso",
    "gerarFolhaHoras",
    "submeterFolhaHoras",
    "aprovarFolhaHoras",
    "rejeitarFolhaHoras",
    "gerarFatura",
    "atualizarFatura",
    "avaliarRisco",
  ]) {
    assert.match(fonte, new RegExp(`servicoGovernanca\\.${acao}`));
  }
});

test("serviço de governança aceita opções de segurança em todas as escritas", async () => {
  const fonte = await ler("../src/services/governanca/ServicoGovernancaTerceiros.js");
  for (const acao of [
    "criarFornecedor",
    "vincularFornecedor",
    "criarTabelaTarifaria",
    "criarProcesso",
    "atualizarProcesso",
    "gerarFolhaHoras",
    "submeterFolhaHoras",
    "aprovarFolhaHoras",
    "rejeitarFolhaHoras",
    "gerarFatura",
    "atualizarFatura",
    "avaliarRisco",
  ]) {
    assert.match(fonte, new RegExp(`${acao}:[^\\n]*options = \\{\\}`));
  }
  assert.match(fonte, /\.\.\.options/);
});

test("cancelamento dos prompts não dispara rejeição, faturamento ou emissão", async () => {
  const fonte = await ler("../src/modules/governanca/pages/PaginaGovernancaTerceiros/index.jsx");
  assert.match(fonte, /if \(motivo === null\) return/);
  assert.match(fonte, /if \(informado === null\) return/);
  assert.match(fonte, /if \(numeroDocumento === null\) return/);
});
