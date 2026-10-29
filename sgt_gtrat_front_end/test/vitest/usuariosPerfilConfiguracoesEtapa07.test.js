import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { beforeEach, describe, expect, it, vi } from "vitest";

const requisitar = vi.hoisted(() => vi.fn());
vi.mock("../../src/core/http.js", () => ({
  requisitar,
  extrairLista: (valor) => valor,
}));

import { ServicoConfiguracaoSistema } from "../../src/services/admin/ServicoConfiguracaoSistema.js";
import { ServicoPerfil } from "../../src/services/perfil/ServicoPerfil.js";
import { ServicoUsuario } from "../../src/services/admin/ServicoUsuario.js";
import { ServicoAutenticacao } from "../../src/services/auth/ServicoAutenticacao.js";
import {
  AUTORIDADES,
  PERFIS,
  podeGerenciarConfiguracoesSistema,
  podeGerenciarUsuarios,
} from "../../src/core/security/roles.js";
import { SENHA_MINIMA_UI, SENHA_MAXIMA_UI } from "../../src/core/security/politicaSenha.js";

const usuario = (perfil) => ({ perfil, autoridade: AUTORIDADES[perfil] });

beforeEach(() => {
  requisitar.mockReset();
  requisitar.mockResolvedValue([]);
});

