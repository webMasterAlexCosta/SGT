// Independent counterexample: real frequency hook/services and real Axios;
// only the network adapter and surrounding user/confirmation UI are controlled.
import React from 'react';
import { act, cleanup, renderHook, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, expect, it, vi } from 'vitest';
const observer = vi.hoisted(() => ({ notify: vi.fn(), refresh: vi.fn() }));
vi.mock('../src/core/events.ts', async original => ({ ...(await original()), notificar: observer.notify }));
vi.mock('../src/modules/auth/context.tsx', () => ({ useAutenticacao: () => ({
  user: { id: 81, perfil: 'GESTOR_LOCAL', autoridade: 'PERFIL_GESTOR_LOCAL', vinculosGestao: [] }, atualizarPerfil: observer.refresh,
}) }));
vi.mock('../src/core/security/useConfirmacaoSenha.tsx', () => ({ useConfirmacaoSenha: () => ({
  confirmarComSenha: ({ acao }) => acao('senha-exclusivamente-sintetica'), ModalConfirmacao: () => null,
}) }));
import { useLogicaFrequencia } from '../src/modules/rh/pages/PaginaFrequencia/useLogicaFrequencia.ts';
import { api } from '../src/core/http.ts';
import { invalidarGeracaoSessao } from '../src/core/security/geracaoSessao.ts';
import { resetarCoordenadorCsrfParaTestes } from '../src/core/http/coordenadorCsrf.ts';
import { redefinirIndicadorRedirecionamento } from '../src/core/events.ts';

let originalAdapter;
const ok = (config, data) => ({ config, data, status: 200, statusText: 'OK', headers: {} });
beforeEach(() => {
  originalAdapter = api.defaults.adapter;
  resetarCoordenadorCsrfParaTestes(); redefinirIndicadorRedirecionamento();
  observer.notify.mockReset(); observer.refresh.mockReset();
});
afterEach(() => { cleanup(); api.defaults.adapter = originalAdapter; invalidarGeracaoSessao(); });

it('an obsolete submitted frequency must not notify the new session when transport cancels its delayed error', async () => {
  let oldConfig, rejectOld;
  const oldRequest = new Promise((_resolve, reject) => { rejectOld = reject; });
  api.defaults.adapter = async config => {
    if (config.url === '/autenticacao/csrf') return ok(config, { headerName: 'X-XSRF-TOKEN', token: 'token-sintetico' });
    if (config.url === '/lotacoes') return ok(config, [{ id: 7, nome: 'Lotacao sintetica', turnos: [{ id: 8, descricao: 'Turno sintetico' }] }]);
    if (config.url === '/contratos') return ok(config, [{ id: 9, ativo: true }]);
    if (config.url === '/jornadas') return ok(config, [{ id: 8, descricao: 'Turno sintetico', dias: [] }]);
    if (config.url === '/empregados/filtrados-frequencia') return ok(config, { content: [{ id: 17, nome: 'Pessoa sintetica', lotacaoId: 7, turnoId: 8 }], totalPages: 1, totalElements: 1, number: 0 });
    if (config.url === '/frequencias' && config.method === 'get') return ok(config, [{ id: 71, empregado: { id: 17 }, vinculoContratualId: 501, status: 'PRESENCA', validadeOperacional: 'VALIDA', estadoNegocial: 'ABERTA' }]);
    if (config.url === '/frequencias' && config.method === 'post') { oldConfig = config; return oldRequest; }
    if (config.url === '/frequencias/fechamentos/atual') return ok(config, null);
    return ok(config, []);
  };
  const { result, unmount } = renderHook(() => useLogicaFrequencia());
  await waitFor(() => expect(result.current.estado.listaDaFrequencia).toHaveLength(1));
  act(() => {
    result.current.acoes.setJustificativaRetificacao('Conferencia sintetica independente');
    result.current.acoes.invocarAlteracaoDeAnotacaoDeObservacao(17, 'Rascunho da sessao A');
  });
  let submitted;
  await act(async () => { submitted = result.current.acoes.invocarGatilhoDeDespachoAoServidorGlobal(); });
  await waitFor(() => expect(oldConfig).toBeDefined());
  // Matches layout teardown in RoteadorPrincipal plus logout/new login generations.
  unmount();
  act(() => { invalidarGeracaoSessao(); invalidarGeracaoSessao(); });
  redefinirIndicadorRedirecionamento();
  observer.notify.mockClear();
  await act(async () => {
    rejectOld({ config: oldConfig, response: { status: 409, data: { code: 'CONFLICT', message: 'Conflito da sessao A' }, headers: {} } });
    await submitted;
  });
  expect(observer.notify.mock.calls).toEqual([]);
});
