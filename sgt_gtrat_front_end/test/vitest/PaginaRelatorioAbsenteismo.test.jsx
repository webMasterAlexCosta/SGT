import React from "react";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({
  iniciarExportacao: vi.fn(),
  notificar: vi.fn(),
  carregarFiltros: vi.fn(),
  gerarRelatorio: vi.fn(),
  gerarResumo: vi.fn(),
  validarFiltros: vi.fn(() => ({ valido: true, codigo: null, mensagem: null })),
}));

const { iniciarExportacao, notificar, carregarFiltros, gerarRelatorio, gerarResumo, validarFiltros } = mocks;

vi.mock("../../src/core/exportacao/context.jsx", () => ({
  useExportacaoRelatorio: () => ({ iniciarExportacao: mocks.iniciarExportacao }),
}));

vi.mock("../../src/core/events.js", () => ({ notificar: mocks.notificar }));

vi.mock("../../src/services/rh/ServicoAbsenteismo.js", () => ({
  INDICADORES_ABSENTEISMO_VAZIOS: {
    faltas: 0,
    atestados: 0,
    dsr: 0,
    totalPessoas: 0,
    taxaGeral: 0,
    diasTrabalhaveis: 0,
    alertasDisrupcao: 0,
    maioresTaxas: [],
  },
  ServicoAbsenteismo: {
    carregarFiltros: mocks.carregarFiltros,
    gerarRelatorio: mocks.gerarRelatorio,
    gerarResumo: mocks.gerarResumo,
    validarFiltros: mocks.validarFiltros,
  },
}));

vi.mock("recharts", () => {
  const Componente = ({ children }) => <div>{children}</div>;
  return {
    PieChart: Componente,
    Pie: Componente,
    Cell: () => null,
    BarChart: ({ data, children }) => (
      <div data-testid="grafico-top10" data-valores={JSON.stringify(data || [])}>{children}</div>
    ),
    Bar: Componente,
    XAxis: () => null,
    YAxis: () => null,
    CartesianGrid: () => null,
    Tooltip: () => null,
    Legend: () => null,
    ResponsiveContainer: Componente,
    LabelList: () => null,
  };
});

import { PaginaRelatorioAbsenteismo } from "../../src/modules/rh/pages/PaginaRelatorioAbsenteismo/index.jsx";

const filtros = {
  contratos: [
    { id: 2, numeroContrato: "002", nome: "Segundo", ativo: true },
    { id: 1, numeroContrato: "001", nome: "Primeiro", ativo: true },
    { id: 3, numeroContrato: "003", nome: "Inativo", ativo: false },
  ],
  lotacoes: [
    { id: 10, nome: "CTE Benfica", turnos: [{ id: 100, descricao: "Turno 1A" }] },
    { id: 20, nome: "CTCE Cidade Nova", turnos: [{ id: 200, descricao: "Turno 2B" }] },
  ],
  turnos: [
    { id: 100, descricao: "Turno 1A" },
    { id: 200, descricao: "Turno 2B" },
  ],
};

const pagina = {
  dados: [
    { id: 1, nome: "Pessoa da Página", cargo: "Auxiliar", lotacao: "CTE Benfica", turno: "Turno 1A", faltas: 1, atestados: 0, dsr: 0, total: 1, taxaIndividual: 5, disrupcao: 1, diasTrabalhaveis: 20 },
  ],
  indicadores: {},
  paginacao: {
    pagina: 0,
    tamanho: 100,
    totalPaginas: 2680,
    totalColaboradores: 268000,
    primeira: true,
    ultima: false,
  },
};

const resumo = {
  faltas: 1200,
  atestados: 450,
  dsr: 80,
  totalPessoas: 268000,
  taxaGeral: 3.25,
  diasTrabalhaveis: 4920000,
  alertasDisrupcao: 42,
  maioresTaxas: [
    { id: 999, nome: "Prioridade Nacional", taxaIndividual: 45.5, faltas: 9, atestados: 0, dsr: 0, total: 9, disrupcao: 12, diasTrabalhaveis: 20 },
  ],
};

function obterSelects(container) {
  const selects = container.querySelectorAll("select");
  return {
    mes: selects[0],
    contrato: selects[1],
    lotacao: selects[2],
    turno: selects[3],
  };
}

