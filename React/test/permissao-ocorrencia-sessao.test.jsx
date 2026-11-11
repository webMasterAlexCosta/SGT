import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import '@testing-library/jest-dom/vitest';
import { invalidarGeracaoSessao } from '../src/core/security/geracaoSessao.ts';
const { requisitar, auth } = vi.hoisted(() => ({ requisitar: vi.fn(), auth: { user: { id: 1, perfil: 'ADMINISTRADOR' } } }));
vi.mock('../src/core/http.ts', () => ({ requisitar, extrairLista: r => Array.isArray(r) ? r : [] }));
vi.mock('../src/modules/auth/context.tsx', () => ({ useAutenticacao: () => auth }));
vi.mock('../src/core/events.ts', () => ({ notificar: vi.fn() }));
import { PaginaPermissoesExcepcionais } from '../src/modules/admin/pages/PaginaPermissoesExcepcionais/index.tsx';
const diferida = () => { let resolve; const promise = new Promise(a => { resolve = a; }); return { promise, resolve }; };
const permissao = (id, finalidade) => ({ id, finalidade, tipo: 'ACESSO_DADOS_PESSOAIS_RESTRITOS', ativa: true });
beforeEach(() => {
  invalidarGeracaoSessao(); auth.user = { id: 1, perfil: 'ADMINISTRADOR' }; requisitar.mockReset();
  requisitar.mockImplementation(({ url }) => Promise.resolve(url === '/usuarios'
    ? [{ id: 5, username: 'Beneficiário ativo', active: true }, { id: 6, username: 'Beneficiário inativo', active: false }] : []));
});
afterEach(cleanup);
async function abrir() {
  render(<PaginaPermissoesExcepcionais />);
  await waitFor(() => expect(requisitar).toHaveBeenCalledWith(expect.objectContaining({ url: '/usuarios' })));
  fireEvent.click(screen.getByRole('button', { name: 'Nova permissão' }));
  const form = within(await screen.findByRole('dialog', { name: 'Conceder permissão excepcional' }));
  fireEvent.change(form.getByLabelText('Escopo da concessão'), { target: { value: 'ocorrencia' } });
  return form;
}
describe('Concessão específica e respostas vigentes', () => {
  it('envia somente leitura da ocorrência, beneficiário ativo e finalidade/validade com confirmação real', async () => {
    const form = await abrir();
    expect(form.queryByRole('option', { name: /Beneficiário inativo/ })).not.toBeInTheDocument();
    fireEvent.change(form.getByLabelText('Beneficiário', { exact: true }), { target: { value: '5' } });
    fireEvent.change(form.getByLabelText('Ocorrência (ID)'), { target: { value: '17' } });
    for (const [name, value] of [['Finalidade', 'Leitura sintética específica'], ['Justificativa', 'Prova sintética de contexto'], ['Autorização administrativa', 'AUTORIZACAO-SINTETICA'], ['Validade', '2027-01-01T12:00']])
      fireEvent.change(form.getByLabelText(name, { exact: true }), { target: { value } });
    expect(form.getByText(/somente leitura/i)).toBeInTheDocument();
    fireEvent.click(form.getByRole('button', { name: 'Conceder', exact: true }));
    fireEvent.change(await screen.findByLabelText('Senha de Acesso', { exact: true }), { target: { value: 'senha-sintetica' } });
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar', exact: true }));
    await waitFor(() => expect(requisitar).toHaveBeenCalledWith(expect.objectContaining({ method: 'POST', url: '/permissoes-excepcionais', retries: 0,
      data: { tipo: 'ACESSO_DADOS_PESSOAIS_RESTRITOS', usuarioBeneficiarioId: 5, ocorrenciaId: 17, contratoId: null, gestaoLocalId: null,
        finalidade: 'Leitura sintética específica', justificativa: 'Prova sintética de contexto', autorizacaoAdministrativa: 'AUTORIZACAO-SINTETICA', validoAte: new Date('2027-01-01T12:00').toISOString() } })));
  });
  it('identificador inseguro recebe erro junto ao campo e não abre confirmação', async () => {
    const form = await abrir();
    fireEvent.change(form.getByLabelText('Ocorrência (ID)'), { target: { value: '9007199254740992' } });
    fireEvent.click(form.getByRole('button', { name: 'Conceder', exact: true }));
    expect(form.getByRole('alert')).toHaveTextContent('Informe um ID de ocorrência inteiro positivo válido.');
    expect(screen.queryByLabelText('Senha de Acesso', { exact: true })).not.toBeInTheDocument();
    expect(requisitar.mock.calls.some(([r]) => r.method === 'POST')).toBe(false);
  });
  it('listagem antiga da sessão A não sobrescreve a lista da sessão B', async () => {
    const a = diferida(), b = diferida(); let calls = 0;
    requisitar.mockImplementation(({ url }) => url === '/permissoes-excepcionais' ? (++calls === 1 ? a.promise : b.promise) : Promise.resolve([]));
    const view = render(<PaginaPermissoesExcepcionais />);
    act(() => { invalidarGeracaoSessao(); auth.user = { id: 2, perfil: 'ADMINISTRADOR' }; });
    view.rerender(<PaginaPermissoesExcepcionais />);
    await act(async () => { b.resolve([permissao(2, 'Permissão atual B')]); });
    await act(async () => { a.resolve([permissao(1, 'Permissão obsoleta A')]); });
    expect(screen.getByText('Permissão atual B')).toBeInTheDocument(); expect(screen.queryByText('Permissão obsoleta A')).not.toBeInTheDocument();
  });
  it('consulta antiga de usos não reabre o modal depois de fechar e escolher outra permissão', async () => {
    const a = diferida(), b = diferida();
    requisitar.mockImplementation(({ url }) => Promise.resolve(url === '/permissoes-excepcionais' ? [permissao(1, 'Permissão A'), permissao(2, 'Permissão B')]
      : url.endsWith('/1/usos') ? a.promise : url.endsWith('/2/usos') ? b.promise : []));
    render(<PaginaPermissoesExcepcionais />);
    await screen.findByText('Permissão A');
    const buttons = screen.getAllByRole('button', { name: 'Usos auditados' });
    fireEvent.click(buttons[0]); fireEvent.click(buttons[1]);
    await act(async () => { b.resolve([{ id: 2, finalidade: 'Uso atual B' }]); });
    const modal = screen.getByRole('dialog', { name: 'Usos auditados da permissão' });
    expect(within(modal).getByText('Uso atual B')).toBeInTheDocument();
    fireEvent.keyDown(document, { key: 'Escape' });
    await act(async () => { a.resolve([{ id: 1, finalidade: 'Uso obsoleto A' }]); });
    expect(screen.queryByRole('dialog', { name: 'Usos auditados da permissão' })).not.toBeInTheDocument();
    expect(screen.queryByText('Uso obsoleto A')).not.toBeInTheDocument();
  });
});
