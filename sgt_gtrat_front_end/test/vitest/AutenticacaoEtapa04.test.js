import { beforeEach, describe, expect, it, vi } from "vitest";

const requisitar = vi.hoisted(() => vi.fn());
vi.mock("../../src/core/http.js", () => ({ requisitar }));

import { ServicoAutenticacao } from "../../src/services/auth/ServicoAutenticacao.js";
import {
  CODIGOS_SESSAO_ENCERRADA,
  ESTADOS_AUTENTICACAO,
  estadoAutenticadoPorUsuario,
  mensagemSessaoEncerrada,
} from "../../src/core/security/estadoAutenticacao.js";

beforeEach(() => {
  requisitar.mockReset();
});

describe("Etapa 04 - autenticação e sessão canônicas", () => {
  it("envia somente matrícula, senha e TOTP opcional no login", async () => {
    requisitar.mockResolvedValue({ authenticated: true, perfil: "GESTOR_LOCAL", autoridade: "PERFIL_GESTOR_LOCAL" });
    await ServicoAutenticacao.autenticar("12345678", "senha", "654321");

    expect(requisitar).toHaveBeenCalledTimes(1);
    const chamada = requisitar.mock.calls[0][0];
    expect(chamada.method).toBe("POST");
    expect(chamada.url).toBe("/autenticacao/autenticar");
    expect(Object.keys(chamada.data).sort()).toEqual(["matricula", "password", "totp"]);
    expect(chamada.data).toEqual({ matricula: "12345678", password: "senha", totp: "654321" });
  });

  it("mantém TOTP opcional ausente no primeiro login e respeita o desafio 202 do Java", async () => {
    requisitar.mockResolvedValue({ requiresTwoFactor: true });
    const retorno = await ServicoAutenticacao.autenticar("12345678", "senha");

    expect(requisitar.mock.calls[0][0].data.totp).toBeUndefined();
    expect(retorno.requiresTwoFactor).toBe(true);
    expect(retorno).not.toHaveProperty("canSkipTwoFactor");
  });

  it("não inventa bloqueio no 429 quando o backend não fornece lockout", async () => {
    requisitar.mockRejectedValue({ status: 429, data: { message: "Muitas tentativas." } });
    const retorno = await ServicoAutenticacao.autenticar("12345678", "senha");
    expect(retorno).toMatchObject({ error: true, status: 429, message: "Muitas tentativas.", lockout: null });
  });

  it("separa validação do link e confirmação da recuperação de senha", async () => {
    requisitar.mockResolvedValue({ valid: true });
    await ServicoAutenticacao.validarTokenRecuperacaoSenha("abc");
    await ServicoAutenticacao.confirmarRecuperacaoSenha("abc");

    expect(requisitar.mock.calls[0][0]).toMatchObject({
      method: "GET",
      url: "/autenticacao/recuperar-senha/validar",
      params: { token: "abc" },
    });
    expect(requisitar.mock.calls[1][0]).toMatchObject({
      method: "POST",
      url: "/autenticacao/recuperar-senha/confirmar",
      data: { token: "abc" },
    });
  });


  it("deduplica renovações concorrentes de sessão", async () => {
    let liberar;
    requisitar.mockImplementationOnce(() => new Promise((resolve) => { liberar = resolve; }));

    const primeira = ServicoAutenticacao.renovarSessao("senha");
    const segunda = ServicoAutenticacao.renovarSessao("senha");

    expect(requisitar).toHaveBeenCalledTimes(1);
    expect(primeira).toBe(segunda);
    liberar({ renewed: true });
    await expect(primeira).resolves.toEqual({ renewed: true });

    requisitar.mockResolvedValueOnce({ renewed: true });
    await ServicoAutenticacao.renovarSessao("senha");
    expect(requisitar).toHaveBeenCalledTimes(2);
  });

  it("mapeia estados autenticados e códigos de encerramento conhecidos sem autoridade implícita", () => {
    expect(estadoAutenticadoPorUsuario({ forceChangePassword: true })).toBe(ESTADOS_AUTENTICACAO.SENHA_INICIAL_OBRIGATORIA);
    expect(estadoAutenticadoPorUsuario({ forceChangePassword: false })).toBe(ESTADOS_AUTENTICACAO.AUTENTICADO);
    expect(CODIGOS_SESSAO_ENCERRADA).toContain("SESSION_REPLACED");
    expect(CODIGOS_SESSAO_ENCERRADA).toContain("SESSION_DEVICE_MISMATCH");
    expect(mensagemSessaoEncerrada("SESSION_EXPIRED", "fallback")).toMatch(/expirou/i);
  });
});
