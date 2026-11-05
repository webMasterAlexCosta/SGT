import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import '@testing-library/jest-dom/vitest';
import { prepararContratoRascunho } from '@sgt/services/admin/ServicoDominioContrato.js';
import { PaginaContratos } from '@sgt/modules/admin/pages/PaginaContratos/index.jsx';
import { api } from '@sgt/core/http.js';
import { configurarBuscadorTokenCsrf, resetarCoordenadorCsrfParaTestes } from '@sgt/core/http/coordenadorCsrf.js';
import { resetarSolicitadorConfirmacaoSenhaParaTestes } from '@sgt/core/http/coordenadorConfirmacaoSenha.js';
import { invalidarGeracaoSessao } from '@sgt/core/security/geracaoSessao.js';
import { redefinirIndicadorRedirecionamento } from '@sgt/core/events.js';

// UI, builder, serviço, confirmação e interceptores reais; só o transporte e a
// identidade sintética são controlados. O endpoint real é verificado à parte.
vi.mock('@sgt/modules/auth/context.jsx', () => ({
  useAutenticacao: () => ({ user: { id: 991, authorities: ['PERFIL_GESTOR_LOCAL'], gestaoLocalId: 1 } }),
}));

const contract = {
  id: 11, numeroContrato: 'C25-SINTETICO', anoContrato: 2026,
  descricao: 'Descrição anterior', tipoContratacaoId: 7, tipoContratacaoDescricao: 'Tipo C25',
  fornecedorId: 901, cnpjEmpresa: '11222333000144', razaoSocialEmpresa: 'Empresa C25 sintética',
  nomeFantasiaEmpresa: '', dataInicio: '2026-11-01', dataFim: '2027-11-01',
  efetivoPrevisto: 10, gestaoLocalId: 1, gestaoLocalNome: 'Gestão C25',
  estadoNegocial: 'RASCUNHO', situacaoTemporal: 'RASCUNHO', habilitacaoAdministrativa: 'ATIVO', ativo: true,
};
const ok = (config, data) => ({ config, data, status: 200, statusText: 'OK', headers: {} });
let originalAdapter, writes;

beforeEach(() => {
  originalAdapter = api.defaults.adapter;
  writes = [];
  invalidarGeracaoSessao();
  resetarCoordenadorCsrfParaTestes();
  resetarSolicitadorConfirmacaoSenhaParaTestes();
  redefinirIndicadorRedirecionamento();
  api.defaults.adapter = async config => {
    const method = config.method.toUpperCase();
    if (config.url === '/autenticacao/csrf') return ok(config, { headerName: 'X-XSRF-TOKEN', parameterName: '_csrf', token: 'synthetic-c25' });
    if (method === 'GET' && config.url === '/contratos') return ok(config, [{ ...contract }]);
    if (method === 'GET' && config.url === '/gestoes-locais') return ok(config, [{ id: 1, nome: 'Gestão C25', ativo: true }]);
    if (method === 'GET' && config.url === '/tipos-contratacao') return ok(config, [{ id: 7, descricao: 'Tipo C25', ativo: true }]);
    if (method === 'PUT' && config.url === '/contratos/11') {
      const data = JSON.parse(config.data);
      writes.push({ method, url: config.url, data });
      return ok(config, { ...contract, ...data });
    }
    throw new Error(`Unexpected request ${method} ${config.url}`);
  };
  configurarBuscadorTokenCsrf(async ({ geracaoSessao }) => api.request({ method: 'GET', url: '/autenticacao/csrf', retries: 0, silentError: true, skipAuthLogout: true, __csrfBootstrap: true, __geracaoSessao: geracaoSessao }));
});

afterEach(() => {
  cleanup();
  api.defaults.adapter = originalAdapter;
  resetarCoordenadorCsrfParaTestes();
  resetarSolicitadorConfirmacaoSenhaParaTestes();
  vi.restoreAllMocks();
});

describe('C25: edição de rascunho conserva a associação canônica', () => {
  it.each([901, '901'])('serializa o fornecedor recebido %s ao alterar somente a descrição', fornecedorId => {
    const payload = prepararContratoRascunho({ ...contract, fornecedorId, descricao: 'Descrição atualizada' }, 1);
    expect(payload).toMatchObject({ id: 11, fornecedorId: 901, descricao: 'Descrição atualizada', cnpjEmpresa: contract.cnpjEmpresa });
  });

  it('rascunho sem fornecedor não inventa uma associação a partir do CNPJ', () => {
    const payload = prepararContratoRascunho({ ...contract, id: undefined, fornecedorId: null }, 1);
    expect(payload.fornecedorId ?? null).toBeNull();
    expect(payload.cnpjEmpresa).toBe(contract.cnpjEmpresa);
  });

  it('preserva a intenção explícita de remover o fornecedor com null', () => {
    const payload = prepararContratoRascunho({ ...contract, fornecedorId: null }, 1);
    expect(payload.fornecedorId).toBeNull();
  });

  it('editar a descrição pela página e confirmar a senha envia o mesmo fornecedor no PUT', async () => {
    render(<PaginaContratos />);
    const editButtons = await screen.findAllByRole('button', { name: 'Editar rascunho C25-SINTETICO' });
    fireEvent.click(editButtons[0]);
    const form = screen.getByRole('dialog', { name: 'Editar rascunho' });
    fireEvent.change(within(form).getByRole('textbox', { name: /^Descrição/ }), { target: { value: 'Descrição atualizada' } });
    fireEvent.click(within(form).getByRole('button', { name: 'Salvar rascunho' }));
    const confirmation = await screen.findByRole('dialog', { name: 'Confirmar alteração do rascunho' });
    fireEvent.change(within(confirmation).getByLabelText('Senha de Acesso'), { target: { value: 'x'.repeat(8) } });
    fireEvent.click(within(confirmation).getByRole('button', { name: 'Confirmar', exact: true }));
    await waitFor(() => expect(writes).toHaveLength(1));
    expect(writes[0]).toMatchObject({ method: 'PUT', url: '/contratos/11', data: {
      fornecedorId: 901, cnpjEmpresa: contract.cnpjEmpresa, descricao: 'Descrição atualizada',
    } });
  });
});
