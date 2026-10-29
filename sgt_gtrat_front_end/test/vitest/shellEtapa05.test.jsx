import React from "react";
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import { existsSync, readFileSync } from "node:fs";
import { resolve } from "node:path";

import { EstruturaPagina } from "../../src/core/ui/EstruturaPagina.jsx";
import { FluxoEtapas } from "../../src/core/ui/FluxoEtapas.jsx";
import { ModalConfirmacao } from "../../src/core/ui/ModalConfirmacao.jsx";
import { FronteiraErro } from "../../src/core/ui/FronteiraErro.jsx";
import Cabecalho from "../../src/Leiaute/Cabecalho/index.jsx";
import BarraLateral from "../../src/Leiaute/BarraLateral/index.jsx";

vi.mock("../../src/modules/auth/context.jsx", () => ({
  useAutenticacao: () => ({
    user: { nome: "Usuário Teste", perfil: "GESTOR_NACIONAL", autoridade: "PERFIL_GESTOR_NACIONAL" },
    sessionExpiresAtEpochMs: Date.now() + 60_000,
  }),
}));

vi.mock("../../src/core/ui/Homologacao.jsx", () => ({ default: () => null }));

const usuario = {
  nome: "Gestor Teste",
  matricula: "12345678",
  perfil: "GESTOR_NACIONAL",
  autoridade: "PERFIL_GESTOR_NACIONAL",
};

const permissoes = {
  painel: true,
  relatorios: true,
  visaoConsolidadaRelatorios: true,
  frequencia: false,
  ocorrencias: false,
  contratos: true,
  governanca: false,
  empregados: true,
  usuarios: false,
  calendario: true,
  lotacoesTurnos: false,
  estruturaOrganizacional: true,
  auditoria: false,
  cadastrosNacionais: false,
};

const secoes = { rh: true, relatorios: true, config: true, sub_estrutura: true, sub_banco: true, sub_sistema: true };

