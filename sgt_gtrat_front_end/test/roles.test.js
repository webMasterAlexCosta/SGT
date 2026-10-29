import test from "node:test";
import assert from "node:assert/strict";
import {
  AUTORIDADES,
  PERFIS,
  autoridadeDoPerfil,
  podeAcessarFrequenciaDiaria,
  podeVisualizarFrequencia,
  podeAcessarRelatorios,
  podeAcessarRelatoriosAdministrativos,
  podeAcessarVisaoConsolidadaRelatorios,
  podeVisualizarEmpregados,
  podeGerenciarEmpregados,
  podeVisualizarCalendario,
  podeGerenciarUsuarios,
  rotaInicialPorPerfil,
} from "../src/core/security/roles.js";

const usuario = (perfil) => ({ perfil, autoridade: autoridadeDoPerfil(perfil) });

test("Gestor Estadual consulta frequência, empregados, relatórios e calendário sem herdar escrita operacional", () => {
  const atual = usuario(PERFIS.GESTOR_ESTADUAL);
  assert.equal(podeAcessarFrequenciaDiaria(atual), false);
  assert.equal(podeVisualizarFrequencia(atual), true);
  assert.equal(podeAcessarRelatorios(atual), true);
  assert.equal(podeVisualizarEmpregados(atual), true);
  assert.equal(podeGerenciarEmpregados(atual), false);
  assert.equal(podeVisualizarCalendario(atual), true);
});

test("Gestor Local conserva as capacidades operacionais previstas pelo Java", () => {
  const atual = usuario(PERFIS.GESTOR_LOCAL);
  assert.equal(podeAcessarFrequenciaDiaria(atual), true);
  assert.equal(podeAcessarRelatorios(atual), true);
  assert.equal(podeVisualizarEmpregados(atual), true);
  assert.equal(podeGerenciarEmpregados(atual), true);
  assert.equal(podeVisualizarCalendario(atual), true);
});

test("Operacional pode realizar frequência sem acessar cadastros gerenciais", () => {
  const atual = usuario(PERFIS.OPERACIONAL);
  assert.equal(podeAcessarFrequenciaDiaria(atual), true);
  assert.equal(podeVisualizarFrequencia(atual), true);
  assert.equal(podeVisualizarCalendario(atual), true);
  assert.equal(podeAcessarRelatorios(atual), false);
  assert.equal(podeAcessarVisaoConsolidadaRelatorios(atual), false);
  assert.equal(podeVisualizarEmpregados(atual), false);
  assert.equal(podeGerenciarEmpregados(atual), false);
  assert.equal(rotaInicialPorPerfil(atual), "/frequencia");
});

test("Presidência possui leitura administrativa nacional, sem relatórios operacionais nem gestão de usuários", () => {
  const atual = usuario(PERFIS.PRESIDENTE);
  assert.equal(podeAcessarRelatorios(atual), false);
  assert.equal(podeAcessarVisaoConsolidadaRelatorios(atual), false);
  assert.equal(podeAcessarRelatoriosAdministrativos(atual), true);
  assert.equal(podeGerenciarUsuarios(atual), false);
});

test("os seis perfis canônicos geram autoridades PERFIL_* sem autoridade implícita", () => {
  const perfis = Object.values(PERFIS);
  assert.equal(perfis.length, 6);
  assert.equal(new Set(perfis).size, 6);
  for (const perfil of perfis) {
    const autoridade = autoridadeDoPerfil(perfil);
    assert.ok(autoridade?.startsWith("PERFIL_"));
    assert.ok(Object.values(AUTORIDADES).includes(autoridade));
  }
  assert.equal(podeAcessarRelatorios({ perfil: "DESCONHECIDO" }), false);
});
