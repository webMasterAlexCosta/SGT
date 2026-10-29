import React from "react";
import { act, render } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { COMPONENTES_MANIFEST } from "./fixtures/componentesManifest.js";
import {
  PaginaVaziaTeste,
  RespostaVaziaTeste,
  UsuarioAdministradorTeste,
  criarPropsComponente,
} from "./fixtures/ambienteComponentes.jsx";

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
      twoFactorEnabled: false,
      twoFactorVerified: true,
      forceChangePassword: false,
      lotacaoId: 1,
      lotacaoNome: "CTE Teste",
      jornadaId: 1,
      turno: "Turno Teste",
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
  iniciarExportacao: vi.fn(),
  tarefas: [],
  requisitar: vi.fn(),
  medirLatencia: vi.fn(),
  carregarRuntimeConfig: vi.fn(),
  buscarLocalizacaoPorCoordenadas: vi.fn(),
}));

vi.mock("../../src/modules/auth/context.jsx", () => ({
  useAutenticacao: () => estado.auth,
  ProvedorAutenticacao: ({ children }) => <>{children}</>,
}));

vi.mock("../../src/core/exportacao/context.jsx", () => ({
  useExportacaoRelatorio: () => ({
    iniciarExportacao: estado.iniciarExportacao,
    tarefas: estado.tarefas,
  }),
  ProvedorExportacaoRelatorio: ({ children }) => <>{children}</>,
}));

vi.mock("../../src/core/http.js", () => ({
  requisitar: estado.requisitar,
  extrairLista: (valor) => {
    if (Array.isArray(valor)) return valor;
    if (Array.isArray(valor?.content)) return valor.content;
    if (Array.isArray(valor?.dados)) return valor.dados;
    return [];
  },
  extrairPagina: (valor) => {
    if (valor && typeof valor === "object" && Array.isArray(valor.content)) return valor;
    const lista = Array.isArray(valor) ? valor : [];
    return { ...PaginaVaziaTeste, content: lista, totalElements: lista.length };
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

vi.mock("../../src/services/system/ServicoSistema.js", () => ({
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
    Bar: Conteiner,
    BarChart: Conteiner,
    CartesianGrid: Vazio,
    Cell: Vazio,
    LabelList: Vazio,
    Legend: Vazio,
    Line: Conteiner,
    LineChart: Conteiner,
    Pie: Conteiner,
    PieChart: Conteiner,
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

const modulosReact = import.meta.glob("../../src/**/*.jsx");

const caminhosComContextoReal = new Set([
  "src/modules/auth/context.jsx",
  "src/core/exportacao/context.jsx",
]);

const ehTipoComponenteReact = (valor) => {
  if (typeof valor === "function") return true;
  return Boolean(valor && typeof valor === "object" && valor.$$typeof);
};

const errosCriticosConsole = [];
let espiaoConsoleError;

const ehErroCritico = (argumentos) => {
  const texto = argumentos
    .map((item) => (item instanceof Error ? `${item.name}: ${item.message}` : String(item)))
    .join(" ");
  return /(?:TypeError|ReferenceError|RangeError|\bError:|Maximum update depth|Cannot read properties|is not a function|is not defined|Falha na aplicação)/i.test(texto);
};

const ProvedorTeste = ({ children }) => (
  <MemoryRouter initialEntries={["/painel"]}>{children}</MemoryRouter>
);

beforeEach(() => {
  estado.auth.user = { ...UsuarioAdministradorTeste };
  estado.auth.loading = false;
  estado.auth.loggingOut = false;
  estado.auth.sessionExpiresAtEpochMs = Date.now() + 60 * 60 * 1000;
  estado.tarefas = [];
  estado.requisitar.mockResolvedValue(RespostaVaziaTeste);
  estado.medirLatencia.mockResolvedValue(12);
  estado.carregarRuntimeConfig.mockResolvedValue({
    EM_MANUTENCAO: false,
    PREVISAO_RETORNO: "",
    CLIENT_IP: "127.0.0.1",
  });
  estado.buscarLocalizacaoPorCoordenadas.mockResolvedValue({ cidade: "Rio de Janeiro", estado: "RJ" });

  errosCriticosConsole.length = 0;
  espiaoConsoleError = vi.spyOn(console, "error").mockImplementation((...args) => {
    if (ehErroCritico(args)) errosCriticosConsole.push(args);
  });
});

afterEach(() => {
  espiaoConsoleError?.mockRestore();
});

describe("Todos os componentes React públicos - smoke de renderização", () => {
  it("manifesto possui fixtures para os exports públicos atuais", async () => {
    const declarados = new Set(
      COMPONENTES_MANIFEST.map((item) => `${item.path}::${item.exportName}`),
    );
    const descobertos = [];

    for (const [caminho, carregar] of Object.entries(modulosReact)) {
      if (caminho.endsWith("/main.jsx")) continue;
      const modulo = await carregar();
      for (const [nomeExportado, valor] of Object.entries(modulo)) {
        if (!/^[A-Z]/.test(nomeExportado) && nomeExportado !== "default") continue;
        if (!ehTipoComponenteReact(valor)) continue;
        descobertos.push(`${caminho}::${nomeExportado}`);
      }
    }

    const semFixture = descobertos.filter((chave) => !declarados.has(chave));
    expect(semFixture, `Componentes sem fixture:\n${semFixture.join("\n")}`).toEqual([]);
  });

  for (const entrada of COMPONENTES_MANIFEST) {
    if (caminhosComContextoReal.has(entrada.file)) continue;

    it(`${entrada.file} :: ${entrada.localName} renderiza sem quebra imediata`, async () => {
      const carregarModulo = modulosReact[entrada.path];
      expect(carregarModulo, `Módulo não encontrado: ${entrada.path}`).toBeTypeOf("function");

      const modulo = await carregarModulo();
      const Componente = modulo[entrada.exportName];

      // Constantes capitalizadas (ex.: LOGIN_THEMES) são validadas pela suíte de importação,
      // mas não são componentes e portanto não devem ser passadas ao React.
      if (!ehTipoComponenteReact(Componente)) {
        expect(Componente).toBeDefined();
        return;
      }

      const props = criarPropsComponente(entrada);

      await act(async () => {
        render(<Componente {...props} />, { wrapper: ProvedorTeste });
        await Promise.resolve();
        await Promise.resolve();
      });

      expect(
        errosCriticosConsole,
        `Erro crítico emitido por ${entrada.file} :: ${entrada.localName}:\n${errosCriticosConsole
          .map((args) => args.map(String).join(" "))
          .join("\n")}`,
      ).toEqual([]);
    });
  }
});
