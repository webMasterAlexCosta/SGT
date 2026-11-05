import React from 'react';
import { act, cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import '@testing-library/jest-dom/vitest';
import { PaginaEmpresasContratadas } from '@sgt/modules/admin/pages/PaginaEmpresasContratadas/index.jsx';
import { api } from '@sgt/core/http.js';
import { configurarBuscadorTokenCsrf, resetarCoordenadorCsrfParaTestes } from '@sgt/core/http/coordenadorCsrf.js';
import { resetarSolicitadorConfirmacaoSenhaParaTestes } from '@sgt/core/http/coordenadorConfirmacaoSenha.js';
import { invalidarGeracaoSessao } from '@sgt/core/security/geracaoSessao.js';
import { EVENT_SHOW_TOAST, redefinirIndicadorRedirecionamento } from '@sgt/core/events.js';

const { identity } = vi.hoisted(() => ({ identity: { user: null } }));
vi.mock('@sgt/modules/auth/context.jsx', () => ({ useAutenticacao: () => identity }));
const company = { id: 901, razaoSocial: 'Empresa C25 sintética', cnpj: '11222333000144', email: null, ativo: true };
const draft = { id: 11, numeroContrato: 'Contrato C25', ativo: true, estadoNegocial: 'RASCUNHO' };
const ok = (config, data) => ({ config, data, status: 200, statusText: 'OK', headers: {} });
const networkError = config => Promise.reject(Object.assign(new Error('Network Error'), { config, code: 'ERR_NETWORK', request: {} }));
let originalAdapter, companies, contracts, sends, notifications, custom;
const onToast = event => notifications.push(event.detail);
const mutations = () => sends.filter(item => ['POST', 'PUT'].includes(item.method));

beforeEach(() => {
  identity.user = { id: 991, authorities: ['PERFIL_GESTOR_LOCAL'] };
  originalAdapter = api.defaults.adapter;
  companies = [];
  contracts = [{ ...draft }];
  sends = [];
  notifications = [];
  custom = null;
  window.addEventListener(EVENT_SHOW_TOAST, onToast);
  invalidarGeracaoSessao();
  resetarCoordenadorCsrfParaTestes();
  resetarSolicitadorConfirmacaoSenhaParaTestes();
  redefinirIndicadorRedirecionamento();
  api.defaults.adapter = async config => {
    const request = { method: config.method.toUpperCase(), url: config.url, signal: config.signal };
    sends.push(request);
    const special = custom?.(config, request);
    if (special !== undefined) return special;
    if (config.url === '/autenticacao/csrf') return ok(config, { headerName: 'X-XSRF-TOKEN', parameterName: '_csrf', token: 'synthetic-c25' });
    if (request.method === 'GET' && config.url === '/fornecedores') return ok(config, companies.map(item => ({ ...item })));
    if (request.method === 'GET' && config.url === '/contratos') return ok(config, contracts.map(item => ({ ...item })));
    if (request.method === 'PUT' && /^\/contratos\/\d+\/fornecedor\/901$/.test(config.url)) return ok(config, { fornecedor: company });
    throw new Error(`Unexpected request ${request.method} ${config.url}`);
  };
  configurarBuscadorTokenCsrf(async ({ geracaoSessao }) => api.request({ method: 'GET', url: '/autenticacao/csrf', retries: 0, silentError: true, skipAuthLogout: true, __csrfBootstrap: true, __geracaoSessao: geracaoSessao }));
});
afterEach(() => {
  cleanup();
  api.defaults.adapter = originalAdapter;
  window.removeEventListener(EVENT_SHOW_TOAST, onToast);
  resetarCoordenadorCsrfParaTestes();
  resetarSolicitadorConfirmacaoSenhaParaTestes();
  vi.restoreAllMocks();
});

async function confirm() {
  const modal = await screen.findByRole('dialog');
  fireEvent.change(within(modal).getByLabelText('Senha de Acesso'), { target: { value: 'x'.repeat(8) } });
  fireEvent.click(within(modal).getByRole('button', { name: 'Confirmar', exact: true }));
}
async function selectExisting() {
  const row = (await screen.findByText(company.razaoSocial, { exact: true })).closest('tr');
  fireEvent.click(within(row).getByRole('button', { name: 'Vincular', exact: true }));
  const region = await screen.findByRole('region', { name: 'Vínculo pendente da empresa cadastrada' });
  const select = within(region).getByLabelText('Contrato para vincular a empresa cadastrada');
  expect(select).toHaveValue('');
  await within(region).findByRole('option', { name: draft.numeroContrato });
  await waitFor(() => expect(within(region).getByRole('button', { name: 'Vincular empresa cadastrada' })).toBeDisabled());
  fireEvent.change(select, { target: { value: '11' } });
  await waitFor(() => expect(within(region).getByRole('button', { name: 'Vincular empresa cadastrada' })).toBeEnabled());
  return region;
}
async function submitCreation() {
  render(<PaginaEmpresasContratadas />);
  await screen.findByText('Nenhuma empresa contratada encontrada.');
  fireEvent.change(screen.getByLabelText('Razão social da empresa'), { target: { value: company.razaoSocial } });
  fireEvent.change(screen.getByLabelText('CNPJ da empresa'), { target: { value: company.cnpj } });
  fireEvent.change(screen.getByLabelText('Contrato a vincular à empresa'), { target: { value: '11' } });
  fireEvent.click(screen.getByRole('button', { name: 'Cadastrar e vincular', exact: true }));
  await confirm();
}

describe('FE-05 recuperação explícita usa empresa do catálogo e somente associação', () => {
  it('empresa preexistente exige nova escolha de rascunho e senha, enviando só PUT', async () => {
    companies = [company];
    render(<PaginaEmpresasContratadas />);
    const region = await selectExisting();
    expect(mutations()).toEqual([]);
    fireEvent.click(within(region).getByRole('button', { name: 'Vincular empresa cadastrada' }));
    expect(mutations()).toEqual([]);
    await confirm();
    await waitFor(() => expect(screen.queryByRole('region', { name: 'Vínculo pendente da empresa cadastrada' })).toBeNull());
    expect(mutations().map(item => [item.method, item.url])).toEqual([['PUT', '/contratos/11/fornecedor/901']]);
  });

  it('POST com resposta perdida atualiza catálogo e recupera por ação explícita com POST1/PUT1', async () => {
    custom = (config, request) => {
      if (request.method === 'POST' && config.url === '/fornecedores') { companies.push(company); return networkError(config); }
      return undefined;
    };
    await submitCreation();
    await screen.findByText('Falha de conexão com o servidor.');
    fireEvent.click(screen.getByRole('button', { name: 'Cancelar', exact: true }));
    await screen.findByText(/Não foi possível confirmar o resultado do cadastro/);
    const region = await selectExisting();
    expect(mutations().map(item => item.method)).toEqual(['POST']);
    fireEvent.click(within(region).getByRole('button', { name: 'Vincular empresa cadastrada' }));
    await confirm();
    await waitFor(() => expect(screen.queryByRole('region', { name: 'Vínculo pendente da empresa cadastrada' })).toBeNull());
    expect(mutations().map(item => item.method)).toEqual(['POST', 'PUT']);
    expect(companies).toHaveLength(1);
    expect(screen.getByLabelText('CNPJ da empresa')).toHaveValue('');
    expect(notifications.filter(item => item.type === 'success').map(item => item.message)).not.toContainEqual(expect.stringMatching(/foi cadastrada/));
  });

  it('409 em contrato publicado conserva empresa e permite nova tentativa somente PUT em outro rascunho', async () => {
    companies = [company];
    custom = (config, request) => {
      if (request.method === 'PUT' && config.url === '/contratos/11/fornecedor/901') {
        contracts = [{ ...draft, estadoNegocial: 'PUBLICADO' }, { ...draft, id: 22, numeroContrato: 'Novo rascunho C25' }];
        return Promise.reject({ config, response: { status: 409, data: { message: 'Contrato C25 publicado.' }, headers: {} } });
      }
      return undefined;
    };
    render(<PaginaEmpresasContratadas />);
    let region = await selectExisting();
    fireEvent.click(within(region).getByRole('button', { name: 'Vincular empresa cadastrada' }));
    await confirm();
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
    region = screen.getByRole('region', { name: 'Vínculo pendente da empresa cadastrada' });
    expect(region).toHaveTextContent('já consta no cadastro');
    expect(region).toHaveTextContent('Contrato C25 publicado.');
    expect(within(region).getByLabelText('Contrato para vincular a empresa cadastrada')).toHaveValue('');
    expect(within(region).getByRole('button', { name: 'Tentar vínculo novamente' })).toBeDisabled();
    fireEvent.change(within(region).getByLabelText('Contrato para vincular a empresa cadastrada'), { target: { value: '22' } });
    fireEvent.click(within(region).getByRole('button', { name: 'Tentar vínculo novamente' }));
    await confirm();
    await waitFor(() => expect(screen.queryByRole('region', { name: 'Vínculo pendente da empresa cadastrada' })).toBeNull());
    expect(mutations().map(item => [item.method, item.url])).toEqual([['PUT', '/contratos/11/fornecedor/901'], ['PUT', '/contratos/22/fornecedor/901']]);
    expect(companies).toHaveLength(1);
  });

  it('troca de geração durante atualização após rede descarta catálogo e aviso antigos', async () => {
    let lookupConfig, resolveLookup;
    const lookup = new Promise(resolve => { resolveLookup = resolve; });
    custom = (config, request) => {
      if (request.method === 'POST' && config.url === '/fornecedores') { companies.push(company); return networkError(config); }
      if (request.method === 'GET' && config.url === '/fornecedores' && companies.length) { lookupConfig = config; return lookup; }
      return undefined;
    };
    await submitCreation();
    await waitFor(() => expect(lookupConfig).toBeDefined());
    await act(async () => { invalidarGeracaoSessao(); });
    expect(lookupConfig.signal.aborted).toBe(true);
    await act(async () => { resolveLookup(ok(lookupConfig, [company])); });
    expect(screen.queryByText(company.razaoSocial, { exact: true })).toBeNull();
    expect(screen.queryByText(/Não foi possível confirmar o resultado do cadastro/)).toBeNull();
    expect(screen.queryByRole('dialog')).toBeNull();
    expect(mutations().map(item => item.method)).toEqual(['POST']);
  });

  it('troca de geração encerra seleção e confirmação pendentes sem enviar PUT', async () => {
    companies = [company];
    render(<PaginaEmpresasContratadas />);
    const region = await selectExisting();
    fireEvent.click(within(region).getByRole('button', { name: 'Vincular empresa cadastrada' }));
    await screen.findByRole('dialog');
    await act(async () => { invalidarGeracaoSessao(); });
    expect(screen.queryByRole('dialog')).toBeNull();
    expect(screen.queryByRole('region', { name: 'Vínculo pendente da empresa cadastrada' })).toBeNull();
    expect(mutations()).toEqual([]);
  });

  it.each(['PERFIL_ADMINISTRADOR', 'PERFIL_GESTOR_NACIONAL', 'PERFIL_GESTOR_ESTADUAL', 'PERFIL_PRESIDENTE', 'PERFIL_OPERACIONAL'])('não oferece a nova ação ao perfil sem escrita contratual %s', async authority => {
    identity.user = { id: 992, authorities: [authority] };
    companies = [company];
    render(<PaginaEmpresasContratadas />);
    await screen.findByText(company.razaoSocial, { exact: true });
    expect(screen.queryByRole('button', { name: 'Vincular', exact: true })).toBeNull();
    expect(mutations()).toEqual([]);
  });
});
