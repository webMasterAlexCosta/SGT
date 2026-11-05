// @vitest-environment jsdom
import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, cleanup, fireEvent, render, screen, within } from '@testing-library/react';
import '@testing-library/jest-dom/vitest';

const { requisitar } = vi.hoisted(() => ({ requisitar: vi.fn() }));
vi.mock('../src/core/http.js', () => ({
  requisitar,
  requisitarDownload: vi.fn(),
  extrairLista: value => Array.isArray(value) ? value : value?.content || [],
  extrairPagina: value => value,
}));
vi.mock('../src/modules/auth/context.jsx', () => ({
  useAutenticacao: () => ({ user: { id: 991, authorities: ['PERFIL_GESTOR_LOCAL'], gestaoLocalId: 1 } }),
}));

import { PaginaContratos } from '../src/modules/admin/pages/PaginaContratos/index.jsx';
import * as eventos from '../src/core/events.js';

// Componentes, serviços e diálogo reais. Somente a fronteira HTTP e a identidade
// são controladas. O transporte abaixo resolve mesmo após AbortSignal, como uma
// resposta já recebida; a vigência da intenção também precisa proteger a UI.
const contratoA = {
  id: 101, numeroContrato: 'H02-A', anoContrato: 2026, descricao: 'Contrato anterior A',
  gestaoLocalId: 1, gestaoLocalNome: 'Gestão sintética', estadoNegocial: 'PUBLICADO',
  situacaoTemporal: 'VIGENTE', habilitacaoAdministrativa: 'ATIVO',
  dataInicio: '2026-01-01', dataFim: '2027-01-01', efetivoPrevisto: 9,
  razaoSocialEmpresa: 'Empresa A', cnpjEmpresa: '11111111000191',
};
const contratoB = {
  id: 202, numeroContrato: 'H02-B', anoContrato: 2026, descricao: 'Contrato selecionado B',
  gestaoLocalId: 1, gestaoLocalNome: 'Gestão sintética', estadoNegocial: 'RASCUNHO',
  situacaoTemporal: 'RASCUNHO', habilitacaoAdministrativa: 'ATIVO',
  dataInicio: '2026-04-01', dataFim: '2027-04-01', efetivoPrevisto: 3,
  razaoSocialEmpresa: 'Empresa B', cnpjEmpresa: '22222222000191',
};
const pendente = () => {
  let resolve, reject;
  const promise = new Promise((ok, fail) => { resolve = ok; reject = fail; });
  return { promise, resolve, reject };
};
let respostas, notificar;

beforeEach(() => {
  respostas = new Map();
  notificar = vi.spyOn(eventos, 'notificar'); // observa a fronteira de notificações sem substituir seu efeito
  requisitar.mockReset();
  requisitar.mockImplementation(async ({ method, url }) => {
    if (method !== 'GET') throw new Error(`Mutação não prevista neste teste: ${method} ${url}`);
    if (respostas.has(url)) return respostas.get(url).promise;
    if (url === '/contratos') return [contratoA, contratoB];
    if (url === '/gestoes-locais') return [{ id: 1, nome: 'Gestão sintética', ativo: true }];
    if (url === '/tipos-contratacao') return [];
    if (url === '/contratos/101') return contratoA;
    if (url === '/contratos/202') return contratoB;
    if (url === '/contratos/101/eventos') return [{ id: 1011, tipoEvento: 'PUBLICACAO', motivo: 'Evento anterior A' }];
    if (url === '/contratos/202/eventos') return [{ id: 2021, tipoEvento: 'CRIACAO', motivo: 'Evento atual B' }];
    if (url === '/contratos/101/versoes') return [{ id: 1017, numeroVersao: 7, numeroContrato: 'H02-A', anoContrato: 2026 }];
    if (url === '/contratos/101/eventos/precedencias') return [{ id: 1018, tipoRelacao: 'PRECEDENCIA_A', eventoFonteId: 1011, eventoAlvoId: 1012 }];
    if (url === '/cargos') return { content: [], number: 0, size: 500, totalElements: 0, totalPages: 1, last: true };
    return [];
  });
});
afterEach(() => { cleanup(); vi.restoreAllMocks(); });

async function montar() {
  render(<PaginaContratos />);
  await screen.findAllByRole('button', { name: 'Abrir ciclo de vida do contrato H02-A' });
}
const abrir = numero => fireEvent.click(screen.getAllByRole('button', { name: `Abrir ciclo de vida do contrato ${numero}` })[0]);
const fechar = () => fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Fechar', exact: true }));
const erroAntigo = () => Object.assign(new Error('Falha tardia do contrato anterior'), { status: 500 });

