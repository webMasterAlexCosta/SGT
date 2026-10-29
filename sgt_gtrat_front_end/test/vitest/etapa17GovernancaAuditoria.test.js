import { describe, expect, it, vi, beforeEach } from "vitest";

const requisitar = vi.hoisted(() => vi.fn());
const extrairLista = vi.hoisted(() => vi.fn((valor) => valor));
vi.mock("../../src/core/http", () => ({ requisitar, extrairLista }));

import { ServicoGovernancaTerceiros } from "../../src/services/governanca/ServicoGovernancaTerceiros.js";
import { ServicoAuditoria } from "../../src/services/admin/ServicoAuditoria.js";
import { protegerDadosAuditoria, textoJsonProtegidoAuditoria } from "../../src/modules/admin/pages/PaginaRegistrosAuditoria/utils.js";

beforeEach(() => {
  requisitar.mockReset();
  extrairLista.mockClear();
});

describe("Etapa 17 - governança e auditoria", () => {
  it("atualiza fornecedor pelo endpoint Java e preserva confirmação no options", async () => {
    requisitar.mockResolvedValue({ id: 7 });
    await ServicoGovernancaTerceiros.atualizarFornecedor(7, { razaoSocial: "Fornecedor", cnpj: "1", ativo: false }, { senhaConfirmacao: "segredo" });
    expect(requisitar).toHaveBeenCalledWith(expect.objectContaining({
      method: "PUT",
      url: "/governanca-terceiros/fornecedores/7",
      data: { razaoSocial: "Fornecedor", cnpj: "1", ativo: false },
      senhaConfirmacao: "segredo",
    }));
  });

  it("consulta detalhe de auditoria por ID em vez de confiar na projeção da grade", async () => {
    requisitar.mockResolvedValue({ id: 19 });
    await ServicoAuditoria.buscarPorId(19, { silentError: true });
    expect(requisitar).toHaveBeenCalledWith(expect.objectContaining({ method: "GET", url: "/registros-auditoria/19", silentError: true }));
  });

  it("protege segredos e dados pessoais recursivamente sem alterar campos operacionais", () => {
    const protegido = protegerDadosAuditoria({
      nome: "Maria",
      senha: "123",
      cpf: "000",
      contato: { email: "a@b.c", status: "ATIVO" },
      tokenSessao: "abc",
    });
    expect(protegido).toEqual({
      nome: "Maria",
      senha: "[PROTEGIDO]",
      cpf: "[PROTEGIDO]",
      contato: { email: "[PROTEGIDO]", status: "ATIVO" },
      tokenSessao: "[PROTEGIDO]",
    });
    const texto = textoJsonProtegidoAuditoria(JSON.stringify({ password: "x", action: "UPDATE" }));
    expect(texto).toContain("[PROTEGIDO]");
    expect(texto).toContain("UPDATE");
    expect(texto).not.toContain('"x"');
  });
});
