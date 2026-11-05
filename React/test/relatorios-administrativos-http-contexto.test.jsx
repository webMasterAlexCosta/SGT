import React from 'react';
import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, expect, it, vi } from 'vitest';
vi.mock('../src/modules/auth/context.jsx', () => ({ useAutenticacao: () => ({ user: { id: 12, perfil: 'GESTOR_LOCAL' } }) }));
import { PaginaRelatoriosAdministrativos } from '../src/modules/rh/pages/PaginaRelatoriosAdministrativos/index.jsx';
import { api } from '../src/core/http.js';
import { EVENT_SHOW_TOAST, redefinirIndicadorRedirecionamento } from '../src/core/events.js';
import { invalidarGeracaoSessao } from '../src/core/security/geracaoSessao.js';
import { resetarCoordenadorCsrfParaTestes } from '../src/core/http/coordenadorCsrf.js';

const ok = (config, data) => ({ config, data, status: 200, statusText: 'OK', headers: {} });
const pagina = { content: [{ id: 77, tipo: 'FREQUENCIA_CONSOLIDADA', versao: 1 }], number: 0, totalPages: 1, totalElements: 1, first: true, last: true };
let originalAdapter, notices, observe;
beforeEach(() => {
  originalAdapter = api.defaults.adapter;
  resetarCoordenadorCsrfParaTestes(); redefinirIndicadorRedirecionamento();
  notices = []; observe = event => notices.push(event.detail);
  window.addEventListener(EVENT_SHOW_TOAST, observe);
});
afterEach(() => {
  cleanup(); api.defaults.adapter = originalAdapter;
  window.removeEventListener(EVENT_SHOW_TOAST, observe);
  invalidarGeracaoSessao(); redefinirIndicadorRedirecionamento();
});

it.each([
  ['emissão', '/relatorios/administrativos/emissoes', 'post', 'Emitir versão formal'],
  ['abertura', '/relatorios/administrativos/emissoes/77', 'get', 'Abrir'],
  ['conhecimento', '/relatorios/administrativos/emissoes/conhecimento', 'post', 'Consultar'],
  ['comparação', '/relatorios/administrativos/emissoes/77/comparacao-vigente', 'get', 'Comparar'],
])('%s antiga não publica erro global nem apaga a prévia atual', async (nome, endpoint, metodo, botao) => {
  let oldConfig, rejectOld;
  const old = new Promise((_resolve, reject) => { rejectOld = reject; });
  api.defaults.adapter = config => {
    if (config.url === '/autenticacao/csrf') return Promise.resolve(ok(config, { headerName: 'X-XSRF-TOKEN', token: 'sintetico' }));
    if (config.url === endpoint && config.method === metodo) { oldConfig = config; return old; }
    if (config.url === '/relatorios/filtros') return Promise.resolve(ok(config, { contratos: [], lotacoes: [], turnos: [] }));
    if (config.url === '/relatorios/administrativos/emissoes') return Promise.resolve(ok(config, pagina));
    if (config.url === '/relatorios/administrativos/previa') return Promise.resolve(ok(config, {
      metadados: { tipo: 'ABSENTEISMO', formato: 'VISUALIZACAO', versao: 0 },
      conteudo: [{ empregadoId: 2, nome: 'Resultado B atual' }],
    }));
    throw new Error('Requisição inesperada: ' + config.url);
  };
  render(<PaginaRelatoriosAdministrativos podeEmitir />);
  await screen.findByText('#77');
  if (nome === 'conhecimento') fireEvent.change(screen.getByLabelText('Instante de conhecimento'), { target: { value: '2026-11-01T12:00' } });
  fireEvent.click(screen.getByRole('button', { name: botao, exact: true }));
  await waitFor(() => expect(oldConfig).toBeDefined());
  fireEvent.change(screen.getByLabelText('Relatório'), { target: { value: 'ABSENTEISMO' } });
  fireEvent.click(screen.getByRole('button', { name: 'Gerar prévia' }));
  await screen.findByText('Resultado B atual');
  notices.length = 0;
  await act(async () => {
    rejectOld({ config: oldConfig, response: { status: 403, data: { code: 'ACCESS_DENIED', message: 'Erro antigo de ' + nome }, headers: {} } });
    await new Promise(resolve => setTimeout(resolve, 0));
  });
  expect(screen.getByText('Resultado B atual')).toBeTruthy();
  expect(notices).toEqual([]);
});

it('erro pertinente continua sendo apresentado uma única vez, sem mascarar recusa do Java', async () => {
  api.defaults.adapter = config => {
    if (config.url === '/autenticacao/csrf') return Promise.resolve(ok(config, { headerName: 'X-XSRF-TOKEN', token: 'sintetico' }));
    if (config.url === '/relatorios/filtros') return Promise.resolve(ok(config, { contratos: [], lotacoes: [], turnos: [] }));
    if (config.url === '/relatorios/administrativos/emissoes') return Promise.resolve(ok(config, pagina));
    return Promise.reject({ config, response: { status: 403, data: { code: 'ACCESS_DENIED', message: 'Recusa atual do Java' }, headers: {} } });
  };
  render(<PaginaRelatoriosAdministrativos />);
  await screen.findByText('#77');
  fireEvent.click(screen.getByRole('button', { name: 'Gerar prévia' }));
  await waitFor(() => expect(notices.some(n => n.message === 'Recusa atual do Java')).toBe(true));
  expect(notices.filter(n => n.message === 'Recusa atual do Java')).toHaveLength(1);
});
