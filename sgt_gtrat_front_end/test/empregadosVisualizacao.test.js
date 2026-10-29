import test from "node:test";
import assert from "node:assert/strict";
import {
  CHAVE_MODO_VISUALIZACAO,
  MODOS_VISUALIZACAO,
  lerModoVisualizacao,
  normalizarModoVisualizacao,
  salvarModoVisualizacao,
} from "../src/modules/rh/pages/EmpregadosLista/visualizacao.js";

const criarArmazenamento = (valorInicial = null) => {
  const dados = new Map();
  if (valorInicial !== null) dados.set(CHAVE_MODO_VISUALIZACAO, valorInicial);
  return {
    getItem: (chave) => dados.get(chave) ?? null,
    setItem: (chave, valor) => dados.set(chave, valor),
    valor: () => dados.get(CHAVE_MODO_VISUALIZACAO),
  };
};

test("visualização de colaboradores aceita tabela, lista e grade", () => {
  assert.equal(normalizarModoVisualizacao(MODOS_VISUALIZACAO.TABELA), "tabela");
  assert.equal(normalizarModoVisualizacao(MODOS_VISUALIZACAO.LISTA), "lista");
  assert.equal(normalizarModoVisualizacao(MODOS_VISUALIZACAO.GRADE), "grade");
  assert.equal(normalizarModoVisualizacao("desconhecido"), "grade");
});

test("preferência visual é persistida sem comprometer a tela", () => {
  const armazenamento = criarArmazenamento();

  assert.equal(salvarModoVisualizacao(armazenamento, "grade"), "grade");
  assert.equal(armazenamento.valor(), "grade");
  assert.equal(lerModoVisualizacao(armazenamento), "grade");

  const armazenamentoCorrompido = criarArmazenamento("invalido");
  assert.equal(lerModoVisualizacao(armazenamentoCorrompido), "grade");
  assert.equal(lerModoVisualizacao({ getItem: () => { throw new Error("bloqueado"); } }), "grade");
});
