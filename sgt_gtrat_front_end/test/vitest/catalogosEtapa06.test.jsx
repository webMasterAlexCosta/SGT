import React from "react";
import { act, renderHook } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import {
  AUTORIDADES,
  PERFIS,
  podeGerenciarCadastrosNacionais,
  podeGerenciarCargos,
  podeGerenciarLotacoesETurnos,
  podeGerenciarTiposContratacao,
  podeVisualizarCatalogosReferencia,
  podeVisualizarEstruturaOrganizacional,
  podeVisualizarLotacoesETurnos,
  podeVisualizarTiposContratacao,
} from "../../src/core/security/roles.js";
import {
  descricaoDisponibilidade,
  limparDependencias,
  opcoesHistoricas,
  opcoesParaNovaOperacao,
} from "../../src/core/catalogos/catalogoTemporal.js";
import { useRequisicaoEncadeada } from "../../src/hooks/useRequisicaoEncadeada.js";

const usuario = (perfil) => ({ perfil, autoridade: AUTORIDADES[perfil] });

const defer = () => {
  let resolve;
  let reject;
  const promise = new Promise((res, rej) => { resolve = res; reject = rej; });
  return { promise, resolve, reject };
};

describe("Etapa 06 - estrutura organizacional, catálogos e validade", () => {
  it("separa consulta e escrita entre Admin, Gestor Local e perfil somente leitura", () => {
    const admin = usuario(PERFIS.ADMINISTRADOR);
    const gestorLocal = usuario(PERFIS.GESTOR_LOCAL);
    const operacional = usuario(PERFIS.OPERACIONAL);

    expect(podeVisualizarEstruturaOrganizacional(admin)).toBe(true);
    expect(podeVisualizarLotacoesETurnos(admin)).toBe(true);
    expect(podeGerenciarLotacoesETurnos(admin)).toBe(false);
    expect(podeVisualizarCatalogosReferencia(admin)).toBe(true);
    expect(podeGerenciarCadastrosNacionais(admin)).toBe(true);

    expect(podeVisualizarLotacoesETurnos(gestorLocal)).toBe(true);
    expect(podeGerenciarLotacoesETurnos(gestorLocal)).toBe(true);
    expect(podeGerenciarTiposContratacao(gestorLocal)).toBe(true);
    expect(podeGerenciarCargos(gestorLocal)).toBe(true);
    expect(podeGerenciarCadastrosNacionais(gestorLocal)).toBe(false);

    expect(podeVisualizarLotacoesETurnos(operacional)).toBe(true);
    expect(podeGerenciarLotacoesETurnos(operacional)).toBe(false);
    expect(podeVisualizarCatalogosReferencia(operacional)).toBe(true);
    expect(podeGerenciarCadastrosNacionais(operacional)).toBe(false);
  });

  it("mantém Presidente em estrutura/tipo contratação, sem conceder catálogos que o Java nega", () => {
    const presidente = usuario(PERFIS.PRESIDENTE);
    expect(podeVisualizarEstruturaOrganizacional(presidente)).toBe(true);
    expect(podeVisualizarTiposContratacao(presidente)).toBe(true);
    expect(podeVisualizarLotacoesETurnos(presidente)).toBe(false);
    expect(podeVisualizarCatalogosReferencia(presidente)).toBe(false);
    expect(podeGerenciarTiposContratacao(presidente)).toBe(false);
  });

  it("permite escrita de Cargo somente ao Gestor Local, conforme o Java corrigido", () => {
    expect(podeGerenciarCargos(usuario(PERFIS.GESTOR_LOCAL))).toBe(true);
    for (const perfil of Object.values(PERFIS).filter((item) => item !== PERFIS.GESTOR_LOCAL)) {
      expect(podeGerenciarCargos(usuario(perfil))).toBe(false);
    }
  });

  it("preserva item inativo em histórico, mas o remove de uma nova operação", () => {
    const itens = [
      { id: 1, nome: "Ativo", ativo: true, validFrom: "2026-01-01" },
      { id: 2, nome: "Histórico", ativo: false, validFrom: "2025-01-01" },
    ];

    const historicas = opcoesHistoricas(itens);
    const novas = opcoesParaNovaOperacao(itens);

    expect(historicas).toHaveLength(2);
    expect(historicas[1]).toMatchObject({ id: 2, label: "Histórico", ativo: false, historico: true });
    expect(historicas[1].metadata.validFrom).toBe("2025-01-01");
    expect(novas.map((item) => item.id)).toEqual([1]);
    expect(descricaoDisponibilidade(itens[1])).toMatch(/preservado para histórico/i);
  });

  it("mantém o inativo já selecionado ao editar um fato existente, sem liberá-lo para novo fato", () => {
    const itens = [{ id: 8, descricao: "Turno legado", ativo: false }];
    expect(opcoesParaNovaOperacao(itens)).toEqual([]);
    expect(opcoesParaNovaOperacao(itens, { idSelecionado: 8 })).toHaveLength(1);
  });

  it("limpa selects dependentes quando o escopo pai muda", () => {
    const estado = { superintendenciaId: "1", gestaoLocalId: "2", lotacaoId: "3", jornadaId: "4" };
    const proximo = limparDependencias(estado, "superintendenciaId", {
      superintendenciaId: ["gestaoLocalId", "lotacaoId", "jornadaId"],
    });
    expect(proximo).toEqual({ superintendenciaId: "1", gestaoLocalId: "", lotacaoId: "", jornadaId: "" });
  });

  it("cancela a requisição encadeada anterior e ignora resposta obsoleta", async () => {
    const primeira = defer();
    const segunda = defer();
    const sinais = [];
    const { result } = renderHook(() => useRequisicaoEncadeada());

    let resultadoPrimeiro;
    let resultadoSegundo;
    await act(async () => {
      const p1 = result.current.executar(({ signal }) => { sinais.push(signal); return primeira.promise; });
      const p2 = result.current.executar(({ signal }) => { sinais.push(signal); return segunda.promise; });
      primeira.resolve("antigo");
      segunda.resolve("novo");
      [resultadoPrimeiro, resultadoSegundo] = await Promise.all([p1, p2]);
    });

    expect(sinais[0].aborted).toBe(true);
    expect(sinais[1].aborted).toBe(false);
    expect(resultadoPrimeiro).toEqual({ ignorado: true, valor: null });
    expect(resultadoSegundo).toEqual({ ignorado: false, valor: "novo" });
    expect(result.current.erro).toBeNull();
    expect(result.current.carregando).toBe(false);
  });

  it("mantém 403 como autoridade final: capacidades não concedem escrita a perfil de leitura", () => {
    const operacional = usuario(PERFIS.OPERACIONAL);
    expect(podeVisualizarCatalogosReferencia(operacional)).toBe(true);
    expect(podeGerenciarCadastrosNacionais(operacional)).toBe(false);
    expect(podeGerenciarLotacoesETurnos(operacional)).toBe(false);
    expect(podeGerenciarTiposContratacao(operacional)).toBe(false);
  });
});
