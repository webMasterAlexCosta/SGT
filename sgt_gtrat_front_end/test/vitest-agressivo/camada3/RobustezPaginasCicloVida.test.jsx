import React, { Profiler, StrictMode } from "react";
import { act, render } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { COMPONENTES_MANIFEST } from "../../vitest-componentes/fixtures/componentesManifest.js";
import {
  RespostaVaziaTeste,
  UsuarioAdministradorTeste,
  criarPropsPopuladas,
  criarRespostaApiPopulada,
  ehComponenteReact,
  ehErroCriticoConsole,
  ehModal,
  ehPagina,
  textoConsole,
} from "../fixtures/ambienteAgressivo.jsx";

const estado = vi.hoisted(() => ({
  modoApi: "populado",
  auth: {
    user: {
      id: 1,
      username: "Administrador Teste",
      nome: "Administrador Teste",
      matricula: "12345678",
      email: "admin.teste@exemplo.invalid",
      perfil: "ADMINISTRADOR",
      autoridade: "PERFIL_ADMINISTRADOR",
      ativo: true,
      active: true,
      twoFactorEnabled: false,
      twoFactorVerified: true,
      forceChangePassword: false,
      lotacaoId: 1,
      lotacaoNome: "CTE Teste",
      jornadaId: 1,
      turno: "Turno 1A",
      uf: "RJ",
      vinculosGestao: [],
    },
    isAdmin: true,
    autenticar: vi.fn(),
    cadastrar: vi.fn(),
    encerrarSessao: vi.fn(),
    loading: false,
    atualizarPerfil: vi.fn(),
    loggingOut: false,
    concluirSaida: vi.fn(),
    atualizarInformacoesSessao: vi.fn(async () => ({ session: { remainingSeconds: 1800 } })),
    sessionExpiresAtEpochMs: Date.now() + 60 * 60 * 1000,
  },
  iniciarExportacao: vi.fn(async () => ({ id: "exportacao-teste" })),
  tarefas: [],
  medirLatencia: vi.fn(async () => 12),
  carregarRuntimeConfig: vi.fn(async () => ({ EM_MANUTENCAO: false, PREVISAO_RETORNO: "", CLIENT_IP: "127.0.0.1" })),
  buscarLocalizacaoPorCoordenadas: vi.fn(async () => ({ cidade: "Rio de Janeiro", estado: "RJ" })),
}));

const respostaPorModo = () => {
  if (estado.modoApi === "erro") return Promise.reject(new Error("HTTP 500 simulado pela camada de robustez"));
  if (estado.modoApi === "null") return Promise.resolve(null);
  if (estado.modoApi === "objeto-vazio") return Promise.resolve({});
  if (estado.modoApi === "lista-vazia") return Promise.resolve([]);
  if (estado.modoApi === "item-parcial") return Promise.resolve({
    content: [{}], dados: [{}], items: [{}], totalElements: 1, totalPages: 1, totalPaginas: 1,
    contratos: [{}], lotacoes: [{}], turnos: [{}], jornadas: [{}], usuarios: [{}], empregados: [{}], eventos: [{}],
  });
  return Promise.resolve(criarRespostaApiPopulada());
};

const requisitarMock = vi.hoisted(() => vi.fn());
const apiGetMock = vi.hoisted(() => vi.fn());
const apiPostMock = vi.hoisted(() => vi.fn());
const apiPutMock = vi.hoisted(() => vi.fn());
const apiPatchMock = vi.hoisted(() => vi.fn());
const apiDeleteMock = vi.hoisted(() => vi.fn());
const apiRequestMock = vi.hoisted(() => vi.fn());

vi.mock("../../../src/modules/auth/context.jsx", () => ({
  useAutenticacao: () => estado.auth,
  ProvedorAutenticacao: ({ children }) => <>{children}</>,
}));

