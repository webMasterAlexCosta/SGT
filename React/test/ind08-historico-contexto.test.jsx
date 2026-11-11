// React/serviços reais com transporte controlado. Não é ensaio HTTP/Java/MySQL.
import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import '@testing-library/jest-dom/vitest';
const { requisitar, autenticacao } = vi.hoisted(() => ({
  requisitar: vi.fn(), autenticacao: { user: null },
}));
vi.mock('../src/core/http.ts', () => ({ requisitar, extrairLista: (v) => Array.isArray(v) ? v : v?.content || [] }));
vi.mock('../src/modules/auth/context.tsx', () => ({ useAutenticacao: () => autenticacao }));
import { invalidarGeracaoSessao } from '../src/core/security/geracaoSessao.ts';
import { PaginaHistorico } from '../src/modules/historico/PaginaHistorico.tsx';
import { FormularioProvaHistorica } from '../src/modules/historico/FormularioProvaHistorica.tsx';

const gestor = (id) => ({ id, username: `gestor-sintetico-${id}`, perfil: 'GESTOR_LOCAL', autoridade: 'PERFIL_GESTOR_LOCAL' });
const nome = (valor) => ({ nome: valor, comprovado: true, tipoFonte: 'PROVA_NOME_HISTORICO', fonteId: 81,
  fonteRegistradaEm: '2026-02-01T12:00:00Z', inicioComprovado: '2026-01-01', fimComprovado: '2026-01-31' });
const previa = { tipoCatalogo: 'CARGO', catalogoId: 8, dataEfetiva: '2026-01-01', ativoAnterior: true,
  ativo: false, ativoAtualAposRegistro: false, versaoAtual: 0, tokenPrevia: 'previa-sintetica-A', impactos: ['Impacto da sessão A.'] };
const alvoCatalogo = { tipoCatalogo: 'CARGO', id: 8 };
const prepararConsultaNome = () => {
  fireEvent.click(screen.getByRole('tab', { name: 'Nome' }));
  for (const [rotulo, valor] of [['ID da pessoa', '4'], ['ID do vínculo', '5'],
    ['Data efetiva consultada', '2026-01-01'], ['Conhecido até (ISO com fuso)', '2026-02-02T00:00:00Z']]) {
    fireEvent.change(screen.getByLabelText(rotulo), { target: { value: valor } });
  }
  fireEvent.click(screen.getByRole('button', { name: 'Consultar histórico' }));
};
const preencherFonte = () => {
  for (const [rotulo, valor] of [['Data efetiva da transição', '2026-01-01'], ['Disponibilidade documentada', 'false'],
    ['Versão esperada do catálogo', '0'], ['Motivo / justificativa', 'Fonte sintética de A'],
    ['Evidência da transição', 'SEI-SINTETICO'], ['Autorização', 'AUT-SINTETICA']]) {
    fireEvent.change(screen.getByLabelText(rotulo), { target: { value: valor } });
  }
};
const montarFonte = (aoSucesso) => {
  const view = render(<FormularioProvaHistorica tipo="catalogo" alvo={alvoCatalogo} aoSucesso={aoSucesso} />);
  preencherFonte();
  return view;
};
const aprovarPrevia = async () => {
  fireEvent.click(screen.getByRole('button', { name: 'Gerar prévia' }));
  await screen.findByText('Impacto da sessão A.');
  fireEvent.click(screen.getByRole('checkbox', { name: /Li os impactos/i }));
};
const trocarSessao = () => { invalidarGeracaoSessao(); autenticacao.user = gestor(2); };

beforeEach(() => {
  invalidarGeracaoSessao();
  autenticacao.user = gestor(1);
  requisitar.mockReset();
  requisitar.mockResolvedValue([]);
});
afterEach(() => { cleanup(); invalidarGeracaoSessao(); });

