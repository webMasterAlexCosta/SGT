// Independent integration of the real administrative screen, service and Axios.
// Only authenticated user context and the network adapter are controlled.
import React from 'react';
import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, expect, it, vi } from 'vitest';
vi.mock('@review/modules/auth/context.jsx', () => ({ useAutenticacao: () => ({ user: { id: 12, perfil: 'GESTOR_LOCAL' } }) }));
import { PaginaRelatoriosAdministrativos } from '@review/modules/rh/pages/PaginaRelatoriosAdministrativos/index.jsx';
import { api } from '@review/core/http.js';
import { EVENT_SHOW_TOAST, redefinirIndicadorRedirecionamento } from '@review/core/events.js';
import { invalidarGeracaoSessao } from '@review/core/security/geracaoSessao.js';
import { resetarCoordenadorCsrfParaTestes } from '@review/core/http/coordenadorCsrf.js';

const ok = (config, data) => ({ config, data, status: 200, statusText: 'OK', headers: {} });
const page = { content: [], number: 0, totalPages: 0, totalElements: 0, first: true, last: true };
let originalAdapter, notices, observe;
beforeEach(() => {
  originalAdapter = api.defaults.adapter;
  resetarCoordenadorCsrfParaTestes(); redefinirIndicadorRedirecionamento();
  notices = [];
  observe = event => notices.push(event.detail);
  window.addEventListener(EVENT_SHOW_TOAST, observe);
});
afterEach(() => {
  cleanup(); api.defaults.adapter = originalAdapter;
  window.removeEventListener(EVENT_SHOW_TOAST, observe);
  invalidarGeracaoSessao(); redefinirIndicadorRedirecionamento();
});

it('a delayed preview error must not publish a global toast after another filter has succeeded', async () => {
  let oldConfig, rejectOld;
  const oldRequest = new Promise((_resolve, reject) => { rejectOld = reject; });
  api.defaults.adapter = config => {
    if (config.url === '/autenticacao/csrf') return Promise.resolve(ok(config, { headerName: 'X-XSRF-TOKEN', token: 'token-exclusivamente-sintetico' }));
    if (config.url === '/relatorios/filtros') return Promise.resolve(ok(config, { contratos: [], lotacoes: [], turnos: [] }));
    if (config.url === '/relatorios/administrativos/emissoes') return Promise.resolve(ok(config, page));
    if (config.url === '/relatorios/administrativos/previa') {
      const body = JSON.parse(config.data);
      if (body.tipo === 'FREQUENCIA_CONSOLIDADA') { oldConfig = config; return oldRequest; }
      return Promise.resolve(ok(config, { metadados: { tipo: 'ABSENTEISMO', formato: 'VISUALIZACAO', versao: 0 }, conteudo: [{ empregadoId: 202, nome: 'Resultado exclusivo do filtro atual' }] }));
    }
    throw new Error('Unexpected controlled request: ' + config.url);
  };
  render(<PaginaRelatoriosAdministrativos />);
  await screen.findByText('Nenhuma emissão formal encontrada.');
  fireEvent.click(screen.getByRole('button', { name: 'Gerar prévia' }));
  await waitFor(() => expect(oldConfig).toBeDefined());
  fireEvent.change(screen.getByLabelText('Relatório'), { target: { value: 'ABSENTEISMO' } });
  fireEvent.click(screen.getByRole('button', { name: 'Gerar prévia' }));
  await screen.findByText('Resultado exclusivo do filtro atual');
  notices.length = 0;
  await act(async () => {
    rejectOld({ config: oldConfig, response: { status: 403, data: { code: 'ACCESS_DENIED', message: 'Falha exclusiva da prévia antiga' }, headers: {} } });
    await new Promise(resolve => setTimeout(resolve, 0));
  });
  expect(screen.getByText('Resultado exclusivo do filtro atual')).toBeTruthy();
  expect(notices).toEqual([]);
});