describe("PaginaRelatorioAbsenteismo - integração de componente com mocks", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    carregarFiltros.mockResolvedValue(filtros);
    gerarRelatorio.mockResolvedValue(pagina);
    gerarResumo.mockResolvedValue(resumo);
    validarFiltros.mockReturnValue({ valido: true, codigo: null, mensagem: null });
  });

  it("carrega filtros, remove contrato inativo e preserva opções Todos", async () => {
    const { container } = render(<PaginaRelatorioAbsenteismo />);

    await waitFor(() => expect(carregarFiltros).toHaveBeenCalledTimes(1));
    const { contrato, lotacao, turno } = obterSelects(container);

    expect(contrato).toHaveTextContent("TODOS OS CONTRATOS");
    expect(contrato).toHaveTextContent("001 - Primeiro");
    expect(contrato).toHaveTextContent("002 - Segundo");
    expect(contrato).not.toHaveTextContent("Inativo");
    expect(lotacao).toHaveTextContent("TODAS AS LOTAÇÕES");
    expect(turno).toHaveTextContent("TODOS OS TURNOS");
  });

  it("calcula página e resumo global sem usar a página como Top 10 nacional", async () => {
    render(<PaginaRelatorioAbsenteismo />);
    await waitFor(() => expect(carregarFiltros).toHaveBeenCalledTimes(1));

    fireEvent.click(screen.getByRole("button", { name: /calcular/i }));

    await waitFor(() => expect(gerarRelatorio).toHaveBeenCalledTimes(1));
    await waitFor(() => expect(gerarResumo).toHaveBeenCalledTimes(1));

    expect(await screen.findByText("Pessoa da Página")).toBeInTheDocument();
    expect(screen.getByText("3.25%")).toBeInTheDocument();
    expect(screen.getByText("1200")).toBeInTheDocument();

    const grafico = await screen.findByTestId("grafico-top10");
    const dadosGrafico = JSON.parse(grafico.dataset.valores);
    expect(dadosGrafico).toEqual([{ nome: "Prioridade Nacional", Taxa: 45.5 }]);
    expect(dadosGrafico.some((item) => item.nome.includes("Pessoa da Página"))).toBe(false);
  });

  it("reseta lotação e turno ao trocar o contrato", async () => {
    const { container } = render(<PaginaRelatorioAbsenteismo />);
    await waitFor(() => expect(carregarFiltros).toHaveBeenCalledTimes(1));
    const { contrato, lotacao, turno } = obterSelects(container);

    fireEvent.change(lotacao, { target: { value: "10" } });
    await waitFor(() => expect(lotacao.value).toBe("10"));
    fireEvent.change(turno, { target: { value: "100" } });
    expect(turno.value).toBe("100");

    fireEvent.change(contrato, { target: { value: "1" } });

    await waitFor(() => {
      expect(lotacao.value).toBe("");
      expect(turno.value).toBe("");
    });
  });

  it("envia filtros e total corretos para exportações PDF e XLSX", async () => {
    const { container } = render(<PaginaRelatorioAbsenteismo />);
    await waitFor(() => expect(carregarFiltros).toHaveBeenCalledTimes(1));
    const { contrato, lotacao, turno } = obterSelects(container);

    fireEvent.change(contrato, { target: { value: "1" } });
    await waitFor(() => expect(contrato.value).toBe("1"));
    fireEvent.change(lotacao, { target: { value: "10" } });
    await waitFor(() => expect(lotacao.value).toBe("10"));
    fireEvent.change(turno, { target: { value: "100" } });
    await waitFor(() => expect(turno.value).toBe("100"));
    fireEvent.click(screen.getByRole("button", { name: /calcular/i }));

    await waitFor(() => expect(screen.getByRole("button", { name: /excel/i })).toBeEnabled());

    fireEvent.click(screen.getByRole("button", { name: /excel/i }));
    fireEvent.click(screen.getByRole("button", { name: /^pdf/i }));

    expect(iniciarExportacao).toHaveBeenNthCalledWith(1, expect.objectContaining({
      tipo: "absenteismo",
      formato: "XLSX",
      totalEstimado: 268000,
      parametros: expect.objectContaining({ contratoId: "1", lotacaoId: "10", jornadaId: "100" }),
    }));
    expect(iniciarExportacao).toHaveBeenNthCalledWith(2, expect.objectContaining({
      tipo: "absenteismo",
      formato: "PDF",
      totalEstimado: 268000,
      parametros: expect.objectContaining({ contratoId: "1", lotacaoId: "10", jornadaId: "100" }),
    }));
  });


  it("ignora resumo antigo quando uma consulta mais nova termina primeiro", async () => {
    let resolverPrimeiro;
    let resolverSegundo;
    const primeiro = new Promise((resolve) => { resolverPrimeiro = resolve; });
    const segundo = new Promise((resolve) => { resolverSegundo = resolve; });
    gerarResumo
      .mockImplementationOnce(() => primeiro)
      .mockImplementationOnce(() => segundo);

    render(<PaginaRelatorioAbsenteismo />);
    await waitFor(() => expect(carregarFiltros).toHaveBeenCalledTimes(1));

    fireEvent.click(screen.getByRole("button", { name: /calcular/i }));
    await waitFor(() => expect(gerarResumo).toHaveBeenCalledTimes(1));
    await waitFor(() => expect(screen.getByRole("button", { name: /calcular/i })).toBeEnabled());

    fireEvent.click(screen.getByRole("button", { name: /calcular/i }));
    await waitFor(() => expect(gerarResumo).toHaveBeenCalledTimes(2));

    resolverSegundo({ ...resumo, taxaGeral: 2.1 });
    expect(await screen.findByText("2.1%")).toBeInTheDocument();

    resolverPrimeiro({ ...resumo, taxaGeral: 99.9 });
    await Promise.resolve();
    await Promise.resolve();

    expect(screen.getByText("2.1%")).toBeInTheDocument();
    expect(screen.queryByText("99.9%")).not.toBeInTheDocument();
  });

  it("notifica falha de resumo sem perder a tabela já carregada", async () => {
    gerarResumo.mockRejectedValue(new Error("resumo indisponível"));
    render(<PaginaRelatorioAbsenteismo />);
    await waitFor(() => expect(carregarFiltros).toHaveBeenCalledTimes(1));

    fireEvent.click(screen.getByRole("button", { name: /calcular/i }));

    expect(await screen.findByText("Pessoa da Página")).toBeInTheDocument();
    await waitFor(() => expect(notificar).toHaveBeenCalledWith(
      "warning",
      "A tabela foi carregada, mas os indicadores gerais não puderam ser calculados.",
    ));
  });
});
