// Provider, serviço e Axios reais; respostas HTTP controladas, sem serviços externos.
import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, cleanup, render, waitFor } from '@testing-library/react';
import { ProvedorAutenticacao, useAutenticacao } from '../src/modules/auth/context.tsx';
import { ServicoAutenticacao } from '../src/services/auth/ServicoAutenticacao.ts';
import { api } from '../src/core/http.ts';
import { resetarCoordenadorCsrfParaTestes } from '../src/core/http/coordenadorCsrf.ts';
import { redefinirIndicadorRedirecionamento } from '../src/core/events.ts';
vi.mock('../src/modules/auth/components/ModalReautenticacao', () => ({ ModalReautenticacao: () => null }));
const A = { id: 831, username: 'http-a-sintetico', perfil: 'GESTOR_LOCAL', autoridade: 'PERFIL_GESTOR_LOCAL' };
const B = { id: 832, username: 'http-b-sintetico', perfil: 'OPERACIONAL', autoridade: 'PERFIL_OPERACIONAL' };
let contexto, adapter;
const Observador = () => { contexto = useAutenticacao(); return <output>{contexto.user?.id || 'sem-sessao'}</output>; };
const montar = () => render(<ProvedorAutenticacao><Observador /></ProvedorAutenticacao>);
const ok = (config, data) => ({ config, data, status: 200, statusText: 'OK', headers: {} });
beforeEach(() => { localStorage.clear(); sessionStorage.clear(); adapter = api.defaults.adapter; resetarCoordenadorCsrfParaTestes(); redefinirIndicadorRedirecionamento(); });
afterEach(() => { cleanup(); api.defaults.adapter = adapter; });
describe('Saída ordenada no transporte real do frontend', () => {
  it('login B só chega ao adapter depois da resposta de logoutA, mesmo após remontar provider', async () => {
    let finalizar, cfgLogout, usuario = A; const eventos = [];
    const fim = new Promise(resolve => { finalizar = resolve; });
    api.defaults.adapter = async config => {
      if (config.url === '/autenticacao/csrf') return ok(config, { headerName: 'X-XSRF-TOKEN', token: 'sintetico' });
      if (config.url === '/autenticacao/sessao') return ok(config, { authenticated: true, user: usuario, session: { remainingSeconds: 3600 } });
      if (config.url === '/usuarios/meu-perfil') return ok(config, usuario);
      if (config.url === '/autenticacao/encerrar-sessao') { cfgLogout = config; eventos.push('logout-enviado'); const res = await fim; eventos.push('logout-recebido'); return res; }
      if (config.url === '/autenticacao/autenticar') { eventos.push('login-enviado'); usuario = B; return ok(config, B); }
      throw new Error(`Rota inesperada no cenário: ${config.url}`);
    };
    const view = montar(); await waitFor(() => expect(contexto.user?.id).toBe(A.id));
    act(() => contexto.encerrarSessao()); await waitFor(() => expect(cfgLogout).toBeDefined());
    view.unmount(); montar(); await waitFor(() => expect(contexto.loading).toBe(false));
    let login; act(() => { login = contexto.autenticar('b', 'senha-apenas-mock'); });
    await act(async () => { await Promise.resolve(); }); const eventosAntes = [...eventos];
    await act(async () => { finalizar(ok(cfgLogout, { message: 'Logout sintético' })); await login; });
    expect(eventosAntes).toEqual(['logout-enviado']); expect(eventos).toEqual(['logout-enviado', 'logout-recebido', 'login-enviado']); expect(contexto.user?.id).toBe(B.id);
  });
  it('falha HTTP de encerramento não é comunicada pelo serviço como revogação bem-sucedida', async () => {
    api.defaults.adapter = async config => {
      if (config.url === '/autenticacao/csrf') return ok(config, { headerName: 'X-XSRF-TOKEN', token: 'sintetico' });
      throw { config, response: { status: 500, headers: {}, data: { message: 'Falha sintética' } } };
    };
    expect(await ServicoAutenticacao.encerrarSessao()).toBe(false);
  });
});
