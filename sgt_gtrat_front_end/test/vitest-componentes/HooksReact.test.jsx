import { act, renderHook } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

const estado = vi.hoisted(() => ({
  user: {
    id: 1,
    perfil: "ADMINISTRADOR",
    autoridade: "PERFIL_ADMINISTRADOR",
    twoFactorEnabled: false,
    twoFactorVerified: false,
  },
}));

vi.mock("../../src/modules/auth/context.jsx", () => ({
  useAutenticacao: () => ({ user: estado.user }),
}));

import { useConfirmacaoSenha } from "../../src/core/security/useConfirmacaoSenha.jsx";

describe("Hooks React - confirmação de senha", () => {
  beforeEach(() => {
    estado.user = {
      id: 1,
      perfil: "ADMINISTRADOR",
    autoridade: "PERFIL_ADMINISTRADOR",
      twoFactorEnabled: false,
      twoFactorVerified: false,
    };
  });

  it("não considera proteção avançada ativa sem segundo fator validado", () => {
    const { result } = renderHook(() => useConfirmacaoSenha());
    expect(result.current.protecaoAvancadaAtiva).toBe(false);
  });

  it("reconhece proteção avançada somente quando 2FA está ativo e validado na sessão", () => {
    estado.user = {
      ...estado.user,
      twoFactorEnabled: true,
      twoFactorVerified: true,
    };

    const { result } = renderHook(() => useConfirmacaoSenha());
    expect(result.current.protecaoAvancadaAtiva).toBe(true);
  });

  it("resolve a confirmação pendente como falsa quando o usuário cancela", async () => {
    const acao = vi.fn();
    const { result } = renderHook(() => useConfirmacaoSenha());

    let promessa;
    act(() => {
      promessa = result.current.confirmarComSenha({
        titulo: "Teste",
        mensagem: "Confirmar ação",
        acao,
      });
    });

    act(() => {
      result.current.fechar();
    });

    await expect(promessa).resolves.toBe(false);
    expect(acao).not.toHaveBeenCalled();
  });
});