describe('H02: detalhe pertence à seleção ainda vigente', () => {
  it('mantém título, resumo e eventos de B quando o sucesso tardio de A chega depois', async () => {
    const a = pendente(); respostas.set('/contratos/101', a);
    await montar(); abrir('H02-A'); fechar(); abrir('H02-B');
    const detalheB = screen.getByRole('dialog', { name: 'H02-B/2026' });
    await within(detalheB).findByText('Empresa B');
    expect(within(detalheB).getByText('Evento atual B')).toBeInTheDocument();

    await act(async () => { a.resolve(contratoA); });

    const vigente = screen.getByRole('dialog', { name: 'H02-B/2026' });
    expect(within(vigente).getByText('Empresa B')).toBeInTheDocument();
    expect(within(vigente).getByText('Evento atual B')).toBeInTheDocument();
    expect(within(vigente).queryByText('Empresa A')).not.toBeInTheDocument();
    expect(within(vigente).queryByText('Evento anterior A')).not.toBeInTheDocument();
  });

  it('erro e finally antigos não encerram o carregamento nem notificam falha na consulta B', async () => {
    const a = pendente(), b = pendente();
    respostas.set('/contratos/101', a); respostas.set('/contratos/202', b);
    await montar(); abrir('H02-A'); fechar(); abrir('H02-B');
    expect(screen.getByText('Carregando ciclo de vida...')).toBeInTheDocument();

    await act(async () => { a.reject(erroAntigo()); });

    expect(screen.getByText('Carregando ciclo de vida...')).toBeInTheDocument();
    expect(notificar).not.toHaveBeenCalledWith('error', 'Não foi possível carregar o ciclo de vida do contrato.');
    await act(async () => { b.resolve(contratoB); });
    expect(within(screen.getByRole('dialog', { name: 'H02-B/2026' })).getByText('Empresa B')).toBeInTheDocument();
    expect(screen.queryByText('Carregando ciclo de vida...')).not.toBeInTheDocument();
  });

  it('fechar o detalhe invalida seu erro tardio mesmo sem abrir outro contrato', async () => {
    const a = pendente(); respostas.set('/contratos/101', a);
    await montar(); abrir('H02-A'); fechar();
    await act(async () => { a.reject(erroAntigo()); });
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(notificar).not.toHaveBeenCalledWith('error', 'Não foi possível carregar o ciclo de vida do contrato.');
  });

  it('controle: erro da consulta corrente encerra carregamento e informa a falha', async () => {
    const b = pendente(); respostas.set('/contratos/202', b);
    await montar(); abrir('H02-B');
    await act(async () => { b.reject(Object.assign(new Error('Falha atual'), { status: 500 })); });
    expect(screen.getByRole('dialog', { name: 'H02-B/2026' })).toBeInTheDocument();
    expect(screen.queryByText('Carregando ciclo de vida...')).not.toBeInTheDocument();
    expect(notificar).toHaveBeenCalledWith('error', 'Não foi possível carregar o ciclo de vida do contrato.');
  });

  it('falha ao carregar B não mostra eventos, versões ou precedências já carregados de A', async () => {
    await montar(); abrir('H02-A');
    await screen.findByText('Evento anterior A');
    expect(screen.getByText('Versão 7')).toBeInTheDocument();
    expect(screen.getByText('PRECEDENCIA_A')).toBeInTheDocument();
    fechar();
    const b = pendente(); respostas.set('/contratos/202', b);
    abrir('H02-B');
    await act(async () => { b.reject(Object.assign(new Error('Falha atual em B'), { status: 500 })); });
    const detalheB = screen.getByRole('dialog', { name: 'H02-B/2026' });
    expect(within(detalheB).queryByText('Evento anterior A')).not.toBeInTheDocument();
    expect(within(detalheB).queryByText('Versão 7')).not.toBeInTheDocument();
    expect(within(detalheB).queryByText('PRECEDENCIA_A')).not.toBeInTheDocument();
    expect(notificar).toHaveBeenCalledWith('error', 'Não foi possível carregar o ciclo de vida do contrato.');
  });

  it('consulta falha não declara ausência de históricos e fechar/reabrir permite carregar o detalhe', async () => {
    const b = pendente(); respostas.set('/contratos/202', b);
    await montar(); abrir('H02-B');
    await act(async () => { b.reject(Object.assign(new Error('Falha atual em B'), { status: 500 })); });
    const detalheB = screen.getByRole('dialog', { name: 'H02-B/2026' });
    expect(within(detalheB).getByRole('alert')).toHaveTextContent(/Não foi possível carregar/);
    expect(within(detalheB).queryByText('Nenhum evento negocial registrado.')).not.toBeInTheDocument();
    expect(within(detalheB).queryByText('Nenhuma versão registrada.')).not.toBeInTheDocument();
    expect(within(detalheB).queryByText('Nenhuma relação de precedência.')).not.toBeInTheDocument();
    fechar(); respostas.delete('/contratos/202'); abrir('H02-B');
    await screen.findByText('Evento atual B');
    expect(within(screen.getByRole('dialog', { name: 'H02-B/2026' })).queryByRole('alert')).not.toBeInTheDocument();
  });

  it('posição temporal de A recebida depois da troca não é exibida sob o contrato B', async () => {
    const temporalA = pendente(); respostas.set('/contratos/101/posicao-temporal', temporalA);
    await montar(); abrir('H02-A'); await screen.findByText('Evento anterior A');
    fireEvent.submit(screen.getByRole('button', { name: 'Reconstruir posição' }).closest('form'));
    expect(screen.getByRole('button', { name: 'Consultando...' })).toBeDisabled();
    fechar(); abrir('H02-B'); await screen.findByText('Evento atual B');
    await act(async () => { temporalA.resolve({ dataReferencia: '2026-01-15', estadoNegocial: 'PUBLICADO', situacaoTemporal: 'VIGENTE', habilitacaoAdministrativa: 'ATIVO' }); });
    const detalheB = screen.getByRole('dialog', { name: 'H02-B/2026' });
    expect(within(detalheB).queryByText('Verdade reconstruída em 15/01/2026')).not.toBeInTheDocument();
    expect(within(detalheB).getByRole('button', { name: 'Reconstruir posição' })).toBeEnabled();
  });

  it('trocar contrato libera a nova consulta temporal e o finally antigo não encerra seu carregamento', async () => {
    const temporalA = pendente(), temporalB = pendente();
    respostas.set('/contratos/101/posicao-temporal', temporalA);
    respostas.set('/contratos/202/posicao-temporal', temporalB);
    await montar(); abrir('H02-A'); await screen.findByText('Evento anterior A');
    fireEvent.submit(screen.getByRole('button', { name: 'Reconstruir posição' }).closest('form'));
    fechar(); abrir('H02-B'); await screen.findByText('Evento atual B');
    expect(screen.getByRole('button', { name: 'Reconstruir posição' })).toBeEnabled();
    fireEvent.submit(screen.getByRole('button', { name: 'Reconstruir posição' }).closest('form'));
    await act(async () => { temporalA.resolve({ dataReferencia: '2026-01-15', estadoNegocial: 'PUBLICADO' }); });
    expect(screen.getByRole('button', { name: 'Consultando...' })).toBeDisabled();
    await act(async () => { temporalB.resolve({ dataReferencia: '2026-02-17', estadoNegocial: 'RASCUNHO', situacaoTemporal: 'RASCUNHO', habilitacaoAdministrativa: 'ATIVO' }); });
    const detalheB = screen.getByRole('dialog', { name: 'H02-B/2026' });
    expect(within(detalheB).getByText('Verdade reconstruída em 17/02/2026')).toBeInTheDocument();
    expect(within(detalheB).queryByText('Verdade reconstruída em 15/01/2026')).not.toBeInTheDocument();
  });

  it('rejeição temporal após fechar o detalhe não notifica nem escapa como promessa não tratada', async () => {
    const temporalA = pendente(); respostas.set('/contratos/101/posicao-temporal', temporalA);
    await montar(); abrir('H02-A'); await screen.findByText('Evento anterior A');
    fireEvent.submit(screen.getByRole('button', { name: 'Reconstruir posição' }).closest('form'));
    fechar();
    await act(async () => { temporalA.reject(Object.assign(new Error('Falha temporal antiga'), { status: 500 })); });
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(notificar.mock.calls.filter(([tipo]) => tipo === 'error')).toHaveLength(0);
  });

  it('controle: erro temporal corrente informa uma vez e libera nova consulta', async () => {
    const temporalB = pendente(); respostas.set('/contratos/202/posicao-temporal', temporalB);
    await montar(); abrir('H02-B'); await screen.findByText('Evento atual B');
    fireEvent.submit(screen.getByRole('button', { name: 'Reconstruir posição' }).closest('form'));
    await act(async () => { temporalB.reject(Object.assign(new Error('Falha temporal atual'), { status: 500 })); });
    expect(screen.getByRole('button', { name: 'Reconstruir posição' })).toBeEnabled();
    expect(notificar.mock.calls.filter(([tipo]) => tipo === 'error')).toEqual([
      ['error', 'Não foi possível consultar a posição temporal do contrato.'],
    ]);
  });
});
