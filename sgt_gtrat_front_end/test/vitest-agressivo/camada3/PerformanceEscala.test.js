import { describe, expect, it } from "vitest";
import { performance } from "node:perf_hooks";
import {
  agruparFaltasNoTopo,
  criarIndiceBusca,
  filtrarFrequenciasPorTermo,
} from "../../../src/services/rh/ServicoDominioFrequencia.js";
import {
  normalizarIdsFiltro,
} from "../../../src/services/rh/ServicoDominioEmpregado.js";
import {
  criarDias,
  formatarLinhaRelatorio,
} from "../../../src/services/rh/ServicoDominioRelatorioFrequencia.js";
import {
  consolidarAbsenteismo,
} from "../../../src/services/rh/ServicoAbsenteismo.js";
import { carregarTodasAsPaginas } from "../../../src/services/rh/nucleoPaginacao.js";

const medir = async (funcao) => {
  const inicio = performance.now();
  const resultado = await funcao();
  return { resultado, duracao: performance.now() - inicio };
};

const esperarAbaixo = (duracao, limite, nome) => {
  expect(duracao, `${nome} excedeu orçamento: ${duracao.toFixed(1)}ms > ${limite}ms`).toBeLessThan(limite);
};

describe("Camada 3D - orçamento de performance para escala corporativa", () => {
  it("deduplica 268 mil IDs sem algoritmo quadrático", async () => {
    const ids = Array.from({ length: 268000 }, (_, indice) => String((indice % 134000) + 1));
    const { resultado, duracao } = await medir(() => normalizarIdsFiltro(ids));
    expect(resultado).toHaveLength(134000);
    esperarAbaixo(duracao, 2500, "normalizarIdsFiltro(268k)");
  });

  it("agrupa 268 mil frequências preservando todos os registros", async () => {
    const lista = Array.from({ length: 268000 }, (_, indice) => ({
      empregadoId: indice + 1,
      status: indice % 17 === 0 ? "FALTA" : indice % 13 === 0 ? "ATESTADO" : "PRESENTE",
    }));
    const { resultado, duracao } = await medir(() => agruparFaltasNoTopo(lista));
    expect(resultado.listaOrdenada).toHaveLength(268000);
    expect(resultado.totalFaltas).toBeGreaterThan(0);
    expect(resultado.totalAtestados).toBeGreaterThan(0);
    esperarAbaixo(duracao, 2500, "agruparFaltasNoTopo(268k)");
  });

  it("filtra 100 mil frequências usando índice pré-calculado", async () => {
    const lista = Array.from({ length: 100000 }, (_, indice) => {
      const item = { nome: `Colaborador ${indice}`, cpf: String(10000000000 + indice), matricula: String(20000000 + indice) };
      return { ...item, indiceBusca: criarIndiceBusca(item) };
    });
    const { resultado, duracao } = await medir(() => filtrarFrequenciasPorTermo(lista, "colaborador 99999"));
    expect(resultado.some((item) => item.nome === "Colaborador 99999")).toBe(true);
    esperarAbaixo(duracao, 1800, "filtrarFrequenciasPorTermo(100k)");
  });

  it("consolida e ordena absenteísmo de 100 mil pessoas dentro do orçamento", async () => {
    const lista = Array.from({ length: 100000 }, (_, indice) => ({
      empregadoId: indice + 1,
      nome: `Colaborador ${indice}`,
      faltas: indice % 5,
      atestados: indice % 3,
      dsrPerdido: indice % 2,
      diasTrabalhaveis: 22,
      taxaIndividual: (100000 - indice) / 1000,
      disrupcao: indice % 11,
    }));
    const { resultado, duracao } = await medir(() => consolidarAbsenteismo(lista, 5));
    expect(resultado.dados).toHaveLength(100000);
    expect(resultado.indicadores.totalPessoas).toBe(100000);
    esperarAbaixo(duracao, 3000, "consolidarAbsenteismo(100k)");
  });

  it("carrega 268 mil itens paginados com concorrência máxima de 4 e ordem íntegra", async () => {
    const total = 268000;
    const tamanho = 5000;
    const totalPages = Math.ceil(total / tamanho);
    let emAndamento = 0;
    let pico = 0;

    const buscarPagina = async (pagina, tamanhoPagina) => {
      emAndamento += 1;
      pico = Math.max(pico, emAndamento);
      await Promise.resolve();
      const inicio = pagina * tamanhoPagina;
      const fim = Math.min(total, inicio + tamanhoPagina);
      const content = Array.from({ length: Math.max(0, fim - inicio) }, (_, offset) => inicio + offset);
      emAndamento -= 1;
      return { content, totalElements: total, totalPages };
    };

    const { resultado, duracao } = await medir(() => carregarTodasAsPaginas({
      buscarPagina,
      tamanhoPagina: tamanho,
      maximoItens: total,
      concorrencia: 4,
    }));

    expect(resultado).toHaveLength(total);
    expect(resultado[0]).toBe(0);
    expect(resultado.at(-1)).toBe(total - 1);
    expect(pico).toBeLessThanOrEqual(4);
    esperarAbaixo(duracao, 2500, "carregarTodasAsPaginas(268k)");
  });

  it("formata 5 mil linhas mensais sem explosão de custo", async () => {
    const diario = Object.fromEntries(criarDias("2026-08-01", "2026-08-31").map((dia) => [dia, { status: "PRESENTE", minutosPagaveis: 480 }]));
    const linha = {
      chaveAlocacao: 1,
      empregadoId: 1,
      nome: "Colaborador",
      cargo: "Auxiliar",
      contrato: "001/2026",
      lotacao: "CTE",
      turno: "1A",
      diario,
      totais: { presencas: 31, minutosFisicos: 14880, minutosPagaveis: 14880, minutosNoturnosReduzidos: 0 },
    };
    const { resultado, duracao } = await medir(() => Array.from({ length: 5000 }, (_, indice) => formatarLinhaRelatorio({ ...linha, chaveAlocacao: indice + 1, empregadoId: indice + 1 })));
    expect(resultado).toHaveLength(5000);
    esperarAbaixo(duracao, 3000, "formatarLinhaRelatorio(5k x 31 dias)");
  });

  it("cria um ano completo de dias em tempo constante aceitável", async () => {
    const { resultado, duracao } = await medir(() => criarDias("2026-01-01", "2026-12-31"));
    expect(resultado).toHaveLength(365);
    esperarAbaixo(duracao, 100, "criarDias(1 ano)");
  });
});
