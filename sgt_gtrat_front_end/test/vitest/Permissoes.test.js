import { describe, expect, it } from "vitest";
import {
  AUTORIDADES,
  PERFIS,
  podeAcessarRelatorios,
  podeAcessarRelatoriosAdministrativos,
  podeAcessarVisaoConsolidadaRelatorios,
  podeGerenciarUsuarios,
  podeVisualizarCalendario,
  podeVisualizarFrequencia,
  rotaInicialPorPerfil,
} from "../../src/core/security/roles.js";

const usuario = (perfil) => ({ perfil, autoridade: AUTORIDADES[perfil] });

describe("Etapa 04 - matriz canônica de perfis", () => {
  it.each([
    PERFIS.ADMINISTRADOR,
    PERFIS.GESTOR_NACIONAL,
    PERFIS.GESTOR_ESTADUAL,
    PERFIS.GESTOR_LOCAL,
  ])("permite relatórios operacionais ao perfil %s conforme métodos do Java", (perfil) => {
    expect(podeAcessarRelatorios(usuario(perfil))).toBe(true);
    expect(podeAcessarVisaoConsolidadaRelatorios(usuario(perfil))).toBe(true);
  });

  it.each([
    PERFIS.ADMINISTRADOR,
    PERFIS.GESTOR_NACIONAL,
    PERFIS.GESTOR_ESTADUAL,
    PERFIS.GESTOR_LOCAL,
    PERFIS.PRESIDENTE,
  ])("permite leitura administrativa ao perfil %s", (perfil) => {
    expect(podeAcessarRelatoriosAdministrativos(usuario(perfil))).toBe(true);
  });

  it("Presidente é leitura nacional, sem gestão de usuários ou frequência", () => {
    const presidente = usuario(PERFIS.PRESIDENTE);
    expect(podeAcessarRelatorios(presidente)).toBe(false);
    expect(podeAcessarRelatoriosAdministrativos(presidente)).toBe(true);
    expect(podeGerenciarUsuarios(presidente)).toBe(false);
    expect(podeVisualizarFrequencia(presidente)).toBe(false);
    expect(rotaInicialPorPerfil(presidente)).toBe("/painel");
  });

  it("Operacional fica sem relatório e inicia em frequência", () => {
    const operacional = usuario(PERFIS.OPERACIONAL);
    expect(podeAcessarRelatorios(operacional)).toBe(false);
    expect(podeVisualizarCalendario(operacional)).toBe(true);
    expect(podeVisualizarFrequencia(operacional)).toBe(true);
    expect(rotaInicialPorPerfil(operacional)).toBe("/frequencia");
  });

  it("os seis perfis canônicos não dependem de role/roles legados", () => {
    Object.values(PERFIS).forEach((perfil) => {
      expect(AUTORIDADES[perfil]).toBe(`PERFIL_${perfil}`);
      expect(String(AUTORIDADES[perfil])).toMatch(/^PERFIL_/);
    });
  });

  it("falha fechado quando perfil/autoridade não são reconhecidos", () => {
    expect(podeAcessarRelatorios({ perfil: "DESCONHECIDO" })).toBe(false);
    expect(podeVisualizarCalendario({})).toBe(false);
    expect(rotaInicialPorPerfil({})).toBe("/perfil");
  });
});
