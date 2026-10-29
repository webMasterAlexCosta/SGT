import { beforeEach, describe, expect, it, vi } from "vitest";

const requisitar = vi.hoisted(() => vi.fn());
vi.mock("../../src/core/http.js", () => ({
  requisitar,
  extrairLista: (valor) => Array.isArray(valor) ? valor : valor?.content || [],
  extrairPagina: (valor) => valor?.content ? valor : { content: Array.isArray(valor) ? valor : [] },
}));

import { ServicoVinculoContratual } from "../../src/services/rh/ServicoVinculoContratual.js";
import {
  criarChaveContextoOpcoesAlocacao,
  ehProgramacaoFuturaCancelavel,
  prepararAlteracaoAlocacao,
  prepararCancelamentoAlocacao,
  validarAlteracaoAlocacao,
} from "../../src/services/rh/ServicoDominioAlocacaoVinculo.js";

beforeEach(() => {
  requisitar.mockReset();
  requisitar.mockResolvedValue([]);
});

describe("Etapa 12 - alocações temporais", () => {
  it("consome os oito endpoints de alocação definidos no VinculoContratualControlador", async () => {
    await ServicoVinculoContratual.listarAlocacoes(9, { mes: "2026-09" });
    await ServicoVinculoContratual.listarEventosAlocacao(9);
    await ServicoVinculoContratual.consultarPosicaoAlocacao(9, { data: "2026-09-12", conhecidoAte: "2026-09-12T12:30:00Z" });
    await ServicoVinculoContratual.obterOpcoesAlocacao(4, { data: "2026-09-12", lotacaoId: 2, jornadaId: 3 });
    await ServicoVinculoContratual.gerarPreviaAlocacao(9, {});
    await ServicoVinculoContratual.alterarAlocacao(9, {});
    await ServicoVinculoContratual.retificarAlocacao(9, 10, {});
    await ServicoVinculoContratual.cancelarProgramacaoAlocacao(9, 10, {});

    expect(requisitar.mock.calls.map(([c]) => `${c.method} ${c.url}`)).toEqual([
      "GET /vinculos-contratuais/9/alocacoes",
      "GET /vinculos-contratuais/9/alocacoes/eventos",
      "GET /vinculos-contratuais/9/alocacoes/posicao",
      "GET /vinculos-contratuais/contratos/4/opcoes-alocacao",
      "POST /vinculos-contratuais/9/previas/alocacoes",
      "POST /vinculos-contratuais/9/alocacoes",
      "POST /vinculos-contratuais/9/alocacoes/10/retificar",
      "POST /vinculos-contratuais/9/alocacoes/10/cancelar-programacao",
    ]);
    expect(requisitar.mock.calls[0][0].params).toEqual({ mes: "2026-09" });
    expect(requisitar.mock.calls[2][0].params).toEqual({ data: "2026-09-12", conhecidoAte: "2026-09-12T12:30:00Z" });
    expect(requisitar.mock.calls[3][0].params).toEqual({ data: "2026-09-12", lotacaoId: 2, jornadaId: 3 });
  });

  it("não repete mutações e não coloca senha no DTO de domínio", async () => {
    const payload = prepararAlteracaoAlocacao({
      dataEfetiva: "2026-09-30", composicaoId: "8", lotacaoId: "2", jornadaId: "3", cargoId: "4",
      justificativa: "Mudança operacional", evidencia: "SEI 1", autorizacao: "Gestor Local",
      confirmarImpacto: true, tokenPrevia: "token-atual", confirmarPrecedenciaUltimoDia: true,
    });
    await ServicoVinculoContratual.alterarAlocacao(7, payload);
    expect(requisitar.mock.calls[0][0]).toMatchObject({ method: "POST", retries: 0, data: payload });
    expect(payload).not.toHaveProperty("password");
    expect(payload).toMatchObject({ composicaoId: 8, lotacaoId: 2, jornadaId: 3, cargoId: 4, confirmarPrecedenciaUltimoDia: true });
  });

  it("preserva LocalDate e exige token/aceite/precedência no último dia", () => {
    const base = {
      dataEfetiva: "2026-10-15", lotacaoId: "2", jornadaId: "3", cargoId: "4",
      justificativa: "J", evidencia: "E", autorizacao: "A", tokenPrevia: "tok", confirmarImpacto: true,
      exigePrecedenciaUltimoDia: true, confirmarPrecedenciaUltimoDia: false,
    };
    expect(validarAlteracaoAlocacao(base)).toMatch(/precedência/i);
    expect(validarAlteracaoAlocacao({ ...base, confirmarPrecedenciaUltimoDia: true })).toBeNull();
    expect(prepararAlteracaoAlocacao(base).dataEfetiva).toBe("2026-10-15");
  });

  it("chave de contexto muda quando data ou dependência muda, impedindo aceitar resposta antiga", () => {
    const a = criarChaveContextoOpcoesAlocacao({ contratoId: 1, data: "2026-09-12", lotacaoId: 2, jornadaId: 3 });
    const b = criarChaveContextoOpcoesAlocacao({ contratoId: 1, data: "2026-09-13", lotacaoId: 2, jornadaId: 3 });
    const c = criarChaveContextoOpcoesAlocacao({ contratoId: 1, data: "2026-09-12", lotacaoId: 9, jornadaId: 3 });
    expect(new Set([a, b, c]).size).toBe(3);
  });

  it("cancelamento futuro mantém somente os campos auditáveis aceitos pelo Java", () => {
    expect(prepararCancelamentoAlocacao({ justificativa: " J ", evidencia: " E ", autorizacao: " A " })).toEqual({
      justificativa: "J", evidencia: "E", autorizacao: "A", confirmarImpacto: false, tokenPrevia: null,
    });
  });

  it("cancelamento só é sugerido para faixa REGISTRADA que ainda começa no futuro", () => {
    expect(ehProgramacaoFuturaCancelavel({ estadoNegocial: "REGISTRADA", dataInicio: "2026-10-01" }, "2026-09-12")).toBe(true);
    expect(ehProgramacaoFuturaCancelavel({ estadoNegocial: "REGISTRADA", dataInicio: "2026-09-01" }, "2026-09-12")).toBe(false);
    expect(ehProgramacaoFuturaCancelavel({ estadoNegocial: "CANCELADA", dataInicio: "2026-10-01" }, "2026-09-12")).toBe(false);
  });

  it("opções vazias permanecem vazias e não inventam composição local", async () => {
    requisitar.mockResolvedValueOnce([]);
    await expect(ServicoVinculoContratual.obterOpcoesAlocacao(4, { data: "2026-09-12" })).resolves.toEqual([]);
    expect(requisitar.mock.calls[0][0].params).toEqual({ data: "2026-09-12" });
  });
});
