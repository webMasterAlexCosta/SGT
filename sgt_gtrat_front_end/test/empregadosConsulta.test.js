import test from "node:test";
import assert from "node:assert/strict";
import {
  SEM_CONTRATO,
  criarConsultaEmpregados,
  consultaPossuiFiltro,
  normalizarIdsFiltro,
  normalizarTextoBusca,
} from "../src/services/rh/ServicoDominioEmpregado.js";

test("consulta normaliza pagina, texto e contratos sem duplicar ids", () => {
  const consulta = criarConsultaEmpregados({
    pagina: -10,
    tamanho: "50",
    busca: "  Joana    Silva  ",
    lotacaoId: 12,
    turnoId: 7,
    contratos: [3, "3", SEM_CONTRATO, null, ""],
  });

  assert.deepEqual(consulta, {
    pagina: 0,
    tamanho: 50,
    busca: "Joana Silva",
    lotacaoId: "12",
    turnoId: "7",
    contratoIds: ["3"],
    incluirSemContrato: true,
  });
  assert.equal(consultaPossuiFiltro(consulta), true);
});

test("consulta limpa não conserva parâmetros de filtros anteriores", () => {
  const limpa = criarConsultaEmpregados({
    pagina: 0,
    tamanho: 50,
    busca: "",
    lotacaoId: "",
    turnoId: "",
    contratos: [],
  });

  assert.deepEqual(limpa.contratoIds, []);
  assert.equal(limpa.incluirSemContrato, false);
  assert.equal(limpa.busca, "");
  assert.equal(limpa.lotacaoId, "");
  assert.equal(limpa.turnoId, "");
  assert.equal(consultaPossuiFiltro(limpa), false);
});

test("normalizadores limitam busca e removem ids repetidos", () => {
  assert.equal(normalizarTextoBusca(` ${"a".repeat(100)} `).length, 80);
  assert.deepEqual(normalizarIdsFiltro([1, "1", 2, null, ""]), ["1", "2"]);
});

test("consulta já normalizada preserva contratoIds na segunda normalização", () => {
  const primeira = criarConsultaEmpregados({
    pagina: 0,
    tamanho: 2000,
    contratos: [144],
  });
  const segunda = criarConsultaEmpregados(primeira);

  assert.deepEqual(primeira.contratoIds, ["144"]);
  assert.deepEqual(segunda.contratoIds, ["144"]);
  assert.deepEqual(segunda, primeira);
});

test("consulta já normalizada preserva o filtro sem contrato", () => {
  const primeira = criarConsultaEmpregados({
    contratos: [SEM_CONTRATO],
  });
  const segunda = criarConsultaEmpregados(primeira);

  assert.equal(primeira.incluirSemContrato, true);
  assert.equal(segunda.incluirSemContrato, true);
  assert.deepEqual(segunda.contratoIds, []);
});