describe("Etapa 05 - shell visual e acessibilidade base", () => {
  it("separa tokens do CSS global sem ponte de utilitários estilo Tailwind", () => {
    const tokens = readFileSync(resolve("src/styles/tokens.css"), "utf8");
    const global = readFileSync(resolve("src/index.css"), "utf8");
    const main = readFileSync(resolve("src/main.jsx"), "utf8");

    expect(tokens).toContain("--cor-correios-azul-800");
    expect(tokens).toContain("--espaco-4");
    expect(tokens).toContain("--z-modal");
    expect(tokens).toContain("--largura-barra-lateral");
    expect(global).not.toContain(".bg-white");
    expect(global).not.toContain(".text-gray-500");
    expect(global).not.toContain(".rounded-lg");
    expect(existsSync(resolve("src/styles/compatibilidade-legada.css"))).toBe(false);
    expect(main).not.toContain("compatibilidade-legada.css");
    expect(existsSync(resolve("src/modules/auth/themeConfig.jsx"))).toBe(false);
  });

  it("estrutura comum de página cobre loading, vazio, erro e filtros ativos", () => {
    const { rerender } = render(
      <EstruturaPagina titulo="Contratos" carregando filtros={<input aria-label="Número" />} quantidadeFiltrosAtivos={2} aoLimparFiltros={vi.fn()} />,
    );
    expect(screen.getByRole("heading", { name: "Contratos" })).toBeInTheDocument();
    expect(screen.getByText("Carregando dados")).toBeInTheDocument();
    expect(screen.getByText("2 filtros ativos")).toBeInTheDocument();

    rerender(<EstruturaPagina titulo="Contratos" vazio tituloVazio="Sem contratos" />);
    expect(screen.getByText("Sem contratos")).toBeInTheDocument();

    rerender(<EstruturaPagina titulo="Contratos" erro={new Error("Falha recuperável")} aoTentarNovamente={vi.fn()} />);
    expect(screen.getByRole("alert")).toHaveTextContent("Falha recuperável");
    expect(screen.getByRole("button", { name: /tentar novamente/i })).toBeInTheDocument();
  });

  it("stepper expõe etapa atual semanticamente", () => {
    render(<FluxoEtapas etapas={["Prévia", "Confirmação", "Resultado"]} etapaAtual={1} />);
    expect(screen.getByText("Confirmação").closest("li")).toHaveAttribute("aria-current", "step");
    expect(screen.getByText("Prévia").closest("li")).not.toHaveAttribute("aria-current");
  });

  it("modal crítico prende foco e bloqueia fechamento durante envio", async () => {
    let resolver;
    const aoConfirmar = vi.fn(() => new Promise((resolve) => { resolver = resolve; }));
    const aoFechar = vi.fn();
    render(
      <ModalConfirmacao
        isOpen
        title="Anular contrato"
        message="A ação será registrada."
        confirmText="Anular contrato"
        onConfirm={aoConfirmar}
        onClose={aoFechar}
      />,
    );

    const dialogo = screen.getByRole("alertdialog", { name: "Anular contrato" });
    const botaoConfirmar = within(dialogo).getByRole("button", { name: "Anular contrato" });
    const botaoCancelar = within(dialogo).getByRole("button", { name: "Cancelar" });
    await waitFor(() => expect(botaoConfirmar).toHaveFocus());

    fireEvent.keyDown(document, { key: "Tab" });
    expect(botaoCancelar).toHaveFocus();
    fireEvent.keyDown(document, { key: "Tab", shiftKey: true });
    expect(botaoConfirmar).toHaveFocus();

    fireEvent.click(botaoConfirmar);
    expect(dialogo).toHaveAttribute("aria-busy", "true");
    fireEvent.keyDown(document, { key: "Escape" });
    expect(aoFechar).not.toHaveBeenCalled();

    resolver();
    await waitFor(() => expect(aoFechar).toHaveBeenCalledTimes(1));
  });

  it("error boundary mostra identificador de suporte sem expor stack em produção", () => {
    const erroConsole = vi.spyOn(console, "error").mockImplementation(() => {});
    const Quebra = () => {
      const erro = new Error("detalhe técnico sensível");
      erro.requestId = "req-etapa05-123";
      throw erro;
    };

    render(<FronteiraErro><Quebra /></FronteiraErro>);
    expect(screen.getByText("req-etapa05-123")).toBeInTheDocument();
    expect(screen.getByText(/informe o identificador/i)).toBeInTheDocument();
    erroConsole.mockRestore();
  });

  it("controle mobile do cabeçalho declara estado e alvo do menu", () => {
    const setMobileMenu = vi.fn();
    render(
      <Cabecalho
        user={usuario}
        aoSair={vi.fn()}
        onNavigate={vi.fn()}
        setMobileMenu={setMobileMenu}
        menuMobileAberto
      />,
    );
    const botao = screen.getByRole("button", { name: "Fechar menu principal" });
    expect(botao).toHaveAttribute("aria-expanded", "true");
    expect(botao).toHaveAttribute("aria-controls", "navegacao-principal");
  });

  it("desktop e mobile usam o mesmo conjunto de itens calculado por capacidades", () => {
    const props = { usuario, permissoes, onNavigate: vi.fn(), sectionsOpen: secoes, toggleSection: vi.fn(), onCloseMenu: vi.fn() };
    const { unmount } = render(
      <MemoryRouter initialEntries={["/painel"]}>
        <BarraLateral {...props} modoMobile={false} mobileMenu={false} />
      </MemoryRouter>,
    );
    const itensDesktop = screen.getAllByRole("button").map((elemento) => elemento.textContent.trim()).filter(Boolean);
    unmount();

    render(
      <MemoryRouter initialEntries={["/painel"]}>
        <BarraLateral {...props} modoMobile mobileMenu />
      </MemoryRouter>,
    );
    const itensMobile = screen.getAllByRole("button").map((elemento) => elemento.textContent.trim()).filter((texto) => texto && texto !== "");

    for (const rotulo of ["Painel gerencial", "Cadastro de empregados", "Visão consolidada", "Contratos e efetivo", "Meu perfil"]) {
      expect(itensDesktop.join(" ")).toContain(rotulo);
      expect(itensMobile.join(" ")).toContain(rotulo);
    }
    expect(screen.getByRole("complementary", { name: "Menu principal" })).not.toHaveAttribute("aria-hidden", "true");
  });


  it("mantém zoom do navegador habilitado e não persiste avatar dentro do serviço HTTP", () => {
    const html = readFileSync(resolve("index.html"), "utf8");
    const servicoPerfil = readFileSync(resolve("src/services/perfil/ServicoPerfil.js"), "utf8");

    expect(html).toMatch(/name="viewport"[^>]*width=device-width[^>]*initial-scale=1\.0/);
    expect(html).not.toMatch(/user-scalable\s*=\s*["']?no/i);
    expect(html).not.toMatch(/maximum-scale\s*=\s*["']?1(?:\.0)?/i);
    expect(servicoPerfil).not.toContain("localStorage");
    expect(servicoPerfil).not.toContain("sgt_foto_url");
  });

  it("AtualizacaoModulo passou a ser chunk lazy em vez de entrar no shell inicial", () => {
    const aplicacao = readFileSync(resolve("src/Aplicacao.jsx"), "utf8");
    expect(aplicacao).toMatch(/const AtualizacaoModulo = lazy\(\(\) => import\("\.\/modules\/AtualizacaoModulo\/index\.jsx"\)\)/);
    expect(aplicacao).not.toMatch(/^import AtualizacaoModulo/m);
  });
});
