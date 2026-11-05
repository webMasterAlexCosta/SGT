import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import '@testing-library/jest-dom/vitest';
const { requisitar, requisitarDownload, user } = vi.hoisted(() => ({ requisitar: vi.fn(), requisitarDownload: vi.fn(), user: { perfil: 'GESTOR_ESTADUAL' } }));
vi.mock('../src/core/http.js', async (original) => ({ ...(await original()), requisitar, requisitarDownload }));
vi.mock('../src/core/config.js', async (original) => { const m = await original(); return { ...m, CONFIG: { ...m.CONFIG, SEARCH_DEBOUNCE_MS: 0 } }; });
vi.mock('../src/modules/auth/context.jsx', () => ({ useAutenticacao: () => ({ user }) }));
vi.mock('../src/core/security/useConfirmacaoSenha.jsx', () => ({ useConfirmacaoSenha: () => ({ confirmarComSenha: vi.fn(), ModalConfirmacao: () => null }) }));
import { PaginaOcorrencias } from '../src/modules/rh/pages/PaginaOcorrencias/index.jsx';

beforeEach(() => {
  user.perfil = 'GESTOR_ESTADUAL'; requisitar.mockReset(); requisitarDownload.mockReset();
  user.vinculosGestao = [{ lotacao: { id: 7, label: 'Lotação sintética' }, jornada: { id: 8, label: 'Turno sintético' } }];
  requisitar.mockImplementation(async ({ url }) => ['/empregados', '/empregados/filtrados-frequencia'].includes(url) ? [{ id: 1, nome: 'Pessoa sintética' }]
    : [{ id: 7, dataOcorrencia: '2026-01-03', motivo: 'ATESTADO', anexos: [{ id: 15, nomeOriginal: 'atestado.pdf', classificacaoAcesso: 'PESSOAL_RESTRITO' }] }]);
  requisitarDownload.mockResolvedValue({ blob: new Blob(['conteudo sintético']), nomeArquivo: 'atestado.pdf' });
  vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {});
  vi.stubGlobal('URL', class extends URL { static createObjectURL = vi.fn(() => 'blob:sintetico'); static revokeObjectURL = vi.fn(); });
});
afterEach(() => { cleanup(); vi.restoreAllMocks(); vi.unstubAllGlobals(); });

const selecionarPessoa = async () => {
  render(<PaginaOcorrencias />);
  fireEvent.change(screen.getByPlaceholderText('Nome ou CPF...'), { target: { value: 'Pessoa' } });
  fireEvent.click(await screen.findByRole('button', { name: /Pessoa sintética/ }));
  await screen.findByText('atestado.pdf');
  const botao = screen.queryByRole('button', { name: /Acessar anexo.*atestado.pdf/ });
  expect(botao).toBeInTheDocument();
  return botao;
};

describe('Acesso auditado ao anexo da ocorrência', () => {
  it('exige finalidade antes do download restrito e envia o texto informado', async () => {
    fireEvent.click(await selecionarPessoa());
    const dialogo = within(screen.getByRole('dialog', { name: /Acessar anexo/ }));
    expect(dialogo.getByRole('button', { name: 'Baixar anexo' })).toBeDisabled();
    expect(requisitarDownload).not.toHaveBeenCalled();
    fireEvent.change(dialogo.getByLabelText(/Finalidade/), { target: { value: '  Conferência do período  ' } });
    fireEvent.click(dialogo.getByRole('button', { name: 'Baixar anexo' }));
    await waitFor(() => expect(requisitarDownload).toHaveBeenCalledTimes(1));
    expect(requisitarDownload.mock.calls[0][0]).toEqual(expect.objectContaining({ url: '/ocorrencias/anexos/15', params: { finalidade: 'Conferência do período' } }));
  });

  it('apresenta recusa de concessão sem baixar arquivo e conserva a finalidade para correção', async () => {
    requisitarDownload.mockRejectedValue(Object.assign(new Error('Concessão específica ausente'), { status: 403 }));
    fireEvent.click(await selecionarPessoa());
    fireEvent.change(screen.getByLabelText(/Finalidade/), { target: { value: 'Auditoria do ato' } });
    fireEvent.click(screen.getByRole('button', { name: 'Baixar anexo' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Concessão específica ausente');
    expect(screen.getByLabelText(/Finalidade/)).toHaveValue('Auditoria do ato');
    expect(URL.createObjectURL).not.toHaveBeenCalled();
  });

  it('respeita a regra Java que dispensa finalidade para Gestor Local', async () => {
    user.perfil = 'GESTOR_LOCAL';
    fireEvent.click(await selecionarPessoa());
    expect(screen.getByRole('button', { name: 'Baixar anexo' })).toBeEnabled();
  });

  it('não oferece acesso ao anexo pessoal restrito para Operacional', async () => {
    user.perfil = 'OPERACIONAL';
    expect(await selecionarPessoa()).toBeDisabled();
    expect(requisitarDownload).not.toHaveBeenCalled();
  });
});