describe('Histórico: respostas e intenções pertencem à sessão que as iniciou', () => {
  it('libera nova consulta após troca de sessão enquanto a página estava ociosa', async () => {
    requisitar.mockImplementation(({ url }) => Promise.resolve(url.endsWith('/nome-historico') ? nome('Nome da consulta nova') : []));
    render(<PaginaHistorico />);
    fireEvent.click(screen.getByRole('tab', { name: 'Nome' }));
    for (const [rotulo, valor] of [['ID da pessoa', '4'], ['ID do vínculo', '5'],
      ['Data efetiva consultada', '2026-01-01'], ['Conhecido até (ISO com fuso)', '2026-02-02T00:00:00Z']]) {
      fireEvent.change(screen.getByLabelText(rotulo), { target: { value: valor } });
    }
    act(trocarSessao);
    fireEvent.click(screen.getByRole('button', { name: 'Consultar histórico' }));
    expect(await screen.findByRole('heading', { name: 'Nome da consulta nova' })).toBeInTheDocument();
  });

  it('descarta consulta nominal de A depois da mudança de sessão e libera consulta de B', async () => {
    let resolverA;
    let consultas = 0;
    requisitar.mockImplementation(({ url }) => !url.endsWith('/nome-historico') ? Promise.resolve([])
      : ++consultas === 1 ? new Promise((resolve) => { resolverA = resolve; }) : Promise.resolve(nome('Nome retornado a B')));
    render(<PaginaHistorico />);
    prepararConsultaNome();
    const sinalA = requisitar.mock.calls.find(([cfg]) => cfg.url.endsWith('/nome-historico'))[0].signal;
    act(trocarSessao);
    await act(async () => resolverA(nome('Nome tardio de A')));
    expect(sinalA.aborted).toBe(true);
    expect(screen.queryByText('Nome tardio de A')).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Consultar histórico' }));
    expect(await screen.findByRole('heading', { name: 'Nome retornado a B' })).toBeInTheDocument();
  });

  it('retira o resultado já exibido quando muda a sessão, mesmo sem desmontar a página', async () => {
    requisitar.mockImplementation(({ url }) => Promise.resolve(url.endsWith('/nome-historico') ? nome('Nome visível de A') : []));
    render(<PaginaHistorico />);
    prepararConsultaNome();
    await screen.findByRole('heading', { name: 'Nome visível de A' });
    act(trocarSessao);
    expect(screen.queryByText('Nome visível de A')).not.toBeInTheDocument();
  });

  it('descarta a prévia de A que termina após a troca da sessão', async () => {
    let resolverA;
    requisitar.mockImplementation(() => new Promise((resolve) => { resolverA = resolve; }));
    montarFonte();
    fireEvent.click(screen.getByRole('button', { name: 'Gerar prévia' }));
    const sinalA = requisitar.mock.calls[0][0].signal;
    act(trocarSessao);
    await act(async () => resolverA(previa));
    expect(sinalA.aborted).toBe(true);
    expect(screen.queryByText('Impacto da sessão A.')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Registrar transição' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Gerar prévia' })).toBeEnabled();
  });

  it('não confirma a intenção de A na sessão B, inclusive antes do próximo render', async () => {
    requisitar.mockImplementation(({ url }) => Promise.resolve(url.endsWith('/previa') ? previa : { eventoId: 99, reenvio: false }));
    montarFonte();
    await aprovarPrevia();
    act(() => {
      trocarSessao();
      fireEvent.submit(screen.getByRole('form', { name: 'Fonte histórica' }));
    });
    await act(async () => {});
    expect(requisitar.mock.calls.filter(([cfg]) => !cfg.url.endsWith('/previa'))).toHaveLength(0);
    expect(screen.queryByText('Impacto da sessão A.')).not.toBeInTheDocument();
  });

  it('não aplica versão de concorrência lida por A ao formulário de B', async () => {
    let resolverA;
    requisitar.mockImplementation(() => new Promise((resolve) => { resolverA = resolve; }));
    montarFonte();
    fireEvent.click(screen.getByRole('button', { name: 'Carregar versões atuais' }));
    const sinalA = requisitar.mock.calls[0][0].signal;
    act(trocarSessao);
    await act(async () => resolverA({ id: 8, version: 99, nome: 'Nome atual de A' }));
    expect(sinalA.aborted).toBe(true);
    expect(screen.getByLabelText('Versão esperada do catálogo')).not.toHaveValue(99);
    expect(screen.getByLabelText('Motivo / justificativa')).toHaveValue('');
    expect(screen.queryByText('Nome atual de A')).not.toBeInTheDocument();
  });

  it('não publica recibo nem sucesso de A depois de invalidada a sessão da confirmação', async () => {
    let resolverA;
    requisitar.mockImplementation(({ url }) => url.endsWith('/previa') ? Promise.resolve(previa)
      : new Promise((resolve) => { resolverA = resolve; }));
    const aoSucesso = vi.fn();
    montarFonte(aoSucesso);
    await aprovarPrevia();
    fireEvent.submit(screen.getByRole('form', { name: 'Fonte histórica' }));
    const sinalA = requisitar.mock.calls[1][0].signal;
    act(trocarSessao);
    await act(async () => resolverA({ eventoId: 99, reenvio: false }));
    expect(sinalA.aborted).toBe(true);
    expect(screen.queryByText('Registrado com sucesso no servidor.')).not.toBeInTheDocument();
    expect(aoSucesso).not.toHaveBeenCalled();
  });

  it('não oferece retry de intenção incerta de A após a sessão mudar', async () => {
    requisitar.mockImplementation(({ url }) => url.endsWith('/previa') ? Promise.resolve(previa)
      : Promise.reject({ status: 0, resultadoIncerto: true, message: 'Resposta sintética perdida.' }));
    montarFonte();
    await aprovarPrevia();
    fireEvent.submit(screen.getByRole('form', { name: 'Fonte histórica' }));
    await screen.findByRole('button', { name: 'Repetir a mesma intenção' });
    act(trocarSessao);
    expect(screen.queryByRole('button', { name: 'Repetir a mesma intenção' })).not.toBeInTheDocument();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });
});

