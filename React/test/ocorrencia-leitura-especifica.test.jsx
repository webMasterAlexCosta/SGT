import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import '@testing-library/jest-dom/vitest';
import { invalidarGeracaoSessao } from '../src/core/security/geracaoSessao.ts';
const { requisitar, requisitarDownload, auth } = vi.hoisted(() => ({ requisitar: vi.fn(), requisitarDownload: vi.fn(), auth: { user: { id: 1, perfil: 'GESTOR_NACIONAL' } } }));
vi.mock('../src/core/http.ts', async original => ({ ...await original(), requisitar, requisitarDownload }));
vi.mock('../src/modules/auth/context.tsx', () => ({ useAutenticacao: () => auth }));
import { PaginaOcorrencias } from '../src/modules/rh/pages/PaginaOcorrencias/index.tsx';
import { AnexoOcorrencia } from '../src/modules/rh/pages/PaginaOcorrencias/AnexoOcorrencia.tsx';
const diferida = () => { let resolve; const promise = new Promise(a => { resolve = a; }); return { promise, resolve }; };
const historico = (id, texto) => ({ ocorrencia: { id, motivo: 'ATESTADO', dataOcorrencia: '2026-11-09', quantidadeDias: 3, observacao: texto,
  anexos: [{ id: 27, nomeOriginal: 'anexo-sintetico.txt', classificacaoAcesso: 'PESSOAL_RESTRITO' }] }, cancelada: false });
beforeEach(() => {
  invalidarGeracaoSessao(); auth.user = { id: 1, perfil: 'GESTOR_NACIONAL' }; requisitar.mockReset().mockResolvedValue([]); requisitarDownload.mockReset();
  vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {});
  vi.stubGlobal('URL', class extends URL { static createObjectURL = vi.fn(() => 'blob:sintetico'); static revokeObjectURL = vi.fn(); });
});
afterEach(() => { cleanup(); vi.restoreAllMocks(); vi.unstubAllGlobals(); });
const formulario = () => within(screen.getByRole('form', { name: 'Consultar ocorrência por ID' }));
function preencher(id, finalidade = 'Auditoria específica autorizada') {
  const f = formulario(); fireEvent.change(f.getByLabelText('ID da ocorrência'), { target: { value: id } });
  fireEvent.change(f.getByLabelText('Finalidade da consulta'), { target: { value: finalidade } });
  fireEvent.click(f.getByRole('button', { name: 'Consultar ocorrência', exact: true }));
}
describe('Leitura específica e anexo preservam identidade e sessão', () => {
  it('consulta com finalidade pela camada compartilhada e apresenta apenas leitura', async () => {
    requisitar.mockResolvedValue(historico(17, 'Observação sintética autorizada'));
    render(<PaginaOcorrencias />); preencher('17', '  Auditoria específica autorizada  ');
    await screen.findByText('Observação sintética autorizada');
    expect(requisitar).toHaveBeenCalledWith(expect.objectContaining({ method: 'GET', url: '/ocorrencias/17', params: { finalidade: 'Auditoria específica autorizada' }, signal: expect.any(AbortSignal) }));
    const detalhe = within(screen.getByRole('region', { name: 'Ocorrência consultada' }));
    expect(detalhe.getByText('Ocorrência #17')).toBeInTheDocument(); expect(detalhe.queryByRole('button', { name: /Excluir|Alterar/ })).not.toBeInTheDocument();
  });
  it('alterar ID invalida resposta anterior e a troca de sessão limpa a leitura restrita', async () => {
    const a = diferida(), b = diferida(); requisitar.mockImplementation(({ url }) => url.endsWith('/17') ? a.promise : b.promise);
    const view = render(<PaginaOcorrencias />); preencher('17'); preencher('18');
    await act(async () => { b.resolve(historico(18, 'Leitura atual B')); });
    await act(async () => { a.resolve(historico(17, 'Leitura antiga A')); });
    expect(screen.getByText('Leitura atual B')).toBeInTheDocument(); expect(screen.queryByText('Leitura antiga A')).not.toBeInTheDocument();
    act(() => { invalidarGeracaoSessao(); auth.user = { id: 2, perfil: 'GESTOR_NACIONAL' }; }); view.rerender(<PaginaOcorrencias />);
    expect(screen.queryByText('Leitura atual B')).not.toBeInTheDocument();
  });
  it('403 preserva ID/finalidade e apresenta recusa sem conteúdo nem sucesso', async () => {
    requisitar.mockRejectedValue({ status: 403, message: 'Concessão específica ausente ou revogada.' });
    render(<PaginaOcorrencias />); preencher('17');
    expect(await screen.findByRole('alert')).toHaveTextContent('Concessão específica ausente ou revogada.');
    expect(formulario().getByLabelText('ID da ocorrência')).toHaveValue('17');
    expect(formulario().getByLabelText('Finalidade da consulta')).toHaveValue('Auditoria específica autorizada');
    expect(screen.queryByRole('region', { name: 'Ocorrência consultada' })).not.toBeInTheDocument();
  });
  it('recusa ID acima da precisão segura sem emitir requisição', () => {
    render(<PaginaOcorrencias />); preencher('9007199254740992');
    expect(formulario().getByRole('alert')).toHaveTextContent('Informe um ID de ocorrência inteiro positivo válido.'); expect(requisitar).not.toHaveBeenCalled();
  });
  it.each(['sessao', 'desmontagem'])('anexo antigo não inicia download após %s', async tipo => {
    const pendente = diferida(); requisitarDownload.mockReturnValue(pendente.promise);
    const anexo = { id: 27, nomeOriginal: 'anexo-sintetico.txt', classificacaoAcesso: 'PESSOAL_RESTRITO' };
    const view = render(<AnexoOcorrencia anexo={anexo} usuario={auth.user} />);
    fireEvent.click(screen.getByRole('button', { name: /Acessar anexo/ }));
    fireEvent.change(screen.getByLabelText(/Finalidade do acesso/), { target: { value: 'Auditoria autorizada' } });
    fireEvent.click(screen.getByRole('button', { name: 'Baixar anexo' }));
    if (tipo === 'sessao') {
      act(() => { invalidarGeracaoSessao(); auth.user = { id: 2, perfil: 'GESTOR_NACIONAL' }; });
      view.rerender(<AnexoOcorrencia anexo={anexo} usuario={auth.user} />);
    } else view.unmount();
    await act(async () => { pendente.resolve({ blob: new Blob(['anexo privado sintético']), nomeArquivo: anexo.nomeOriginal }); });
    expect(URL.createObjectURL).not.toHaveBeenCalled(); expect(HTMLAnchorElement.prototype.click).not.toHaveBeenCalled();
  });
});
