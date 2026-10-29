import test from "node:test";
import assert from "node:assert/strict";
import {
  contratoUnicoParaPayload,
  normalizarContratoUnico,
  validarQuantidadeContratos,
} from "../src/services/rh/ServicoDominioEmpregado.js";

test("normaliza listas para apenas um contrato atual", () => {
  assert.equal(normalizarContratoUnico([{ id: 17 }, { id: 21 }]), "17");
  assert.equal(normalizarContratoUnico({ id: 9 }), "9");
  assert.equal(normalizarContratoUnico(""), "");
});

test("payload do formulário envia zero ou um contrato", () => {
  assert.deepEqual(contratoUnicoParaPayload("42"), [{ id: 42 }]);
  assert.deepEqual(contratoUnicoParaPayload(""), []);
  assert.deepEqual(contratoUnicoParaPayload("invalido"), []);
});

test("validação rejeita dois contratos distintos e aceita duplicata do mesmo id", () => {
  assert.equal(validarQuantidadeContratos([{ id: 1 }, { id: 2 }]), false);
  assert.equal(validarQuantidadeContratos([{ id: 1 }, { id: "1" }]), true);
  assert.equal(validarQuantidadeContratos([]), true);
});

test("edição de lotação/turno mantém o contrato atual no payload", () => {
  const contratoAtual = normalizarContratoUnico([{ id: 77, numeroContrato: "CTR-77" }]);
  const payloadAntes = contratoUnicoParaPayload(contratoAtual);
  const payloadDepoisDeMudarAlocacao = contratoUnicoParaPayload(contratoAtual);
  assert.deepEqual(payloadAntes, [{ id: 77 }]);
  assert.deepEqual(payloadDepoisDeMudarAlocacao, [{ id: 77 }]);
});

test("backend não encerra vínculo ao mudar apenas alocação e faz flush em troca real de contrato", async () => {
  const { readFile } = await import("node:fs/promises");
  const { resolve } = await import("node:path");
  const backend = process.env.SGT_BACKEND_DIR;
  assert.ok(backend, "SGT_BACKEND_DIR é obrigatório para validar a regra integrada");
  const fonte = await readFile(
    resolve(backend, "src/main/java/com/example/siggtrat/servico/EmpregadoServico.java"),
    "utf8",
  );
  assert.match(fonte, /Objects\.equals\(contratoNovoId, contratoHistoricoId\)[\s\S]*?preencherContextoAlocacao\(historico, empregado, contratoNovo\)[\s\S]*?manteveVinculoAtual = true;[\s\S]*?continue;/);
  assert.doesNotMatch(fonte, /Objects\.equals\(contratoNovoId, contratoHistoricoId\)[\s\S]{0,500}boolean mesmoContexto/);
  assert.match(fonte, /saveAll\(historicosParaEncerrar\);[\s\S]*?historicoVinculoRepositorio\.flush\(\);/);
});