vi.mock("../../../src/core/exportacao/context.jsx", () => ({
  useExportacaoRelatorio: () => ({ iniciarExportacao: estado.iniciarExportacao, tarefas: estado.tarefas }),
  ProvedorExportacaoRelatorio: ({ children }) => <>{children}</>,
}));

vi.mock("../../../src/core/http.js", () => ({
  requisitar: requisitarMock,
  extrairLista: (valor) => {
    if (Array.isArray(valor)) return valor;
    if (Array.isArray(valor?.content)) return valor.content;
    if (Array.isArray(valor?.dados)) return valor.dados;
    if (Array.isArray(valor?.items)) return valor.items;
    return [];
  },
  extrairPagina: (valor) => {
    if (valor && typeof valor === "object" && Array.isArray(valor.content)) return valor;
    const lista = Array.isArray(valor) ? valor : [];
    return { content: lista, totalElements: lista.length, totalPages: lista.length ? 1 : 0, number: 0, size: 100, first: true, last: true };
  },
  criarConfiguracaoRequisicaoAxios: (opcoes) => opcoes,
  api: {
    get: apiGetMock,
    post: apiPostMock,
    put: apiPutMock,
    patch: apiPatchMock,
    delete: apiDeleteMock,
    request: apiRequestMock,
  },
}));

vi.mock("../../../src/services/system/ServicoSistema.js", () => ({
  ServicoSistema: {
    medirLatencia: estado.medirLatencia,
    carregarRuntimeConfig: estado.carregarRuntimeConfig,
    buscarLocalizacaoPorCoordenadas: estado.buscarLocalizacaoPorCoordenadas,
  },
}));

vi.mock("recharts", () => {
  const Conteiner = ({ children }) => <div>{children}</div>;
  const Vazio = () => null;
  return {
    Area: Conteiner,
    AreaChart: Conteiner,
    Bar: Conteiner,
    BarChart: Conteiner,
    CartesianGrid: Vazio,
    Cell: Vazio,
    ComposedChart: Conteiner,
    LabelList: Vazio,
    Legend: Vazio,
    Line: Conteiner,
    LineChart: Conteiner,
    Pie: Conteiner,
    PieChart: Conteiner,
    Radar: Conteiner,
    RadarChart: Conteiner,
    PolarAngleAxis: Vazio,
    PolarGrid: Vazio,
    PolarRadiusAxis: Vazio,
    ReferenceLine: Vazio,
    ResponsiveContainer: Conteiner,
    Scatter: Conteiner,
    ScatterChart: Conteiner,
    Tooltip: Vazio,
    XAxis: Vazio,
    YAxis: Vazio,
    ZAxis: Vazio,
  };
});

const modulosReact = import.meta.glob("../../../src/**/*.jsx");
const caminhosComContextoReal = new Set(["src/modules/auth/context.jsx", "src/core/exportacao/context.jsx"]);
const paginas = COMPONENTES_MANIFEST.filter((entrada) => ehPagina(entrada) && !caminhosComContextoReal.has(entrada.file));

const ProvedorTeste = ({ children }) => <MemoryRouter initialEntries={["/painel"]}>{children}</MemoryRouter>;

const flush = async (ms = 0) => {
  await act(async () => {
    await Promise.resolve();
    await Promise.resolve();
    await new Promise((resolve) => setTimeout(resolve, ms));
  });
};

beforeEach(() => {
  estado.modoApi = "populado";
  estado.auth.user = { ...UsuarioAdministradorTeste, active: true };
  estado.auth.loading = false;
  estado.auth.loggingOut = false;
  estado.tarefas = [];
  const ligar = (mock) => mock.mockImplementation(() => respostaPorModo());
  [requisitarMock, apiGetMock, apiPostMock, apiPutMock, apiPatchMock, apiDeleteMock, apiRequestMock].forEach(ligar);
});

afterEach(() => {
  vi.restoreAllMocks();
});

