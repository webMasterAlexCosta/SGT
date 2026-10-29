import { beforeEach, describe, expect, it, vi } from "vitest";

const requisitar = vi.hoisted(() => vi.fn());
vi.mock("../../src/core/http.js", () => ({
  requisitar,
  extrairLista: (valor) => Array.isArray(valor) ? valor : valor?.content || [],
  extrairPagina: (valor) => valor?.content ? valor : { content: Array.isArray(valor) ? valor : [], totalElements: 0, totalPages: 0, number: 0, size: 20, first: true, last: true },
}));

import { ServicoPessoa } from "../../src/services/rh/ServicoPessoa.js";
import { ServicoVinculoContratual } from "../../src/services/rh/ServicoVinculoContratual.js";
import { ServicoMotivoDesligamento } from "../../src/services/rh/ServicoMotivoDesligamento.js";
import {
  formatarCpfProtegido,
  prepararCadastroVinculo,
  prepararDesligamento,
  validarCadastroVinculo,
} from "../../src/services/rh/ServicoDominioPessoaVinculo.js";

beforeEach(() => {
  requisitar.mockReset();
  requisitar.mockResolvedValue([]);
});

describe("Etapa 11 - Pessoas e vínculos contratuais", () => {
  it("consome os endpoints canônicos declarados pelo Java", async () => {
    await ServicoPessoa.listar({ busca: "Ana", page: 0 });
    await ServicoPessoa.buscarPorId(1);
    await ServicoPessoa.buscarDadosRestritos(1, "Conferência documental");
    await ServicoPessoa.atualizarDadosOpcionais(1, {});
    await ServicoPessoa.corrigirIdentidade(1, {});
    await ServicoPessoa.listarCorrecoesIdentidade(1);
    await ServicoVinculoContratual.listar({ contratoId: 2 });
    await ServicoVinculoContratual.buscarPorId(3);
    await ServicoVinculoContratual.listarEventos(3);
    await ServicoVinculoContratual.consultarPosicaoPessoa(1, { data: "2026-09-12", conhecidoAte: "2026-09-12T12:00:00Z" });
    await ServicoVinculoContratual.obterOpcoesAlocacaoInicial(2, "2026-09-12");
    await ServicoVinculoContratual.cadastrar({});
    await ServicoVinculoContratual.gerarPreviaRetificacaoInicio(3, {});
    await ServicoVinculoContratual.retificarInicio(3, {});
    await ServicoVinculoContratual.alterarMatricula(3, {});
    await ServicoVinculoContratual.cancelarProgramacao(3, {});
    await ServicoVinculoContratual.desligar(3, {});
    await ServicoVinculoContratual.gerarPreviaRetificacaoDesligamento(3, {});
    await ServicoVinculoContratual.retificarDesligamento(3, {});
    await ServicoVinculoContratual.cancelarDesligamentoProgramado(3, {});
    await ServicoVinculoContratual.tratarFimNatural(2);
    await ServicoMotivoDesligamento.listar();
    await ServicoMotivoDesligamento.buscarPorId(4);
    await ServicoMotivoDesligamento.criar({});
    await ServicoMotivoDesligamento.atualizar(4, {});

    expect(requisitar.mock.calls.map(([config]) => `${config.method} ${config.url}`)).toEqual([
      "GET /pessoas",
      "GET /pessoas/1",
      "GET /pessoas/1/dados-restritos",
      "PUT /pessoas/1/dados-opcionais",
      "POST /pessoas/1/correcoes-identidade",
      "GET /pessoas/1/correcoes-identidade",
      "GET /vinculos-contratuais",
      "GET /vinculos-contratuais/3",
      "GET /vinculos-contratuais/3/eventos",
      "GET /vinculos-contratuais/pessoas/1/posicao",
      "GET /vinculos-contratuais/contratos/2/opcoes-alocacao",
      "POST /vinculos-contratuais",
      "POST /vinculos-contratuais/3/previas/retificacao-inicio",
      "POST /vinculos-contratuais/3/retificar-inicio",
      "POST /vinculos-contratuais/3/alterar-matricula",
      "POST /vinculos-contratuais/3/cancelar-programacao",
      "POST /vinculos-contratuais/3/desligar",
      "POST /vinculos-contratuais/3/previas/retificacao-desligamento",
      "POST /vinculos-contratuais/3/retificar-desligamento",
      "POST /vinculos-contratuais/3/cancelar-desligamento-programado",
      "POST /vinculos-contratuais/contratos/2/tratar-fim-natural",
      "GET /motivos-desligamento",
      "GET /motivos-desligamento/4",
      "POST /motivos-desligamento",
      "PUT /motivos-desligamento/4",
    ]);
  });

  it("não repete mutações críticas e envia finalidade apenas como parâmetro de leitura", async () => {
    await ServicoPessoa.buscarDadosRestritos(8, " auditoria interna ");
    await ServicoVinculoContratual.desligar(9, { dataFim: "2026-09-30" });
    expect(requisitar.mock.calls[0][0]).toMatchObject({ method: "GET", params: { finalidade: "auditoria interna" }, retries: 0 });
    expect(requisitar.mock.calls[1][0]).toMatchObject({ method: "POST", retries: 0 });
    expect(requisitar.mock.calls[1][0].data).not.toHaveProperty("password");
  });

  it("cria vínculo para pessoa existente sem duplicar cadastro", () => {
    expect(prepararCadastroVinculo({
      modoPessoa: "EXISTENTE", pessoaId: "18", contratoId: "7", dataInicio: "2026-10-01", composicaoId: "4",
      pessoa: { nome: "ignorado", cpf: "52998224725" },
    })).toMatchObject({ pessoaId: 18, pessoa: null, contratoId: 7, dataInicio: "2026-10-01", composicaoId: 4 });
  });

  it("cria nova pessoa e normaliza CPF somente no payload de escrita", () => {
    const payload = prepararCadastroVinculo({
      modoPessoa: "NOVA", contratoId: "7", dataInicio: "2026-10-01", composicaoId: "4",
      pessoa: { nome: "Ana Lima", cpf: "529.982.247-25", endereco: {} },
    });
    expect(payload.pessoa).toMatchObject({ nome: "Ana Lima", cpf: "52998224725" });
    expect(formatarCpfProtegido(payload.pessoa.cpf)).toBe("***.982.247-**");
  });

  it("impede CPF estruturalmente inválido antes da transação", () => {
    expect(validarCadastroVinculo({
      modoPessoa: "NOVA", contratoId: "7", dataInicio: "2026-10-01", composicaoId: "4",
      pessoa: { nome: "Ana", cpf: "111.111.111-11", endereco: {} },
    })).toMatch(/CPF válido/i);
  });

  it("exige os quatro elementos de aceite para inclusão excedente", () => {
    const base = {
      modoPessoa: "EXISTENTE", pessoaId: "2", contratoId: "7", dataInicio: "2026-10-01", composicaoId: "4",
      confirmarInclusaoExcedente: true, justificativaExcedente: "Demanda", evidenciaExcedente: "Processo 1", autorizacaoExcedente: "Gestor A",
    };
    expect(validarCadastroVinculo(base, { exigeJustificativaExcedente: true })).toBeNull();
    expect(validarCadastroVinculo({ ...base, autorizacaoExcedente: "" }, { exigeJustificativaExcedente: true })).toMatch(/autorização/i);
  });

  it("mantém último dia e motivo no DTO de desligamento", () => {
    expect(prepararDesligamento({
      dataFim: "2026-10-15", motivoDesligamentoId: "3", justificativa: "Fim", evidencia: "Processo", autorizacao: "Gestor",
      confirmarImpacto: true, tokenPrevia: "tok-atual",
    })).toEqual({
      dataFim: "2026-10-15", motivoDesligamentoId: 3, justificativa: "Fim", evidencia: "Processo", autorizacao: "Gestor",
      confirmarImpacto: true, tokenPrevia: "tok-atual",
    });
  });
});
