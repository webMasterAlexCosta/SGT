import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { AUTORIDADES, PERFIS, podeVisualizarContratos } from "../src/core/security/roles.js";
import {
  UFS_BRASIL,
  filtrarContratos,
  obterUfsDisponiveis,
} from "../src/services/admin/ServicoDominioContrato.js";

const raiz = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");

test("presidente pode abrir a área de contratos em modo de consulta", () => {
  assert.equal(podeVisualizarContratos({ perfil: PERFIS.PRESIDENTE, autoridade: AUTORIDADES.PRESIDENTE }), true);
});

test("acesso nacional oferece as 27 UFs brasileiras no filtro de contratos", () => {
  const ufs = obterUfsDisponiveis([], true);
  assert.equal(ufs.length, 27);
  assert.deepEqual(ufs, [...UFS_BRASIL]);
  assert.ok(ufs.includes("RJ"));
  assert.ok(ufs.includes("SP"));
  assert.ok(ufs.includes("DF"));
});

test("filtro por estado retorna somente contratos da UF selecionada", () => {
  const contratos = [
    { id: 1, numeroContrato: "RJ-001", uf: "RJ", gestaoLocalId: 10, ativo: true },
    { id: 2, numeroContrato: "SP-001", uf: "SP", gestaoLocalId: 20, ativo: true },
    { id: 3, numeroContrato: "RJ-002", uf: "RJ", gestaoLocalId: 11, ativo: false },
  ];

  const filtrados = filtrarContratos({ contratos, filtroUf: "RJ" });
  assert.deepEqual(filtrados.map((item) => item.id), [1, 3]);

  const porGestaoLocal = filtrarContratos({ contratos, filtroGestaoLocal: "11" });
  assert.deepEqual(porGestaoLocal.map((item) => item.id), [3]);
});

test("tela de contratos trata presidente como acesso nacional de leitura", async () => {
  const pagina = await readFile(
    path.join(raiz, "src/modules/admin/pages/PaginaContratos/index.jsx"),
    "utf8",
  );
  const filtros = await readFile(
    path.join(raiz, "src/modules/admin/pages/PaginaContratos/components/FiltrosContratos/index.jsx"),
    "utf8",
  );

  assert.match(pagina, /acessoNacionalContratos\s*=\s*acessoGlobal\s*\|\|\s*presidente/);
  assert.match(pagina, /todos os contratos do Brasil/i);
  assert.match(filtros, /Estado \(UF\)/);
  assert.match(filtros, /Todos os estados/);
  assert.match(filtros, /Todas as Gestões Locais/);
});
