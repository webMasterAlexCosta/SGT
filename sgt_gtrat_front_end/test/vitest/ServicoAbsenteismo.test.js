import { beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("../../src/services/rh/ServicoFrequencia.js", () => ({
  ServicoFrequencia: {
    carregarFiltrosRelatorio: vi.fn(),
    gerarRelatorioAbsenteismo: vi.fn(),
    gerarResumoAbsenteismo: vi.fn(),
    obterUrlExportacaoAbsenteismo: vi.fn(),
  },
}));

import { ServicoFrequencia } from "../../src/services/rh/ServicoFrequencia.js";
import {
  INDICADORES_ABSENTEISMO_VAZIOS,
  ServicoAbsenteismo,
  consolidarAbsenteismo,
  validarFiltrosAbsenteismo,
} from "../../src/services/rh/ServicoAbsenteismo.js";

describe("ServicoAbsenteismo - regras e confiabilidade", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("rejeita competência futura e aceita competência válida", () => {
    const referencia = new Date(2026, 7, 21);

    expect(validarFiltrosAbsenteismo({ mes: 9, ano: 2026, referencia })).toEqual({
      valido: false,
      codigo: "COMPETENCIA_FUTURA",
      mensagem: "Não é possível gerar relatórios para competências futuras.",
    });

    expect(validarFiltrosAbsenteismo({ mes: 8, ano: 2026, referencia })).toEqual({
      valido: true,
      codigo: null,
      mensagem: null,
    });
  });

  it("consolida faltas, atestados, DSR e ordena pela maior taxa", () => {
    const resultado = consolidarAbsenteismo([
      {
        empregadoId: 2,
        nome: "Bruno",
        faltas: 1,
        atestados: 1,
        dsrPerdido: 1,
        diasTrabalhaveis: 20,
        taxaIndividual: 10,
        disrupcao: 3,
      },
      {
        empregadoId: 1,
        nome: "Ana",
        faltas: 2,
        atestados: 0,
        dsr: 0,
        diasTrabalhaveis: 20,
        taxaIndividual: 15,
        disrupcao: 9,
      },
    ], 8);

    expect(resultado.dados.map((item) => item.nome)).toEqual(["Ana", "Bruno"]);
    expect(resultado.dados[1].total).toBe(3);
    expect(resultado.indicadores).toMatchObject({
      faltas: 3,
      atestados: 1,
      dsr: 1,
      totalPessoas: 2,
      diasTrabalhaveis: 40,
      alertasDisrupcao: 1,
      taxaGeral: 10,
    });
  });

  it("retorna indicadores vazios com lista vazia", () => {
    expect(consolidarAbsenteismo([])).toEqual({
      dados: [],
      indicadores: { ...INDICADORES_ABSENTEISMO_VAZIOS },
    });
  });

  it("usa mock do serviço de frequência para carregar e normalizar filtros", async () => {
    ServicoFrequencia.carregarFiltrosRelatorio.mockResolvedValue({
      contratos: [{ id: 1 }],
      lotacoes: [{ id: 2 }],
      turnos: [{ id: 3 }],
    });

    await expect(ServicoAbsenteismo.carregarFiltros()).resolves.toEqual({
      contratos: [{ id: 1 }],
      lotacoes: [{ id: 2 }],
      turnos: [{ id: 3 }],
    });
    expect(ServicoFrequencia.carregarFiltrosRelatorio).toHaveBeenCalledTimes(1);
  });

  it("normaliza página retornada pela API sem perder metadados de paginação", async () => {
    ServicoFrequencia.gerarRelatorioAbsenteismo.mockResolvedValue({
      registros: [{ empregadoId: 10, nome: "Colaborador", faltas: 1, diasTrabalhaveis: 20, taxaIndividual: 5 }],
      pagina: 2,
      tamanho: 100,
      totalPaginas: 7,
      totalColaboradores: 650,
      primeira: false,
      ultima: false,
    });

    const resultado = await ServicoAbsenteismo.gerarRelatorio({ mes: 8, ano: 2026, page: 2, size: 100 });

    expect(ServicoFrequencia.gerarRelatorioAbsenteismo).toHaveBeenCalledWith(
      { mes: 8, ano: 2026, page: 2, size: 100 },
      {},
    );
    expect(resultado.dados[0]).toMatchObject({ id: 10, nome: "Colaborador", faltas: 1, taxaIndividual: 5 });
    expect(resultado.paginacao).toEqual({
      pagina: 2,
      tamanho: 100,
      totalPaginas: 7,
      totalColaboradores: 650,
      primeira: false,
      ultima: false,
    });
  });

  it("normaliza resumo global e Top 10 retornados pelo backend", async () => {
    ServicoFrequencia.gerarResumoAbsenteismo.mockResolvedValue({
      faltas: 120,
      atestados: 40,
      dsrPerdido: 12,
      totalColaboradores: 268000,
      taxaGeral: 3.25,
      diasTrabalhaveis: 4920000,
      alertasDisrupcao: 8,
      maioresTaxas: [
        { empregadoId: 9, nome: "Maior Taxa", taxaIndividual: 44.5, faltas: 8, diasTrabalhaveis: 18 },
      ],
    });

    const resumo = await ServicoAbsenteismo.gerarResumo({ mes: 8, ano: 2026 });

    expect(resumo).toMatchObject({
      faltas: 120,
      atestados: 40,
      dsr: 12,
      totalPessoas: 268000,
      taxaGeral: 3.25,
      diasTrabalhaveis: 4920000,
      alertasDisrupcao: 8,
    });
    expect(resumo.maioresTaxas[0]).toMatchObject({ id: 9, nome: "Maior Taxa", taxaIndividual: 44.5 });
  });
});