const carregarComponente = async (entrada) => {
  const caminho = `../../../${entrada.file}`;
  const carregar = modulosReact[caminho];
  expect(carregar, `Módulo não encontrado: ${caminho}`).toBeTypeOf("function");
  const modulo = await carregar();
  return modulo[entrada.exportName];
};

describe("Camada 3A - páginas sobrevivem a respostas HTTP anormais", () => {
  const modos = ["null", "objeto-vazio", "lista-vazia", "item-parcial", "erro"];

  for (const entrada of paginas) {
    for (const modo of modos) {
      it(`${entrada.file} :: ${entrada.localName} permanece montável com API=${modo}`, async () => {
        estado.modoApi = modo;
        const consoleError = vi.spyOn(console, "error").mockImplementation(() => {});
        const consoleWarn = vi.spyOn(console, "warn").mockImplementation(() => {});
        try {
          const Componente = await carregarComponente(entrada);
          if (!ehComponenteReact(Componente)) return;
          const props = criarPropsPopuladas(entrada, { abrirModais: false });
          let resultado;
          await act(async () => {
            resultado = render(<Componente {...props} />, { wrapper: ProvedorTeste });
            await Promise.resolve();
          });
          await flush(10);
          expect(resultado.container).toBeTruthy();
          resultado.unmount();
          await flush();
        } finally {
          consoleError.mockRestore();
          consoleWarn.mockRestore();
        }
      });
    }
  }
});

describe("Camada 3B - StrictMode, responsividade e orçamento de rerender", () => {
  for (const entrada of COMPONENTES_MANIFEST) {
    if (caminhosComContextoReal.has(entrada.file)) continue;

    it(`${entrada.file} :: ${entrada.localName} tolera montagem dupla, 3 viewports e rerenders`, async () => {
      estado.modoApi = "populado";
      const errosCriticos = [];
      const consoleError = vi.spyOn(console, "error").mockImplementation((...args) => {
        if (ehErroCriticoConsole(args)) errosCriticos.push(args);
      });
      const consoleWarn = vi.spyOn(console, "warn").mockImplementation((...args) => {
        if (ehErroCriticoConsole(args)) errosCriticos.push(args);
      });

      try {
        const Componente = await carregarComponente(entrada);
        if (!ehComponenteReact(Componente)) return;
        const commits = [];
        const props = criarPropsPopuladas(entrada, { abrirModais: ehModal(entrada), carregando: false });
        const onRender = (_id, fase, duracao) => commits.push({ fase, duracao });

        const arvore = (p) => (
          <StrictMode>
            <Profiler id={entrada.localName} onRender={onRender}>
              <Componente {...p} />
            </Profiler>
          </StrictMode>
        );

        window.innerWidth = 1920;
        let resultado;
        await act(async () => {
          resultado = render(arvore(props), { wrapper: ProvedorTeste });
          await Promise.resolve();
        });
        await flush();

        for (const largura of [768, 320, 1366]) {
          window.innerWidth = largura;
          window.dispatchEvent(new Event("resize"));
          const proximaProps = criarPropsPopuladas(entrada, {
            abrirModais: ehModal(entrada),
            carregando: largura === 768,
          });
          await act(async () => {
            resultado.rerender(arvore(proximaProps));
            await Promise.resolve();
          });
          await flush();
        }

        expect(
          errosCriticos,
          `Erro em StrictMode/responsividade de ${entrada.file}:\n${errosCriticos.map(textoConsole).join("\n")}`,
        ).toEqual([]);
        expect(commits.length, `Rerenders excessivos em ${entrada.file}`).toBeLessThanOrEqual(30);
        const duracaoTotal = commits.reduce((soma, item) => soma + Number(item.duracao || 0), 0);
        expect(duracaoTotal, `Renderização muito lenta em ${entrada.file}: ${duracaoTotal.toFixed(1)}ms`).toBeLessThan(2500);
      } finally {
        consoleError.mockRestore();
        consoleWarn.mockRestore();
      }
    });
  }
});

