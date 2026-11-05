// Oráculos independentes C25. Componentes/serviços/interceptores reais.
// Apenas identidade sintética do hook e transporte Axios são controlados.
// Os aliases permitem executar os mesmos oráculos contra o original e a derivada.
import React from 'react';
import { act, cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import '@testing-library/jest-dom/vitest';
import { IndicadorEfetivo } from '@sgt/modules/painel/components/IndicadorEfetivo/index.jsx';
import { PaginaEmpresasContratadas } from '@sgt/modules/admin/pages/PaginaEmpresasContratadas/index.jsx';
import { api } from '@sgt/core/http.js';
import { configurarBuscadorTokenCsrf, resetarCoordenadorCsrfParaTestes } from '@sgt/core/http/coordenadorCsrf.js';
import { resetarSolicitadorConfirmacaoSenhaParaTestes } from '@sgt/core/http/coordenadorConfirmacaoSenha.js';
import { invalidarGeracaoSessao } from '@sgt/core/security/geracaoSessao.js';
import { EVENT_SHOW_TOAST, redefinirIndicadorRedirecionamento } from '@sgt/core/events.js';

vi.mock('@sgt/modules/auth/context.jsx', () => ({
  useAutenticacao: () => ({ user: { id: 991, authorities: ['PERFIL_GESTOR_LOCAL'] } }),
}));

const ok = (config, data) => ({ config, data, status: 200, statusText: 'OK', headers: {} });
const rejected = (config, status, message) => Promise.reject({ config, response: { status, data: { message }, headers: {} } });
const networkError = config => Promise.reject(Object.assign(new Error('Network Error'), { config, code: 'ERR_NETWORK', request: {} }));
const catalogs = { contratos: [{ id: 11, nome: 'Contrato C25' }], lotacoes: [], turnos: [], cargos: [], permiteDetalhamentoOperacional: true };
const summary = { efetivoPrevisto: 10, efetivoAlocado: 15, registrosLancados: 15, pendentesLancamento: 0, presentes: 15, ausentes: 0, outrosRegistros: 0, deficitContratual: 0, excedenteContratual: 5, percentualCobertura: 100, percentualAtendimento: 150 };
const indicator = { inicio: '2026-10-01', fim: '2026-10-31', periodo: 'MENSAL', resumoAtual: summary, resumoAnoAnterior: null, serieAtual: [], serieAnoAnterior: [], detalhamentoContratos: [{ id: 11, nome: 'Contrato C25', ...summary }], detalhamentoLotacoes: [], detalhamentoTurnos: [], detalhamentoCargos: [], quadroDimensionado: [] };
const company = { id: 901, razaoSocial: 'Empresa C25 sintética', cnpj: '11222333000144', email: null, ativo: true };
let original, custom, sends, companies, notifications;
const onToast = event => notifications.push(event.detail);
const mutations = () => sends.filter(request => ['POST', 'PUT'].includes(request.method));

beforeEach(() => {
  original = api.defaults.adapter;
  custom = null;
  sends = [];
  companies = [];
  notifications = [];
  window.addEventListener(EVENT_SHOW_TOAST, onToast);
  invalidarGeracaoSessao();
  resetarCoordenadorCsrfParaTestes();
  resetarSolicitadorConfirmacaoSenhaParaTestes();
  redefinirIndicadorRedirecionamento();
  api.defaults.adapter = async config => {
    const request = { url: config.url, method: config.method.toUpperCase(), signal: config.signal };
    sends.push(request);
    const special = custom?.(config, request);
    if (special !== undefined) return special;
    if (config.url === '/autenticacao/csrf') return ok(config, { headerName: 'X-XSRF-TOKEN', parameterName: '_csrf', token: 'synthetic-c25' });
    if (config.url === '/painel/efetivo/filtros') return ok(config, catalogs);
    if (config.url === '/painel/efetivo') return ok(config, indicator);
    if (request.method === 'GET' && config.url === '/contratos') return ok(config, [{ id: 11, numeroContrato: 'Contrato C25', ativo: true, estadoNegocial: 'RASCUNHO' }]);
    if (request.method === 'GET' && config.url === '/fornecedores') return ok(config, companies.map(value => ({ ...value })));
    if (request.method === 'POST' && config.url === '/fornecedores') { companies.push(company); return ok(config, company); }
    if (request.method === 'PUT' && config.url === '/contratos/11/fornecedor/901') return ok(config, { contratoId: 11, fornecedor: company });
    throw new Error(`Unexpected request ${request.method} ${request.url}`);
  };
  configurarBuscadorTokenCsrf(async ({ geracaoSessao }) => api.request({ method: 'GET', url: '/autenticacao/csrf', retries: 0, silentError: true, skipAuthLogout: true, __csrfBootstrap: true, __geracaoSessao: geracaoSessao }));
});
afterEach(() => {
  cleanup();
  api.defaults.adapter = original;
  window.removeEventListener(EVENT_SHOW_TOAST, onToast);
  resetarCoordenadorCsrfParaTestes();
  resetarSolicitadorConfirmacaoSenhaParaTestes();
  vi.restoreAllMocks();
});

async function openCompany() {
  render(<PaginaEmpresasContratadas />);
  await screen.findByText('Nenhuma empresa contratada encontrada.');
  fireEvent.change(screen.getByLabelText('Razão social da empresa'), { target: { value: company.razaoSocial } });
  fireEvent.change(screen.getByLabelText('CNPJ da empresa'), { target: { value: company.cnpj } });
  fireEvent.change(screen.getByLabelText('Contrato a vincular à empresa'), { target: { value: '11' } });
  fireEvent.click(screen.getByRole('button', { name: 'Cadastrar e vincular', exact: true }));
}
async function confirm() {
  const modal = await screen.findByRole('dialog');
  fireEvent.change(within(modal).getByLabelText('Senha de Acesso'), { target: { value: 'x'.repeat(8) } });
  fireEvent.click(within(modal).getByRole('button', { name: 'Confirmar', exact: true }));
}

describe('C25: integridade de filtros e valor autoritativo', () => {
  it('FE-03 mantém explícito o erro do catálogo mesmo se o indicador consolidado carregar', async () => {
    custom = config => config.url === '/painel/efetivo/filtros' ? rejected(config, 403, 'CATALOGO_C25_RECUSADO') : undefined;
    render(<IndicadorEfetivo referencia="2026-10-31" />);
    await waitFor(() => expect(screen.getByRole('button', { name: 'XLSX', exact: true })).toBeEnabled());
    expect(screen.queryByText('CATALOGO_C25_RECUSADO')).not.toBeNull();
  });
  it('FE-03 a recuperação do catálogo remove somente seu erro', async () => {
    let failCatalog = true;
    custom = config => {
      if (config.url === '/painel/efetivo/filtros' && failCatalog) return rejected(config, 403, 'CATALOGO_C25_RECUSADO');
      if (config.url === '/painel/efetivo') return rejected(config, 403, 'INDICADOR_C25_RECUSADO');
      return undefined;
    };
    const view = render(<IndicadorEfetivo referencia="2026-10-30" />);
    await screen.findByText('INDICADOR_C25_RECUSADO');
    expect(screen.queryByText('CATALOGO_C25_RECUSADO')).not.toBeNull();
    failCatalog = false;
    view.rerender(<IndicadorEfetivo referencia="2026-10-31" />);
    await screen.findByRole('option', { name: 'Contrato C25' });
    await screen.findByText('INDICADOR_C25_RECUSADO');
    expect(screen.queryByText('CATALOGO_C25_RECUSADO')).toBeNull();
    expect(screen.getByRole('button', { name: 'XLSX', exact: true })).toBeDisabled();
  });
  it('FE-04 preserva 150% devolvido pelo Java na coluna de atendimento do detalhamento', async () => {
    render(<IndicadorEfetivo referencia="2026-10-31" />);
    await waitFor(() => expect(screen.getByRole('button', { name: 'XLSX', exact: true })).toBeEnabled());
    fireEvent.click(screen.getByRole('button', { name: /Abrir leitura operacional/ }));
    const modal = screen.getByRole('dialog', { name: 'Leitura operacional do efetivo' });
    const row = within(modal).getByText('Contrato C25', { exact: true }).closest('tr');
    const cell = within(row).getAllByRole('cell').at(-1);
    expect(cell).toHaveTextContent('150,0%');
    expect(cell.querySelector('i')).toHaveStyle({ width: '100%' });
  });
  it.each([[0, '0,0%', '0%'], [75.5, '75,5%', '75.5%'], [100, '100,0%', '100%']])('FE-04 preserva texto e barra para %s%%', async (taxa, texto, largura) => {
    custom = config => config.url === '/painel/efetivo' ? ok(config, {
      ...indicator,
      detalhamentoContratos: [{ id: 11, nome: 'Contrato C25', ...summary, percentualAtendimento: taxa }],
    }) : undefined;
    render(<IndicadorEfetivo referencia="2026-10-31" />);
    await waitFor(() => expect(screen.getByRole('button', { name: 'XLSX', exact: true })).toBeEnabled());
    fireEvent.click(screen.getByRole('button', { name: /Abrir leitura operacional/ }));
    const modal = screen.getByRole('dialog', { name: 'Leitura operacional do efetivo' });
    const row = within(modal).getByText('Contrato C25', { exact: true }).closest('tr');
    const cell = within(row).getAllByRole('cell').at(-1);
    expect(cell).toHaveTextContent(texto);
    expect(cell.querySelector('i')).toHaveStyle({ width: largura });
  });
});

describe('C25 RI03: redes e invalidação da intenção', () => {
  it('POST conhecido + perda de resposta PUT preserva empresa e repete exclusivamente a associação', async () => {
    let failAssociation = true;
    custom = (config, request) => request.method === 'PUT' && config.url.includes('/fornecedor/') && failAssociation ? networkError(config) : undefined;
    await openCompany(); await confirm();
    await screen.findByRole('region', { name: 'Vínculo pendente da empresa cadastrada' });
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
    expect(companies).toHaveLength(1);
    expect(mutations().map(request => request.method)).toEqual(['POST', 'PUT']);
    failAssociation = false;
    fireEvent.click(screen.getByRole('button', { name: 'Tentar vínculo novamente', exact: true }));
    await confirm();
    await waitFor(() => expect(screen.queryByRole('region', { name: 'Vínculo pendente da empresa cadastrada' })).toBeNull());
    expect(mutations().map(request => request.method)).toEqual(['POST', 'PUT', 'PUT']);
    expect(companies).toHaveLength(1);
  });
  it('troca de geração durante POST impede associação e sucesso tardios', async () => {
    let pendingConfig, resolvePost;
    const deferred = new Promise(resolve => { resolvePost = resolve; });
    custom = (config, request) => {
      if (request.method === 'POST' && config.url === '/fornecedores') { pendingConfig = config; return deferred; }
      return undefined;
    };
    await openCompany(); await confirm();
    await waitFor(() => expect(pendingConfig).toBeDefined());
    await act(async () => { invalidarGeracaoSessao(); });
    expect(pendingConfig.signal.aborted).toBe(true);
    await act(async () => { resolvePost(ok(pendingConfig, company)); });
    expect(mutations().map(request => request.method)).toEqual(['POST']);
    expect(notifications.filter(value => value.type === 'success')).toEqual([]);
    expect(screen.queryByRole('dialog')).toBeNull();
  });
});