describe("Etapa 07 - usuários, perfil e configurações administrativas", () => {
  it("restringe configurações do sistema ao Administrador técnico", () => {
    expect(podeGerenciarConfiguracoesSistema(usuario(PERFIS.ADMINISTRADOR))).toBe(true);
    for (const perfil of [PERFIS.GESTOR_NACIONAL, PERFIS.PRESIDENTE, PERFIS.GESTOR_ESTADUAL, PERFIS.GESTOR_LOCAL, PERFIS.OPERACIONAL]) {
      expect(podeGerenciarConfiguracoesSistema(usuario(perfil))).toBe(false);
    }
  });

  it("mantém gestão de usuários somente nos quatro perfis autorizados pelo matcher Java", () => {
    expect(podeGerenciarUsuarios(usuario(PERFIS.ADMINISTRADOR))).toBe(true);
    expect(podeGerenciarUsuarios(usuario(PERFIS.GESTOR_NACIONAL))).toBe(true);
    expect(podeGerenciarUsuarios(usuario(PERFIS.GESTOR_ESTADUAL))).toBe(true);
    expect(podeGerenciarUsuarios(usuario(PERFIS.GESTOR_LOCAL))).toBe(true);
    expect(podeGerenciarUsuarios(usuario(PERFIS.PRESIDENTE))).toBe(false);
    expect(podeGerenciarUsuarios(usuario(PERFIS.OPERACIONAL))).toBe(false);
  });

  it("consome os dois endpoints administrativos de configuração sem confirmação de senha inventada", async () => {
    await ServicoConfiguracaoSistema.buscarConfiguracoes();
    await ServicoConfiguracaoSistema.salvarConfiguracoes([{ chave: "HORAS_DIA_TRABALHO", valor: "8" }]);
    expect(requisitar.mock.calls[0][0]).toMatchObject({ method: "GET", url: "/sistema/configuracoes" });
    expect(requisitar.mock.calls[1][0]).toMatchObject({ method: "POST", url: "/sistema/configuracoes" });
    expect(requisitar.mock.calls[1][0].senhaConfirmacao).toBeUndefined();
  });

  it("cobre os sete comandos diretos de Perfil e mantém validar-senha no controlador correto", async () => {
    await ServicoPerfil.buscarStatusSegundoFator();
    await ServicoPerfil.configurarSegundoFator("atual");
    await ServicoPerfil.ativarSegundoFator("123456");
    await ServicoPerfil.desativarSegundoFator("atual", "123456");
    await ServicoPerfil.alterarSenha("atual", "novaSenha", "123456");
    await ServicoPerfil.validarSenha("atual");
    await ServicoPerfil.enviarAvatar(new File(["x"], "avatar.jpg", { type: "image/jpeg" }));

    const pares = requisitar.mock.calls.map(([config]) => `${config.method} ${config.url}`);
    expect(pares).toEqual([
      "GET /perfil/2fa/status",
      "POST /perfil/2fa/configurar",
      "POST /perfil/2fa/ativar",
      "DELETE /perfil/2fa",
      "POST /perfil/alterar-senha",
      "POST /perfil/validar-senha",
      "POST /perfil/avatar",
    ]);
    expect(requisitar.mock.calls.at(-1)[0].data).toBeInstanceOf(FormData);
  });

  it("mantém os oito comandos administrativos de usuário e meu-perfil canônico", async () => {
    await ServicoUsuario.listarTodos();
    await ServicoUsuario.criar({ username: "u" }, { senhaConfirmacao: "s" });
    await ServicoUsuario.atualizar(10, { email: "u@correios.com.br" }, { senhaConfirmacao: "s" });
    await ServicoUsuario.redefinirSenha(10, { newPassword: "12345678" }, { senhaConfirmacao: "s" });
    await ServicoUsuario.ativar(10, { senhaConfirmacao: "s" });
    await ServicoUsuario.excluir(10, { senhaConfirmacao: "s" });
    await ServicoUsuario.concederAcessoRetroativo(10, { senhaConfirmacao: "s" });
    await ServicoUsuario.revogarAcessoRetroativo(10, { senhaConfirmacao: "s" });
    await ServicoAutenticacao.buscarMeuPerfil();

    const pares = requisitar.mock.calls.map(([config]) => `${config.method} ${config.url}`);
    expect(pares).toContain("GET /usuarios");
    expect(pares).toContain("POST /usuarios");
    expect(pares).toContain("PUT /usuarios/10");
    expect(pares).toContain("POST /usuarios/10/redefinir-senha");
    expect(pares).toContain("POST /usuarios/10/ativar");
    expect(pares).toContain("DELETE /usuarios/10");
    expect(pares).toContain("POST /usuarios/10/conceder-acesso-retroativo");
    expect(pares).toContain("POST /usuarios/10/revogar-acesso-retroativo");
    expect(pares).toContain("GET /usuarios/meu-perfil");
  });

  it("usa a política de senha do Java de referência apenas como dica de UX", () => {
    expect(SENHA_MINIMA_UI).toBe(8);
    expect(SENHA_MAXIMA_UI).toBe(72);
  });

  it("edita somente as três chaves aceitas pelo ConfiguracaoSistemaServico atual", () => {
    const fonte = readFileSync(resolve(process.cwd(), "src/modules/admin/pages/PaginaConfiguracoesSistema/index.jsx"), "utf8");
    for (const chave of ["HORAS_DIA_TRABALHO", "HORA_INICIO_NOTURNO", "MINUTOS_HORA_NOTURNA"]) {
      expect(fonte).toContain(chave);
    }
    expect(fonte).not.toContain('chave: "HORA_FIM_NOTURNO"');
    expect(fonte).not.toContain("localStorage");
  });

  it("revalida perfil e sessão quando a própria conta é alterada", () => {
    const fonte = readFileSync(resolve(process.cwd(), "src/modules/admin/pages/PaginaUsuarios/index.jsx"), "utf8");
    expect(fonte).toContain("propriaContaEmEdicao");
    expect(fonte).toContain("await atualizarPerfil()");
    expect(fonte).toContain("await atualizarInformacoesSessao({ touch: false, silentError: true })");
  });

  it("não persiste senha temporária, confirmação de senha ou configuração administrativa no Web Storage", () => {
    const fonteUsuarios = readFileSync(resolve(process.cwd(), "src/modules/admin/pages/PaginaUsuarios/index.jsx"), "utf8");
    const fonteConfiguracoes = readFileSync(resolve(process.cwd(), "src/modules/admin/pages/PaginaConfiguracoesSistema/index.jsx"), "utf8");
    expect(fonteUsuarios).not.toMatch(/localStorage|sessionStorage/);
    expect(fonteConfiguracoes).not.toMatch(/localStorage|sessionStorage/);
  });
});
