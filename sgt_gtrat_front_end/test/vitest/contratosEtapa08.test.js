import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { beforeEach, describe, expect, it, vi } from "vitest";

const requisitar = vi.hoisted(() => vi.fn());
vi.mock("../../src/core/http.js", () => ({
  requisitar,
  extrairLista: (valor) => valor,
}));

import { ServicoContrato } from "../../src/services/admin/ServicoContrato.js";
import {
  filtrarContratos,
  obterAcoesCicloVida,
  prepararContratoRascunho,
} from "../../src/services/admin/ServicoDominioContrato.js";
import {
  AUTORIDADES,
  PERFIS,
  podeGerenciarContratos,
  podeVisualizarContratos,
} from "../../src/core/security/roles.js";

const usuario = (perfil) => ({ perfil, autoridade: AUTORIDADES[perfil] });
const lerFonte = (arquivo) => readFileSync(resolve(process.cwd(), arquivo), "utf8");

beforeEach(() => {
  requisitar.mockReset();
  requisitar.mockResolvedValue([]);
});

describe("Etapa 08 - contratos: núcleo e ciclo de vida", () => {
  it("consome exatamente os 16 endpoints previstos e não antecipa endpoints excepcionais da Etapa 10", async () => {
    await ServicoContrato.listarTodos();
    await ServicoContrato.obterOpcoes();
    await ServicoContrato.buscarPorId(7);
    await ServicoContrato.listarEventos(7);
    await ServicoContrato.listarPrecedencias(7);
    await ServicoContrato.listarVersoes(7);
    await ServicoContrato.consultarPosicaoTemporal(7, { data: "2026-09-11", conhecidoAte: "2026-09-11T12:00:00Z" });
    await ServicoContrato.criar({ numeroContrato: "1" });
    await ServicoContrato.publicar(7, { dataPublicacao: "2026-09-11", referenciaSei: "SEI" });
    await ServicoContrato.atualizar(7, { descricao: "x" });
    await ServicoContrato.inativar(7, { dataEfetiva: "2026-09-11", motivo: "x" });
    await ServicoContrato.reativar(7, { dataEfetiva: "2026-09-11", motivo: "x" });
    await ServicoContrato.encerrarAntecipadamente(7, { dataEfetiva: "2026-09-11", motivo: "x" });
    await ServicoContrato.cancelarFuturo(7, { dataEfetiva: "2026-09-11", motivo: "x" });
    await ServicoContrato.anular(7, { dataEfetiva: "2026-09-11", dataAto: "2026-09-11", motivo: "x" });
    await ServicoContrato.excluir(7);

    expect(requisitar).toHaveBeenCalledTimes(16);
    const pares = requisitar.mock.calls.map(([c]) => `${c.method} ${c.url}`);
    expect(pares).toEqual([
      "GET /contratos",
      "GET /contratos/opcoes",
      "GET /contratos/7",
      "GET /contratos/7/eventos",
      "GET /contratos/7/eventos/precedencias",
      "GET /contratos/7/versoes",
      "GET /contratos/7/posicao-temporal",
      "POST /contratos",
      "POST /contratos/7/publicar",
      "PUT /contratos/7",
      "POST /contratos/7/inativar",
      "POST /contratos/7/reativar",
      "POST /contratos/7/encerrar-antecipadamente",
      "POST /contratos/7/cancelar-futuro",
      "POST /contratos/7/anular",
      "DELETE /contratos/7",
    ]);
    expect(requisitar.mock.calls[6][0].params).toEqual({ data: "2026-09-11", conhecidoAte: "2026-09-11T12:00:00Z" });

    const fonte = lerFonte("src/services/admin/ServicoContrato.js");
    expect(fonte).not.toMatch(/corrigir-anulacao|transferir-gestao|transferencias-gestao/);
  });

  it("permite leitura dos contratos aos seis perfis e escrita apenas ao Gestor Local", () => {
    const perfis = [PERFIS.ADMINISTRADOR, PERFIS.GESTOR_NACIONAL, PERFIS.PRESIDENTE, PERFIS.GESTOR_ESTADUAL, PERFIS.GESTOR_LOCAL, PERFIS.OPERACIONAL];
    for (const perfil of perfis) expect(podeVisualizarContratos(usuario(perfil))).toBe(true);
    expect(podeGerenciarContratos(usuario(PERFIS.GESTOR_LOCAL))).toBe(true);
    for (const perfil of perfis.filter((p) => p !== PERFIS.GESTOR_LOCAL)) {
      expect(podeGerenciarContratos(usuario(perfil))).toBe(false);
    }
  });

  it("mantém rascunho intencionalmente incompleto sem inventar obrigatoriedade de publicação", () => {
    expect(prepararContratoRascunho({ gestaoLocalId: 12 })).toMatchObject({
      numeroContrato: null,
      anoContrato: null,
      descricao: null,
      tipoContratacaoId: null,
      cnpjEmpresa: null,
      razaoSocialEmpresa: null,
      nomeFantasiaEmpresa: null,
      dataInicio: null,
      dataFim: null,
      gestaoLocalId: 12,
      efetivoPrevisto: 0,
    });
  });

  it("separa as ações por estado negocial, situação temporal e habilitação administrativa", () => {
    expect(obterAcoesCicloVida({ estadoNegocial: "RASCUNHO" }, true)).toEqual(["EDITAR", "PUBLICAR", "DESCARTAR"]);
    expect(obterAcoesCicloVida({ estadoNegocial: "PUBLICADO", situacaoTemporal: "VIGENTE", habilitacaoAdministrativa: "ATIVO" }, true)).toEqual(["INATIVAR", "ENCERRAR_ANTECIPADAMENTE", "ANULAR"]);
    expect(obterAcoesCicloVida({ estadoNegocial: "PUBLICADO", situacaoTemporal: "FUTURO", habilitacaoAdministrativa: "ATIVO" }, true)).toContain("CANCELAR_FUTURO");
    expect(obterAcoesCicloVida({ estadoNegocial: "PUBLICADO", situacaoTemporal: "VIGENTE", habilitacaoAdministrativa: "INATIVO" }, true)).toContain("REATIVAR");
    expect(obterAcoesCicloVida({ estadoNegocial: "PUBLICADO", situacaoTemporal: "VIGENTE", habilitacaoAdministrativa: "ATIVO" }, false)).toEqual([]);
  });

  it("filtra independentemente estado negocial, situação temporal e habilitação administrativa", () => {
    const contratos = [
      { id: 1, estadoNegocial: "RASCUNHO", situacaoTemporal: "RASCUNHO", habilitacaoAdministrativa: "ATIVO" },
      { id: 2, estadoNegocial: "PUBLICADO", situacaoTemporal: "VIGENTE", habilitacaoAdministrativa: "ATIVO" },
      { id: 3, estadoNegocial: "PUBLICADO", situacaoTemporal: "FUTURO", habilitacaoAdministrativa: "INATIVO" },
    ];
    expect(filtrarContratos({ contratos, filtroStatus: "RASCUNHOS" }).map((c) => c.id)).toEqual([1]);
    expect(filtrarContratos({ contratos, filtroStatus: "INATIVOS" }).map((c) => c.id)).toEqual([3]);
    expect(filtrarContratos({ contratos, filtroVigencia: "FUTURO" }).map((c) => c.id)).toEqual([3]);
  });

  it("formulário de rascunho usa Tipo de Contratação por ID e não oferece checkbox de ativo", () => {
    const fonte = lerFonte("src/modules/admin/pages/PaginaContratos/components/ModalContrato/index.jsx");
    expect(fonte).toContain("tipoContratacaoId");
    expect(fonte).toContain("cnpjEmpresa");
    expect(fonte).toContain("anoContrato");
    expect(fonte).not.toMatch(/type=["']checkbox["'][^>]*ativo|name=["']ativo["']/i);
  });

  it("detalhe carrega timeline, precedências, versões e posição bitemporal", () => {
    const pagina = lerFonte("src/modules/admin/pages/PaginaContratos/index.jsx");
    for (const chamada of ["listarEventos", "listarPrecedencias", "listarVersoes", "consultarPosicaoTemporal"]) expect(pagina).toContain(chamada);
    const detalhes = lerFonte("src/modules/admin/pages/PaginaContratos/components/ModalDetalhesContrato/index.jsx");
    expect(detalhes).toContain("Estado negocial");
    expect(detalhes).toContain("Situação temporal");
    expect(detalhes).toContain("Habilitação");
    expect(detalhes).toContain("Conhecido até");
  });

  it("timeline distingue data efetiva, data do ato e instante de registro", () => {
    const fonte = lerFonte("src/modules/admin/pages/PaginaContratos/components/ModalDetalhesContrato/index.jsx");
    expect(fonte).toContain("Efetiva:");
    expect(fonte).toContain("Ato:");
    expect(fonte).toContain("Registro:");
    expect(fonte).toContain("evento.dataEfetiva");
    expect(fonte).toContain("evento.dataAto");
    expect(fonte).toContain("evento.registradoEm");
  });

  it("publicação e anulação exigem os dados comprobatórios corretos na UI", () => {
    const fonte = lerFonte("src/modules/admin/pages/PaginaContratos/components/ModalAcaoContrato/index.jsx");
    expect(fonte).toMatch(/PUBLICAR:[\s\S]*exigeReferencia:\s*true/);
    expect(fonte).toMatch(/ANULAR:[\s\S]*exigeDataAto:\s*true[\s\S]*exigeReferencia:\s*true/);
    expect(fonte).toContain("composição original consistente");
  });
});
