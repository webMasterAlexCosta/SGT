import React from "react";
import { act, fireEvent, render } from "@testing-library/react";
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
  textoConsole,
} from "../fixtures/ambienteAgressivo.jsx";

const estado = vi.hoisted(() => ({
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
  requisitar: vi.fn(),
  medirLatencia: vi.fn(),
  carregarRuntimeConfig: vi.fn(),
  buscarLocalizacaoPorCoordenadas: vi.fn(),
}));

vi.mock("../../../src/modules/auth/context.jsx", () => ({
  useAutenticacao: () => estado.auth,
  ProvedorAutenticacao: ({ children }) => <>{children}</>,
}));

vi.mock("../../../src/core/exportacao/context.jsx", () => ({
  useExportacaoRelatorio: () => ({
    iniciarExportacao: estado.iniciarExportacao,
    tarefas: estado.tarefas,
  }),
  ProvedorExportacaoRelatorio: ({ children }) => <>{children}</>,
}));

vi.mock("../../../src/core/http.js", () => ({
  requisitar: estado.requisitar,
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
    get: vi.fn(async () => RespostaVaziaTeste),
    post: vi.fn(async () => RespostaVaziaTeste),
    put: vi.fn(async () => RespostaVaziaTeste),
    patch: vi.fn(async () => RespostaVaziaTeste),
    delete: vi.fn(async () => RespostaVaziaTeste),
    request: vi.fn(async () => RespostaVaziaTeste),
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
  const Conteiner = ({ children }) => <div data-testid="grafico-mock">{children}</div>;
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
const caminhosComContextoReal = new Set([
  "src/modules/auth/context.jsx",
  "src/core/exportacao/context.jsx",
]);

const ProvedorTeste = ({ children }) => (
  <MemoryRouter initialEntries={["/painel"]}>{children}</MemoryRouter>
);

let errosCriticos = [];
let consoleError;
let consoleWarn;

const registrarSeCritico = (...args) => {
  if (ehErroCriticoConsole(args)) errosCriticos.push(args);
};

const descarregarMicrotarefas = async () => {
  await act(async () => {
    await Promise.resolve();
    await Promise.resolve();
    await new Promise((resolve) => setTimeout(resolve, 0));
  });
};

beforeEach(() => {
  estado.auth.user = { ...UsuarioAdministradorTeste, active: true };
  estado.auth.loading = false;
  estado.auth.loggingOut = false;
  estado.auth.sessionExpiresAtEpochMs = Date.now() + 60 * 60 * 1000;
  estado.tarefas = [];
  estado.requisitar.mockResolvedValue(criarRespostaApiPopulada());
  estado.iniciarExportacao.mockResolvedValue({ id: "exportacao-teste" });
  estado.medirLatencia.mockResolvedValue(12);
  estado.carregarRuntimeConfig.mockResolvedValue({ EM_MANUTENCAO: false, PREVISAO_RETORNO: "", CLIENT_IP: "127.0.0.1" });
  estado.buscarLocalizacaoPorCoordenadas.mockResolvedValue({ cidade: "Rio de Janeiro", estado: "RJ" });

  errosCriticos = [];
  consoleError = vi.spyOn(console, "error").mockImplementation(registrarSeCritico);
  consoleWarn = vi.spyOn(console, "warn").mockImplementation(registrarSeCritico);
});

afterEach(() => {
  consoleError?.mockRestore();
  consoleWarn?.mockRestore();
});

const carregarComponente = async (entrada) => {
  const caminho = `../../../${entrada.file}`;
  const carregar = modulosReact[caminho];
  expect(carregar, `Módulo não encontrado: ${caminho}`).toBeTypeOf("function");
  const modulo = await carregar();
  return modulo[entrada.exportName];
};

const validarConsole = (entrada, etapa) => {
  expect(
    errosCriticos,
    `${etapa} gerou erro crítico em ${entrada.file} :: ${entrada.localName}:\n${errosCriticos.map(textoConsole).join("\n")}`,
  ).toEqual([]);
};

const valorInputTeste = (elemento) => {
  const tipo = String(elemento.type || "text").toLowerCase();
  if (tipo === "date") return "2026-08-20";
  if (tipo === "month") return "2026-08";
  if (tipo === "time") return "08:00";
  if (tipo === "number" || tipo === "range") return "1";
  if (tipo === "email") return "teste@exemplo.invalid";
  if (tipo === "tel") return "21999999999";
  if (tipo === "password") return "SenhaTeste123!";
  return "Teste de interação";
};

const interagirComControles = async (container) => {
  const inputs = [...container.querySelectorAll("input:not([disabled]):not([readonly])")].slice(0, 4);
  for (const input of inputs) {
    if (["checkbox", "radio"].includes(String(input.type).toLowerCase())) fireEvent.click(input);
    else if (String(input.type).toLowerCase() !== "file") fireEvent.change(input, { target: { value: valorInputTeste(input) } });
  }

  const textareas = [...container.querySelectorAll("textarea:not([disabled]):not([readonly])")].slice(0, 2);
  for (const textarea of textareas) fireEvent.change(textarea, { target: { value: "Observação de teste" } });

  const selects = [...container.querySelectorAll("select:not([disabled])")].slice(0, 3);
  for (const select of selects) {
    const opcoes = [...select.options].filter((opcao) => !opcao.disabled);
    if (opcoes.length) fireEvent.change(select, { target: { value: opcoes[Math.min(1, opcoes.length - 1)].value } });
  }

  const botoes = [...container.querySelectorAll("button:not([disabled])")]
    .filter((botao) => !/excluir definitivamente|apagar definitivamente/i.test(`${botao.textContent} ${botao.title} ${botao.getAttribute("aria-label") || ""}`))
    .slice(0, 4);
  for (const botao of botoes) fireEvent.click(botao);

  fireEvent.keyDown(document, { key: "Escape", code: "Escape" });
  await descarregarMicrotarefas();
};

describe("Camada 2A - todos os componentes com dados reais representativos", () => {
  for (const entrada of COMPONENTES_MANIFEST) {
    if (caminhosComContextoReal.has(entrada.file)) continue;

    it(`${entrada.file} :: ${entrada.localName} suporta dados populados e troca de loading`, async () => {
      const Componente = await carregarComponente(entrada);
      if (!ehComponenteReact(Componente)) {
        expect(Componente).toBeDefined();
        return;
      }

      const props = criarPropsPopuladas(entrada, { abrirModais: false, carregando: false });
      let resultado;
      await act(async () => {
        resultado = render(<Componente {...props} />, { wrapper: ProvedorTeste });
        await Promise.resolve();
      });
      await descarregarMicrotarefas();
      validarConsole(entrada, "Renderização populada");

      const propsCarregando = criarPropsPopuladas(entrada, { abrirModais: false, carregando: true });
      await act(async () => {
        resultado.rerender(<Componente {...propsCarregando} />);
        await Promise.resolve();
      });
      await descarregarMicrotarefas();
      validarConsole(entrada, "Transição para loading");
    });
  }
});

describe("Camada 2B - modais realmente abertos", () => {
  for (const entrada of COMPONENTES_MANIFEST.filter(ehModal)) {
    if (caminhosComContextoReal.has(entrada.file)) continue;

    it(`${entrada.file} :: ${entrada.localName} abre com conteúdo sem quebrar`, async () => {
      const Componente = await carregarComponente(entrada);
      if (!ehComponenteReact(Componente)) return;
      const props = criarPropsPopuladas(entrada, { abrirModais: true, carregando: false });
      await act(async () => {
        render(<Componente {...props} />, { wrapper: ProvedorTeste });
        await Promise.resolve();
      });
      await descarregarMicrotarefas();
      validarConsole(entrada, "Modal aberto");
    });
  }
});

describe("Camada 2C - interação automática em componentes públicos", () => {
  for (const entrada of COMPONENTES_MANIFEST) {
    if (caminhosComContextoReal.has(entrada.file)) continue;

    it(`${entrada.file} :: ${entrada.localName} tolera interação básica do usuário`, async () => {
      const Componente = await carregarComponente(entrada);
      if (!ehComponenteReact(Componente)) return;
      const props = criarPropsPopuladas(entrada, { abrirModais: ehModal(entrada), carregando: false });
      let resultado;
      await act(async () => {
        resultado = render(<Componente {...props} />, { wrapper: ProvedorTeste });
        await Promise.resolve();
      });
      await descarregarMicrotarefas();
      await interagirComControles(resultado.container);
      validarConsole(entrada, "Interação");
    });
  }
});

describe("Camada 2D - acessibilidade estrutural mínima", () => {
  for (const entrada of COMPONENTES_MANIFEST) {
    if (caminhosComContextoReal.has(entrada.file)) continue;

    it(`${entrada.file} :: ${entrada.localName} não cria controles anônimos nem IDs duplicados`, async () => {
      const Componente = await carregarComponente(entrada);
      if (!ehComponenteReact(Componente)) return;
      const props = criarPropsPopuladas(entrada, { abrirModais: ehModal(entrada), carregando: false });
      let resultado;
      await act(async () => {
        resultado = render(<Componente {...props} />, { wrapper: ProvedorTeste });
        await Promise.resolve();
      });
      await descarregarMicrotarefas();

      const ids = [...resultado.container.querySelectorAll("[id]")].map((el) => el.id).filter(Boolean);
      const duplicados = [...new Set(ids.filter((id, indice) => ids.indexOf(id) !== indice))];
      expect(duplicados, `IDs duplicados em ${entrada.file}: ${duplicados.join(", ")}`).toEqual([]);

      const botoesAnonimos = [...resultado.container.querySelectorAll("button")].filter((botao) => {
        const nome = `${botao.getAttribute("aria-label") || ""} ${botao.getAttribute("aria-labelledby") || ""} ${botao.title || ""} ${botao.textContent || ""}`.trim();
        return !nome;
      });
      expect(botoesAnonimos.length, `Botões sem nome acessível em ${entrada.file}`).toBe(0);

      const imagensSemAlt = [...resultado.container.querySelectorAll("img")].filter((img) => !img.hasAttribute("alt"));
      expect(imagensSemAlt.length, `Imagens sem atributo alt em ${entrada.file}`).toBe(0);

      const camposSemNome = [...resultado.container.querySelectorAll("input:not([type='hidden']), select, textarea")].filter((campo) => {
        if (campo.getAttribute("aria-label") || campo.getAttribute("aria-labelledby") || campo.title) return false;
        if (campo.id && resultado.container.querySelector(`label[for="${CSS.escape(campo.id)}"]`)) return false;
        if (campo.closest("label")) return false;
        if (campo.placeholder) return false;
        return true;
      });
      expect(camposSemNome.length, `Campos sem rótulo identificável em ${entrada.file}`).toBe(0);
    });
  }
});
