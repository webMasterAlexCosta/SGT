import test from "node:test";
import assert from "node:assert/strict";
import {
  normalizarHistoricoContrato,
  normalizarItemHistoricoContrato,
} from "../src/services/rh/ServicoDominioEmpregado.js";

test("histórico plano preserva todas as informações do contrato", () => {
  const item = normalizarItemHistoricoContrato({
    id: 10,
    empregadoId: 4,
    empregadoNome: "Pessoa Teste",
    contratoId: 20,
    numeroContrato: "TERC-20",
    nomeContrato: "Empresa Exemplo",
    descricaoContrato: "Vigilância patrimonial",
    tipoContrato: "TERCEIRIZAÇÃO",
    contratoDataInicio: "2026-01-01",
    contratoDataFim: "2026-12-31",
    contratoAtivo: true,
    efetivoPrevisto: 30,
    efetivoAtual: 24,
    saldoEfetivo: 6,
    percentualOcupacao: 80,
    diaSemanaReduzido: "SATURDAY",
    horasDiaReduzido: 4,
    gestaoLocalId: 2,
    gestaoLocalNome: "GTRAT-RJ",
    superintendenciaId: 1,
    superintendenciaNome: "SE/RJ",
    uf: "RJ",
    dataInicio: "2026-02-01T10:00:00-03:00",
    dataFim: null,
    observacao: "Iniciado em cadastro",
    ativo: true,
  });

  assert.equal(item.numeroContrato, "TERC-20");
  assert.equal(item.nomeContrato, "Empresa Exemplo");
  assert.equal(item.descricaoContrato, "Vigilância patrimonial");
  assert.equal(item.gestaoLocalNome, "GTRAT-RJ");
  assert.equal(item.superintendenciaNome, "SE/RJ");
  assert.equal(item.efetivoPrevisto, 30);
  assert.equal(item.efetivoAtual, 24);
  assert.equal(item.saldoEfetivo, 6);
  assert.equal(item.percentualOcupacao, 80);
  assert.equal(item.ativo, true);
});

test("histórico aceita a página plana devolvida pelo Java", () => {
  const [item] = normalizarHistoricoContrato({
    content: [{ id: 1, contratoId: 2, numeroContrato: "CTR-2", ativo: true }],
  });

  assert.equal(item.contratoId, 2);
  assert.equal(item.numeroContrato, "CTR-2");
  assert.equal(item.ativo, true);
});