describe('IND-08: contexto do acervo na página real', () => {
  it('HIS-001/HIS-006: conserva D/K e explicita o limite da cadeia lida no conhecimento atual', async () => {
    const versao = { id: 31, numeroVersao: 1, eventoOrigemId: 41, dataEfetiva: '2026-01-01',
      registradoEm: '2026-02-01T12:00:00Z', numeroContrato: 'SINTETICO', anoContrato: 2026,
      descricao: 'Versão do acervo atual', tipoContratacaoId: null, tipoContratacaoDescricao: null,
      fornecedorId: null, cnpjEmpresa: null, razaoSocialEmpresa: null, nomeFantasiaEmpresa: null,
      dataInicio: '2026-01-01', dataFim: '2026-12-31', efetivoPrevisto: 0, gestaoLocalId: null,
      gestaoLocalNome: null, matriculaVinculoObrigatoria: false, matriculaTipoContratacaoObrigatoria: false,
      matriculaVinculoObrigatoriaEfetiva: false, diaSemanaReduzido: null, horasDiaReduzido: null,
      habilitacaoAdministrativa: null, tipoContratacaoComprovado: null };
    requisitar.mockImplementation(({ url }) => Promise.resolve(url.endsWith('/versoes') ? [versao]
      : url.endsWith('/posicao-temporal') ? { contratoId: 9, dataReferencia: '2026-01-01',
        conhecidoAte: '2026-01-02T00:00:00Z', estadoNegocial: 'PUBLICADO', situacaoTemporal: 'VIGENTE',
        habilitacaoAdministrativa: null, vigenciaInicio: '2026-01-01', vigenciaFim: '2026-12-31',
        versaoContratual: null, eventosAplicados: [], relacoesConsideradas: [] } : []));
    render(<PaginaHistorico />);
    for (const [rotulo, valor] of [['ID do contrato', '9'], ['Data efetiva consultada', '2026-01-01'],
      ['Conhecido até (ISO com fuso)', '2026-01-02T00:00:00Z']]) fireEvent.change(screen.getByLabelText(rotulo), { target: { value: valor } });
    fireEvent.click(screen.getByRole('button', { name: 'Consultar histórico' }));
    await screen.findByText('Fotografia contratual desconhecida neste recorte.');
    fireEvent.click(screen.getByText('Publicação original e acervo contratual · conhecimento atual'));
    const tabela = screen.getByRole('table', { name: 'Cadeia completa de fotografias retornadas' });
    fireEvent.click(within(tabela).getByText('Ver registro 31'));
    expect(await within(tabela).findByRole('note', { name: /limites da comprovação histórica/i })).toHaveTextContent(/não permite distinguir/i);
    expect(screen.getByText(/O corte 'conhecido até' não é aplicado a estas composições/i)).toBeInTheDocument();
    expect(screen.getByText(/Publicação original não comprovada/i)).toBeInTheDocument();
    const parametros = Object.fromEntries(requisitar.mock.calls.map(([cfg]) => [cfg.url, cfg.params]));
    expect(parametros['/contratos/9/posicao-temporal']).toEqual({ data: '2026-01-01', conhecidoAte: '2026-01-02T00:00:00Z' });
    expect(parametros['/contratos/9/composicoes']).toEqual({ data: '2026-01-01' });
    expect(parametros['/contratos/9/provas-historicas']).toEqual({ conhecidoAte: '2026-01-02T00:00:00Z' });
    expect(parametros['/contratos/9/versoes']).toBeUndefined();
    expect(parametros['/contratos/9/eventos']).toBeUndefined();
    await waitFor(() => expect(screen.getByText('Fotografia contratual desconhecida neste recorte.')).toBeInTheDocument());
  });
});
