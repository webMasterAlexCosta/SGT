// Auditoria independente: regressões adversariais sobre a cópia isolada da candidata03.
import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, cleanup, fireEvent, render, screen, within } from '@testing-library/react';
import '@testing-library/jest-dom/vitest';
const { requisitar } = vi.hoisted(() => ({ requisitar: vi.fn() }));
vi.mock('../src/core/http.js', () => ({ requisitar, requisitarDownload: vi.fn(), extrairLista: r => Array.isArray(r) ? r : [] }));
vi.mock('../src/modules/auth/context.jsx', () => ({ useAutenticacao: () => ({ user: { perfil: 'ADMINISTRADOR', autoridade: 'PERFIL_ADMINISTRADOR' } }) }));
vi.mock('../src/core/events.js', () => ({ notificar: vi.fn() }));
import { PainelExcecoesContrato } from '../src/modules/admin/pages/PaginaContratos/components/PainelExcecoesContrato/index.jsx';
const documentos = [
  { id: 1, titulo: 'Documento A', classificacaoAcesso: 'PESSOAL_RESTRITO', estado: 'PUBLICADO' },
  { id: 2, titulo: 'Documento B', classificacaoAcesso: 'PESSOAL_RESTRITO', estado: 'PUBLICADO' },
];
const abrir = async id => {
  const titulo = await screen.findByText(`Documento ${id === 1 ? 'A' : 'B'}`);
  fireEvent.click(within(titulo.closest('article')).getByRole('button', { name: 'Detalhes' }));
  return screen.findByRole('heading', { name: `Documento ${id === 1 ? 'A' : 'B'} · cadeia documental` });
};
beforeEach(() => { requisitar.mockReset(); });
afterEach(cleanup);
describe('Revisão documental: a seleção precisa governar todas as respostas', () => {
  it('não mostra permissões de A sob o título de B quando A responde por último', async () => {
    let concluirPermissoesA;
    requisitar.mockImplementation(({ url }) => {
      if (url === '/contratos/9/documentos') return Promise.resolve(documentos);
      if (url === '/contratos/9/documentos/1') return Promise.resolve(documentos[0]);
      if (url === '/contratos/9/documentos/2') return Promise.resolve(documentos[1]);
      if (url.endsWith('/documentos/1/permissoes')) return new Promise(resolve => { concluirPermissoesA = resolve; });
      if (url.endsWith('/documentos/2/permissoes')) return Promise.resolve([{ id: 22, usuarioBeneficiarioUsername: 'Leitor exclusivo B', finalidade: 'Fonte B', ativo: true }]);
      return Promise.resolve([]);
    });
    render(<PainelExcecoesContrato contrato={{ id: 9, estadoNegocial: 'PUBLICADO' }} />);
    await abrir(1);
    await abrir(2);
    await screen.findByText(/Leitor exclusivo B/);
    await act(async () => concluirPermissoesA([{ id: 11, usuarioBeneficiarioUsername: 'Leitor exclusivo A', finalidade: 'Fonte A', ativo: true }]));
    const painelB = screen.getByRole('heading', { name: 'Documento B · cadeia documental' }).parentElement;
    expect(painelB).not.toHaveTextContent('Leitor exclusivo A');
    expect(painelB).toHaveTextContent('Leitor exclusivo B');
  });
  it('expõe a recusa das permissões e acessos em vez de transformá-la em listas vazias', async () => {
    requisitar.mockImplementation(({ url }) => {
      if (url === '/contratos/9/documentos') return Promise.resolve(documentos);
      if (url === '/contratos/9/documentos/1') return Promise.resolve(documentos[0]);
      if (url.endsWith('/permissoes') || url.endsWith('/acessos')) return Promise.reject({ status: 403, message: 'Leitura de permissões recusada.' });
      return Promise.resolve([]);
    });
    render(<PainelExcecoesContrato contrato={{ id: 9, estadoNegocial: 'PUBLICADO' }} />);
    await abrir(1);
    await act(async () => {});
    expect(screen.queryByRole('alert')).toHaveTextContent(/recusada|não foi possível/i);
  });
});
