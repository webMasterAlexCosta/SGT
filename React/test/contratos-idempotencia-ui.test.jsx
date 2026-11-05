// @vitest-environment jsdom
import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import '@testing-library/jest-dom/vitest';

const { requisitar, confirmarComSenha } = vi.hoisted(() => ({ requisitar: vi.fn(), confirmarComSenha: vi.fn() }));
vi.mock('../src/core/http.js', () => ({ requisitar, requisitarDownload: vi.fn(), extrairLista: (r) => Array.isArray(r) ? r : r?.content || [] }));
vi.mock('../src/modules/auth/context.jsx', () => ({ useAutenticacao: () => ({ user: { perfil: 'GESTOR_LOCAL', gestaoLocalId: 1 } }) }));
vi.mock('../src/core/security/useConfirmacaoSenha.jsx', () => ({ useConfirmacaoSenha: () => ({ confirmarComSenha, ModalConfirmacao: () => null, fechar: vi.fn() }) }));
vi.mock('../src/core/events.js', () => ({ notificar: vi.fn() }));
import { PainelExcecoesContrato } from '../src/modules/admin/pages/PaginaContratos/components/PainelExcecoesContrato/index.jsx';

beforeEach(() => {
  requisitar.mockReset();
  requisitar.mockImplementation(async (pedido) => {
    if (pedido.method === 'POST') throw { status: 0, resultadoIncerto: true, message: 'Resultado ainda não confirmado.' };
    return [];
  });
  confirmarComSenha.mockReset();
  confirmarComSenha.mockImplementation(async ({ acao }) => { await acao('senha-sintetica'); return true; });
});
afterEach(cleanup);

const montar = () => render(<PainelExcecoesContrato contrato={{ id: 9, estadoNegocial: 'PUBLICADO' }} eventos={[]} podeGerenciar />);
const lerMetadados = (formData) => new Promise((resolve, reject) => {
  const leitor = new FileReader();
  leitor.onload = () => resolve(JSON.parse(leitor.result));
  leitor.onerror = reject;
  leitor.readAsText(formData.get('metadados'));
});
const pedidos = (url) => requisitar.mock.calls.map(([r]) => r).filter((r) => r.method === 'POST' && r.url === url);
const preencherDocumento = () => {
  montar();
  const form = screen.getByRole('button', { name: 'Registrar com confirmação' }).closest('form');
  fireEvent.change(within(form).getByLabelText(/Título/), { target: { value: 'Documento sintético' } });
  fireEvent.change(within(form).getByLabelText(/Referência SEI/), { target: { value: 'SEI-SINTETICO' } });
  return form;
};

describe('Intenção e idempotência dos comandos contratuais suportados pelo Java', () => {
  it('retém a mesma chave documental após resultado incerto e troca quando a intenção muda', async () => {
    const form = preencherDocumento();
    fireEvent.submit(form);
    await waitFor(() => expect(pedidos('/contratos/9/documentos')).toHaveLength(1));
    await waitFor(() => expect(within(form).getByRole('button', { name: 'Registrar com confirmação' })).not.toBeDisabled());
    fireEvent.submit(form);
    await waitFor(() => expect(pedidos('/contratos/9/documentos')).toHaveLength(2));
    const [primeira, segunda] = await Promise.all(pedidos('/contratos/9/documentos').map((r) => lerMetadados(r.data)));
    expect(segunda.chaveIdempotencia).toBe(primeira.chaveIdempotencia);
    expect(within(form).getByLabelText(/Título/)).toHaveValue('Documento sintético');
    await waitFor(() => expect(within(form).getByRole('button', { name: 'Registrar com confirmação' })).not.toBeDisabled());
    fireEvent.change(within(form).getByLabelText(/Título/), { target: { value: 'Nova intenção documentada' } });
    fireEvent.submit(form);
    await waitFor(() => expect(pedidos('/contratos/9/documentos')).toHaveLength(3));
    const terceira = await lerMetadados(pedidos('/contratos/9/documentos')[2].data);
    expect(terceira.chaveIdempotencia).not.toBe(primeira.chaveIdempotencia);
  });

  it('retém a chave da correção do identificador em nova tentativa da mesma intenção', async () => {
    montar();
    fireEvent.click(screen.getByRole('tab', { name: 'Correções' }));
    const form = screen.getByRole('button', { name: 'Corrigir preservando histórico' }).closest('form');
    for (const [nome, valor] of [[/Novo número/, '123'], [/Novo ano/, '2026'], [/Data efetiva/, '2026-10-01'], [/Data do ato/, '2026-10-02'], [/SEI/, 'SEI-SINTETICO'], [/Autorização/, 'AUTORIZACAO-SINTETICA'], [/Motivo/, 'Correção comprovada']]) {
      fireEvent.change(within(form).getByLabelText(nome), { target: { value: valor } });
    }
    fireEvent.submit(form);
    await waitFor(() => expect(pedidos('/contratos/9/correcoes-identificador')).toHaveLength(1));
    await waitFor(() => expect(confirmarComSenha.mock.results[0].value).resolves.toBeDefined());
    fireEvent.submit(form);
    await waitFor(() => expect(pedidos('/contratos/9/correcoes-identificador')).toHaveLength(2));
    const [primeira, segunda] = pedidos('/contratos/9/correcoes-identificador');
    expect(segunda.data.chaveIdempotencia).toBe(primeira.data.chaveIdempotencia);
  });

  it('bloqueia duplo envio documental enquanto o usuário confirma a senha', async () => {
    let resolver;
    confirmarComSenha.mockImplementation(() => new Promise((resolve) => { resolver = resolve; }));
    const form = preencherDocumento();
    fireEvent.submit(form); fireEvent.submit(form);
    expect(confirmarComSenha).toHaveBeenCalledTimes(1);
    await act(async () => resolver(false));
    expect(within(form).getByLabelText(/Título/)).toHaveValue('Documento sintético');
    expect(pedidos('/contratos/9/documentos')).toHaveLength(0);
  });
});
