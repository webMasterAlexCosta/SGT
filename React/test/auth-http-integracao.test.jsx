// Provider, serviço e Axios reais; adapter controlado, sem backend real.
import React from 'react';
import { afterEach, beforeEach, expect, it, vi } from 'vitest';
import { act, cleanup, render, waitFor } from '@testing-library/react';
import { ProvedorAutenticacao, useAutenticacao } from '../src/modules/auth/context.jsx';
import { api } from '../src/core/http.js';
import { resetarCoordenadorCsrfParaTestes } from '../src/core/http/coordenadorCsrf.js';
import { redefinirIndicadorRedirecionamento } from '../src/core/events.js';
vi.mock('../src/modules/auth/components/ModalReautenticacao', () => ({ ModalReautenticacao: () => null }));
const A = { id: 901, username: 'a-sintetico', email: 'antes@example.invalid', perfil: 'GESTOR_LOCAL', autoridade: 'PERFIL_GESTOR_LOCAL' };
let contexto, adapter;
const Observador = () => { contexto = useAutenticacao(); return <output>{contexto.user?.email || 'sem-sessao'}</output>; };
const ok = (config, data) => ({ config, data, status: 200, statusText: 'OK', headers: {} });
beforeEach(() => { adapter = api.defaults.adapter; resetarCoordenadorCsrfParaTestes(); redefinirIndicadorRedirecionamento(); });
afterEach(() => { cleanup(); api.defaults.adapter = adapter; });
it('erro401 de revalidação superada não dispara logout antes do descarte no provider', async () => {
  let rejeitarAntiga, configAntiga, contador = 0;
  const antiga = new Promise((resolve, reject) => { rejeitarAntiga = reject; });
  api.defaults.adapter = async config => {
    if (config.url === '/autenticacao/sessao') return ok(config, { authenticated: true, user: A, session: { remainingSeconds: 3600 } });
    if (config.url === '/autenticacao/csrf') return ok(config, { headerName: 'X-XSRF-TOKEN', token: 'sintetico' });
    if (config.url === '/usuarios/meu-perfil') {
      contador += 1;
      if (contador === 2) { configAntiga = config; return antiga; }
      return ok(config, contador === 1 ? A : { ...A, email: 'atual@example.invalid' });
    }
    return ok(config, {});
  };
  render(<ProvedorAutenticacao><Observador /></ProvedorAutenticacao>);
  await waitFor(() => expect(contexto.loading).toBe(false));
  let p1; act(() => { p1 = contexto.atualizarPerfil(); });
  await waitFor(() => expect(configAntiga).toBeDefined());
  await act(async () => contexto.atualizarPerfil());
  expect(contexto.user.email).toBe('atual@example.invalid');
  await act(async () => {
    rejeitarAntiga({ config: configAntiga, response: { status: 401, data: { code: 'SESSION_REPLACED', action: 'LOGOUT' }, headers: {} } });
    await p1;
  });
  expect(contexto.user?.email).toBe('atual@example.invalid'); expect(contexto.loggingOut).toBe(false);
});
