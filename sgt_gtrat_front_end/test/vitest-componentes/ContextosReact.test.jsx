import React from "react";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({
  autenticar: vi.fn(),
  cadastrar: vi.fn(),
  buscarMeuPerfil: vi.fn(),
  informacoesSessao: vi.fn(),
  encerrarSessao: vi.fn(),
  renovarSessao: vi.fn(),
  validarSenha: vi.fn(),
  solicitarFrequencia: vi.fn(),
  solicitarAbsenteismo: vi.fn(),
  solicitarTerceirizados: vi.fn(),
  consultarExportacao: vi.fn(),
  obterUrlDownload: vi.fn((id) => `/api/relatorios/exportacoes/${id}/arquivo`),
}));

vi.mock("../../src/services/auth/ServicoAutenticacao.js", () => ({
  ServicoAutenticacao: {
    autenticar: mocks.autenticar,
    cadastrar: mocks.cadastrar,
    buscarMeuPerfil: mocks.buscarMeuPerfil,
    informacoesSessao: mocks.informacoesSessao,
    encerrarSessao: mocks.encerrarSessao,
    renovarSessao: mocks.renovarSessao,
    validarSenha: mocks.validarSenha,
  },
}));

vi.mock("../../src/core/exportacao/ServicoExportacaoRelatorio.js", () => ({
  ServicoExportacaoRelatorio: {
    solicitarFrequencia: mocks.solicitarFrequencia,
    solicitarAbsenteismo: mocks.solicitarAbsenteismo,
    solicitarTerceirizados: mocks.solicitarTerceirizados,
    consultar: mocks.consultarExportacao,
    obterUrlDownload: mocks.obterUrlDownload,
  },
}));

import { ProvedorAutenticacao, useAutenticacao } from "../../src/modules/auth/context.jsx";
import { ProvedorExportacaoRelatorio, useExportacaoRelatorio } from "../../src/core/exportacao/context.jsx";

const ConsumidorAuth = () => {
  const contexto = useAutenticacao();
  return (
    <div>
      <span data-testid="loading">{String(contexto.loading)}</span>
      <span data-testid="usuario">{contexto.user?.username || "SEM_USUARIO"}</span>
      <span data-testid="estado-auth">{contexto.estadoAutenticacao}</span>
      <button type="button" onClick={() => contexto.autenticar("12345678", "senha")}>Entrar</button>
    </div>
  );
};

const ConsumidorExportacao = () => {
  const contexto = useExportacaoRelatorio();
  return (
    <div>
      <span data-testid="tarefas">{contexto.tarefas.length}</span>
      <button
        type="button"
        onClick={() => contexto.iniciarExportacao({ tipo: "absenteismo", formato: "PDF", parametros: {}, totalEstimado: 10 })}
      >
        Exportar
      </button>
    </div>
  );
};

describe("Contexto de autenticação - componente real", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
    sessionStorage.clear();
    mocks.encerrarSessao.mockResolvedValue(undefined);
  });

  it("encerra o carregamento sem usuário quando não existe sessão autenticada", async () => {
    mocks.informacoesSessao.mockResolvedValue({ authenticated: false });
    mocks.buscarMeuPerfil.mockResolvedValue(null);

    render(
      <ProvedorAutenticacao>
        <ConsumidorAuth />
      </ProvedorAutenticacao>,
    );

    await waitFor(() => expect(screen.getByTestId("loading")).toHaveTextContent("false"));
    expect(screen.getByTestId("usuario")).toHaveTextContent("SEM_USUARIO");
  });

  it("normaliza e disponibiliza o usuário quando a sessão está autenticada", async () => {
    const usuario = {
      id: 1,
      username: "Gestor Teste",
      matricula: "12345678",
      perfil: "GESTOR_LOCAL",
      autoridade: "PERFIL_GESTOR_LOCAL",
      twoFactorEnabled: false,
      twoFactorVerified: true,
    };
    mocks.informacoesSessao.mockResolvedValue({
      authenticated: true,
      user: usuario,
      session: { remainingSeconds: 1800 },
    });
    mocks.buscarMeuPerfil.mockResolvedValue(usuario);

    render(
      <ProvedorAutenticacao>
        <ConsumidorAuth />
      </ProvedorAutenticacao>,
    );

    await waitFor(() => expect(screen.getByTestId("loading")).toHaveTextContent("false"));
    expect(screen.getByTestId("usuario")).toHaveTextContent("Gestor Teste");
  });

  it("falha fechado quando a sessão não contém perfil e autoridade reconhecidos", async () => {
    mocks.informacoesSessao.mockResolvedValue({
      authenticated: true,
      user: { username: "Legado" },
      session: { remainingSeconds: 1800 },
    });
    mocks.buscarMeuPerfil.mockResolvedValue({ username: "Legado" });

    render(
      <ProvedorAutenticacao>
        <ConsumidorAuth />
      </ProvedorAutenticacao>,
    );

    await waitFor(() => expect(screen.getByTestId("loading")).toHaveTextContent("false"));
    expect(screen.getByTestId("usuario")).toHaveTextContent("SEM_USUARIO");
    expect(screen.getByTestId("estado-auth")).toHaveTextContent("NAO_AUTENTICADO");
  });

  it("desafio de segundo fator não publica usuário antes de um TOTP válido", async () => {
    mocks.informacoesSessao.mockResolvedValue({ authenticated: false });
    mocks.autenticar.mockResolvedValue({ requiresTwoFactor: true });

    render(
      <ProvedorAutenticacao>
        <ConsumidorAuth />
      </ProvedorAutenticacao>,
    );

    await waitFor(() => expect(screen.getByTestId("loading")).toHaveTextContent("false"));
    fireEvent.click(screen.getByRole("button", { name: "Entrar" }));

    await waitFor(() => expect(screen.getByTestId("estado-auth")).toHaveTextContent("SEGUNDO_FATOR_REQUERIDO"));
    expect(screen.getByTestId("usuario")).toHaveTextContent("SEM_USUARIO");
    expect(mocks.buscarMeuPerfil).not.toHaveBeenCalled();
  });
});

describe("Contexto de exportações - componente real", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    sessionStorage.clear();
    mocks.solicitarAbsenteismo.mockResolvedValue({
      id: "exp-1",
      relatorio: "Absenteísmo",
      formato: "PDF",
      status: "PROCESSANDO",
      etapa: "Preparando",
      processados: 0,
      totalEstimado: 10,
      percentual: 0,
      podeBaixar: false,
      falhou: false,
    });
    mocks.consultarExportacao.mockResolvedValue(null);
  });

  it("inicia exportação pelo serviço e registra a tarefa no contexto", async () => {
    render(
      <ProvedorExportacaoRelatorio>
        <ConsumidorExportacao />
      </ProvedorExportacaoRelatorio>,
    );

    expect(screen.getByTestId("tarefas")).toHaveTextContent("0");
    fireEvent.click(screen.getByRole("button", { name: "Exportar" }));

    await waitFor(() => expect(mocks.solicitarAbsenteismo).toHaveBeenCalledTimes(1));
    await waitFor(() => expect(screen.getByTestId("tarefas")).toHaveTextContent("1"));
  });

  it("protege o hook quando usado fora do provedor", () => {
    const ConsumidorInvalido = () => {
      useExportacaoRelatorio();
      return null;
    };

    const consoleError = vi.spyOn(console, "error").mockImplementation(() => {});
    try {
      expect(() => render(<ConsumidorInvalido />)).toThrow(/ProvedorExportacaoRelatorio/);
    } finally {
      consoleError.mockRestore();
    }
  });
});