describe("Camada 3C - componentes limpam listeners e intervalos ao desmontar", () => {
  for (const entrada of COMPONENTES_MANIFEST) {
    if (caminhosComContextoReal.has(entrada.file)) continue;

    it(`${entrada.file} :: ${entrada.localName} não deixa listener/intervalo órfão`, async () => {
      estado.modoApi = "populado";
      const ativosWindow = new Map();
      const ativosDocument = new Map();
      const intervalos = new Set();
      let proximoIntervalo = 100000;

      const adicionar = (mapa, tipo, listener) => {
        if (tipo === "selectionchange") return;
        if (!mapa.has(tipo)) mapa.set(tipo, new Set());
        mapa.get(tipo).add(listener);
      };
      const remover = (mapa, tipo, listener) => mapa.get(tipo)?.delete(listener);

      const addWinReal = window.addEventListener.bind(window);
      const remWinReal = window.removeEventListener.bind(window);
      const addDocReal = document.addEventListener.bind(document);
      const remDocReal = document.removeEventListener.bind(document);

      const addWin = vi.spyOn(window, "addEventListener").mockImplementation((tipo, listener, options) => {
        adicionar(ativosWindow, tipo, listener);
        return addWinReal(tipo, listener, options);
      });
      const remWin = vi.spyOn(window, "removeEventListener").mockImplementation((tipo, listener, options) => {
        remover(ativosWindow, tipo, listener);
        return remWinReal(tipo, listener, options);
      });
      const addDoc = vi.spyOn(document, "addEventListener").mockImplementation((tipo, listener, options) => {
        adicionar(ativosDocument, tipo, listener);
        return addDocReal(tipo, listener, options);
      });
      const remDoc = vi.spyOn(document, "removeEventListener").mockImplementation((tipo, listener, options) => {
        remover(ativosDocument, tipo, listener);
        return remDocReal(tipo, listener, options);
      });

      const setIntervalGlobal = vi.spyOn(globalThis, "setInterval").mockImplementation(() => {
        const id = proximoIntervalo++;
        intervalos.add(id);
        return id;
      });
      const clearIntervalGlobal = vi.spyOn(globalThis, "clearInterval").mockImplementation((id) => {
        intervalos.delete(id);
      });

      let setIntervalWindow;
      let clearIntervalWindow;
      if (window.setInterval !== globalThis.setInterval) {
        setIntervalWindow = vi.spyOn(window, "setInterval").mockImplementation(() => {
          const id = proximoIntervalo++;
          intervalos.add(id);
          return id;
        });
        clearIntervalWindow = vi.spyOn(window, "clearInterval").mockImplementation((id) => intervalos.delete(id));
      }

      try {
        const Componente = await carregarComponente(entrada);
        if (!ehComponenteReact(Componente)) return;
        const props = criarPropsPopuladas(entrada, { abrirModais: ehModal(entrada) });
        let resultado;
        await act(async () => {
          resultado = render(<Componente {...props} />, { wrapper: ProvedorTeste });
          await Promise.resolve();
        });
        await flush();
        resultado.unmount();
        await flush();

        const listenersRestantes = [];
        for (const [tipo, listeners] of ativosWindow) if (listeners.size) listenersRestantes.push(`window:${tipo}=${listeners.size}`);
        for (const [tipo, listeners] of ativosDocument) if (listeners.size) listenersRestantes.push(`document:${tipo}=${listeners.size}`);
        expect(listenersRestantes, `Listeners não removidos por ${entrada.file}`).toEqual([]);
        expect([...intervalos], `Intervals não removidos por ${entrada.file}`).toEqual([]);
      } finally {
        addWin.mockRestore();
        remWin.mockRestore();
        addDoc.mockRestore();
        remDoc.mockRestore();
        setIntervalGlobal.mockRestore();
        clearIntervalGlobal.mockRestore();
        setIntervalWindow?.mockRestore();
        clearIntervalWindow?.mockRestore();
      }
    });
  }
});
